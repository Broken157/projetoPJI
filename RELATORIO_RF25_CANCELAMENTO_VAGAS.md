# Relatório Técnico — RF25: Cancelamento de Vagas

## 1. Objetivo e escopo

Auditar e completar somente os aspectos viáveis do cancelamento lógico de vagas, preservando o endpoint, o banco oficial, o frontend e os RFs já concluídos. O núcleo analisado inclui estados, propriedade por JWT, candidaturas vinculadas, log, transação, rollback e regressões. Motivo persistido, confirmação obrigatória na API e notificações foram avaliados conforme a estrutura real.

## 2. Estado inicial

### Existente no `DELETE`

- `DELETE /api/vagas/{id}` retornando `204 No Content`;
- método `VagaService.deletar` anotado com `@Transactional`;
- localização da vaga e `404` para ID inexistente;
- validação de usuário `CONTRATANTE` proprietário pelo JWT/relacionamento persistido;
- cancelamento permitido para `ABERTA` e `PAUSADA`;
- soft delete por `vaga.status = CANCELADA`;
- carregamento das candidaturas e alteração de todas para `CANCELADA_POR_VAGA`;
- criação de `LogVagaCancelada` com vaga, usuário e timestamp;
- tabela oficial `log_vagas_canceladas` e repository correspondente;
- confirmação visual no frontend por digitação de `DELETAR`, seguida do mesmo `DELETE` sem body.

### Correto

- não havia `vagaRepository.delete` no fluxo de vaga;
- a vaga e as candidaturas eram preservadas fisicamente;
- os dois estados permitidos já seguiam a decisão consolidada;
- a operação já era única e transacional;
- identidade e propriedade não dependiam de IDs enviados pelo cliente;
- log era estruturalmente persistido;
- RF03, RF05 e RF06 já possuíam comportamento compatível com `CANCELADA`.

### Parcial

- `ENCERRADA` e `CANCELADA` eram bloqueadas, mas por `IllegalArgumentException`, produzindo `400` em vez de `422`;
- existia somente um cenário indireto de teste do cancelamento no CRUD RF03;
- a confirmação existe na interface, mas não é exigida pelo contrato backend;
- a tabela de log não possui coluna para motivo, e nenhum DTO/request de cancelamento existia.

### Ausente

- motivo obrigatório e persistido;
- suíte dedicada ao RF25;
- prova integrada de rollback após falha no registro do log;
- testes de todos os status de candidatura, IDOR e regressões cruzadas;
- notificações de candidatos;
- locking/versionamento concorrente.

Baseline real antes das alterações: **175 testes aprovados, 0 falhas, 0 erros e 0 ignorados**, com `BUILD SUCCESS`.

## 3. Auditoria por camada

| Camada | Estado inicial | Resultado |
| --- | --- | --- |
| Entity | Parcial para o requisito completo | `Vaga`, `Candidatura` e `LogVagaCancelada` atendem ao núcleo, mas o log não possui motivo. |
| Enum | Correto | `StatusVaga` contém quatro estados e `StatusCandidatura` contém `CANCELADA_POR_VAGA`. |
| DTO | Ausente para cancelamento | Não havia request de motivo/confirmação; não foi criado porque o motivo não poderia ser legitimamente persistido e a rota atual seria quebrada. |
| Repository | Correto | Repositories existentes suportam vaga, candidaturas e log; nenhuma exclusão física é chamada. |
| Service | Parcial | Orquestração já correta; ajustado somente o erro de transição inválida para `422`. |
| Controller | Correto para o contrato atual | Rota `DELETE /api/vagas/{id}` e `204` preservados. |
| Segurança | Correto, sem prova dedicada | JWT, papel e propriedade já estavam no Service; agora possuem cenários específicos, inclusive IDOR. |
| Banco | Parcial para motivo | `log_vagas_canceladas` existe, mas armazena somente vaga, usuário e data. |
| Testes | Ausente como suíte RF25 | Criada suíte com 13 métodos e 16 execuções. |

## 4. Cancelamento preexistente

Antes desta tarefa, `VagaService.deletar` já executava, na mesma transação:

1. busca da vaga;
2. resolução do contratante autenticado;
3. comparação da propriedade persistida;
4. validação de `ABERTA` ou `PAUSADA`;
5. mudança da vaga para `CANCELADA`;
6. atualização de todas as candidaturas para `CANCELADA_POR_VAGA`;
7. criação de `log_vagas_canceladas` com vaga, usuário e horário.

O método não fazia hard delete. A lacuna funcional corrigível era o código HTTP das transições inválidas. Motivo e confirmação não eram recebidos pela API.

## 5. Máquina de estados

Regra preservada e testada:

```text
ABERTA → CANCELADA
PAUSADA → CANCELADA
```

Bloqueios:

```text
ENCERRADA → CANCELADA = 422
CANCELADA → CANCELADA = 422
CANCELADA → SUSPENDER/REABRIR/ENCERRAR = 422 pelo RF31
```

A mensagem agora informa o estado atual e os estados permitidos. O `PUT` do RF07 continua sem controlar status.

## 6. Conflito documental

A descrição antiga limitava o cancelamento a vagas `ABERTA`. A decisão consolidada posterior permite `ABERTA` ou `PAUSADA`.

Foi mantida a decisão consolidada, que já estava corretamente presente no Service. Ambos os caminhos foram provados na suíte dedicada.

## 7. Soft delete

O endpoint permanece:

```http
DELETE /api/vagas/{id}
```

com retorno `204 No Content`. Apesar do verbo HTTP, a remoção é lógica: o registro permanece em `vagas`, com o mesmo ID, e seu status passa para `CANCELADA`.

O teste consulta repository e PostgreSQL diretamente e comprova:

- registro ainda existente;
- mesmo ID;
- contagem física igual a 1;
- status `CANCELADA`;
- log vinculado ao mesmo ID.

## 8. Candidaturas

O enum real contém:

- `PENDENTE`;
- `EM_ANALISE`;
- `APROVADO`;
- `REJEITADO`;
- `RETIRADA`;
- `CANCELADA_POR_VAGA`.

A regra consolidada atual determina que todas as candidaturas vinculadas recebam `CANCELADA_POR_VAGA`. O comportamento preexistente já fazia isso e foi preservado. O teste cria uma candidatura em cada estado real e confirma que:

- todas continuam fisicamente existentes;
- quantidade, IDs e `vaga_id` permanecem;
- todas terminam em `CANCELADA_POR_VAGA`;
- nenhuma candidatura é deletada.

Essa decisão substitui o estado corrente inclusive de candidaturas aprovadas, rejeitadas ou retiradas. Foi adotada porque o requisito atual diz explicitamente “todas” e o código já consolidava essa semântica. Não existe tabela separada para guardar o status anterior; a preservação comprovada refere-se ao registro, vínculo e identidade histórica.

## 9. Motivo

Não existe campo de motivo em `vagas`, `LogVagaCancelada` ou `log_vagas_canceladas`. A tabela de log oficial possui apenas:

- `id`;
- `vaga_id`;
- `cancelado_por_id`;
- `data_cancelamento`.

Portanto, o motivo **não é recebido nem persistido**. Não foi criado DTO que descartaria silenciosamente o dado, coluna artificial, migration ou log textual fingindo atender ao requisito.

Esse é um bloqueio estrutural para a conclusão integral do RF25 e exige decisão futura de banco/contrato.

## 10. Log

**`log_vagas_canceladas` existe: SIM**

O cancelamento cria um registro com:

- FK da vaga cancelada;
- FK do usuário contratante que realizou a ação;
- timestamp de cancelamento.

A suíte valida vaga, usuário, intervalo temporal e unicidade no fluxo testado. A estrutura não comporta motivo.

## 11. Transação e rollback

`VagaService.deletar` permanece anotado com `@Transactional`. Vaga, candidaturas e log participam da mesma transação.

Para provar rollback real, o teste adiciona somente no PostgreSQL efêmero do Testcontainers uma constraint temporária que rejeita a inserção do log. A chamada alcança a etapa do log e falha por integridade. Depois da falha:

- vaga continua `ABERTA`;
- candidatura continua `PENDENTE`;
- nenhum log parcial existe.

A constraint temporária é removida no próprio teste/limpeza e nunca integra schema, migration ou banco oficial. A falha injetada é mapeada pelo handler global para `409 Conflict`; o ponto comprovado é a atomicidade/rollback integral.

## 12. Segurança

- visitante: `401 Unauthorized`;
- artista: `403 Forbidden`;
- outro contratante: `403 Forbidden`;
- proprietário com vaga `ABERTA`/`PAUSADA`: `204 No Content`;
- vaga inexistente: `404 Not Found`;
- estado incompatível: `422 Unprocessable Entity`.

O usuário é derivado do JWT e carregado do estado persistido. A propriedade é comparada com `vaga.contratante.usuarioId`; o endpoint não recebe `contratanteId` ou `usuarioId`.

O cenário IDOR confirma que o contratante B não altera vaga, candidatura ou log pertencentes ao contratante A.

## 13. Banco

**Banco alterado: NÃO**

> Nenhuma tabela, coluna, tipo, enum, constraint, índice, migration ou script SQL foi alterado.

`ddl-auto=validate` permanece ativo. A comparação SHA-256 antes/depois confirmou os arquivos oficiais e o schema de teste idênticos. A constraint usada no teste de rollback existe somente durante a execução no container descartável.

## 14. Frontend

**Frontend alterado: NÃO**

> Nenhum arquivo HTML, CSS ou JavaScript foi alterado.

O frontend atual já exige a digitação de `DELETAR` antes de chamar a rota. Essa confirmação visual foi preservada, mas não é validação backend: uma chamada direta autenticada ao endpoint continua sem body de confirmação/motivo.

## 15. Testes

### Baseline

Executado em `backend`:

```powershell
.\mvnw.cmd -o '-Dmaven.repo.local=C:\Users\masca\.m2\repository' '-Dspring.jpa.show-sql=false' test
```

- total: 175;
- aprovados: 175;
- falhas: 0;
- erros: 0;
- ignorados: 0;
- resultado: `BUILD SUCCESS`.

### Novos RF25

Classe: `VagaCancelamentoRf25IntegrationTest`

- métodos: 13;
- execuções: 16;
- aprovadas: 16;
- falhas: 0;
- erros: 0;
- ignoradas: 0;
- resultado direcionado: `BUILD SUCCESS`.

Cenários: `401`, artista, IDOR, `404`, aberta, pausada, encerrada, recancelamento, soft delete, log completo, todos os status de candidatura, rollback PostgreSQL e regressões RF03/RF05/RF06/RF31.

### Final

- total: 191;
- aprovados: 191;
- falhas: 0;
- erros: 0;
- ignorados: 0;
- PostgreSQL 18.4/Testcontainers preservado;
- resultado: **`BUILD SUCCESS`**.

## 16. Regressões

| Área | Execuções finais | Resultado |
| --- | ---: | --- |
| RF03 | 27 | Aprovado; cancelada sai do feed e permanece no histórico do candidato. |
| RF04 | 19 | Aprovado. |
| RF05 | 21 | Aprovado; dono/candidato acessam, terceiro recebe `404`. |
| RF06 | 20 | Aprovado; nova candidatura em cancelada retorna `422`. |
| RF07 | 15 | Aprovado; edição não redefine status. |
| RF08 | 23 | Aprovado. |
| RF31 | 24 | Aprovado; as três ações retornam `422` após cancelamento. |
| RF25 | 16 | Aprovado. |
| Autenticação/JWT | Cobertura integrada | Aprovado. |
| Suíte completa | 191 | `BUILD SUCCESS`. |

## 17. Notificações

Não foi encontrada infraestrutura consolidada de notificação neste fluxo. WebSocket, serviço paralelo ou implementação parcial do RF23 não foram criados.

A notificação de todos os candidatos permanece como **integração futura com RF23**.

## 18. Conflitos e decisões

| Conflito | Impacto | Decisão |
| --- | --- | --- |
| Documento antigo: somente `ABERTA`; consolidado: `ABERTA` ou `PAUSADA`. | Divergência na máquina de estados. | Aplicar a decisão consolidada. |
| Motivo obrigatório, mas nenhuma coluna legítima. | Não é possível persistir motivo sem mudar banco. | Não inventar armazenamento; registrar bloqueio e classificar como parcial. |
| Confirmação exigida, mas API recebe `DELETE` sem body. | Chamada direta pode contornar o modal. | Preservar frontend/compatibilidade; registrar necessidade de contrato coordenado futuro. |
| “Todas” candidaturas versus estados históricos finais. | Estados anteriores são substituídos. | Seguir o requisito atual e comportamento consolidado; preservar registros/IDs/vínculos. |
| `DELETE` pode sugerir remoção física. | Risco de interpretação incorreta. | Preservar rota, documentando soft delete de negócio. |
| Falha de log injetada gera `409`, não `500`. | Handler global classifica integridade como conflito. | Preservar padrão global; rollback foi integralmente comprovado. |
| Ausência de locking/versionamento. | Cancelamentos simultâneos podem competir. | Não alterar banco; registrar risco futuro. |

## 19. Pendências

- decidir e autorizar estrutura oficial para persistir motivo de cancelamento;
- coordenar um contrato backend de confirmação/motivo com a integração frontend, sem descartar dados;
- implementar notificações somente no RF23;
- avaliar locking/versionamento em evolução própria;
- revisar em requisito futuro se o status anterior de candidaturas finais deve possuir histórico separado.

## 20. Conclusão

**RF25 PARCIAL.**

O núcleo de soft delete está operacional e comprovado: somente o proprietário cancela, `ABERTA`/`PAUSADA` viram `CANCELADA`, estados finais retornam `422`, vaga/candidaturas permanecem, todos os candidatos recebem `CANCELADA_POR_VAGA`, o log é criado e o rollback é integral. A regressão terminou com **191/191 testes aprovados** e **`BUILD SUCCESS`**, sem alterar banco ou frontend.

O requisito não pode ser classificado como concluído porque o banco oficial não possui local para persistir o motivo obrigatório e a confirmação é aplicada somente no frontend, não pelo contrato backend. Esses pontos não foram ocultados nem simulados.

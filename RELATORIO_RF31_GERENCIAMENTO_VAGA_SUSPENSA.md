# Relatório Técnico — RF31: Gerenciamento de Vaga Suspensa

## 1. Objetivo e escopo

Auditar e concluir no backend o gerenciamento manual do ciclo de estado da vaga: suspensão, reabertura e encerramento definitivo. A entrega preserva banco, frontend e os RFs já concluídos. Cancelamento, notificações e encerramento automático por data-limite não fazem parte desta implementação.

## 2. Estado inicial

### Existente

- entidade `Vaga` com propriedade persistida em `PerfilContratante` e campo `status`;
- enum `StatusVaga` com `ABERTA`, `PAUSADA`, `ENCERRADA` e `CANCELADA`;
- campo `dataLimiteCandidatura` no modelo e no banco;
- autenticação JWT e resolução do usuário autenticado sem IDs de identidade fornecidos pelo cliente;
- helpers de Service para exigir papel `CONTRATANTE` e propriedade da vaga;
- exceção global `UnprocessableEntityException`, já mapeada para HTTP `422`;
- RF03 filtrando o feed por `ABERTA`;
- RF04 publicando novas vagas como `ABERTA`;
- RF05 restringindo vagas não abertas ao proprietário ou artista já candidato;
- RF06 aceitando nova candidatura somente em vaga `ABERTA`;
- RF07 preservando o status ao editar detalhes;
- operação `DELETE /api/vagas/{id}` com cancelamento preexistente, pertencente ao escopo parcial do RF25.

### Correto

- `VagaAtualizacaoRequest` não possui campo `status`;
- `VagaService.atualizar` salva novamente o estado persistido após editar os detalhes;
- propriedade é comparada com o usuário do JWT;
- listagem, detalhes e candidaturas já reagiam corretamente aos valores do status;
- `ddl-auto=validate` estava ativo.

### Parcial

- toda a infraestrutura de autenticação, autorização, persistência e erro `422` podia ser reutilizada, mas ainda não havia máquina de estados do RF31;
- `dataLimiteCandidatura` existe, porém não há agendamento ou serviço de encerramento automático;
- há cancelamento preexistente no `DELETE`, mas ele não foi tratado, ampliado ou reescrito nesta tarefa.

### Ausente

- endpoint específico para solicitar `SUSPENDER`, `REABRIR` ou `ENCERRAR`;
- validação das quatro transições consolidadas;
- resposta de negócio para transições inválidas;
- testes dedicados ao RF31;
- mecanismo de locking/versionamento para mudanças simultâneas de estado;
- notificações de candidatos.

O baseline real, executado antes de editar, foi de **151 testes aprovados, 0 falhas, 0 erros e 0 ignorados**, com `BUILD SUCCESS`.

## 3. Auditoria por camada

| Camada | Estado inicial | Resultado |
| --- | --- | --- |
| Entity | Correto para o RF31 manual | `Vaga.status` e propriedade foram reutilizados; nenhuma entidade foi alterada. |
| Enum | Correto para o banco atual | Quatro estados confirmados; `RASCUNHO` não existe e não foi criado. |
| DTO | Ausente para RF31 | Criado DTO mínimo de ação; o DTO RF07 continua sem status arbitrário. |
| Repository | Suficiente | `findById` e persistência JPA atendem ao fluxo; nenhuma consulta ou repository novo foi necessário. |
| Service | Ausente para RF31 | Implementadas propriedade, máquina de estados e persistência em método transacional. |
| Controller | Ausente para RF31 | Criado somente `PATCH /api/vagas/{id}/status`; rotas anteriores foram preservadas. |
| Segurança | Parcialmente pronta | JWT/papel/propriedade já existiam; foram aplicados e testados no novo fluxo, inclusive IDOR. |
| Testes | Ausente para RF31 | Criada suíte dedicada com 16 métodos e 24 execuções. |

## 4. Máquina de estados

Regra final implementada:

```text
ABERTA → PAUSADA
PAUSADA → ABERTA
ABERTA → ENCERRADA
PAUSADA → ENCERRADA
```

- `ENCERRADA` é final no RF31;
- `CANCELADA` é final no RF31;
- nenhuma outra transição é aceita;
- repetição da mesma ação ou ação incompatível retorna `422 Unprocessable Entity`;
- ação desconhecida, inclusive `CANCELAR`, retorna `400 Bad Request`.

Contrato criado:

```http
PATCH /api/vagas/{id}/status
Content-Type: application/json

{"acao":"SUSPENDER"}
```

As ações aceitas são `SUSPENDER`, `REABRIR` e `ENCERRAR`. O response reutiliza `VagaResponse` e informa o novo `status`.

## 5. Conflito documental

A descrição antiga mencionava somente `PAUSADA → ENCERRADA`, enquanto a decisão consolidada posterior permite também `ABERTA → ENCERRADA`.

Foi aplicada a decisão consolidada, por ser explicitamente indicada como a regra mais recente do RF31. Assim, tanto vaga aberta quanto pausada podem ser encerradas. A decisão não altera o sentido de `ENCERRADA` como estado final.

## 6. Alterações

| Arquivo | Alteração | Motivo |
| --- | --- | --- |
| `backend/src/main/java/com/portifolio/dto/VagaStatusAcaoRequest.java` | Novo DTO contendo somente `acao`, obrigatória. | Impedir envio/salvamento de status arbitrário. |
| `backend/src/main/java/com/portifolio/service/VagaService.java` | Método transacional `gerenciarStatus`, matriz de transições e mensagens `422`. | Centralizar autenticação contextual, papel, propriedade e regra de negócio. |
| `backend/src/main/java/com/portifolio/controller/VagaController.java` | Endpoint `PATCH /api/vagas/{id}/status`. | Expor operação específica sem duplicar ou alterar `PUT`. |
| `backend/src/main/java/com/portifolio/config/SecurityConfig.java` | Método `PATCH` incluído na configuração CORS. | Permitir integração web futura com a nova operação protegida. |
| `backend/src/test/java/com/portifolio/controller/VagaGerenciamentoRf31IntegrationTest.java` | Nova suíte PostgreSQL/Testcontainers. | Comprovar regras, segurança, histórico e regressões. |

Nenhum repository, entity, enum persistido, endpoint de cancelamento ou DTO do RF07 foi alterado.

## 7. Segurança e propriedade

- sem JWT: `401 Unauthorized`, bloqueado pelo Spring Security;
- usuário `ARTISTA`: `403 Forbidden`;
- outro `CONTRATANTE`: `403 Forbidden`;
- vaga inexistente: `404 Not Found`;
- transição semanticamente inválida: `422 Unprocessable Entity`;
- ação ou formato inválido: `400 Bad Request`;
- proprietário: pode executar somente as transições autorizadas.

A operação recebe apenas `vagaId` como identificador do recurso e `acao` como comando. A identidade é obtida do JWT, o papel vem do usuário persistido e a propriedade é validada comparando o usuário autenticado ao contratante persistido na vaga. Não há `contratanteId` ou `usuarioId` no novo payload.

O cenário IDOR testa o contratante B tentando executar as três ações na vaga do contratante A; todas retornam `403` e o status permanece inalterado.

## 8. Candidaturas existentes

O método RF31 modifica exclusivamente `Vaga.status`. Ele não consulta nem salva candidaturas.

O teste dedicado cria duas candidaturas, registra IDs/status e percorre:

```text
ABERTA → PAUSADA → ABERTA → ENCERRADA
```

Após cada transição, a quantidade, os IDs e os status `PENDENTE`/`EM_ANALISE` permanecem iguais. Nenhuma candidatura recebe `CANCELADA_POR_VAGA`.

A regressão RF06 também prova:

- `PAUSADA`: nova candidatura retorna `422`;
- após `REABRIR`: nova candidatura válida retorna `201` e `PENDENTE`;
- `ENCERRADA`: nova candidatura retorna `422`.

## 9. Relação com RF25

> O cancelamento de vagas não foi implementado nesta tarefa e permanece reservado ao RF25.

O cancelamento preexistente em `DELETE /api/vagas/{id}` foi preservado sem alterações. A nova operação não aceita `CANCELAR`: a tentativa retorna `400` e mantém o status da vaga. Não foram adicionados motivo, log, notificação nem `CANCELADA_POR_VAGA` ao RF31.

## 10. Banco

**Banco alterado: NÃO**

> Nenhuma tabela, coluna, tipo, enum, constraint, índice, migration ou script SQL foi alterado.

`ddl-auto=validate` permanece ativo. A comparação SHA-256 antes/depois confirmou os scripts oficiais, migrations e schema de teste idênticos.

## 11. Frontend

**Frontend alterado: NÃO**

> Nenhum arquivo HTML, CSS ou JavaScript foi alterado.

Todos os 79 arquivos protegidos — banco, frontend, schema de teste e configuração de persistência — permaneceram idênticos ao baseline. A criação dos controles visuais é integração futura.

## 12. Testes

### Baseline

Executado em `backend`:

```powershell
.\mvnw.cmd -o '-Dmaven.repo.local=C:\Users\masca\.m2\repository' '-Dspring.jpa.show-sql=false' test
```

- total: 151;
- aprovados: 151;
- falhas: 0;
- erros: 0;
- ignorados: 0;
- resultado: `BUILD SUCCESS`.

### Novos RF31

Classe: `VagaGerenciamentoRf31IntegrationTest`

- métodos: 16;
- execuções: 24;
- aprovadas: 24;
- falhas: 0;
- erros: 0;
- ignoradas: 0;
- resultado direcionado: `BUILD SUCCESS`.

Cenários: visitante, artista, IDOR nas três ações, `404`, suspensão, reabertura, encerramento a partir de dois estados, repetição/transições inválidas, estados finais, ação `CANCELAR`, payload sem ação, preservação de candidaturas e regressões RF03/RF05/RF06.

### Final

- total: 175;
- aprovados: 175;
- falhas: 0;
- erros: 0;
- ignorados: 0;
- PostgreSQL/Testcontainers mantido, com PostgreSQL 18.4;
- resultado: **`BUILD SUCCESS`**.

## 13. Regressões

| Área | Execuções finais | Resultado |
| --- | ---: | --- |
| RF03 | 27 | Aprovado; feed reage ao ciclo completo. |
| RF04 | 19 | Aprovado; publicação continua iniciando em `ABERTA`. |
| RF05 | 21 | Aprovado; visibilidade/histórico também cobertos na suíte RF31. |
| RF06 | 20 | Aprovado; bloqueio fora de `ABERTA` também coberto na suíte RF31. |
| RF07 | 15 | Aprovado; edição continua sem controlar status. |
| RF08 | 23 | Aprovado. |
| Autenticação/JWT | Cobertura integrada | Aprovado. |
| RF31 | 24 | Aprovado. |
| Suíte completa | 175 | `BUILD SUCCESS`. |

## 14. Conflitos e decisões

| Ponto | Evidência/impacto | Decisão |
| --- | --- | --- |
| `RASCUNHO` | Ausente no enum Java e no enum PostgreSQL atual. | Não criar enum, migration ou comportamento artificial. |
| `ABERTA → ENCERRADA` | Descrição antiga diverge da decisão consolidada. | Implementar a decisão consolidada, permitindo a transição. |
| Data-limite | `data_limite_candidatura` existe, mas não há job/agendamento de encerramento. | Não implementar automação; manter como capacidade futura. |
| Notificações | Não há infraestrutura consolidada no fluxo e o tema pertence ao RF23. | Não implementar nesta tarefa. |
| Cancelamento | Há implementação preexistente no `DELETE`, pertencente ao RF25. | Preservar sem alterar; rejeitar `CANCELAR` no endpoint RF31. |
| Concorrência | Não existe `@Version`, locking otimista ou pessimista na vaga. Duas requisições simultâneas podem ler o mesmo estado antes do commit. | Não alterar banco nem introduzir refatoração estrutural; registrar o risco para evolução futura. |
| Frontend/CORS | Frontend ainda não possui controles; chamada futura usa `PATCH`. | Preservar arquivos visuais e habilitar apenas `PATCH` no CORS backend. |

## 15. Pendências

- integrar o frontend futuro ao `PATCH /api/vagas/{id}/status`;
- tratar o cancelamento completo exclusivamente no RF25;
- definir no RF23 se e como candidatos serão notificados;
- definir, em requisito futuro, o encerramento automático baseado em `dataLimiteCandidatura`;
- avaliar controle de concorrência em evolução própria, pois o modelo atual não possui versionamento/locking;
- adicionar `RASCUNHO` somente se o banco oficial e o requisito futuro forem alterados de forma coordenada.

## 16. Conclusão

**RF31 CONCLUÍDO NO BACKEND.**

Há evidência automatizada para JWT, papel, propriedade, IDOR, `401/403/404/422`, quatro transições válidas, estados finais, rejeição do cancelamento, preservação de candidaturas e regressões RF03/RF04/RF05/RF06/RF07/RF08. A operação é transacional, a regressão terminou com **175/175 testes aprovados** e **`BUILD SUCCESS`**, e banco/frontend permaneceram inalterados.

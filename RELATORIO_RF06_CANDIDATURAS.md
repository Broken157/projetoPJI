# Relatório de Finalização do RF06 — Candidaturas a Vagas

Data da execução: 15/08/2026

## 1. Estado antes da tarefa

A adaptação inicial ao banco oficial já estava funcional. A criação de candidatura já obtinha a identidade pelo JWT, exigia usuário `ARTISTA`, perfil completo, vaga `ABERTA`, bloqueava duplicidade e sempre persistia o estado inicial `PENDENTE` com data atual. Os limites de 2000 caracteres para mensagem e 255 para link também já estavam no DTO.

O enum Java já refletia o enum PostgreSQL oficial:

- `PENDENTE` → `pendente`;
- `EM_ANALISE` → `em analise`;
- `APROVADO` → `aprovado`;
- `REJEITADO` → `rejeitado`;
- `RETIRADA` → `retirada`;
- `CANCELADA_POR_VAGA` → `cancelada_por_vaga`.

Assim, a decisão documental `ACEITA` foi corretamente interpretada como `APROVADO`, e `REJEITADA` como `REJEITADO`, sem criar valores e sem alterar o banco.

### Auditoria inicial dos endpoints

| Método | Rota | Comportamento antes | Autorização/propriedade antes | Comportamento exigido pelo RF06 |
|---|---|---|---|---|
| `GET` | `/api/candidaturas/minhas-vagas` | Listava candidaturas recebidas pelo contratante autenticado. | Já exigia `CONTRATANTE` e filtrava pelas próprias vagas. | Preservar. |
| `GET` | `/api/candidaturas` | Executava `findAll()` e retornava todas as candidaturas. | Qualquer usuário autenticado via dados de todos. | Retornar somente recursos acessíveis ao usuário atual. |
| `GET` | `/api/candidaturas/{id}` | Retornava qualquer candidatura existente. | Não validava artista nem proprietário da vaga. | Permitir apenas artista candidato ou contratante dono da vaga. |
| `POST` | `/api/candidaturas` | Criava candidatura pendente com as principais validações do RF06. | Já derivava o artista do JWT e rejeitava representação de terceiro. | Preservar e cobrir integralmente com testes. |
| `PUT` | `/api/candidaturas/{id}` | Podia trocar vaga, artista, conteúdo e status diretamente a partir do payload. | Qualquer autenticado podia modificar qualquer candidatura. | Tornar vínculos imutáveis, validar proprietário, papel e transição. |
| `DELETE` | `/api/candidaturas/{id}` | Excluía fisicamente o registro. | Qualquer autenticado podia excluir qualquer candidatura. | Manter a rota por compatibilidade, mas realizar retirada lógica segura. |

Antes desta tarefa, a suíte padrão possuía 27 testes aprovados. Parte da criação e a propagação de cancelamento de vaga já estavam cobertas, mas consulta privada, análise, estados terminais e retirada não tinham cobertura suficiente.

## 2. Problemas encontrados

### Autorização

1. `GET /api/candidaturas`, `GET /api/candidaturas/{id}`, `PUT` e `DELETE` dependiam somente da autenticação global; não havia controle de papel no módulo.
2. Um artista podia enviar estados de decisão, inclusive aprovação ou rejeição.
3. Um contratante não proprietário podia manipular candidaturas de vagas alheias.

### Propriedade e privacidade

1. A listagem genérica expunha candidaturas de todos os artistas e contratantes.
2. A consulta por ID expunha uma candidatura privada a qualquer usuário autenticado.
3. Atualização e exclusão não percorriam a relação `candidatura → vaga → contratante → usuário autenticado`.
4. A atualização permitia trocar `vaga_id` e `artista_id`, violando a identidade histórica da candidatura.

### Transições

1. O status recebido no payload era aplicado diretamente com `setStatus()`.
2. Não existia máquina de estados explícita.
3. Estados terminais podiam ser revertidos.
4. `CANCELADA_POR_VAGA` podia ser alterado manualmente.

### Validação e segurança

1. `DELETE` removia o histórico apesar da existência do estado `RETIRADA`.
2. O conteúdo original da candidatura podia ser substituído pelo contratante ou por terceiro junto com a alteração de status.
3. A autorização não era executada antes das validações do payload, o que poderia produzir respostas diferentes para recursos de terceiros.

## 3. Alterações realizadas

| Arquivo | Alteração | Motivo | Regra do RF06 atendida |
|---|---|---|---|
| `backend/src/main/java/com/portifolio/service/CandidaturaService.java` | A listagem genérica passou a ser filtrada por ator: próprias candidaturas para artista e candidaturas das próprias vagas para contratante. | Eliminar exposição global sem remover a rota existente. | Consulta e propriedade. |
| `backend/src/main/java/com/portifolio/service/CandidaturaService.java` | A consulta por ID passou a exigir artista proprietário ou contratante proprietário, retornando 404 para recurso privado inacessível. | Evitar enumeração e acesso a candidaturas alheias. | Autorização e privacidade. |
| `backend/src/main/java/com/portifolio/service/CandidaturaService.java` | Foi criada regra explícita de transição conforme papel e estado atual. | Impedir `status do payload → save()` e regressões arbitrárias. | Análise, resultado e estados terminais. |
| `backend/src/main/java/com/portifolio/service/CandidaturaService.java` | Vaga e artista tornaram-se imutáveis após a criação; mensagem e link não são regravados pelo endpoint de estado. | Preservar identidade, autoria e histórico. | Propriedade e integridade. |
| `backend/src/main/java/com/portifolio/service/CandidaturaService.java` | `DELETE` passou a realizar `RETIRADA` lógica somente pelo próprio artista em estado permitido. | Preservar o contrato da rota e o histórico oficial. | Retirada pelo artista. |
| `backend/src/main/java/com/portifolio/service/CandidaturaService.java` | A autorização passou a ocorrer antes da validação de vínculos do payload. | Evitar inferência de dados privados por diferença de erro. | Segurança de acesso. |
| `backend/src/test/java/com/portifolio/controller/CandidaturaControllerRf06IntegrationTest.java` | Adicionada suíte de integração com 20 casos executados em PostgreSQL 18 e schema oficial de teste. | Cobrir criação, consulta, análise, retirada, terminais, propriedade e HTTP. | Testes obrigatórios do RF06. |

O `CandidaturaController` e suas rotas foram preservados. O `CandidaturaRequest` e o formato de `CandidaturaResponse` também foram mantidos para compatibilidade.

## 4. Matriz final de permissões

| Operação | ARTISTA | CONTRATANTE | Regra |
|---|---|---|---|
| Criar candidatura | Sim, somente para si, com perfil completo e vaga aberta. | Não; HTTP 403. | Identidade vem do JWT; `artistaId` não define propriedade. |
| Listar em `GET /api/candidaturas` | Somente as próprias. | Somente as recebidas nas próprias vagas. | Nunca existe listagem global. |
| Listar em `/minhas-vagas` | Não; HTTP 403. | Somente as candidaturas de vagas próprias. | Rota de dashboard do contratante preservada. |
| Consultar detalhe | Somente candidatura própria. | Somente candidatura de vaga própria. | Recurso alheio é tratado como não acessível, HTTP 404. |
| Registrar análise/resultado | Não; HTTP 403. | Sim, somente em vaga própria e por transição permitida. | `APROVADO` e `REJEITADO` são os nomes oficiais. |
| Retirar via `PUT` com `RETIRADA` | Sim, somente a própria e em estado retirável. | Não. | Apenas `PENDENTE` ou `EM_ANALISE`. |
| Retirar via `DELETE` | Sim, com as mesmas regras da retirada via `PUT`. | Não; HTTP 403. | Retirada lógica; resposta 204. |
| Excluir fisicamente | Não. | Não. | Histórico preservado. |
| Alterar vaga/artista da candidatura | Não. | Não. | Vínculos imutáveis; HTTP 422. |
| Reverter estado terminal | Não. | Não. | HTTP 422. |

## 5. Matriz final de estados

| Estado atual | Próximo estado permitido | Ator autorizado | Observação |
|---|---|---|---|
| `PENDENTE` | `EM_ANALISE` | Contratante proprietário | Registra início da análise. |
| `PENDENTE` | `APROVADO` | Contratante proprietário | Resultado direto permitido. |
| `PENDENTE` | `REJEITADO` | Contratante proprietário | Resultado direto permitido. |
| `PENDENTE` | `RETIRADA` | Artista proprietário | Pode ocorrer por `PUT` ou `DELETE` lógico. |
| `EM_ANALISE` | `APROVADO` | Contratante proprietário | Estado final. |
| `EM_ANALISE` | `REJEITADO` | Contratante proprietário | Estado final. |
| `EM_ANALISE` | `RETIRADA` | Artista proprietário | Permitida porque ainda não foi aprovada. |
| `APROVADO` | Nenhum | Nenhum | Final; não pode ser retirado nem revertido. |
| `REJEITADO` | Nenhum | Nenhum | Final; interpretação conservadora para não reabrir decisão concluída. |
| `RETIRADA` | Nenhum | Nenhum | Final; preserva desistência histórica. |
| `CANCELADA_POR_VAGA` | Nenhum | Nenhum | Final e definido exclusivamente pelo cancelamento da vaga. |

Transições para o mesmo estado também são recusadas como ausência de transição válida. Toda combinação não listada retorna HTTP 422, exceto tentativa de decisão por artista, que retorna HTTP 403 por falta de permissão.

## 6. Frontend

Nenhuma alteração no frontend foi realizada.

As rotas existentes, os nomes JSON e o retorno em lista usado por `frontend/public/js/main.js` foram preservados. O dashboard do contratante continua consumindo `GET /api/candidaturas/minhas-vagas`.

## 7. Banco

Nenhuma alteração no banco de dados foi realizada.

- Nenhuma tabela, coluna, enum, constraint, relacionamento, índice, migration ou script SQL foi editado.
- O backend respeita `APROVADO`/`REJEITADO`, conforme o enum oficial.
- O Hibernate permaneceu com `spring.jpa.hibernate.ddl-auto=validate`.
- Não foi usado `ddl-auto=create`.

## 8. Testes

### Suíte automatizada

- Comando: `mvn test`.
- Resultado: `BUILD SUCCESS`.
- Total: **47 testes**.
- Aprovados: **47**.
- Falhas: **0**.
- Erros: **0**.
- Ignorados: **0**.

Distribuição:

- 20 testes de integração específicos do RF06, adicionados nesta tarefa;
- 18 testes de integração de vagas, perfis, candidaturas e cancelamento;
- 8 testes unitários de normalização de paginação de vagas;
- 1 teste de integração HTTP de cadastro, login, usuário e exclusão de conta.

Os 20 novos casos cobrem:

- artista completo, data real e status inicial forçado para `PENDENTE`;
- perfil incompleto 422;
- contratante tentando candidatar-se 403;
- vaga inexistente 404;
- vagas `PAUSADA`, `ENCERRADA` e `CANCELADA` 422;
- duplicidade 409;
- outro `artistaId` 403 sem criação de registro;
- limites de mensagem e link;
- consultas isoladas de artista e contratante;
- análise e resultado pelo proprietário;
- bloqueio de outro contratante;
- bloqueio de aprovação/rejeição pelo artista;
- transição inválida e não reversão de terminal;
- retirada lógica própria;
- bloqueio de retirada por outro artista;
- bloqueio de retirada após aprovação;
- imutabilidade de `CANCELADA_POR_VAGA`.

### PostgreSQL e smoke test

Os testes de integração usaram PostgreSQL 18.4 via Testcontainers, executaram `backend/src/test/resources/db/schema-test.sql` e iniciaram o Hibernate com `ddl-auto=validate`.

Também foi criado um PostgreSQL 18 temporário isolado na porta 5435, inicializado nesta ordem:

1. `database/sos_artistas.sql`;
2. `database/migration_rf03.sql`.

O backend iniciou na porta 8080 contra esse banco com validação do schema. O smoke test HTTP confirmou:

- cadastro e login de dois contratantes e dois artistas;
- conclusão do perfil do artista;
- publicação de três vagas;
- candidatura criada como `PENDENTE` apesar de o cliente enviar `APROVADO`;
- listagem do artista e dashboard do contratante isolados por propriedade;
- detalhe privado inacessível a outro artista e outro contratante;
- outro contratante bloqueado na análise;
- artista bloqueado ao tentar aprovar;
- `PENDENTE → EM_ANALISE` pelo contratante proprietário;
- retirada lógica `EM_ANALISE → RETIRADA` pelo próprio artista;
- candidatura retirada preservada e sem reversão;
- aprovação pelo contratante e bloqueio de retirada posterior;
- cancelamento da vaga propagando `CANCELADA_POR_VAGA`;
- bloqueio de alteração manual após cancelamento;
- contratante bloqueado ao tentar criar candidatura.

Resultado do smoke test: **aprovado**.

O backend e o contêiner temporários foram encerrados e removidos. As portas 8080 e 5435 foram liberadas. Os contêineres preexistentes `palco-postgres` e `portifoliodb` não foram alterados.

## 9. Pendências

1. **Notificações dependentes do RF23:** o schema oficial contém `notificacoes`, mas o backend ainda não possui Entity, Repository ou serviço de notificações, e as regras completas de mensagem, preferência e entrega pertencem ao RF23. Não foi criado sistema paralelo nem implementado o RF23 parcialmente. As notificações de mudança de estado permanecem como dependência explícita.
2. **Paginação das candidaturas:** a segurança das listagens foi concluída, mas elas ainda retornam `List` para preservar o contrato atual. A paginação obrigatória definida pelo RNF de grandes listagens deve ser implementada em tarefa própria, com decisão de compatibilidade para o frontend.
3. **DTO específico de transição:** o `PUT` foi preservado com o payload legado completo. Uma evolução futura pode aceitar um DTO contendo somente `status`, desde que o contrato com consumidores seja versionado ou confirmado.
4. **Interpretação conservadora consolidada:** `REJEITADO` foi tratado como terminal. A documentação diz que o artista pode retirar enquanto não estiver aceito, mas também orienta considerar rejeição como estado final. A solução prioriza ausência de regressões e preservação histórica.

## 10. REGISTRO PARA RELATÓRIO SEMANAL

### Data

15/08/2026.

### Objetivo da tarefa

Finalizar e tornar seguro o backend do RF06, com autorização, propriedade, transições de estado, retirada lógica e testes.

### Backend alterado

- `backend/src/main/java/com/portifolio/service/CandidaturaService.java`: isolamento de consultas, autorização por propriedade, máquina de estados, vínculos imutáveis e retirada lógica.
- `backend/src/test/java/com/portifolio/controller/CandidaturaControllerRf06IntegrationTest.java`: 20 testes de integração RF06.

### Frontend alterado

Não. Nenhum arquivo.

### Banco alterado

Não. Nenhum arquivo.

### Funcionalidades concluídas ou avançadas

- criação segura preservada e integralmente testada;
- consulta privada por artista e contratante;
- análise/resultado somente pelo dono da vaga;
- máquina de estados explícita;
- retirada lógica pelo próprio artista;
- proteção de estados terminais e de `CANCELADA_POR_VAGA`;
- dashboard e cancelamento de vaga validados em regressão e smoke test.

### Testes

47 aprovados, 0 falhas, 0 erros e 0 ignorados. Smoke test HTTP aprovado contra PostgreSQL 18.4 inicializado pelos scripts oficiais.

### Pendências

- integrar notificações quando o RF23 for implementado;
- paginar listagens de candidaturas em tarefa específica;
- avaliar DTO dedicado de transição sem quebrar consumidores;
- manter documentada a decisão de `REJEITADO` como estado terminal.

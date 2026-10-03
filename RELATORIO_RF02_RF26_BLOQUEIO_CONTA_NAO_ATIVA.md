# RF02/RF26 — bloqueio de acesso normal para conta não ativa

Data: **28/09/2026**. Checkout: `projetoPJI-react00b-baseline`, branch `integracao-recuperada-2026-09-15`, HEAD inicial `a6164e38b177c765638d61eb0d68c4b46f79f052`. A árvore já continha centenas de alterações locais; foram preservadas. Não houve commit nem push.

## 1. Objetivo

Impedir que uma conta cujo `status_conta` persistido não seja `ATIVA` crie ou use acesso normal por login convencional, refresh, JWT antigo e canais em tempo real, mantendo o acesso de contas ativas.

## 2. RF/RNF trabalhados

**RF02:** login, estado da conta e respostas de credenciais. **RF26:** bloqueio enquanto a verificação de e-mail não levou à ativação; o fluxo completo de link, reenvio e ativação não integra esta entrega. **RNF06:** pendência de consentimento de menor não autoriza acesso. **RNF08:** identidade e estado conferidos no backend. **RNF10:** testes focados e suíte completa em PostgreSQL/Testcontainers.

## 3. Diagnóstico confirmado

`AuthService.cadastrar` criava conta convencional em `PENDENTE_VERIFICACAO_EMAIL`; `GoogleAccountAccessPolicy.acessoNormalPermitido` aceitava todo usuário sem `googleId`, independentemente do estado. Essa política era chamada por login, refresh e `UserDetailsServiceImpl`, responsável por JWT HTTP e STOMP CONNECT. O teste antigo aprovava login da conta recém-cadastrada. SEND/SUBSCRIBE em STOMP revalidavam apenas o JWT; as entregas a conexões WebSocket/SSE já abertas não consultavam o estado do destinatário.

O enum atual contém `PENDENTE_VERIFICACAO_EMAIL`, `PENDENTE_TIPO_PERFIL`, `PENDENTE_CONSENTIMENTO`, `ATIVA` e `BLOQUEADA`. **`INATIVA` não existe nele**; a condição `status_conta == ATIVA` também negará qualquer futuro estado diferente.

## 4. Arquivos alterados

**Produção, somente backend:** `backend/src/main/java/com/portifolio/security/GoogleAccountAccessPolicy.java`, `security/UserDetailsServiceImpl.java`, `service/AuthService.java`, `config/StompJwtChannelInterceptor.java`, `realtime/ChatRealtimeService.java` e `realtime/NotificacaoRealtimeService.java`.

**Testes de contrato e segurança:** `controller/AuthControllerRf01Rf02IntegrationTest.java`, `security/JwtAuthenticationIntegrationTest.java`, `security/GenericEndpointsSecurityIntegrationTest.java`, `config/StompJwtChannelInterceptorTest.java` e o novo `realtime/RealtimeAccountAccessTest.java` (todos sob `backend/src/test/java/com/portifolio/`).

**Fixtures e expectativas de regressão:** `UsuarioControllerIT.java`; `controller/{CandidaturaControllerRf06IntegrationTest,ChatRf24IntegrationTest,DenunciaRf14Rf18IntegrationTest,NotificacaoRf23IntegrationTest,PerfilEdicaoRf08IntegrationTest,PortfolioRf16IntegrationTest,RecuperacaoSenhaRf09IntegrationTest,SalvoRf19IntegrationTest,TalentoRf13IntegrationTest,VagaCancelamentoRf25IntegrationTest,VagaControllerRf03IntegrationTest,VagaDetalhesRf05IntegrationTest,VagaEdicaoRf07IntegrationTest,VagaGerenciamentoRf31IntegrationTest,VagaPublicacaoRf04IntegrationTest}.java`; `service/VagaPrazoRf23Rf06IntegrationTest.java`; `realtime/{ChatWebSocketRf24IntegrationTest,NotificacaoSsePoolIntegrationTest,NotificacaoWebSocketRf23IntegrationTest}.java`. Fixtures que simulam atores aptos passaram a criar contas `ATIVA` com e-mail verificado; expectativas de token antigo após desativação passaram a 401.

`AuthService.java` e alguns testes já tinham alterações locais antes desta tarefa; elas foram mantidas. A atualização exigida por `AGENTS.md` executou `graphify update .` e regenerou artefatos em `graphify-out/`.

## 5. Comportamento antes/depois

| Cenário | Antes | Depois |
|---|---|---|
| Cadastro convencional pendente, senha correta | Login emitia JWT; `rememberMe` podia emitir refresh | 403, orientação de confirmar e-mail, nenhum token/cookie novo |
| Conta pendente de consentimento ou bloqueada | Acesso convencional podia ser liberado | Login e refresh recusados |
| JWT emitido enquanto a conta era ativa; estado muda | Usuário convencional continuava autenticando | Reconsulta persistida; endpoint protegido e novo stream SSE retornam 401 |
| STOMP já conectado; estado muda | SEND/SUBSCRIBE checavam apenas assinatura/expiração | Cada SEND/SUBSCRIBE reconsulta identidade e estado persistidos |
| Push para conexão WebSocket/SSE antiga | Entrega sem conferir estado | Chat/notificação só entregam quando destinatário persistido continua apto |
| Conta ativa | Login, JWT, rememberMe, rotação e logout | Preservados |
| Conta provisória Google | Fluxo de conclusão específico | Preservado; sem JWT normal enquanto não apta |

## 6. Autenticação, autorização e estado da conta

Senha errada, e-mail inexistente e conta Google sem senha local recebem **401** com a mesma mensagem genérica: `Email ou senha incorretos.`. Depois de credenciais convencionais válidas, estado não apto recebe **403** pelo `ForbiddenException` global, com orientação para e-mail/consentimento quando cabível. Refresh válido de conta que deixou de ser apta recebe 403; refresh inválido/expirado mantém 401. JWT antigo deixa de criar principal e o endpoint protegido responde **401**. O corpo de erro segue `ErroResposta` do handler global; não foi criada exceção nem forma de erro isolada.

O backend obtém e-mail/ID do JWT assinado e consulta o usuário persistido; STOMP confere também o principal da sessão. O cliente não decide papel ou status. Para push, o destinatário é recarregado por ID e seu e-mail é confrontado antes da entrega. BCrypt, assinatura JWT, hash/rotação de refresh, cookie e CORS foram preservados.

## 7. Testes alterados/adicionados

O teste que esperava login 200/JWT de cadastro pendente foi substituído por 403 sem JWT, cookie ou refresh. Casos adicionais cobrem login `ATIVA`, quatro estados não ativos, refresh, JWT antigo, stream SSE, `rememberMe`/rotação/logout e resposta genérica para senha incorreta, e-mail inexistente e conta Google sem senha local. Testes STOMP verificam revalidação durante conexão existente; `RealtimeAccountAccessTest` verifica que push de notificação/SSE/chat cessa após mudança de status. Fixtures antigos de outras áreas foram atualizados para representar explicitamente contas ativas.

## 8. Testes focados

- Seleção de regressão de 18 classes com PostgreSQL/Testcontainers: **324 testes, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
- Seleção posterior de autenticação, Google, RF09, JWT, segurança genérica, STOMP, WebSocket/SSE e tempo real: **94 testes, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.
- A última asserção de resposta genérica para conta Google sem senha local foi incluída depois da segunda seleção e integra a suíte completa abaixo.

## 9. Resultado de `mvn test`

Execução de `cd backend; .\mvnw.cmd test`, Java 26.0.1, PostgreSQL 18.4 em Testcontainers, `spring.jpa.hibernate.ddl-auto=validate`, sem H2 ou criação automática do schema. Os 53 XMLs do Surefire confirmam o total.

| TOTAL | PASSED | FAILURES | ERRORS | SKIPPED | Maven |
|---:|---:|---:|---:|---:|---|
| **746** | **727** | **2** | **0** | **17** | **BUILD FAILURE** |

As duas falhas reproduzem o baseline documentado em `RELATORIO_ESTABILIZACAO_TESTES_2026-09-27.md` (740/721/2/0/17):

1. `PerfilEdicaoRf08IntegrationTest.contratanteSemBiografiaOuLocalizacaoFicaIncompleto`: o teste espera `false`, recebe `true` para completude do contratante (contrato antigo de RF08).
2. `ManuDumpSchemaIntegrationTest.entityManagerFactorySobeComValidateEQuarentaETresTabelas`: espera nome iniciado por `palco_test_`, mas o banco descartável do container se chama `test`.

Nenhuma falha/erro adicional surgiu; os seis testes extras em relação ao baseline passaram. Os 17 testes condicionais não foram ativados artificialmente. Log desta execução: `%TEMP%\pji-rf02-rf26-final-full-20260928.log`; XMLs em `backend/target/surefire-reports/`.

## 10. Regressões verificadas

Passaram na suíte completa: RF24 Google **18/18**, RF09 recuperação de senha **13/13**, `AuthControllerRf01Rf02IntegrationTest` **16/16** (inclui RF25 rememberMe/refresh/logout), `JwtAuthenticationIntegrationTest` **4/4**, `GenericEndpointsSecurityIntegrationTest` **27/27**, STOMP **9/9**, `RealtimeAccountAccessTest` **2/2**, WebSocket de chat **1/1**, SSE pool **1/1** e WebSocket de notificações **3/3**. A asserção de resposta genérica para conta Google sem senha local passou na suíte completa. Testes de vagas, chat, salvos e demais fixtures afetados passaram após representar contas ativas corretamente.

## 11. Backend alterado

**Sim.** Os seis arquivos de produção da seção 4 aplicam a política de acesso normal a contas convencionais e Google, revalidam sessões STOMP e restringem push para contas não ativas. O texto de resposta do cadastro agora informa a pendência real.

## 12. Frontend alterado

**Não.** O contrato de login/erro e a orientação de pendência foram resolvidos pela API. Uma interface de confirmação de e-mail continua dependente de implementação posterior do RF26.

## 13. Banco alterado

**Não.** Nenhum SQL oficial, tabela, coluna, enum, constraint, índice, trigger, função ou dump foi alterado. Testes escreveram somente em bancos descartáveis do Testcontainers.

## 14. Migrations criadas

**Não.** Nenhuma migration ou DDL foi criado.

## 15. Riscos

- `email_verificado` e `status_conta` são campos distintos. Esta correção usa `ATIVA` como chave de acesso normal; o fluxo completo e as transições consistentes do RF26 ainda não existem. Um registro historicamente inconsistente (`ATIVA` com e-mail não verificado) demanda decisão e saneamento próprios antes de exigir os dois campos, sob risco de bloquear contas legadas.
- Conexões STOMP/SSE antigas não são encerradas proativamente quando o estado muda. Novos frames STOMP são recusados, novos acessos HTTP negados e eventos de chat/notificação não são entregues a conta não apta. Uma desconexão imediata exigiria mecanismo de evento de mudança de estado.
- Push faz leitura adicional do usuário persistido por destinatário. O custo deve ser observado sob carga; não há medida de desempenho nesta tarefa.
- As duas asserções historicamente falhas fora deste escopo permaneceram e impedem declarar a suíte integral verde. Esta entrega não altera RF08 nem a configuração do nome do banco de teste.

## 16. Pendências

Implementar RF26 completo (token de confirmação de uso único, expiração, envio/reenvio limitado, ativação e caminho do menor para consentimento), sem ampliar retroativamente esta correção. Resolver separadamente a asserção antiga RF08 de biografia e a asserção de nome de banco no teste do dump, ambas confirmadas na execução final. Os 17 skips condicionais continuam pendentes de seu ambiente próprio.

## 17. Próximo passo

Projetar a confirmação de e-mail e as transições de estado com o schema oficial existente, incluindo política explícita para registros `ATIVA`/`email_verificado=false`, testes de token/reenvio e preservação do fluxo Google e de consentimento. Tratar as duas falhas históricas em tarefas próprias, sem mascarar o resultado de `mvn test`.

## 18. Registro semanal — 28/09/2026

| Campo | Registro |
|---|---|
| Objetivo | Fechar bypass de sessão normal para estado diferente de `ATIVA` |
| RF/RNF | RF02, bloqueio parcial RF26; RNF06, RNF08, RNF10 |
| Backend | Política comum de estado; login/refresh/JWT; STOMP e entregas WebSocket/SSE |
| Testes | Fixtures de contas ativas, novos casos de estado/refresh/JWT/tempo real; PostgreSQL/Testcontainers; execução final na seção 9 |
| Frontend, banco, migrations | Não alterados |
| Situação | Bloqueio implementado; focos verdes; `mvn test` 746/727/2/0/17, BUILD FAILURE pelas duas falhas históricas fora do escopo; RF26 completo pendente |
| Próximo passo | RF26 completo e correção isolada das falhas históricas da suíte |

Sem commit, push, reset, clean, checkout destrutivo ou stash automático.

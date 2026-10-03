# RF26 — confirmação de e-mail e ativação da conta (28/09/2026)

**Objetivo e fontes.** Implementar cadastro convencional com confirmação de e-mail, token de uso único e reenvio, preservando a autorização por estado persistido. Foram consultados integralmente o `rf e rnf.txt` revisado e os relatórios `RELATORIO_RF02_RF26_BLOQUEIO_CONTA_NAO_ATIVA.md`, `RELATORIO_BASELINE_TESTES_VERDE_2026-09-28.md` e `AUDITORIA_DELTA_RFS_REVISADOS_2026-09-28.md`. Contratos relacionados: RF01, RF02, RF09, RF24, RF25, RF26, RF27 e RNF01, RNF06, RNF08, RNF09, RNF10, RNF19. Checkout `integracao-recuperada-2026-09-15`, com working tree previamente modificado e preservado; sem commit ou push.

## Diagnóstico anterior e estrutura reutilizada

O cadastro em `AuthService` já gravava `PENDENTE_VERIFICACAO_EMAIL`, mas não criava nem enviava token. Não havia endpoint de confirmação ou reenvio. A recuperação RF09 já dispunha de SMTP condicional por `MAIL_HOST` e `MAIL_FROM`, URL de frontend configurável, token aleatório de 32 bytes e hash SHA-256. O login convencional, refresh, JWT, STOMP e demais acessos já barravam estados não ativos. O verificador Google exige `email_verified=true`, mas o vínculo de uma conta convencional pendente não concluía a pendência de e-mail.

A tabela oficial `usuarios` já contém `email_verificado`, `token_verificacao`, `tentativas_verificacao_email`, `ultimo_reenvio_verificacao` e `status_conta`. Não há coluna exclusiva de expiração do token de verificação: o instante do envio inicial ou reenvio em `ultimo_reenvio_verificacao` é a referência para **1 hora de validade**. `token_expiracao` permanece exclusivo do RF09; os dois fluxos não compartilham token ou prazo.

## Implementação, API e segurança

| Fluxo | Contrato aplicado |
|---|---|
| Cadastro convencional | Cria conta pendente, `email_verificado=false`, hash SHA-256 de token aleatório Base64URL de 32 bytes, instante de emissão e envio SMTP. Não retorna token nem sessão. Sem remetente ou com falha de envio, responde `503` no formato global e desfaz o cadastro. Contratante com menos de 18 anos é rejeitado conforme RF01. |
| `POST /api/auth/confirm-email` | Recebe `{ "token": "..." }` no corpo. Sob bloqueio de linha, valida hash, estado, validade e idade; limpa o token. Retorna `200` com `mensagem` no DTO existente, `404` para token inválido/usado, `410` para expirado, `409` para estado incompatível e `400` para corpo inválido. |
| `POST /api/auth/resend-confirmation` | Recebe `{ "email": "..." }`; para conta pendente apta, substitui o hash/token anterior. Resposta `200` genérica igual para conta existente, ausente ou já confirmada. Repetição em menos de 1 minuto ou mais de 5 solicitações/dia por chave recebe `429` antes da consulta ao banco, sem revelar existência. Os campos persistidos impõem mais uma barreira de 1 minuto e 5 reenvios até 24 horas após o último envio; quando ela bloqueia, a resposta continua genérica. Falha SMTP preserva o link anterior. |
| Transições | Adulto apto vai a `ATIVA`. Artista de 14–17 anos vai a `PENDENTE_CONSENTIMENTO` e continua sem sessão até RF27. Estado `BLOQUEADA`, tipo/idade incompatível ou conta já confirmada não são ativados pela rota. |
| Google RF24 | Um ID Token validado com e-mail verificado conclui apenas a pendência convencional de e-mail. Conta Google nova segue `PENDENTE_TIPO_PERFIL`; menor segue aguardando consentimento; perfil incompleto continua sujeito a `GoogleAccountAccessPolicy`. O vínculo existente é lido com bloqueio de linha para não sobrescrever mudança concorrente de estado. |

O link SMTP usa `/confirmar-email#token=...`: o fragmento não integra a requisição HTTP de navegação. A página retira o fragmento do histórico e envia o token somente no corpo do `POST`. O token bruto não é armazenado nem lançado em logs; falhas de envio registram apenas a classe da exceção. A senha e o hash BCrypt não são alterados pela confirmação. Login/refresh continuam exigindo o estado consultado no banco; não há status confiado ao cliente nem relaxamento de RF02. As respostas novas usam `PasswordRecoveryResponse` e o tratamento global `ErroResposta`.

## Arquivos alterados nesta tarefa

| Grupo | Arquivos e motivo |
|---|---|
| Backend de produção | `AuthService`, `AuthController`, `FrontendController`, `SecurityConfig`, `UsuarioRepository`, `ApiExceptionHandler`: emissão no cadastro, transições Google, rotas pública de API e deep link, consultas bloqueadas, erros HTTP. Novos `EmailVerificationService`, `EmailVerificationEmailSender`, `SmtpEmailVerificationEmailSender`, `ResendConfirmationRateLimiter`, `ConfirmEmailRequest`, `ResendConfirmationRequest`, `TokenExpiredException`, `TooManyRequestsException`, `EmailDeliveryUnavailableException`: lógica, transporte e contrato RF26. |
| Frontend | `src/lib/auth.js`, `src/App.jsx`, `src/pages/account/contracts.js`, nova `src/pages/EmailConfirmation.jsx`, `src/pages/AuthScreens.jsx`, `src/pages/registration/ArtistRegistration.jsx` e `HirerRegistration.jsx`: chamada à API, rota pública, estados aguardando/confirmado/inválido/expirado/reenvio e texto de cadastro. A nova tela reutiliza classes visuais da recuperação de senha; não houve redesign. Em `AuthScreens.jsx`, a leitura do token RF09 foi alinhada em uma linha ao `#token=` que o SMTP RF09 já envia, evitando a regressão real do link de recuperação. |
| Testes | Novo `EmailVerificationRf26IntegrationTest` e `EmailVerificationTestConfig`; ajustes em `AuthControllerRf01Rf02IntegrationTest`, `GoogleAuthRf24HardeningIntegrationTest`, `GenericEndpointsSecurityIntegrationTest`, `OfficialLocalApiIntegrationTest`, `UsuarioControllerIT` e `src/pages/account/contracts.test.js`. Um caso antigo de contratante menor foi removido da matriz de acesso privado por contrariar RF01; a rejeição desse cadastro passou a ter teste RF26 explícito. |

## Testes e ambiente

| Execução | Total | Passed | Failures | Errors | Skipped | Resultado |
|---|---:|---:|---:|---:|---:|---|
| Focada final RF26 + Google RF24 | 30 | 30 | 0 | 0 | 0 | `BUILD SUCCESS` |
| `backend> .\mvnw.cmd test` final | 758 | 741 | 0 | 0 | 17 | `BUILD SUCCESS` |

Os testes RF26 cobrem cadastro, hash/expiração/uso único, login e JWT/refresh antes da confirmação, adulto, menor, contratante menor rejeitado, bloqueio, token inválido/expirado, reenvio/limite, cota persistida, falhas SMTP, ausência de token em resposta/log e deep link público. A regressão focada anterior também executou RF09, RF01/RF02, JWT e autorização de endpoints (86 testes; 2 expectativas antigas corrigidas); a execução focada final passou 30/30. O frontend passou em `npm run build` após o ajuste final e em 20 testes Node de contratos de conta/cadastro. A correção de leitura do fragmento RF09 foi conferida no código e no build; não houve teste de entrega SMTP/navegação real para RF09.

Os **54 XMLs Surefire** confirmam os totais finais: 758 testes, 741 passed, 0 failures, 0 errors e 17 skipped. Os skips são 4 de `CurrentLocalSchemaIntegrationTest` e 13 de `OfficialLocalApiIntegrationTest`, condicionais e sem ativação artificial. O baseline anterior (747/730/0/0/17) foi preservado e ampliado apenas pelos cenários de RF26 e pelo alinhamento do caso antigo de contratante menor.

A execução completa incluiu as regressões de RF09, RF01/RF02, RF24 Google, RF25 rememberMe/refresh/logout, JWT, autorização de endpoints, STOMP/WebSocket e SSE. Não foi usado H2 nem `ddl-auto=create/update`.

PostgreSQL **18.4** em Docker/Testcontainers (`postgres:18-alpine`), base descartável `test` em porta efêmera, com o helper oficial e `spring.jpa.hibernate.ddl-auto=validate`. Não foram ativados `palco.current-db-tests` ou `palco.official-db-tests`.

**Limites de alteração:** backend de produção **sim**, somente RF26/controle de idade pertinente; frontend **sim**, integração mínima RF26 e leitura do fragmento RF09; banco oficial, esquema versionado, dump, SQL e migrations **não foram alterados nesta tarefa**. O working tree já continha mudanças locais, inclusive em SQL, antes do RF26; elas foram preservadas. `graphify-out/` foi atualizado conforme `AGENTS.md`.

## Riscos, pendências e próximo passo

1. A entrega SMTP real depende de `MAIL_HOST`, `MAIL_FROM` e `FRONTEND_BASE_URL` válidos. Os testes usam remetente de captura; não homologam um provedor de e-mail real. Sem remetente, o cadastro falha fechado com `503`. Um erro de commit posterior ao envio pode produzir um link sem conta persistida; uma fila transacional eliminaria essa janela, mas exigiria estrutura fora do escopo.
2. O limitador em memória é por processo (até 10 mil chaves, armazenadas como hash do e-mail). Em múltiplas instâncias, a cota persistida continua protegendo contas existentes, mas o `429` por chave não é global. O bloqueio persistido responde genericamente para impedir enumeração.
3. RF27, conclusão de perfil Google e testes opcionais contra banco local/oficial permanecem pendentes. A confirmação do e-mail de menor não constitui consentimento. O frontend foi validado por build e testes de contrato, sem homologação visual manual.

**Próximo passo:** configurar e homologar SMTP/URL pública em ambiente controlado e, em escopo separado, implementar RF27 e conclusão do perfil Google; manter os 17 testes condicionais sob decisão própria de execução.

## Registro semanal — 28/09/2026

| Campo | Registro |
|---|---|
| Objetivo/RF/RNF | RF26 completo, preservando RF01/RF02/RF09/RF24/RF25/RF27 e RNF01/06/08/09/10/19. |
| Backend/frontend | Fluxo de verificação, SMTP, reenvio, transições e página mínima integrados. |
| Banco/migrations | Nenhuma alteração nesta tarefa; `ddl-auto=validate`. |
| Evidência | Testes focados 30/30; Node 20/20; build Vite verde; suíte completa 758/741/0/0/17, `BUILD SUCCESS`. |
| Pendências/próximo passo | Homologação SMTP real, RF27, perfil Google e testes condicionais em escopo próprio. |

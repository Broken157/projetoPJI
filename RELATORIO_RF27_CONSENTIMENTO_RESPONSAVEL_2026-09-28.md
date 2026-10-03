# RF27 — consentimento do responsável para artista adolescente

**Data:** 28/09/2026. **Estado:** fluxo principal implementado e regressão automatizada verde; entrega SMTP real e lacunas preexistentes de RF01/RF24/RF10 ainda exigem validação própria. Não houve commit nem push.

## Objetivo, fontes e diagnóstico

Implementar o convite e a decisão inicial do responsável legal de ARTISTA de 14 a 17 anos, após a confirmação do e-mail do próprio artista, sem alterar o banco. Foram confrontados o `rf e rnf.txt` revisado, `RELATORIO_RF26_CONFIRMACAO_EMAIL_2026-09-28.md`, `RELATORIO_RF02_RF26_BLOQUEIO_CONTA_NAO_ATIVA.md`, `RELATORIO_BASELINE_TESTES_VERDE_2026-09-28.md`, `AUDITORIA_DELTA_RFS_REVISADOS_2026-09-28.md`, código atual, SQL versionado e dump restaurado nos testes. RF01, RF02, RF10, RF24, RF25, RF26, RF27, RF35 e RF44; RNF06, RNF08, RNF09 e RNF10 foram considerados. Serena localizou símbolos/referências; Graphify avaliou o vínculo `ResponsavelLegal` e foi atualizado após as alterações.

Antes, `AuthService` persistia o contato do responsável e o RF26 mudava o menor para `PENDENTE_CONSENTIMENTO`, mas não havia convite, decisão ou reenvio. O modal do frontend declarava a autorização indisponível. Login, refresh, JWT antigo, STOMP, WebSocket e SSE já eram barrados por estado de conta não ativo; essas barreiras foram preservadas.

## Estrutura oficial reutilizada

| Estrutura | Campos/limites reais |
|---|---|
| `responsaveis_legais` / `ResponsavelLegal` | `id` PK; `usuario_id` único, FK a `usuarios(id)` com `ON DELETE SET NULL`; `nome_responsavel` varchar(150) NOT NULL; `telefone_responsavel` varchar(20) NOT NULL; `email_responsavel` varchar(150) NOT NULL; `versao_termo` varchar(50) nullable; `token_consentimento` varchar(255) nullable; `consentimento_revogado` boolean nullable com default false; `data_consentimento` timestamp nullable. |
| `usuarios` | `email_verificado`, `status_conta`, vínculo JPA ao responsável; enum de status existente: `PENDENTE_VERIFICACAO_EMAIL`, `PENDENTE_TIPO_PERFIL`, `PENDENTE_CONSENTIMENTO`, `ATIVA`, `BLOQUEADA`. |
| Ausências | Não há coluna dedicada a envio, expiração, tentativas, data de recusa ou enum da decisão; não há índice/unique específico do token do responsável. A FK única impede mais de um responsável por usuário. |

O token bruto é gerado com 32 bytes de `SecureRandom` e codificado em Base64URL. Somente `v1:<SHA-256 hexadecimal>:<segundos da emissão>:<número de reenvios>` fica em `token_consentimento` (até 255 caracteres). O prazo é **24 horas**; reenvio exige intervalo mínimo de **1 minuto** e até **5 reenvios por janela de 24 horas**, além do limitador por chave em memória antes da consulta à conta. O token RF27 não reutiliza `token_verificacao` do RF26 nem `token_recuperacao` do RF09. Uma decisão limpa o token; reenvio válido o substitui. A consulta e a decisão usam hash; a decisão bloqueia a linha do responsável para serializar cliques concorrentes.

## API, transições e autorização

| Endpoint público | Entrada e comportamento |
|---|---|
| `POST /api/auth/guardian-invite` | `{ "token": "..." }`; consulta somente o nome do artista e uma instrução contextual, sem dados privados do responsável/menor. |
| `POST /api/auth/guardian-decision` | `{ "token": "...", "decisao": "AUTORIZAR"|"RECUSAR" }`; valida hash, prazo, uso único, e-mail verificado, idade, papel, vínculo e estado. |
| `POST /api/auth/resend-guardian-invite` | `{ "email": "email do artista" }`; resposta genérica igual para conta inexistente, não pendente ou recusada; aplica limite antes da busca. |

Corpos inválidos seguem `400`/`ErroResposta`; token ausente/usado/substituído `404`; expirado `410`; estado incompatível ou cadastro inicial insuficiente `409`; limite de reenvio `429`. Consulta e decisões `200` usam `Cache-Control: no-store`. O link SMTP usa `/consentimento-responsavel#token=...`; o frontend remove o fragmento do histórico e envia o token apenas no corpo do POST. Não há `userId`, papel ou status aceito do cliente como autoridade.

No cadastro convencional, o menor começa em `PENDENTE_VERIFICACAO_EMAIL`. O RF26 confirma o e-mail, grava `email_verificado=true`, muda para `PENDENTE_CONSENTIMENTO` e tenta enviar o convite ao responsável persistido no RF01. O backend rejeita e-mails do menor e responsável iguais, inclusive diferença apenas de caixa/espaços. Sem autorização não há sessão normal. No Google, ID Token validado e e-mail verificado dispensam RF26; o cadastro provisório permanece `PENDENTE_TIPO_PERFIL` até receber os dados de artista/responsável suportados hoje, quando passa a `PENDENTE_CONSENTIMENTO` e envia o convite. Repetir login Google pendente não reenvia convite nem emite JWT. O login Google após autorização continua condicionado à política persistida de acesso.

**AUTORIZAR:** somente com conta ainda pendente e perfil artístico inicial com subtipo, raio e área principal; grava `data_consentimento`, `consentimento_revogado=false`, limpa token e ativa a conta. **RECUSAR:** grava a data, `consentimento_revogado=true`, limpa token e muda a conta para `BLOQUEADA`; reenvio não reabre essa decisão. Não foi implementada revogação, reconsideração nem RF44. A coluna `data_consentimento` guarda também a data da recusa por falta de campo específico; consumidores futuros devem interpretar conjuntamente o booleano e o status, nunca a data isoladamente. `versao_termo` não foi preenchida porque não existe um termo RF27 versionado correspondente nesta entrega.

Se o SMTP do responsável falhar após RF26, o e-mail do menor permanece confirmado e a conta permanece pendente; o token anterior é preservado quando existir, ou fica nulo para reenvio posterior. A resposta de confirmação informa a pendência e a possibilidade de reenvio. O endpoint público de reenvio mantém resposta genérica mesmo em falha de entrega para não enumerar contas. A UI apresenta erros de rede/limite quando recebidos.

O menor autorizado continua sujeito às proteções existentes: dados do responsável, CPF/documentos, e-mail, telefone, nascimento e experiência não são tornados públicos por RF27. Atualmente RF10 ainda devolve `404` para todo artista menor, inclusive autorizado; a projeção pública restrita prevista no RF10 revisado é uma pendência própria, sem enfraquecer a privacidade nesta tarefa. A política de acesso relê o estado do banco em login, refresh, JWT e canais em tempo real; `PENDENTE_CONSENTIMENTO` e `BLOQUEADA` não são liberados por claims antigos.

## Arquivos alterados nesta tarefa

| Grupo | Arquivos e motivo |
|---|---|
| Backend de produção | `AuthService`, `EmailVerificationService`, `GoogleAccountAccessPolicy`, `AuthController`, `SecurityConfig`, `FrontendController`, `ResponsavelLegalRepository`, `ResendConfirmationRateLimiter`, `GoogleAuthRequest`; novos `GuardianConsentService`, `GuardianConsentEmailSender`, `SmtpGuardianConsentEmailSender`, `GuardianDecisionRequest`, `GuardianInviteResponse`. Implementam validação, convite, decisão, segurança e rotas usando o schema existente. |
| Frontend | `src/lib/auth.js`, `src/App.jsx`, `src/pages/account/contracts.js`, `src/pages/EmailConfirmation.jsx`, `src/pages/registration/ArtistRegistration.jsx`, `src/pages/registration/GuardianConfirmation.jsx` e novo `src/pages/GuardianConsent.jsx`. Reutilizam o cartão de recuperação e expõem espera/reenvio, consulta, Autorizar/Recusar e estados final/inválido/expirado/usado. |
| Testes | Novo `GuardianConsentRf27IntegrationTest`; `EmailVerificationTestConfig` captura separadamente convites RF27; `EmailVerificationRf26IntegrationTest` limpa o capturador; `src/pages/account/contracts.test.js` reconhece os dois novos deep links. |

## Testes e resultado

| Execução | Total | Passed | Failures | Errors | Skipped | Resultado |
|---|---:|---:|---:|---:|---:|---|
| RF27 focado final | 10 | 10 | 0 | 0 | 0 | `BUILD SUCCESS` |
| RF26 + RF24 + RF27 focados anteriores | 37 | 37 | 0 | 0 | 0 | `BUILD SUCCESS` |
| `backend> .\mvnw.cmd test` final | **768** | **751** | **0** | **0** | **17** | **`BUILD SUCCESS`** |
| Frontend `node --test` | 63 | 63 | 0 | 0 | 0 | Sucesso |
| Frontend `npm run build` | — | — | — | — | — | Vite build concluído |

Os casos RF27 cobrem adulto fora do fluxo; transição RF26; validação de e-mails distintos; hash/uso único/expiração; autorizar e recusar; ativação e bloqueio; link inválido; reenvio, substituição, não enumeração e limite; falha SMTP; login, refresh e JWT antigo; estado incompatível/perfil inicial insuficiente; fluxo Google novo/provisório/autorizado; privacidade do responsável e experiência via RF10; decisões opostas simultâneas e duplo clique em Autorizar. A suíte completa inclui RF01/RF02, RF09, RF10, RF24, RF25, RF26, autorização genérica, STOMP, WebSocket e SSE. Foram usados PostgreSQL **18.4** em Docker/Testcontainers e `spring.jpa.hibernate.ddl-auto=validate`, sem H2 ou `create/update`. Os **55 XMLs Surefire** confirmam os totais. Os 17 testes locais/oficiais condicionais não foram habilitados artificialmente. Log final: `backend/target/rf27-full-final.log`.

**Banco/schema/migrations/dump/SQL oficiais:** nenhuma alteração nesta tarefa. **Backend de produção:** sim, apenas para RF27 e integração imediata com RF01/RF24/RF26. **Frontend:** sim, somente interface mínima RF27. O working tree já tinha muitas alterações, inclusive SQL preexistente, que foram preservadas.

## Riscos, pendências e próximo passo

1. O SMTP real e a URL pública dependem de `MAIL_HOST`, `MAIL_FROM` e `FRONTEND_BASE_URL` e não foram homologados com um responsável real. Os testes capturam o envio em memória. Enviar e-mail dentro da transação deixa uma janela entre entrega e commit; fila/outbox exigiria escopo estrutural futuro.
2. O limitador inicial é por processo. Os dados de tentativa no campo de token protegem a conta persistida, mas a política de taxa entre múltiplas instâncias requer infraestrutura compartilhada. O campo `consentimento_revogado` representa recusa inicial nesta implementação, embora seu nome sugira revogação posterior; a ausência de estado/data de recusa dedicados merece decisão de modelagem futura, sem DDL nesta tarefa.
3. O link prova controle do e-mail informado para o responsável, não identidade civil/parentesco. Não houve validação jurídica do termo/base legal ou política de retenção. Não se afirmou conformidade LGPD automática.
4. RF01 revisado ainda contém lacunas preexistentes de `@username` e CPF/CNPJ; RF24 frontend continua sem provedor Google configurado e com botão desabilitado. O backend RF27 testa Google com dados atualmente suportados, mas não substitui a conclusão integral desses RFs. RF10 de menor autorizado e avisos RF44 ficam para suas tarefas. Metas numéricas de cobertura do RNF10 não foram medidas; testes de integração críticos passaram.

**Próximo passo:** homologar entrega SMTP e URL em ambiente controlado; tratar separadamente as lacunas RF01/RF24/RF10 e, se autorizado, definir representação explícita da recusa/auditoria em versão futura do banco.

## Registro semanal — 28/09/2026

| Campo | Registro |
|---|---|
| Objetivo | RF27: convite, decisão, bloqueio e reenvio do responsável legal. |
| Evidência | RF27 10/10; frontend 63/63 e build verde; suíte Maven 768/751/0/0/17, `BUILD SUCCESS`. |
| Backend/frontend | Ambos alterados para o fluxo RF27. |
| Banco/migrations | Nenhuma alteração nesta tarefa; `ddl-auto=validate`. |
| Pendências | SMTP real, RF01/RF24/RF10 e semântica de recusa/retencão. |
| Próximo passo | Homologação controlada e tarefas independentes dos RFs relacionados. |

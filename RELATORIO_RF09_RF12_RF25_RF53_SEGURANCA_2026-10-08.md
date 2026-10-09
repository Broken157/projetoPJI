# RELATÓRIO RF09 + RF12 + RF25 + RF53 — SEGURANÇA — 08/10/2026

## 1. Resumo executivo

RF09/RF12 auditados e preservados no núcleo, com correções comprovadas de concorrência/revogação; RF25 e o recorte Segurança do RF53 implementados sem mudança estrutural. Foco final: 216/216, 0 failures/errors/skipped, BUILD SUCCESS. Fechamento da regressão completa, Semgrep e Graphify: APROVADO no recorte e na instância atual (D15 explícita). RF09 CONFIRMADO/CONSOLIDADO; RF12 CONFIRMADO/CONSOLIDADO; RF25 CONCLUÍDO NO BACKEND; RF53 RECORTE SEGURANÇA CONCLUÍDO. RF53 integral permanece PARCIAL.

## 2. Branch e HEAD inicial/final

`integracao-recuperada-2026-09-15`; HEAD inicial `ef9748f297da3989cd2d66b874cf4be0fec24edb` (checkpoint RF06/RF23/RF28/RF45 esperado). Fetch fork exit 0; divergência 0/0. HEAD final: ef9748f297da3989cd2d66b874cf4be0fec24edb. Backend/index inicialmente limpos. Preexistentes preservados: README.md, database/02_tables/04_vagas.sql, 25 arquivos operacionais palco-comunidades-agenda e demais documentos/artefatos não rastreados. Não houve staging/commit/push/reset/clean/stash.

## 3. Fontes consultadas

Baseline oficial `C:/Users/masca/OneDrive/Área de Trabalho/-/trabalhosAula/tecnico/pji/rf e rnf.txt`: RF02/RF08/RF09/RF12/RF24/RF25/RF53 e RNF01/RNF08/RNF09/RNF10/RNF19. SHA-256 `08FB838914D57F1C6B54F3CFDC73DB5FE1F7CD511B4DFCA5A736DC094CA20EDA`. Database05/palco-database como única fonte estrutural; ZIP oficial SHA-256 `6158C813929AC0DB9B3B12030E8B9D84C3B647611986DD6D60B2FC50D0F97EBF`. Foram relidos os trechos pertinentes dos relatórios RF54/RF08, RF37, Vagas, Candidaturas e auditoria estrutural de 08/10. Seus resultados anteriores são históricos. Context7 resolve_library_id/query_docs: `/spring-projects/spring-data-jpa`, documentação oficial de locking/transactions na branch main, sem afirmar correspondência de versão fixa; comportamento confirmado pelos testes PostgreSQL.

Referências de API: [locking](https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/locking.adoc), [transactions](https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/transactions.adoc).

## 4. Estado inicial RF09

Documentalmente DESENVOLVIDO. PasswordRecoveryService/SmtpPasswordRecoveryEmailSender/ResetPasswordRequest e RecuperacaoSenhaRf09IntegrationTest já atendiam resposta genérica, Google-only, geração segura/hash/1h, policy antes de consumir token e rollback. Lacuna real: leitura do reset sem lock, sem revogação de JWT access-only. Matriz anterior às alterações em evidencias/seguranca-sessoes-2026-10-08/auditoria-inicial.md.

## 5. Estado inicial RF12

Documentalmente DESENVOLVIDO. AuthController.logout já invalidava somente cookie atual, apagava cookie e revogava JWT pela blacklist. Contrato 204 público/idempotente preservado. Acrescentada prova negativa de reutilização, access-only, múltiplos dispositivos e corrida com refresh.

## 6. Estado inicial RF25

Documentalmente PARCIAL. Login true/false, hash, cookie-only, rotação na mesma sessão e lock da linha já existiam. Lacunas: erro de refresh sem cookie zero, serialização com senha/reset e APIs de sessões. Não houve troca de default, nome de cookie, formato de access token ou estratégia de rotação por preferência.

## 7. Estado inicial RF53 Segurança

Senha atual BCrypt e PasswordPolicy já existiam em PUT /api/usuarios/me; Google-only era negado. Faltavam API isolada de Segurança, listagem e encerramento próprio/total; JWT access-only podia continuar após mudança de senha. Outros recortes de Configurações não foram implementados.

## 8. Arquivos auditados

Controllers Auth/Usuario; AuthService, PasswordRecoveryService, RefreshTokenService, UsuarioService, SMTP; Usuario/RefreshToken; UsuarioRepository/RefreshTokenRepository; JWT/filter/resolver, GoogleAccountAccessPolicy, SessionCookiePolicy, SecurityConfig; requests/responses de auth/senha; exception handler; testes relacionados e OfficialPostgreSQLContainer/OfficialSchemaFixtures; database05/02_tables/01_usuarios.sql. Serena consultado por símbolos de AuthController/AuthService/RefreshTokenService; Graphify orientou navegação inicial.

## 9. Arquivos alterados/criados

**10 arquivos Java de produção e 4 de testes (14 no total):**

- [AuthController.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/controller/AuthController.java) — alterado
- [RefreshTokenRepository.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/repository/RefreshTokenRepository.java) — alterado
- [UsuarioRepository.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/repository/UsuarioRepository.java) — alterado
- [JwtService.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/security/JwtService.java) — alterado
- [AuthService.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/service/AuthService.java) — alterado
- [PasswordRecoveryService.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/service/PasswordRecoveryService.java) — alterado
- [RefreshTokenService.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/service/RefreshTokenService.java) — alterado
- [UsuarioService.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/service/UsuarioService.java) — alterado
- [RecuperacaoSenhaRf09IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/RecuperacaoSenhaRf09IntegrationTest.java) — alterado
- [AlteracaoSenhaRequest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/AlteracaoSenhaRequest.java) — criado
- [SessaoResponse.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/SessaoResponse.java) — criado
- [SessoesSegurancaRf12Rf25Rf53IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/SessoesSegurancaRf12Rf25Rf53IntegrationTest.java) — criado
- [SessoesConcorrenciaIntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/SessoesConcorrenciaIntegrationTest.java) — criado
- [JwtRevogacaoInstanciaTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/security/JwtRevogacaoInstanciaTest.java) — criado

Escopo Java exato e hashes em escopo-autorizado.json e java-testado-manifesto.json. Relatório novo e evidências sob evidencias/seguranca-sessoes-2026-10-08/. Nenhum histórico anterior foi reescrito.

## 10. Estrutura física de usuários/reset

usuarios.senha varchar(255) nullable; google_id varchar(255) UNIQUE; token_recuperacao varchar(255); token_expiracao timestamp. CHECK exige senha ou google_id. Um único reset vigente por usuário é suficiente. BCrypt na senha; SHA-256 do raw reset no campo existente. Nenhum nullable/enum/constraint modificado.

## 11. Estrutura física de refresh_tokens

id bigserial PK; usuario_id FK nullable → usuarios, ON DELETE SET NULL; token_hash varchar(64) NOT NULL UNIQUE; expiracao timestamp NOT NULL; ativo boolean NOT NULL; data_criacao timestamp; dispositivo_info varchar(255), ip_criacao varchar(45), ultimo_uso timestamp. Índice existente idx_refresh_tokens_usuario. Múltiplas linhas por usuário já são suportadas. Sessão órfã é rejeitada. Não se declarou ausência de colunas de dispositivo/IP; sem produtor confiável, não se inventam esses dados.

## 12. RF09 — solicitação

POST /api/auth/forgot-password recebe e-mail no body. Mantém 200 e mensagem genérica; conta inexistente não cria usuário/token nem envia reset. Solicitação da conta existente bloqueia/recarrega usuário para serializar a substituição do campo de reset.

## 13. Não enumeração

Resposta idêntica: “Se o e-mail estiver cadastrado, você receberá as instruções em breve.” Não retorna ID, papel, estado, provedor, token/expiração. Teste compara integralmente os corpos para local/inexistente/Google-only. Isto comprova o contrato HTTP, sem certificar tempo constante ou toda infraestrutura externa.

## 14. Google-only

senha IS NULL com google_id existente: nenhum reset/nova senha artificial. Email informativo orienta Entrar com Google, quando SMTP configurado; HTTP continua genérico. Infraestrutura ausente/falha entrega mantém resposta genérica. Adapter e sender capturado testados; não se alegou entrega real no provedor de e-mail.

## 15. Geração do token de reset

SecureRandom, 32 bytes (256 bits), Base64 URL sem padding, 43 caracteres. Entropia supera 128 bits. Raw somente no envio de e-mail; não no response/log/persistência. Link existente usa tela frontend /redefinir-senha#token=; frontend não foi alterado.

## 16. SHA-256 do reset

SHA-256 UTF-8 → 64 hex; teste compara raw e hash persistido e verifica diferença. SHA-256 protege tokens aleatórios; não foi usado para senha local. Não existe histórico adicional de tokens.

## 17. Expiração de reset

LocalDateTime.now()+1h no campo timestamp oficial; válido somente se expiração estritamente posterior ao instante da validação. Pedido posterior substitui o hash anterior. Falha de entrega limpa o reset gerado conforme comportamento existente.

## 18. Redefinição

POST /api/auth/reset-password, body token+novaSenha. Lookup hash → ID → PESSIMISTIC_WRITE usuário → refresh JPA → revalida hash/expiração → PasswordPolicy.encode → atualiza BCrypt → limpa reset → revoga refresh → agenda blacklist local após commit. Inválido/expirado conserva 404 genérico vigente. Não há mutação por GET/query.

## 19. RNF19 centralizado

PasswordPolicy existente foi preservado: 8–72 codepoints, A-Z/a-z/0-9, especial não alfanumérico e não whitespace; senha exata sem trim/strip/normalização. Mensagem: “A senha deve ter entre 8 e 72 caracteres e conter ao menos uma letra maiúscula, uma letra minúscula, um número e um caractere especial.” BCryptPasswordEncoder para todas as senhas locais. Ressalva: UTF-8 pode ultrapassar 72 bytes antes de 72 caracteres; guard técnico já existente permanece e responde mensagem segura/422. Não virou regra ASCII ou limite funcional em bytes.

## 20. Senha inválida não consome token

Policy antes da mutação. Testes verificam hash anterior, hash reset, expiração e sessões preservados; mesmo token pode ser usado com senha válida. Falha técnica BCrypt também preserva tudo. Depois de sucesso, token não pode ser reutilizado.

## 21. Revogação após reset

Todos refresh do titular ficam inativos na mesma transação. JWTs associados à sessão passam a falhar pela linha física inativa; JTIs emitidos na instância são revogados após commit. JWT access-only do terceiro permanece válido. Nenhuma garantia de revogação access-only distribuída/durável: D15.

## 22. RF12 — logout

POST /api/auth/logout lê cookie, invalida apenas a sessão correspondente e revoga JWT apresentado pela blacklist vigente; cookie zero e 204 sem body. Seguro/idempotente quando repetido/sem segredo válido. Não cria sessão e não encerra outros dispositivos. Client redirecionamento/contexto visual permanece fora do backend.

## 23. rememberMe=false

Default false preservado (incluindo Google). Access normal, zero inserção em refresh_tokens, sem refresh JSON ou cookie persistente. Mantida a limpeza defensiva existente Set-Cookie Max-Age=0; é remoção, sem credencial persistente. Logout access-only invalida JWT local sem registro artificial.

## 24. rememberMe=true

Conta elegível gera UUID raw, hash SHA-256, ativo=true, timestamp real e expiração +30 dias conforme configuração vigente. Login novo sem cookie anterior não invalida outros dispositivos. Estado pendente/bloqueado continua sem sessão normal; RF02/Google regressões verdes no foco.

## 25. Cookie

Nome preservado palco_refresh; HttpOnly=true; Secure=true; SameSite=Strict; Path=/api/auth; Max-Age=2592000. Remoção usa mesmo nome/path/flags e Max-Age=0. Não houve redução para ambiente HTTP, localStorage/sessionStorage ou serialização do raw.

## 26. Hash do refresh

SHA-256 hexadecimal 64 caracteres; raw permanece somente cookie. LoginResponse, RefreshResponse e GoogleAuthResponse mantêm JsonIgnore para segredo transportado internamente ao controller. Listagem usa DTO explícito sem hash/raw/cookie.

## 27. Renovação

POST /api/auth/refresh usa exclusivamente cookies.ler; body/query/Authorization não substituem cookie. Token ausente/aleatório/expirado/revogado/órfão: 401, mensagem “Sessão expirada. Entre novamente.” e cookie removido. Sem novo access/refresh. Estado de conta inapto mantém proibição de acesso normal vigente (403), sem enfraquecimento do RF02.

## 28. Rotação

Sem criar histórico/família ou nova linha por uso: identidade da sessão permanece estável; segredo/hash antigo é substituído por novo sob lock, com novo access/cookie. Token anterior não encontra hash vigente e nunca renova. ativo continua true para a sessão sobrevivente; isto não mantém o segredo antigo ativo. CriadaEm/ID preservados; ultimoUso agora recebe uso real. API antiga já operava assim e foi preservada.

## 29. Concorrência da rotação

Ordem global usuário → sessão. Lookup inicial identifica titular; após espera no usuário, EntityManager.refresh e lookup/refresh da sessão reavaliam hash, ativo e expiração. Teste bloqueia a linha no PostgreSQL até duas requisições estarem esperando: exatamente 200+401, uma sessão ativa e somente o novo cookie funciona.

## 30. Múltiplos dispositivos

Duas sessões independentes do mesmo usuário coexistem. Logout/revogar uma não encerra a outra; ação total/senha/reset encerra todas do titular. Cookie anterior do mesmo navegador continua sendo substituído/invalidado conforme comportamento existente; não há invalidação automática de outros dispositivos.

## 31. RF53 — mudança de senha

Nova rota protegida PUT /api/auth/senha, body senhaAtual+novaSenha, 204 e cookie zero. Reutiliza helper de UsuarioService/PasswordPolicy. PUT /api/usuarios/me mantém compatibilidade e passa pela mesma regra, lock e revogação; não se deslocou RF26/contatos/consentimento para a nova rota.

## 32. Senha atual

JWT determina usuário. Bloqueia/recarrega credencial atual; BCrypt.matches antes de nova policy/encode. Atual ausente/incorreta rejeita sem alterações/revogação. Nova inválida idem. Sem proibição inventada de reutilização/histórico de N senhas; testes provam espaços preservados e BCrypt real.

## 33. Google-only nas configurações

Não cria primeira senha usando somente JWT. Conta Google-only elegível alcança o fluxo e recebe 422 seguro, senha permanece NULL. Decisão funcional de vincular/criar senha local permanece pendente; não se ampliou RF24.

## 34. Revogação total

Senha/reset/DELETE /api/auth/sessoes executam UPDATE dos refresh do titular na transação. Blacklist aplica snapshot de JTIs emitidos localmente somente afterCommit. Rollback não invalida access. Não há revogação de terceiro ou JWT raw guardado.

## 35. Listagem de sessões

GET /api/auth/sessoes protegido, identidade no SecurityContext/JWT. Somente own active AND expiracao>agora, ordem dataCriacao/id desc. Lista não fabrica sessão access-only e não é diretório de usuários. Cache-Control no-store.

## 36. Identificador público

sessionId = refresh_tokens.id, campo físico do próprio titular. IDs numéricos não concedem autoridade; ownership é obrigatório na consulta de revogação. Nunca usar hash/raw como identificador de API.

## 37. Sessão atual

atual=true somente quando SHA-256 do cookie coincide com hash de uma sessão própria retornada. Sem cookie/access-only → não inventa marcador; cookie de outro usuário não identifica atual. Não infere navegador/IP/geolocalização.

## 38. Encerrar uma sessão

DELETE /api/auth/sessoes/{sessionId}; lock do usuário e query por ID+owner, 404 seguro para alheia/inexistente. ativo=false transacional. Se cookie identifica alvo, limpa; se remota, não limpa cookie atual. JWT vinculado ao alvo falha via persistência. Testes verificam sessão restante.

## 39. Encerrar todas

DELETE /api/auth/sessoes: titular JWT, todos refresh próprios inativos, blacklist local afterCommit, cookie zero e 204. IDs/email em query/body não definem outro titular. Terceiro continua autenticado/renova; action total também cobre access-only emitido localmente.

## 40. Sessões expiradas

Excluídas da listagem; rejeitadas em refresh, sem criar sessão. Não foi inventado job de limpeza nem se apagou histórico físico. Legados com timestamp nulo não recebem valor fabricado; DTO usa campo real.

## 41. IDOR

Sem JWT → 401 em senha/listar/revogar uma/todas. A não lista/revoga B; ID alheio dá 404 sem alterar linhas. Payload senha com usuarioId/email de B ainda altera somente A e preserva B. Queries de owner nunca concedem autoridade.

## 42. CSRF, Origin e CORS

SecurityConfig/CORS/SessionCookiePolicy existentes preservados: autenticação stateless; cookie Strict, allowlist de Origin e CORS credenciado. Novas mutações validam Origin; /api/** continua autenticado. Origem externa falha antes do consumo/invalidação. HttpOnly não foi tratado como autorização. Não se certificou todo deployment HTTPS/proxy ou CSRF fora destes contratos.

## 43. Access JWT

Contrato de assinatura/claims/TTL mantido, jwt.expiration=86400000. Login/refresh retornam access no JSON conforme contrato. JWT com sessao valida ativo/expiração no PostgreSQL; logout atual revoga JTI. Metadados locais de emissão permitem ações de segurança cobrirem access-only conhecido na instância.

## 44. Blacklist

ConcurrentHashMap vigente mantido; registros JTI+expiração agrupados por titular para ação total, com expurgo de expirados. Fallback legado sem JTI usa SHA-256, nunca JWT raw como chave. Não houve Redis, tabela, versionamento de token ou novo contrato JWT. Revogação transacional só produz efeito afterCommit.

## 45. D15 — durabilidade e escala

**FUNCIONAL NA INSTÂNCIA ATUAL + DÍVIDA TÉCNICA D15 PARA RESTART/ESCALA.** Blacklist e JTIs emitidos são locais/voláteis. Tokens access-only de outra instância ou anteriores a restart não são conhecidos; blacklist não é global/durável/cluster-safe. JwtRevogacaoInstanciaTest demonstra token rejeitado na instância original mas aceito em outra com a mesma chave. Revogação persistente do refresh e validação de sessao na linha física continuam independentes dessa dívida. Antes de escalar, decidir arquitetura em tarefa própria.

## 46. Transações

Reset, mudança de senha, refresh facade, logout persistente e revogações uma/todas transacionais. Ordem de locks compartilhada com emissão de login. Validações antes da mutação; rollback físico de senha/reset/refresh verificado. Cookie de sucesso é escrito depois do retorno do proxy transacional/commit; blacklist após commit. SMTP síncrono existente continua dependência operacional, sem outbox inventado.

## 47. Concorrência

Cinco provas com PostgreSQL/READ_COMMITTED, holder transacional e pg_stat_activity demonstrando duas esperas reais: refresh×refresh, reset×reset, logout×refresh, senha×refresh e reset×refresh. Reset produz no máximo um sucesso; após ações de segurança nenhum segredo anterior/nova rotação anterior ao commit renova. Não usa H2, DDL auxiliar, sleep como única prova ou alteração de isolamento global.

## 48. Logs — RNF09

Inspeção de serviços/filtros/config/DTO/SMTP e captura runtime: sem senha atual/nova, BCrypt/hash reset/hash refresh, raw reset/refresh, JWT, cookie ou Authorization em logs/respostas de Segurança. SMTP registra somente classe de falha, sem mensagem contendo link. DTO de credenciais usa Getter/Setter sem toString. RNF09 integral/trilha universal não foi declarado concluído.

Revisão explícita das 32 perguntas obrigatórias em [revisao-manual-seguranca.md](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/seguranca-sessoes-2026-10-08/revisao-manual-seguranca.md). Respostas incluem D15 e limites de evidência, sem ocultar restart/escala.

## 49. Compatibilidade da API

| Método/rota | Contrato |
|---|---|
| POST /api/auth/forgot-password | 200 genérico; e-mail body |
| POST /api/auth/reset-password | body token/novaSenha; sucesso 200; inválido/expirado 404 vigente |
| POST /api/auth/login e /google | access JSON, refresh cookie-only; default rememberMe=false |
| POST /api/auth/refresh | cookie-only; 200+rotação ou 401+cookie zero |
| POST /api/auth/logout | 204, atual; idempotência preservada |
| PUT /api/auth/senha | novo, protegido, senhaAtual/novaSenha; 204+cookie zero |
| GET /api/auth/sessoes | novo, protegido, DTO mínimo próprio |
| DELETE /api/auth/sessoes/{sessionId} | novo, próprio, 204 ou 404 seguro |
| DELETE /api/auth/sessoes | novo, total próprio, 204+cookie zero |
| PUT /api/usuarios/me | compatibilidade de senha atual/policy preservada |

RefreshRequest é transporte interno, sem fallback HTTP. Não existia reset mutável GET/query a remover.

## 50. Banco alterado?

**NÃO.** Database05 oficial, SQL anteriores e esquema preservados. Pacote ZIP íntegro e baseline imutável; ddl-auto=validate mantido. Banco descartável de testes usa carga oficial completa existente e fixtures DML normais; não representa alteração do banco da aplicação. Delta legado database/02_tables/04_vagas.sql já existia e foi preservado por hash.

Preservação final: 1276 arquivos distintos capturados inicialmente, 1267 fora do delta Java autorizados; zero alteração protegida inesperada e zero arquivo novo fora de escopo. Index inalterado, cached vazio; diff database05/frontend vazio; delta legado e 25 deltas frontend operacionais são preexistentes, com hashes preservados. Baseline/ZIP intactos. git diff --check exit 0 sem saída. Evidência em preservacao-final.json/status-final.txt.

## 51. Frontend alterado?

**NÃO.** frontend e os 25 deltas preexistentes em palco-comunidades-agenda preservados. Nenhum npm/build/frontend/HTML/CSS/JS/React editado/executado; Maven somente copia recursos estáticos já existentes para diretório gerado.

## 52. SQL/migration criada?

**NÃO.** Nenhum arquivo SQL, migration, enum, seed oficial, coluna, índice, FK, constraint, rotina ou trigger criado/editado. Campos/metadados e locks usam a estrutura atual.

## 53. Testes novos/alterados

Novos: SessoesSegurancaRf12Rf25Rf53IntegrationTest (36 execuções, parâmetros incluídos), SessoesConcorrenciaIntegrationTest (5), JwtRevogacaoInstanciaTest (5). RF09 recebeu 2 casos adicionais (access-only/terceiro e segredo/log/validação). Total acrescido: 48 execuções. Todos com persistência/regra real proporcional ao risco, sem remoção de casos.

## 54. Ajuste de testes antigos

RecuperacaoSenhaRf09IntegrationTest mantém os 13 cenários anteriores e acrescenta dois. Demais testes históricos preservados. Primeira tentativa falhou compilando o genérico do teste novo, sem cenários; corrigido. Segunda: 211 total/210 passed/1 failure/0 errors/0 skipped, fixture Google-only não elegível bloqueada corretamente com 401. Fixture completada com dados normais/helper oficial, sem relaxar auth, e adicionada prova D15. Final: 216/216. Logs separados, nenhuma tentativa falha apagada.

## 55. Comandos

Checkpoint: git status --short; git branch --show-current; git rev-parse HEAD; git log --oneline -15; git fetch fork; git rev-list --left-right --count fork/integracao-recuperada-2026-09-15...HEAD. Execuções Maven por Executar-Maven.ps1 (JDK 21.0.12): .\mvnw.cmd test -Dtest=<14 classes reais do resumo>; depois .\mvnw.cmd test. Semgrep Docker e graphify update . conforme seções 59/60. Encerramento: diff name-status database05/database/frontend/palco-comunidades-agenda/cached; diff --check; status/diff stat; hashes de preservação. Sem add/commit/push.

## 56. Testes focados

**216 total/216 passed/0 failures/0 errors/0 skipped; BUILD SUCCESS; exit 0; 179,9 s; 14 suítes.** maven-focado-3-resumo.json. Classes: AuthControllerRf01Rf02IntegrationTest, CadastroRf01Rf24IntegrationTest, GoogleAuthRf24HardeningIntegrationTest, PerfilEdicaoRf08IntegrationTest, RecuperacaoSenhaRf09IntegrationTest, SessoesConcorrenciaIntegrationTest, SessoesSegurancaRf12Rf25Rf53IntegrationTest, GenericEndpointsSecurityIntegrationTest, JwtAuthenticationIntegrationTest, JwtRevogacaoInstanciaTest, SmtpPasswordRecoveryEmailSenderConfigurationTest, SmtpPasswordRecoveryEmailSenderTest, UsuarioServicePasswordPolicyTest, PasswordPolicyTest.

## 57. Maven completo

**1815 total/1798 passed/0 failures/0 errors/17 skipped; BUILD SUCCESS; exit 0; 810,0 s (13min30s); 80 suítes.** Execução própria em maven-completo.log/maven-completo-resumo.json, início 08/10/2026 20:15:43 −03:00 e fim 20:29:13 −03:00. Fontes Java permanecem com os hashes do foco aprovado; resultados obtidos dos XML novos desta execução, não do agregado de arquivos antigos.

## 58. Contadores e skips

Comparação em regressao-comparacao.json: +48 execuções, três novas suítes (36+5+5) e RF09 13→15. Nenhuma suíte anterior removida; nenhuma outra contagem de testes/skips alterada. Os 17 skips têm mensagens reais de propriedades opt-in ausentes: palco.current-db-tests (4) e palco.official-db-tests (13).

Baseline anterior: 1767 total/1750 passed/0 failures/0 errors/17 skipped (histórico RF06/RF23/RF28/RF45). Acréscimo esperado 48 casos, sem redução. Skips condicionais conhecidos: 4 CurrentLocalSchemaIntegrationTest e 13 OfficialLocalApiIntegrationTest; flags de bases locais opt-in, não testes novos ignorados. Cobertura JaCoCo/percentuais RNF10 não executada nem inferida.

## 59. Semgrep

**Semgrep 1.178.0 via Docker; p/java; 384 arquivos (main+test, incluindo os 14 do delta e fontes novas não rastreadas); 60 regras; ~100.0% linhas parseadas; 0 findings/0 blocking/0 errors; exit 0; 45,7 s.** Cobertura integral do delta confirmada, missingJavaFiles=[]. Resumo/log/JSON em semgrep-java-resumo.json/semgrep-java-saida.log/semgrep-java.json.

Imagem Docker oficial por digest fixo e ruleset p/java local previamente validado, SHA-256 5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1. Montagens read-only, rede none, --no-git-ignore; ignore vazio só no container para incluir fontes novas. Sem instalação nativa, ajustes App Control/Defender, parser ou TLS desabilitado. **0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.** Não substitui testes de sessão.

## 60. Graphify

Inicial MCP: 6789 nós/23657 arestas/360 comunidades; 91% extracted/9% inferred. BFS amplo truncado, refinado por vizinhos de RefreshTokenService/PasswordRecoveryService; símbolos/services/tests inspecionados no código. **graphify update . — AST-only, exit 0, 37,0 s; 6888 nós/24241 arestas/373 comunidades; MCP pós-update confirma 91% extracted/9% inferred/0% ambiguous e encontra o teste novo de sessões.** Warnings preservados: 45 SQL sem extração por tree_sitter_sql ausente; 75 arquivos sem classificação suportada; 102 comunidades renomeadas por hubs porque 360 labels antigos divergem das 373 comunidades atuais. Não foi instalado parser, não foi aplicado contorno Windows e não houve LLM labeling/extração semântica de documentos. Grafo de código atualizado; rótulos/semântica documental não são alegados atuais. Logs e estatísticas MCP próprios no diretório de evidências.

## 61. Riscos

D15 access-only volátil entre processos/restart (prova explícita); senha UTF-8 funcionalmente válida pode exceder limite técnico BCrypt; SMTP/configuração/entrega e HTTPS/origins dependem operação; sessão persistente não fornece automaticamente dispositivo/IP confiável. Locks por usuário serializam ações de identidade e podem aumentar espera durante SMTP síncrono. Não há certificação de carga/cluster/segurança absoluta.

## 62. Pendências

Decidir D15 antes de escala; definir primeira senha/vinculação Google-only em contrato seguro; demais recortes RF53 (RF26, RF27, RF48, RF22 e integração visual) em tarefas próprias. Não há blocker estrutural novo para senha/refresh/lista/revogação desta tarefa. Validações finais: nenhuma execução obrigatória pendente; foco/completo/Semgrep/AST e preservação aprovados. 1276 arquivos iniciais distintos, 1267 fora do escopo Java preservados; nenhum arquivo novo inesperado, nenhum delta Java após testes; index hash idêntico; ZIP/baseline intactos; diff --check exit 0, sem saída.

## 63. Conclusão RF09

**CONFIRMADO/CONSOLIDADO**. Núcleo anterior preservado, uso único concorrente e revogação local após commit consolidados. Sem ampliar conclusão para infraestrutura SMTP externa ou D15 durável.

## 64. Conclusão RF12

**CONFIRMADO/CONSOLIDADO**. Logout somente atual, revogação/cookie, idempotência, access-only e múltiplos dispositivos comprovados; D15 explícita.

## 65. Conclusão RF25

**CONCLUÍDO NO BACKEND**. rememberMe, cookie/hash, rotação concorrente, multidispositivo e gestão de sessões próprios; limitação access-only D15 não é ocultada.

## 66. Conclusão RF53 Segurança

**RECORTE SEGURANÇA CONCLUÍDO**. Senha atual/Policy/BCrypt, lista mínima/IDOR, encerra uma/todas, transação/rollback e revogação local provados. Criação de primeira senha Google-only não inventada.

## 67. Conclusão RF53 integral

**PARCIAL.** Recorte Segurança não significa central completa, frontend, e-mail/telefone/revalidação/privacidade/exclusão concluídos. Nenhuma conclusão alheia foi promovida.

## 68. Próximos passos

Revisar contratos/evidências deste delta e integrar a interface em tarefa separada. Priorizar decisões D15 e vínculo seguro Google-only conforme necessidade de operação; avançar recortes RF53 especializados com respectivas regras. Não se pede novo pacote/DDL para este recorte.

## 69. Registro semanal — 08/10/2026

Auditoria RF09/RF12/RF25/RF53 Segurança contra baseline e database05; ajustes mínimos de concorrência, cookie de falha, revogação e APIs próprias; 48 execuções novas, foco 216/216. Completo 1815 total/1798 passed/0 failures/0 errors/17 skipped, BUILD SUCCESS, 810,0 s; Semgrep 384 arquivos/60 regras/0 findings/blocking/errors, exit 0; Graphify AST 6888/24241/373, exit 0; diff --check exit 0 e 1267 arquivos fora do escopo preservados. Banco/frontend/SQL/index preservados; RF53 integral PARCIAL; D15 para restart/escala e ressalva BCrypt mantidas. Nenhum staging/commit/push.

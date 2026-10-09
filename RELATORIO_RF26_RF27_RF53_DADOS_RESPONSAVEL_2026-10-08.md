# RF26 + RF27 + recortes de Dados Pessoais/Responsável do RF53 — 08/10/2026

## 1. Resumo executivo

Consolidação do que o database05 representa, com contenção das alterações inseguras. RF26 de cadastro e consentimento inicial RF27 foram preservados; telefone do titular ganhou rota finalística com validação existente. PUTs de usuário deixaram de substituir e-mail confiável e dados de responsável sem confirmação/revalidação. RF26 total, RF27 total e RF53 integral permanecem **PARCIAIS**: não há e-mail pendente, vínculo familiar, separação de responsável validado/pedido pendente nem episódios históricos. Não se falsifica persistência com memória/JWT/JSON/notificação.

Validações novas: Focados **567/567**, completo **1855 total / 1838 passed / 0 failures / 0 errors / 17 skipped — BUILD SUCCESS**; Semgrep Docker **0 findings**, AST Graphify concluído; preservação/diff check aprovados. Nenhum resultado do checkpoint anterior é apresentado como execução desta tarefa.

## 2. Branch/HEAD

Checkout: C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline. Branch: integracao-recuperada-2026-09-15. HEAD inicial: **08b58f467b90f1f244b858570e70aeb9cdfced4b**, commit `feat: consolida seguranca RF09 RF12 RF25 RF53`. HEAD final: 08b58f467b90f1f244b858570e70aeb9cdfced4b. Fetch inicialmente falhou por DNS no sandbox; repetição autorizada concluiu, divergência fork/HEAD **0/0**. Sem add/commit/push/reset/clean/stash/checkout destrutivo.

Backend/index estavam sem delta inicial. README, database/02_tables/04_vagas.sql e frontend operacional já tinham alterações; documentos e arquivos não rastreados antigos foram preservados. Evidência: checkpoint-inicial.json/status-inicial.txt/log-inicial.txt/diff-inicial.txt e manifesto de 1282 arquivos distintos.

## 3. Fontes

Fonte funcional vigente: [rf e rnf.txt](<C:/Users/masca/OneDrive/Área de Trabalho/-/trabalhosAula/tecnico/pji/rf e rnf.txt>), SHA-256 **08FB838914D57F1C6B54F3CFDC73DB5FE1F7CD511B4DFCA5A736DC094CA20EDA**. RF01/02/08/10/24/26/27/44/53 lidos, RF09/12/25/48/22 consultados somente nas interfaces necessárias. Fonte não editada.

Relatórios consultados: AUDITORIA_DATABASE05_BASELINE_RF_RNF_2026-10-08.md, RELATORIO_RF54_RF08_2026-10-08.md, RELATORIO_RF37_2026-10-08.md, RELATORIO_RF06_RF23_RF28_RF45_2026-10-08.md e RELATORIO_RF09_RF12_RF25_RF53_SEGURANCA_2026-10-08.md. Relatórios anteriores não prevalecem sobre código/database05/baseline. Esta auditoria corrige, neste recorte, a suposição anterior de que troca protegida de e-mail e revalidação completa seriam apenas backend-only.

Context7 /spring-projects/spring-data-jpa: [locks](https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/locking.adoc) e [transações](https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/transactions.adoc). Documentação da branch main, sem alegar versão pinada; projeto Spring Boot 4.0.6. Comportamento concorrente decidido pelos testes PostgreSQL.

## 4. Database05 auditado

Fonte exclusiva: database05/palco-database/. ZIP oficial C:/Users/masca/Downloads/palco-database05.zip, SHA-256 **6158C813929AC0DB9B3B12030E8B9D84C3B647611986DD6D60B2FC50D0F97EBF**. **46/46 arquivos correspondem byte a byte ao ZIP**, sem diferenças (database05-integridade.json). Arquivos atuais preservados; ddl-auto=validate mantido. Nenhuma fonte database04/anteriores foi usada como schema atual.

Inspeção direta: 01_types/01_enums.sql, 02_tables/01_usuarios.sql, 02_perfis.sql, 10_agenda_gamificacao_e_logs.sql, procedures de cadastro/perfil/recuperação/exclusão e funções/triggers relacionadas. status_conta_enum: PENDENTE_VERIFICACAO_EMAIL, PENDENTE_TIPO_PERFIL, PENDENTE_CONSENTIMENTO, ATIVA, BLOQUEADA. PK bigserial; e-mail/username/google_id/CPF/CNPJ UNIQUE; username CHECK e chk_auth_method preservados. Nenhum trigger/procedure cria e-mail pendente, vínculo familiar ou histórico RF27.

## 5. Estado inicial RF26

**JÁ EXISTE** o cadastro pendente, token hash/validade/consumo com lock, confirmação por papel/idade, reenvio com resposta genérica e cota/cooldown. **INCORRETO**: UsuarioService editava usuarios.email imediatamente pelo PUT. **BLOCKER FÍSICO**: não há endereço pendente de troca nem estado/token específico durável para substituir pedido anterior preservando a credencial atual.

## 6. Estado inicial RF27

**JÁ EXISTE/PARCIAL**: registro normalizado atual, convite específico com hash+emissão+tentativas, autorização/recusa e reenvio. **INCORRETO**: edição de responsável sobrescrevia identidade/contato sem invalidar autorização. **BLOCKER C07/D05**: vínculo familiar e episódios/pedido pendente ausentes. **DÍVIDA TÉCNICA**: rate limit público adicional é local, sem garantia distribuída.

## 7. Estado inicial RF53 deste recorte

**PARCIAL**. Telefone tinha obrigatoriedade/tamanho no PUT, mas não a mesma validação de formato do cadastro. E-mail e responsável tinham edição direta insegura. Segurança RF09/RF12/RF25/RF53 permanece concluída no checkpoint anterior; Privacidade/RF48 e Exclusão/RF22 ficaram fora do escopo.

## 8. Arquivos auditados

Modelos Usuario, ResponsavelLegal e PerfilArtista; enums StatusConta/TipoUsuario; CadastroDadosRequest/CadastroRequest/GoogleCadastroRequest/UsuarioRequest/UsuarioAtualizacaoRequest/UsuarioResponse/ConfirmEmailRequest/GuardianDecisionRequest/ResendConfirmationRequest; UsuarioRepository/ResponsavelLegalRepository/PerfilArtistaRepository; AuthService/UsuarioService/EmailVerificationService/GuardianConsentService/RefreshTokenService/PasswordRecoveryService; CadastroValidator/PasswordPolicy; GoogleAccountAccessPolicy/GoogleRegistrationContextService/GoogleIdTokenVerifierAdapter; JwtAuthFilter/UserDetailsServiceImpl/AuthenticatedUserResolver/MenorAutorizadoPolicy; AuthController/UsuarioController/SecurityConfig/ApiExceptionHandler/CapabilitiesController; GuardianApplicationNoticeService/listener/sender e SMTPs de confirmação/convite/recuperação; testes reais nas seções 54/55. PerfilArtistaService/PerfilContratanteService não alteram o e-mail da conta. CapabilitiesController possui outros flags antigos, não reescritos fora deste recorte.

Graphify MCP orientou Auth/Cadastro/Policy/Sender/RF44/testes. Serena initial_instructions lido; overview falhou por language-server manager não inicializado. Sem instalar/reconfigurar/contornar ferramentas; fontes e Graphify decidiram a auditoria.

## 9. Arquivos alterados

**14 arquivos Java: 8 produção e 6 testes.**

- [UsuarioController.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/controller/UsuarioController.java>)
- [GuardianDecisionRequest.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/GuardianDecisionRequest.java>)
- [ResponsavelAtualResponse.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/ResponsavelAtualResponse.java>)
- [TelefoneAtualizacaoRequest.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/TelefoneAtualizacaoRequest.java>)
- [UsuarioAtualizacaoRequest.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/UsuarioAtualizacaoRequest.java>)
- [UsuarioRequest.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/UsuarioRequest.java>)
- [GuardianConsentService.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/service/GuardianConsentService.java>)
- [UsuarioService.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/service/UsuarioService.java>)
- [AvisoResponsavelRf44IntegrationTest.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/AvisoResponsavelRf44IntegrationTest.java>)
- [ConfirmacoesConcorrenciaRf26Rf27IntegrationTest.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/ConfirmacoesConcorrenciaRf26Rf27IntegrationTest.java>)
- [DadosResponsavelRf26Rf27Rf53IntegrationTest.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/DadosResponsavelRf26Rf27Rf53IntegrationTest.java>)
- [PerfilEdicaoRf08IntegrationTest.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/PerfilEdicaoRf08IntegrationTest.java>)
- [JwtAuthenticationIntegrationTest.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/security/JwtAuthenticationIntegrationTest.java>)
- [GuardianConsentServiceTest.java](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/service/GuardianConsentServiceTest.java>)

Além desses arquivos Java: este relatório e evidências próprias em evidencias/dados-responsavel-2026-10-08/. Graphify gera artefatos derivados em graphify-out/. Nenhum relatório antigo, baseline funcional ou arquivo de frontend/banco foi editado.

## 10. Estrutura física de verificação de e-mail

usuarios.email varchar(150) UNIQUE NOT NULL; email_verificado boolean; token_verificacao varchar(255); tentativas_verificacao_email integer; ultimo_reenvio_verificacao timestamp; status_conta enum; google_id UNIQUE; telefone varchar(20) NOT NULL; data_criacao timestamp. token_verificacao guarda SHA-256 hex64; emissão/validade RF26 derivam de ultimo_reenvio_verificacao. token_recuperacao/token_expiracao continuam exclusivos do fluxo de senha, sem reuso para e-mail. Não há email_pendente ou pedido de troca.

## 11. Estrutura física de responsável

responsaveis_legais: id bigserial PK, usuario_id bigint UNIQUE FK usuarios(id) ON DELETE SET NULL; nome_responsavel varchar150 NOT NULL; telefone_responsavel varchar20 NOT NULL; email_responsavel varchar150 NOT NULL; versao_termo varchar50; token_consentimento varchar255; consentimento_revogado boolean; data_consentimento timestamp. FK identifica o usuário menor, mas não expressa parentesco/vínculo familiar. Índice UNIQUE implicitamente sustenta uma linha atual por usuário. Não há coluna separada de expiração/recusa/pedido/versionamento; token existente guarda protocolo finalístico com emissão, não raw.

## 12. C07

**CONFIRMADO.** (1) Não existe atributo durável de vínculo familiar. (2) Registro único atual não separa contato/pessoa validada de uma alteração pendente. (3) Não há episódio reconstruível para preservar dados necessários, substituir token e associar nova decisão sem reaproveitar autorização antiga. Atualização de nome/e-mail/telefone/vinculoResponsavel é rejeitada com 422; campos iguais do cliente legado são compatíveis, sem reescrita.

Capacidade mínima conceitual futura: vínculo funcional e pedido/episódio de alteração associado ao titular, separando validado e pendente, com finalidade/token/validade/decisão e evidência mínima. Não escolhemos tabela/coluna nem escrevemos SQL.

## 13. D05

**ABERTA + BLOCKER DA TRILHA.** Schema mantém somente consentimento atual/data/termo/flag; não registra episódios de substituição/revalidação. Política de conteúdo mínimo, finalidade, retenção e descarte deve ser definida antes do pacote oficial. Log técnico não é trilha funcional durável; não se retêm dados antigos de forma improvisada.

## 14. Cadastro convencional

AuthService.cadastrar valida RF01 e senha central, cria usuário/perfil/associações/responsável aplicável atomicamente e inicia RF26. Conta fica PENDENTE_VERIFICACAO_EMAIL; resposta não emite sessão normal. Não reimplementamos cadastro nem criamos campos nullable/OAuth provisório. Nome/e-mail/telefone do responsável são representáveis; vínculo exigido pela baseline continua C07, sem declarar RF01 completo.

## 15. Estado pendente

PENDENTE_VERIFICACAO_EMAIL não recebe login/JWT normal. PENDENTE_CONSENTIMENTO não recebe uso normal. ATIVA é necessária às políticas atuais. APIs privadas mantêm autenticação e estado; não se liberou /api/** nem se emitiu JWT temporário genérico para pendentes.

## 16. Google

GoogleTokenVerifier aceita identidade/e-mail verificado pelo OIDC; conta existente é reutilizada e verifica e-mail sem repetir RF26. Primeiro acesso utiliza contexto finalístico assinado RF24, sem usuário provisório. Criação definitiva atômica aplica dados/idade/menor. Menor Google fica PENDENTE_CONSENTIMENTO e não ganha sessão antes da decisão real; Google-only não ganha senha local.

## 17. Token RF26

**Representável SIM.** SecureRandom, 32 bytes = 256 bits, Base64 URL sem padding (43 caracteres); somente SHA-256 hex64 em token_verificacao. Finalidade exclusiva, sem token de senha/refresh/JWT. Raw só no link de entrega ao destinatário, nunca em consulta/retorno/log de produção. Nenhuma alteração da geração preexistente foi necessária.

## 18. Expiração

RF26: **1 hora**, EmailVerificationService.VALIDADE, a partir de ultimo_reenvio_verificacao; template SMTP corresponde. RF27: **24 horas**, GuardianConsentService.VALIDADE e emissão no protocolo; template correspondente. São políticas técnicas preexistentes no código, não durações funcionais fixadas pela baseline nem configuráveis atualmente. Não inventamos novo prazo oficial.

## 19. Uso único

RF26 confirma hash sob PESSIMISTIC_WRITE e transação; valida estado/expiração, consome token, marca verificado e determina próximo estado. RF27 usa hash-prefixo parametrizado e lock no responsável; decisão consome token. Testes novos mantêm duas operações esperando lock físico e verificam vencedor único; nenhum schema adicional foi usado.

## 20. Reenvio RF26

Novo token substitui hash anterior sob lock; cooldown técnico1min e máximo5 reenviados, com contador e última emissão persistidos. Cota é reaberta conforme janela técnica24h derivada do último envio. Falha de SMTP restaura hash/contador/emissão anteriores; não se deixam dois tokens válidos. Reenvio não ativa conta.

## 21. Anti-enumeração

RF26/RF27 públicos retornam mensagens genéricas sem papel/idade/estado/existência/Google/contato. RateLimiter usa hash do e-mail e barreira uniforme, antes de consulta ao destinatário. É local/volátil (limite10.000 chaves, janela24h, cooldown1min, máximo5), sem Redis ou alegação de distribuição. Registros reais possuem controle durável adicional. Com JWT RF27, pedido para outro titular dá403 independentemente da existência de B; anônimo mantém fluxo público finalístico e envia somente ao contato já persistido. Tempo de resposta não foi certificado como constante.

Limite explícito de titularidade: o canal público por e-mail não comprova quem solicita; terceiro anônimo que conhece o endereço pode solicitar envio ao responsável já cadastrado. Isso não consulta/edita/autoriza B, mas impede certificar proibição absoluta de qualquer solicitação de reenvio por terceiro. A sessão de A é impedida de reenviar B; um eventual canal autenticado limitado de pendentes exige definição própria, sem liberar acesso normal.

## 22. Login bloqueado

AuthService.exigirAcessoNormal/GoogleAccountAccessPolicy continuam negando contas pendentes; UserDetailsServiceImpl reconsulta estado em cada autenticação JWT; refresh também aplica acessoNormal antes de emitir/rotacionar. Filtro/assinatura/claims/TTL/D15 não foram reimplementados.

## 23. Confirmação adulto

E-mail válido e não expirado de ARTISTA adulto/CONTRATANTE apto marca verificado e ATIVA conforme fluxo existente. Contratante menor e idade<14 não são ativados. Confirmação não emite JWT; login posterior segue credenciais/política normais.

## 24. Confirmação menor

ARTISTA de14–17, após RF26, fica PENDENTE_CONSENTIMENTO e recebe convite RF27 quando SMTP/registro permitem. E-mail verificado não é autorização. Falha do envio do convite não desfaz confirmação do e-mail nem ativa menor; reenvio segue disponível.

## 25. Troca de e-mail

**BLOCKER ESTRUTURAL.** Não existe representação durável para novo endereço+pedido/token/expiração/substituição preservando o confiável atual. Não implementamos pedido/confirmador improvisado. PUT /api/usuarios/me e /api/usuarios/{id} rejeitam mudança; e-mail equivalente case-insensitive não é reescrito. Nome/telefone/senha no mesmo pedido rejeitado não persistem parcialmente.

## 26. E-mail pendente

**Ausente.** Menor capacidade futura: pedido durável associado ao titular, novo endereço, hash de token de finalidade própria, validade/estado e invalidação do pedido substituído; credencial antiga preservada até confirmação transacional. A estrutura física será escolhida pelo pacote oficial, não por esta tarefa. Não usamos token_recuperacao, token_expiracao, telefone, bio, notificação, arquivo, mapa ou JWT para armazenar endereço pretendido.

## 27. Unicidade

usuarios.email UNIQUE preservado, CadastroValidator consulta ignore-case, contenção da edição consulta ignore-case para conflito com terceiro. Endereço ocupado dá409; mudança para disponível dá422 por falta de fluxo. Futuro confirmador deverá revalidar disponibilidade dentro da transação, além da solicitação inicial. Não foi implementado confirmador inexistente nem contornado UNIQUE.

## 28. Takeover/concorrência

JWT define o titular; ID alheio no legado dá403. Email confiável não muda após tentativa; login e recuperação continuam usando o antigo e novo endereço não ganha conta/credencial. Payload com usuarioId não altera terceiro. Pedidos de troca não são persistidos: não há dois endereços definitivos nem duas pendências improvisadas. Cenários positivos de confirmação de troca são bloqueados estruturalmente, não tratados como testes omitidos de fluxo entregue.

## 29. Telefone do titular

**CONCLUÍDO NO BACKEND neste recorte.** PUT /api/usuarios/me/telefone, JSON `{"telefone":"(11) 99999-9999"}`, JWT obrigatório, resposta204. Service bloqueia/recarrega usuário atual, reutiliza Bean Validation de CadastroDadosRequest.telefone e recalcula completude na transação. DTO usa a mesma constante TELEFONE, NotBlank e tamanho20. PUTs genéricos validam o mesmo formato. Não se aplica normalização nova, OTP, SMS/WhatsApp, flag de verificado ou RF27 ao telefone do titular.

## 30. Consentimento RF27

Consentimento inicial existente é funcional para ARTISTA14–17 após e-mail verificado, com nome/e-mail/telefone no registro atual, token de finalidade própria e decisão real. Não cria conta/sessão para responsável. Vínculo familiar obrigatório na baseline não pode ser persistido; portanto consentimento inicial do **contrato completo continua PARCIAL por C07**, embora o mecanismo representável seja consolidado. Adulto/CONTRATANTE não usa consulta finalística do responsável; nova inclusão pelo PUT fica contida.

## 31. Vínculo

**Ausente fisicamente.** Associação usuario_id identifica titular, não mãe/pai/tutor. Nenhum enum funcional foi inventado e nenhum valor foi guardado em nome/versao_termo/token. DTO de edição reconhece vinculoResponsavel somente para rejeitar uma solicitação não representável. Não anunciamos esse atributo como persistido nem solicitamos campo fictício ao frontend.

## 32. CPF do responsável

**NÃO coletado/exigido.** CPF no cadastro corresponde ao titular/entidade conforme RF01. Nenhum cpfResponsavel foi reintroduzido. A consulta titular finalística não retorna CPF/CNPJ.

## 33. Token do responsável

**Representável SIM para consentimento inicial.** 256bits SecureRandom/Base64url43; coluna token_consentimento recebe protocolo preexistente `v1:SHA256hex64:emissãoEpoch:tentativas`. Finalidade coerente na coluna de consentimento; tamanho cabe no varchar255. Validade24h derivada de emissão, comparação pelo hash parametrizado, consumo null na decisão. Não há episódios/versionamento para novo responsável. Raw não consta em consultas/logs; toString do DTO de decisão agora o omite.

## 34. Autorizar

Token válido -> usuário ARTISTA menor, pendente, e-mail verificado, responsável atual elegível; cadastro inicial exige subtipo e área principal. Transação/lock registra data, revogado=false, consome token e muda estado para ATIVA. PerfilCompleto RF08 não é falsificado pela autorização; privacidade de menor e RF44 permanecem. Token consumido não duplica efeito.

## 35. Recusar

Preservado fluxo existente: data da decisão, consentimento_revogado=true, token consumido, conta BLOQUEADA/sem acesso normal. Schema não possui enum de decisão negativa específico. Não criamos exclusão automática, banimento novo, prazo de espera ou tentativas ilimitadas. Política após recusa permanece pendência funcional; representação atual não é histórico completo.

## 36. Reenvio do responsável

POST /api/auth/resend-guardian-invite mantém resposta genérica e entrega somente ao responsável persistido. Novo hash substitui o antigo; emissão/tentativas no registro atual garantem cooldown1min/cota5 conforme janela técnica existente; mapa local adicional limita pedidos públicos uniformemente. Erro SMTP restaura registro anterior. Sessão autenticada de A não reenvia B (403 sem consulta ao destino). Pedido público anônimo é finalístico, não autorização/acesso a dados. Pending accounts não recebem acesso amplo apenas para reenviar.

## 37. Revalidação de e-mail do responsável

**BLOCKED C07/D05.** Uma linha de contato atual não preserva validado antigo + novo pendente com episódio/trilha. Edição relevante é rejeitada; dado original/autorização/estado permanecem porque nenhuma mudança foi aplicada. Não enviamos convite a endereço novo nem o tratamos como responsável validado.

## 38. Revalidação de telefone do responsável

**BLOCKED C07/D05**, mesma razão. Telefone do responsável não é o telefone do titular. Alteração exige consentimento conforme baseline e não pode ser simples UPDATE; pedido é rejeitado. Nenhum OTP foi criado.

## 39. Substituição de responsável

**BLOCKED C07/D05.** Mudança de nome/pessoa/vínculo deve invalidar autorização para o vínculo novo e produzir novo episódio. Mesmo e-mail/telefone não prova identidade. Não sobrescrevemos campos validados nem copiamos autorizado=true para nova pessoa; alterações nome/e-mail/telefone/vínculo são contidas nos dois PUTs.

## 40. Estado da conta na revalidação

Estado físico **PENDENTE_CONSENTIMENTO já existe** e impede acesso normal. Porém não há fluxo de revalidação entregue: pedido bloqueado não inicia revalidação e deixa estado anterior, pois vínculo não mudou. Testes modelam transição oficial com DML somente para provar integração da política atual. Não afirmamos que API inicia/reverte episódio inexistente.

## 41. JWT/refresh antigos

JwtAuthFilter verifica assinatura, ID/e-mail e carrega UserDetails; política consulta status persistido. Quando a conta fica pendente, JWT antigo não autentica API normal (401). Refresh respeita estado e não emite JWT/rotação para pendente (403 conforme política existente); login também403. Nenhum logout global novo por e-mail foi inventado; assinatura/claims/TTL/cookie/D15 permanecem. Sessões/blacklist não foram reimplementadas.

## 42. Responsável antigo × pendente

Database05 representa somente uma pessoa/contato corrente; não permite manter as duas versões necessárias com decisão associada. Esta lacuna é blocker, não erro resolvido por reset simples em campo atual. Alterações ficam bloqueadas; contato validado anterior preservado. Canal autenticado limitado de edição para conta já pendente também não existe; não abrimos APIs privadas para isso.

## 43. Trilha mínima

**Não representável para trocas/revalidações.** data_consentimento/versao_termo/flag documentam estado atual, não sequência de episódios, pessoa anterior, pedido novo, resultado por versão ou retenção. D05 precisa decidir evidência mínima e prazos, seguida da menor capacidade oficial. Não usamos logs técnicos como persistência funcional.

## 44. RF44

GuardianApplicationNoticeService resolve depois da ação/commit a candidatura/FKs, menor ATIVA com consentimento válido, contato persistido e e-mail distinto/validado. Integração preservada; teste novo muda estado para PENDENTE_CONSENTIMENTO antes da resolução e comprova nenhum envio ao contato novo como autorizado. A fixture DML não declara entregue a revalidação bloqueada. Não foi reimplementado catálogo de eventos, retry/outbox ou policy de destinatário antigo durante troca.

## 45. Privacidade

GET /api/usuarios/me/responsavel é privado e finalístico: nome/e-mail/telefone/dataConsentimento/consentimentoRevogado atuais, no-store, sem Entity/token/CPF/vínculo fictício. Identidade vem do JWT, nenhum ID/query concede titularidade. /me continua consulta privada compatível. RF10/RF37/RF45 mantêm whitelist: sem responsável, contatos privados, DOB, CPF/CNPJ, tokens ou experiência de menor. Não se adicionou dado a DTO público.

## 46. IDOR

Sem JWT, rotas privadas401; A não lê/edita perfil privado de B pelo ID (403), não altera telefone de B por payload e não reenvia convite de B com sua sessão. Consulta /me/responsavel ignora usuarioId e retorna apenas A. Decisão pública usa token; IDs não são autoridade. Reenvio público só solicita mensagem ao contato persistido, sem divulgar estado/dados nem aceitar destinatário arbitrário.

## 47. Logs

Inspeção de services/SMTP/handler: falhas de entrega logam classe de exceção, não payload/contato/token. Nenhum password/JWT/Authorization/cookie/CPF foi acrescentado a logs. GuardianDecisionRequest, TelefoneAtualizacaoRequest e ResponsavelAtualResponse omitem segredos/dados privados de toString. Validação retorna campo/mensagem, sem rejectedValue. Logs de testes podem conter dumps automáticos de MockMvc em tentativas falhas; são fixtures e não credenciais reais. Resultado final e revisão são reportados separadamente.

## 48. Concorrência

Novo ConfirmacoesConcorrenciaRf26Rf27IntegrationTest segura lock em terceira transação e observa pelo pg_stat_activity **duas transações efetivamente esperando Lock** antes de liberar. Cenários: email×email, reenvioRF26×confirmação antiga, autorizar×autorizar, autorizar×recusar e reenvioRF27×decisão. No máximo uma decisão/consumo; reenvio não reabre conta autorizada. Consulta/SELECT/fixtures DML usam somente tabelas oficiais; nenhum DDL/helper escondido. Troca de e-mail/substituição não são simuladas, pois estão bloqueadas.

## 49. Transações

RF26 confirmação/reenvio e RF27 decisão/reenvio já eram @Transactional e mantêm locks. UsuarioService trava/recarrega titular nos dois PUTs e no telefone finalístico; validações de e-mail/telefone/responsável antecedem mudanças. Rejeição impede persistência parcial, inclusive nome/senha/telefone no mesmo corpo. SMTP de confirmação/convite conserva política existente de envio na transação e restauração em falhas; não representa outbox durável. RF44 continua AFTER_COMMIT. Nenhum SQL estrutural foi criado.

## 50. Compatibilidade API

| Rota | Contrato nesta tarefa |
|---|---|
| POST /api/auth/cadastro, /google, /google/cadastro | Preservados; menor permanece pendente e Google não repete RF26 |
| POST /api/auth/confirm-email | Token finalístico; uso único/expiração; adulto ATIVA, menor PENDENTE_CONSENTIMENTO |
| POST /api/auth/resend-confirmation | E-mail, resposta genérica/cooldown/cota |
| POST /api/auth/guardian-invite, /guardian-decision | Token e decisão; sem ID arbitrário/JWT do responsável |
| POST /api/auth/resend-guardian-invite | Público genérico para pendentes; com JWT não aceita destino de terceiro |
| PUT /api/usuarios/me e /api/usuarios/{id} | Campos atuais compatíveis; telefone validado; email diferente422/ocupado409; alteração responsável422; propriedade preservada |
| PUT /api/usuarios/me/telefone | Novo, JWT, body telefone,204; sem OTP ou dados de terceiros |
| GET /api/usuarios/me/responsavel | Novo, JWT de ARTISTA14–17, resposta privada/no-store; não representa vínculo ausente |

Erro padrão mantido:400 payload/formato,401 ausente/inválido,403 titularidade/conta inapta conforme fluxo,404 token inexistente/usado,410 expirado,409 conflito real,422 operação bloqueada estruturalmente. Nenhuma tela/frontend foi ajustado; integração posterior deve impedir edição de campos bloqueados e utilizar contrato finalístico suportado.

## 51. Database alterado SIM/NÃO

**NÃO.** Database05 intacto e ZIP validado; nenhum enum/tabela/coluna/constraint/FK/índice/function/procedure/trigger/seed modificado. Database legado possui somente delta preexistente preservado por hash. Banco real da aplicação não foi usado para alterações desta tarefa; PostgreSQL de testes é descartável, inicializado pelo pacote oficial completo.

## 52. Frontend alterado SIM/NÃO

**NÃO pela tarefa.** frontend/, palco-comunidades-agenda/, HTML/CSS/JS/React preservados. Alterações antigas continuam no status; não alegamos working tree globalmente limpo. Contratos de integração posterior estão na seção50.

## 53. Migration/SQL SIM/NÃO

**NÃO.** Nenhum arquivo SQL/migration/patch/schema-test/DDL criado ou editado. Queries/locks/fixtures DML Java dos testes exercitam somente a estrutura oficial existente. ddl-auto=validate mantido; sem H2/create/update/create-drop.

## 54. Testes novos/alterados

Novos: DadosResponsavelRf26Rf27Rf53IntegrationTest (**34 execuções**) e ConfirmacoesConcorrenciaRf26Rf27IntegrationTest (**5**). AvisoResponsavelRf44IntegrationTest ganhou **1** cenário de pendência/contato novo sem aviso. PerfilEdicaoRf08IntegrationTest atualiza duas expectativas de edição insegura; GuardianConsentServiceTest adapta construtor à checagem de titular; JwtAuthenticationIntegrationTest contém mudança direta e mantém regressão de ID+subject após mudança persistida com DML. Nenhum teste/suíte antigo foi removido nem skip acrescentado.

## 55. Testes focados

**567 total / 567 passed / 0 failures / 0 errors / 0 skipped; 24 suítes; BUILD SUCCESS; exit 0; 312.6 s.** Início 08/10/2026 22:55:16; fim 08/10/2026 23:00:29. Resumo e log próprios: maven-focado-4-resumo.json / maven-focado-4.log.

24 classes reais: DadosResponsavelRf26Rf27Rf53IntegrationTest, ConfirmacoesConcorrenciaRf26Rf27IntegrationTest, EmailVerificationRf26IntegrationTest, GuardianConsentRf27IntegrationTest, GuardianConsentServiceTest, AuthControllerRf01Rf02IntegrationTest, CadastroRf01Rf24IntegrationTest, GoogleAuthRf24HardeningIntegrationTest, GoogleRegistrationContextServiceTest, PerfilEdicaoRf08IntegrationTest, PerfilTaxonomiaRf54Rf08IntegrationTest, PerfilPublicoRf10IntegrationTest, DescobertaPublicaRf37IntegrationTest, DescobertaFiltrosRf37IntegrationTest, CandidatosVagaRf45IntegrationTest, AvisoResponsavelRf44IntegrationTest, GenericEndpointsSecurityIntegrationTest, JwtAuthenticationIntegrationTest, SessoesSegurancaRf12Rf25Rf53IntegrationTest, SessoesConcorrenciaIntegrationTest, RecuperacaoSenhaRf09IntegrationTest, JwtRevogacaoInstanciaTest, PasswordPolicyTest, UsuarioServicePasswordPolicyTest.

Tentativas anteriores preservadas: (1) Docker desligado/sem acesso no sandbox; 28 registros Surefire,9 passaram/19 erros de bootstrap, depois JVM desta tentativa interrompido durante Mockito; não é validação de integração. Engine instalado iniciado normalmente, sem alteração de Defender/AppControl. Diagnóstico jcmd não conseguiu anexar ao JVM do sandbox e foi encerrado, sem alegar thread dump útil. (2) compilação interrompida por construtor de teste não atualizado: zero suítes executadas. (3) 567total/563passed/4failures/0errors/0skips: três cenários novos com parâmetro/status/fixture incorretos e um teste legado de edição de e-mail; corrigidos preservando asserções de segurança, sem relaxar produção. Logs/resumos separados não foram sobrescritos.

## 56. mvn test

**1855 total / 1838 passed / 0 failures / 0 errors / 17 skipped; 82 suítes; BUILD SUCCESS; exit 0; 756.6 s.** Início 08/10/2026 23:00:37; fim 08/10/2026 23:13:14. Log/resumo próprios: maven-completo.log e maven-completo-resumo.json.

Comando completo .\mvnw.cmd test, via Executar-Maven.ps1, JDK21.0.12. PostgreSQL18/Testcontainers, init.sql+seed.sql oficiais completos e ddl-auto=validate. Helpers oficiais, POM e resources não foram modificados. Captura XML por horário da execução impede misturar relatórios velhos.

## 57. Total/passed/failures/errors/skipped

**+40 execuções, nenhuma suíte antiga removida, nenhum skip alterado.** Somente contagens novas: DadosResponsavelRf26Rf27Rf53IntegrationTest 34; ConfirmacoesConcorrenciaRf26Rf27IntegrationTest 5; AvisoResponsavelRf44IntegrationTest 48 → 49. Demais contagens anteriores preservadas.

Baseline real do checkpoint:1815total/1798passed/0failures/0errors/17skipped. Aumento previsto e comprovável pelos XMLs: +34+5+1 = **40 execuções novas**. Os17 skips condicionais anteriores são4 CurrentLocalSchemaIntegrationTest (opt-in palco.current-db-tests) e13 OfficialLocalApiIntegrationTest (opt-in palco.official-db-tests). Comparação por classe/teste/skip registrada em regressao-comparacao.json, sem tratar opt-ins como sucesso.

## 58. Semgrep

**Semgrep 1.178.0, p/java, 388 arquivos, 60 regras, ~100.0% linhas parseadas, 0 findings / 0 blocking / 0 errors; exit 0; 46.2 s.** Todos os 14 arquivos Java do delta cobertos, missingJavaFiles=[]. Resultado é complementar à auditoria manual/testes; logs e JSON próprios não foram misturados com anteriores.

Workflow Docker existente, imagem oficial fixa semgrep/semgrep@sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b, p/java local com hash verificado; mount somente leitura, rede none, inclui main/test e fontes não rastreadas. Não usamos Semgrep nativo Windows nem desabilitamos TLS; não alteramos Defender/AppControl. **“0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.”**

## 59. Graphify

Inicial MCP:6888nós/24241arestas/373comunidades,91%extraído/9%inferido/0%ambíguo. Busca/vizinhos localizaram EmailVerificationService, GuardianConsentService, MenorAutorizadoPolicy, UserDetailsServiceImpl e GuardianApplicationNoticeService; código/schema decidiram as conclusões. **graphify update . concluído: AST-only, exit 0, 37,7 s; 6948 nós / 24588 arestas / 361 comunidades.** MCP final confirmou 91% extracted / 9% inferred / 0% ambiguous e reconheceu TelefoneAtualizacaoRequest no arquivo novo (L7).

Warnings preservados:45 arquivos SQL sem extração por tree_sitter_sql ausente;75 arquivos sem extensão/shebang suportado;96 comunidades renomeadas pelo hub, pois373 labels salvos divergem das361 comunidades atuais. Nenhum parser foi instalado e nenhum LLM labeling executado. graph.json, graph.html e GRAPH_REPORT.md derivados atualizados; grafo curado anterior foi salvo pela ferramenta. Extração semântica de documentos/rótulos não é alegada atual. Logs/metadados/MCP próprios: graphify-update.log, graphify-update-execucao.json e graphify-final-mcp.json.

Atualização solicitada somente AST, sem LLM labeling/extração semântica nova de documentos. Nenhum parser/ambiente Python foi instalado, nenhuma proteção Windows foi alterada e nenhum contorno de política aplicado. Warnings são registrados como limites, não ocultados.

## 60. Blockers

| Recorte | Blocker concreto | Capacidade mínima futura |
|---|---|---|
| RF26/RF53 troca de e-mail | Nenhum endereço/pedido pendente durável separado do e-mail confiável | Pedido titular finalístico com novo e-mail, hash/validade/estado, substituição e confirmação transacional/unique |
| RF27 inicial/RF01/RF24 | Vínculo familiar ausente | Atributo funcional durável, sem catálogo fechado inventado |
| RF27/RF53 revalidação/substituição | Uma pessoa/contato corrente; não separa validado e pendente com decisão associada | Episódio/pedido que preserve a autoridade antiga conforme política e segregue novo vínculo/consentimento |
| RF27 trilha/D05 | Sem histórico mínimo de troca/revalidação | Evidência por episódio com conteúdo/retensão/finalidade definidos, sem guardar dados antigos indefinidamente |
| Configurações de conta já pendente | Sem contexto autenticado limitado de edição | Definição de canal finalístico seguro; não liberar sessão normal ou API inteira |

Não há blocker novo para telefone titular ou geração/consumo de tokens iniciais. Nenhuma solicitação de mudança de banco foi executada; menor solução conceitual documentada para pacote oficial futuro.

## 61. Riscos

Troca de e-mail/revalidação ficam indisponíveis até capacidade oficial; integração visual deve apresentar esse limite. Consentimento inicial não fecha requisito de vínculo. Dados atuais não permitem história completa nem diferenciar todos os episódios de recusa/revogação. Rate limit uniforme local não garante restart/escala; controle durável limita registros reais, mas não substitui proteção distribuída de borda. SMTP dentro de transação RF26/RF27 não é fila durável/outbox; entrega e commit não são atomicidade distribuída. D15 de access-only local/volátil permanece exatamente como checkpoint anterior. Nenhuma ausência absoluta de vazamento/segredo/LGPD completa foi certificada.

## 62. Pendências

Resolver C07/D05 no menor pacote oficial; definir retenção/evidência de episódios; definir canal limitado para pendentes; decidir comportamento após recusa e destinatário RF44 durante troca quando o modelo existir; definir proteção operacional de abuso em múltiplas instâncias. RF48/RF22/integração frontend seguem tarefas próprias. Sem correção silenciosa de banco ou duração oficial inventada.

## 63. Conclusão RF26 cadastro

**CONFIRMADO/CONSOLIDADO no backend.** Pendência, hash/entropia/validade, uso único/concorrência, reenvio protegido/genérico, papel/idade, Google e integração RF27 foram auditados e testados. Limites operacionais da barreira local explicitados.

## 64. Conclusão RF26 troca de e-mail

**BLOCKED ESTRUTURALMENTE.** Não é implementável com segurança e durabilidade no database05 atual. Edição direta contida, credencial antiga preservada. Nenhum pedido stateless/memória/coluna alheia substitui a capacidade ausente.

## 65. Conclusão RF26 total

**PARCIAL.** Cadastro entregue não torna completa a troca posterior exigida pela baseline.

## 66. Conclusão RF27 consentimento

**FUNCIONAL/CONSOLIDADO no mecanismo inicial representável; PARCIAL no contrato completo por vínculo C07.** Autorizar/Recusar, expiração, uso único, reenvio, menor/Google e privacidade preservados. Não se afirma persistência de vínculo familiar inexistente.

## 67. Conclusão RF27 revalidação/substituição

**BLOCKED C07/D05.** Alterações relevantes são rejeitadas; responsável validado não é sobrescrito nem autorização antiga transferida. Estado pendente/JWT/refresh/RF44 já integram a restrição física, mas isso não representa entrega do produtor de revalidação.

## 68. Conclusão RF27 total

**PARCIAL por C07/D05.** Trilha mínima de episódios também bloqueada. Não existe conclusão integral segura sem vínculo e estado pendente/trilha representáveis.

## 69. Conclusão RF53 Dados Pessoais

**PARCIAL: telefone concluído; e-mail bloqueado estruturalmente.** Titular JWT, formato central, ownership/rollback/privacidade testados. PUTs inseguros contidos.

## 70. Conclusão RF53 Responsável

**PARCIAL: consulta titular segura entregue; alterações/revalidação/substituição bloqueadas C07/D05.** Sem atributo de vínculo fictício, CPF do responsável ou exposição pública.

## 71. Conclusão RF53 integral

**PARCIAL.** Segurança já concluída no checkpoint08b58f4 e preservada/regredida nesta tarefa. Privacidade/RF48 e Exclusão/RF22 não implementadas. Não promovemos requisito orquestrador completo por entrega de um recorte.

## 72. Próximo passo

Revisar capacidades mínimas de identidade/pendência/vínculo/episódios e política D05 com responsáveis pelo próximo pacote oficial, sem enviar mensagem ou gerar SQL automaticamente. Adaptar backend ao pacote homologado em tarefa posterior, com concorrência/ownership/rollback e integração frontend separada. Preservação final: **1282 arquivos iniciais,1272 fora do escopo Java intactos; protectedChanges=[], unexpectedNewFiles=[], javaAfterTestsChanges=[]; baseline/ZIP intactos; index idêntico/cached=[]; database05Diff=[]; frontendDiff=[]; git diff --check exit0, sem saída.** Deltas antigos de banco legado e 25 arquivos operacionais frontend preservados por hash; HEAD permaneceu 08b58f467b90f1f244b858570e70aeb9cdfced4b. Sem staging/commit/push. Evidência detalhada: preservacao-final.json, status-final.txt e diff-stat-final.txt.

## 73. Registro semanal — 08/10/2026

Objetivo: consolidar RF26/RF27 e recortes Dados Pessoais/Responsável RF53 sem alterar database05/frontend. Dependências:RF01/02/08/10/24/25/44. Backend: arquivos reais na seção9. Frontend:NÃO; Banco:NÃO; migrations/SQL:NÃO. Entregue somente o representável: preservação do cadastro/consentimento, telefone finalístico, consulta privada e contenção de edições sem confirmação/revalidação. Vínculo ausente, nenhum CPF do responsável, dados privados, nenhuma autorização reaproveitada para pessoa nova, estado atual respeitado por JWT/refresh/RF44. Testes reais nas seções55–59. C07/D05/troca de e-mail confirmados; próximo passo depende capacidade oficial/política mínima, sem fingir persistência com JVM.

# RF13 — Participação no Banco de Talentos do Contratante — 03/10/2026

**Estado: RF13 PARCIAL.** Entrada e consulta do vínculo estão implementadas e validadas. A notificação obrigatória ao CONTRATANTE permanece bloqueada pelo tipo oficial de notificação: database04 não oferece valor semanticamente compatível nem existe estratégia compatível consolidada. Nenhum tipo de candidatura, mensagem, convite ou salvo foi usado para disfarçar esse evento. Banco/frontend/documentos anteriores preservados; sem staging, commit ou push.

Checkout: `C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline`.
Branch: `integracao-recuperada-2026-09-15`; HEAD inicial `069b144`.
Evidências: `evidencias/rf13-2026-10-03/`.

## 1. Objetivo

Implementar somente a ação explícita do ARTISTA de entrar no Banco de um CONTRATANTE específico e consultar sua própria participação. Sem filtros/listagem RF17, matching, dashboard, convites, follow, candidatura, chat ou remoção.

## 2. Requisitos consultados

SHA-256 da fonte oficial consultada: `3e7ca8fa7e78a1967cf5f86c86f6a60f736879b02cfbd8f9d195040fea873183`.

Fonte oficial atual: `C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt`. RF13 integral (1170–1245); RF01, RF02, RF08, RF10, RF11, RF17, RF19, RF27, RF35, RF36, RF37, RF42, RF44; RNF02, RNF07, RNF08 e RNF09. Relatórios recentes RF37, RF45, RF06, RF01/RF24 e sincronização database04, e históricos RF13 final/adaptação de schema consultados.

RF08, RF13 e RF37 estão atualmente alinhados: Disponível para oportunidades = Não não remove o perfil da descoberta pública. A disponibilidade pode afetar elegibilidade/comportamento no Banco de Talentos, mas não a existência pública do perfil. “Pode limitar” não foi convertido em bloqueio obrigatório de entrada. As antigas premissas de snapshot, cadastro sem username e veto geral aos menores não são fonte superior ao contrato atual.

## 3. Estado inicial

Backend limpo contra HEAD, com working tree previamente sujo em frontend, README, SQL histórico, documentos/evidências e derivados. Manifesto antes da implementação: 1.151 arquivos, incluindo 569 frontend e 143 banco/SQL (classificações podem sobrepor-se). Status e HEAD preservados nas evidências. Nenhum delta anterior foi revertido ou atribuído a RF13.

Baseline RF37 documental: 989 total / 972 passed / 0 failures / 0 errors / 17 skipped; não é uma nova execução RF13.

## 4. Auditoria Graphify

A primeira tentativa estrutural foi verificar o catálogo para o Graphify MCP HTTP: nenhuma ferramenta Graphify estava exposta nesta sessão. Não foi possível consultar o servidor por esse conector; não se afirma uma chamada HTTP bem-sucedida. Serena foi consultado após ler suas instruções, mas `get_symbols_overview` falhou porque o language server manager não estava inicializado. Auditoria continuou pelas fontes atuais.

Mapeados TalentoController/Service/Repository, BancoTalentos/Id, Usuario, PerfilArtista/Contratante e respectivos repositories, PerfilPublicoService, MenorAutorizadoPolicy, JWT/política de acesso, SecurityConfig, NotificacaoPersistenceService, evento/listener e gateway SSE/WebSocket, testes antigos e novos e fronteiras RF17/RF42. Não iniciados graphify-mcp.exe, onboarding ou reparos de MCP/Python/App Control. Evidência: `auditoria-estrutural-ferramentas.json`.

## 5. Auditoria database04

Única fonte ativa: `database04/palco-database/`. Auditados enums, tabela/FKs/PK/check/índices, timestamp, as quatro funções do Banco, init, triggers/procedures e seed pertinentes. Nenhum trigger cria notificação ao inserir banco_talentos. Seed não contém participação no Banco; sua notificação demonstrativa é CANDIDATURA.

`fn_adicionar_banco_talentos` usa ON CONFLICT, mas retorna true também na repetição e só valida papéis, sem conta/publicabilidade/completude. A API usa DML parametrizado na mesma tabela para distinguir primeira criação. `fn_artista_no_banco_talentos` consulta EXISTS; `fn_remover_banco_talentos` existe no pacote, sem chamada/endpoint Java atual; preservada, sem ampliação. `fn_listar_banco_talentos` representa consulta de membros, fronteira RF17; não chamada nem modificada. O defeito histórico de retorno char(2)/varchar dessa função não foi reapresentado como nova execução.

## 6. Estrutura persistente real do Banco

Tabela `banco_talentos`: contratante_id bigint NOT NULL, artista_id bigint NOT NULL, data_adicao timestamp NOT NULL DEFAULT current_timestamp; PK composta (contratante_id, artista_id), FKs para usuarios com ON DELETE CASCADE e CHECK de IDs diferentes. Índices oficiais para ambos os IDs já existem. Sem coluna de status, sem unicidade global por artista e sem novo objeto SQL.

Entidades BancoTalentos/BancoTalentosId já mapeavam essa estrutura e foram conservadas. RF13 utiliza exatamente a relação oficial.

## 7. Implementação RF13 que já existia

Existiam entidade/mapeamento e funções oficiais do vínculo, sem repositório Java de participação, POST de entrada ou GET de estado do artista. TalentoController/Service/Repository implementavam busca profissional autenticada do CONTRATANTE e catálogos. Seu nome histórico RF13 não provava participação persistida.

A consulta profissional não restringe resultados a banco_talentos e exclui menores; essa dívida pertence à adequação RF17, não à operação de participação implementada. Os 42 testes antigos continuam validando seus comportamentos existentes.

## 8. Problemas encontrados

Ausência de operação explícita de entrada/estado; restrição genérica de SecurityConfig impedia POST e GET de ARTISTA nesse namespace; risco de confundir a busca global legada e Salvos com participação; ausência de tipo semanticamente válido para a notificação RF36. A relação e sua PK já eram adequadas, portanto não houve mudança estrutural.

A limitação da notificação foi explicitada antes de editar produção. Implementadas as partes independentes e permitidas; RF13 integral não foi declarado concluído.

## 9. Arquivos de produção alterados

Paths relativos ao checkout:

| Arquivo | Alteração |
|---|---|
| backend/src/main/java/com/portifolio/config/SecurityConfig.java | Duas permissões específicas GET/POST, somente ARTISTA, antes do matcher profissional |
| backend/src/main/java/com/portifolio/controller/BancoTalentosParticipacaoController.java — novo | Entrada e consulta no mesmo recurso |
| backend/src/main/java/com/portifolio/service/BancoTalentosParticipacaoService.java — novo | Ator, conta, consentimento, completude, alvo e transação |
| backend/src/main/java/com/portifolio/repository/BancoTalentosRepository.java — novo | INSERT idempotente e EXISTS bindados |
| backend/src/main/java/com/portifolio/dto/BancoTalentosParticipacaoRequest.java — novo | Confirmação explícita; extras não definem identidade |
| backend/src/main/java/com/portifolio/dto/BancoTalentosParticipacaoResponse.java — novo | Whitelist contratanteId/participante |

TalentoService/Repository, RF06/RF45/RF37, notificações/realtime, modelo SQL, perfil completo e política do menor não receberam edição.

## 10. Arquivos de teste alterados/adicionados

Novo `backend/src/test/java/com/portifolio/controller/BancoTalentosParticipacaoRf13IntegrationTest.java`: 57 casos executados, PostgreSQL real/Testcontainers/database04, sem DDL complementar. Nenhum teste preexistente alterado, desabilitado, removido ou convertido em skip.

## 11. Endpoints finais

| Operação | Endpoint | Sucesso |
|---|---|---|
| Entrar | POST /api/talentos/contratantes/{contratanteId}/participacao | 201 primeira inserção; 200 repetição válida |
| Consultar estado próprio | GET /api/talentos/contratantes/{contratanteId}/participacao | 200 |

Ambos exigem ARTISTA autenticado. Não há listagem completa, rota redundante de estado ou DELETE novo.

## 12. Payload/contrato

POST recebe `{"confirmado":true}`. Ausente/null: 400; false: 422. A ação precisa ser confirmada explicitamente pelo cliente; a jornada visual correspondente ainda não foi integrada. Campos extras, incluindo artistaId/usuarioId, são ignorados com segurança, como no padrão de DTO de atualização existente; nunca definem ator.

Resposta: `{"contratanteId":123,"participante":true}`; GET antes da entrada retorna participante=false. Somente dois campos, sem Entity, responsável, consentimento, experiência, dados pessoais ou notificação simulada. O contratanteId da rota identifica exclusivamente o alvo.

## 13. Autenticação

JwtAuthFilter valida assinatura/validade e revalida subject/e-mail + ID persistido por UserDetailsServiceImpl. Token inválido/ausente ou conta sem acesso normal não autentica: 401. Nenhum filtro, emissor JWT ou política global foi reescrito.

## 14. Autorização

SecurityConfig libera somente GET/POST do recurso de participação para ROLE_ARTISTA; outros métodos permanecem negados pelo namespace vigente. Service também exige o papel ARTISTA persistido. CONTRATANTE/ADMIN/MODERADOR recebem 403; o contratante não insere artista arbitrário.

## 15. Identidade pelo JWT

AuthenticatedUserResolver.usuarioAtual deriva a identidade do SecurityContext e reconsulta usuarios pelo subject autenticado. O filtro já vincula o subject ao ID assinado antes disso. Requests adulterando artistaId em body/query criam somente a relação do ator autenticado; outro artista consultando o mesmo alvo recebe seu próprio estado, sem acesso ao vínculo alheio.

## 16. Conta apta

Exige status ATIVA, acessoNormalPermitido da GoogleAccountAccessPolicy vigente e proteção RF27 quando aplicável. JWT antigo não prevalece sobre estado/papel persistido. Quatro estados não aptos testados recebem 401 na fronteira JWT; a defesa de negócio no service recebe 422 se a conta deixar de ser apta após autenticar.

## 17. Perfil completo

POST consome apenas Boolean.TRUE.equals(usuario.getPerfilCompleto()), a flag oficial RF08/database04, e existência do perfil ARTISTA correspondente. Não recalcula nem introduz segunda fórmula. Incompleto/ausente: 422, sem relação.

Sem exigência adicional de avatar, foto, portfólio, raio, seguidores ou candidatura. Teste mantém a flag true e remove foto/URL/raio para provar que RF13 não impõe requisitos adicionais. GET não exige completude e não apaga relação quando a flag se torna false.

## 18. Validação do contratante alvo

Reutilizada Specification MenorAutorizadoPolicy.perfilPublicavel(CONTRATANTE,id), a mesma construção de publicabilidade RF10/RF37: conta ATIVA, papel CONTRATANTE, adulto e perfil correspondente. Não exige completude do alvo, que não é critério RF10/RF37.

Inexistente, papel diferente/interno, menor, perfil ausente ou estado não publicável: 404 com a mesma mensagem genérica. Não expõe causa privada nem existência da conta. Alvo posteriormente privado permanece com vínculo armazenado, mas consulta/entrada devolvem 404.

## 19. Definição de participação

Uma linha do par específico contratante/artista com data de adição do PostgreSQL. O mesmo artista pode entrar em Bancos diferentes. Não existe vínculo global nem entrada automática por descoberta, salvar, seguir, candidatura ou acesso ao perfil.

## 20. Unicidade

PK oficial (contratante_id,artista_id) é a barreira física. INSERT ON CONFLICT do nothing usa exatamente esse par; não utiliza exists+save nem captura SQL bruto. Nenhum UNIQUE ou índice adicionado.

## 21. Concorrência

Três repetições de teste com duas requisições, duas threads e barreira de início em PostgreSQL real. Em cada corrida: uma 201 e uma 200, exatamente uma linha; nenhum 500. A constraint e o ON CONFLICT coordenam transações inclusive entre instâncias da aplicação; não foi necessário advisory lock/synchronized.

## 22. Semântica de repetição/idempotência

Não havia contrato HTTP anterior de entrada a preservar. Escolhido 200 para repetição válida do mesmo par, com o mesmo estado; 201 apenas quando INSERT afeta uma linha. Timestamp original permanece. Guards de conta/completude/alvo/confirmacao continuam aplicados ao POST; um vínculo histórico não libera entrada com condições hoje inválidas.

Não existe segunda relação ou efeito secundário. Como a primeira notificação está bloqueada, zero notificações não é prova de uma cadeia RF36 RF13 completa.

## 23. Notificação RF36

**PENDENTE/BLOQUEADA POR COMPATIBILIDADE SEMÂNTICA DO BANCO.** `01_types/01_enums.sql` e TipoNotificacao Java contêm apenas CANDIDATURA, MENSAGEM, CONVITE, EDITAL e SALVO. `notificacoes.tipo_notificacao` é NOT NULL; não há tipo genérico, valor Banco, estratégia consolidada ou trigger correspondente.

A infraestrutura existente suporta persistirNaTransacaoAtual (MANDATORY), persistir (REQUIRES_NEW), eventos persistidos e entrega AFTER_COMMIT, mas exige esse enum. Não foram criados tipos Java fictícios, valores nulos, notificações em memória ou aliases de significado incorreto. A participação é persistida, porém **não gera notificação ao contratante**, inclusive na primeira entrada.

Menor mudança estrutural a avaliar em pacote oficial futuro: incluir tipo próprio compatível, por exemplo BANCO_TALENTOS, mediante decisão/aprovação do grupo. Depois adaptar enum Java, persistir apenas para INSERT real, publicar NotificacaoPersistida para entrega AFTER_COMMIT e testar criação/repetição/corrida/rollback/transporte. Nenhuma mudança foi aplicada ou autorização de banco inferida.

## 24. Falha realtime

Infraestrutura existente captura falhas de gateway/WebSocket/SSE após commit; sua regressão foi executada. RF13 não gera evento RF36 enquanto não existir tipo compatível, portanto o teste de primeira notificação e o cenário de falha realtime **específicos do RF13 permanecem pendentes**. Não se apresentou o teste genérico do listener como prova de uma entrega RF13 inexistente.

A participação não depende de broker/SMTP nem é revertida por um transporte que não é chamado. Não foi criado aviso RF44 de Banco: RF44 atual prevê publicação/candidatura e não expressa obrigação dessa ação.

## 25. Consulta de estado

GET utiliza o mesmo ator/conta/proteção do menor e alvo público, então EXISTS por chave composta. Somente estado do artista autenticado. False antes, true depois; outro artista recebe false. Consultas repetidas preservam linhas/timestamps/contagens e não enviam eventos. Perfil incompleto permite consulta de vínculo atual se a conta continuar apta; conta/consentimento/alvo incompatíveis bloqueiam a leitura sem apagar histórico.

## 26. Disponibilidade para oportunidades

Não integra o guard RF13 nem é modificada. False permite entrar, preserva vínculo e descoberta/detalhe público RF37/RF10. Testados false antes da entrada e true→false depois. Elegibilidade/filtro profissional específico fica para RF17; nenhuma remoção automática ou alteração RF37.

## 27. Menor autorizado

ARTISTA de 14/17 anos, ATIVA, flag completa e responsável normalizado com data de consentimento e sem revogação pode entrar. Reutilizada MenorAutorizadoPolicy, sem novo consentimento. Sem responsável, data pendente, revogação ou idade abaixo de 14: bloqueado. Revogação posterior bloqueia novas entradas/consulta sem apagar a relação.

Whitelist de resposta não expõe responsável, consentimento, idade ou experiência; o registro do consentimento permanece igual após participar.

## 28. Separação RF13 x RF17

Novo fluxo cria/consulta a relação oficial; não corrige filtros/listagem gerencial/matching. A busca profissional legada ainda é global e exclui menores, inclusive participantes autorizados; seu nome histórico e os 42 testes verdes não tornam RF17 concluído. RF17 futuro deve consumir banco_talentos e consolidar elegibilidade profissional conforme seus requisitos.

## 29. Separação RF13 x RF19/RF41

Participar não cria item salvo nem follow. UI legada OfficialTalentBank usa /salvos no favorito, relação independente; não foi convertida em participação. Teste compara contagem das 43 tabelas públicas antes/depois: apenas banco_talentos ganha linha. RF41 não foi implementado; não foi criada tabela alternativa de follow.

## 30. Separação RF13 x RF06/RF42

Não cria candidatura, convite ou conversa. RF42 ainda não tem implementação de convite de vaga encontrada no backend; o vínculo fornece a base persistente, sem antecipar esse RF. RF06/RF45/chat mantiveram regressões e código. Nenhuma sala/evento é gerado automaticamente.

## 31. Remoção/saída

**NÃO criada.** DELETE/PUT/PATCH do recurso recebem 403 e deixam a linha intacta. A função SQL oficial de remover e as cascatas existentes não foram apagadas nem ampliadas; nenhum endpoint/fluxo Java atual chama essa função. Saída funcional depende de requisito específico futuro.

## 32. Erros HTTP

| Cenário | HTTP |
|---|---:|
| Primeira entrada / repetição válida / estado | 201 / 200 / 200 |
| Anônimo, JWT inválido ou conta bloqueada no filtro | 401 |
| Papel não ARTISTA / métodos não liberados | 403 |
| Contratante inexistente/inacessível | 404 genérico |
| Confirmação ausente/null ou ID malformado | 400 |
| Confirmação false, perfil incompleto/ausente, consentimento/conta incompatível no service | 422 |

Handler global existente; sem stacktrace/SQL/dados sensíveis. Não foi criado 409 artificial para uma repetição bem-sucedida e idempotente.

## 33. Transações

POST @Transactional; JPA e NamedParameterJdbcTemplate utilizam o DataSource/gerenciador do projeto. Teste injeta falha de negócio após INSERT real e confirma rollback da linha. GET @Transactional(readOnly=true), sem DML/eventos. ON CONFLICT preserva transação concorrente sem erro de unicidade.

Controle das evidências: 128 arquivos textuais conferidos, zero JWTs assinados e zero valores locais conhecidos de JWT_SECRET/DB_PASSWORD; propriedades de runtime retiradas somente das cópias XML. Isso é conferência restrita de evidências, não scan secrets completo (`evidencias-privacidade.json`).

Atomicidade participação + notificação futura não é declarada implementada. Será validada quando a notificação tiver representação oficial compatível.

## 34. Queries/performance

Queries novas com valores bindados, sem SQL concatenado de entrada cliente. Estado por EXISTS na PK; INSERT único, sem carregar coleção do Banco. Não há processamento/N+1 por membros nem paginação em memória nova. Reutiliza lookup público do alvo e lookup do ator; sem índice adicional. A complexidade depende do plano/índice PostgreSQL, aproximadamente lookup por chave, sem alegar O(1) absoluto ou benchmark de carga.

## 35. Banco alterado: NÃO

Nenhuma edição em SQL, database04/database02/histórico, enum, tabela/coluna, constraint, índice, trigger/function/procedure, init, seed ou migration. ddl-auto=validate mantido. DML/limpeza de fixtures somente em PostgreSQL descartável/Testcontainers; banco de desenvolvimento não operado.

Conferência explícita inicial/final ZIP → pacote: **46/46 idênticos, zero divergências e zero adicionais**. ZIP SHA-256 `52b1c4af06d79a7efae47e6fa320b4a0a32359efaae1d40e2a73d06129c6df2b`; fingerprint oficial `db8f05cabadfd3935b7c703b7b21252f170fa529c0d30e7e61755b46d7fd5f39`. Manifesto confirma os 143 paths banco/SQL preservados; nenhuma inferência só por Maven verde.

## 36. Frontend alterado: NÃO

569 arquivos frontend preexistentes preservados por hash; nenhum npm/test/build/redesign. Profile.jsx/ProfileShell e OfficialTalentBank lidos para contrato. A tela profissional do contratante chama /talentos e favoritar usa /salvos; não existe integração do POST/GET de participação e confirmação do artista auditada nesses componentes. Integração futura em Profile.jsx/ProfileShell precisa consumir o novo recurso, em tarefa própria autorizada.

O `.env.example` da árvore palco-comunidades-agenda já estava modificado no status inicial, com última escrita de 21/09/2026, e ficou fora do manifesto herdado que excluía arquivos .env. Sua preexistência e hash adicional foram registrados e reconferidos, sem atribuí-lo ao delta RF13.

Recursos existentes copiados por Maven para target não editam as origens. Working tree anterior mantido.

## 37. Testes novos

57 casos na nova classe: identidade/papéis/JWT antigo; completude/alvo; confirmação/body adulterado; estado antes/depois e de outro ator; múltiplos Bancos; idempotência/timestamp; três corridas; rollback; indisponibilidade antes/depois; proteção do menor; whitelist; consultas sem mutação; ausência de remoção; ddl-auto/PK reais e isolamento das tabelas.

Dos 38 critérios solicitados, **19 (notificar primeira entrada) e 20 (falha realtime RF13) permanecem pendentes** pelo enum oficial. Critérios 16/repetição e 17/concorrência verificam ausência de duplicação/efeitos indevidos, sem alegar primeira notificação funcional. Ausência de notificações semanticamente falsas é uma guarda de integridade, não substitui o critério 19.

## 38. Testes alterados

Nenhuma classe antiga alterada; os 42 casos legados TalentoRf13IntegrationTest preservados, assim como condições dos 17 skips. Nova fixture completada via OfficialSchemaFixtures/função oficial, sem alteração de schema.

Diagnósticos preservados: primeira compilação falhou por chamada currentUser inexistente (corrigida para usuarioAtual), 14,687 s, sem executar testes; primeira bateria executada 441 total/440 passed/1 failure/0 errors/0 skips, 04:22 min, por colisão de usernames na busca parcial RF37. Corrigida a fixture colega, sem modificar RF37 ou enfraquecer assertions; acrescentados dois cenários de preservação posterior. Resultados finais abaixo são separados dos diagnósticos.

## 39. Bateria focada

**443 total/443 passed/0 failures/0 errors/0 skipped, BUILD SUCCESS, exit 0**, 19 classes, **03:58 min**, término **2026-10-03T14:36:05-03:00**.

| Classe focada | Passed/total |
|---|---:|
| BancoTalentosParticipacaoRf13IntegrationTest | 57/57 |
| CandidatosVagaRf45IntegrationTest | 40/40 |
| CandidaturaControllerRf06IntegrationTest | 57/57 |
| ChatRf24IntegrationTest | 15/15 |
| DescobertaPublicaRf37IntegrationTest | 71/71 |
| GuardianConsentRf27IntegrationTest | 10/10 |
| NotificacaoRf23IntegrationTest | 11/11 |
| PerfilEdicaoRf08IntegrationTest | 28/28 |
| PerfilPublicoRf10IntegrationTest | 16/16 |
| SalvoRf19IntegrationTest | 25/25 |
| TalentoRf13IntegrationTest | 42/42 |
| Database04BootstrapIntegrationTest | 5/5 |
| NotificacaoEventoListenerTest | 1/1 |
| NotificacaoSsePoolIntegrationTest | 1/1 |
| NotificacaoWebSocketRf23IntegrationTest | 3/3 |
| GenericEndpointsSecurityIntegrationTest | 26/26 |
| JwtAuthenticationIntegrationTest | 4/4 |
| GuardianConsentServiceTest | 3/3 |
| PerfilCompletoServiceTest | 28/28 |
| **Total** | **443/443** |

Comando no backend: `.\mvnw.cmd '-Dtest=BancoTalentosParticipacaoRf13IntegrationTest,TalentoRf13IntegrationTest,PerfilEdicaoRf08IntegrationTest,PerfilCompletoServiceTest,PerfilPublicoRf10IntegrationTest,GuardianConsentRf27IntegrationTest,GuardianConsentServiceTest,DescobertaPublicaRf37IntegrationTest,SalvoRf19IntegrationTest,NotificacaoRf23IntegrationTest,NotificacaoEventoListenerTest,NotificacaoWebSocketRf23IntegrationTest,NotificacaoSsePoolIntegrationTest,GenericEndpointsSecurityIntegrationTest,JwtAuthenticationIntegrationTest,Database04BootstrapIntegrationTest,CandidaturaControllerRf06IntegrationTest,CandidatosVagaRf45IntegrationTest,ChatRf24IntegrationTest' '-Dspring.test.mockmvc.print=NONE' test`.

Consolidação dos 19 XMLs selecionados e comparação com resumo Maven; cópias sem properties de runtime. Evidências focados.log/.exit, focados-totais.json e focados-reports/.

## 40. Maven completo

**1.046 total/1.029 passed/0 failures/0 errors/17 skipped, BUILD SUCCESS, exit 0**, **62 classes**, **06:47 min**, término **2026-10-03T14:46:13-03:00**.

Comando `.\mvnw.cmd test '-Dspring.test.mockmvc.print=NONE'`, sem clean, após carregar scripts/database04/environment.ps1 e PALCO_TEST_DATABASE04_PATH. Flag somente suprime dumps MockMvc; não desabilita assertions. JDK 21.0.11, Spring Boot 4.0.6, Testcontainers 2.0.5, postgres:18-alpine, database04 oficial, validate.

Docker Desktop já instalado estava desligado; iniciado em segundo plano conforme autorização. Sem H2, reinstalação ou alteração de schema. Conferidos somente XMLs da execução atual no diretório target-maven/database04/surefire-reports. Evidências maven-completo.log/.exit, completo-totais.json e completo-reports/.

## 41. Total/passed/failures/errors/skipped

| Execução | Total | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Focados finais | 443 | 443 | 0 | 0 | 0 |
| Maven completo final | 1046 | 1029 | 0 | 0 | 17 |

A nova classe RF13 adiciona 57 casos ao baseline RF37 de 989. Soma dos XMLs coincide com os resumos Maven.

Os mesmos 17 skips condicionais: CurrentLocalSchemaIntegrationTest 4, OfficialLocalApiIntegrationTest 13. Não são cobertura executada nem foram alterados para obter verde. RF06 histórico permanece 867/850/0/0/17, e RF37 histórico 989/972/0/0/17; os relatórios antigos não foram modificados.

## 42. Semgrep

Imagem Docker oficial fixada em digest sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b. Versão 1.178.0; ruleset p/java, **266 arquivos/60 regras/~100% de linhas parseadas/0 findings/0 blocking/0 erros/exit 0**, 244,14 s. Inclusão dos seis arquivos de produção RF13 conferida em paths.scanned.

Ruleset local reutilizado do cache RF37, com proveniência de download Windows TLS verificado registrada no RF45 e SHA-256 5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1. Checkout montado somente para leitura. Não usada instalação nativa nem desativado TLS; nenhum código alterado para corrigir ferramenta. Nenhum novo scan secrets declarado.

**0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.** Evidências semgrep-java.json/.exit, semgrep-java-saida.log e semgrep-resumo.json.

## 43. Graphify

**graphify update . concluído, exit 0**, extração AST sem LLM; estado final **5.787 nós/19.214 arestas/337 comunidades**, **28,82 s**, incluindo os dois cenários finais de preservação. A primeira atualização anterior também terminou com exit 0 (5.785/19.202/322); seus logs foram preservados separadamente. Evidências graphify-update.log/.exit e graphify-execucao.json.

MCP não exposto na auditoria inicial; esse limite não foi disfarçado como sucesso HTTP. graphify-mcp.exe/graphify label não executados, sem configuração MCP/Python/Defender/App Control ou contorno de segurança. Derivados graphify-out/cache/backup são próprios da atualização, não código de produção nem staging automático.

## 44. Riscos

Primeira entrada não notifica CONTRATANTE até existir tipo compatível: RF13 integral incompleto. A UI profissional legada continua global e exclui menores; não representa automaticamente RF17 nem a ação de participação. Integração visual ausente. Alterações concorrentes de publicabilidade/estado entre a validação e o commit não são serializadas globalmente; a unicidade do par é protegida no PostgreSQL.

Plano/carga, SMTP real, entrega RF13 em produção e todos os demais RFs não homologados. Working tree amplo exige seleção específica em eventual checkpoint futuro, sem staging global.

## 45. Limitações

Maven/Semgrep verdes validam o código implementado, sem suprir o requisito de notificação bloqueado. Sem novo endpoint de saída, filtros/gerência RF17 ou convite RF42. Os 17 skips de integração local continuam condicionais. Auditoria inicial estrutural pelas fontes devido indisponibilidade de MCP/Serena; atualização CLI final registrada separadamente.

## 46. Pendências

Decisão funcional e pacote oficial com tipo de notificação compatível, antes de adaptar Java/gerar RF36 somente na primeira inserção; testes RF13 19/20 correspondentes. Nenhum schema foi improvisado. Integrar confirmação e estado no perfil do contratante em tarefa frontend própria. Adequar consumidor RF17 à relação persistida e à política atual de menor autorizado, sem reaproveitar como autoridade as premissas históricas.

## 47. Conclusão

**RF13 PARCIAL.** Concluídas entrada explícita e consulta própria de participação, JWT/papéis/conta/completude, alvo RF10/RF37, Banco específico, unicidade/concorrência, transação, idempotência, menor autorizado, disponibilidade e separação dos outros RFs. Notificação obrigatória não implementada porque o database04 não oferece representação semântica compatível. Não se declara RF13 concluído por causa dos testes verdes.

Banco/frontend/SQL/documentos anteriores preservados. Sem remoção, staging, commit ou push. git diff --check: **exit 0**, sem erros de whitespace na reconferência de fechamento.

## 48. Próximo passo recomendado

Revisar o delta RF13 e a pendência da seção 23. O menor desbloqueio exige decisão do grupo sobre o tipo oficial e uma autorização específica para a entrega de banco, antes de qualquer alteração estrutural. Depois concluir persistência/after-commit e cenários RF13 de notificação/realtime; integrar o frontend em tarefa autorizada distinta. RF17 continua sua própria implementação.

## Registro pronto para consolidação semanal

**Data:** 03/10/2026.
**Objetivo:** auditar RF13 e implementar participação específica no Banco do contratante.
**RF/RNF trabalhado:** RF13; dependências RF01/RF02/RF08/RF10/RF27/RF36/RF37; fronteiras RF11/RF17/RF19/RF35/RF42/RF44; RNF02/RNF07/RNF08/RNF09.
**Backend alterado:** POST/GET de participação, service/repository/DTOs e permissões ARTISTA específicas.
**Frontend alterado:** NÃO. **Banco alterado:** NÃO.
**Funcionalidades concluídas/avançadas:** entrada explícita, estado próprio, relação específica, idempotência/concorrência, menor autorizado e preservação da disponibilidade/histórico; RF13 integral PARCIAL.
**Bugs corrigidos:** ausência de operação de participação/estado, confusão entre explorador legado e vínculo; fixture de teste ajustada à busca parcial vigente.
**Decisões técnicas:** PK oficial + ON CONFLICT, flag de completude existente, política pública e do menor compartilhadas, 201/200, nenhuma remoção nem tipo de notificação indevido.
**Segurança/autorização:** ator JWT, papel/estado persistidos, confirmação explícita, alvo público, binding, whitelist de dois campos.
**Testes/resultados:** focados 443/443, 0 failures/errors/skips, BUILD SUCCESS (03:58 min); completo 1046/1029/0/0/17, BUILD SUCCESS (06:47 min); 57 casos novos, três corridas aprovadas. Graphify CLI final exit 0 (5.787 nós/19.214 arestas). Semgrep 266/60/0 findings/0 erros/exit 0. Pacote 46/46 idêntico; fronteiras preservadas.
**Pendências:** tipo oficial de notificação RF13/RF36 e testes correspondentes, integração frontend e consumidor RF17.
**Próximos passos:** decisão estrutural oficial, concluir notificação somente após autorização e integrar visual em tarefa própria.


# RF35 — Sistema de mensagens

Registro solicitado: 03/10/2026. Execução/fechamento: 04/10/2026, America/Sao_Paulo.

## 1. Objetivo

Auditar o chat existente e corrigir somente lacunas do backend RF35 usando database05 oficial, com testes reais, sem alterar SQL, banco de desenvolvimento ou frontend. Situação final: **PARCIAL**, pela dependência funcional RF22 descrita abaixo.

## 2. Requisitos e fontes

Fonte oficial atual: `C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt`, SHA-256 `3e7ca8fa7e78a1967cf5f86c86f6a60f736879b02cfbd8f9d195040fea873183`. RF35, RF22, RF27, RF36, RF42, RF45, RF06, RF18 e RNF02/05/06/07/08/09/10/17 consultados. Relatórios recentes RF42, RF45, RF06, RF13/RF36 e sincronização database05 usados como contexto e revalidados pelas fontes atuais.

A matriz de [76 critérios](evidencias/rf35-2026-10-03/criterios-aceitacao.md) distingue teste local do histórico anônimo e exclusão real orquestrada de conta. Históricos antigos não foram reescritos.

## 3. HEAD/checkpoint

Checkout `C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline`, branch `integracao-recuperada-2026-09-15`; HEAD/upstream `a4ec8393caa4dde8cc58d10eac512c0aff40de96` (`feat: implementa convite para vaga RF42`). RF13/RF36 e RF42 estavam checkpointados, sem alterações soltas no backend/database05/scripts ativos; index vazio. Estado inicial global sujo: README, SQL histórico e frontend já modificados, além de arquivos não rastreados históricos. Esses deltas não foram atribuídos a RF35 nem restaurados.

Evidências: `checkpoint-inicial.json`, `git-status-inicial.txt`, `git-log-inicial.txt` e `arquivos-iniciais.json` no diretório `evidencias/rf35-2026-10-03`. Manifesto inicial: 1417 arquivos; 1408 protegidos, excluídas somente nove alterações existentes autorizadas.

## 4. Auditoria Graphify inicial

Graphify MCP HTTP `http://127.0.0.1:8765/mcp` acessível. Grafo inicial: 5971 nós. Consultas 502/505 truncadas em 36/42 e 34/56 nós; 503/504 completas com 31/43 e 17/18 nós/arestas. Respostas integrais salvas. Dependências: chat, DTOs/repositories, JWT/STOMP, eventos/listeners, notificações, menor/consentimento, RF42 e denúncias. O grafo inclui SQL histórico; arquivos reais decidiram a fonte ativa. Não foi executado `graphify label`.

## 5. Database05

Snapshot oficial ativo: `database05/palco-database`; PostgreSQL real/Testcontainers e `PALCO_TEST_DATABASE05_PATH`, `ddl-auto=validate`. `mensagens_chat` já dispõe de `editada`, `data_edicao`, `texto_original`, `excluida`, `data_exclusao` e `remetente_id` nullable com `ON DELETE SET NULL`. A participação possui cascade da conta. Nenhum blocker estrutural desses itens do chat foi confirmado. Campo `url_anexo` comporta referência opaca privada, sem nova tabela/coluna.

## 6. Estado inicial RF35

Classificação anterior às edições registrada em [auditoria-inicial.md](evidencias/rf35-2026-10-03/auditoria-inicial.md): A = concluído e preservado; B = parcial; C = ausente; D = incompatível; E = impedimento estrutural.

| Parte | Inicial | Ação |
|---|---|---|
| Sala/reuso concorrente, texto, páginas/fila privada | A | Preservados e regredidos |
| Ownership/current account/menor | A/B | Guard reforçado por operação |
| Par apenas com papéis diferentes | D | ARTISTA ↔ CONTRATANTE explícito |
| Notificação MENSAGEM | B | Persistência atômica na transação da mensagem |
| Leitura | A/B | Lote de até 50 recebidas por chamada |
| Edição/exclusão | B | Clock, flags/datas e preservação de reporte prévio |
| Anexos privados | C | Upload/download/validator/storage privado |
| Histórico após remoção | D | LEFT JOIN/identidade anônima/participante remanescente |
| Exclusão orquestrada RF22 | C | Contenção mantida, integração real pendente |
| Schema do chat | Sem E | Estrutura atual suficiente para alterações locais |

## 7. Funcionalidades existentes preservadas

REST para sala, histórico, envio, leitura, edição e exclusão; identificação por Principal; criação transacional com dois participantes; par ordenado/advisory lock PostgreSQL; paginação padrão 20/máximo 50; ordenação timestamp/ID; projeções de sala; texto literal até 4000; filas privadas STOMP e infraestrutura WS/SSE/notificações existente. Regressões existentes foram mantidas sem retirar asserts ou introduzir skips.

## 8. Gaps encontrados

ADMIN/MODERADOR admitidos pela mera diferença de papéis; guard da abertura insuficiente para operações futuras; notificação persistida em outra transação após commit; edição/exclusão não preenchiam trilha oficial; anexos inexistentes; joins descartavam remetente nulo e exigência de dois participantes impedia arquivo histórico; marcação de leitura coletava IDs ilimitados. RF22 real permanece indisponível, independentemente das FKs favoráveis.

## 9. Arquivos de produção

Nove Java: `ChatController`, `ChatService`, novo `ChatAnexoStorage`, `ChatMensagemResponse`, `ChatMensagemProjection`, `MensagemChatRepository`, `ParticipanteChatRepository`, `DenunciaRepository` e `WebSocketConfig`. Sem alteração nas entidades/SQL, segurança de exclusão RF22, RF42/RF45/RF06 ou infraestrutura compartilhada de notificações.

| Arquivo em `backend/src/main/java/com/portifolio/` | Alteração |
|---|---|
| `controller/ChatController.java` | Upload e download autenticado |
| `service/ChatService.java` | Guards atuais, atomização do alerta, trilha, leitura limitada, DTO anônimo/privado |
| `service/ChatAnexoStorage.java` (novo) | Referência UUID/sala, storage privado, limites, links e operações de arquivo |
| `dto/ChatMensagemResponse.java` | editada/dataEdicao |
| `repository/projection/ChatMensagemProjection.java` | Flags/datas em projeção |
| `repository/MensagemChatRepository.java` | Histórico com remetente nulo, lock de alteração e leitura por IDs limitados |
| `repository/ParticipanteChatRepository.java` | Sala remanescente, preview excluído e contagem anônima |
| `repository/DenunciaRepository.java` | Existência de reporte MENSAGEM prévio |
| `config/WebSocketConfig.java` | Handshake sem query |

## 10. Arquivos de teste

Novo `backend/src/test/java/com/portifolio/controller/ChatRf35IntegrationTest.java`; ampliado `backend/src/test/java/com/portifolio/realtime/NotificacaoWebSocketRf23IntegrationTest.java` para MENSAGEM real via HTTP→WS/SSE e bloqueio de query no handshake. Demais testes preservados.

## 11. Criação/reuso de sala

`POST /api/chat/salas` continua criando/reutilizando a mesma sala nos dois sentidos, com par ordenado e `pg_advisory_xact_lock` na transação PostgreSQL. Consulta não cria sala. Regressão concorrente com duas solicitações garante uma sala e dois participantes. Arquivo histórico com um remanescente não recebe mensagem nova sem destinatário.

## 12. Autenticação

Identidade por JWT/Principal, com conta e papel persistidos reconsultados. Token antigo não mantém acesso a conta bloqueada/removida. CONNECT exige Authorization Bearer válido e identidade ID/email atual; SUBSCRIBE/SEND revalidam a sessão. Handshake `/ws` rejeita query; JWT permanece no frame CONNECT. Não há credencial em URL.

## 13. Autorização/ownership

Sala própria exigida no histórico/envio/leitura/upload/download; edição/exclusão exigem autor além de participação. Terceiro e destinatário sem autoria recebem 404 nas operações de mensagem para não revelar recurso alheio. Papel/conta do ator impróprios: 403 no service (ou 401 na autenticação de conta inapta); payload: 400; falha real de persistência: 409; regra de negócio: 422. `ApiExceptionHandler` preservado.

## 14. ARTISTA ↔ CONTRATANTE

Guarda explícita exige um ARTISTA e um CONTRATANTE. Mesmo papel e auto conversa continuam bloqueados. ADMIN/MODERADOR não acessam chat operacional por exceção implícita. Papel alterado em sala existente é reconsultado.

## 15. Menor

ARTISTA 14–17 exige conta ativa e consentimento persistido vigente; responsável/dados de consentimento não entram nas DTOs. A existência da sala não dispensa prova profissional nem consentimento atual, revalidados nas operações. Consentimento revogado bloqueia histórico, envio, leitura, edição/exclusão, upload/download e reuso. Membership RF13 sozinho não concede contato.

## 16. Candidatura

Prova existente RF06/RF45 relaciona artista e vaga do contratante correto. Chat não cria candidatura, não muda seu status nem contorna limite/completude. Mantida interpretação histórica existente; duração do contato após cancelamento/encerramento ainda não está consolidada oficialmente. Não foi criada expiração arbitrária.

## 17. RF42

Convite profissional persistido/canônico por vaga do mesmo contratante pode liberar a conversa do menor autorizado. Outro proprietário, link genérico ou contexto inválido não libera; leitura do convite não destrói a prova. Código RF42 preservado; suíte específica regredida.

## 18. RF45

Visualização/contato com candidatos preservados e regredidos pela suíte RF45. Novo guard é compartilhado com o chat, sem novo módulo nem alteração no frontend de candidatos. RF06 também regredido.

## 19. Envio

Texto válido trim até 4000 caracteres; vazio, excesso ou placeholder reservado rejeitados. Ator, sala e timestamp vêm do servidor; IDs extras do payload não concedem autoridade. XSS permanece texto literal, sem renderização HTML no backend. Mensagem e alerta são persistidos juntos; emissão do chat inclui eco ao autor e destinatário da dupla.

## 20. Realtime

STOMP SEND `/app/chat/salas/{id}/mensagens`, fila `/user/queue/chat`; não existe tópico público por sala. Evento só após commit; autor recebe eco e destinatário recebe mensagem, terceiro não recebe. WS/SSE de MENSAGEM testados a partir de envio real HTTP em servidor RANDOM_PORT, com limite de cinco segundos. Falhas de transporte isoladas não desfazem persistência.

## 21. RF36

`NotificacaoPersistenceService.persistirNaTransacaoAtual` MANDATORY grava MENSAGEM na mesma transação; publica-se `NotificacaoPersistida`, entregue pelo listener AFTER_COMMIT à infraestrutura existente WS/SSE. Destino somente outro participante, texto genérico e link `/mensagens?sala={id}`. Sem segunda persistência por evento bruto. Alertas sobrevivem ao transporte indisponível e continuam na central.

## 22. Status de leitura

PATCH sala/lidas mantém 204; marca somente recebidas não lidas do ator, nunca suas próprias mensagens. Seleção/bulk update limitado a 50 IDs por chamada, ordem timestamp/ID descendente, incluindo remetente removido. Repetir chamada para próximos lotes. Status projetado e contagem real preservados; leitura/edição/exclusão não criam alerta novo. Esta alteração delimita o contrato do endpoint: uma chamada não promete marcar toda uma sala ilimitada.

## 23. Edição em 15 minutos

Somente autor participante, conta apta e mensagem não excluída. Clock compartilhado do servidor; 0/899/900 segundos permitidos, 901 bloqueado: exatamente 15 minutos é inclusivo. Preserva autor/sala/data_envio; persiste editada/data_edicao e os projeta em REST/realtime. Quando já existe reporte MENSAGEM, mantém primeiro texto anterior em `texto_original`, restrito e ausente das DTOs; não implementa arquivo de todas as versões.

## 24. Exclusão

Somente autor, sem limite de 15 minutos. Exclusão lógica grava excluida/data_exclusao, mantém ID/ordem/sala e exibe “Mensagem excluída pelo autor”. Repetição preserva data. Mensagem comum substitui texto, retira referência do anexo e limpa arquivo após commit; rollback mantém conteúdo. Mensagem previamente reportada conserva evidência restrita, mas REST/realtime/previews mostram placeholder e download devolve 404. Mensagem excluída não aceita edição.

## 25. Anexos

`POST /api/chat/salas/{id}/anexos` multipart `arquivo`, `texto` opcional; `GET /api/chat/mensagens/{id}/anexo` autenticado. Um arquivo por mensagem, conforme campo existente. Reuso de `ArquivoPortfolioValidator`: JPG/JPEG/PNG até 5 MiB, PDF até 10 MiB e MP3 até 20 MiB; MIME/extensão/tamanho/estrutura/nomes conferidos. Config multipart existente 21/22 MB preservada. Formatos diferentes não foram ampliados.

`app.chat.storage-root`, default `./storage/chat`, configurável fora de frontend/static/public; referência servidor `sala/UUID.ext`, nunca URL/caminho do cliente. Path normalizado, prefixo da sala, referência estrita e links ancestrais verificados; arquivo temporário/rename atômico e cleanup transacional. Nome original não é persistido/exposto. Resposta com nome genérico `anexo.ext`, attachment, no-store, nosniff e CSP sandbox. ACL antes da leitura; bytes íntegros testados para cada formato, identificação manipulada e traversal rejeitados.

Storage local privado depende de volume persistente, permissões de operação e backup; não há antivírus ou análise completa de conteúdo PDF, nem hash/metadados adicionais no banco. A reutilização do validator não equivale a prova de arquivo absolutamente inofensivo. Nenhum arquivo é servido estaticamente. Cleanup cobre rollback/exclusão normais; interrupção abrupta do processo entre filesystem e commit PostgreSQL pode deixar arquivo órfão privado, exigindo reconciliação operacional. Não há transação distribuída entre arquivo e banco.

## 26. Denúncias

Tabela/enum oficiais aceitam MENSAGEM; consulta do reporte existente impede exclusão destrutiva de evidência prévia. Teste insere reporte de fixture antes de edição/exclusão e verifica registro/conteúdo/arquivo conservados sem exposição. API atual de denúncia não oferece novo fluxo MENSAGEM; nenhum módulo de moderação privada/pública foi criado. Retenção jurídica e acesso moderador específico seguem pendentes.

## 27. RF22/anonimização

FK oficial SET NULL conserva mensagens; LEFT JOIN inclui remetente removido. Sala com participante remanescente e histórico continuam disponíveis: “Usuário Removido”, ID null, avatar null para fallback genérico; nenhuma identidade anterior/email/username é projetada. Conteúdo necessário e arquivo privado preservados conforme prova local. Token removido rejeitado; envio sem outro destinatário bloqueado.

**Limite real:** teste remove fixture pelo SQL oficial somente no PostgreSQL Testcontainers; não prova exclusão de conta orquestrada pela API. DELETE continua negado e `UsuarioServiceDeletionContainmentTest` preserva indisponibilidade segura. RF22 completo não foi implementado por efeito colateral. Política RF22 de eliminação de dados/anexos versus histórico/evidência RF35 precisa ser consolidada na futura orquestração. A DTO não revela metadados pessoais do usuário apagado; texto livre pode conter dados fornecidos pelos participantes e exige política de retenção, não uma promessa de sanitização universal.

## 28. Paginação

Histórico padrão 20/máximo 50, timestamp desc/ID desc; empates e página seguinte testados. Salas também paginadas e ordenadas por última mensagem/data da sala/ID. Nenhuma sala inteira é carregada em memória para histórico/leitura. IDs de leitura limitados a 50.

## 29. N+1/performance

Histórico e lista de salas usam projections/queries; anexos/status/identidade não exigem leitura de arquivo ou consulta extra por linha. Testes de 20 salas (teto quatro queries existente) e 50 mensagens (teto constante incluindo guards) preservam limites. Validação de dupla faz consultas por operação, não por mensagem da página. Não foi executado benchmark de carga/infra distribuída.

## 30. Banco alterado

**NÃO.** Sem SQL, enum, FK, constraint, migration, índice ou configuração de snapshot alterados. Dados de teste isolados nos containers são necessários aos testes e não representam alteração do banco de desenvolvimento. SQL histórico já sujo preservado.

## 31. Frontend alterado

**NÃO nesta tarefa.** Alterações anteriores preservadas byte a byte. Disclaimer pendente na interface: “a plataforma não se responsabiliza por acordos realizados entre usuários”. Interface também deve renderizar Editada, avatar genérico, upload/download autenticado e chamadas de leitura em lotes; nenhuma integração visual foi declarada validada.

## 32. Database05 46/46

Comparação de paths/bytes/SHA-256: **46/46 idênticos, 0 ausentes, 0 divergentes, 0 adicionais**. ZIP oficial `C:\Users\masca\Downloads\palco-database05.zip`, SHA-256 confirmado `6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf`. Evidência `snapshot-final-byte-a-byte.json`; preservação dos 1408 arquivos protegidos em `preservacao-final.json`.

## 33. Testes novos

Suíte RF35: autorização/IDOR/papel/estado atual; borda Clock; trilha/placeholder/reporte prévio; lote de leitura/ordem/query count; atomicidade/rollback/falhas WS/SSE/chat; formatos privados válidos/inválidos/paths; cleanup; histórico anônimo pela FK; consentimento revogado em todas as operações. WS/SSE compartilhados ampliados com MENSAGEM real e rejeição de query. Sem relaxamento de schema ou asserts antigos.

## 34. Regressões

23 classes focadas: chat REST/STOMP/RF35, RF42, RF06, RF45, RF27 service/integration, notificações/AFTER_COMMIT/WS/SSE/RF13, contenção RF22, endpoints genéricos/JWT, bootstrap/mapping database05, validator/portfolio/denúncia compartilhados e interceptor STOMP. Lista exata em `focados-classes.txt`; testes completos seguem a execução focada verde.

## 35. Focados

23 classes, **527 total / 527 passed / 0 failures / 0 errors / 0 skipped**, BUILD SUCCESS, exit 0, duração **04:34 min**. Evidências: `focados.log`, `focados-totais.json`, `focados-reports/` (XML sem propriedades de ambiente). Suíte RF35: 63/63; WS/SSE: 10/10; RF42 77/77, RF45 40/40, RF06 57/57. Lista exata em `focados-classes.txt`.

Tentativas conservadas separadamente: primeira falhou na compilação do helper multipart dos novos testes; segunda 527 total/463 passed/64 errors (63 de fixture sem telefone obrigatório, um timeout do caso SSE BANCO_DE_TALENTOS existente); diagnóstico seguinte 73 total/71 passed/2 failures (limpeza de salas da fixture). Correções exclusivamente nesses helpers/fixtures, sem relaxar asserts/schema/skips; diagnóstico WS/SSE 10/10, e rodada focada final integral verde. Não usar métricas das tentativas como resultado final.

## 36. Maven completo

Comando `.\mvnw.cmd test '-Dspring.test.mockmvc.print=NONE'`: **BUILD SUCCESS**, exit 0, **65 classes / 1293 total / 1276 passed / 0 failures / 0 errors / 17 skipped**, duração **09:38 min**, término 04/10/2026 00:51:58 -03:00. Evidências: `maven-completo.log`, `maven-completo-totais.json` e `maven-completo-reports/`. PostgreSQL 18.4/database05 reais; fontes não alteradas entre focados e completo.

## 37. Totais e proveniência

Métricas por classe nos JSON/XML sanitizados. Baseline RF42 anterior: 1227 total/1210 passed/17 skipped; atual: 1293/1276/17, incremento **66 casos** (63 da nova suíte RF35 + três WS/SSE), todas as 64 classes antigas mantidas, uma classe nova. Skips idênticos por classe: `CurrentLocalSchemaIntegrationTest` quatro e `OfficialLocalApiIntegrationTest` treze; nenhuma classe ausente/skip novo (`regressao-comparacao.json`). `maven-proveniencia.json` distingue init/JDBC database05 de qualquer snapshot anterior. Sem H2, patch/migration ou DDL auxiliar de testes; setup só DML de fixture e scripts oficiais.

Proveniência: focados 17 inicializações oficiais/33 ocorrências JDBC database05; completo 42 inicializações/82 ocorrências JDBC database05; zero inicialização/JDBC ativo database04. Logs de falhas de transporte, fixtures de rollback, pool/shutdown e avisos legados de collection fetch não produziram failures/errors na execução final. O teto de consultas de chat foi testado especificamente, sem declarar resolvidos os avisos de outras funcionalidades.

Fechamento: 344 fontes Java com hashes iguais aos testes, 1408 arquivos protegidos inalterados (incluindo 770 de frontend); HEAD a4ec8393caa4dde8cc58d10eac512c0aff40de96, index vazio, nenhum staging/commit/push. Escopo: oito Java de produção existentes + um novo; um teste existente + um novo. Conferência de whitespace: git diff --check sem erros, exit 0; os dois Java novos/relatório não rastreados também conferidos sem whitespace final. Artefatos git-*-final.txt/git-inspecao-final.json registram os quatro comandos finais; diff/stat globais incluem deltas históricos preservados e não incluem conteúdos não rastreados. Novos arquivos constam em escopo-final.json/status, sem staging.

Privacidade: 167 arquivos de evidência anteriores à inspeção Git verificados; zero ocorrência de JWT assinado/valor de segredo de execução conhecido (um valor de ambiente com tamanho mínimo auditado). XMLs copiados sem properties de ambiente; XMLs originais do target mantidos. A verificação é limitada aos padrões/valores conhecidos, sem declarar ausência absoluta de segredos.

## 38. Semgrep

Docker oficial **1.178.0**, imagem `semgrep/semgrep@sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b`, ruleset **p/java**, SHA-256 local `5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1`. Scan: **273 arquivos / 60 regras / ~100.0% linhas parseadas / 0 findings / 0 blocking / 0 erros / exit 0**; duração do scan **246 s**. Todas as nove fontes de produção alteradas cobertas, incluindo o Java novo não rastreado, via --no-git-ignore, sem staging.

Config oficial já obtida pelo Windows com TLS verificado e reutilizada por hash; montagem read-only e --network none. A verificação TLS NÃO foi desabilitada. Semgrep do workflow deste ambiente executado via Docker; instalação nativa Windows bloqueada por App Control não deve ser usada. Nenhum código/política de segurança do Palco/Windows foi alterado para reparar ferramentas. Evidências: semgrep-java.json, semgrep-java-saida.log, semgrep-java-resumo.json, semgrep-java-execucao.json e Executar-Semgrep.ps1.

“0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.” Este scan Java não equivale a um novo scan específico de secrets.
## 39. Graphify final

`graphify update .` AST: **exit 0**, **27.7 s**. Totais confirmados pelo MCP HTTP: **6074 nós / 20676 arestas / 341 comunidades**; confiança global **91% EXTRACTED / 9% INFERRED / 0% AMBIGUOUS**. CLI reextraiu 294 arquivos não cacheados; nós passaram de 5971 a 6074. Evidências: graphify-update.log, graphify-update-execucao.json, graphify-final-resumo.json e resposta integral graphify-mcp-final-602.txt.

Avisos: 45 arquivos SQL sem contribuição por tree_sitter_sql ausente; 75 arquivos sem extensão/shebang suportados; etiquetas antigas 340 versus 341 comunidades, 102 nomes derivados de hubs; HTML agregado com 341 nós de comunidade/1251 arestas entre comunidades. Nenhum parser foi instalado, label semântico executado, launcher graphify-mcp.exe iniciado ou App Control/Defender reparado. Não houve bloqueio de execução nesta tarefa nem contorno de segurança. Atualização AST e totais não equivalem a nova validação semântica de todos os requisitos.
## 40. Riscos

Exclusão global e retenção RF22 ainda não entregues; evidência reportada é conservada restritamente sem inventar prazo legal. Storage requer volume/backup/permissões adequados. Notificação/transporte AFTER_COMMIT isola falha e mantém registro consultável, mas não fornece outbox distribuído ou garantia de reentrega a socket offline. Prova profissional histórica depois de encerramento/cancelamento exige decisão oficial de duração.

## 41. Limitações

Não houve teste visual, frontend, carga, implantação ou exclusão orquestrada real de conta. Avatar null depende de fallback da interface. Preservação da primeira evidência reportada não é trilha integral de todas as versões. Anexos privados usam contrato mínimo do schema e validator atual. Limitações de parser SQL/semântica do grafo não substituem auditoria das fontes.

## 42. RF35 final

**PARCIAL.** Lacunas locais do chat implementadas e testes focados/completos verdes. RF22 orquestrado pela API permanece ausente; teste da estrutura oficial com fixture não encerra esse requisito funcional. Não existe blocker estrutural confirmado no schema atual para os itens locais entregues. Contenção do hard delete inseguro preservada. Semgrep/Graphify/evidências são complementares; não substituem a integração funcional faltante.

## 43. Impacto RF36

MENSAGEM passa a usar persistência atômica + evento persistido AFTER_COMMIT, mantendo central, entrega privada e isolamento WS/SSE existentes. Leitura/edição/exclusão não duplicam alertas. BANCO_DE_TALENTOS/CONVITE/CANDIDATURA preservados por regressão; não foi inventado novo enum/canal nem refeito o módulo.

## 44. Pendências

RF22 global com orquestração segura e integração real de histórico anônimo; decisão de retenção de texto/anexos/evidências; duração de interação profissional após encerramento/cancelamento; integração visual/disclaimer/fallback/anexos/leitura em lotes. Novo módulo de denúncia privada ou moderação não pertence à entrega atual.

## 45. Próximo passo

Tratar RF22 em tarefa própria, auditando todas as relações/arquivos e política consolidada, sem liberar DELETE genérico. Depois executar cenário API real exclusão→histórico remanescente→PII e reclassificar RF35. Integração visual exige tarefa própria. Não fazer staging/commit/push nesta tarefa.

## Registro semanal

Data: 03/10/2026; execução final em 04/10/2026. Objetivo: concluir/auditar RF35. RF/RNF: os relacionados na seção 2. Backend alterado: SIM (chat/DTOs/projeções/guards/storage/notificação). Frontend alterado: NÃO. Banco alterado: NÃO. Database05: 46/46, zero divergentes/ausentes/adicionais. Chat: sala/reuso/JWT/páginas preservados; autorização por operação reforçada. Menores: consentimento e prova profissional atuais. Realtime: privado AFTER_COMMIT; WS/SSE isolados. Notificações: MENSAGEM atômica. Edição/exclusão: flags/datas/Clock/placeholder/evidência prévia. Anexos: armazenamento/download privado com validação compartilhada. RF22: suporte local testado; orquestração ausente. Testes: focados 527/527, completo 1293 total/1276 passed/0 failures/0 errors/17 skips históricos, ambos BUILD SUCCESS. Semgrep: Docker 1.178.0, 273 arquivos/60 regras/0 findings/0 erros/exit 0. Graphify: AST exit 0, 6074 nós/20676 arestas/341 comunidades, MCP HTTP confirmado. RF35 final: PARCIAL. RF36: MENSAGEM integrada sem duplicação. Pendências/próximo passo: RF22/retenção e interface, seções 44–45.

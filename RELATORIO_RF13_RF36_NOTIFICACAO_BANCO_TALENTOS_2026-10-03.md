# RF13 → RF36 — Notificação de entrada no Banco de Talentos — 03/10/2026

**RF13 backend: CONCLUÍDO. RF36: integração de entrada no Banco concluída; requisito global permanece PARCIAL. RF17: PARCIAL, preservado.**

Checkout: C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline.
Branch: integracao-recuperada-2026-09-15.
HEAD inicial: ef570e1ebc72eef967c283205a98cebe62f7d430 — chore: sincroniza ambiente com database05 oficial.
Evidências: evidencias/rf13-rf36-2026-10-03/.

## 1. Objetivo

Concluir a pendência RF13 de notificar o CONTRATANTE somente quando o ARTISTA cria sua participação específica no Banco. Integrar à persistência/central/realtime RF36 existente, sem alterar outros eventos, RF17, banco oficial ou frontend.

Metodologia: auditar → alterar → testar focado → Maven completo → Semgrep Docker → Graphify update → verificar preservação → relatar. Sem staging, commit ou push.

## 2. Requisitos consultados

Fonte oficial: C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt. SHA-256 atual: **3e7ca8fa7e78a1967cf5f86c86f6a60f736879b02cfbd8f9d195040fea873183**.

Lidos RF13 integral (1170–1244), RF36 integral (3125–3213), RF27, RF44 e RNF02/RNF06/RNF07/RNF08/RNF09/RNF10/RNF17. RF13 exige notificar a entrada real; RF36 exige persistência independente de WebSocket/SSE; RF27 limita os dados do menor; RF44 não define participação no Banco como evento obrigatório de e-mail.

Consultados os relatórios RF13, sincronização database05 e RF17 de 03/10/2026. São históricos: seus resultados e conclusões não foram reescritos. Este relatório registra a conclusão complementar sobre o pacote database05.

## 3. Checkpoint inicial

**ef570e1** confirmado no HEAD, no upstream fork/integracao-recuperada-2026-09-15 e por git ls-remote do fork nesta tarefa. Sincronização database05 checkpointada; zero deltas soltos em backend, scripts/database05, database05, Compose, launcher e .gitattributes. Nenhum commit foi criado pelo agente.

Working tree já possuía alterações de frontend, README, SQL histórico e artefatos locais. Manifesto inicial: **1404 arquivos**. Fronteira de preservação: todos os arquivos anteriores, exceto os cinco Java explicitamente autorizados pelo delta desta tarefa. Estado inicial em checkpoint-inicial.json/git-status-inicial.txt.

## 4. Auditoria Graphify

Graphify MCP HTTP disponível e consultado antes de editar Java. Grafo inicial: **5867 nós**. Queries amplas de participação, persistência e serialização foram explicitamente truncadas; não foram tratadas como leitura completa das fontes.

Query final restrita a NotificacaoPersistenceService/NotificacaoEventoListener, depth 1, context call/field: **10 nós, sem truncamento**, incluindo repository, service, gateway e consumidores RF06/Vaga. Código completo confirmou MANDATORY, saveAllAndFlush, evento persistido e listener AFTER_COMMIT.

O grafo também contém SQL/documentação histórica; isso não seleciona a fonte ativa. Autoridade estrutural: database05 completo e PostgreSQL dos testes. Evidências: graphify-auditoria-202.txt a graphify-auditoria-205.txt. Sem reparos de MCP/Python/Windows.

## 5. Database05

Única fonte ativa: **database05/palco-database/**, PostgreSQL real/Testcontainers. Variável: **PALCO_TEST_DATABASE05_PATH**. Helper de bootstrap continua rejeitando configuração legada e validando fingerprint dos 46 arquivos; nenhuma alteração de infraestrutura de bootstrap nesta tarefa.

ZIP oficial: C:\Users\masca\Downloads\palco-database05.zip. SHA-256: **6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf**. Database04 permanece histórico; seus bytes, scripts e ambiente foram preservados.

## 6. Enum oficial

tipo_notificacao_enum contém, nesta ordem: CANDIDATURA, MENSAGEM, CONVITE, EDITAL, SALVO, **BANCO_DE_TALENTOS**. O label já pertence ao pacote oficial; não foi acrescentado ao SQL pelo backend.

Tabela notificacoes usa tipo_notificacao, usuario_destino_id, mensagem_alerta, link_contexto, lida e data_criacao. Não possui coluna de ator/metadados de participação; não foi inventada uma coluna ou tabela para essa integração.

## 7. Enum Java

Adicionado exatamente **TipoNotificacao.BANCO_DE_TALENTOS("BANCO_DE_TALENTOS")**. Converter autoApply usa getDatabaseValue/fromDatabaseValue, que percorre values(); passa a escrever e ler o label oficial sem aliases.

Auditados usos do enum, converter, NotificacaoResponse, eventos, central, gateway, candidatura, chat e salvos. Não há switch de produção sobre TipoNotificacao que exija novo case. JSON usa o nome exato do enum. SchemaMapping voltou à igualdade integral Java ↔ PostgreSQL, sem exceção adicional para label ausente no Java.

## 8. Arquivos de produção

Somente dois arquivos alterados:

| Arquivo | Alteração |
|---|---|
| backend/src/main/java/com/portifolio/model/enums/TipoNotificacao.java | Novo valor oficial |
| backend/src/main/java/com/portifolio/service/BancoTalentosParticipacaoService.java | Persistir na transação atual somente se criada; publicar evento persistido |

Controller/DTO RF13, repository/PK/ON CONFLICT, políticas de identidade/menor/publicabilidade, RF17, central, converter e infraestrutura compartilhada de notificações/realtime ficaram intactos.

## 9. Arquivos de teste

| Arquivo | Alteração |
|---|---|
| BancoTalentosParticipacaoRf13IntegrationTest.java | Adaptação das expectativas de notificação; 11 casos adicionais de central/transação/transportes |
| OfficialSchemaMappingIntegrationTest.java | Enum Java/PostgreSQL integralmente equivalente |
| NotificacaoWebSocketRf23IntegrationTest.java | Preservar casos CANDIDATURA e acrescentar BANCO_DE_TALENTOS por POST RF13 real em WebSocket/SSE |

Nenhum teste removido ou desabilitado. Demais fontes de teste, inclusive RF17, preservadas. A listagem dos critérios mínimos está em criterios-aceitacao.md.

## 10. Fluxo RF13 antes

POST validava JWT/conta/perfil/alvo/confirmação e executava INSERT ON CONFLICT. Retornava 201/200 conforme linha criada/existente; GET consultava estado. Não persistia notificação, pois o checkpoint funcional RF13 anterior utilizava database04 sem label apropriado.

Após a sincronização database05, a representação SQL ficou disponível, mas o Java/fluxo ainda não utilizavam o tipo. A guarda antiga de zero notificações comprovava essa pendência; não comprovava o requisito de notificar.

## 11. Fluxo final

ARTISTA autenticado → guards existentes → INSERT parametrizado → se criada, persistir NotificacaoEvento na transação atual → publicar NotificacaoPersistida → commit da participação e notificação → listener AFTER_COMMIT → gateway WebSocket/SSE.

POST mantém resposta contratanteId/participante e HTTP 201/200. Nenhum endpoint, body ou resposta foi ampliado. GET continua somente leitura. Não existe etapa paralela de notificação nem chamada direta de transporte dentro da transação.

## 12. Condição de primeira inserção

**banco.adicionar(...) retorna true somente quando jdbc.update(...) == 1.** Esse resultado é a autoridade do ramo que cria a notificação. INSERT ON CONFLICT DO NOTHING e PK composta permanecem os mesmos.

Não usado exists() antes do INSERT para decidir notificação. Resultado 0 não chama persistência/evento; resultado 1 cria exatamente uma notificação para o destino validado.

## 13. Repetição

Primeira entrada: **201**, uma relação e uma notificação. Repetição válida: **200**, mesma relação/data de adição, mesmo alerta/id/data, sem nova entrega realtime. Marcar como lida e repetir não reabre nem substitui o alerta.

GET, consulta RF17, perfil público e mudança de disponibilidade não criam notificação de entrada. Relações históricas anteriores à implementação não recebem backfill automático por repetição; a tarefa não autoriza emissão retrospectiva.

## 14. Concorrência

Preservada PK (contratante_id, artista_id) + INSERT ON CONFLICT. Três corridas reais, duas threads/requisições com barreira, produziram **uma 201 + uma 200 + uma linha + uma notificação + uma entrega**, sem 500.

Nenhum advisory lock/synchronized/constraint novo. O segundo INSERT espera a decisão PostgreSQL da primeira transação; somente o criador publica o evento.

## 15. Destinatário

Somente o **CONTRATANTE dono do Banco alvo**. Set.of(contratanteId) define um destinatário. Alvo continua validado pela política pública existente; identidade do ARTISTA vem exclusivamente do contexto JWT persistido.

Payload/query com artistaId/usuarioId/contratanteId alheios não alteram ator ou destino. Banco distinto recebe seu próprio alerta. ARTISTA, outro CONTRATANTE, administrador e responsável não são destinatários desse evento.

## 16. Conteúdo

Mensagem constante: **“Um artista entrou no seu Banco de Talentos.”**

Link: **/perfis/ARTISTA/{id do artista autenticado}**, padrão já usado por SalvoService e reconhecido pela navegação Profile/App do frontend atual. Contrato público API correspondente já existe em /api/perfis/publicos/ARTISTA/{id}. Não inventada nova URL visual.

O contexto identifica o ator por ID público. Nenhum campo estruturado de ator foi acrescentado a um modelo que não o possui. Central mantém seis campos: id, tipo, mensagem, link, lida e data.

## 17. Privacidade

Texto não contém nome nem outro dado pessoal; link contém somente identificador público. DTO não acrescenta CPF, e-mail, telefone, nascimento, idade, endereço, responsável, consentimento, experiência ou informações internas.

Teste verifica whitelist, valores privados das fixtures e isolamento da central. O e-mail interno do destinatário em NotificacaoPersistida continua sendo usado pelo gateway para o principal STOMP, sem entrar no DTO/recurso da central. Abrir o link continua sujeito às políticas RF10/RF27 atuais.

## 18. Menor autorizado

ARTISTA de 14/17 anos autorizado mantém a entrada existente e recebe o mesmo tratamento profissional: uma notificação ao CONTRATANTE, mensagem genérica e contexto público. Responsável/consentimento permanecem intactos e privados.

Guards de menor sem autorização, responsável ausente, consentimento incompatível/revogado e idade mínima continuam os mesmos. Regressões RF27/RF10 executadas; experiência privada do menor não foi usada em mensagem, filtro ou metadado.

## 19. RF44 não ampliado

**Nenhum e-mail ao responsável**, evento de aviso ou nova regra RF44. Publicação/candidatura pertencem ao escopo inicial do RF44; participação no Banco não recebeu obrigação externa inventada.

Teste de contagens das 43 tabelas confirma que a entrada altera somente banco_talentos e notificacoes. A integração usa exclusivamente a cadeia de notificações existente.

## 20. Persistência da notificação

Reutilizado NotificacaoPersistenceService.persistirNaTransacaoAtual (MANDATORY). saveAllAndFlush grava Notificacao real com tipo BANCO_DE_TALENTOS, destino específico, mensagem/link mínimos, lida=false e timestamp existente.

Evento publicado é **NotificacaoPersistida**, evitando que o listener do evento bruto persista novamente. Não há segunda escrita, notificação apenas em memória ou dependência de assinante ativo.

## 21. Transação

Participação e notificação pertencem à **mesma transação @Transactional** do POST. JDBC/JPA usam o gerenciador/DataSource vigente. Sem REQUIRES_NEW nessa integração.

Falha de persistência antes do commit aborta a entrada e reverte ambos; teste injeta DataIntegrityViolationException e recebe 409 pelo handler existente, com zero linhas/alertas/entregas. Rollback externo depois de ambos persistidos e evento registrado também reverte ambos e suprime a entrega. Não se mascara estado inconsistente como sucesso.

A semântica de outros eventos RF36 permanece a existente; não alterado o caminho bruto AFTER_COMMIT/REQUIRES_NEW usado em outros fluxos.

## 22. After-commit

Reutilizado NotificacaoEventoListener.entregar(NotificacaoPersistida), @TransactionalEventListener(AFTER_COMMIT). Durante a transação principal, há linha e alerta persistidos, porém zero chamadas de gateway; depois do commit, exatamente uma chamada.

Rollback principal depois do flush não deixa alerta órfão nem entrega pendente disparada. Repetir depois do rollback pode criar normalmente o vínculo/alerta, porque a primeira tentativa não foi confirmada.

## 23. WebSocket

Infraestrutura STOMP existente, JWT em Authorization no CONNECT, fila privada /user/queue/notificacoes. Novo cenário abre destino/usuário isolado, executa POST RF13 por HTTP real e recebe o tipo/link/mensagem/id da notificação persistida somente no destino.

Assertion de entrega abaixo de cinco segundos aprovada nesse cenário local; não é benchmark p95/p99 em produção. Casos CANDIDATURA e rejeição de token ausente/inválido/expirado preservados.

## 24. SSE

Infraestrutura /api/notificacoes/stream existente, Bearer em header. Novo cenário executa POST RF13 real e recebe evento notificacao com BANCO_DE_TALENTOS e dados coerentes com a linha persistida. Cliente isolado não recebe o alerta.

Transporte conserva serialização/rota e tratamento de conexões fechadas existentes. Regressão de pool/conexões SSE incluída. Nenhum JWT em query string.

## 25. Falha realtime

Três cenários novos: exceção WebSocket, exceção SSE e exceção do gateway. **201 preservado, vínculo preservado e alerta persistido preservado.** Falha WebSocket não impede a tentativa SSE; falha SSE não apaga o resultado WebSocket.

Tratamento permanece no gateway/listener existente, após commit. Sem retry/outbox/infraestrutura nova; alertas ficam consultáveis na central mesmo sem entrega realtime. Repetição continua 200 sem duplicar persistência.

## 26. Listagem RF36

GET /api/notificacoes mantém paginação/ordem/contrato e deriva destino do JWT. A central retorna BANCO_DE_TALENTOS normalmente via NotificacaoResponse. Artista e outro contratante não acessam o alerta.

Nenhuma listagem ilimitada, novo mapper ou consulta global introduzida. Regressão RF23 de central/paginação/N+1 e segurança preservada.

## 27. Leitura

Conversão JPA relê o novo tipo como TipoNotificacao.BANCO_DE_TALENTOS. JSON entrega o nome exato; tipo e link do payload realtime coincidem com a linha PostgreSQL.

Consulta de estado/central/perfil/RF17 não grava segundo alerta. Leitura de participação permanece disponível sob os guards vigentes, sem alteração funcional RF17.

## 28. Marcar lida

PATCH /api/notificacoes/{id}/lida continua restrito ao destino; artista/outro dono recebem 404. Count não lidas muda de 1 para 0; repetição RF13 preserva o mesmo alerta lido.

PATCH /api/notificacoes/lidas marca os alertas BANCO_DE_TALENTOS do próprio usuário, sem marcar o alerta de outro Banco. Três vínculos/alertas permanecem intactos no teste de isolamento.

## 29. Banco alterado: NÃO

Nenhum SQL, enum SQL, tabela, coluna, PK/FK, constraint, índice, function, procedure, trigger, migration, seed ou init alterado. Nenhum patch aplicado. **ddl-auto=validate** mantido.

DML somente em PostgreSQL descartável/Testcontainers. Banco de desenvolvimento database05 e banco histórico database04 não receberam mutação nesta tarefa. Snapshot/config/scripts de sincronização permanecem checkpointados e intactos.

## 30. Frontend alterado: NÃO

Somente leitura das rotas/padrão de link. Sem npm install/build/test/format, popup, badge ou alteração visual. **770 arquivos** protegidos pelo manifesto inicial; alterações históricas já presentes continuam pertencendo ao working tree anterior.

Recursos existentes copiados para target Maven são derivados de backend, sem editar suas origens. Integração visual/diferenciação do tipo fica para tarefa própria.

## 31. Database05 46/46

Reconferido após Maven completo, Semgrep e Graphify: **46/46 arquivos idênticos byte a byte ao ZIP oficial**, mesmos paths, zero divergências, zero ausentes e zero adicionais no pacote. SHA-256 do ZIP confirmado: **6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf**. Resultado em snapshot-final-byte-a-byte.json.

Os **1399 arquivos protegidos** permanecem idênticos ao manifesto inicial, incluindo 770 arquivos frontend, 264 outros Java de produção, 47 arquivos database04 e cinco scripts database04. Exatamente cinco arquivos anteriores mudaram nesta tarefa: dois de produção e três de testes. Nenhum arquivo novo em src/banco/scripts/frontend. As **335 fontes Java testadas** permanecem idênticas ao estado da execução. Evidências: preservacao-final.json, delta-final.json, fontes-testadas-final.json e escopo-final.json; HEAD/branch mantidos e index vazio.

Fingerprint: **c68460169fcd2538fefd34a109ee3ea7640e553ce1882dd225d88d655229c134**. Comparação direta dos bytes descomprimidos com os arquivos locais, incluindo o arquivo vazio git, sem normalização Git. .gitattributes -text preservado.

## 32. Testes novos

**13 casos adicionais:** 11 na classe RF13 (central/privacidade, consultas, Salvo independente, marcar lida/todas, commit, rollback, falha de persistência, duas falhas de transporte e gateway) e dois casos reais de WebSocket/SSE com BANCO_DE_TALENTOS.

RF13 passa de 57 para **68 casos**; realtime passa de 3 para **5**. As antigas verificações de zero notificações para ações válidas foram substituídas pela contagem/tipo/destino corretos, sem retirar as verificações de duplicação, concorrência, timestamp, whitelist ou efeitos nas outras tabelas.

## 33. Regressões

20 classes focadas: RF13 participação, RF17 busca, RF23 notificações, listener, WebSocket, SSE pool, bootstrap database05, schema mapping, segurança genérica, JWT, consentimento RF27, consentimento service, Salvos RF19, RF06 candidaturas, RF45 candidatos, chat RF24, chat WebSocket, ManuDump/schema, perfil público RF10 e SQL oficial validado.

RF17 86/86 preservado. Nenhuma política pública compartilhada foi modificada; RF37 entra pela regressão Maven completa. As regressões de candidatura/chat garantem que o enum ampliado não altera seus tipos existentes.

## 34. Bateria focada

**416 total / 416 passed / 0 failures / 0 errors / 0 skipped; 20 classes; BUILD SUCCESS; exit 0; 04:27 min; término 03/10/2026 18:23:18 -03:00.**

Comando Maven com as 20 classes em focados-classes.txt e -Dspring.test.mockmvc.print=NONE. Nenhum teste desabilitado; flag suprime somente dumps de request/response. Evidências: focados.log/.exit, focados-totais.json e focados-reports/.

## 35. Maven completo

**1148 total / 1131 passed / 0 failures / 0 errors / 17 skipped; 63 classes; BUILD SUCCESS; exit 0; 07:55 min; término 03/10/2026 18:32:27 -03:00.** Evidências: maven-completo.log/.exit, maven-completo-totais.json e maven-completo-reports/.

Comando `.\mvnw.cmd test '-Dspring.test.mockmvc.print=NONE'`, sem clean/filtro, ambiente scripts/database05/environment.ps1 e PALCO_TEST_DATABASE05_PATH. JDK 21.0.11, Spring Boot 4.0.6, Testcontainers 2.0.5 e PostgreSQL 18.4/postgres:18-alpine, validate.

## 36. Totais

| Rodada | Classes | Total | Passed | Failures | Errors | Skipped | Duração | Resultado |
|---|---:|---:|---:|---:|---:|---:|---|---|
| Focada | 20 | 416 | 416 | 0 | 0 | 0 | 04:27 min | BUILD SUCCESS / exit 0 |
| Completa | 63 | 1148 | 1131 | 0 | 0 | 17 | 07:55 min | BUILD SUCCESS / exit 0 |

Baseline checkpoint database05: 1135 total / 1118 passed / zero failures/errors / 17 skips. Acréscimo de 13 casos; resultados reais da rodada prevalecem sobre expectativa. Mesmos skips condicionais: CurrentLocalSchemaIntegrationTest 4 e OfficialLocalApiIntegrationTest 13, sem mudança das condições.

XMLs consolidados somente se posteriores ao início de cada rodada; cópias sem properties de runtime mantêm casos/resultados. Fontes Java usadas na rodada são reconferidas por hash após as validações; logs demonstram init/URL database05, sem inicialização ativa database04.

## 37. Semgrep

**Semgrep 1.178.0 via Docker; p/java; 266 arquivos de produção rastreados pelo Git; 60 regras; ~100.0% das linhas parseadas; 0 findings; 0 blocking; 0 erros; execução concluída com sucesso; exit 0; 257,8 segundos.** Os dois arquivos Java de produção alterados estão explicitamente presentes em paths.scanned.

Container executado com **--network none** e mount **:ro**; regras locais, métricas e consulta de nova versão desativadas por flags normais da ferramenta. Evidências: semgrep-configuracao.json, semgrep-java.json, semgrep-java-saida.log, semgrep-java.exit, semgrep-java-execucao.json e semgrep-java-resumo.json.

Workflow Docker oficial, p/java em cache com SHA-256 **5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1**, proveniência de download Windows com TLS verificado registrada nas evidências anteriores. Imagem fixada em digest sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b; checkout somente leitura.

Semgrep nativo Windows não utilizado. TLS, Defender/App Control e instalação Python não alterados. Nenhum código do Palco alterado para corrigir ferramenta; nenhum novo scan secrets declarado.

**0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.**

## 38. Graphify final

**graphify update . concluído; exit 0; 28,4 segundos; 5884 nós, 19708 arestas e 333 comunidades.** Graphify MCP HTTP confirmou esses totais no graph_stats final: 91% EXTRACTED, 9% INFERRED, 0% AMBIGUOUS. O update não renovou a camada semântica anterior.

Log registra 45 arquivos SQL sem contribuição ao grafo por ausência de tree_sitter_sql, 75 arquivos não classificados e labels salvos de 345 comunidades anteriores versus 333 atuais. HTML usa visão agregada de 333 comunidades/1244 arestas entre comunidades devido ao limite de 5000 nós. Esses limites não substituem a auditoria direta das fontes SQL nem os testes PostgreSQL.

Consulta HTTP final confirmada após inicialização de nova sessão; encerramentos prematuros de respostas intermediárias foram registrados sem reparos do servidor. Evidências: graphify-update.log/.exit, graphify-update-execucao.json, graphify-final-resumo.json, graphify-mcp-final-212.txt e graphify-mcp-final-tentativas.txt.

Update AST após mudanças Java; sem graphify label, graphify-mcp.exe, instalação de parser ou reparos de Windows/App Control. Limites de parser SQL/labels semânticos registrados conforme log; auditoria de banco feita diretamente no pacote/PostgreSQL.

## 39. RF13 final

**CONCLUÍDO no backend.** Os 31 critérios mínimos estão rastreados em criterios-aceitacao.md e aprovados pelas verificações correspondentes: focados 416/416, Maven completo verde, Semgrep sem findings e snapshot/proteção de escopo intactos. Graphify inicial/final concluídos.

Entrada/consulta, JWT, conta/perfil/alvo, Banco específico, idempotência/concorrência, menor autorizado e disponibilidade continuam os contratos anteriores. Este delta entrega a única pendência funcional restante de backend: notificação ao contratante na criação real.

Conclusão de backend não declara integração visual, SMTP, carga ou homologação jurídica. Relatório histórico RF13 permanece intacto.

## 40. RF36 final

**PARCIAL globalmente.** A integração **RF13 → RF36 de entrada no Banco está concluída**, com as validações desta tarefa aprovadas. Persistência, central, leitura/lida e transportes existentes aceitam o novo evento.

Convites RF42, seguidores RF41, demais integrações/visualização e critérios globais RF36 não foram implementados ou declarados concluídos por este delta. RF17 permanece PARCIAL pela decisão de experiência privada do menor.

## 41. Riscos

Entrega realtime é best effort, sem nova estratégia de retry/outbox; a central mantém a notificação. Histórico de participações anterior ao delta não gera alerta por repetição. Links podem tornar-se indisponíveis se o perfil perder visibilidade; política pública continua aplicada.

Concorrência de alteração de estado/publicabilidade conserva os limites existentes da validação; unicidade da relação e atomicidade vínculo/alerta são PostgreSQL/transação. Dívidas SQL legadas permanecem no pacote oficial, sem correção local.

17 integrações locais condicionais não executadas limitam cobertura; sucesso não é teste de carga ou confirmação de porcentagem RNF10. Working tree amplo inclui mudanças anteriores, exigindo seleção específica em eventual checkpoint futuro.

## 42. Pendências

Integração frontend visual e revisão das outras entregas RF36 em tarefas próprias. Decisão funcional RF17 sobre experiência privada do menor, chat RF35 e convite RF42 permanecem fora deste escopo. Dívidas SQL somente por novo pacote oficial.

Nenhuma pendência de representação de enum/notificação de entrada RF13 permanece após as validações finais aprovadas. Não realizado staging/commit/push; revisão/checkpoint posterior pertence ao usuário.

## 43. Próximo passo

Revisar o delta específico de dois arquivos de produção, três testes e evidências. Em tarefa posterior autorizada, integrar o tipo/contexto no frontend e continuar RF36 sem alterar artificialmente o status RF17.

Inspeções finais solicitadas: git status --short, git diff --stat, git diff --check e git diff --name-status -- backend. Saídas/códigos registrados em git-inspecao-final.json e git-*-final.txt na pasta de evidências. O diff global inclui alterações anteriores, preservadas e não atribuídas a esta tarefa.

## Registro semanal

**Data:** 03/10/2026.
**Objetivo:** concluir RF13 backend integrando notificação de entrada ao RF36.
**RF/RNF:** RF13/RF36; regressões RF06/RF10/RF17/RF19/RF27/RF45/chat; RNF02/RNF06/RNF07/RNF08/RNF09/RNF10/RNF17; fronteira RF44.
**Backend alterado:** enum oficial Java e service RF13; três testes adaptados/ampliados.
**Frontend alterado:** NÃO.
**Banco alterado:** NÃO.
**database05:** 46/46 idêntico ao ZIP oficial, mesmos paths, zero divergências/ausentes/adicionais; database04 intacto.
**Funcionalidade concluída:** uma notificação BANCO_DE_TALENTOS somente para primeira relação confirmada, sem duplicação.
**Segurança/privacidade:** ator JWT, destino específico, mensagem genérica/contexto público, menor autorizado protegido, sem RF44 novo.
**Testes/resultados:** focados 416/416 em 20 classes, zero failures/errors/skips, 04:27 min; completo 1148 total/1131 passed/zero failures/errors/17 skips conhecidos em 63 classes, 07:55 min; ambos BUILD SUCCESS/exit 0. Semgrep Docker 1.178.0, 266 arquivos/60 regras/zero findings/erros/exit 0. Graphify AST 5884 nós/19708 arestas/333 comunidades, exit 0; preservação final aprovada.
**RF13 final:** CONCLUÍDO no backend.
**RF36:** integração de entrada concluída; global PARCIAL.
**RF17:** PARCIAL, regra/testes preservados.
**Pendências:** frontend e demais RF36/decisão RF17, sem pendência de notificação RF13 após validação.
**Próximo passo:** revisão/checkpoint específico pelo usuário; integração visual em tarefa própria.

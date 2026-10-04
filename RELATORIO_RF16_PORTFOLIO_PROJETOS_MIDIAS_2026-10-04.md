# RF16 — Portfólio por Projetos e Mídias — 04/10/2026

**RF16 BLOQUEADO POR ESTRUTURA; suporte existente de mídias individuais PARCIAL.** Auditoria concluída no caminho B: não foi criado projeto fictício. Produção, testes, frontend, banco e SQL foram preservados. Os resultados de regressão abaixo verificam contratos existentes, não critérios de projeto ainda inexistentes.

Evidências: [decisão estrutural](evidencias/rf16-2026-10-04/DECISAO_ESTRUTURAL_RF16.md), [auditoria database05](evidencias/rf16-2026-10-04/auditoria-estrutura-database05.md), [contratos e cobertura](evidencias/rf16-2026-10-04/auditoria-contratos-portfolio.md).

## 1. Objetivo

Confrontar RF16 revisado com database05 e código atual; implementar somente com modelo semanticamente suficiente. Na ausência da estrutura, preservar produção, regredir PORT1/PORT2, documentar o gap e propor evolução conceitual para novo pacote oficial completo.

## 2. RF/RNFs consultados

Fonte oficial atual, consultada nesta tarefa: `C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt`. Leitura integral de RF10, RF14, RF15, RF16, RF18, RF19, RF22, RF27, RF30, RF32, RF36, RF40, RF44 e RNF02, RNF05, RNF06, RNF07, RNF08, RNF09, RNF10, RNF17; RF08 também consultado para completude. Extratos em rf-rnf-consultados.txt.

RF16 exige projeto real, título, descrição opcional, capa, múltiplas mídias, RASCUNHO privado e PUBLICADO válido. RF32 absorvido; RF40 separado; RF15 fora do MVP. Portfolio não é requisito de perfil_completo. Regressão não comprova as metas quantitativas de cobertura/performance.

Históricos consultados: RELATORIO_SINCRONIZACAO_DATABASE05_2026-10-03.md; RELATORIO_RF22_EXCLUSAO_CONTA_2026-10-04.md; RELATORIO_RF35_SISTEMA_MENSAGENS_2026-10-03.md; RELATORIO_RF36_NOTIFICACOES_TEMPO_REAL_2026-10-04.md; RELATORIO_RF45_CANDIDATOS_VAGA_2026-10-03.md; RELATORIO_RF19_SALVOS_2026-09-28.md; RELATORIO_RF16_PORTFOLIO_ARQUIVOS_FINAL.md; RELATORIO_FRONTEND_PORTFOLIO_SALVOS.md; AUDITORIA_BANCO_POS_DECISOES.md; MAPA_INTEGRACAO_FRONTEND_API.md (PORT1/PORT2 e PO-01–PO-14).

A antiga conclusão RF16 “concluído” referia-se a arquivos individuais no MVP então autorizado; não prova RF16 revisado por projetos. Regras históricas de bloqueio público de todo menor foram superadas por RF45/RF27 atuais. Relatórios antigos preservados, sem reescrita.

## 3. HEAD/checkpoint

HEAD inicial: **03ca67ecbb94f1d53a909c1e1d86629d5773ea92**, commit “feat: conclui RF36 backend de notificacoes”. Branch integracao-recuperada-2026-09-15; upstream e fork coincidem com HEAD; rev-list **0/0** após git fetch fork bem-sucedido. A tentativa no sandbox falhou com SEC_E_NO_CREDENTIALS; execução autorizada fora dele concluiu exit 0, sem alterar TLS/segurança do Windows.

Index vazio; nenhum delta em backend/database05/database04/scripts no início. Não houve add/staging/commit/push/reset/clean/stash/restore. Evidência checkpoint-inicial.json.

## 4. Graphify inicial

MCP consultado antes da navegação profunda/decisão. **6.214 nós, 21.299 arestas, 356 comunidades; EXTRACTED 91%, INFERRED 9%, AMBIGUOUS 0%**. query_graph mapeou PORT1/PORT2 e dependências; get_neighbors dos services e get_node dos controllers/storage/entities/validators/público/menor/RF44/Salvos/Denúncia/RF22/Security/resolver confirmaram fontes. Query ampla truncada foi complementada por consultas específicas; PortfolioArquivo ambíguo resolvido pelo ID de nó do model.

MCP HTTP em http://127.0.0.1:8765/mcp confirmado diretamente por initialize + tools/call graph_stats; respostas brutas e resumo arquivados. Grafo orientou; SQL/código atuais decidiram, inclusive distinção das fontes históricas database02 vistas no grafo.

## 5. Estado inicial

Working tree histórico deliberadamente sujo, incluindo README, SQL histórico database/02_tables/04_vagas.sql, frontend e documentação/evidências antigas. Preservação por manifesto inicial de **1.427 arquivos**, incluindo **276 Java de produção**, fontes de teste, **770 frontend**, **47 database04 e cinco scripts database04**. Não se atribuem deltas anteriores ao RF16.

## 6. Endpoints atuais PORT1/PORT2

Todos permanecem, sem novos endpoints de projeto:

| Método | Rota /api/portfolio | Contrato |
|---|---|---|
| POST | /arquivos | Multipart arquivo individual |
| GET | /me/arquivos | Próprios, paginados |
| GET | /publico/artistas/{artista}/arquivos | Mídias de artista publicável |
| GET | /arquivos/{id}/conteudo | Conteúdo próprio |
| GET | /publico/arquivos/{id}/conteudo | Público revalidado, inclusive Range |
| DELETE | /arquivos/{id} | Exclusão individual pelo owner |
| POST | /videos | URL/legenda de embed individual |
| PUT | /videos/{id} | Edição individual pelo owner |
| GET | /me/videos | Embeds próprios, paginados |
| GET | /publico/artistas/{artista}/videos | Embeds de artista publicável |
| DELETE | /videos/{id} | Exclusão individual pelo owner |

## 7. Database05

Fonte ativa exclusiva: database05/palco-database. Inventário direto de **46 arquivos / 43 tabelas**, enums, funções, procedimentos, queries, triggers, init e seed. Inventário/hashes/ocorrências com linhas em inventario-46-arquivos.json/md. SQL oficial preservado; database04 apenas histórico. Nenhuma inicialização auxiliar/H2/migration/DDL de schema.

## 8. Tabelas relevantes

portfolio_arquivos e embeds_externos pertencem diretamente ao ARTISTA. galerias_virtuais/itens_galeria/interacoes_galeria são domínio histórico; denuncias_plagio, reportes_usuario, moderacao_conteudo e itens_salvos usam alvos de perfil ou polimórficos, sem projeto. usuarios/perfis_artistas/responsaveis_legais definem identidade/owner/consentimento.

PK/FK, campos/tipos/nullabilidade, defaults, enums, constraints, índices, ON DELETE e capacidade de relação inventariados em auditoria-estrutura-database05.md. Não se inferiu integridade de alvo polimórfico só por conteudo_id/alvo_id.

## 9. Enums relevantes

**status_projeto_portfolio_enum existe** com RASCUNHO/PUBLICADO; **não é usado por nenhuma coluna**. status_galeria_enum ATIVA/AGENDADA/ENCERRADA não o substitui. plataforma_embed_enum inclui YOUTUBE/VIMEO/SPOTIFY/SOUNDCLOUD; API aceita três primeiros. tipo_midia_enum VIDEO/AUDIO/POST_SOCIAL não é agregador de mídia. tipo_conteudo_enum não possui projeto/arquivo/embed; OBRA em tipo_alvo_salvo_enum não habilita suporte de produto.

## 10. Projeto existe?

**NÃO.** Não há tabela, mapping, ID, repository ou service de projeto profissional. Arquivo/filename, URL de perfil, galeria, vaga e publicação social não equivalem ao RF16.

## 11. Status existe?

Enum existe; **estado de projeto persistido NÃO**. Não há transição RASCUNHO→PUBLICADO nem filtro público por estado RF16.

## 12. Capa existe?

**NÃO.** Sem referência/flag/relação explícita. Primeira mídia não foi promovida a capa; nenhuma capa inventada no frontend.

## 13. Múltiplas mídias existem?

Múltiplos arquivos e embeds **por artista SIM; por projeto NÃO**. Galeria histórica é M:N de arquivos, sem owner consistente por projeto, sem embeds e sem estados/capa requeridos.

## 14. Embed vincula a projeto?

**NÃO.** EmbedExterno contém artista_id, plataforma, URLs/iframe/tipo/legenda/ordem; nenhum projeto_id ou associação equivalente.

## 15. Ordenação existe?

Arquivos: ordem técnica data_upload DESC/id DESC. Embeds: coluna ordem_exibicao existe, mas lista usa id DESC e API não aceita ordenar. Sem ordenação controlável de mídias de projeto; RF30 permanece parcial. Nenhuma coluna criada.

## 16. Rascunho existe?

**NÃO.** Rotas /me dão leitura própria de mídia; não representam rascunho persistido/incompleto/retomável. Não se confunde preview/localStorage/IndexedDB com RF16.

## 17. Publicação existe?

**NÃO para projeto.** Mídia individual torna-se publicamente acessível conforme publicabilidade do artista; não há etapa persistida que exija título+capa+mídia.

## 18. Decisão estrutural

**BLOCKER ESTRUTURAL — caminho B**, registrada em DECISAO_ESTRUTURAL_RF16.md. Respostas A–Q explícitas na auditoria. Ausência de entidade/vínculo/status/capa impede implementação backend-only segura sem alterar schema; enum isolado não resolve.

## 19. Implementação realizada ou blocker

Somente documentação/evidência de auditoria/regressão. Sem Entity fictícia, endpoints de projeto, prefixos mágicos, agrupamento por sessão/timestamp, JSON indevido, galeria paralela ou schema Hibernate automático. Lacunas existentes de retenção/storage/avisos registradas separadamente; não houve correção de produção no caminho B.

## 20. Arquivos de produção

**Nenhum alterado.** Código Java, configuração, banco/SQL, scripts operacionais e frontend preservados. Scripts novos em evidencias/rf16-2026-10-04 apenas capturam inventário/testes/verificação; não são bootstrap/migration ou código do produto.

## 21. Arquivos de teste

**Nenhum alterado ou criado no backend.** As 16 classes focadas já existiam. Resultados atuais foram capturados em XML sanitizado sem properties de ambiente, JSON e logs separados. Não houve skip novo/relaxamento de assertions.

## 22. Autenticação

SecurityConfig permite GET /api/portfolio/publico/** e exige ARTISTA nas rotas privadas. JWT validado; AuthenticatedUserResolver consulta usuário persistido pelo contexto, sem autoridade do ID do request. Anônimo/token inválido 401 nos fluxos privados.

## 23. Autorização

CRUD próprio exige ARTISTA e conta ATIVA, não apenas usuário autenticado. CONTRATANTE/ADMIN/MODERADOR não gerenciam material como artistas. Público depende RF10/RF27 e status da conta. Perfil completo/foto/experiência pública não foram acrescentados como condição de projeto.

## 24. Ownership

FK artista_id e identity JWT no upload; queries próprias em edit/delete; arquivo/embed alheio 404. Repositório de arquivo usa lock pessimista na exclusão. Sem relação mídia→projeto para validar, sem endpoint de projeto implementado. CRUD próprio não possui verificação explícita adicional de consentimento; não confundir seu gate de status com toda a política pública.

## 25. Rascunho

Privacidade por estado RF16 não implementável no schema atual. Não há rascunho público nem privado salvo como projeto. Mídia individual continua no contrato existente; não deve ser anunciada como rascunho.

## 26. Publicação

Critérios título+capa+ao menos uma mídia são exigências futuras, não testes aprovados nesta tarefa. Não há produtor real de publicação de projeto. Nenhuma alteração de perfil_completo.

## 27. Edição

PUT de embed próprio atualiza URL/legenda com validação, preserva owner; arquivo não tem endpoint de edição de projeto. Não há estado PUBLICADO para validar integridade ao remover capa/mídia.

## 28. Exclusão

DELETE individual verifica owner, mas **não consulta denúncias/provas/reportes/moderação antes de remover arquivo**. CASCADE pode apagar itens/interações de galeria usados na análise. RF22 possui recusas conservadoras que não são compartilhadas por esse DELETE. Lacuna de retenção existente, não resolvida; não atestar preservação universal de evidência.

Exclusão de projeto não existe. Não houve cascade novo nem política jurídica inventada.

## 29. Capa

Bloqueada por ausência de campo/relação. Futuro modelo deve referenciar mídia do mesmo projeto/artista e impedir PUBLICADO sem capa válida. Nenhuma primeira imagem inferida.

## 30. Arquivos

JPG/JPEG/PNG: 5 MiB; PDF: 10 MiB; MP3: 20 MiB. Extensão+MIME+tamanho+conteúdo; nomes seguros. PNG CRC/estrutura/ImageIO; JPEG markers/ImageIO; MP3 frames/ID3; PDF apenas assinatura %PDF-, sem sanitização completa ou antivírus.

DOCX/vídeo upload rejeitados. Teto multipart técnico 21 MiB/22 MiB retorna 413; regra de negócio 422. Bytes e formatos reais documentados na auditoria de contratos.

## 31. Storage

UUID por owner, root restrito, traversal/links/NOFOLLOW_LINKS, temporário+force/move e cleanup de upload no rollback. Sem paths internos em DTO.

**DELETE individual físico antes do commit**, backup em memória e restauração no rollback: não é DB-first/AFTER_COMMIT, XA ou recuperação durável. Crash/commit indeterminado/falha de compensação são riscos. RF22 usa cleanup AFTER_COMMIT; falha física pós-commit exige reconciliação. Estratégias preservadas e diferenciadas; não criado outbox/storage novo. Teste de symlink é interceptação de Files, não criação real de link Windows.

## 32. Embeds/RF30

YouTube, Vimeo e Spotify suportados por allowlist estrita/HTTPS/IDs e normalização; sem iframe bruto do cliente/javascript/data/domínio arbitrário. Não segue URL/redirect enviado pelo cliente. DTO revalida URL antiga e não devolve HTML armazenado.

RF30 **PARCIAL**: sem projeto, ordenação editável e verificação de disponibilidade/privacidade/removal na origem; async visual não validado nesta tarefa. SOUNDCLOUD do enum não é suporte da API.

## 33. RF18

Barreiras determinísticas reais de arquivos/URL/metadata; não dependem de API paga. Legenda valida tamanho/controles, não faz sanitização HTML geral; renderização deve escapar texto. Não há moderação completa/filtro público de projeto ou retenção integrada no DELETE individual. **RF18 não declarado concluído.**

## 34. RF14

Upload registra data/hora; denuncias_plagio registra perfil, motivo, provas por URL e timestamps. DenunciaService atual recebe perfil/vaga, não projeto/arquivo. Não há vínculo estruturado de prova de projeto; retenção de exclusão individual é pendência. Data/hash de manifesto são evidência técnica, não prova jurídica de autoria; RF15 não implementado.

## 35. RF10 público

PerfilPublicoService usa DTO e política de publicabilidade; cidade/UF e ausência de dados privados. Mídias por endpoints separados, sem bytes na listagem. **Projetos PUBLICADO ausentes.** Sem exposição nova de CPF, contatos, nascimento, responsável, consentimento ou paths.

## 36. Menor/RF27

Menor 14–17 ATIVA/autorizado pode exibir mídias individuais conforme política atual; sem autorização/conta apta/idade válida, público retorna 404 inclusive ID direto/Range. Experiência/dados do responsável/autodeclarações privadas não projetados na mídia. Regressão preserva revogação histórica como bloqueio técnico, sem criar funcionalidade nova de revogação RF27.

## 37. RF44

Existe AvisoResponsavelCandidaturaEvento/Listener e GuardianApplicationNoticeSender AFTER_COMMIT, exercitados na regressão RF06. **Não existe evento/sender de publicação do portfólio.** Não enviar aviso em editor/rascunho/navegação; nenhum aviso fictício criado. Integração de publicação requer produtor real futuro.

## 38. RF19

PERFIL_ARTISTA/VAGA permanecem suportados; OBRA rejeitado pela API apesar de enum físico. Sem novo tipo salvo ou integração artificial ao projeto inexistente. Salvamento continua independente de Seguir/Banco e não gera evento novo RF36 nesta tarefa.

## 39. RF22

Orquestração existente elimina arquivos/embeds/vínculos suportados, limpa físicos AFTER_COMMIT, remove PII/owner/perfil e anonimizados necessários. Recusas para evidência/caminhos desconhecidos/retencão não representável foram preservadas e regredidas. Não há novo conjunto de projeto. Não usar a procedure SQL histórica como prova do fluxo Java atual.

## 40. RF32/Galeria

Absorvido pelo RF16 vigente. Tabelas históricas permanecem intactas; não foram reutilizadas como projeto nem criada galeria nova. Futuro filtro/visualização deve consultar projetos publicados, sem copiar conteúdo.

## 41. RF40 separado

Publicação social livre não equivale a projeto profissional. Não existe tabela de RF40 no pacote, não houve feed/API/produção RF40 criada. Título de projeto PUBLICADO continua obrigatório no requisito.

## 42. Paginação

PORT1/PORT2: padrão 20, máximo 50, page >= 0, size fora da faixa 422; query filtra artista no banco e ordena de forma estável. Sem listas ilimitadas ou filtro de página completa em memória. Paginação de projeto ainda inexistente.

## 43. Performance

Listagem lê metadata; bytes só no endpoint de conteúdo e no backup da exclusão individual. DTOs de página não navegam owner por item. Política pública faz consultas auxiliares por operação.

Sem benchmark RNF05 de 100 usuários/p95/p99 ou medição nova de query count do portfólio. Sem projeto, não existe prova projeto→capa→mídias livre de N+1. Regressão verde não é atestado quantitativo de performance/cobertura.

## 44. Banco alterado

**NÃO.** Schema, SQL, enums, FK, constraints, índices, snapshots, migrations e banco de desenvolvimento intactos. DML de fixtures em PostgreSQL descartável é parte dos testes, sem escrever no banco local/oficial.

## 45. Frontend alterado

**NÃO.** Sem npm/build/test/frontend/editor/modal/card/galeria/preview. Maven copia recursos para build backend sem alterar fontes; hashes dos 770 arquivos do frontend conservados.

## 46. Database05 46/46

ZIP: C:\Users\masca\Downloads\palco-database05.zip. SHA-256 **6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf**. Inicial e final: **46/46 idênticos byte a byte; zero divergentes, ausentes ou adicionais**. Inclui arquivo vazio git. Database04 e scripts históricos preservados. snapshot-final-byte-a-byte.json e preservacao-final.json.

## 47. Testes focados

**16 classes; 359 total / 359 passed / 0 failures / 0 errors / 0 skipped; BUILD SUCCESS; exit 0; 03:19 min**, término 04/10/2026 16:43:07 -03:00.

Classes: PortfolioRf16IntegrationTest (26), PortfolioMultipartHttpIntegrationTest (3), PortfolioStorageServiceTest (15), ArquivoPortfolioValidatorTest (67), VideoPortfolioValidatorTest (24), PerfilPublicoRf10IntegrationTest (16), DenunciaRf14Rf18IntegrationTest (21), SalvoRf19IntegrationTest (25), ExclusaoContaRf22IntegrationTest (49), GuardianConsentRf27IntegrationTest (10), GuardianConsentServiceTest (3), CandidaturaControllerRf06IntegrationTest (57), JwtAuthenticationIntegrationTest (4), GenericEndpointsSecurityIntegrationTest (26), Database05BootstrapIntegrationTest (8), OfficialSchemaMappingIntegrationTest (5).

PostgreSQL Testcontainers/database05, PALCO_TEST_DATABASE05_PATH, ddl-auto=validate, sem H2/schema auxiliar. Comando Maven test com filtro real e -Dspring.test.mockmvc.print=NONE. OfficialLocalApiIntegrationTest condicional ao banco local não foi habilitado.

## 48. Maven completo

**67 classes / 1390 total / 1373 passed / 0 failures / 0 errors / 17 skipped; BUILD SUCCESS; exit 0**. Total time:  09:02 min; Finished at: 2026-10-04T16:54:31-03:00.

Comando obrigatório: `.\mvnw.cmd test '-Dspring.test.mockmvc.print=NONE'`, sem clean/filtro/skip novo. Ambiente database05, PostgreSQL real/validate. Não somar a rodada focada à completa. XMLs sanitizados em maven-completo-reports e resultados por classe em maven-completo-totais.json. Os mesmos 17 skips do checkpoint RF36 foram comparados nominalmente: CurrentLocalSchemaIntegrationTest (4) e OfficialLocalApiIntegrationTest (13). Zero classes/casos skipped novos ou ausentes; total e conjunto de classes mantidos. Proveniência registrada em maven-proveniencia.json; inicializações/JDBC ativos database04: zero.

## 49. Semgrep

**Não executado nesta tarefa**, conforme condição do prompt: nenhuma produção Java mudou no caminho B. Não se reutilizou resultado histórico como novo scan nem se declarou secrets scan. Workflow continua Docker oficial quando houver delta Java; Semgrep nativo Windows não foi utilizado. Zero findings isolado não provaria regras de autorização/segurança de negócio/ausência absoluta de segredos.

## 50. Graphify final

Somente auditoria MCP/HTTP; **sem graphify update**, pois não houve alteração Java. Métricas confirmadas: **6.214/21.299/356; confiança 91%/9%/0%**. Hash graph.json conservado. Sem label/graphify-mcp.exe/parser/App Control/Defender/Python/TLS alterados ou contorno de segurança.

## 51. Riscos

Estrutura insuficiente pode induzir projeto falso se mídias forem renomeadas. Retenção pode ser perdida no DELETE individual por falta de guards e CASCADE. Compensação física em memória não é recuperação durável. Moderação e URL de prova polimórficas carecem de vínculo explícito; público individual não equivale a publicação controlada. Provedor externo pode mudar/remover conteúdo. Consentimento no CRUD próprio e avisos RF44 precisam revisão proporcional ao fluxo futuro.

## 52. Limitações

Sem projeto/status/capa/relação 1:N, a tarefa não entrega RF16 revisado. Não houve homologação visual, SMTP real de publicação, benchmark, cobertura percentual nova, antivírus, parser PDF completo ou prova jurídica. Testes atuais não exercitam critérios de projeto inexistente, falha da compensação sob crash nem retenção segura no DELETE individual. Verificação limitada final de 129 arquivos de evidência: zero ocorrências dos valores locais conhecidos >=8 caracteres e zero JWTs assinados; não equivale a scan amplo de segredos. XMLs copiados sem properties de ambiente.

## 53. Menor evolução estrutural conceitual necessária

**Proposta não aplicada; nomes/constraints/migration finais dependem da responsável pelo banco.**

- Um agregado persistente **Projeto de Portfólio**: ID, owner ARTISTA por FK, título permissível em rascunho/obrigatório e não vazio ao publicar, descrição opcional, status usando o enum já oficial, referência explícita de capa e datas de criação/atualização/publicação. RASCUNHO aceita campos incompletos; PUBLICADO exige título+capa+mídia.
- Uma associação persistente **Mídia do Projeto**: ID e FK do projeto, ordem controlável; exatamente um alvo interno portfolio_arquivos ou externo embeds_externos, por FKs verificáveis. Um mesmo item não pode ser sequestrado por outro projeto; unicidade/owner consistente precisam ser garantidos. Arquivos e embeds atuais podem ser reutilizados, sem copiar bytes/conteúdo.
- Capa aponta explicitamente a uma mídia interna elegível do **mesmo** projeto/artista; associação/capa/status não inferidos de ordem/nome/URL. Integridade circular e regras de publicação devem ser validadas no desenho oficial e no backend transacional.
- RF10/RF30: consultar apenas PUBLICADO e paginar; impedir que rota pública antiga de mídia exponha item agora vinculado a RASCUNHO. Compatibilidade deve manter contratos individuais para itens legados ainda sem projeto, com regra explícita e revisão de transição.
- RF18/RF14: alvo inequívoco de projeto/mídia e retenção restrita; tipo_conteudo_enum atual não representa esses alvos. Definir no pacote oficial o vínculo/vocabulário necessário e bloquear delete de evidência até política aplicável, sem prometer autoria jurídica.
- RF19: decidir a semântica de OBRA/projeto antes de habilitar; não reaproveitar IDs polimórficos ambíguos. RF22: orquestrar novo agregado/associações, salvos/evidências e cleanup físico AFTER_COMMIT com reconciliação. RF44: evento real de publicação AFTER_COMMIT, aviso mínimo ao responsável, sem nova autorização e sem duplicar a ação.
- Migração: preservar todos os IDs, bytes, URLs e metadados legados. Não converter arquivo/galeria automaticamente em projeto PUBLICADO nem inferir título/capa. Associação de legado deve ser explícita, revisada pelo artista e idempotente; agrupamentos novos começam privados até publicação válida. Plano de rollback/compatibilidade/retencão depende de análise do pacote futuro.

Backend-only não consegue criar esses vínculos/estados persistentes sem modelo autorizado. Solicitar futuramente **novo pacote completo da Manuela**, sem patch local/híbrido. Esta proposta não define migration como fato consumado e não contém SQL aplicado ou arquivo de migration de exemplo.

Nenhuma mudança foi aplicada ao database05.
A alteração depende de autorização e de um novo pacote oficial completo.

## 54. RF16 final

**BLOQUEADO POR ESTRUTURA** no requisito revisado; suporte individual PORT1/PORT2 **PARCIAL**. Auditoria/regressão documental concluída. Não declarar RF16 backend CONCLUÍDO com arquivos/embeds isolados.

## 55. Pendências

Novo pacote oficial para projeto/capa/mídias/status; retenção no DELETE individual e política física após commit/reconciliação; moderação/denúncia com alvos inequívocos; ordenação e verificação de disponibilidade RF30; publicação RF44; compatibilidade RF19/RF22/rotas públicas com rascunhos. Performance/cobertura/homologação futuras. Nenhuma destas lacunas foi escondida por testes verdes.

## 56. Próximo passo

Revisar a proposta conceitual com Manuela e receber pacote oficial completo autorizado. Depois auditar novamente o modelo, implementar RF16 e testar projetos/drafts/publicação/capa/owner/retencão/integrações. Os riscos independentes dos endpoints atuais podem ser tratados em correção específica, preservando schema/frontend e seus contratos.

## Registro semanal — 04/10/2026

| Campo | Registro |
|---|---|
| Objetivo | Auditar/implementar RF16; caminho B confirmado |
| RF/RNF | RF08/10/14/15/16/18/19/22/27/30/32/36/40/44; RNF02/05/06/07/08/09/10/17 |
| Backend alterado | NÃO; produção e testes preservados |
| Frontend alterado | NÃO |
| Banco alterado | NÃO |
| Database05 | 46/46 byte a byte; fonte oficial ativa; database04 histórico preservado |
| Estrutura de projeto | Ausente; enum de status isolado existe |
| Rascunho/Publicação | Sem persistência de projeto |
| Múltiplas mídias/Capa | Sem vínculo de projeto/sem capa explícita |
| Arquivos | Individuais JPG/JPEG/PNG 5 MiB, PDF 10 MiB, MP3 20 MiB, validação/storage reais |
| Vídeo externo | YouTube/Vimeo/Spotify individuais; sem upload de vídeo |
| Moderação | Barreiras determinísticas; gaps de alvo/retencão/exclusão individual documentados |
| Menor/RF44 | Publicidade individual com consentimento; só aviso de candidatura existente, sem publicação |
| Perfil público | Mídias individuais, sem projetos PUBLICADO |
| Testes | Focados 359/359; 1390 total / 1373 passed / 0 failures / 0 errors / 17 skipped, BUILD SUCCESS |
| Semgrep | Não executado; nenhum delta Java de produção |
| Graphify | MCP HTTP auditado; 6214 nós/21299 arestas/356 comunidades; sem update |
| RF16 final | BLOQUEADO POR ESTRUTURA, suporte individual PARCIAL |
| Blocker estrutural | Projeto/status persistido/capa/associação a arquivos e embeds ausentes |
| Pendências | Modelo oficial e integrações; riscos de storage/retencão individuais |
| Próximo passo | Novo pacote completo oficial autorizado da Manuela, sem aplicar proposta local |

## Inspeção final

git status --short, git diff --stat, git diff --check e git diff --name-status -- backend executados, todos exit 0. **git diff --check: zero erros de whitespace, stdout vazio**; 23 avisos LF→CRLF de arquivos históricos registrados separadamente. **Diff do backend vazio; index vazio; HEAD preservado**. Deltas históricos não foram revertidos/incorporados/staged. Nenhum add/commit/push.

Foram criados somente este relatório e evidencias/rf16-2026-10-04/. Manifestos finais comprovam zero mudanças nos 1.427 arquivos iniciais; nenhum arquivo Java/teste/configuração/SQL/frontend adicionado. Documentos novos também verificados quanto a whitespace e ausência de placeholders.

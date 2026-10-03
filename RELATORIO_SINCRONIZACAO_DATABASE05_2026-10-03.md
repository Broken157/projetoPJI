# Sincronização oficial database05 — Palco — 03/10/2026

**Estado da sincronização: CONCLUÍDA.** Esta tarefa altera ambiente/configuração e infraestrutura de testes. Não implementa notificações RF13/RF36 nem altera a regra RF17 de experiência privada do menor.

Checkout: C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline.
Branch: integracao-recuperada-2026-09-15.
HEAD inicial: 4db3c4a973bbcdba27a20aea7ca571359be700e3 — feat: avanca RF17 com busca no banco de talentos.
Evidências: evidencias/database05-2026-10-03/.

## 1. Objetivo

Tornar database05/palco-database a única fonte oficial ativa do desenvolvimento, Compose, bootstrap e testes PostgreSQL, preservando integralmente database04 e o SQL oficial recebido.

Metodologia: auditar → comparar → sincronizar ambiente → bootstrap → validar Spring → testar → relatar. Sem staging, commit, push, reset, clean, stash ou restauração global.

## 2. Origem do pacote

Pacote completo oficial da Manuela: C:\Users\masca\Downloads\palco-database05.zip. A pasta database05/palco-database já havia sido recebida/copiada pelo usuário antes da execução. Não foi montado pacote híbrido nem copiado apenas um enum para database04.

## 3. SHA-256 do ZIP

SHA-256 esperado e confirmado: **6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf**.

Fingerprint da árvore database05, sobre paths relativos ordenados ordinalmente e hashes dos bytes: **c68460169fcd2538fefd34a109ee3ea7640e553ce1882dd225d88d655229c134**. Manifesto novo em database05/snapshot.properties, fora da pasta oficial.

## 4. Contagem de arquivos

database04/palco-database: **46 arquivos**.
database05/palco-database: **46 arquivos**.
ZIP database05: **46 arquivos regulares**.

Incluído o arquivo incomum palco-database/git, pertencente ao pacote oficial. Nenhum arquivo removido ou acrescentado dentro da pasta oficial.

## 5. Comparação completa database04 × database05

Comparados paths relativos e SHA-256 de todos os arquivos antes de selecionar o snapshot ativo. **45 arquivos idênticos e um diferente; nenhum path adicionado/removido**. Resultado completo em comparacao-04-05.json.

Comparação textual do único arquivo diferente foi apenas leitura. Não presumida previamente uma diferença restrita ao enum.

## 6. Paths diferentes

Único arquivo diferente: **01_types/01_enums.sql**.

SHA-256 database04: 965de578ffd6e85934f8a2fde5d36ef70e5ee2c865d83554f5f5d952db596d53.
SHA-256 database05: 8310d4ab54450fdfba3c3afa54cb0121dd196d1a41325c899d6ac631cf241d25.

init.sql, seed.sql, tabelas, functions, procedures, queries, triggers e demais arquivos possuem os mesmos bytes do database04.

## 7. Diferença semântica

tipo_notificacao_enum passou de cinco para seis labels, adicionando BANCO_DE_TALENTOS ao final. Nenhuma nova tabela, coluna, tipo, function, procedure, trigger, constraint ou índice foi encontrada na comparação dos pacotes.

A medição PostgreSQL posterior confirmou a diferença real; não se concluiu equivalência apenas pela aparência dos arquivos.

## 8. Enum BANCO_DE_TALENTOS

Ordem oficial: CANDIDATURA, MENSAGEM, CONVITE, EDITAL, SALVO, **BANCO_DE_TALENTOS**.

Confirmado no arquivo oficial, no PostgreSQL local, no Compose e no teste de bootstrap. TipoNotificacao Java continua com cinco valores conhecidos: sua extensão e o uso funcional não foram autorizados nesta sincronização.

## 9. Dívidas SQL históricas ainda presentes

| Rotina | Evidência atual no database05 | Limite desta tarefa |
|---|---|---|
| sp_atualizar_perfil | Continua usando foto_perfil; usuarios possui foto_perfil_url | Rotina original intacta; não chamada para contornar bootstrap |
| sp_publicar_vaga | Continua acessando vagas_categorias_afirmativas, ausente do schema atual | Não corrigida, não substituída por patch |
| fn_listar_banco_talentos, declarada em fn_filtrar_banco_talentos.sql | Retorno estado varchar e SELECT p.estado char(2), sem cast correspondente | Incompatibilidade histórica preservada |
| Unicidade física global de candidaturas | Snapshot não adicionou constraint global vaga/artista | Regras RF06 existentes não alteradas |

Os 45 arquivos iguais e o inventário de definitions confirmam continuidade das rotinas. Não foram reexecutadas procedures mutantes contra database04 para demonstrar novamente defeitos históricos. Startup verde não homologa todos os caminhos dessas rotinas legadas.

## 10. Database04 preservado

**SIM.** Comparação final contra o manifesto inicial: 47 arquivos de database04 (46 oficiais + metadata) e cinco scripts históricos com zero divergências. As consultas ao banco anterior foram somente leitura; nenhum reset/drop/bootstrap aplicado nele.

database04/palco-database, seu snapshot.properties, scripts/database04 e ambiente privado anterior não foram retargetados ou editados. Banco local palco_dev_manu04 recebeu apenas consultas de catálogo para comparação.

## 11. Database05 preservado byte a byte

**46/46 idênticos byte a byte ao ZIP original**, com o SHA-256 oficial novamente confirmado após Maven e Graphify. Mesmos paths; zero ausentes, zero adicionais, zero hashes divergentes. Verificação compara os bytes descomprimidos diretamente com cada arquivo local, incluindo o arquivo vazio git. Evidência: snapshot-final-byte-a-byte.json.

Manifesto e evidências estão fora de database05/palco-database. SQL oficial não recebeu formatação, normalização, correção, inclusão ou remoção.

## 12. .gitattributes

As duas regras -text já estavam no working tree antes da tarefa e foram preservadas:

    database04/palco-database/** -text
    database05/palco-database/** -text

git check-attr confirmou text: unset para ambos os arquivos de enum. Não se interpretou whitespace ou line endings originais como autorização para editar o pacote.

## 13. Referências operacionais antigas auditadas

Pesquisa por database04, palco_dev_manu04, PALCO_TEST_DATABASE04_PATH e scripts/database04 em configurações, scripts, Maven e testes.

**Operacionais migradas:** URL default, diretório Maven, Compose, launcher local, resolução/validação de snapshot do Testcontainers, bootstrap e caminhos de testes locais condicionais.

**Históricas preservadas:** relatórios/evidências anteriores, scripts/database04, SQL históricos, nomes/comentários de testes RF17/RF04 e mensagens históricas de VagaService. Essas strings não escolhem o snapshot. Comentário antigo da pendência RF36 permanece para a tarefa funcional posterior; não foi tratado como configuração ativa.

Graphify MCP HTTP foi consultado antes das edições de infraestrutura: query de bootstrap/schema identificou OfficialPostgreSQLContainer, teste anterior e mapeamentos. Resposta ampla mostrou 43/355 nós, com truncamento explícito; fontes completas e PostgreSQL foram a autoridade da auditoria. Nenhum reparo de MCP/Windows/Python.

referencias-auditadas.json separa as categorias. Não houve substituição global no checkout.

## 14. Arquivos operacionais alterados/criados

| Arquivo | Mudança |
|---|---|
| backend/pom.xml | Build isolado em target-maven/database05 |
| backend/src/main/resources/application.properties | URL default palco_dev_manu05; validate mantido |
| backend/.gitignore | Ignorar ambiente privado database05 |
| docker-compose.yml | Projeto/container/volume/snapshot/init database05 |
| run-local.ps1 | Carregar explicitamente environment database05 |
| scripts/database05/* | Cinco scripts equivalentes novos, preservando database04 |
| database05/snapshot.properties | Proveniência e integridade fora do SQL oficial |
| backend/.env.database05.local.ps1 | Ambiente privado novo, credenciais locais existentes sem exposição |
| OfficialPostgreSQLContainer.java, em src/test | Path/variável/fingerprint/init database05; recusa de configuração antiga |
| Database05BootstrapIntegrationTest.java | Renomeação do teste anterior, preservando cinco contratos e acrescentando três provas de infraestrutura |
| ManuDumpSchemaIntegrationTest.java | Proveniência/path/expectativa de SHA atuais |
| OfficialSchemaMappingIntegrationTest.java | Path database05 e comparação exata do enum com um label oficial ainda sem uso Java |
| CurrentLocalSchemaIntegrationTest.java / OfficialLocalApiIntegrationTest.java | URL/banco alvo database05, condições de skip intactas |
| BancoTalentosParticipacaoRf13IntegrationTest.java | Assertion estrutural reconhece seis labels; mantém zero notificações e ausência do valor no enum Java |

Relatório e evidências novos não substituem históricos. Nenhuma classe Java de produção alterada; implementação RF17 e seus 86 testes permanecem byte a byte iguais ao checkpoint.

## 15. Nova fonte ativa

**database05/palco-database**. Não utilizados database04, database02, dump histórico, migration RF07, patches ou scripts de catálogo complementares para inicialização ativa.

Cada bootstrap verifica fingerprint dos 46 arquivos antes do init. O snapshot anterior falha na validação ativa.

## 16. Scripts/database05

Novos scripts: environment.ps1, Initialize-Database.ps1, Start-Backend.ps1, Test-Backend.ps1 e docker-init.sh.

Initialize recusa banco existente, limita nome ao palco_dev_manu05 e sufixos permitidos, exige PostgreSQL 18+, verifica hashes e executa init.sql original com ON_ERROR_STOP=1. Não existe reset/drop de banco de desenvolvimento nesse workflow.

Start-Backend mantém profile banco-oficial-local e desabilita restart do DevTools no smoke. Test-Backend seleciona somente database05; modo local descartável anterior continua separado e protegido por nomes temporários UUID, sem uso do palco_dev_manu04.

## 17. Variáveis de ambiente

Nova variável: **PALCO_TEST_DATABASE05_PATH**; system property correspondente: palco.test.database05-path.

Helper recusa explicitamente PALCO_TEST_DATABASE04_PATH/palco.test.database04-path. Loader novo limpa somente a variável antiga do processo e recusa DB_URL que não aponte para palco_dev_manu05. Não há fallback silencioso para database04.

DB_USER, DB_PASSWORD, JWT_SECRET e JAVA_HOME mantêm contratos anteriores. Ambiente privado database05 usa arquivo próprio ignorado; nenhuma credencial foi impressa no relatório ou incluída em configuração Compose sanitizada. Arquivos privados anteriores conservados.

## 18. Testcontainers

OfficialPostgreSQLContainer carrega pacote completo database05, PostgreSQL **18.4**, imagem postgres:18-alpine. Banco do container: palco_test_manu05. Cópia do pacote para o container e execução psql usam init.sql relativo, incluindo seed oficial exatamente uma vez; validateSnapshot fixa os bytes oficiais.

Três provas adicionais: seis labels reais de notificação; rejeição do fingerprint database04; rejeição da configuração legada. Sem H2, withInitScript de fixture histórica ou DDL complementar.

## 19. Banco local palco_dev_manu05

Criado nesta tarefa, pois consulta prévia confirmou ausência. Servidor local PostgreSQL **18.4 Windows**, localhost:5432, conexão administrativa vigente; novo banco UTF8/template0. palco_dev_manu04 não foi recriado, resetado ou migrado.

Compose separado: projeto palco-manu05, container palco-postgres-manu05, volume palco-manu05_palco_manu05_data, porta host 5435. Logs confirmam criação de volume/container novos, estado healthy e database05. Não houve docker compose down -v nem remoção de volume histórico.

## 20. Bootstrap init

**init.sql exit 0**, com ON_ERROR_STOP=1, VERBOSITY=verbose e execução relativa dentro do pacote original. Log init-seed-local.log e resumo bootstrap-local-resumo.json.

Bootstrap termina com mensagem oficial de conclusão. Sem ERROR/FATAL, sem SQL adicional de correção. Inventário medido após inicialização, não inferido de sucesso do comando.

## 21. Seed

**Seed concluído dentro do mesmo init.sql, exit global 0**. O contrato oficial inclui `\i seed.sql`; não foi aplicado novamente. Não existe exit de processo separado para seed, pois a execução é única.

Catálogos/conta reservada/fixtures oficiais continuam sob assertions preservadas do bootstrap. Dados artificiais de integração são DML apenas nos bancos descartáveis dos testes.

## 22. Inventário estrutural

Inventários PostgreSQL completos, incluindo definitions e PK/FK: inventario-database04.json, inventario-database05.json e inventario-comparacao.json.

| Objeto | Database04 | Database05 | Comparação |
|---|---:|---:|---|
| Tabelas | 43 | 43 | Idênticas |
| Colunas | 280 | 280 | Idênticas, incluindo defaults/tipos/nullabilidade |
| Enums | 24 | 24 | Mesmo conjunto de tipos |
| Labels de enums | 98 | 99 | Apenas BANCO_DE_TALENTOS |
| Functions | 12 | 12 | Definitions idênticas |
| Procedures | 12 | 12 | Definitions idênticas |
| Triggers não internos | 3 | 3 | Idênticos |
| Constraints totais | 280 | 280 | Definitions idênticas |
| PKs | 43 | 43 | Idênticas |
| FKs | 57 | 57 | Idênticas |

PK composta banco_talentos(contratante_id,artista_id), seus relacionamentos e integridade de username preservados. Consulta de catálogo somente leitura. IDs internos do PostgreSQL não foram usados para afirmar equivalência.

## 23. Spring ddl-auto=validate

**Mantido validate** nos properties, profile e testes. EntityManagerFactory inicializou contra palco_dev_manu05 sem SchemaManagementException. Nenhum create/update/create-drop ou retirada de Entity/constraint.

Labels adicionais do enum não exigiram adaptação funcional Java para inicializar. Isso não comprova que o Java atual consegue ler notificações com o novo label; tal uso permanece fora desta tarefa.

## 24. Startup

BackendApplication iniciou em **8,336 s**, Tomcat 8080; log confirma jdbc:postgresql://localhost:5432/palco_dev_manu05?stringtype=unspecified e catálogo palco_dev_manu05/public. PostgreSQL 18.4, JDK 21.0.11, Spring Boot 4.0.6, Hibernate 7.2.12.Final.

Processo de smoke iniciado pela tarefa foi encerrado por Ctrl+C após validação, antes dos testes, evitando duas execuções Maven simultâneas no mesmo build. Exit de interrupção não representa falha de startup. Compose permanece disponível. spring-startup.log e spring-resumo.json preservam as evidências.

## 25. Smoke tests

**6/6**: GET /api/capacidades, /api/areas, /api/vagas e /api/perfis/publicos/ARTISTA/1 → 200; GET /api/talentos anônimo e POST /api/auth/login com e-mail inexistente → 401.

Primeiro payload de login usou identificador e recebeu 400; contrato LoginRequest exige email. Corrigido somente o payload, reexecutada apenas a chamada de login. Diagnóstico preservado em http-smoke-diagnostico.json; resultados finais em http-smoke.json/http-smoke-resumo.json. Nenhum backend alterado para esse diagnóstico.

## 26. Testes focados

**268 total / 268 passed / 0 failures / 0 errors / 0 skipped; 15 classes; BUILD SUCCESS; exit 0; 03:22 min; término 03/10/2026 17:28:34 -03:00.** Evidências: focados-final.log, focados-final.exit, focados-final-totais.json e focados-final-reports/.

15 classes: Database05BootstrapIntegrationTest, ManuDumpSchemaIntegrationTest, OfficialSchemaMappingIntegrationTest, ValidatedOfficialSqlIntegrationTest, AuthControllerRf01Rf02IntegrationTest, JwtAuthenticationIntegrationTest, GenericEndpointsSecurityIntegrationTest, BancoTalentosParticipacaoRf13IntegrationTest, BancoTalentosBuscaRf17IntegrationTest, NotificacaoRf23IntegrationTest, NotificacaoEventoListenerTest, NotificacaoSsePoolIntegrationTest, NotificacaoWebSocketRf23IntegrationTest, ChatRf24IntegrationTest e ChatWebSocketRf24IntegrationTest.

Diagnóstico inicial: **268 total / 266 passed / 2 failures / 0 errors / 0 skipped, 03:13 min, exit 1**. Duas assertions antigas exigiam cinco labels/igualdade integral Java-PostgreSQL; adaptadas explicitamente ao snapshot novo. Comparação continua exata: Java conhecido + único label de snapshot autorizado; demais divergências falham. RF13 mantém assertion de zero notificações e enum Java sem novo valor. Nenhuma assertion funcional apagada ou skip criado. Evidências focados-diagnostico-*.

## 27. Maven completo

**BUILD SUCCESS; exit 0; 63 classes; duração 07:18 min; término 03/10/2026 17:36:51 -03:00.** Evidências: maven-completo.log, maven-completo.exit, maven-completo-totais.json e maven-completo-reports/.

Comando no backend: `.\mvnw.cmd test '-Dspring.test.mockmvc.print=NONE'`. Sem clean, filtro de classes ou testes desabilitados. Ambiente database05 e PALCO_TEST_DATABASE05_PATH carregados antes de executar.

## 28. Totais

**1135 total / 1118 passed / 0 failures / 0 errors / 17 skipped.** Totais consolidados dos XMLs desta rodada, coerentes com o resumo Maven. Bateria focada final: **268/268**, sem skips.

17 skips condicionais preexistentes permitidos: CurrentLocalSchemaIntegrationTest 4 e OfficialLocalApiIntegrationTest 13. Condições mantidas; sem novo skip. Integrações locais condicionais não contam como cobertura executada.

Baseline RF17: 1132 casos. Renomeação de bootstrap conserva cinco; três novos testes de infraestrutura totalizam **1135 efetivamente medidos**. Não houve redução de casos, remoção de assertion funcional ou adição de skip.

## 29. Database05 comprovado nos testes

Bootstrap verificou fingerprint dos 46 arquivos, nome real do banco e enum de seis labels no PostgreSQL 18.4. Maven registra inicializações `[database05]` e URLs `palco_test_manu05`; nenhuma inicialização `[database04]` ou URL de container `palco_test_manu04`. Evidência resumida em maven-proveniencia.json.

Os **335 arquivos Java** usados na bateria final mantiveram seus hashes após os testes. Arquivos de produção e teste RF17 também coincidem com o manifesto anterior à tarefa, comprovando que a regressão verde corresponde às fontes entregues.

Proveniência é demonstrada por fingerprint, logs de init, URL/nome do container e consulta real do enum, não apenas por nome de classe ou variável. Relatórios XML copiados omitindo properties de runtime, conservando testcase/resultados; consolidação considera apenas XMLs posteriores ao início da rodada.

## 30. Database04 não utilizado na inicialização nova

Helper ativo não resolve caminho antigo; configuração antiga é recusada e bytes do snapshot antigo falham na validação. Um teste lê o fingerprint histórico apenas para provar a rejeição, sem carregar SQL database04 no PostgreSQL.

Nomes históricos remanescentes de RF13/RF17/SQL não são seleção de source. Scripts antigos não foram reescritos para esconder histórico; executar script antigo contra infraestrutura nova provoca recusa, exigindo o workflow database05.

## 31. Frontend alterado: NÃO

**770 arquivos de frontend preservados**, com zero divergências contra o manifesto inicial e nenhum novo arquivo nas raízes protegidas. Alterações frontend já presentes no Git são anteriores à tarefa e não foram reescritas. Evidências: preservacao-final.json e paths-protegidos-final.json.

Sem npm install/build/format, alteração visual, salvamento de fonte frontend ou integração nova. Cópia de recursos existentes para target Maven é derivado de backend, sem editar origens.

## 32. SQL oficial alterado: NÃO

**NÃO.** Database05 permanece 46/46 idêntico ao ZIP e database04 inteiro conserva os hashes iniciais. SQL histórico preexistente também foi preservado. Manifesto final de 1298 arquivos protegidos registra zero mudanças, incluindo os **266 arquivos Java de produção**, frontend, SQL e documentação histórica.

Criar banco/volume novos e executar init oficial é bootstrap autorizado, não edição de schema local. Manifesto snapshot.properties está fora de palco-database e não altera os 46 arquivos do pacote.

## 33. Semgrep

**Semgrep não reexecutado porque a tarefa não alterou Java de produção.**

Ajustes Java estão exclusivamente em infraestrutura/testes. Semgrep nativo Windows não utilizado; TLS/App Control/Defender/Python intactos. Resultado do RF17 permanece histórico, não é apresentado como novo scan database05.

## 34. Graphify

**Executado `graphify update .`, exit 0.** Grafo final: **5867 nós / 19577 arestas / 345 comunidades**. O Graphify MCP HTTP confirmou os mesmos totais após o update (91% EXTRACTED, 9% INFERRED, 0% AMBIGUOUS). Evidências: graphify-update.log, graphify-update-resumo.json e graphify-mcp-stats-final.txt.

Limites registrados pela ferramenta: parser tree_sitter_sql ausente, portanto 45 arquivos SQL não contribuíram para o grafo; nomes de comunidades usam labels anteriores, sem novo label/LLM. Nenhuma instalação ou reparo foi feito. A auditoria SQL utiliza fontes completas e inventários PostgreSQL, não cobertura SQL do grafo.

Update justificado pelas mudanças estruturais na infraestrutura Java de testes (classe bootstrap e métodos do helper), além dos scripts/configuração. Sem graphify label, graphify-mcp.exe ou reparos de segurança. Derivados não stageados; atualização AST não homologa o banco.

## 35. Riscos

Rotinas SQL legadas mantêm defeitos descritos. Java ainda não reconhece BANCO_DE_TALENTOS: uma gravação externa desse tipo pode provocar erro de conversão ao lê-lo; implementar representação/fluxo em tarefa funcional antes de usar o label.

Credenciais privadas e DB_URL explícitos continuam responsabilidade do ambiente; loader rejeita dev errado, sem mudar variáveis globais do Windows. Compose em 5435 e PostgreSQL local em 5432 são instâncias distintas com mesmo pacote, não bancos compartilhados.

## 36. Incompatibilidades

Não encontrada incompatibilidade impeditiva entre o backend atual e o schema database05 para startup/fluxos existentes. As duas falhas iniciais eram expectativas estruturais do snapshot anterior, corrigidas nos testes sem alterar produção.

Dívidas de procedures/function e label ainda sem uso Java permanecem limites explícitos. Não se alega validação funcional das rotinas não executadas, carga ou homologação de todos os caminhos possíveis.

## 37. RF13 estruturalmente desbloqueado

**SIM**, quanto à representação oficial de tipo_notificacao_enum. Nenhuma notificação, evento after-commit, endpoint ou enum Java novo foi implementado.

“database05 disponibiliza representação oficial BANCO_DE_TALENTOS e, se toda a sincronização for aprovada, isso permite uma tarefa posterior de conclusão RF13/RF36.”

RF13 permanece PARCIAL nesta tarefa: participação existente funciona; a conclusão da notificação exige implementação e testes funcionais posteriores.

## 38. RF17 preservado

Classes Java de produção e BancoTalentosBuscaRf17IntegrationTest mantidos como checkpointados. RF17 continua PARCIAL pela decisão sobre experiência privada do menor. Sua regressão de 86 casos foi executada no pacote novo; nenhuma decisão funcional alterada.

## 39. Pendências

Implementar RF13/RF36 com enum Java, persistência/evento e regressões próprias em tarefa posterior autorizada. Decidir experiência privada do menor no RF17 separadamente. Dívidas SQL só podem ser tratadas em novo pacote oficial, sem patch local.

17 integrações condicionais antigas não executadas permanecem limite de cobertura. Nenhuma pendência funcional foi disfarçada de problema de sincronização.

## 40. Conclusão

**Sincronização database05 CONCLUÍDA** para ambiente, bootstrap e regressão do backend atual. Startup com validate e seis smokes passaram; focados 268/268; Maven 1135 total / 1118 passed / zero failures/errors / 17 skips antigos, BUILD SUCCESS. Preservação final do pacote e do baseline foi confirmada após testes e Graphify.

Banco completo oficial selecionado; banco local novo e Compose separados; SQL database05/database04 preservados; Java de produção/RF17/frontend intactos. Sem staging, commit ou push. Alterações históricas continuam no working tree e não pertencem ao delta desta tarefa.

Inspeção Git final registrada em git-status-final.txt, git-diff-stat-final.txt e git-diff-check-final.txt; códigos de saída em git-inspecao-final.json. A diferença global inclui alterações preexistentes. O delta específico desta tarefa contra o manifesto inicial consta em delta-final.json; a preservação dos demais arquivos consta em preservacao-final.json e paths-protegidos-final.json.

## 41. Próximo passo

Revisar este delta de sincronização e suas evidências. Depois, executar tarefa própria de conclusão RF13/RF36 sobre database05, mantendo a pendência RF17 e a governança do pacote oficial.

## Registro semanal

**Data:** 03/10/2026.
**Objetivo:** sincronizar ambiente com database05 oficial.
**RF/RNF relacionado:** infraestrutura de RF13/RF17/RF36; RNF02/RNF07/RNF08/RNF10.
**Backend/configuração alterado:** scripts/configuração e infraestrutura de testes; Java de produção não alterado.
**Frontend alterado:** NÃO.
**Banco oficial alterado localmente:** NÃO.
**Snapshot ativo anterior:** database04.
**Novo snapshot ativo:** database05.
**Database04 preservado:** SIM, 47 arquivos e cinco scripts sem divergência.
**Database05 46/46 idêntico:** SIM, comparação byte a byte com ZIP original.
**Diferenças relevantes:** único label BANCO_DE_TALENTOS; 45/46 arquivos iguais, mesma estrutura exceto 98→99 labels.
**Testcontainers:** fonte database05, validação de fingerprint e rejeição de configuração antiga.
**Banco local:** palco_dev_manu05 criado em PostgreSQL 18.4; Compose novo em volume próprio, healthy.
**ddl-auto:** validate.
**Testes/resultados:** focados 268/268; Maven completo 1135 total / 1118 passed / 0 failures / 0 errors / 17 skipped, BUILD SUCCESS em 07:18 min; seis smokes; Graphify update exit 0; Semgrep dispensado por ausência de alteração Java de produção.
**Pendências:** conclusão funcional RF13/RF36, decisão RF17, dívidas SQL oficiais.
**Próximo passo:** revisar sincronização e tratar RF13/RF36 separadamente.
**RF13:** permanece PARCIAL, com representação estrutural oficialmente disponível para conclusão posterior.
**RF17:** permanece PARCIAL pela experiência privada do menor.


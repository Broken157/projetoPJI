# RF11 — Painel principal / Dashboard — 05/10/2026

**RF11 backend: CONCLUÍDO.** Focados 836/836; Maven completo 1493 total / 1476 passed / 0 failures / 0 errors / 17 skipped históricos, BUILD SUCCESS. Semgrep/grafo confirmados; database05/frontend/históricos preservados. Integração/homologação visual pendente; RF17 continua PARCIAL. Sem staging, commit ou push.

## 1. Objetivo

Auditar e concluir o agregador profissional RF11, reutilizando os módulos atuais e mantendo database05, frontend e históricos. Fluxo: auditoria → correções mínimas → testes focados → Maven completo → Semgrep Docker → Graphify AST/MCP → preservação e relatório.

## 2. Requisitos

Fonte oficial atual: C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt, SHA-256 3e7ca8fa7e78a1967cf5f86c86f6a60f736879b02cfbd8f9d195040fea873183. RF02/03/06/08/11/13/17/19/23/28/35/36/42/45/27/37; RNF02/05/06/07/08/09/10/13/17. RF19 somente para comprovar neutralidade de Salvos; RF42 somente na regressão compartilhada. Fonte selecionada em evidencias/rf11-2026-10-05/requisitos-consultados.txt.

Relatórios RF03, RF06, participação RF13 e integração RF13/RF36, RF17, RF35, RF36 concluído, RF45, RF18, sincronização database05 e MAPA_INTEGRACAO_FRONTEND_API (DASH1) usados como contexto. Prevalecem requisitos atuais, código e database05; conclusões históricas não foram reescritas.

## 3. HEAD e checkpoint

Checkout C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline; branch integracao-recuperada-2026-09-15. HEAD inicial **4ce8ddb3371fcd817d217e520be22e966926312b**, checkpoint “feat: avanca RF18 com moderacao e denuncias”. Upstream igual ao HEAD; fork **0/0** após fetch. Índice vazio e nenhum delta RF18 solto em backend/database05/database04/scripts. Working tree histórico sujo preservado, sem reset/clean/stash/restore, staging, commit ou push. Manifesto inicial: 1437 arquivos.

## 4. Graphify inicial

MCP HTTP disponível e consultado antes da exploração/alteração de produção: **6319 nós, 21666 arestas, 343 comunidades; EXTRACTED 91%, INFERRED 9%, AMBIGUOUS 0%**. Dashboard/DTOs/repos, Vaga/RF03/prazo, Candidatura/RF45, Chat, Notificacao, Talento/Banco, JWT/Security e política de menor orientaram a auditoria. Consultas tiveram truncamento de orçamento; leituras diretas decidiram as conclusões. Evidências graphify-inicial-*.json e auditoria-dashboard-inicial.md.

## 5. Database05

Único snapshot ativo: database05/palco-database. PostgreSQL 18 real/Testcontainers, PALCO_TEST_DATABASE05_PATH oficial e ddl-auto=validate. DML de fixtures somente nos containers descartáveis. Sem H2, patch, migrations, schema auxiliar, índices ou alteração de scripts operacionais. database04 é histórico preservado.

## 6. Dashboard inicial

GET /api/dashboard, identidade do token/persistência, switch explícito de papéis, perfilCompleto, avatar, recomendação real por funções, candidaturas recentes próprias e talentos do Banco próprio já existiam. COUNT de chat RF35 também existia. Lacunas: notificações sem contador, ausência de candidaturas próprias do artista/vagas próprias, matching sem área/especialização/prazo, recentes sem vigência RF45 e size até 50 sem teto específico de preview. Auditoria A–X foi escrita antes do patch.

## 7. Endpoint

Preservado **GET /api/dashboard?size=1..50**. Identidade não aceita userId/artistId/contratanteId como autoridade. Nenhuma rota alternativa de Dashboard criada. size ausente continua 5; prévias recebem min(size,5). size inválido continua 400 com mensagem anterior.

## 8. DTO inicial e final

Preservados tipoUsuario, nomeExibicao, avatarUrl, perfilCompleto, notificacoes, mensagens, vagasRecomendadas, candidaturasRecentes e talentosSugeridos, além de content/totalElements/hasMore e campos dos cards. Adicionados perfilIncompleto; minhasCandidaturas no ARTISTA; minhasVagas/vagasPorStatus/quantidadeBancoTalentos no CONTRATANTE; quantidadeNaoLidas em notificacoes; área compatível, contagem de especializações e motivoRecomendacao nos cards de vagas. Nenhum campo consumido renomeado/removido. Prévia do artista reaproveita DTO de candidatura sem preencher identificação desnecessária do candidato.

## 9. Arquivos de produção

11 Java: DashboardService, VagaService, VagaRepository, CandidaturaRepository, BancoTalentosRepository, DashboardResponse, DashboardVagaResponse, VagaRecomendadaProjection; novos DashboardVagaPropriaResponse, VagaDashboardProjection e VagaStatusProjection. Relação exata em escopo-autorizado.json. Nenhum pom, propriedade, dependência, entity, SQL, bootstrap ou frontend alterado.

## 10. Arquivos de teste

DashboardRf11IntegrationTest ampliado de 14 para 43 casos. ModeracaoRf18IntegrationTest ajusta apenas a chamada do mecanismo compartilhado substituído e explicita área do artista na fixture; conserva o assert de ocultação e total. Nenhum teste desabilitado ou skip novo. Asserts legados de função/20 itens foram atualizados à elegibilidade por área e ao preview de 5, mantendo total/hasMore e comparação de consultas.

## 11. Autenticação

JWT ausente/inválido/expirado/revogado/subject removido → 401. JwtAuthFilter revalida assinatura, ID e usuário atual; AuthenticatedUserResolver resolve a persistência, sem IDs do cliente. Query/body extra não substituem ator. Endpoints de destino conservam suas próprias autorizações RF06/RF45/RF35/RF17.

## 12. Estado da conta

Política persistente GoogleAccountAccessPolicy é reavaliada no filtro e no agregador. PENDENTE_VERIFICACAO_EMAIL, PENDENTE_TIPO_PERFIL, PENDENTE_CONSENTIMENTO e BLOQUEADA com JWT antigo não autenticam (401 na política vigente). Nenhum acesso profissional baseado apenas em claim antiga.

## 13. Papéis

ARTISTA → módulos do artista; CONTRATANTE → módulos do contratante; ADMIN/MODERADOR → 403. Não existe fallback “qualquer não artista é contratante”. Dashboard administrativo separado não foi criado/alterado.

## 14. Artista

Identidade pública mínima, avatar determinístico existente, completude/aviso, vagas profissionais elegíveis, prévia/total das próprias candidaturas e contadores reais de mensagens/notificações. Nenhum carregamento de módulo completo ou dado pessoal privado.

## 15. Perfil incompleto

Consome a flag persistida RF08 sem recalcular critérios ou alterar o usuário. perfilIncompleto é a negação de perfilCompleto. Incompleto continua abrindo o painel e pode receber oportunidades pela área cadastrada; POST RF06 conserva bloqueio 422 e nenhuma candidatura é criada.

## 16. Recomendação

O único mecanismo legado foi evoluído no módulo de vagas: VagaService.recomendarParaArtista → VagaRepository.findRecomendadasParaArtista. Dashboard somente chama e apresenta resultados. Área principal/secundárias conferem a mesma elegibilidade; ordenar por funções compatíveis DESC, especializações compatíveis DESC, publicação DESC NULLS LAST e ID DESC. Contagens em SQL, antes da paginação. Especialização exige função comum e relação real no catálogo da mesma área.

## 17. Explicabilidade

Card informa areaId/areaCompativel, quantidadeFuncoesCoincidentes, quantidadeEspecializacoesCoincidentes e motivo textual com as contagens e desempates reais. Uma vaga pode ser elegível pela área com zero funções em comum; texto não afirma coincidência inexistente. Sem score opaco, percentual inventado ou experiência privada.

## 18. RF03

A auditoria encontrou recomendação legada somente por função no Dashboard; o feed RF03 possuía filtros taxonômicos, ABERTA, prazo e moderação, porém não um matching hierárquico completo reutilizável. A correção necessária substitui essa consulta e concentra o único matching no serviço de vagas, com a regra RF03. Não há segundo algoritmo no Dashboard. A busca pública mantém seus filtros/cursor por ID e o contrato atual; este trabalho não declara RF03 inteiro concluído. Prazo vem de VagaPrazoPolicy; bloqueios RF18 permanecem no SQL antes de total/LIMIT.

## 19. Candidaturas do artista

Consulta projetada por artista autenticado, ordem data/ID DESC, COUNT e preview até 5. totalElements representa tentativas históricas próprias, inclusive retiradas quando existentes; não é contador de candidaturas ativas. Status são os persistidos. Retirada/recandidatura e autorizações continuam RF06; nenhum ACEITA/REJEITADA ou transição foi criado.

## 20. Mensagens do artista

COUNT existente RF35: somente sala com participação atual do usuário; não lida recebida exclui remetente igual ao próprio usuário e respeita mensagens com remetente removido. Nenhum preview/histórico, texto original ou referência de anexo no Dashboard. Não cria sala nem marca leitura.

## 21. Notificações do artista

NotificacaoService.contarNaoLidas reutiliza COUNT do destinatário RF36. Inclui quantidadeNaoLidas no DTO existente de disponibilidade. Não lista todas para contar, não entrega notificação alheia, não marca lida e não cria evento ao renderizar.

## 22. Contratante

Identidade pública mínima, mensagens/notificações próprias, resumo de vagas próprias em todos os estados reais, candidatos vigentes de vagas próprias e acesso ao Banco próprio com count e sugestões RF17 existentes.

## 23. Vagas próprias

Projeção mínima com id/título/status/publicação/prazo e flag prazoVencido; consulta por dono autenticado, data/ID DESC, COUNT e preview. vagasPorStatus usa GROUP BY SQL com zeros para estados sem ocorrência. prazoVencido usa VagaPrazoPolicy; a leitura não encerra a vaga nem executa job RF23.

## 24. Candidaturas recentes

Preservada seção operacional ABERTA/PAUSADA consumida pelo frontend. Corrigidos status PENDENTE/EM_ANALISE, ID de artista positivo e ausência de tentativa posterior por vaga/artista, inclusive quando a tentativa posterior foi retirada — mesma vigência RF45. Owner/total/página usam os mesmos filtros. Histórico retirado não volta junto da tentativa atual. Projeção mantém somente IDs, nomes públicos, avatar, vaga, data e status.

## 25. Mensagens do contratante

Mesma query RF35 de participação e remetente recebido; o papel não concede leitura de sala alheia. Banco/candidatura/convite no painel não criam chat. Atalho conserva autorização do endpoint especializado.

## 26. Notificações do contratante

Mesmo COUNT RF36 do próprio destinatário, sem corpo de alertas ou mutações. Testes por ambos os papéis incluem outra conta e notificação já lida.

## 27. Banco de Talentos

quantidadeBancoTalentos conta relações persistidas do próprio contratante em SQL; é contagem de memberships, não de perfis atualmente elegíveis à sugestão. talentosSugeridos mantém TalentoService.recomendarDoContratante, contexto de vaga própria e filtros RF17 antes de total/página. Sem vaga de contexto, sugestões vazias mesmo que haja memberships. Nenhuma inclusão/exclusão automática.

## 28. RF17

Continua **PARCIAL** pela decisão pendente sobre experiência privada de menor. Dashboard não passa filtro de experiência, não projeta essa experiência e não altera a política. Testado menor autorizado do Banco próprio; público fora do Banco e membro de outro contratante não entram. RF13 backend concluído e RF36 concluído são checkpoints posteriores aos históricos parciais.

## 29. Ranking e engajamento

Matching usa somente taxonomia/publicação/ID. Sem acesso a seguidores, medalhas, conquistas, visualizações, salvos, chats ou curtidas para ordenar. RF19 testado: salvar a vaga de menor relevância não muda a resposta. Campos legados de contagem dos talentos continuam JsonIgnore; não foram reintroduzidos na API.

## 30. Privacidade

DTOs/projeções sem CPF/CNPJ/telefone/email privado/nascimento/responsável/consentimento/senha/refresh token/experiência privada/texto_original/storage path. Asserts percorrem recursivamente o JSON e verificam valores sentinela privados. Identidade/nome/biografia/URL pública nos cards legados permanecem conforme contratos profissionais existentes.

## 31. Menor

MenorAutorizadoPolicy exige idade 14–17, conta ATIVA, responsável com consentimento persistido e não revogado segundo proteção vigente. Abaixo de 14/sem autorização/consentimento revogado → 403 no agregador. Guard adicional sem exposição de dados do responsável. RF45/RF17 mantêm minimização; nenhuma decisão pendente de experiência privada foi resolvida.

## 32. Efeitos colaterais

Dashboard @Transactional(readOnly=true). Não chama save/update/chat/read/membership/notificação/candidatura/transição de vaga. Snapshots integrais comparados antes/depois para usuários, perfis/áreas, vagas, candidaturas, salas/participantes/mensagens, notificações e Banco. Testes RF06 confirmam que ler o painel não concede candidatura.

## 33. Paginação e limites

Todas as prévias têm content/totalElements/hasMore. Default histórico 5; máximo efetivo 5 mesmo quando size=20/50. Validação pública 1–50 preservada. Teto de 5 é decisão técnica do agregador, não número obrigatório do RF11. Listagens completas continuam endpoints especializados, com RNF17 próprio. Data/publicação e ID estabilizam empates.

## 34. Performance / N+1

Cenário de 20 vagas ou 20 candidatos/membros compara preview 1 e 5. Artista: 10 consultas JPA em ambos; contratante: 15 (1) / 14 (5), com 16 invocações query* do NamedParameterJdbcTemplate em ambos. Invocações incluem sobrecargas internas, não devem ser somadas ao JPA como número exato de SQL. A diferença de uma consulta é a otimização COUNT do Pageable para a única vaga própria. Nenhum crescimento linear ao aumentar itens; RF17 carrega taxonomia em lote e recomendação carrega IDs limitados com EntityGraph após página.

Não foi inventado teto absoluto novo. Existem revalidações fixas de identidade nos serviços compartilhados; não há laço de consulta por card. RNF05 de 100 usuários, p95≤1s/p99≤2s e 15 minutos não foi validado por este teste.

## 35. Compatibilidade frontend

Auditoria somente leitura de frontend/src/pages/account/DashboardPage.jsx e accountService: contrato GET real, campos dos cards e booleans preservados; notificacoes.quantidadeNaoLidas é aditivo. Frontend já apresenta aviso perfilCompleto=false. Textos legados exigem futura adequação: completude menciona portfólio/localização e talentos descreve “ativos e completos”, enquanto o contrato revisado tem suas próprias políticas. Novos resumos/explicações ainda precisam de integração visual. Auditoria histórica F/G demo não é prova de consumo atual.

## 36. Banco alterado

**NÃO.** Nenhum snapshot, SQL, índice, FK, enum, migration, trigger, bootstrap ou script operacional alterado. Banco de desenvolvimento não iniciado/manipulado; DML de teste somente em PostgreSQL descartável oficial.

## 37. Frontend alterado

**NÃO.** Arquivos/frontend, inclusive deltas históricos anteriores, protegidos por hashes iniciais. Nenhum React, teste frontend, CSS, mock, asset ou ligação de API foi alterado.

## 38. Database05 46/46

Inicial e final: ZIP oficial **6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf**, **46/46 idênticos byte a byte**; zero divergentes/ausentes/adicionais. Manifesto protegido final: 1427 arquivos, zero mudanças; inclui 770 frontend, 47 database04 e cinco scripts database04. Dez Java existentes autorizados excluídos dos 1437 iniciais; três Java novos fora do manifesto inicial. As 365 fontes/recursos finais mantêm hashes após testes. Evidências snapshot-final-byte-a-byte.json, preservacao-final.json, delta-final.json e fontes-testadas-final.json. Working tree histórico não atribuído ao RF11.

## 39. Focados

**836 total / 836 passed / 0 failures / 0 errors / 0 skipped; BUILD SUCCESS; 5min34s.** Uma única bateria de 27 classes reais em classes-focadas.txt; totais/classes/durações em maven-focado-final-totais.json. RF11: **43/43**. Nenhuma soma de baterias sobrepostas.

Tentativas preservadas: rf11-diagnostico — compilação passou, mas Docker desligado impediu Testcontainers (dois erros de inicialização); Docker Desktop instalado iniciado normalmente, sem alteração de segurança. rf11-diagnostico-docker — 81 total/79 passed/2 failures/0 errors/0 skips; asserts novos corrigidos para examinar apenas candidaturas próprias e permitir a economia de COUNT do paginador. Nenhum comportamento produtivo alterado para satisfazer esses asserts, nenhum caso desabilitado. Logs, exits e cópias sanitizadas de XML mantidos.

## 40. Maven completo

**1493 total / 1476 passed / 0 failures / 0 errors / 17 skipped; BUILD SUCCESS; 69 classes; 8min33s.** Comando .\mvnw.cmd test '-Dspring.test.mockmvc.print=NONE', sem clean, executado depois do foco. PostgreSQL real database05 e validate, sem inicialização database04/H2/patch.

Baseline RF18: 1464/1447/17; resultado medido adiciona 29 casos RF11. **Mesmos 17 skips nominais**, zero skips novos e nenhuma classe anterior ausente: CurrentLocalSchemaIntegrationTest (4) e OfficialLocalApiIntegrationTest (13), nomes completos em regressao-comparacao.json. Totais finais não somados aos focados. Evidências maven-completo-totais.json, maven-completo-reports e maven-proveniencia.json. Percentuais de cobertura RNF10 não medidos nesta tarefa.

## 41. Semgrep

Docker oficial semgrep/semgrep@sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b, versão **1.178.0**. **p/java; 360 arquivos; 60 regras; ~100.0% das linhas parseadas; 0 findings; 0 blocking; 0 errors; exit 0; 35,1s.** Todos os 13 Java alterados/novos cobertos, inclusive os três untracked.

Produção/testes montados somente leitura. Cache oficial do ruleset auditado pelo SHA-256 5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1; rede do container desativada. Ignore substituído somente no container para incluir testes; ignore do repositório preservado. Semgrep Windows nativo não usado, TLS não desabilitado; nenhum scan secrets amplo executado. Evidências semgrep-java-resumo.json, semgrep-java.json, log e exit. Verificação local de evidências considera apenas valores de segredo conhecidos e JWT assinados, não substitui scan amplo.

“0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.”

## 42. Graphify

**graphify update . AST-only, exit 0; 27,9s.** MCP HTTP final: **6365 nós / 21886 arestas / 343 comunidades; EXTRACTED 91%, INFERRED 9%, AMBIGUOUS 0%**. DashboardService reconhecido na fonte atual (grau 35), com buscarMinhasCandidaturas/buscarMinhasVagas/notificacoesDisponiveis/vagasPorStatus; chamada ao novo método compartilhado VagaService.recomendarParaArtista confirmada no grafo e no Java. Hash do grafo mudou após atualização.

Evidências graphify-final-resumo.json, graphify-final-dashboard.json, graphify-final-recomendacao.json e grafo-atualizacao-final.json. Sem label, graphify-mcp.exe, instalação de parser ou contorno de App Control. Algumas arestas de chamada são INFERRED: Java e testes confirmam sua existência; grafo não substitui prova funcional.

## 43. Riscos

Integração/homologação visual não executada; textos atuais do frontend ainda precisam de revisão. Mudança de elegibilidade permite card da mesma área mesmo sem função coincidente, explicado com zero real. Prévia máxima 5 pode oferecer menos itens que clientes legados pedindo size maior; total/hasMore e endpoints completos permanecem. RF17 tem pendência funcional própria.

## 44. Limitações

PostgreSQL READ_COMMITTED; múltiplos SELECTs podem observar commits entre consultas, sem prometer snapshot distribuído. Não houve teste de carga de 100 usuários nem p95/p99. Revalidações fixas nos serviços permanecem. RF16 bloqueado e RF18 parcial não foram alterados; Semgrep/grafo complementam testes e fontes.

## 45. RF11 final

**RF11 backend CONCLUÍDO** no escopo revisado. Critérios A–Q atendidos por fontes, 43 casos RF11, foco/full verdes, Semgrep/grafo e preservação. Matriz em evidencias/rf11-2026-10-05/matriz-criterios-rf11.md. Não se declara frontend homologado, RNF05 de carga/RNF10 de cobertura inteiros concluídos nem RF17/RF16/RF18 resolvidos.

## 46. Integração visual

**Integração/homologação visual pendente.** Implementação backend e compatibilidade de shape não comprovam layout, fluxo demonstrativo ou consumo visual dos novos campos.

## 47. Pendências

Validações obrigatórias desta tarefa concluídas. Em tarefa própria integrar os novos resumos/explicações e ajustar textos do frontend. Preservadas decisões RF17 de experiência privada, pacote RF16 e produtores não suportados RF18; teste de carga e medição percentual de cobertura ficam separados. Sem staging/commit/push nesta tarefa.

## 48. Próximo passo

Entregar relatório e evidências para revisão humana/checkpoint controlado pelo usuário. Inspeção final: git status --short, git diff --stat, git diff --check e git diff --name-status -- backend concluídos; **git diff --check exit 0, stdout vazio**, índice vazio e HEAD 4ce8ddb3371fcd817d217e520be22e966926312b preservado. Git emitiu apenas avisos de conversão LF/CRLF, sem erro de whitespace. Os três Java novos aparecem no status como untracked e no escopo/evidência Semgrep, além dos dez Java existentes modificados. Nenhum staging, commit ou push; históricos preservados.

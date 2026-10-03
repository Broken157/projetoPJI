# RF17 — Busca e gestão do Banco de Talentos — 03/10/2026

**Estado: RF17 backend PARCIAL, com busca/gestão implementada e subregra de experiência do menor pendente de decisão funcional.** Menores autorizados aparecem, mas experiência privada não é usada para restringi-los nem é projetada. A integração de chat baseada somente em vínculo RF13 permanece pendência RF35; convite pertence ao RF42. Validações finais: focada 467/467; Maven completo 1132 total / 1115 passed / 0 failures / 0 errors / 17 skipped, BUILD SUCCESS; Semgrep p/java 0 findings/erros, exit 0; Graphify update exit 0.

Checkout: C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline.
Branch: integracao-recuperada-2026-09-15. HEAD inicial: 2a98aa17739efe4af3381a888c664ff8da584a03.
Evidências: evidencias/rf17-2026-10-03/.

## 1. Objetivo

Pesquisar e ordenar somente ARTISTAS já participantes do Banco específico do CONTRATANTE autenticado. Adequar o endpoint existente, sem participação automática, saída, notificação RF13, convite RF42, novo chat, alteração de schema ou frontend.

## 2. RF/RNFs consultados

Fonte oficial atual: C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt. RF17 integral, RF03, RF08, RF10, RF11, RF13, RF19, RF27, RF35, RF36, RF37, RF42, RF44; RNF02, RNF07, RNF08, RNF10, RNF13; RNF06/RNF09 para privacidade e logs.

Lidos integralmente os relatórios RF13 participação, RF37 descoberta pública, RF45 candidatos, RF06 recandidatura e sincronização database04. Resultados antigos, bloqueios históricos e interpretações antigas não foram apresentados como validação deste checkout. Skill pji-backend-audit aplicada para rastreabilidade; prevaleceram o escopo e os comandos autorizados pelo usuário.

RF08/RF13/RF37 mantêm disponibilidade=false na descoberta pública. RF13 exige completude para entrar, sem condição permanente expressa de completude para consultar um membro histórico no RF17. RF10/RF27 exigem experiência privada para menor; não encontrada autorização inequívoca para o contratante filtrar por esse dado privado.

## 3. Estado inicial

Backend e relatório RF13 limpos contra HEAD. Working tree já sujo em README, frontend, SQL histórico e arquivos não rastreados. Nenhum reset, clean, stash, restore ou mistura desses trabalhos. Manifesto inicial: 1.096 arquivos; suplemento protegido inclui 917 arquivos (770 frontend, 140 database e 7 scripts), incluindo arquivos ignorados e .env por hash, sem copiar valores.

Baseline documental RF13: 1046 total / 1029 passed / 0 failures / 0 errors / 17 skipped. Não é uma nova execução RF17.

## 4. Checkpoint Git

Confirmado HEAD 2a98aa1 — feat: avanca RF13 com participacao no banco de talentos. Commit contém exatamente os oito arquivos esperados: SecurityConfig, cinco arquivos novos de participação, teste RF13 e relatório RF13. git status desses paths estava vazio, assim como o status de backend antes das edições RF17.

A condição de checkpoint exigida no prompt foi satisfeita antes de implementar. Sem git add/commit/push nesta tarefa; pendência de notificação RF13 não foi incorporada ao RF17.

## 5. Auditoria Graphify

Graphify MCP streamable HTTP utilizado antes das edições de produção. O cliente MCP do catálogo inicialmente falhou no initialize e não expôs ferramentas; a chamada ao mesmo endpoint já configurado http://127.0.0.1:8765/mcp funcionou por HTTP: initialize, tools/list, tools/call. Servidor Graphify 1.29.0. Nenhuma configuração, launcher ou política do Windows alterada.

Estado inicial: **5787 nós / 19214 arestas / 337 comunidades; 91% EXTRACTED / 9% INFERRED / 0% AMBIGUOUS**. Queries amplas tiveram truncamento explícito; get_neighbors/get_node menores confirmaram service/repository, participação RF13, modelos e taxonomia, perfil/completude, política do menor, Salvos, Chat, Vaga e SecurityConfig. Ambiguidades de nomes foram resolvidas por node ID. Grafo orienta navegação; fontes atuais, testes e SQL sustentam as conclusões.

Serena get_symbols_overview falhou por language server não inicializado. Não foi reparado; auditoria continuou pelas fontes e Graphify. Evidências: graphify-auditoria-inicial.json, graphify-neighbors-inicial.txt e graphify-componentes-iniciais.txt.

## 6. Auditoria database04

Única fonte ativa: database04/palco-database. Tabela banco_talentos com PK (contratante_id, artista_id), FKs, CHECK IDs diferentes e data_adicao; perfis_artistas com cidade/estado, disponibilidade e ultima_atualizacao; taxonomia oficial normalizada e experiência por área; itens_salvos independente. Enums e sete áreas oficiais preservados.

Conferência direta inicial/final do ZIP oficial: **46/46 arquivos oficiais idênticos nas duas conferências**, 0 diferenças e 0 arquivos adicionais; hashes individuais em database04-inicial.json e database04-final.json. ZIP SHA-256 52b1c4af06d79a7efae47e6fa320b4a0a32359efaae1d40e2a73d06129c6df2b; fingerprint oficial db8f05cabadfd3935b7c703b7b21252f170fa529c0d30e7e61755b46d7fd5f39.

Funções/procedures legadas do Banco não são chamadas pelo caminho Java RF17. O defeito histórico de tipo retornado por fn_listar_banco_talentos não foi corrigido nem reexecutado contra o banco de desenvolvimento.

## 7. TalentoController/Service legado

GET /api/talentos, seus catálogos e contexto de vaga já existiam. Service resolve ator pelo SecurityContext, exige CONTRATANTE/ATIVA e protege ownership de vaga/contexto. Repository já usava NamedParameterJdbcTemplate, filtros PostgreSQL, SELECT limitado, count e taxonomia em três consultas por página.

DashboardService consome recomendarDoContratante; esse caminho também deve respeitar o Banco próprio, conforme RF11/RNF13. Não foi desenvolvido dashboard completo. Catálogo /api/talentos/especializacoes é compartilhado publicamente com RF03 e permaneceu público, sem transformar o catálogo em acesso à gestão privada.

## 8. Busca global antiga

Confirmada nas fontes: a query anterior não consultava banco_talentos, exigia perfil_completo=true e excluía todos os menores. Poderia mostrar artista público não participante, e as recomendações de dashboard reutilizavam esse universo global.

Essas premissas foram removidas explicitamente. Regressões antigas receberam vínculos DML de fixture e novas expectativas de completude histórica/menor/whitelist; verificações de filtros, ordenação, proteção e N+1 conservadas.

## 9. Vínculo RF13 reutilizado

EXISTS em banco_talentos é a autoridade de participação. Mesma linha oficial criada pelo POST RF13, sem relação alternativa. Teste percorre lista vazia → POST confirmado RF13 → membro no RF17 e compara a relação depois de consultar.

Completude é guard de entrada RF13 e não condição permanente da busca. Disponibilidade=false ou flag incompleta não apaga nem esconde automaticamente membro ATIVA autorizado. Conta operacionalmente não apta fica oculta, sem excluir relação histórica.

## 10. Arquivos de produção

Seis arquivos existentes:

| Arquivo em backend/src/main/java/com/portifolio | Mudança |
|---|---|
| controller/TalentoController.java | Parâmetro aditivo q para nome público |
| dto/FiltroTalentos.java | q, IDs positivos, limite de offset e rejeição de raio; construtores internos anteriores conservados |
| dto/TalentoResponse.java | username/salvo, experiência de menor ausente, sem raio/contadores no JSON |
| dto/DashboardTalentoResponse.java | Ocultar contadores internos na projeção que reutiliza a busca profissional |
| repository/TalentoRepository.java | Membership/dono em count e página; aptidão/consentimento; nome literal; Salvo EXISTS; experiência mínima só para adultos |
| service/TalentoService.java | Passar dono resolvido ao repository e mapear whitelist aditiva |

Não alterados SecurityConfig/checkpoint RF13, MenorAutorizadoPolicy, RF06, RF37, chat, persistência Salvos, notificações ou modelo SQL.

## 11. Arquivos de teste

Novo: backend/src/test/java/com/portifolio/controller/BancoTalentosBuscaRf17IntegrationTest.java — **86 casos**.

Adaptados: TalentoRf13IntegrationTest.java e DashboardRf11IntegrationTest.java, no mesmo diretório. Nome histórico RF13 da primeira classe foi preservado. Fixtures passam a conter vínculos reais; retirada de assertions sobre contadores é substituída por assertions de ausência e as ordens por IDs permanecem verificadas. Nenhum skip novo ou alteração de condições de skip.

## 12. Endpoint final

**GET /api/talentos**, JWT CONTRATANTE apto. 200 para página, inclusive vazia. Sem novo endpoint paralelo de busca.

Exemplo: GET /api/talentos?q=João&areaId=1&funcaoIds=10,11&cidade=Recife&estado=PE&disponivel=false&ordenacao=RELEVANCIA&page=0&size=20.

Envelope anterior preservado: content, page, size, totalElements, hasMore, contexto. artistaId é o usuarioId já usado pelo RF10/RF35; username é público, conservado sem normalizar case nem criar segundo identificador.

## 13. Parâmetros finais

| Parâmetro | Contrato |
|---|---|
| q | Nome público/nome artístico em usuarios.nome; trecho literal, trim, até 150 caracteres, sem controles internos |
| areaId | Uma área oficial positiva |
| funcaoIds / especializacaoIds | Conjuntos oficiais; OR no mesmo conjunto; máximo 50 por conjunto |
| cidade / estado | Igualdade de cidade sem distinção de case; UF normalizada com duas letras, conforme contrato vigente |
| experienciaMinima | Enum oficial; mínimo por área do adulto; SEM_EXPERIENCIA significa ausência de mínimo, inclusive NULL legado |
| disponivel | Boolean opcional; false é filtro válido; ausência inclui true/false/NULL |
| ordenacao | RELEVANCIA (default) ou ATUALIZACAO, allowlist |
| page / size | Page >=0, default 0; size 1–50, default 20; offset limitado a Integer.MAX_VALUE |
| localizacao | Compatibilidade textual literal de cidade/UF, até 150 caracteres; formatos anteriores conservados |
| tipos | Filtro opcional legado por tipos oficiais de perfil artístico |
| vagaId / recomendados | Contexto próprio legado para compatibilidade; sem convite, sem candidatura |
| raios | Opções não vazias rejeitadas com 400; não integra RF17 revisado |

IDs extras de ownership nunca são consumidos como autoridade. Parâmetros extras legados permanecem sem efeito; testes adulteram dono/usuarioId/contratanteId/body sem obter Banco alheio.

Frontend atual ainda envia localização no campo principal de busca, oferece raio e busca estado Salvo por card. Integração futura deve usar q para nome, retirar raio e consumir salvo do card. Nenhum arquivo da UI foi alterado.

## 14. Autenticação

JwtAuthFilter valida assinatura, expiração e correspondência subject/ID persistido; UserDetailsServiceImpl reaplica acesso normal atual. Ausência/token inválido/conta atualmente não apta: 401. Não foi alterada a política global de autenticação.

## 15. Autorização

SecurityConfig restringe gestão ao papel CONTRATANTE; TalentoService também verifica papel e status ATIVA persistidos. ARTISTA/ADMIN/MODERADOR: 403. Token emitido antes de mudar papel/estado não prevalece. Catálogo compartilhado público de especializações não retorna artistas nem membership.

## 16. Ownership do Banco

AuthenticatedUserResolver.usuarioAtual → contratante() → dono persistido → buscarDoContratante → repository.buscar(dono,...). Nenhum parâmetro do cliente define dono. Chamadas internas recomendarDoContratante com outro dono são rejeitadas.

Vaga/contexto alheio continua 404 genérico conforme contrato legado; seu ID apenas identifica referência, sem conceder acesso ao Banco. Path RF13 de participação continua exclusivo ARTISTA.

## 17. Regra de membro RF13

Count e SELECT paginado compartilham o WHERE com EXISTS b.contratante_id=:dono AND b.artista_id=p.usuario_id. Dados profissionais em lote usam exclusivamente IDs dessa página autorizada, jamais o universo global.

Artista público sem vínculo ou apenas salvo não entra; membro não salvo entra. Mesmo artista pode pertencer a mais de um Banco, sem promover relação global. Busca não faz INSERT/UPDATE/DELETE em banco_talentos.

## 18. Busca por nome

Nome artístico não possui coluna separada no schema atual: usuarios.nome é nomeExibicao público RF10 do ARTISTA. q pesquisa somente esse campo; username serve de identificação, sem adicionar pesquisa privada. LOWER preserva a convenção de case/acentos: João/JOÃO coincidem, Joao pode não coincidir.

POSITION trata %, _, barras e payload SQL como texto literal sem curinga. Valores bindados; não é necessário introduzir LIKE/escape ou extensão unaccent. E-mail, telefone, CPF, responsável, endereço e tokens não são pesquisáveis.

## 19. Taxonomia

Catálogo oficial Área → Função → Especialização reutilizado. IDs inexistentes/incompatíveis e especialização sem função apropriada são rejeitados conforme handler vigente (422 para regra semântica; formato/ID não positivo 400). OR dentro das opções e AND entre categorias.

EXISTS evita multiplicação de artistas; não há peso extra para principal. Especializações sem uma função compatível do próprio artista naquela área não satisfazem filtro/matching nem entram na projeção profissional como compatíveis.

## 20. Área

areaId restringe por perfil_artista_area, principal ou secundária. Contexto de vaga fornece sua área; área explícita diferente da referência mantém 422 legado. Sem área/contexto não exige área artificial para listar membros.

## 21. Função

Funções pertencem à área selecionada. Qualquer opção selecionada satisfaz o filtro do conjunto; categorias distintas continuam AND. IDs são valores bindados; não criadas strings livres.

## 22. Especialização

Exige função selecionada e compatibilidade no catálogo oficial. Query verifica também associação compatível do artista, sem confiar apenas em dados legados possivelmente inconsistentes. Catálogo compartilhado e regressões RF03 preservados.

## 23. Cidade/UF

Usados campos estruturados perfis_artistas.cidade/estado. Cidade com igualdade sem case; UF normalizada segundo padrão atual de duas letras. localizacao textual anterior é alternativa literal do mesmo par; não usa endereço completo, coordenadas ou raio.

## 24. Experiência

Enum database04: SEM_EXPERIENCIA < INICIANTE < INTERMEDIARIO < EXPERIENTE < ESPECIALISTA. Mínimo ordinal vigente reutilizado, exclusivamente na área selecionada. Experiência de outra área não satisfaz o filtro. Mínimo diferente de SEM_EXPERIENCIA exige área/contexto; sem área 422.

Adulto pode ter nivelExperiencia na área projetada. SEM_EXPERIENCIA conserva a convenção anterior de não restringir mínimo, inclusive para nível NULL. Filtro ocorre no PostgreSQL antes da paginação.

## 25. Disponibilidade

Ausência não exclui membro false ou NULL. Filtro explicitamente true/false aplica igualdade, sem converter NULL em false. Mudança de disponibilidade não apaga vínculo e mantém descoberta RF37/detalhe RF10. A gestão não altera disponibilidade.

## 26. Menor autorizado

ARTISTA 14–17, ATIVA, com responsável normalizado, data de consentimento e sem revogação aparece se participante. Datas limite usam Clock da aplicação e hoje.minusYears, alinhadas a MenorAutorizadoPolicy.publicavel. Menor sem autorização/pendente/revogado/abaixo de 14 fica oculto antes de count/página.

Sem nova autorização, aviso RF44, alteração de política compartilhada ou exclusão do histórico. Não expostos responsável, consentimento, nascimento, idade ou flags internas.

## 27. Privacidade da experiência do menor

**SUBREGRA PENDENTE DE DECISÃO FUNCIONAL.** Ausente autorização inequívoca nos RF17/RF27/RF10 para filtrar por experiência privada do menor. Portanto mínimo não restringe menores autorizados; eles continuam submetidos aos demais filtros públicos e ao membership. Experiência não é parâmetro de ordenação.

SQL retorna NULL para o campo de experiência do menor e a propriedade nivelExperiencia é omitida do JSON. Teste altera o dado privado entre todos os níveis e NULL; para todos os mínimos, resposta permanece equivalente. Isso comprova a guarda de não inferência adotada, não cumprimento de um filtro privado de experiência.

Não foi inventado consentimento, interpretação de autorização ou alteração de banco. Por essa subregra, não se declara RF17 integral concluído.

## 28. Projeção/DTO

Whitelist: artistaId, username, nomeExibicao, avatarUrl, biografia curta (até 400), localizacao/cidade/estado, urlPortfolio, tipoPerfilArtistico, disponibilidade, áreas/funções/especializações permitidas, salvo e ultimaAtualizacao. Experiência somente de adulto. Identificadores suficientes às ações RF10/RF35/RF42, sem perfil inteiro ou sala automática.

Sem Entity JPA, documentos, contato privado, endereço completo, autodeclarações, senha/hash/token, nascimento, responsável, consentimento, idade, raio, score/percentual/medalha/posição ou contadores de compatibilidade no JSON. Os contadores internos existentes só apoiam ordenação e mapeamento compartilhado; JsonIgnore impede sua projeção, inclusive na sugestão que reutiliza talentos.

## 29. Salvar/Salvo RF19

EXISTS itens_salvos do mesmo dono JWT, tipo PERFIL_ARTISTA e alvo=usuarioId. Não confunde Salvos com membership e não faz query por card. Estado de outro contratante não vaza.

Endpoint RF19 existente reutilizado sem alteração: teste salva/desalva, lê novo estado RF17 e confirma relação RF13 igual. Salvo não participa de ORDER BY.

## 30. Ver Perfil RF10

artistaId e username correspondem aos identificadores existentes. Testes abrem GET /api/perfis/publicos/ARTISTA/{artistaId} para adulto/menor autorizado e verificam privacidade. Nenhum segundo detalhe/projeção pública foi criado.

## 31. Conversar RF35

artistaId alimenta POST /api/chat/salas com usuarioDestinoId. Listar não cria sala. Testado adulto listado → RF10 → ação explícita RF35 criada/reutilizável conforme serviço existente.

**Pendência RF35:** proteção atual de menor exige candidatura persistida com vaga do contratante. Apenas membership RF13 não habilita iniciar conversa; teste confirma 422 sem sala. Regra profissional não foi ampliada silenciosamente; contato com menor que já tem candidatura continua coberto na regressão ChatRf24/RF45 existente. Sem novo chat, anexos, retenção ou anonimização.

## 32. Fronteira RF42

Sem implementação de convite, listagem nova de vagas próprias, candidatura, notificação CONVITE ou efeito automático. Contextos de vagas próprios já existentes são apenas referência profissional preservada. artistaId é suficiente para futura integração RF42, que deverá validar membership e vaga própria ABERTA.

## 33. Paginação

LIMIT/OFFSET aplicados no PostgreSQL antes de montar cards/taxonomia. Default 20, teto 50; inválidos 400. Testes: primeira/intermediária/última/vazia/além do total, limite, estabilidade, múltiplas áreas e ausência de duplicatas. Não carrega Banco inteiro para filtrar ou paginar em memória.

## 34. Count/distinct

count(distinct p.usuario_id), com exatamente o WHERE da página. Outer join somente perfis_artistas ↔ usuarios por PK; demais relações por EXISTS/subqueries. Taxonomia em lote vem depois dos IDs limitados. Cenário 53 artistas com múltiplas áreas/funções/especializações mantém total=53 e páginas sem repetição.

## 35. Compatibilidade

Somente entre membros aptos do próprio Banco. Área é condição inicial quando fornecida. RELEVANCIA: número de funções compatíveis DESC, depois especializações compatíveis DESC, depois perfis_artistas.ultima_atualizacao DESC NULLS LAST e usuarioId ASC.

As contagens usam filtros profissionais ou contexto próprio legado e correspondência taxonômica válida. Sem taxonomia de matching, contagens zero e ordem por atualização/ID. Nenhum score persistido ou retornado.

## 36. Explicabilidade RNF13

Regra lexicográfica: função precede especialização, depois atualização e ID. Principal/secundária têm a mesma elegibilidade; cidade/UF/experiência adulta/disponibilidade são filtros objetivos, não pesos ocultos.

Testes demonstram duas funções/uma especialização à frente de uma função/duas especializações; especialização desempata depois; empate estável; inserções de Salvos, candidaturas, mensagens, visualizações, medalhas/ranking e parâmetros de seguidores não mudam ordem. RF41 não possui relação operacional de seguidores neste escopo; não foi inventada tabela/fixture para declarar RF41 entregue.

## 37. Ordenação por atualização

Timestamp escolhido: perfis_artistas.ultima_atualizacao. Coluna oficial existente, mantida por PerfilArtistaService, PerfilCompletoService e alterações de conteúdo profissional/portfólio existentes. Sem falsa equivalência com banco_talentos.data_adicao, sem calcular timestamp novo para ordenar.

ATUALIZACAO: timestamp DESC NULLS LAST, usuarioId ASC. Teste torna data_adicao de outro artista posterior sem alterar resultado e verifica NULLS LAST. Escritas externas que não atualizarem a coluna podem produzir timestamp desatualizado; não foi alterada procedure/trigger para corrigir dívida oficial.

## 38. Desempate estável

Sempre usuarioId ASC após critérios profissionais/timestamp. Repetição da mesma página com dados estáveis produz os mesmos IDs/JSON. OFFSET não cria snapshot entre requisições concorrentes; alterações de perfil/vínculo entre páginas podem deslocar resultados.

## 39. N+1/query count

**5 consultas JDBC por página sem contexto:** count, página com Salvo EXISTS, áreas, funções, especializações. **6 com contexto**, incluindo lookup próprio da vaga. Avatar calculado em memória; nenhum lookup por card.

Novo teste mede páginas 1/50 com taxonomia múltipla, menores e Salvos: **JDBC 5/5 e JPA 2/2**, total 7/7 no cenário sem contexto. As consultas JPA correspondem à autenticação/resolução do ator. Regressão legada mantém JDBC 6/6 com contexto. Teto do dashboard conservado.

Não equivale a teste de carga ou plano em produção. POSITION/LOWER/OFFSET e subqueries podem custar com grande volume; nenhum índice novo.

## 40. Binding/injeção

NamedParameterJdbcTemplate vincula dono, datas, nome/localização/cidade/UF, IDs, enums, LIMIT/OFFSET. Fragmentos dinâmicos são constantes do servidor; ordenação limitada a enum. Nome não entra em SQL concatenado.

Spy confere SQL preparado sem payload malicioso; testes com %, _, barras e tentativa de injeção retornam apenas correspondências literais. IDs extras/body não definem dono.

## 41. Erros HTTP

200 página/vazia; 401 ausência/token inválido/conta bloqueada no filtro; 403 papel incorreto/guard interno de dono; 404 contexto individual alheio/inexistente; 400 conversão, paginação, offset, q/controle/tamanho, IDs não positivos, sort ou raio; 422 taxonomia inexistente/incompatível, UF/cidade conforme padrão vigente e mínimo sem área.

ApiExceptionHandler preservado; sem SQL/stacktrace no erro. Não foi substituído o padrão semântico 422 por 400 apenas para mudar convenção.

## 42. Banco alterado: NÃO

Nenhum arquivo SQL, init/seed, enum, PK/FK, constraint, índice, migration, procedure/function/trigger ou objeto de schema alterado. spring.jpa.hibernate.ddl-auto=validate mantido; testes PostgreSQL reais/Testcontainers usam pacote database04 original, sem H2 nem DDL de fixture.

DML/limpeza somente nos containers descartáveis de teste. Banco de desenvolvimento não recebeu operação. **Banco alterado: NÃO.** ZIP oficial novamente comparado pelo SHA-256 de cada arquivo: 46/46, 0 diferenças/adições. Fronteiras protegidas: 917 hashes iguais (140 arquivos de banco, 770 frontend e 7 scripts), sem ausências/adições. Manifesto de 1.096 arquivos iniciais encontrou somente seis arquivos Java de produção e duas regressões alterados; relatórios anteriores, checkpoint RF13, SQL histórico e README preservados. Nova suíte e relatório RF17, evidências e derivados Graphify são acréscimos próprios. Os 335 arquivos Java testados permaneceram iguais após Maven/Semgrep/Graphify. Evidências: fronteiras-final.json, delta-arquivos-iniciais.json, backend-testado-final.json e checkpoint-final.json.

## 43. Frontend alterado: NÃO

Somente leitura de OfficialTalentBank/TalentFilters, contrato API e navegação. Nenhum npm install/build/test/format. Maven copia recursos existentes para target backend, sem escrever nas origens. Working tree anterior preservado; **Frontend alterado: NÃO.** Os 770 arquivos dos dois diretórios de frontend, incluindo arquivos ignorados, conservaram seus hashes; nenhum arquivo adicionado/removido. Alterações anteriores continuam no working tree e não pertencem ao RF17. Sem npm/install/build/formatação. Leitura de OfficialTalentBank.jsx e TalentFilters.jsx apenas para verificar contrato.

Integração futura precisa retirar filtro de raio, adicionar q para nome, consumir salvo do card em lote e tratar limites de experiência do menor/chat/RF42. Não declarada homologação visual.

## 44. Notificação RF13 mantida pendente

RF13 continua PARCIAL exclusivamente porque database04 não possui tipo compatível para entrada no Banco e notificação RF36 correspondente. Esse bloqueio não pertence ao RF17. Enum não ampliado; não reutilizados CONVITE/CANDIDATURA/SALVO para disfarçar evento.

Consultar Banco não emite notificação. Fluxo de participação/concorrência/idempotência permanece como checkpointado, com regressão de 57 casos.

## 45. Testes novos

86 casos PostgreSQL: autenticação/papéis/estado atual, isolamento JWT/IDs/body/path, participantes de dois Bancos/públicos/salvos, entrada real RF13, nome público/literal/case/acentos/privacidade, taxonomia/IDs/OR/AND, cidade/UF, mínimo adulto, disponibilidade, histórico, menor/consentimento/revogação/experiência não inferível, whitelist, páginas/count, critérios de compatibilidade/atualização, Salvos, RF10/RF35, zero efeitos de busca, N+1 e validate/PK.

Os 73 critérios mínimos estão rastreados em criterios-aceitacao.md. O critério de experiência do menor valida a guarda conservadora; não é apresentado como filtro privado integralmente implementado. Ordenação usa estruturas reais, sem testes apagados/skips artificiais.

## 46. Regressões

17 classes focadas: novo RF17, TalentoRf13IntegrationTest, BancoTalentosParticipacaoRf13IntegrationTest, PerfilEdicaoRf08IntegrationTest, PerfilCompletoServiceTest, PerfilPublicoRf10IntegrationTest, SalvoRf19IntegrationTest, GuardianConsentRf27IntegrationTest, GuardianConsentServiceTest, DescobertaPublicaRf37IntegrationTest, ChatRf24IntegrationTest, GenericEndpointsSecurityIntegrationTest, JwtAuthenticationIntegrationTest, Database04BootstrapIntegrationTest, DashboardRf11IntegrationTest, VagaControllerRf03IntegrationTest e AreaArtisticaIntegrationTest.

MenorAutorizadoPolicy não alterada; full inclui RF45 e demais consumidores. RF03 incluído para catálogo compartilhado. RF11 incluído por consumo do mesmo caminho de talentos.

Diagnóstico inicial preservado: **467 total / 465 passed / 2 failures / 0 errors / 0 skipped**, 03:44 min, exit 1. Falhas de teste no dashboard: assertion de contador de vagas indevidamente alterada foi restaurada; fixture de N+1 foi vinculada ao Banco. Código de produção não mudou para esses ajustes e assertions de ordenação/N+1 permaneceram.

## 47. Bateria focada

**467 total / 467 passed / 0 failures / 0 errors / 0 skipped; 17 classes; BUILD SUCCESS; exit 0; duração 03:31 min; término 03/10/2026 16:06:41 -03:00.** Evidências: focados-final.log, focados-final.exit, focados-final-totais.json e focados-final-reports/. Nova suíte RF17: 86/86.

Comando no backend: .\mvnw.cmd '-Dtest=BancoTalentosBuscaRf17IntegrationTest,TalentoRf13IntegrationTest,BancoTalentosParticipacaoRf13IntegrationTest,PerfilEdicaoRf08IntegrationTest,PerfilCompletoServiceTest,PerfilPublicoRf10IntegrationTest,SalvoRf19IntegrationTest,GuardianConsentRf27IntegrationTest,GuardianConsentServiceTest,DescobertaPublicaRf37IntegrationTest,ChatRf24IntegrationTest,GenericEndpointsSecurityIntegrationTest,JwtAuthenticationIntegrationTest,Database04BootstrapIntegrationTest,DashboardRf11IntegrationTest,VagaControllerRf03IntegrationTest,AreaArtisticaIntegrationTest' '-Dspring.test.mockmvc.print=NONE' test.

## 48. Maven completo

**BUILD SUCCESS; exit 0; 63 classes; duração 07:42 min; término 03/10/2026 16:21:55 -03:00.** 1132 total / 1115 passed / 0 failures / 0 errors / 17 skipped. Evidências: maven-completo.log, maven-completo.exit, maven-completo-totais.json e maven-completo-reports/.

Comando .\mvnw.cmd test '-Dspring.test.mockmvc.print=NONE', sem clean/filtro. Ambiente carregado de scripts/database04/environment.ps1 e PALCO_TEST_DATABASE04_PATH para pacote oficial. JDK 21.0.11, Spring Boot 4.0.6, Hibernate 7.2.12.Final, Testcontainers 2.0.5, PostgreSQL 18.4 postgres:18-alpine, validate.

A execução necessitou acesso autorizado ao Docker/cache local pelo sandbox; isso não é contorno do Windows App Control nem alteração de ferramenta. Avisos preexistentes de Open-in-view/fetch de coleção em outros cenários não mudam a paginação SQL RF17; suas queries LIMIT/OFFSET e contagem foram verificadas diretamente.

## 49. Totais

| Rodada | Classes | Total | Passed | Failures | Errors | Skipped | Duração | Exit |
|---|---:|---:|---:|---:|---:|---:|---|---:|
| Focada final | 17 | 467 | 467 | 0 | 0 | 0 | 03:31 min | 0 |
| Maven completo | 63 | 1132 | 1115 | 0 | 0 | 17 | 07:42 min | 0 |

Somente XMLs posteriores ao início de cada rodada entram nas somas; cópias sem properties de runtime preservam casos, resultados e logs. A verificação de 103 arquivos XML/log novos não encontrou valores de runtime conhecidos nem tokens JWT assinados; nenhuma outra evidência histórica foi sanitizada nesta tarefa (privacidade-evidencias.json). Mesmos 17 skips condicionais: CurrentLocalSchemaIntegrationTest 4, OfficialLocalApiIntegrationTest 13. Nenhuma condição habilitada/alterada, nenhum novo skip; não contam como cobertura executada.

Baseline RF13 1046 + 86 casos RF17 = 1132 total esperado na regressão. Resultados históricos RF06/RF45/RF37 não reescritos nem substituídos.

## 50. Semgrep

**Semgrep 1.178.0 via Docker, p/java: 266 arquivos rastreados pelo Git, 60 regras Java, ~100.0% das linhas parseadas, 0 findings, 0 blocking, 0 erros, execução concluída com sucesso, exit 0; 219,998 s.** Todos os seis arquivos de produção modificados constam de paths.scanned. Evidências: semgrep-java.json, semgrep-java-saida.log, semgrep-java.exit, semgrep-java-resumo.json e semgrep-java-tempo.json.

Imagem oficial Docker fixada em digest sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b; cache p/java oficial baixado pelo Windows anteriormente com TLS verificado e SHA-256 5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1, copiado somente para evidência RF17. Checkout montado readonly; instalação Windows nativa não utilizada, TLS não desabilitado, App Control/Defender/Python intactos.

Semgrep do projeto utiliza Docker neste ambiente. Nenhum código do Palco alterado para corrigir ferramenta. Não realizado novo scan secrets nem alegada ausência absoluta de segredos.

**0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.**

## 51. Graphify final

**graphify update . concluído, exit 0; 26,259 s; 5858 nós / 19567 arestas / 347 comunidades**, contra 5787/19214/337 inicialmente. MCP HTTP graph_stats final confirma 5858/19567/347, 91% EXTRACTED / 9% INFERRED / 0% AMBIGUOUS. Foram extraídos 247 arquivos sem cache; 73 arquivos sem extensão/shebang suportado foram explicitamente ignorados pela ferramenta. Evidências: graphify-update.log, graphify-update.exit, graphify-update-resumo.json e graphify-mcp-final.txt. O CLI informou mudança das comunidades e adotou nomes por hub; os rótulos semânticos anteriores não foram renovados por LLM, conforme proibição de label.

graphify update . somente extração AST; sem graphify label/LLM nem graphify-mcp.exe. Derivados/cache/backups não são código de produção e não foram stageados. Consulta HTTP inicial e update final são evidências distintas; nenhum reparo de Windows/Python/configuração MCP.

## 52. Riscos

POSITION/LOWER/OFFSET e subqueries podem ter custo elevado em grandes Bancos; testes medem consultas, sem homologar p95/p99/carga RNF. Timestamp depende das escritas que o mantêm; procedimentos externos legados continuam sua dívida própria. Concorrência entre páginas/read statements pode mudar total/posição/estado, sem snapshot global.

Conteúdo público textual fornecido pelo usuário continua sujeito às políticas vigentes; esta tarefa não cria moderação textual nova. O frontend ainda usa contrato/controles legados e requer integração futura.

## 53. Limitações

Experiência privada de menor não é filtrada; o resultado do menor permanece independente do dado privado. Somente vínculo RF13 não inicia chat de menor no RF35 atual. RF42 não implementado. Integração visual, benchmark, metas de cobertura percentual e conformidade jurídica integral não homologados.

17 integrações locais condicionais seguem não executadas; seus contratos históricos não são substituto das regressões PostgreSQL descartáveis executadas. Nenhuma ambiguidade foi mascarada com skip.

## 54. Pendências

Decisão funcional explícita sobre experiência privada de menor no Banco, sem inferir autorização. Avaliar RF35 em tarefa própria para reconhecer RF13 como interação profissional válida, mantendo revalidação de consentimento/conta. Implementar RF42 separadamente e integrar frontend em tarefa autorizada.

Notificação/schema RF13 permanece sua pendência independente. Nenhuma mudança de banco necessária para a busca implementada; nenhuma proposta de alteração aplicada automaticamente.

## 55. Conclusão

**RF17 backend PARCIAL.** Busca do Banco próprio, identidade JWT, membership oficial, filtros públicos/profissionais permitidos, paginação PostgreSQL/count distinto, compatibilidade explicável/atualização real, Salvo em lote, menor autorizado e proteção de experiência implementados. Validação final: 467/467 focados; Maven 1132/1115/0/0/17, BUILD SUCCESS; Semgrep 266 arquivos/60 regras/0 findings/0 erros/exit 0; Graphify update exit 0.

A conclusão parcial refere-se à subregra de filtro privado de experiência do menor, não a falha de SQL/testes nem à notificação RF13. RF35/RF42 e integração visual têm suas fronteiras documentadas. Banco/frontend intactos. Sem staging, commit ou push.

## 56. Próximo passo recomendado

Revisar o delta RF17 e decidir a regra de experiência privada do menor antes de ampliar seu tratamento. Tratar reconhecimento de RF13 no chat e convite RF42 em tarefas próprias; integrar frontend apenas com autorização específica. Não usar staging global sobre alterações anteriores.

## Registro pronto para consolidação semanal

**Data:** 03/10/2026.
**Objetivo:** adequar busca/gestão RF17 ao Banco próprio do contratante.
**RF/RNF trabalhado:** RF17; dependências RF03/RF08/RF10/RF11/RF13/RF19/RF27/RF37; fronteiras RF35/RF36/RF42/RF44; RNF02/RNF06/RNF07/RNF08/RNF09/RNF10/RNF13.
**Backend alterado:** seis arquivos existentes de controller/service/repository/filtros/DTOs; nova suíte RF17 e duas regressões adaptadas.
**Frontend alterado:** NÃO. **Banco alterado:** NÃO.
**Funcionalidades concluídas/avançadas:** Banco próprio, membership, filtros profissionais permitidos, histórico, menores autorizados, paginação/count, ordenação explicável, estado Salvo em lote; RF17 parcial pela subregra de experiência privada.
**Bugs corrigidos:** universo global legado, exclusão geral de menores, condição permanente de completude, contadores internos/raio no card, ausência de nome e Salvo em lote.
**Decisões técnicas:** EXISTS do dono JWT, Clock/consentimento, mínimo somente adulto, JsonIgnore, POSITION literal, timestamp do perfil, empate ID, cinco queries JDBC sem contexto.
**Segurança/privacidade:** filtros e datas bindados, conta/papel persistidos, sem privados/experiência de menor, sem efeitos de consulta.
**Testes/resultados:** focada final 467/467, 03:31 min; completa 1132 total/1115 passed/0 failures/0 errors/17 skipped, 07:42 min, BUILD SUCCESS; 86 novos RF17 verdes; N+1 JDBC 5/5 e JPA 2/2 para 1/50 cards; Semgrep Docker 1.178.0, 266 arquivos, 60 regras, 0 findings/erros/exit 0; Graphify 5858/19567/347, exit 0; database04 46/46 e fronteiras por hash intactos.
**Pendências:** subregra de experiência do menor, fronteira RF35 de chat só com RF13, RF42/integração visual/carga.
**Próximos passos:** revisar delta específico, decidir privacidade e integrar RFs/frentes próprias.
**RF13:** continua PARCIAL exclusivamente pela notificação/schema, sem atribuir essa pendência ao RF17.

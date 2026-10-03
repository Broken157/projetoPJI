# Auditoria RF06/RF45 — Candidatura, recandidatura e candidatos da vaga

**Data:** 28/09/2026. **Modo:** somente leitura do produto; este relatório é o único arquivo criado. **Estado:** RF06 e RF45 ainda não atendem integralmente ao contrato revisado. Nenhuma implementação, teste, alteração de banco, commit ou push foi feita.

## 1. Resumo executivo

O backend já deriva o artista do usuário autenticado, exige ARTISTA/perfil completo/vaga ABERTA, registra a candidatura e sua notificação, preserva uma retirada lógica e confere a propriedade da vaga antes de listar candidatos. O cliente integrado já usa essas APIs. Faltam a candidatura simples sem mensagem/link, a única recandidatura com duas tentativas preservadas, a restrição de retirada ao estado ABERTA/PAUSADA, a lista operacional só com candidatos vigentes e o contato contextual autorizado. O fluxo formal `EM_ANALISE`/`ACEITA`/`REJEITADA` continua disponível na API e na interface.

Há dois impedimentos físicos independentes no candidato histórico de banco D1: `mensagem_apresentacao` e `link_portfolio_candidatura` são `NOT NULL` sem default; `candidatura_unica` proíbe uma segunda linha para o mesmo par vaga/artista, inclusive após `RETIRADA`. Remover validações Java, gravar texto fictício, sobrescrever ou apagar a primeira tentativa não satisfaz RF06. A versão remota oficial do banco **não foi confirmada**; D1 e B1 são evidência histórica, não homologação remota.

## 2. Estado atual e fontes

Fontes confrontadas: `rf e rnf.txt` revisado (RF06, RF08, RF35, RF36, RF44, RF45; RNF05/06/08/10/14/16/17); `AUDITORIA_DELTA_RFS_REVISADOS_2026-09-28.md`; `RELATORIO_VAGAS_RF04_RF07_RF23_RF28_2026-09-28.md`; `RELATORIO_RF04_TAXONOMIA_FRONTEND_2026-09-28.md`; `AUDITORIA_BANCO_POS_DECISOES.md`; `AUDITORIA_SNAPSHOT_BANCO_OFICIAL.md`; relatórios recentes de testes. Serena e Graphify orientaram a localização; as conclusões abaixo foram conferidas nos arquivos atuais. A auditoria delta é um retrato anterior às entregas posteriores de RF26/RF27/Vagas e não substitui o código atual.

O baseline **herdado**, documentado no relatório de Vagas e no relatório RF04 de frontend, é **779 total, 762 passed, 0 failures, 0 errors, 17 skipped condicionais, BUILD SUCCESS**. Não é resultado de uma nova execução nesta auditoria. O `ddl-auto=validate` está em `backend/src/main/resources/application.properties:16`. Os 17 testes condicionais não foram habilitados nem executados.

## 3. Fluxo revisado a implementar

1. `POST /api/candidaturas`: ARTISTA autenticado, conta apta, vaga existente/ABERTA e não vencida, perfil completo conforme RF08 revisado; confirmação explícita no cliente; corpo mínimo com `vagaId`. O servidor deriva o artista do JWT, cria tentativa `PENDENTE` e notifica conforme RF36; para menor, RF44 prevê aviso ao responsável após a ação.
2. Rejeitar candidatura ativa duplicada com 409. Após a primeira retirada, permitir **uma** nova tentativa com outro ID e timestamp. Depois de retirada da segunda, recusar a terceira com 409 e mensagem **“Limite de recandidatura atingido”**. Vaga fora de ABERTA ou perfil incompleto: 422; sem permissão: 403; inexistente: 404.
3. `DELETE /api/candidaturas/{id}` mantém a linha e marca `RETIRADA` somente para o próprio artista e enquanto a vaga estiver ABERTA ou PAUSADA. Retirada repetida ou de vaga ENCERRADA/CANCELADA é recusada. O histórico do artista preserva as tentativas.
4. `GET /api/vagas/{id}/candidaturas` apresenta ao dono uma página de **candidaturas vigentes**, uma por artista; retirada antiga não reaparece após recandidatura. Perfil e conversa são ações do RF45. Não há etapa operacional de análise, aceite ou rejeição formal. Valores legados do enum podem continuar armazenados para leitura histórica, sem novas transições por API.

## 4. Backend atual

| Camada | Evidência atual | Lacuna para RF06/RF45 |
|---|---|---|
| Entity/DTO | `Candidatura.java:20-52` mapeia uma linha por ID e declara `candidatura_unica`; `CandidaturaCriacaoRequest.java:13-23` exige mensagem/link; `CandidaturaRequest.java` exige eco de `vagaId`/`artistaId` e campos obrigatórios para o PUT. | Criação mínima é recusada antes de persistir; mapeamentos continuam incompatíveis com null real; PUT conserva superfície funcional antiga. |
| Controller | `CandidaturaController.java:29-64` expõe lista genérica, lista do contratante, detalhe, POST, PUT e DELETE; `VagaController.java:110-115` expõe lista paginada por vaga. | Remover/desativar transições formais do PUT mantendo leitura e retirada compatíveis; não apagar dados históricos. |
| Criação | `CandidaturaService.java:163-217` usa usuário autenticado, `findByIdForUpdate` da vaga, status ABERTA/prazo, `perfilCompleto` persistido, impede **qualquer** linha anterior via `existsByVagaIdAndArtistaUsuarioId`, grava mensagem/link, `PENDENTE` e timestamp; publica evento CANDIDATURA ao dono. | Trocar a regra “já existiu” por “há vigente?” e “quantas tentativas?”; coordenar criação/retirada na mesma disciplina transacional; fazer campos opcionais após a mudança física. |
| Retirada/formalização | `CandidaturaService.java:219-327` aceita PUT do contratante para `EM_ANALISE`/`ACEITA`/`REJEITADA`; PUT do artista e DELETE retiram `PENDENTE`/`EM_ANALISE`/`REJEITADA`, mas `retirar` **não consulta o estado da vaga**. | Retirada deve usar o estado ABERTA/PAUSADA; contratante não deve mudar status formal; notificação de retirada pode permanecer sem duplicação. |
| Lista | `CandidaturaService.java:69-148` confere proprietário da vaga e pagina 20 por padrão/máximo 50. `CandidaturaRepository.java:18-58` consulta **todos** os status e o `countQuery` também conta retiradas. Lista genérica e `/minhas-vagas` do contratante incluem histórico. | Filtrar vigentes no banco **antes** de paginar/contar, inclusive visão resumida/dashboard; preservar lista histórica própria do artista. |
| Dependências | `VagaService.java:284-307` usa `Optional<Candidatura> findByVagaIdAndArtistaUsuarioId` para detalhe/`minhaCandidaturaId`; `VagaService.java:445-447` muda **todas** as linhas, inclusive RETIRADA, para CANCELADA_POR_VAGA. Consultas de destinatários e de vagas canceladas em `CandidaturaRepository.java:27-42,62-78` não usam DISTINCT. | Duas linhas invalidam a consulta Optional; escolher tentativa vigente/mais recente explicitamente. Preservar retirada histórica no cancelamento e deduplicar IDs em listagem/notificação com paginação segura. |
| Notificações | Criação publica `NotificacaoEvento` ao dono; retirada persiste/notifica dono; PUT formal notifica artista (`CandidaturaService.java:207-215,273-291`). | Manter criação/retirada idempotentes, remover eventos de formalização; aviso RF44 para menor é dependência própria ainda não observada no fluxo de candidatura. |

`StatusCandidatura.java:5-12` e `database/01_types/01_enums.sql:4` ainda contêm `PENDENTE`, `EM_ANALISE`, `ACEITA`, `REJEITADA`, `RETIRADA` e `CANCELADA_POR_VAGA`. **Dados históricos podem permanecer**; a ação operacional de produzir novos estados formais deve sair da API e das telas. Não se propõe alterar enum nesta tarefa.

`PerfilCompletoService.java:56-78` ainda exige URL de portfólio e raio do artista; **não exige foto**. RF08 revisado dispensa os três, mas `CandidaturaService` exige `usuario.perfilCompleto=true`. Portanto, URL/raio antigos podem bloquear RF06 indiretamente. A coluna histórica `perfis_artistas.raio_atuacao NOT NULL` e a função de completude do dump agravam a divergência. Corrigir RF08 e homologar sua estrutura em escopo próprio; não enfraquecer RF06 nem inventar raio/portfólio aqui.

## 5. Frontend integrado e legado

O fluxo em uso passa por `palco-comunidades-agenda/src/App.jsx:79` e `src/pages/Vacancies.jsx:7-22` para `src/pages/vacancies/OfficialApplications.jsx`. O `ApplicationDialog` usa `textarea` e URL **required**, envia ambos (`OfficialApplications.jsx:12-32`). `CandidateList` busca página por vaga, oferece “Ver perfil”, mas mantém seletor/transições `EM_ANALISE`/`ACEITA`/`REJEITADA` e PUT formal; não há botão de conversa (`OfficialApplications.jsx:44-88`). A contagem na gestão usa `totalElements` da lista sem filtro (`OfficialVacancies.jsx:81`). O detalhe desativa “Candidatar-se” se existir **qualquer** `minhaCandidaturaId`, inclusive RETIRADA (`OfficialVacancies.jsx:61`); não há tratamento explícito da mensagem de limite. `Applications` mostra histórico/retirada, mas habilita retirada por status da candidatura sem consultar estado da vaga e conserva ações formais quando recebe `vagaId` (`Vacancies.jsx:16-23`). `Inbox.jsx:25` cria sala com apenas `usuarioDestinoId`.

O `frontend/src` legado também contém formulário obrigatório, painel de análise e testes do fluxo formal (`components/candidaturas/CandidaturaAction.jsx`, `CandidaturaReviewPanel.jsx`). Deve ser classificado e atualizado se continuar servido/usado; não tomar esse frontend legado como prova de comportamento do cliente integrado. Na implementação, deixar confirmação simples, erro 409 de limite, botão de recandidatura apenas quando elegível, histórico do artista e lista atual do dono; preservar o desenho existente.

## 6. Autorização, privacidade e chat

`SecurityConfig.java:93-94` exige autenticação para `/api/**` fora das rotas públicas explícitas. `CandidaturaService.criar` deriva o artista do JWT, não do corpo. `listarPorVaga` carrega a vaga e compara `vaga.contratante.usuarioId` com o autenticado; leitura por ID devolve 404 a artista alheio (`CandidaturaService.java:103-163,336-365`). Esses controles devem permanecer, inclusive em consultas novas. IDs no corpo servem apenas para localizar recurso, jamais para estabelecer autoria ou propriedade.

O contato RF45 ainda **não** verifica o vínculo contextual: `ChatController.java:32-40` recebe só `usuarioDestinoId`; `ChatService.java:58-68,215-234` confere papéis opostos e, para menor, apenas a existência de **qualquer** candidatura persistida entre a dupla, sem vaga, proprietário, status vigente ou consentimento naquele ponto. Para adultos, o ID isolado basta para criar/reabrir sala. Leitura e envio de mensagens exigem participação na sala, mas isso não corrige a autorização da criação.

Plano: a ação “Conversar” deve levar `vagaId` e `candidaturaId` ao servidor (em rota contextual ou contrato equivalente). O servidor deriva o contratante do JWT, carrega vaga própria e candidatura **vigente daquela vaga**, resolve o artista do registro e só então reutiliza/cria sala; nenhuma rota genérica pode contornar essa política para o início de conversa pelo contratante no RF45. Revalidar as regras de menor/conta ativa e projetar somente dados profissionais mínimos (`CandidaturaVagaResponse.java`), sem e-mail/CPF/contato privado. Outras origens legítimas de chat, se existirem, precisam de autorização própria; o ID do destinatário sozinho não é prova.

## 7. Banco candidato e conflitos físicos

No SQL versionado `database/02_tables/04_vagas.sql:66-76` e no dump histórico D1 `palco.manu.dump` (SHA-256 `687F6BB5C7B43DCB97AA67401A66DF57BE077BEB3813E3AEE172D36D6F2DAB5F`), `candidaturas` tem `id bigserial` PK, FKs não nulas `vaga_id` e `artista_id` com cascade, mensagem `text NOT NULL`, link `varchar(255) NOT NULL`, `status_candidatura_enum` com default PENDENTE, `data_candidatura` com default timestamp e `UNIQUE(vaga_id, artista_id)` incondicional; índices por artista e vaga. Status/timestamp aceitam null fisicamente, mas a criação Java os define. D1 contém 43 tabelas/277 colunas; B1 é uma cópia local com 43/278 por correção RF07 de vaga. Nenhuma tabela de histórico de tentativas foi identificada. `log_vagas_canceladas` registra cancelamento da vaga, não uma tentativa do artista.

| Conflito | RF/esperado | Físico atual e por que backend não basta | Menor necessidade, sem DDL aqui | Dados/testes | Prioridade |
|---|---|---|---|---|---|
| Mensagem/link | RF06: ausentes de verdade são válidos | Ambas as colunas são NOT NULL sem default; inserir null falha mesmo removendo `@NotBlank`. Strings vazias/links fictícios falseariam a ausência. | Admitir ausência real nos dois campos; alinhar Entity/DTO/procedure/API após homologação. | Preservar valores existentes; testar POST só com `vagaId`, null persistido e respostas. | Alta, bloqueante |
| Segunda tentativa | RF06: duas tentativas com histórico e só uma vigente | UNIQUE global rejeita nova linha para o mesmo par após RETIRADA; sobrescrever ou excluir a antiga perde ID/status/data. | Permitir segunda linha do par e manter garantia de no máximo uma vigente; contar no máximo duas sob serialização no backend. Não é necessária nova tabela para guardar as duas linhas. | Linhas existentes viram tentativa 1 sem reescrita; testar 1ª/2ª/3ª, concorrência, listagens e notificações. | Alta, bloqueante |

`database/04_procedures/sp_enviar_candidatura.sql:51-76` também rejeita qualquer par preexistente; a rotina é um caminho histórico separado, não chamada pelo POST Java atual. Precisa ser confrontada com o contrato aprovado se continuar exposta. O `ATIVO` usado ali para status da conta é inconsistente com o enum `ATIVA`, outro risco da rotina, sem correção neste escopo.

## 8. Representação mínima da recandidatura

| Opção | Preserva tentativas/status/tempo? | Custo e conclusão |
|---|---|---|
| **A — até duas linhas em `candidaturas` por par** | Sim: cada linha já tem PK, status e timestamp; a primeira RETIRADA permanece. | **Escolha mínima coerente.** Substituir a unicidade global por regra que permita outra linha sem permitir duas vigentes; serviço verifica total e vigente sob bloqueio transacional compartilhado por criação/retirada. A terceira tentativa é 409 com texto exato. |
| B — uma linha com contador/histórico auxiliar | Um contador sozinho não preserva IDs, status e datas; histórico auxiliar restauraria isso. | Exigiria outra estrutura e sincronização ou sobrescrita com perda de auditoria; maior que A. |
| C — reaproveitar estrutura existente | `notificacoes`, `log_vagas_canceladas` e `itens_salvos` têm outras finalidades e não representam a tentativa com PK/FK/status. | Não há substituto semântico identificado em D1/B1; rejeitada. |

“Vigente” deve significar linha ainda não retirada/cancelada; valores formais legados podem ser lidos como histórico ou como vínculo não retirado, **sem** novas transições. Filtrar antes da paginação, não apenas esconder cards no React. A vaga já é bloqueada no POST; retirada e qualquer escrita concorrente da mesma dupla devem seguir a mesma serialização, para não permitir duas vigentes nem uma terceira tentativa. A garantia física de uma vigente complementa a checagem do serviço. Se existirem escritores externos, alinhar também a rotina histórica antes da liberação.

## 9. Testes antigos: classificação

| Classe/caso | Classificação | Próxima asserção |
|---|---|---|
| `CandidaturaControllerRf06IntegrationTest`: autenticação, papel, vaga inexistente/não ABERTA, perfil incompleto, identidade adulterada, leitura própria/404, ownership da lista, retirada lógica e notificação | **MANTER** as proteções; **ATUALIZAR** fixtures/payload e estado da vaga quando necessário. | 401/403/404/422, JWT como ator, histórico e isolamento permanecem. |
| `candidaturaDuplicadaRetorna409` e `constraintRealTrataDuplicidadeMesmoSePreConsultaNaoEncontrar` | **ATUALIZAR**. | Duplicidade **vigente** 409; corrida independente da pré-consulta; não tratar qualquer linha RETIRADA como duplicata. |
| `validacaoDeTamanho...`/`aceitaLimitesExatos...` e testes do formulário obrigatório | **ATUALIZAR**. | Campos opcionais, mas, quando informados, limites/validação apropriados; criar só com vaga. |
| `retiradaPreservaCamposNotificaDonoUmaVezEImpedeRecandidatura` | **SUBSTITUIR** a expectativa final. | Primeira retirada preserva linha; segunda candidatura ganha novo ID; segunda retirada preserva ambas; terceira recebe 409 e texto exato, sem nova notificação. |
| `contratanteProprietarioRegistraAnaliseEResultado`, `analisePersisteNomeOficial...`, `candidaturaAprovadaNaoPodeSerRetirada`, `transicaoInvalida...`, `falhaDeNotificacaoDesfazAnalise...`, `retiradaViaPut...` | **SUBSTITUIR/ATUALIZAR** conforme retirada do PUT formal. | Contratante não faz transição formal; leitura de valor legado não o apaga; DELETE do artista é o caminho de retirada. |
| `VagaCancelamentoRf25IntegrationTest.todosOsStatusDeCandidaturaDevemVirarCanceladaPorVagaSemPerderHistorico` | **ATUALIZAR**. | Cancelar vaga não converte RETIRADA histórica em CANCELADA_POR_VAGA; preservar linhas/datas e evitar aviso duplicado ao mesmo artista. |
| `ChatRf24IntegrationTest.menorExigeCandidaturaPersistidaEntreADupla` | **ATUALIZAR**. | Candidatura antiga/retirada ou vaga alheia não bastam para contato RF45; contexto válido, consentimento e sala do par devem ser verificados. |
| `frontend/src/rf06-candidatura.test.js`, `rf06b-minhas-candidaturas.test.js`, `gabriel-integration.test.js` e testes `CandidaturaAction`/`CandidaturaReviewPanel`/`candidaturaService` | **ATUALIZAR/SUBSTITUIR** se o legado continuar usado. | Remover contrato de campos obrigatórios/aceite/rejeição e provar histórico e recandidatura. |
| Cliente integrado `OfficialApplications.jsx`/`OfficialVacancies.jsx` | **NOVO TESTE NECESSÁRIO**. | POST mínimo; 409 limite; botão só após retirada elegível; lista do dono exclui retirada, tem perfil/chat; nenhuma ação formal; erros de ownership. |
| Backend RF06/RF45 | **NOVO TESTE NECESSÁRIO**. | Retirar em ABERTA/PAUSADA e bloquear em ENCERRADA/CANCELADA; 2 tentativas e corrida; ordenação/contagem/paginação filtradas; `VagaService.buscarPorId` com duas linhas; notificação única; acesso IDOR; menor e RF44 sem vazar dados. |

`VagaPrazoRf23Rf06IntegrationTest` já cobre abertura/prazo/concorrência com encerramento: manter e expandir onde a segunda tentativa altere o resultado. Não executar os 17 testes condicionais para fabricar cobertura.

## 10. Mudanças possíveis sem banco

| ITEM | JÁ EXISTE | BACKEND PODE CORRIGIR SOZINHO? | FRONTEND PODE CORRIGIR SOZINHO? | DEPENDE DE BANCO? | RISCO | ORDEM |
|---|---|---|---|---|---|---|
| Ownership da lista/IDOR | Sim, JWT+dono em serviço | Sim, reforçar regressão | Não autoriza recursos | Não | Regressão de privacidade | 1 |
| Estado da vaga na retirada | Retirada sem esse guard | Sim, consulta e regra transacional | Só pode ocultar botão | Não | Retirada após encerramento | 2 |
| Remover aceite/rejeição formal | API, UI e testes ainda oferecem | Sim, encerrar mutação e preservar leitura histórica | Sim, retirar controles, mas API continuaria exposta | Não para ações; não apagar enum | Clientes antigos/chats de teste | 3 |
| Visualização atual do dono | Lista paginada sem filtro | Sim, filtrar RETIRADA/CANCELADA_POR_VAGA antes do count | Não garante segurança/paginação | Não para filtro atual; sim para duas tentativas | Contagens e retirada vazadas | 4 |
| Chat contextual | Sala por ID isolado | Sim, validar dono+vaga+candidatura antes de criar/reusar | Pode adicionar botão e contexto, sem autorizar | Não para vínculo atual | Contato indevido, sobretudo menor | 5 |
| Mensagem/link opcionais reais | DTO/Entity/UI exigem | Não: NOT NULL rejeita null | Não: payload mínimo falha na API/banco | **Sim** | Falso dado se usar string fictícia | 6, após banco |
| Recandidatura com histórico | Há linha/retirada, mas bloqueio global | Não: UNIQUE rejeita 2ª linha | Não: botão não vence UNIQUE | **Sim** | Perda de histórico/corrida se sobrescrever | 7, após banco |

## 11. Mudanças bloqueadas e critérios de liberação

**Bloqueado pelo candidato D1/B1:** criação sem ambos os campos e segunda linha do par. A implementação completa deve esperar identificação/homologação do schema oficial e aprovação da menor mudança de candidatura. A separação de RF06/RF45, retirada por estado, remoção do fluxo formal, filtro atual, ownership e chat contextual são alterações de aplicação que podem ser preparadas sem banco; nesta auditoria não foram executadas.

**Dependência RF08 fora deste pedido de banco:** completude atualmente exige URL/raio apesar do contrato revisado, e raio é fisicamente obrigatório no dump. Mesmo liberando a estrutura da candidatura, usuários sem esses dados podem seguir inelegíveis indevidamente. Tratar em frente própria, sem inventar valores. **Dependência RF44:** e-mail informativo ao responsável por candidatura de menor continua uma pendência de fluxo/entrega, sem necessidade estrutural comprovada aqui.

## 12. Texto pronto para o pedido mínimo à Manu — somente candidatura

> **RF06 e RF45 — estrutura de candidaturas no schema oficial.** Favor confirmar a versão oficial vigente frente ao candidato histórico `palco.manu.dump` (SHA-256 `687F6BB5C7B43DCB97AA67401A66DF57BE077BEB3813E3AEE172D36D6F2DAB5F`). Nele, `candidaturas` tem PK `id`, FKs obrigatórias para vaga/artista, status e data por linha; `mensagem_apresentacao` e `link_portfolio_candidatura` são `NOT NULL` sem default e a constraint `candidatura_unica` é global para `(vaga_id, artista_id)`. RF06 revisado permite candidatura apenas com vaga, sem mensagem/link, e **uma** recandidatura após retirada, preservando ambas as tentativas; RF45 exibe apenas a tentativa vigente ao dono. O backend não pode persistir null nesses campos nem a segunda linha sob as constraints atuais; sobrescrever/apagar a primeira viola o histórico. A necessidade estrutural mínima é admitir ausência real de mensagem/link e permitir até duas linhas históricas por par, mantendo garantia de no máximo uma vigente. O serviço contará e serializará as tentativas, bloqueará a terceira e filtrará a lista operacional; preservar linhas atuais, IDs, status e timestamps como primeira tentativa. Confirmar também se `sp_enviar_candidatura` permanece caminho suportado, para alinhá-la ao mesmo contrato. Nenhuma alteração de outros domínios integra este pedido.

Não há DDL, migration nem execução de banco neste relatório.

## 13. Ordem de implementação proposta

1. Confirmar versão do banco oficial e aprovar as duas necessidades estruturais de `candidaturas`; mapear linhas atuais como tentativa 1 sem apagá-las. Definir tratamento apenas dos status formais legados para leitura e filtro.
2. Em tarefa de aplicação, preservar ownership/JWT e ajustar retirada para ABERTA/PAUSADA, inclusive PUT legado ou sua desativação; bloquear criação/retirada fora de estado e eliminar a mutação formal de contratante.
3. Após sincronização do schema, alinhar Entity/DTO/POST a campos opcionais reais; contar tentativas e vigência com serialização e proteção concorrente; responder 409 com mensagem exata na terceira.
4. Corrigir consultas de `CandidaturaRepository`, `VagaService.buscarPorId`, cancelamento e notificações para duas linhas; separar histórico do artista e lista atual/dash do dono com paginação correta.
5. Autorizar chat por contexto de vaga+candidatura no servidor e fechar bypass da rota por ID isolado; então adaptar formulário, detalhe, lista, chat e mensagens no cliente integrado.
6. Atualizar/substituir testes legados e adicionar integração PostgreSQL/Testcontainers e contratos do frontend; executar focados e suíte normal. Tratar completude RF08 e aviso RF44 em escopos próprios antes de declarar RF06 integralmente pronto.

## 14. Riscos e limites da evidência

- O dump D1 é candidato histórico de 43 tabelas; B1 é cópia local alterada em vaga. Nenhuma inspeção nesta auditoria prova o schema remoto atual. O SQL versionado também é uma fonte de instalação, não a autoridade remota por si só.
- Duas linhas por par quebram `Optional<Candidatura>` e consultas sem DISTINCT; podem duplicar vaga em cursor, aviso de cancelamento e contagem se a mudança for só na constraint. Filtrar no cliente depois de paginar também entrega páginas/contagens incorretas.
- `VagaService.cancelar` hoje sobrescreve `RETIRADA`. Preservar linha sem preservar o status de retirada não mantém o histórico necessário para a regra de tentativas.
- FKs com `ON DELETE CASCADE` significam que uma exclusão física de vaga/artista pode eliminar histórico; RF22/retenção precisa considerar esse limite. Nenhuma política de retenção foi decidida aqui.
- A rota atual de chat aceita ID isolado; manter esse caminho como bypass torna ineficaz um novo botão contextual. Proteções de menores e sessão ativa devem ser verificadas no servidor.
- Nenhum teste/build foi executado neste modo de auditoria; o baseline 779/762/0/0/17 é documental e pode ter mudado com alterações locais posteriores. Os 17 skipped não demonstram cobertura.

## 15. Registro semanal — 28/09/2026

| Campo | Registro |
|---|---|
| Objetivo | Auditar RF06/RF45 e definir plano exato antes da implementação. |
| Diagnóstico | Autorização básica existe; candidatura simples e segunda tentativa bloqueadas por NOT NULL/UNIQUE; retirada/lista/chat/fluxo formal precisam ajuste. |
| Backend/frontend/testes | Somente leitura; nenhuma alteração. |
| Banco/SQL/dump/migrations | Somente leitura; nenhum DDL ou alteração. |
| Verificação | Código, requisitos, relatórios, SQL versionado e hash do dump histórico; suíte não reexecutada. Baseline herdado: 779 total, 762 passed, 0 failures, 0 errors, 17 skipped, BUILD SUCCESS. |
| Pendências | Versão oficial do schema, autorização da mudança mínima de candidatura, dependência RF08 e aviso RF44. |
| Próximo passo | Enviar à Manu apenas a seção 12 e, após confirmação estrutural, implementar na ordem da seção 13 com regressão completa. |

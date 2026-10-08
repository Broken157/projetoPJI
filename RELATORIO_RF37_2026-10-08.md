# RF37 — Descoberta e busca de perfis — 08/10/2026

## 1. Resumo executivo

Consolidação do contrato existente de descoberta pública para ARTISTAS e CONTRATANTES. Foram acrescentados filtros profissionais, subtipos públicos e consulta privada dos favoritos de ARTISTA, preservando o detalhe RF10, a publicabilidade e a paginação no banco.

**Estado funcional: backend principal entregue; RF37 PARCIAL por C01/C06.** Banco de Talentos ATIVO/DESATIVADO e favorito de CONTRATANTE não são representáveis com semântica confiável no database05. Pedidos dessas capacidades recebem erro explícito. Nome artístico próprio também não possui campo no modelo; essa limitação está registrada na seção 10.

A suíte focada final aprovou 243/243 testes. A regressão completa terminou com BUILD SUCCESS: 1647 total, 1630 passed, 0 failures, 0 errors, 17 skipped. Nenhuma alteração de banco, frontend, enum, migration, seed ou SQL oficial está autorizada ou foi feita.

## 2. Branch e HEAD inicial/final

Branch: `integracao-recuperada-2026-09-15`.

HEAD inicial: `0821cd81362ef474e1ba62ad651e96481f3fafa4` — `feat: consolida RF54 e RF08 no backend`. Ancestralidade do checkpoint confirmada; `git fetch fork` concluído, divergência inicial `0/0`.

HEAD final confirmado: `0821cd81362ef474e1ba62ad651e96481f3fafa4`, igual ao inicial. Index vazio e preservado byte a byte: SHA-256 `D3ABFA45F9A576A216E1FA3546E9A2B2BE3142050B11095A516B829CF6A7C45F`.

Backend inicialmente limpo e index inicialmente vazio. Sem staging, commit, push ou comando destrutivo. Evidências: `evidencias/rf37-2026-10-08/checkpoint-inicial.json` e `status-inicial.txt`.

## 3. Fontes utilizadas

| Fonte | Evidência/uso |
|---|---|
| RF/RNF consolidado em `C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt` | SHA-256 `08FB838914D57F1C6B54F3CFDC73DB5FE1F7CD511B4DFCA5A736DC094CA20EDA`, confirmado antes de editar |
| `database05/palco-database/` | Fonte estrutural exclusiva; pacote integral de 46 arquivos verificado pelo suporte PostgreSQL dos testes |
| `C:\Users\masca\Downloads\palco-database05.zip` | SHA-256 `6158C813929AC0DB9B3B12030E8B9D84C3B647611986DD6D60B2FC50D0F97EBF`, confirmado |
| `AUDITORIA_DATABASE05_BASELINE_RF_RNF_2026-10-08.md` | C01/C06 revalidados em schema/código; documento preservado |
| `RELATORIO_RF54_RF08_2026-10-08.md` | Contrato profissional do checkpoint, confirmado em fontes/testes; relatório preservado |
| Código e testes atuais | Decidem a conclusão; Graphify orientou a navegação |
| Graphify MCP | Consulta inicial a controller/service/repository/policy/taxonomia/favoritos; 6616 nós no grafo inicial |
| Context7 `/jakartaee/persistence` | Criteria, projeção e subqueries correlacionadas; adotado EXISTS em WHERE e leitura dos favoritos em lote |

Foram consultados RF08/RF10/RF13/RF19/RF27/RF29/RF47/RF48/RF54 e RNF02/05/06/07/08/10/13/17 no recorte necessário. Nenhum desses requisitos foi reimplementado integralmente. A baseline funcional permaneceu inalterada.

## 4. Estado inicial

A matriz anterior à primeira edição está em `evidencias/rf37-2026-10-08/auditoria-inicial.md`.

| Capacidade | Estado inicial | Encaminhamento |
|---|---|---|
| Listagem anônima dos dois papéis | JÁ EXISTE | Evoluir a mesma rota |
| Nome público/username, cidade, UF e Área | JÁ EXISTE/PARCIAL | Preservar semântica; validar IDs e cadeia |
| Função/Especialização/Subtipo | FALTA no RF37 | Implementar filtros singulares oficiais |
| Área principal/secundária | JÁ EXISTE no modelo | Considerar qualquer Área válida, sem deduplicação em memória |
| Favorito ARTISTA | RF19 representa; falta integração RF37 | JWT + EXISTS + estado em lote |
| Favorito CONTRATANTE/Banco ativo | BLOQUEADO C06/C01 | Erro documentado, sem simulação |
| Nome artístico próprio | Ausente no modelo | Registrar limitação |
| Menor/publicabilidade/DTO/paginação | JÁ EXISTE | Reusar policy, ampliar regressão |
| Recomendação/follow/afirmativas/frontend/banco | FORA DO ESCOPO | Preservar |

Preexistiam mudanças em README, `database/02_tables/04_vagas.sql`, 25 arquivos rastreados de `palco-comunidades-agenda/` e documentos/arquivos não rastreados. Foram registradas e preservadas; não pertencem a RF37.

## 5. Auditoria dos endpoints existentes

| Endpoint | Estado final |
|---|---|
| `GET /api/perfis/publicos` | Evoluído; busca pública com `tipo=ARTISTA\|CONTRATANTE` opcional e filtros abaixo |
| `GET /api/perfis/publicos/{tipo}/{id}` | Detalhe RF10 preservado, incluindo publicabilidade e privacidade |
| Endpoints profissionais RF08/RF54 | Contratos preservados; infraestrutura de validação reutilizada |
| Endpoints RF19 de salvar/remover/listar | Preservados; somente leitura mínima adicionada ao repository para RF37 |

Não foram criadas rotas duplicadas. Parâmetros aceitos: `q,tipo,cidade,estado,areaId,page,size,funcaoId,especializacaoId,tipoPerfil,tipoContratante,somenteFavoritos,bancoTalentosAtivo`. Qualquer chave fora dessa lista é rejeitada com 400. Os dois últimos filtros dependem do contexto/autenticação/capacidade descritos nas seções 14–16.

Mudanças observáveis: ID profissional inexistente ou cadeia incompatível retorna 422; formato/contexto inválido retorna 400. O DTO acrescenta campos públicos opcionais, conforme papel e autenticação.

## 6. Arquivos auditados

| Camada | Fontes principais |
|---|---|
| Controller/DTO | `PerfilPublicoController`, `FiltroDescobertaPublica`, `PerfilDescobertaResponse`, `ArtistaPublicoResponse`, `ContratantePublicoResponse` |
| Service/validação | `PerfilPublicoService`, `PerfilProfissionalService`, `TaxonomiaProfissional`, `PerfilCompletoService`, `SalvoService`, `AvatarService` |
| Repository | `PerfilDescobertaRepository`, `ItemSalvoRepository`, `AreaArtisticaRepository`, `FuncaoRepository`, `EspecializacaoRepository`, repositories dos dois perfis |
| Modelo | `Usuario`, `PerfilArtista`, `PerfilContratante`, `PerfilArtistaArea` e ID composto, `Funcao`, `Especializacao`, `ItemSalvo`, `ResponsavelLegal`, enums existentes |
| Segurança | `SecurityConfig`, JWT/resolução autenticada, `MenorAutorizadoPolicy`, exceções e tratamento global |
| PostgreSQL oficial | `01_types/01_enums.sql`, `02_tables/01_usuarios.sql`, `02_perfis.sql`, `03_tags.sql`, `09_moderacao_e_engajamento.sql`, taxonomia/perfis e suporte integral `OfficialPostgreSQLContainer` |
| Testes | Sete classes da suíte focada; `OfficialSchemaFixtures` e `OfficialPostgreSQLContainer` |
| Consumidores frontend, somente leitura | `palco-comunidades-agenda/src/lib/api.js`, `src/pages/Profile.jsx`, referências a perfis públicos |

Referências estruturais: `02_perfis.sql` representa tipo/localização e pares do Banco; `03_tags.sql` representa a taxonomia e seleções por Área; `09_moderacao_e_engajamento.sql` representa favoritos; enum de favoritos em `01_enums.sql`. Não se usou banco legado como fonte atual.

## 7. Arquivos alterados

Seis arquivos Java de produção, exclusivamente no fluxo RF37:

| Arquivo relativo ao checkout | Mudança |
|---|---|
| `backend/src/main/java/com/portifolio/controller/PerfilPublicoController.java` | Novos parâmetros na rota e allowlist |
| `backend/src/main/java/com/portifolio/dto/FiltroDescobertaPublica.java` | IDs/subtipos/favoritos/Banco e validação do contexto |
| `backend/src/main/java/com/portifolio/dto/PerfilDescobertaResponse.java` | Subtipo público e favorito próprio opcional |
| `backend/src/main/java/com/portifolio/repository/PerfilDescobertaRepository.java` | EXISTS profissionais/favoritos antes de count/page e projeção escalar |
| `backend/src/main/java/com/portifolio/repository/ItemSalvoRepository.java` | Uma consulta de leitura em lote por owner/tipo/IDs da página |
| `backend/src/main/java/com/portifolio/service/PerfilPublicoService.java` | Validação reutilizada, JWT, blockers e montagem do DTO |

Dois arquivos de testes: `backend/src/test/java/com/portifolio/controller/DescobertaFiltrosRf37IntegrationTest.java` (novo) e `DescobertaPublicaRf37IntegrationTest.java` (ajustado). Documentação/evidências: este relatório e `evidencias/rf37-2026-10-08/`. Graphify gera artefatos derivados em `graphify-out/`.

Os 16 fontes/testes específicos do checkpoint RF54/RF08 foram protegidos por hash; nenhuma alteração neles foi necessária.

## 8. Busca de ARTISTAS implementada

Listagem e busca pública sem autenticação, com `tipo=ARTISTA` ou contexto artístico derivado de filtro profissional. Critérios públicos: `q,areaId,funcaoId,especializacaoId,tipoPerfil,cidade,estado`. `somenteFavoritos=true` é privado e depende do JWT.

Exemplo: `GET /api/perfis/publicos?tipo=ARTISTA&areaId=2&funcaoId=3&especializacaoId=3&estado=SP&page=0&size=20`. Os IDs exemplificados existem no seed atual; a aplicação não os inventa.

Disponibilidade false e incompletude adulta não ocultam automaticamente perfil que atende à policy vigente. Não há filtro de experiência, raio, afirmação, follow, medalha ou score.

## 9. Busca de CONTRATANTES implementada

Listagem pública independente de vagas e membros do Banco, combinando `q,tipoContratante,cidade,estado`, com paginação e DTO público. Todos os quatro subtipos físicos foram testados.

Exemplo: `GET /api/perfis/publicos?tipo=CONTRATANTE&tipoContratante=ONG&cidade=Campinas&estado=SP`.

Sem parâmetros bloqueados, funciona normalmente. Banco ativo e favoritos de CONTRATANTE continuam explicitamente bloqueados; não são ignorados nem simulados. Filtros artísticos nesse contexto retornam 400.

## 10. Filtros textuais

`q` busca nome público OU username, com comparação case-insensitive. Prefixo `@` pesquisa somente username, mantendo a busca literal parcial existente. `q=@` isolado é inválido. A normalização faz strip/limites e mantém acentos; não se promete busca sem acentos.

ARTISTA usa `usuarios.nome` e username. CONTRATANTE mantém o nome público RF10: `nome_empresa` não vazio, com fallback a `usuarios.nome`. O nome pessoal oculto atrás do nome público empresarial não se torna um campo adicional de busca.

**NOME ARTÍSTICO: não há campo específico representável no modelo atual**. Não se usou biografia, integrantes, empresa, arquivo ou prefixo como substituto para o ARTISTA.

`q` aceita até 150 caracteres e cidade até 100; controles são rejeitados. `%\_\\` recebem escape LIKE e valores usam binding. Entradas de injeção são literais; testes verificam ausência de concatenação e preservação da tabela.

## 11. Filtros taxonômicos

| Combinação | Validação |
|---|---|
| Área | Deve existir |
| Função | Deve existir; infraestrutura `buscarTaxonomia` RF54 reutilizada |
| Especialização | Deve existir |
| Área + Função | A Função pertence à Área |
| Função + Especialização | Compatibilidade via `TaxonomiaProfissional.especializacoesCompativeis` |
| Área + Especialização | Relação oficial alcançável por Função da Área |
| Área + Função + Especialização | Toda a cadeia válida |

IDs não positivos/formato inválido retornam 400; IDs ausentes e incompatibilidades semânticas retornam 422. Filtros válidos sem perfis correspondentes retornam 200 com página vazia.

Na seleção dos perfis, um único EXISTS correlaciona a Área do ARTISTA, suas Funções selecionadas, Especializações selecionadas e compatibilidade oficial. Uma Especialização presente em outra Área ou sem Função compatível selecionada não gera correspondência. Essa proteção também foi exercitada com relações de teste fisicamente aceitas pelo schema, mas semanticamente incompatíveis.

## 12. Filtros de tipo/localização

ARTISTA: `ARTISTA_SOLO,DUPLA,BANDA,GRUPO_ARTISTICO,ESTUDIO,PRODUTORA_EMPRESA`.

CONTRATANTE: `PESSOA_FISICA,SETOR_PUBLICO,SETOR_PRIVADO,ONG`.

Enums físicos/Java existentes preservados. Binding tipado; aliases como `EMPRESA` não são aceitos silenciosamente. Subtipo implica o papel correspondente. Cidade pública profissional usa igualdade case-insensitive; estado usa duas letras normalizadas para maiúsculas. Endereço completo e raio não integram filtro ou retorno.

## 13. Múltiplas Áreas

Qualquer Área profissional válida participa da descoberta, principal ou secundária. O filtro não consulta apenas `areaPrincipalId` nem exige `principal=true`.

Função e Especialização precisam corresponder à mesma Área representada no EXISTS. Não há joins de coleções no SELECT paginado externo, nem DISTINCT para esconder duplicações. Testes cobrem duas Áreas, múltiplas Funções/Especializações, count único e páginas sem repetição/perda.

## 14. Favoritos ARTISTA

**ENTREGUE.** `somenteFavoritos=true` exige usuário autenticado. Owner vem exclusivamente de `AuthenticatedUserResolver`/JWT; não existe parâmetro para selecionar a coleção de terceiros.

O EXISTS usa `itens_salvos.usuario_id`, tipo físico `PERFIL_ARTISTA` e ID do ARTISTA antes de contagem e paginação. Estado `favorito` é lido em uma consulta em lote, limitada aos IDs de ARTISTAS da página e ao mesmo usuário.

Sem filtro ou com false, busca continua pública. Anônimo não recebe estado favorito; autenticado recebe booleano próprio apenas para ARTISTA. Favoritos true sem contexto artístico recebe 401 anônimo ou 400 autenticado, evitando retirar CONTRATANTES silenciosamente de uma busca mista.

A consulta não salva/remove favorito, não publica evento nem notifica dono. A notificação legada do ato de salvar em `SalvoService` permanece intacta e é assunto RF19 separado.

## 15. Favorito CONTRATANTE — estado

**BLOQUEADO POR C06 / ampliação RF19.** O enum físico de itens salvos é `PERFIL_ARTISTA,OBRA,VAGA`; não existe tipo correto de CONTRATANTE.

`tipo=CONTRATANTE&somenteFavoritos=true` retorna 401 sem autenticação e 422 C06 quando autenticado. A busca sem esse filtro funciona. O DTO de CONTRATANTE não apresenta booleano favorito falso como se a capacidade existisse. Nenhum CONTRATANTE é salvo/consultado como `PERFIL_ARTISTA`.

## 16. Banco de Talentos ativo — estado

**RF37 filtro Banco Ativo Sim/Não: BLOQUEADO POR C01 / próximo pacote oficial.**

`banco_talentos` representa CONTRATANTE ↔ ARTISTA/data de entrada/unicidade; não possui estado persistente independente ATIVO/DESATIVADO. `perfis_contratantes` também não fornece esse estado.

Sem parâmetro, busca funciona para bancos com zero ou vários membros. Com `bancoTalentosAtivo=true` ou false no contexto correto, retorna 422 C01 antes da consulta. Valor malformado ou combinação com filtros artísticos retorna 400. Não se inferiu estado por quantidade de membros.

Menor capacidade ausente: estado persistente próprio do Banco/CONTRATANTE, com semântica oficial de ativação/desativação. Nenhum desenho físico/SQL foi aplicado.

## 17. Privacidade

DTO explícito, escalar, sem entidade JPA/coleções. Não expõe CPF/CNPJ, e-mail/telefone privados, endereço completo, nascimento/idade, senha, googleId, tokens, responsável, consentimento, condição afirmativa, experiência, motivo interno de moderação ou score.

Dados pessoais não são alvos do q. Favorito próprio não revela owner da coleção ou favoritos de outro ator. Conta bloqueada não aparece como perfil normal. Não se criou opt-out geral de descoberta nem ampliou tratamento de dados RF48.

A asserção nova verifica nomes de campos JSON obrigatórios e subconjunto público permitido; não confunde cidade com idade. Teste de menor também verifica ausência dos valores privados conhecidos.

## 18. Menores

Reusada `MenorAutorizadoPolicy.publicavel`, comum à descoberta/count e ao detalhe RF10: conta ATIVA, papel correto e idade adulta; ARTISTA de 14 a 17 anos precisa de consentimento vigente do responsável, não revogado. Contratante menor e artista abaixo da idade mínima não se tornam publicáveis.

Menor autorizado continua aparecendo, inclusive com filtros profissionais e disponibilidade false. O card não contém idade, experiência, responsável ou condições privadas. Regressões de consentimento ausente/revogado, bloqueio e estados de conta permanecem; RF27/RF29 não foram alterados.

## 19. DTO público

Campos básicos preservados: `usuarioId,tipo,username,nomeExibicao,avatarUrl,cidade,estado`.

Campos opcionais acrescentados: `tipoPerfil` apenas ARTISTA, `tipoContratante` apenas CONTRATANTE e `favorito` apenas ARTISTA para ator autenticado. Campos opcionais nulos são omitidos via `JsonInclude.NON_NULL`. Não há serialização de taxonomia completa no card; filtros funcionam por EXISTS.

Metadados da página: `content,page,size,totalElements,totalPages,first,last,hasNext,hasPrevious`. Avatar reusa URL/fallback opaco RF10 sem persistir substitutos nem consultar por item.

## 20. Paginação

Default 20, máximo 50, `page>=0`, `size>=1` e offset dentro do limite inteiro. Erros retornam 400 sanitizado.

Count e SELECT recebem os mesmos critérios de publicabilidade/filtro. `setFirstResult/setMaxResults` limita os registros no PostgreSQL. Página vazia é 200; além do final mantém total correto e conteúdo vazio.

Testes percorrem 52 ARTISTAS em páginas 50+2 com múltiplas relações e favoritos, verificando total, IDs únicos e ausência de perda. CONTRATANTES também possuem regressão de count/páginas/conta bloqueada.

## 21. Ordenação

Mantida a ordenação pública existente: nome de exibição em minúsculas, crescente, com ID crescente como desempate. Nenhum score/relevância oculta foi introduzido.

Ordenação é fixa no backend. Todos os parâmetros `sort`, inclusive nome de campo aparentemente público, são rejeitados pela allowlist. Não é aceito nome arbitrário de coluna, direção ou trecho SQL.

Estabilidade foi verificada sobre dados estáveis. Offset não garante fotografia imutável entre requisições enquanto perfis são criados/renomeados; não se declara cursor/snapshot implementado.

## 22. Consultas/N+1

Projeção escalar a partir de Usuário e joins de perfis 1:1. Coleções profissionais e favoritos entram em EXISTS correlacionados; não há collection fetch no SELECT paginado externo, deduplicação posterior ou DISTINCT para corrigir contagem.

| Cenário verificado | Evidência |
|---|---|
| Área simples, página 1 versus 50 | 3 statements em ambos: existência no catálogo, count e página |
| Área/Função/Especialização, 52 perfis com múltiplas relações | Mesma quantidade para size 1/50; até 5 statements, incluindo validação; SQL contém limite físico |
| Favoritos privados e 52 perfis | Mesma quantidade para size 1/50; até 5 statements, incluindo autenticação e leitura em lote |
| C01 solicitado no contexto correto | Erro antes das queries de descoberta; zero SQL no cenário testado |
| C06 solicitado autenticado | Nenhuma leitura de itens_salvos como CONTRATANTE |

Inspeção SQL por `StatementInspector` nos testes PostgreSQL. Custo constante com o tamanho da página nesses cenários, sem consulta por perfil. Não se mediu p95/p99, teste de carga de 100 usuários ou homologação de 15 minutos; RNF05 global não é declarado concluído.

A regressão RF19 possui warnings preexistentes de collection fetch com paginação em memória. Eles pertencem ao fluxo legado de listagem de salvos, não ao SELECT RF37. Esse fluxo não foi ampliado/corrigido nesta tarefa.

## 23. Segurança/autorização

Binding Criteria/JPQL para valores de q/localização/IDs/owner; nomes de propriedades são constantes do backend. LIKE literal escapa curingas. Chaves fora do contrato, aliases e parâmetros privados são rejeitados.

Consulta pública não exige JWT. `somenteFavoritos=true` exige ator válido; identidade nunca vem de querystring. Usuário A não consulta favoritos de B. Erros 400/401/422 usam exceções e handler existentes, sem SQL/stack trace no retorno.

| RNF | Resultado no recorte RF37 |
|---|---|
| RNF02 | Binding, escape, allowlist e entradas maliciosas testadas |
| RNF05 | Projeção/EXISTS/lote e custo constante por página testados; carga global não medida |
| RNF06 | Minimização e privacidade por DTO/policy; sem alegar conclusão jurídica/global |
| RNF07 | Taxonomia oficial/cadeias válidas e ownership; nenhum patch de integridade no banco |
| RNF08 | Separação público/privado, JWT e conta publicável preservados |
| RNF10 | Testes focados e regressão completa conforme seções 32–34 |
| RNF13 | Busca permanece independente da recomendação RF47 |
| RNF17 | Página 20/50, limite físico, count e desempate estável |

Não há evento de notificação disparado por consulta RF37. Permissões RF19 de mutação e detalhe RF10 não foram flexibilizadas.

## 24. Compatibilidade RF08/RF54

Reusa modelo de múltiplas Áreas, uma principal e seleções de até cinco Funções/Especializações por Área. A descoberta não muda limites de escrita/completude do perfil.

`TaxonomiaProfissional.especializacoesCompativeis`, `FuncaoRepository.buscarTaxonomia` e `EspecializacaoRepository.contarDaArea` já existiam e foram apenas consumidos. Nenhuma regra paralela de edição profissional foi criada.

Suítes RF08/RF54/completude passaram na rodada focada. Os 16 fontes/testes específicos desse checkpoint e o relatório RF54/RF08 permanecem com os hashes iniciais.

## 25. Conflitos encontrados

| Fonte A | Fonte B | Impacto e comportamento adotado |
|---|---|---|
| Nova tarefa exige validação de IDs/cadeia | Antigo teste RF37 esperava 200 vazio para Área 99 | Alteração explícita para 422; teste ajustado, sem remover proteção |
| RF37 prevê filtro Banco ativo | database05 só representa pares de membros/data | 422 C01; quantidade de membros não determina estado |
| RF37 prevê favorito CONTRATANTE | Enum de salvos não possui esse alvo | 422 C06 autenticado; não mapear para ARTISTA |
| Nome artístico quando aplicável | Modelo atual não possui atributo próprio | Nome/username funcionam; aspecto artístico próprio ausente e não substituído |
| RF37 exige favoritos privados e consulta sem notificar | RF19 legado notifica dono no ato de salvar | RF37 somente lê; regra de mutação RF19 preservada e pendência separada |
| Teste antigo comparava integralmente anônimo/autenticado | Novo favorito próprio depende de autenticação | Comparação dos dados públicos preservada; estado privado verificado separadamente |

RF08/RF13/RF37 estão alinhados: disponibilidade Não não retira o perfil da descoberta pública. Não há novo conflito documental nesse ponto.

## 26. Blockers

| ID/aspecto | Confirmação | Menor capacidade/encaminhamento |
|---|---|---|
| C01 | Ausência de estado Banco ativo/inativo em Banco/perfil do CONTRATANTE | Pacote oficial com estado persistente independente e regras de ciclo de vida |
| C06 | Ausência de tipo/semântica confiável para favorito CONTRATANTE | Ampliação oficial RF19 com alvo correto e integridade/ownership, sem tipo falso |
| Nome artístico próprio | Ausência de campo específico no perfil/modelo atual | Decisão de modelagem funcional/oficial para esse aspecto; nenhuma mudança nesta tarefa |

C01/C06 impedem concluir integralmente RF37. A limitação de nome artístico está explicitada, sem alegar suporte por `usuarios.nome` a um atributo separado. Busca principal não fica condicionada à existência dessas novas capacidades.

## 27. Database alterado SIM/NÃO

**NÃO — delta da tarefa.** ZIP/snapshot oficiais preservados; testes inicializam o pacote integral `init.sql` com o `seed.sql` oficial, PostgreSQL 18 e `ddl-auto=validate`, sem patches ou fallback de snapshot.

Fingerprint agregado dos 46 arquivos validado pelo suporte existente: `c68460169fcd2538fefd34a109ee3ea7640e553ce1882dd225d88d655229c134`.

Comparação final SHA-256: **814/814 arquivos protegidos intactos**, incluindo database05, fontes frontend, README, alteração SQL legada preexistente, 16 fontes/testes do checkpoint RF54/RF08 e relatórios anteriores. ZIP/RF-RNF mantiveram os hashes iniciais. Os oito Java do delta mantiveram seus hashes após suíte focada/completa/Semgrep/Graphify. Evidência: `preservacao-inicial.json`, `preservacao-final.json`, `java-validado-manifesto.json`.

Alteração já existente em `database/02_tables/04_vagas.sql` permanece atribuída ao estado inicial. Testes escrevem somente fixtures em bancos descartáveis; não houve execução sobre banco de produção nem edição de schema/seed oficial.

## 28. Frontend alterado SIM/NÃO

**NÃO — delta da tarefa.** Frontend oficial, `frontend/` e `palco-comunidades-agenda/` não foram editados. Os 25 deltas rastreados preexistentes da aplicação operacional permanecem preservados.

A inspeção dos consumidores encontrou navegação ao detalhe RF10 em `Profile.jsx`; não comprovou tela integrada que envie os novos filtros da listagem RF37. A integração/UI desses filtros requer tarefa própria. Não foi executado build/teste frontend nem reivindicada entrega visual.

## 29. Migration/SQL SIM/NÃO

**NÃO.** Nenhuma migration, arquivo SQL, tabela/coluna/enum/constraint/índice/FK/function/procedure/trigger/seed foi criado ou alterado pela tarefa.

As queries parametrizadas Java e inserções de fixtures nos testes descartáveis não modificam o pacote estrutural oficial. `ddl-auto=validate` permanece. Não foi criado índice sem evidência nem proposta física aplicada para C01/C06.

## 30. Testes criados/alterados

| Classe | Mudança/cobertura |
|---|---|
| `DescobertaFiltrosRf37IntegrationTest` | Nova, 47 cenários: taxonomia válida/inválida, Área secundária, cadeia na mesma Área, seis subtipos ARTISTA, quatro CONTRATANTE, AND/OR, privacidade de menor, C01/C06, favoritos A/B/JWT, parâmetros malformados/privados, count e SQL constante nas páginas 1/50 |
| `DescobertaPublicaRf37IntegrationTest` | 71 cenários mantidos; whitelist/subtipos/favorito opcional, comparação pública anônimo/autenticado, Área inexistente 422, queries constantes com validação do catálogo e nome público RF10 |

Outras cinco classes são regressões sem edição: `PerfilPublicoRf10IntegrationTest`, `PerfilTaxonomiaRf54Rf08IntegrationTest`, `PerfilEdicaoRf08IntegrationTest`, `PerfilCompletoServiceTest`, `SalvoRf19IntegrationTest`.

A nova classe não adiciona Funções/Especializações ao catálogo oficial: usa as quatro de cada conjunto do seed e confirma a contagem. Relações/usuários/favoritos são dados de teste descartáveis. Não se desabilitou cenário crítico nem removeu validação para obter verde.

## 31. Comandos executados

Checkpoint: `git status --short`, `git branch --show-current`, `git rev-parse HEAD`, `git log --oneline -12`, `git fetch fork`, `git rev-list --left-right --count fork/integracao-recuperada-2026-09-15...HEAD` e verificação de ancestralidade. Integridade por `Get-FileHash -Algorithm SHA256`; leitura de fontes e matriz antes de editar.

Maven executado no diretório `backend`, usando JDK `C:/Program Files/Java/jdk-21.0.12`:

```powershell
.\mvnw.cmd '-Dtest=DescobertaPublicaRf37IntegrationTest,DescobertaFiltrosRf37IntegrationTest,PerfilPublicoRf10IntegrationTest,PerfilTaxonomiaRf54Rf08IntegrationTest,PerfilEdicaoRf08IntegrationTest,PerfilCompletoServiceTest,SalvoRf19IntegrationTest' test
.\mvnw.cmd test
```

Semgrep: `evidencias/rf37-2026-10-08/Executar-Semgrep.ps1` com imagem oficial fixa e montagens somente leitura, descritas na seção 35. Graphify MCP inicial/final e `C:/Users/masca/.local/bin/graphify.exe update .` AST, seção 36.

Verificações finais executadas: `git diff --name-status -- database05`, `git diff --name-status -- database`, `git diff --name-status -- frontend`, `git diff --cached --name-status`, `git diff --check`, `git status --short` e conferência adicional de backend/`palco-comunidades-agenda`/hashes. Database05/frontend/index sem delta; SQL legado e frontend operacional contêm somente deltas preexistentes preservados. **git diff --check: exit 0, sem saída/erros de whitespace.** Resultado em `preservacao-final.json`, `status-final.txt` e `git-diff-check.txt`.

Nenhum `git add`, commit, push, reset, clean, stash ou checkout destrutivo foi executado.

## 32. Testes focados

**BUILD SUCCESS — 243 total / 243 passed / 0 failures / 0 errors / 0 skipped**, sete classes, exit 0. Duração Maven final: **1min39s**, conclusão **08/10/2026 16:44:47 -03:00**.

| Classe | Total/passados |
|---|---:|
| DescobertaFiltrosRf37IntegrationTest | 47/47 |
| DescobertaPublicaRf37IntegrationTest | 71/71 |
| PerfilEdicaoRf08IntegrationTest | 28/28 |
| PerfilPublicoRf10IntegrationTest | 16/16 |
| PerfilTaxonomiaRf54Rf08IntegrationTest | 27/27 |
| SalvoRf19IntegrationTest | 25/25 |
| PerfilCompletoServiceTest | 29/29 |

As duas primeiras rodadas executaram 243 testes com cinco falhas da nova asserção de privacidade: primeiro substring idade encontrou cidade; depois a whitelist exigiu campos opcionais ausentes. A correção final exige os campos básicos e verifica subconjunto permitido de chaves JSON. Nenhuma dessas correções alterou produção, desabilitou teste ou removeu privacidade.

Evidência: logs `maven-focado-1.log`, `maven-focado-2.log`, `maven-focado-final.log`, exits, XMLs arquivados por rodada e `maven-focado-final-resumo.json`. Os XMLs finais foram selecionados pelo início da execução; não se somaram relatórios antigos.

## 33. mvn test

**BUILD SUCCESS**, exit 0; duração Maven **9min39s**, conclusão **08/10/2026 16:56:23 -03:00**. Foram conferidos 76 XMLs de classes desta execução: 1647 total, 1630 aprovados, zero falhas/erros e 17 skips opcionais. A soma coincide com o resumo Maven.

PostgreSQL 18.4/Testcontainers com inicialização integral oficial e validação JPA, incluindo as regressões RF01/RF02/RF06/RF08/RF10/RF13/RF17/RF19/RF22/RF23/RF24/RF26/RF27/RF35/RF36/RF44/RF45/RF54 e os testes de schema/segurança atuais. Nenhuma execução sobre banco de produção foi necessária.

XMLs arquivados em `evidencias/rf37-2026-10-08/completo-surefire/` e resumo em `maven-completo-resumo.json`. Seleção por LastWriteTimeUtc a partir do início da rodada; sem mistura com resultados focados/históricos.

Log: `evidencias/rf37-2026-10-08/maven-completo.log`; início/exits/resultados frescos mantidos junto à evidência. O delta Java foi congelado antes da rodada e tem manifesto SHA-256 em `java-validado-manifesto.json`.

## 34. Total/passed/failures/errors/skipped

| Execução | Total | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Focada final | 243 | 243 | 0 | 0 | 0 |
| Completa | 1647 | 1630 | 0 | 0 | 17 |

Os 17 skips são opcionais preexistentes e tiveram motivo confirmado nos XMLs: quatro em `CurrentLocalSchemaIntegrationTest` pela ausência de `palco.current-db-tests=true` e 13 em `OfficialLocalApiIntegrationTest` pela ausência de `palco.official-db-tests=true`. Nenhum teste RF37 foi desabilitado ou pulado. As integrações database05/Testcontainers executaram normalmente.

Resultados são desta tarefa/checkout; não se reutilizam totais históricos como execução nova.

## 35. Semgrep

**Sucesso, exit 0 — Semgrep 1.178.0, p/java, 373 arquivos Java, 60 regras, ~100.0% das linhas parseadas, 0 findings, 0 blocking, 0 erros.** Duração 35,6s.

Main e testes foram escaneados, incluindo a nova classe não rastreada. Cobertura conferida por `paths.scanned`: 8/8 arquivos Java do delta presentes, nenhum ausente. A execução ampla é complementar; não se limitou a depender do estado de staging.

Evidências: `semgrep-java.json`, `semgrep-java-saida.log`, `semgrep-java.exit`, `semgrep-java-execucao.json` e `semgrep-java-resumo.json` no diretório da tarefa. O script verifica hash de regras, cobertura, quantidade de regras e resultado.

Configuração prevista na evidência executável: Docker oficial `semgrep/semgrep@sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b`; regras `p/java` locais oficiais, SHA-256 `5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1`.

Fontes main/test montadas somente leitura, rede none, métricas/version-check desligados; inclusão de fontes não rastreados e ignore vazio montado apenas no container. Nenhum ignore do repositório foi alterado.

Não se usou Semgrep nativo Windows nem se desabilitou TLS. Nenhum contorno de Defender/App Control foi aplicado.

**0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.** Este scan p/java não é apresentado como auditoria completa de segredos/RFs.

## 36. Graphify

Inicial: MCP `query_graph` orientou a inspeção dos símbolos RF37, MenorAutorizadoPolicy, SalvoService e integração RF08/RF54. Grafo inicial com 6616 nós; a consulta encontrou 146 e exibiu 56 dentro do orçamento. Não se inferiu ausência de funcionalidade dos nós omitidos.

Evidência inicial em `graphify-mcp-inicial.json`. Código, schema e testes decidiram as conclusões.

**Atualização AST concluída, exit 0, 33,9s**, às 16:58:30 -03:00. Grafo final: **6655 nós, 22971 edges, 362 comunidades**; `graph.json`, `graph.html` e `GRAPH_REPORT.md` atualizados em `graphify-out/`. O Graphify também preservou backup de quatro arquivos do grafo curado.

MCP final `graph_stats` confirmou os números e `get_node` encontrou `DescobertaFiltrosRf37IntegrationTest` no novo arquivo, linha 36, grau 40. Respostas arquivadas em `graphify-mcp-final.json`; comando/log/exit/duração em `graphify-update.*` e `graphify-update-execucao.json`.

Limitações da ferramenta explicitadas: 45 arquivos SQL não contribuíram ao grafo pela ausência de `tree_sitter_sql`; 75 arquivos de extensões não suportadas foram omitidos. Nomes semânticos das comunidades não foram reetiquetados por LLM; somente a atualização AST solicitada foi executada. Nenhum desses avisos substitui a leitura direta dos SQLs/schema/testes usada nesta auditoria. Não se alegou atualização semântica de documentos/imagens.

Nenhuma instalação de Python/DLL/parser nem contorno de política Windows foi autorizado ou aplicado.

## 37. Riscos

C01/C06 e nome artístico próprio são limitações reais do modelo. Os filtros bloqueados precisam ser tratados pela futura UI; retornar 422 não significa que a capacidade foi entregue.

A API passou de 200 vazio para 422 para catálogo inexistente/incompatível. Consumidores devem diferenciar busca válida sem resultados de filtro inválido. Novos campos DTO são aditivos, mas clientes com validação rígida de schema precisam revisar a integração.

Custo SQL constante é evidência contra N+1 nos cenários testados, não benchmark de carga. Offset tem as limitações de concorrência descritas na seção 21. Catálogo completo RF54 permanece dependente dos dados oficiais; RF37 não inventa IDs.

A notificação legada do ato de salvar RF19 e seus warnings de paginação continuam fora desta tarefa. Não se declarou RF19 globalmente concluído nem alterou regras privadas para facilitar busca.

## 38. Pendências

C01: estado persistente independente do Banco. C06: favorito de CONTRATANTE com tipo/semântica confiável. Nome artístico próprio: atributo ausente e decisão de modelagem necessária se exigido separadamente.

Integração frontend dos novos filtros em tarefa própria. Carga de homologação para RNF05 global e catálogo integral no pacote oficial RF54, sem criação informal de seed.

Não há implementação de RF47/48, follow, ranking, medalhas, raio ou filtro privado de experiência/disponibilidade nesta entrega. Esses domínios continuam com suas decisões/pendências anteriores.

## 39. Conclusão real do RF37

**RF37 — BACKEND FUNCIONAL PRINCIPAL CONCLUÍDO / PARCIAL POR DEPENDÊNCIAS ESTRUTURAIS C01/C06.**

Entregues: busca ARTISTAS/CONTRATANTES, taxonomia validada em qualquer Área, subtipos/localização/texto público, AND entre critérios/OR textual, paginação, favorito ARTISTA privado por JWT e DTO/publicabilidade/menor protegidos.

Bloqueados: filtro Banco ativo (C01), favorito CONTRATANTE (C06). Nome artístico próprio não é representável e não foi falsificado.

Validações concluídas: focados 243/243, completo 1647 total/1630 passed/0 failures/0 errors/17 skipped, Semgrep 0 findings/0 blocking, Graphify AST/MCP atualizado e preservação confirmada. Index vazio, HEAD preservado e git diff --check aprovado. Essas evidências sustentam o recorte entregue; não eliminam os blockers ou substituem testes de carga e demais decisões pendentes.

Não se declara RF37 integralmente concluído nem altera conclusões dos RFs dependentes.

## 40. Próximo passo recomendado

Solicitar no próximo pacote oficial somente as capacidades C01/C06 comprovadas; decidir a semântica de nome artístico próprio antes de qualquer mudança de modelo. Preservar o princípio de adaptar backend ao banco quando suficiente.

Após o pacote oficial, integrar os filtros hoje bloqueados com regressões específicas, sem parsing textual ou tipos falsos. A UI de descoberta e seus filtros requer autorização em tarefa separada. Não é necessário reimplementar RF08/RF54 para consumir esta busca.

## 41. Registro semanal

**Data:** 08/10/2026. **RF:** RF37. **Objetivo:** consolidar descoberta pública de ARTISTAS/CONTRATANTES sem alterar database05/frontend.

**Dependências:** RF08/RF10/RF13/RF19/RF27/RF29/RF47/RF48/RF54; RNF02/05/06/07/08/10/13/17 no recorte da busca. **Backend:** seis arquivos produção e dois testes da seção 7. **Frontend/banco/migration/SQL:** NÃO.

**Funcionalidades:** filtros profissionais/subtipos, múltiplas Áreas, favoritos ARTISTA por JWT/EXISTS/lote, DTO mínimo e paginação com count correto. **Correções:** rejeição explícita de catálogo impossível; whitelist privada/pública e expectativas antigas atualizadas sem remover proteção.

**Decisões:** busca pública separada do Banco/recomendação; disponibilidade false não exclui; afirmativas não são filtros; identidade privada não exposta; Banco ativo não inferido; favorito CONTRATANTE não falsificado; nome artístico próprio ausente no modelo.

**Testes:** focados 243/243; completo BUILD SUCCESS, 1647 total/1630 passed/0 failures/0 errors/17 skipped opcionais documentados. **Semgrep:** p/java Docker, 373 arquivos/60 regras/~100% parse/0 findings/0 blocking. **Graphify:** AST exit 0, MCP confirma nova classe; 6655 nós/22971 relações. **Preservação:** 814/814 arquivos protegidos, oito Java congelados e index intactos; git diff --check exit 0.

**Pendências:** C01/C06, nome artístico próprio, pacote/catálogo oficial e integração frontend específica. **Próximo passo:** pacote mínimo oficial e integração posterior autorizada. Sem staging/commit/push.

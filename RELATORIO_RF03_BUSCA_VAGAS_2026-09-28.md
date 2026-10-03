# RF03 — Busca e listagem pública de vagas — 28/09/2026

## Diagnóstico e contrato

Auditei o RF03/RF04 revisado, os relatórios de Vagas e da taxonomia RF04, a auditoria delta e o código atual. A API já oferecia `GET /api/vagas` público, estado `ABERTA`, filtros básicos, cursor por ID crescente e limite de página. Faltavam exclusão imediata das vagas abertas com prazo vencido, experiência, especialização, abrangência, tipo de remuneração e critérios afirmativos. A busca por texto no frontend fazia várias consultas e mesclava páginas; remuneração/experiência eram filtradas após a paginação, tornando a contagem e o cursor incoerentes. A interface exibia opções de demonstração para funções, especializações e afirmativas.

## Entrega

| Camada | Alterações |
|---|---|
| Backend | `VagaController`, `VagaBuscaFiltro`, `VagaSpecifications` e `VagaService`: filtros públicos reais, busca título/contratante, somente `ABERTA` não vencida, OR entre IDs do mesmo filtro e AND entre filtros, ordenação estável por ID, `count` sem cursor e `size+1` para `hasMore`. `VagaListagemResponse`: `totalElements` do feed filtrado. `FuncaoRepository` e `EspecializacaoRepository`: validação dos IDs existentes e compatíveis com área/função. |
| Catálogos e privacidade | `SecurityConfig` e `TalentoService`: apenas GET dos catálogos de funções, especializações compatíveis e categorias afirmativas tornou-se público; a busca privada de talentos continua restrita. `VagaResponse` omite `enderecoCompleto` quando ausente. Feed e similares não transportam endereço completo nem indicador de propriedade, mesmo com JWT do dono; gerenciamento/detalhe autorizado preservam seus dados. |
| Frontend integrado | `OfficialVacancies.jsx`, `VacancySearchFilters.jsx`, `useVacancies.js`, `searchContracts.js`, `catalog.js` e `official-vacancies.css`: filtros reais para visitantes e usuários, catálogo oficial Área → Função → Especialização, categorias afirmativas da API, estado de carregamento/erro/vazio, limpeza de vínculos inválidos, uma consulta paginada por combinação de filtros e contagem fornecida pela API. `VacancyForm.jsx` reutiliza o carregador paginado de especializações sem mudar o contrato RF04. |
| Testes | `VagaControllerRf03IntegrationTest.java`, `GenericEndpointsSecurityIntegrationTest.java` e `searchContracts.test.js`: estados e prazo, cada filtro, combinações/IDs inválidos, catálogo anônimo, count/cursor, privacidade, escrita bloqueada, payload, limpeza de taxonomia, reset e resultado vazio. |

O nível de experiência já é texto opcional em `vagas.experiencia` (`varchar(100)`); a busca usa igualdade sem diferenciar maiúsculas/minúsculas, sem criar enum ou converter níveis. Remuneração usa os campos existentes e a escolha visual do RF04. As categorias afirmativas descrevem a **vaga**, nunca autodeclarações de artistas. O backend permanece a autoridade sobre IDs, compatibilidade e visibilidade. A listagem faz a consulta filtrada antes da paginação e carrega as relações dos IDs da página em segunda etapa; não há filtro sobre resultados truncados no cliente.

## Verificação

- Foco final RF03/RF04/RF05/segurança: **110 testes, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**. O foco anterior, antes do último ajuste de similares, havia passado com 84/84.
- `node --test src/pages/vacancies/contracts.test.js src/pages/vacancies/searchContracts.test.js`: **29/29 passaram**.
- `npm run build`: **sucesso**, Vite compilou 118 módulos. A primeira execução restrita encontrou “Acesso negado” no esbuild; a execução com acesso adequado passou.
- `git diff --check` dos arquivos Java alterados: **sem erros de whitespace**.
- `graphify update .`: executado conforme `AGENTS.md`; alterou artefatos gerados em `graphify-out/`.
- Primeira suíte completa: **782 total, 764 passaram, 1 failure, 0 errors, 17 skipped**. A única falha era a asserção antiga de 401 para `GET /api/funcoes`; o teste passou a esperar 200 apenas na leitura pública, preservando 401/403 para operações protegidas. `.\mvnw.cmd test` final: **782 total, 765 passaram, 0 failures, 0 errors, 17 skipped, BUILD SUCCESS**. Logs finais: `backend/target/rf03-2026-09-28/rf03-focus-verified.log` e `rf03-full-final.log`. Os 17 testes condicionais permaneceram sem habilitação artificial de `palco.current-db-tests` ou `palco.official-db-tests`.

Testcontainers usa PostgreSQL 18 descartável (`OfficialPostgreSQLContainer`); não se conectou ao banco oficial. `spring.jpa.hibernate.ddl-auto=validate` permanece. **Backend de produção e frontend integrado foram alterados. Banco oficial, schema, tabelas, colunas, enums, SQL, dump e migrations não foram alterados.** Nenhum commit, push, reset, clean de Git ou stash foi executado; as muitas mudanças locais preexistentes foram preservadas.

## Riscos e pendências

O filtro de experiência depende do texto salvo na vaga; variações de redação podem reduzir resultados, pois não há catálogo de níveis para `vagas.experiencia`. A contagem e as páginas são consistentes na consulta testada, mas publicações concorrentes entre requisições podem mudar o conjunto, como em qualquer cursor sobre feed vivo. A validação manual visual/ponta a ponta com API ativa e catálogos reais permanece pendente; os testes automatizados e o build não substituem essa inspeção. A homologação do snapshot remoto oficial continua em tarefa própria.

## Registro semanal e próximo passo

| Campo | Registro |
|---|---|
| Objetivo | Fechar RF03 público sobre os contratos e catálogos já existentes, sem tocar candidatura ou Banco de Talentos. |
| Resultado | Feed válido, filtros no servidor, taxonomia/afirmativas por catálogo, paginação e privacidade verificadas. |
| Testes | Foco RF03/RF04/RF05/segurança 110/110; frontend 29/29 e build; Maven completo 782 total, 765 aprovados, 0 failures, 0 errors, 17 skipped, BUILD SUCCESS. |
| Próximo passo | Homologar visualmente busca anônima e autenticada contra a API ativa e registrar qualquer diferença de catálogo/dados oficiais em tarefa separada. |

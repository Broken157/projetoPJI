# Sincronização Palco com database04 — 02/10/2026

**Estado:** fonte ativa migrada integralmente; init/seed, validação JPA e startup recuperados. **Homologação completa bloqueada:** Maven final com 791 testes, 739 passed, 34 failures, 1 error e 17 skipped; BUILD FAILURE, exit 1. Cadastro sem username e ausência da garantia física de unicidade de candidatura impedem o critério verde, sem correção permitida dentro deste escopo.

Checkout: `C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline`. Branch: `integracao-recuperada-2026-09-15`. HEAD inicial: `a6164e38b177c765638d61eb0d68c4b46f79f052`. Nenhum commit, push, stash, reset, clean ou restauração em massa foi executado.

## 1. Objetivo

Substituir database02 como fonte ativa por database04 completo, recuperar PostgreSQL/Spring com `ddl-auto=validate`, alinhar Testcontainers e verificar a regressão. Escopo: configuração, compatibilidade Java e infraestrutura/fixtures de testes. Nenhuma funcionalidade nova foi implementada.

## 2. Fonte única ativa

`database04/palco-database` contém o pacote completo recebido da Manu. Inicialização local, Compose e `OfficialPostgreSQLContainer` usam esse mesmo diretório. Não houve sobreposição parcial de arquivos sobre database02, importação de dump, migration histórica RF07 nem reaproveitamento da correção local de vírgula do database02.

## 3. SHA-256 do ZIP

Origem: `C:\Users\masca\Downloads\palco-database04.zip`.

Esperado e verificado:

```text
52b1c4af06d79a7efae47e6fa320b4a0a32359efaae1d40e2a73d06129c6df2b
```

## 4. Manifesto e fingerprint

Manifesto em `database04/snapshot.properties`, fora do pacote oficial. Há 46 arquivos: 45 SQL e o arquivo vazio `git`, também preservado. Fingerprint:

```text
db8f05cabadfd3935b7c703b7b21252f170fa529c0d30e7e61755b46d7fd5f39
```

Algoritmo: nomes relativos com `/`, ordenação ordinal, uma linha `nome<TAB>sha256_em_minúsculas<LF>` por arquivo, SHA-256 do texto UTF-8. Os hashes individuais constam no manifesto e em `snapshot-manifest.json`.

## 5. SQL oficial preservado

A comparação final entre cada entrada do ZIP e cada arquivo extraído confirmou **46/46 idênticos, 0 divergências e 0 arquivos adicionais dentro do pacote**. Não foi alterado nenhum SQL de tabela, enum, índice, constraint, trigger, função, procedure, query, init ou seed. Evidência: `snapshot-final-verification.json`.

## 6. Database02 desativado

Os cinco scripts foram renomeados de `scripts/database02` para `scripts/database04`; o bootstrap foi renomeado para `Database04BootstrapIntegrationTest`. Paths, nomes, hashes e variáveis de teste agora identificam database04. A variável ativa é `PALCO_TEST_DATABASE04_PATH`, com propriedade opcional `palco.test.database04-path`.

`database` e `database02` permanecem como histórico, sem carga nos caminhos ativos. A referência histórica no README e a regra de ignore do ambiente privado database02 são intencionais. `ManuDumpSchemaIntegrationTest` mantém o nome histórico, mas verifica database04 e não restaura dump. Evidência: `active-source-references.log` e o helper de Testcontainers.

## 7. Banco local criado

Foi criado **`palco_dev_manu04`**, PostgreSQL 18.4 em `localhost:5432`, vazio, com `template0` e UTF-8. O script verificou a inexistência do banco antes da criação. Uma nova chamada foi recusada com “Banco palco_dev_manu04 ja existe; nenhuma inicializacao executada.”

Os bancos anteriores foram preservados: `palco_dev_manu02`, `palco_manu_teste`, `portifoliodb`, `xoppo`, além dos bancos administrativos. Nenhum deles recebeu init/seed/patch nesta tarefa. Evidências: `local-catalog.log`, `local-init-command.log` e `initialize-existing-guard.log`.

## 8. Init oficial

Executado no diretório original dos includes relativos com `psql -X --no-password -a -v ON_ERROR_STOP=1 -v VERBOSITY=verbose -f init.sql`, usando credencial por ambiente. **Exit code 0**. Log completo: `init-seed-strict.log`; código do comando em `local-init-command.exit`.

Não houve correção SQL nem comando estrutural complementar depois do init. Os arquivos de `05_query` fazem parte do pacote preservado, mas o init original não os executa.

## 9. Seed oficial

O próprio `init.sql` inclui `seed.sql`. O seed foi executado **uma única vez nesse fluxo**, concluiu sua transação com `COMMIT` e compartilha o exit 0 do init. Não foi rodado novamente de forma separada. Nenhum catálogo/seed antigo foi importado.

## 10. Catálogos e dados iniciais

Após init/seed: 7 usuários, incluindo a identidade reservada ID 0; 7 áreas; 4 funções artísticas; 4 especializações; 1 vaga; 0 vínculos de Banco de Talentos.

| ID da área | Nome oficial |
|---|---|
| 1 | Artes Cênicas |
| 2 | Música |
| 3 | Dança |
| 4 | Artes Visuais |
| 5 | Audiovisual |
| 6 | Arte e Tecnologia |
| 7 | Artes Literárias |

Os testes passam a respeitar esses IDs/nomes. DML de limpeza e fixtures é aplicado somente em bancos descartáveis de teste. O agendador existente pode encerrar a vaga demonstrativa cujo prazo já venceu; por isso uma listagem pública vazia após startup é compatível com esse seed.

## 11. Objetos efetivamente criados

| Objeto em `public` | Quantidade |
|---|---:|
| Tabelas | 43 |
| Colunas | 280 |
| Tipos enum | 24 |
| Functions | 12 |
| Procedures | 12 |
| Triggers não internos | 3 |

Contagens obtidas do catálogo PostgreSQL, não da quantidade de arquivos SQL. O Compose novo confirmou as mesmas contagens principais de estrutura. A criação de uma rotina não comprova que todos os seus caminhos executam corretamente.

## 12. Spring antes das adaptações

A primeira tentativa no sandbox encontrou problema de visibilidade do cache/classes Maven, antes de validar o schema. Ela não foi tratada como defeito do banco. A execução com acesso ao runtime correto conectou o Hikari e registrou a primeira incompatibilidade real:

```text
Schema validation: missing table [categorias_afirmativas]
```

Arquivo: `spring-before-verified.log`, causa em `entityManagerFactory`; processo terminou com exit 1. O Java ainda esperava a entidade/tabela antiga, inexistente no database04. Não foi criada tabela para contornar a falha.

## 13. Incompatibilidades Java ↔ database04

| Diferença | Classificação | Tratamento |
|---|---|---|
| Categoria como entidade e join table | A: Java desatualizado | Enum, coluna escalar e consultas JPA compatíveis |
| `BLOQUEADA` ausente no enum Java de candidatura | A | Valor oficial acrescentado; switch terminal compatível |
| `MUNICIPAL` ausente em `Abrangencia` | A | Valor oficial acrescentado |
| Plataforma obrigatória de embed não preenchida | A | Mapear/preencher provedor já validado pelo fluxo existente |
| Enum SQL de plágio com `_`, contrato API com espaços | A | Conversão nas queries de inserção/projeção |
| `username` obrigatório sem campo no cadastro atual | Contrato funcional pendente | Mapeamento obrigatório preservado; cadastro continua bloqueado |
| Mais de uma categoria e 50+ | B: estrutura não representa RF04 revisado | Rejeição explícita 422; pendência de decisão oficial |
| Unicidade vaga/artista sem constraint física | B: garantia física ausente | Teste de conflito mantido como evidência; sem SQL extra |
| Rotinas com referências/types incompatíveis | Dívida SQL sem chamada Java direta | Provas de execução registradas; SQL intacto |

Nenhuma entidade válida foi silenciada para o Hibernate iniciar. O mapeamento antigo de categoria foi substituído pelo modelo que efetivamente existe.

## 14. Mudanças de backend

Alterações de produção limitadas a `CategoriaAfirmativa`, seu converter/adaptador de catálogo, `Vaga`, `VagaService`, `VagaController`, `VagaSpecifications`, enums `StatusCandidatura`/`Abrangencia`, mapeamento `BancoTalentos`/`BancoTalentosId`, `EmbedExterno`, preenchimento da plataforma em `PortfolioVideoService` e conversão do enum em `DenunciaRepository`. `CandidaturaService` apenas trata `BLOQUEADA` como estado terminal no switch já existente. `Usuario` mantém o campo obrigatório.

Não foram adicionados endpoints, filtros, convites, eventos ou regras de RF novos. Testes de funcionalidades existentes foram adaptados em fixtures e expectativas estruturais, sem transformar a tarefa em implementação dessas funcionalidades.

Consulta técnica: Context7, biblioteca `/hibernate/hibernate-orm` (documentação `main`), e guia oficial [Hibernate ORM 7.2, atualmente 7.2.25.Final](https://docs.hibernate.org/orm/7.2/userguide/html_single/). Runtime observado: Hibernate **7.2.12.Final**, Java 21.0.11. A documentação consultada não foi usada como prova da versão instalada. Foi mantido o padrão de converters/string binding já existente no projeto.

## 15. Categorias afirmativas e limite do RF04

Database04: uma coluna opcional `vagas.categoria_afirmativa`, enum `MULHER`, `ETNICO_RACIAL`, `PCD`, `LGBTQIA`. Não existem `categorias_afirmativas` nem `vagas_categorias_afirmativas` na estrutura ativa.

O catálogo API preserva IDs estáveis 1–4, derivados desses valores. O contrato de lista é preservado: resposta contém zero ou um ID; criação/edição rejeita mais de uma categoria e IDs positivos sem representação oficial com **422** e erro contextual. Não salva apenas a primeira, não descarta categorias silenciosamente e não mapeia 50+ para outro valor. Busca com vários IDs continua expressando alternativas entre vagas, sem persistir várias categorias na mesma vaga.

Teste cobre rejeição de múltiplas categorias, ID 5/50+ não representável, ausência de persistência e rollback de edição rejeitada, incluindo título e categoria originais.

**Limitação estrutural do database04 frente ao RF04 revisado:** uma categoria por vaga e ausência de 50+. RF04 não está totalmente homologado quanto às categorias múltiplas. Nenhuma adaptação de UI foi feita.

## 16. Username

`Usuario.username` permanece obrigatório e único, conforme o banco, sem default provisório. Nenhum service de cadastro gera username. A decisão do usuário foi mantida: RF01 deve incorporar `@username` corretamente em tarefa posterior.

O cadastro convencional atual tenta persistir `NULL`: PostgreSQL **23502**, HTTP 409 pela tradução existente. Criação inicial de conta Google também esbarra na ausência de username. Testes reais de cadastro permanecem como evidência desse bloqueio. Fixtures independentes recebem identidades próprias apenas dentro dos bancos de teste; isso não altera o fluxo de cadastro.

Não cabe à Manu tornar username opcional para esta sincronização. A pendência é completar o contrato funcional de cadastro posteriormente.

## 17. Banco de Talentos

Foi mapeada a relação oficial `banco_talentos`: chave composta `(contratante_id, artista_id)`, `data_adicao` obrigatório, FKs para `usuarios` e check de IDs diferentes. Não foi criado repositório/API paralela nem implementado RF13/RF17.

Teste de mapeamento verifica o mesmo artista associado a dois contratantes distintos e a rejeição de IDs iguais (23514), respeitando o dono de cada coleção. A estrutura existente de `/api/talentos` continua sendo um explorador global legado, distinto da coleção salva específica por contratante. A integração funcional dessa coleção e seus filtros/convites fica para a tarefa de RF correspondente.

## 18. Functions/procedures auditadas

Não foram encontradas chamadas diretas Java ativas a `fn_buscar_vagas`, `fn_listar_banco_talentos`, `sp_publicar_vaga`, `sp_atualizar_perfil` ou outras rotinas via `CALL`, `@Procedure` ou stored-procedure query. Fluxos atuais usam services/repositories JPA. Triggers oficiais continuam sendo executados pelo PostgreSQL conforme o init; isso não equivale a uma chamada direta Java.

| Rotina | Prova executada | Resultado/impacto | Menor ajuste a avaliar pela Manu |
|---|---|---|---|
| `fn_buscar_vagas()` | SELECT isolado no banco local; testes SQL de busca/paginação | Exit 0, 0 vagas abertas após prazo vencido; SQL validado passa | Nenhum encontrado nos cenários verificados |
| `fn_listar_banco_talentos(4)` | Sondagem transacional | 42804: `character(2)` retornado para `estado`, declarado `varchar`, coluna 9 | Compatibilizar tipo de retorno/cast em `03_functions/fn_filtrar_banco_talentos.sql:63` |
| `sp_atualizar_perfil(p_usuario_id=>1)` | CALL transacional separado | 42703: coluna `foto_perfil` inexistente | Usar a coluna oficial `foto_perfil_url`, em `04_procedures/sp_atualizar_perfil.sql:62` |
| `sp_publicar_vaga(... categorias=>array[1])` | CALL transacional com contratante/área válidos | 42P01: `vagas_categorias_afirmativas` inexistente | Adaptar rotina à coluna escalar oficial e rejeitar múltiplas, em `04_procedures/sp_publicar_vaga.sql:139-142` |

As sondagens mutáveis usaram transação/rollback e não deixaram novas linhas. Sequências PostgreSQL podem avançar mesmo com rollback; não foi feito reset de sequência no banco de desenvolvimento. Os logs incluem comandos, SQLSTATE e contexto completo. A consulta conjunta original interrompeu antes de `sp_atualizar_perfil`; por isso esta procedure foi sondada separadamente, assim como a busca.

Os oito arquivos `05_query` foram preservados. Referências estáticas antigas a `foto_perfil`, `localizacao` e `nivel_medalha` são dívida a revisar, sem afirmar que todos esses arquivos foram executados. Não foi feita auditoria funcional exaustiva das 12 functions e 12 procedures. As falhas demonstradas não impedem o startup porque não são chamadas diretas nos fluxos Java auditados.

## 19. Testcontainers antes/depois

Antes: helper/scripts/bootstrap carregavam database02, suas identidades/hashes e complemento de catálogo de teste. Depois: **somente o pacote database04 completo**, PostgreSQL `postgres:18-alpine`, `palco_test_manu04`, init original com seed embutido, fingerprint obrigatório de 46 arquivos e log completo por inicialização.

Foi removida a carga automática de `db/catalogo-test.sql`. Nenhum dump, migration RF07, tabela auxiliar ou DDL complementar é aplicado. DML de isolamento/fixtures após init permanece permitido. A alternativa local cria apenas banco descartável com prefixo `palco_test_` + UUID e valida loopback/banco administrativo; as execuções reportadas usaram Docker real.

Na primeira rodada completa, três testes legados executaram constraints temporárias de injeção de falha em seus bancos descartáveis. Essa execução é uma ocorrência de diagnóstico, não prova de conformidade final. Os testes foram corrigidos para injetar falhas nos repositories com spies, mantendo assertions de rollback/after-commit. A busca final no Java de teste registrou **0 comandos de DDL complementar**. Nenhum arquivo SQL da Manu foi alterado, inclusive naquela rodada.

## 20. Scripts, Compose e ambiente privado

Scripts ativos: `Initialize-Database.ps1`, `environment.ps1`, `Start-Backend.ps1`, `Test-Backend.ps1`, `docker-init.sh`, em `scripts/database04`. O teste padrão invoca `backend/.\mvnw.cmd test`; testes focados/alternativa local continuam opcionais. O inicializador verifica identidade/hash, recusa banco existente e não repete seed.

Compose: projeto `palco-manu04`, container `palco-postgres-manu04`, banco `palco_dev_manu04`, porta **5435**, volume novo `palco_manu04_data` (nome efetivo `palco-manu04_palco_manu04_data`). Snapshot montado somente para leitura. `docker compose config --quiet` e `docker compose up -d --wait --wait-timeout 60 database` passaram; container saudável e init/seed oficial confirmado. Não houve remoção de volumes antigos.

Backend local usa `localhost:5432`. Para usar Compose, definir `DB_URL=jdbc:postgresql://localhost:5435/palco_dev_manu04`. Credenciais são fornecidas por ambiente ou `backend/.env.database04.local.ps1`, ignorado pelo Git; variáveis de processo existentes têm precedência. O ambiente privado anterior não foi alterado. Maven usa `backend/target-maven/database04`, evitando classes antigas de outro snapshot e o diretório utilizado pelo editor.

## 21. Ddl-auto=validate

Mantido em `application.properties` e no profile `banco-oficial-local`. Não foi usado `create`, `update` nem `create-drop`. O EntityManagerFactory inicializou contra database04 real; bootstrap, enum e mapeamento também foram verificados no PostgreSQL dos testes.

## 22. Startup final

Startup final com o código que encerrou a suíte: `Initialized JPA EntityManagerFactory` às 17:58:58 e `Started BackendApplication` às 17:59:02 de 02/10/2026. Tomcat 8080; `ddl-auto=validate` passou. Processo próprio PID 28824 confirmado pela classe `com.portifolio.BackendApplication` e pela porta em escuta; permanece em execução ao encerramento da tarefa. Compose permanece saudável. Evidências: `spring-final.log`, `backend-runtime-final.json`, `compose-health-final.log`.

A execução após a adaptação inicial registrou `Initialized JPA EntityManagerFactory` e `Started BackendApplication`, Tomcat 8080. Esse processo foi encerrado de forma controlada para validar o código final; o exit 1 de `spring-after.exit` corresponde ao encerramento do processo já iniciado, não a uma falha de validate. A execução final é registrada separadamente em `spring-final.log`.

## 23. Smoke test final

| Requisição HTTP no backend final | Resultado |
|---|---|
| GET `/api/capacidades` | 200 |
| GET `/api/areas` | 200, sete áreas oficiais |
| GET `/api/vagas/categorias-afirmativas` | 200, quatro opções oficiais |
| GET `/api/vagas` | 200, `content=[]`, `totalElements=0` |
| GET `/api/talentos/areas`, sem sessão | 401 |
| GET `/api/usuarios/me`, sem sessão | 401 |
| POST `/api/auth/login`, credenciais inválidas de smoke | 401 |

Evidências: `http-smoke-final.json`, `http-areas-final.json`, `http-categorias-final.json`. Não foi cadastrado usuário novo no banco de desenvolvimento para contornar username.

Autenticação positiva de contas existentes, senha BCrypt, JWT/cookies e restrições de autorização são exercitados por `GenericEndpointsSecurityIntegrationTest`; não dependem de implementar cadastro. Não foi presumida a senha em texto dos usuários seeded nem anunciado login com o seed sem verificação.

## 24. Testes focados

Executados primeiro bootstrap/mapeamento/SQL/segurança/RF03/RF04. Histórico preservado:

| Rodada | Total | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Focados iniciais | 126 | 49 | 27 | 50 | 0 |
| Focados após fixtures/categorias | 127 | 124 | 3 | 0 | 0 |
| Reparo RF03 + mapeamento | 36 | 36 | 0 | 0 | 0 |
| Regressão de fixtures/rollback, 13 classes | 198 | 189 | 9 | 0 | 0 |
| Reparo denúncias/portfólio/talentos/mapeamento | 87 | 87 | 0 | 0 | 0 |
| Estado explícito de fixture Google incompleta | 1 | 1 | 0 | 0 | 0 |

Na suíte completa final, as sete classes solicitadas passaram **127/127**, sem failures/errors/skips:

| Classe | Passed/total |
|---|---:|
| Database04BootstrapIntegrationTest | 5/5 |
| ManuDumpSchemaIntegrationTest | 6/6 |
| OfficialSchemaMappingIntegrationTest | 5/5 |
| ValidatedOfficialSqlIntegrationTest | 28/28 |
| GenericEndpointsSecurityIntegrationTest | 26/26 |
| VagaControllerRf03IntegrationTest | 31/31 |
| VagaPublicacaoRf04IntegrationTest | 26/26 |

Isso usa os XML da rodada completa final, sem apresentar a soma de reparos isolados como se fosse uma execução focada única.

Assertions incompatíveis com o catálogo/trigger de outro snapshot foram adaptadas. Não foram apagados testes válidos para obter verde: cadastro real e unicidade física permanecem verificados, inclusive quando falham. Segurança genérica usa conta existente de fixture com login real; o teste de cadastro pertence às classes específicas e continua ativo.

## 25. Maven completo

Comando no backend: **`.\mvnw.cmd test`**, sem filtro e sem modo offline, via `scripts/database04/Test-Backend.ps1`. Docker/Testcontainers real; nenhuma flag habilitou os testes locais condicionais. Logs e XML das rodadas diagnósticas foram preservados em pastas distintas para não confundir seus resultados.

| Rodada completa | Total | Passed | Failures | Errors | Skipped | Resultado |
|---|---:|---:|---:|---:|---:|---|
| Baseline anterior à troca, informado pelo usuário | 784 | 767 | 0 | 0 | 17 | BUILD SUCCESS, não reexecutado aqui |
| Database02 bloqueado, informado pelo usuário | 788 | 244 | 0 | 527 | 17 | BUILD FAILURE, não reexecutado aqui |
| Database04 diagnóstico inicial | 791 | 617 | 41 | 116 | 17 | BUILD FAILURE |
| Database04 segunda rodada | 791 | 738 | 35 | 1 | 17 | BUILD FAILURE |
| Database04 final | 791 | 739 | 34 | 1 | 17 | BUILD FAILURE, exit 1 |

As contagens informadas pelo usuário são referências históricas; não substituem prova deste checkout. O aumento de 788 para 791 decorre dos três novos testes de seed, chave por contratante e rejeição/rollback de categorias incompatíveis.

## 26. Contadores e conflitos restantes

791 total = **739 passed + 34 failures + 1 error + 17 skipped**. Rodada terminou em 02/10/2026 às 17:57:01, duração Maven 05:14. `full-final-summary.json` consolida 57 classes e os 57 XML estão preservados em `full-final-reports`.

| Classe com conflito | Failures | Errors | Causa |
|---|---:|---:|---|
| AuthControllerRf01Rf02IntegrationTest | 9 | 0 | Cadastro sem username, 23502/409 |
| EmailVerificationRf26IntegrationTest | 9 | 0 | Depende de cadastro incompatível |
| GoogleAuthRf24HardeningIntegrationTest | 5 | 0 | Criação inicial sem username; resposta 401 |
| GuardianConsentRf27IntegrationTest | 10 | 0 | Cadastro convencional/Google antes do consentimento |
| UsuarioControllerIntegrationTest | 0 | 1 | HTTP 409 no cadastro que prepara o cenário |
| CandidaturaControllerRf06IntegrationTest | 1 | 0 | Ausência de UNIQUE físico vaga/artista |

Portanto, **33 failures + 1 error** estão ligados ao cadastro e suas dependências; **1 failure** demonstra a falta da barreira física de candidatura. Skips preservados: `CurrentLocalSchemaIntegrationTest` 4 e `OfficialLocalApiIntegrationTest` 13. Nenhuma flag habilitou essas condições.

O teste `constraintRealTrataDuplicidadeMesmoSePreConsultaNaoEncontrar` força a pré-consulta a devolver falso apesar de já existir uma candidatura. Database04 não contém UNIQUE `(vaga_id, artista_id)`; a gravação retorna 201 em vez de 409. Isso prova a ausência de uma segunda barreira física, não que toda candidatura duplicada no fluxo ordinário seja aceita: a pré-consulta/serialização já existentes continuam protegendo os demais cenários.

A relação completa de testes que falharam, com nome/assertion, fica em `remaining-conflicts.json` e nos XML finais. Nenhum conflito foi ocultado com skip, alteração do SQL ou implementação de RF.

## 27. Frontend

**Frontend alterado nesta tarefa: NÃO.** A comparação de hashes dos arquivos existentes antes da tarefa registra 0 alterados e 0 removidos em suas árvores. Há alterações de frontend no working tree que já existiam na auditoria inicial; elas não são delta desta sincronização. Não foi executado build npm nem redesenhada a UI de categorias.

## 28. Banco alterado

**SQL/schema oficial alterado: NÃO.** Criar bancos novos e executar init/seed original são as ações autorizadas. Não houve patch no banco de desenvolvimento/Compose. Fixtures e limpeza DML são restritas aos testes descartáveis; a ocorrência legada de DDL na rodada diagnóstica e sua remoção estão explicitadas no item 19.

## 29. RF implementado

**RF novo implementado: NÃO.** Cadastro com username, candidatura, perfil, Banco de Talentos, filtros/convites, portfólio e chat não receberam features. Plataforma de embed e tradução do enum de denúncia apenas fazem os fluxos vigentes persistirem no schema oficial. Categorias incompatíveis recebem erro explícito, sem simulação de suporte.

## 30. Riscos e limites

- Cadastro convencional/primeira conta Google não é compatível até o fluxo de username correto; os fluxos que dependem de novo cadastro ficam bloqueados em cascata.
- RF04 revisado não cabe integralmente na estrutura oficial atual; rejeição 422 pode ser percebida por clientes existentes e não equivale a homologação de múltiplas/50+.
- Unicidade de candidatura depende das defesas Java existentes, sem a garantia física exigida pelo teste conservado.
- Há três defeitos de rotinas SQL demonstrados, além de referências estáticas em queries; criação bem-sucedida não significa execução funcional homologada de todas as rotinas.
- Os 17 testes condicionais continuam sem execução. `validate` confirma compatibilidade de mapeamento, não cobertura de todos os RFs ou todas as constraints.
- O checkout já estava amplamente modificado antes da tarefa; um commit global poderia misturar frentes de trabalho.

## 31. Pendências reais para a Manu

1. Corrigir o tipo retornado de `estado` em `fn_listar_banco_talentos` (42804).
2. Corrigir a coluna usada por `sp_atualizar_perfil` para `foto_perfil_url` (42703).
3. Adequar `sp_publicar_vaga` à estrutura escalar atual de categoria (42P01), sem descartar combinações não representáveis.
4. Consolidar com os requisitos a garantia de unicidade vaga/artista. Para o contrato conservado no teste, a menor barreira física é uma constraint única no par; o efeito sobre retirada/reinscrição precisa da decisão oficial de ciclo de vida. Nenhuma constraint foi adicionada aqui.
5. Resolver a diferença estrutural de categorias múltiplas/50+ frente ao RF04 revisado numa nova entrega oficial, se esse requisito permanecer vigente.

As correções devem vir num pacote oficial completo e novamente validado; não copiar trechos de versões históricas. Username obrigatório não é uma solicitação de flexibilização do banco.

## 32. Pendências futuras de RF

RF01: incorporar `@username`, validação/unicidade e os contratos de cadastro convencional/Google. Revalidar depois os cenários de autenticação, verificação de email e consentimento que dependem desse cadastro. RF04: implementar/homologar múltiplas e 50+ somente após estrutura/decisão correspondente. RF13/RF17: integrar a coleção persistente por contratante, distinguindo-a do explorador global legado. RF06: revalidar a garantia/ciclo de candidatura após decisão oficial.

Não há autorização nesta tarefa para desenvolver essas pendências nem as demais RFs listadas pelo usuário.

## 33. Aptidão do ambiente

**Apto para continuar desenvolvimento restrito com contas existentes: SIM. Infraestrutura integralmente homologada segundo o critério solicitado: NÃO.** Banco/Compose sobem, init/seed passam, backend conecta, validate/startup e os 127 testes focados passam. A suíte completa tem 34 failures/1 error; cadastro e a garantia física de candidatura permanecem bloqueados. O checkpoint verde posterior deve aguardar as correções/decisões correspondentes.

O critério solicitado de infraestrutura totalmente homologada exige suíte completa com 0 failures/0 errors. Se houver conflitos remanescentes, o ambiente pode permitir desenvolvimento com contas existentes, mas o checkpoint verde deve aguardar resolução dos bloqueios. O relatório não apresenta “Spring iniciou” como equivalente a “regressão verde”.

## 34. Candidatos ao checkpoint Git e preservação

Antes de qualquer alteração: `git status`, branch, remotes, diff stat, diff check e HEAD foram capturados em `evidencias/database04-2026-10-02/*-before.*`. Baseline: **116 arquivos no diff contra HEAD, 3594 inserções e 1552 exclusões**, além dos untracked já existentes. Foram fingerprintados 1715 arquivos para distinguir o delta desta tarefa.

Comparação com o baseline: **59 arquivos existentes modificados nesta tarefa** (7 são derivados Graphify); **6 paths antigos renomeados**, correspondentes aos cinco scripts e ao bootstrap; **58 arquivos novos fora do Graphify**, incluindo os 46 do pacote, manifesto, destinos dos renomes, três classes Java e os dois relatórios. Outros 109 arquivos novos são derivados Graphify/cache/backup. A lista completa de paths está em `delta-database04.tsv`; os logs de evidência e o ambiente privado ignorado são registrados separadamente. Os 1650 paths restantes do baseline continuam com os mesmos bytes. Frontend e snapshots históricos: 0 modificados/0 removidos.

Estado final contra HEAD, incluindo o trabalho anterior: `git diff --stat` registra **123 arquivos, 3789 inserções, 1639 exclusões**. Isso não é uma contagem exclusiva desta tarefa e não inclui os untracked. `git diff --check`: **exit 0, nenhuma ocorrência**. Branch, HEAD e remotes permanecem iguais à auditoria inicial; não houve staging automático. `status-after.log`, `diff-stat-after.log` e `diff-check-after.log` capturam o estado revisável.

Candidatos, para revisão posterior sem staging automático:

1. Snapshot/inicialização: `database04` completo + manifesto, cinco renomes de scripts, Compose, configurações/env ignore, README e diretório de build isolado.
2. Compatibilidade Java: classes/mapeamentos/queries descritos no item 14, incluindo os três arquivos novos de modelo/converter.
3. Infraestrutura e regressão: helper, bootstrap renomeado, testes de schema, fixtures explícitas e injeção de falhas sem DDL extra. Manter as evidências dos testes que falham.
4. Documentação: este relatório e `REGISTRO_SEMANAL_2026-10-02.md`; escolher evidências sanitizadas para versionamento.
5. Artefatos derivados Graphify, separados do código: `graphify update .` terminou com exit 0, **5476 nós, 17603 arestas, 321 comunidades**. Gerou/atualizou grafo, HTML, relatório, manifestos, labels, cache e backup datado. Revisar caches/backups antes de decidir o que entra no checkpoint; nenhuma rotulagem adicional com LLM foi executada.

Credenciais locais, tokens, diretórios de build e caches de runtime não são candidatos. `.gitignore` confirmado para os dois ambientes privados e `target-maven`; nenhum ambiente privado está rastreado. Logs, XML e relatórios foram sanitizados, incluindo JWTs/cookies e valores reais de DB_PASSWORD/JWT_SECRET. Verificação registrada em `secrets-audit.json`: **0 ocorrências remanescentes dos segredos locais e 0 JWTs** no conjunto inspecionado. Para senha curta, a verificação usa literal delimitado para não confundir fragmentos de hashes/IDs com credencial. Os arquivos oficiais SQL não participam das escritas de sanitização.

O próximo checkpoint é controlado e depende do estado real da regressão; nenhum commit/push foi feito. Como a suíte ainda falha, os grupos acima são candidatos de revisão, não uma autorização automática para publicar um checkpoint verde.

## Evidências e reprodução

Pasta de evidências: `C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline\evidencias\database04-2026-10-02`.

- [Manifesto final do pacote](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/database04-2026-10-02/snapshot-final-verification.json).
- [Init/seed completo](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/database04-2026-10-02/init-seed-strict.log).
- [Catálogo local](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/database04-2026-10-02/local-catalog.log).
- [Objetos e contratos do catálogo final](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/database04-2026-10-02/schema-catalog-final.json).
- [Conflitos da regressão](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/database04-2026-10-02/remaining-conflicts.json).
- [Maven final](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/database04-2026-10-02/mvn-test-full-final.log).
- [Preservação de arquivos](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/database04-2026-10-02/preservation-audit.json).
- [Lista completa do delta](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/database04-2026-10-02/delta-database04.tsv).
- [Startup final](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/database04-2026-10-02/spring-final.log).
- [Smoke final](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/database04-2026-10-02/http-smoke-final.json).
- [Auditoria de segredos](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/database04-2026-10-02/secrets-audit.json).
- [Registro semanal](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/REGISTRO_SEMANAL_2026-10-02.md).

Na raiz do checkout, com JDK 21, Docker e ambiente privado configurados:

```powershell
.\scripts\database04\Start-Backend.ps1
.\scripts\database04\Test-Backend.ps1
```

Executar o inicializador somente para um banco novo; ele recusa `palco_dev_manu04` já criado. Referência da flag de erro fatal: [psql PostgreSQL 18 — ON_ERROR_STOP](https://www.postgresql.org/docs/18/app-psql.html).

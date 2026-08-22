# RELATÓRIO TÉCNICO — RF03: LISTAGEM E BUSCA DE VAGAS

## 1. Objetivo e escopo

Auditar e finalizar o backend do RF03 sem alterar o banco de dados nem o frontend. O trabalho seguiu a ordem: auditar o fluxo existente, identificar lacunas, reaproveitar o que já estava correto, implementar somente o necessário e executar testes de integração e regressão.

O fluxo auditado foi: requisição HTTP → `VagaController` → `VagaService` → `VagaSpecifications`/repositórios → entidades e banco oficial.

## 2. Estado inicial encontrado

O RF03 já estava parcialmente implementado e possuía uma base funcional relevante:

- `GET /api/vagas` era público e retornava somente vagas com status `ABERTA` no feed principal;
- havia filtros por título, cidade, estado, modalidade, tipo de contrato, faixa salarial e tags;
- a consulta usava `Specification`, parâmetros vinculados e paginação no banco;
- a paginação era por cursor (`id > cursor`), com ordenação estável por `id` crescente, tamanho padrão 20 e máximo 50;
- a resposta já expunha `content`, `nextCursor` e `hasMore`;
- vagas canceladas eram mostradas apenas ao artista autenticado que havia se candidatado;
- o carregamento em lote com `EntityGraph` evitava paginação sobre `join fetch` de coleção e reduzia risco de N+1;
- o DTO público não expunha senha, tokens, e-mail, telefone ou data de nascimento;
- existiam 102 testes aprovados no baseline válido.

As lacunas identificadas foram:

- ausência do filtro por empresa;
- ausência do filtro de área de atuação, embora o banco oficial já possuísse o campo `vagas.categoria` compatível;
- ausência de endpoint de vagas similares por tags;
- ausência da indicação de vaga própria para contratante autenticado;
- listagem de vagas canceladas sem cursor independente;
- cobertura incompleta das combinações de filtros, páginas finais, recomendações e isolamento das canceladas;
- frontend sem consumo do novo endpoint de similares e sem evidência de rolagem infinita/botão de retorno ao topo do feed público.

## 3. Auditoria por camada

### 3.1 Entidades e enums

- `Vaga.status` continua sendo a fonte de verdade para separar `ABERTA` de `CANCELADA`.
- `Vaga.categoria` foi reutilizado como área de atuação, evitando coluna ou migração nova.
- `PerfilContratante.nomeEmpresa`, com fallback para o nome do usuário, foi reutilizado no filtro por empresa.
- Modalidade e tipo de contrato continuam validados pelos enums já existentes.
- Nenhuma entidade recebeu mudança estrutural ou de mapeamento de banco.

### 3.2 DTOs

- `VagaBuscaFiltro` recebeu `empresa`, `areaAtuacao` e `cursorCanceladas`.
- `VagaListagemResponse` recebeu `nextCursorCanceladas` e `hasMoreCanceladas`, preservando o contrato existente de paginação do feed.
- `VagaResponse` recebeu `propriaDoContratante`, calculado no servidor a partir do JWT e da associação persistida.
- Nenhum dado pessoal adicional passou a ser serializado.

### 3.3 Repositórios e consultas

- Os filtros continuam baseados em `Specification` e parâmetros vinculados, sem concatenação de valores em SQL.
- O filtro `empresa` consulta, sem diferenciar maiúsculas/minúsculas, o nome empresarial e o nome de usuário do contratante.
- O filtro `areaAtuacao` consulta `vagas.categoria`, também sem diferenciar maiúsculas/minúsculas.
- A recomendação aplica: status `ABERTA`, pelo menos uma tag em comum, exclusão da vaga de origem, cursor e limite.
- A busca de canceladas usa consulta paginada de IDs do artista autenticado, seguida de carregamento em lote das entidades.
- A ordem final é preservada pelo ID crescente, inclusive após o carregamento em lote.

### 3.4 Serviço

- A regra de negócio permanece centralizada em `VagaService`.
- O feed público combina todos os filtros opcionais e mantém resultado irrestrito quando eles são omitidos, exceto pela regra obrigatória de status `ABERTA`.
- A identidade do usuário é obtida pelo contexto de autenticação; IDs de proprietário/artista enviados pelo cliente não determinam autorização.
- As canceladas possuem cursor próprio e não interferem no cursor do feed aberto.
- Vagas similares são retornadas em página por cursor, com origem inexistente tratada como `404` e origem sem tags resultando em página vazia.
- Foram acrescentadas validações para cursor negativo, faixa salarial inválida, sigla de estado incompatível e IDs de tags não positivos.

### 3.5 Controller e segurança

- `GET /api/vagas` continua público e recebeu apenas parâmetros opcionais aditivos.
- Foi criado `GET /api/vagas/{id}/similares`, também público.
- `GET /api/vagas/minhas` continua protegido e não é confundido com o padrão público de similares.
- A seção de canceladas só é calculada quando o JWT representa um artista válido; visitantes e outros artistas não recebem essas vagas.
- `propriaDoContratante` só é verdadeiro quando o contratante do JWT corresponde ao contratante persistido da vaga.

## 4. Alterações realizadas

| Arquivo | Alteração mínima realizada |
|---|---|
| `backend/src/main/java/com/portifolio/dto/VagaBuscaFiltro.java` | Novos filtros por empresa e área, além de cursor independente para canceladas. |
| `backend/src/main/java/com/portifolio/dto/VagaListagemResponse.java` | Metadados de paginação da seção de canceladas. |
| `backend/src/main/java/com/portifolio/dto/VagaResponse.java` | Indicador seguro `propriaDoContratante`. |
| `backend/src/main/java/com/portifolio/repository/specification/VagaSpecifications.java` | Especificações para empresa, área e exclusão da vaga de origem. |
| `backend/src/main/java/com/portifolio/repository/CandidaturaRepository.java` | Consulta paginada dos IDs de vagas canceladas do artista. |
| `backend/src/main/java/com/portifolio/service/VagaService.java` | Composição dos filtros, validações, paginação independente, similares e cálculo de propriedade. |
| `backend/src/main/java/com/portifolio/controller/VagaController.java` | Novos parâmetros opcionais e endpoint de similares. |
| `backend/src/main/java/com/portifolio/config/SecurityConfig.java` | Liberação explícita somente do GET público de similares. |
| `backend/src/test/java/com/portifolio/controller/VagaControllerRf03IntegrationTest.java` | Nove novos cenários de integração do RF03. |

## 5. Critérios de aceite e evidências

| Critério | Situação inicial | Resultado final | Evidência |
|---|---|---|---|
| Feed principal somente com vagas abertas | Atendido | Mantido | Specification obrigatória por `StatusVaga.ABERTA` e testes de integração. |
| Filtro por título | Atendido | Mantido | Busca parcial sem distinção de caixa. |
| Filtro por empresa | Ausente | Atendido | Busca por nome empresarial ou nome do contratante. |
| Filtros por cidade e estado | Atendido | Mantido e validado | Combinação coberta em integração; estado limitado a dois caracteres. |
| Filtros por modalidade e contrato | Atendido | Mantido | Conversão pelos enums e erro controlado para valor inválido. |
| Faixa salarial | Atendido parcialmente | Atendido e validado | Limites mínimo/máximo combináveis; negativos ou mínimo maior que máximo são rejeitados. |
| Área de atuação | Ausente | Atendido | Parâmetro `areaAtuacao` mapeado para a coluna oficial `categoria`. |
| Tags | Atendido | Mantido e combinado | IDs positivos, correspondência por tags e resultado sem duplicatas. |
| Filtros opcionais | Parcialmente coberto | Atendido | Teste confirma ausência de restrição adicional quando omitidos. |
| Paginação por cursor | Atendido | Mantido e ampliado | Padrão 20, máximo 50, consulta de `size + 1`, `nextCursor`, `hasMore`, última página e ausência de duplicação. |
| Ordem estável | Atendido | Mantido | ID crescente em consultas e remontagem do lote. |
| Canceladas somente para quem se candidatou | Atendido | Mantido e reforçado | JWT e candidaturas persistidas; tentativa de informar outro artista é ignorada. |
| Canceladas com paginação independente | Ausente | Atendido | `cursorCanceladas`, `nextCursorCanceladas` e `hasMoreCanceladas`. |
| Vagas similares por tags | Ausente | Atendido no backend | Endpoint público, somente abertas, origem excluída, cursor e limite. |
| Indicação de vaga própria | Ausente | Atendido | `propriaDoContratante` derivado do JWT, sem ID de dono no payload. |
| Privacidade | Atendido | Mantido | DTO público continua sem credenciais e dados pessoais sensíveis. |
| Proteção contra N+1 | Estrutura existente | Mantida | Página de IDs seguida de carregamento em lote com `EntityGraph`. |

## 6. Banco de dados

**Banco de dados alterado: NÃO.**

> **NÃO ALTEREI O BANCO DE DADOS.**

Foram preservados sem modificação:

- `database/sos_artistas.sql`;
- `database/schema-test.sql`;
- `database/migration_rf03.sql`;
- `backend/src/test/resources/db/schema-test.sql`.

Os hashes antes e depois permaneceram idênticos. A configuração `spring.jpa.hibernate.ddl-auto=validate` também foi preservada, portanto a aplicação apenas valida o schema oficial.

## 7. Frontend

**Frontend alterado: NÃO.**

> **NÃO ALTEREI O FRONTEND.**

Os hashes de todos os arquivos do frontend permaneceram idênticos. Como o escopo solicitado é backend, ficaram registradas, sem implementação, as seguintes pendências visuais/de integração:

- consumir `GET /api/vagas/{id}/similares` no carrossel de vagas relacionadas; atualmente o frontend usa uma listagem genérica;
- implementar ou comprovar rolagem infinita do feed usando `nextCursor` e `hasMore`;
- exibir o botão de retorno ao topo após mais de 300 px de rolagem;
- se a interface também mostrar canceladas no feed, consumir o cursor independente dessa seção.

## 8. Segurança e privacidade

- O feed e as recomendações públicas não dependem de IDs de usuário fornecidos pelo cliente.
- A exceção das canceladas depende exclusivamente do artista do JWT e de candidaturas persistidas.
- Um artista não consegue visualizar canceladas vinculadas a outro artista.
- O indicador de vaga própria é calculado no servidor e não concede autorização por si só.
- Os filtros usam Criteria API/JPQL com parâmetros, reduzindo risco de injeção.
- A resposta pública não inclui senha, refresh token, e-mail, telefone ou data de nascimento.

## 9. Desempenho

- A paginação é executada no banco, não em memória.
- O cursor por ID evita o custo crescente típico de offsets altos e assegura continuidade estável.
- A consulta lê no máximo `size + 1` IDs para calcular `hasMore`.
- As associações necessárias são carregadas em lote depois da seleção paginada de IDs, evitando N+1 e o problema de paginar um `join fetch` de coleção.
- O limite máximo de 50 itens foi mantido para proteger o endpoint.
- Nenhuma busca ampla `findAll()` foi introduzida no fluxo do RF03.

## 10. Testes executados

### Baseline antes das alterações

- Comando: `.\\mvnw.cmd '-Dspring.jpa.show-sql=false' test`
- Resultado válido após disponibilização do Docker: **102 testes**, 0 falhas, 0 erros, 0 ignorados, `BUILD SUCCESS`.
- A primeira tentativa não iniciou os testes de integração porque o daemon do Docker estava parado; isso foi uma indisponibilidade de infraestrutura, não uma falha funcional. O baseline válido foi obtido antes das edições.

### Novos testes do RF03

Foram adicionados **9 métodos de teste**, levando a classe RF03 a **27 execuções aprovadas**. Os novos cenários cobrem:

1. cada filtro compatível e a combinação entre filtros;
2. omissão de filtros;
3. tamanho padrão, limite, última página e ausência de duplicação;
4. filtros e cursores inválidos;
5. cursor independente e isolamento das canceladas;
6. similares por tags, somente abertas e sem a origem;
7. origem sem tags e acesso público;
8. paginação de similares sem duplicação;
9. indicação de propriedade por JWT e privacidade para visitante.

### Regressão final

- Comando: `.\\mvnw.cmd '-Dspring.jpa.show-sql=false' test`
- Ambiente de integração: PostgreSQL 18.4 em Testcontainers.
- Resultado: **111 testes**, 0 falhas, 0 erros, 0 ignorados.
- Status: **BUILD SUCCESS**.
- Regressões relevantes aprovadas: RF06 (20), RF07 (15), RF08 (23), perfil completo (17), RF03 (27), validação de tamanho (8) e integração de usuário (1).

## 11. Conflitos e decisões técnicas

- O requisito chama o filtro de “área de atuação”, enquanto o schema oficial armazena o conceito em `vagas.categoria`. A API expõe `areaAtuacao`, mas consulta o campo já existente; nenhuma migração foi necessária.
- “Empresa” foi interpretada como `PerfilContratante.nomeEmpresa`, com fallback para `Usuario.nome`, cobrindo perfis ainda sem nome empresarial preenchido.
- A exceção de vagas canceladas permanece isolada do feed aberto, mas agora possui metadados próprios de cursor para não misturar dois conjuntos com progressões diferentes.
- A recomendação simples foi implementada por interseção de tags, sem criar algoritmo de ranking não solicitado.
- Requisitos de rolagem e botão de topo são responsabilidades visuais. Foram documentados, mas não justificam alteração de frontend em uma tarefa limitada ao backend.

## 12. Conclusão

**Classificação: RF03 CONCLUÍDO NO ESCOPO DE BACKEND.**

Há evidência automatizada para listagem pública somente de vagas abertas, filtros obrigatórios, filtros opcionais, combinação de filtros, paginação por cursor, ordenação estável, proteção das vagas canceladas, recomendações por tags, ausência de duplicação, privacidade e regressão dos RF06, RF07 e RF08.

O RF03 completo na experiência do usuário ainda depende das integrações visuais registradas na seção 7. Elas não foram implementadas porque o frontend foi explicitamente preservado.

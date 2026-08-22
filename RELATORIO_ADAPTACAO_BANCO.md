# Relatório de Adaptação do Projeto Palco ao Banco Oficial

Data da validação: 15/08/2026

## 1. Estado inicial

### Papel dos arquivos SQL

| Arquivo | Papel confirmado |
|---|---|
| `sos_artistas.sql` | Schema principal. Cria enums, tabelas, constraints, relacionamentos, defaults e o índice de `refresh_tokens`. |
| `migration_rf03.sql` | Migration complementar do RF03. Acrescenta `cancelada`, garante os estados complementares de candidatura, campos de vaga/contratante, `fotos_vaga` e índices de busca. |
| `schema-test.sql` | Schema integral usado pelos testes PostgreSQL. Contém o schema principal e aplica ao final a parte complementar de cancelamento/índices necessária aos testes. |

A interpretação esperada no pedido foi confirmada. As cópias no projeto e os arquivos oficiais anexados possuem conteúdo idêntico após normalizar somente as quebras de linha CRLF/LF.

### Mapa da implementação atualmente existente

| Tabela SQL | Entity | Repository | Service | Controller/DTO | Frontend consumidor |
|---|---|---|---|---|---|
| `usuarios` | `Usuario` | `UsuarioRepository` | `AuthService`, `UsuarioService` | `AuthController`, `UsuarioController`, DTOs de cadastro/login/usuário | `cadastro-contratante.html`, `login.html`, `perfil.html`, `main.js` |
| `perfis_artistas` | `PerfilArtista` | `PerfilArtistaRepository` | `PerfilArtistaService` | `PerfilArtistaController`, DTOs de perfil | `perfil.html`, `main.js` |
| `perfis_contratantes` | `PerfilContratante` | `PerfilContratanteRepository` | `PerfilContratanteService` | `PerfilContratanteController`, DTOs de perfil | cadastro, perfil, dashboard e publicação de vaga |
| `tags`, `tags_artista` | `Tag`, `PerfilArtista.tags` | `TagRepository` | `TagService`, `PerfilArtistaService` | `TagController`, DTOs de tag/perfil | perfil e publicação de vaga |
| `vagas`, `tags_vaga`, `fotos_vaga` | `Vaga` | `VagaRepository` | `VagaService` | `VagaController`, DTOs de vaga | publicação, listagem, detalhe, edição e dashboard |
| `candidaturas` | `Candidatura` | `CandidaturaRepository` | `CandidaturaService` | `CandidaturaController`, DTOs de candidatura | dashboard e fluxo de candidatura |
| `log_vagas_canceladas` | `LogVagaCancelada` | `LogVagaCanceladaRepository` | `VagaService` | cancelamento por `VagaController` | confirmação e listagem de vagas |
| `refresh_tokens` | `RefreshToken` | `RefreshTokenRepository` | `RefreshTokenService`, `AuthService` | endpoints de refresh/logout | login/logout e painel de testes |

As demais tabelas do schema pertencem a módulos futuros ainda não implementados. Elas não precisaram de Entities vazias apenas para satisfazer o Hibernate: `ddl-auto=validate` valida corretamente as entidades mapeadas e preserva as tabelas adicionais do banco.

### Incompatibilidades e regressões encontradas

1. O cadastro inicial de artista persistia `NULL` em `nivel_medalha`, `score_engajamento` e `ultima_atualizacao`, anulando na prática os defaults definidos em `perfis_artistas`.
2. O perfil de artista não atualizava `usuarios.perfil_completo`; por isso um artista não conseguia cumprir o pré-requisito normal da candidatura.
3. A criação de candidatura aceitava `artistaId` e `status` enviados pelo cliente, não exigia vaga `ABERTA` nem perfil completo e permitia criar candidatura em nome de outro artista.
4. Os DTOs não antecipavam vários limites físicos `varchar`, `numeric(10,2)`, `numeric(5,2)` e `check (nivel_medalha between 1 and 5)`. Entradas inválidas poderiam chegar ao PostgreSQL como erro de integridade.
5. O cadastro convencional não rejeitava idade inferior a 14 anos ou data futura, contrariando a decisão consolidada do RF01.
6. O Maven Wrapper falhava antes de iniciar o Maven ao indexar como array uma propriedade `Target` nula em diretórios normais do Windows.
7. A classe `UsuarioControllerIT` não entrava no comando padrão `mvn test` por causa do padrão de nomes do Surefire.

### Entidades, endpoints e telas afetados

- Entidade diretamente ajustada: `PerfilArtista`.
- Entidades validadas sem necessidade de alteração estrutural: `Usuario`, `PerfilContratante`, `Tag`, `Vaga`, `Candidatura`, `LogVagaCancelada` e `RefreshToken`.
- Endpoints com comportamento reforçado: cadastro convencional/Google, atualização de perfil de artista, criação de candidatura e respostas HTTP 422.
- Todos os endpoints que recebem os DTOs ajustados agora rejeitam limites incompatíveis antes da operação SQL.
- Nenhuma tela precisou ser alterada; o contrato JSON atual foi preservado.

## 2. Alterações no backend

| Arquivo | Alteração | Motivo / relação com banco e requisitos |
|---|---|---|
| `backend/mvnw.cmd` | Tratamento seguro de diretório Maven sem `Target`. | Permitir build/test reproduzível no Windows. |
| `model/PerfilArtista.java` | `@PrePersist` para medalha 1, score 0,00 e timestamp. | Respeitar defaults de `perfis_artistas` e RF29 sem gravar `NULL`. |
| `service/AuthService.java` | Idade mínima 14, data futura inválida e validação dos tamanhos dos dados Google. | RF01 e limites de `usuarios` (`nome`, `email`, `google_id`, `foto_perfil`). |
| `service/PerfilArtistaService.java` | Preserva medalha/score quando omitidos, atualiza `perfil_completo` e exige o próprio usuário nas mutações. | RF08/RF06 e relação 1:1 `usuarios` ↔ `perfis_artistas`. |
| `service/CandidaturaService.java` | Identidade derivada do JWT, vaga obrigatoriamente `ABERTA`, perfil completo, status inicial sempre `PENDENTE` e bloqueio de representação de outro artista. | RF06; FKs `vaga_id`/`artista_id`; enums oficiais de vaga/candidatura. |
| `exception/UnprocessableEntityException.java` | Nova exceção de domínio HTTP 422. | Retorno previsto pelo RF06 para vaga indisponível/perfil incompleto. |
| `exception/ApiExceptionHandler.java` | Mapeamento da nova exceção para 422 no formato padrão da API. | Preserva contrato de erro sem mascarar falhas. |
| `dto/CadastroRequest.java` | Limites para nome, telefone, e-mails, senha, perfil e responsável. | Colunas `usuarios` e RF01. |
| `dto/GoogleAuthRequest.java` | Limite de telefone. | `usuarios.telefone varchar(20)`. |
| `dto/UsuarioRequest.java` | Limites de todas as strings persistidas. | Colunas `usuarios`. |
| `dto/UsuarioAtualizacaoRequest.java` | Limites de nome, telefone, e-mail e senha. | Colunas `usuarios`. |
| `dto/PerfilArtistaRequest.java` | Limites de localização/URLs, medalha 1–5 e `numeric(5,2)`. | `perfis_artistas` e RF29. |
| `dto/PerfilContratanteRequest.java` | Limites de empresa, tipo, localização e banner. | `perfis_contratantes`. |
| `dto/VagaRequest.java` | Limites de colunas, UF com 2 caracteres, remuneração `numeric(10,2)` e URLs de foto com 500 caracteres. | `vagas` e `fotos_vaga`; RF04/RF07. |
| `dto/CandidaturaRequest.java` | Mensagem até 2000 e link até 255 caracteres. | `candidaturas` e RF06. |
| `dto/TagRequest.java` | Nome até 50 caracteres. | `tags.nome varchar(50)`. |
| `test/.../VagaControllerRf03IntegrationTest.java` | Casos para defaults do artista, regras/identidade da candidatura, status pendente e conclusão segura do perfil. | Validação real em PostgreSQL dos RF03, RF06, RF08 e RF25. |
| `test/.../UsuarioControllerIT.java` | Classe renomeada internamente para terminar em `Test`. | Inclusão automática no `mvn test`; cobre cadastro, login, edição e exclusão. |

## 3. Alterações no frontend

> Nenhuma alteração no frontend foi necessária.

O backend manteve os nomes e formatos JSON consumidos por `frontend/public/js/main.js`. Não houve redesign, alteração visual, textual ou de navegação.

## 4. Banco de dados

> O schema oficial do banco de dados não foi alterado.

- Nenhuma tabela, coluna, constraint, enum, índice ou relacionamento foi modificado.
- `database/sos_artistas.sql`, `database/migration_rf03.sql`, `database/schema-test.sql` e `backend/src/test/resources/db/schema-test.sql` permanecem equivalentes aos anexos oficiais.
- Não foi encontrada incompatibilidade tecnicamente impossível de resolver no backend.
- Os avisos `already exists, skipping` da migration são esperados, pois o schema principal oficial já contém alguns campos/tabelas que a migration garante de forma idempotente.

## 5. Compatibilidade

| Módulo | Resultado |
|---|---|
| Autenticação JWT e BCrypt | Validado por integração e smoke test. |
| Refresh tokens | Geração, renovação, logout e rejeição após logout validados. |
| Usuários | Cadastro, consulta `/me`, atualização e exclusão validados. |
| Perfis de contratante | Atualização e vínculo 1:1 validados. |
| Perfis de artista | Defaults, atualização, `perfil_completo`, tags e autorização validados. |
| Vagas | Create, read paginado, filtros, detalhe, update e delete lógico validados. |
| Candidaturas | Criação pendente, perfil completo, identidade, duplicidade e cancelamento por vaga validados. |
| Tags | Criação e associações N:N com artista/vaga validadas. |
| Fotos de vaga | Persistência e ordem em `fotos_vaga` validadas. |
| Dashboard do contratante | Contratos de `/vagas/minhas` e `/candidaturas/minhas-vagas` preservados; validação visual não executada. |
| Módulos futuros do schema | Não foi possível confirmar porque ainda não há implementação correspondente, e implementá-los não fazia parte desta tarefa. |

## 6. Testes

### Build e suíte automatizada

- Comando final: `mvn test`.
- Resultado: **BUILD SUCCESS**.
- Total: **27 testes aprovados, 0 falhas, 0 erros, 0 ignorados**.
- Distribuição:
  - 18 testes de integração RF03/vagas/perfis/candidaturas em PostgreSQL 18;
  - 8 testes unitários de normalização da paginação;
  - 1 teste de integração de usuário com servidor HTTP real.
- `schema-test.sql` foi executado em PostgreSQL 18.4 via Testcontainers.
- O Hibernate iniciou com `spring.jpa.hibernate.ddl-auto=validate`; não foi usado `create`, não houve supressão de erro de mapeamento.

### Validação com os scripts de produção

Foi criado um PostgreSQL 18 temporário isolado com:

1. `sos_artistas.sql`;
2. `migration_rf03.sql`;
3. aplicação iniciada contra esse banco com `ddl-auto=validate`;
4. smoke tests HTTP de ponta a ponta.

Fluxos aprovados no smoke test:

- cadastro e login de contratante;
- `/usuarios/me`;
- perfil de contratante;
- criação de tag;
- publicação, feed público, edição e cancelamento de vaga;
- cadastro e login de artista;
- conclusão de perfil de artista;
- candidatura criada como `PENDENTE` mesmo quando o cliente tentou enviar outro status;
- candidatura convertida em `CANCELADA_POR_VAGA` no cancelamento;
- vaga cancelada visível somente na seção do artista candidato;
- refresh token, logout e rejeição do refresh invalidado;
- exclusão de conta.

O backend e o PostgreSQL temporários foram encerrados e removidos. O contêiner preexistente `palco-postgres` não foi alterado nem removido.

### Frontend

- Não foi executado `npm build`/`npm test` porque Node.js e npm não estão instalados/disponíveis neste ambiente.
- Como nenhum arquivo do frontend foi modificado, a compatibilidade foi verificada pelo contrato estático de `main.js` e pelos smoke tests da API consumida pelas telas.

## 7. Pendências

1. **Conflito documental de enum de candidatura:** o banco oficial usa `aprovado`/`rejeitado`, enquanto a decisão consolidada do RF06 menciona `ACEITA`/`REJEITADA`. Pela prioridade definida, o backend mantém `APROVADO`/`REJEITADO`. A documentação deve ser alinhada em tarefa separada; o banco não foi alterado.
2. **Autorização dos demais endpoints de candidatura:** a criação foi protegida, mas a granularidade de consulta, atualização e remoção ainda merece uma tarefa funcional própria para definir transições por artista/contratante sem ampliar silenciosamente o escopo atual.
3. **Listagens genéricas fora de vagas:** alguns módulos existentes ainda utilizam `findAll()` (tags/perfis/candidaturas). Não impedem a compatibilidade com o schema, mas devem receber paginação quando os respectivos RFs forem implementados.
4. **Login Google real:** a validação estrutural e os limites do schema foram compilados, porém não foi possível confirmar um login real sem um ID Token externo válido.
5. **Build visual do frontend:** não foi possível confirmar neste ambiente por ausência de Node/npm.

## Resumo de alterações

### BACKEND ALTERADO

- `backend/mvnw.cmd`;
- `PerfilArtista.java`;
- `AuthService.java`;
- `PerfilArtistaService.java`;
- `CandidaturaService.java`;
- `UnprocessableEntityException.java`;
- `ApiExceptionHandler.java`;
- DTOs de cadastro, Google, usuário, perfis, vaga, candidatura e tag;
- testes de vagas/RF03 e usuário.

### FRONTEND ALTERADO

nenhum.

### BANCO ALTERADO

nenhum.

### TESTES

27 aprovados; 0 falhas; 0 erros; 0 ignorados. Build e inicialização contra PostgreSQL 18 aprovados. Smoke test completo aprovado.

### PENDÊNCIAS

Alinhar o nome documental dos status de candidatura, detalhar autorização/transições dos endpoints restantes de candidatura, paginar listagens genéricas, validar OAuth Google com credencial externa e executar o build visual do frontend em ambiente com Node/npm.

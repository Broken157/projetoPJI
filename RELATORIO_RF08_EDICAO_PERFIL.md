# RELATÓRIO — RF08: EDIÇÃO DE PERFIL

## 1. Estado antes da tarefa

O projeto já possuía os endpoints `GET/PUT/DELETE /api/usuarios/me`, as rotas legadas de usuário por ID, `GET/POST/PUT/DELETE /api/perfis-artistas`, `GET/POST/PUT/DELETE /api/perfis-contratantes`, autenticação JWT, login convencional/Google, refresh token, logout e catálogo de tags.

Já estavam corretos ou aproveitáveis:

- entidades e relacionamentos de `Usuario`, `PerfilArtista`, `PerfilContratante`, `Tag` e `tags_artista` compatíveis com o banco oficial;
- autenticação JWT e resolução do usuário atual pelo contexto autenticado;
- hashes BCrypt no cadastro/login;
- infraestrutura de refresh tokens com operação para invalidar todos os tokens de um usuário;
- transações nos Services de perfil;
- limites físicos de parte dos campos nos DTOs;
- regra do RF06 que exige artista com `perfil_completo = true` para candidatar-se;
- suporte existente a URL de banner, URL de portfólio, tags e avatar/foto já persistidos no modelo.

Estavam parciais:

- `PerfilArtistaService` recalculava completude somente com biografia e localização;
- `PerfilContratanteService` usava somente biografia e localização, sem conferir o cadastro;
- mutações de perfil verificavam parcialmente propriedade, mas não bloqueavam todos os casos de tipo cruzado e divergência entre ID da rota e do payload;
- edição de usuário permitia alterar data de nascimento e trocar senha sem validar a senha atual;
- DTOs possuíam limites, mas faltava validação server-side de URLs.

Faltavam:

- regra central e testável de completude para os dois tipos;
- recálculo completo nos dois sentidos (`false → true` e `true → false`);
- portfólio e ao menos uma tag na regra do artista;
- credencial local ou Google e campos obrigatórios de cadastro na regra;
- proteção contra manipulação de `perfilCompleto`, tipo, medalha e score;
- revogação dos refresh tokens após troca de senha;
- respostas públicas de usuário sem dados privados;
- testes específicos e integrados do RF08;
- campos indispensáveis na tela existente para senha atual, portfólio e tags.

## 2. Problemas encontrados

### Autenticação

- As rotas eram protegidas por JWT, mas o Spring Security respondia `403` também quando não havia autenticação. O ponto global foi configurado para responder `401` nesses casos; usuários autenticados sem autorização continuam recebendo `403`.

### Autorização

- O tipo do usuário autenticado não era validado em todas as mutações de perfil.
- Medalha e score podiam ser recebidos no DTO e alterados pelo próprio artista, apesar de não pertencerem ao RF08.

### Propriedade

- Rotas genéricas `PUT/DELETE /api/usuarios/{id}` não restringiam a mutação ao próprio usuário.
- Era possível enviar no payload um `usuarioId` diferente do ID da rota.
- A criação de perfil precisava bloquear criação de perfil ARTISTA por CONTRATANTE e vice-versa.

### Validações

- URLs de portfólio e banner não tinham validação de formato no servidor.
- O e-mail precisava manter unicidade também durante a edição.
- Data de nascimento estava exposta como editável, embora não faça parte dos campos editáveis do RF08.

### Cálculo de `perfil_completo`

- Artista ignorava cadastro, credencial, portfólio e tags.
- Contratante ignorava cadastro e credencial.
- A regra não estava centralizada.
- Campos enviados pelo cliente podiam influenciar indevidamente o estado persistido.

### Senha

- Nova senha não exigia senha atual.
- Não havia validação com `BCryptPasswordEncoder.matches`.
- A alteração não encerrava sessões revogáveis.
- Conta exclusivamente Google poderia receber senha local por um fluxo não oficial.

### Tags

- A resolução de IDs existentes já era parcialmente aproveitável, mas faltavam testes de atomicidade, ID inexistente, remoção da última tag e duplicidade.

### Transações

- Os Services já tinham transações, mas o recálculo precisava ocorrer dentro da mesma operação, somente depois do estado final do perfil e das tags.

### Segurança e privacidade

- Listagem e consulta de outro usuário retornavam e-mail, telefone, data de nascimento e dados do responsável.
- Rotas legadas aceitavam campos protegidos como tipo, data de nascimento, senha e `perfilCompleto`.

### Integração com RF06

- Como a flag era calculada de forma incompleta, a candidatura podia ser liberada sem todos os requisitos oficiais do artista.

## 3. Alterações realizadas

| Arquivo | Alteração | Motivo | Regra atendida |
| --- | --- | --- | --- |
| `backend/src/main/java/com/portifolio/service/PerfilCompletoService.java` | Novo serviço central para calcular e persistir completude e timestamp | Eliminar regras divergentes e cálculo pelo cliente | RF08, RF06 |
| `backend/src/main/java/com/portifolio/service/PerfilArtistaService.java` | Propriedade/tipo, ID da rota versus payload, tags existentes e únicas, recálculo transacional; medalha/score ignorados | Bloquear IDOR, tipo cruzado e manipulação de campos protegidos | RF08 |
| `backend/src/main/java/com/portifolio/service/PerfilContratanteService.java` | Propriedade/tipo, ID da rota versus payload e recálculo transacional | Bloquear IDOR e aplicar regra própria do contratante | RF08 |
| `backend/src/main/java/com/portifolio/service/UsuarioService.java` | Edição segura de nome/telefone/e-mail, nascimento imutável, senha atual com BCrypt, revogação de refresh tokens, recálculo e responses públicas sanitizadas | Completar dados cadastrais, senha e privacidade | RF08, RF33 |
| `backend/src/main/java/com/portifolio/dto/UsuarioAtualizacaoRequest.java` | Inclusão de `senhaAtual`; nascimento legado aceito no JSON, porém ignorado pelo Service | Preservar contrato sem permitir alteração indevida | RF08 |
| `backend/src/main/java/com/portifolio/dto/PerfilArtistaRequest.java` | Validação de URLs e IDs de tags não nulos | Validação server-side | RF08 |
| `backend/src/main/java/com/portifolio/dto/PerfilContratanteRequest.java` | Validação da URL de banner | Validação server-side | RF08 |
| `backend/src/main/java/com/portifolio/config/SecurityConfig.java` | `AuthenticationEntryPoint` com `401` para requisição não autenticada | Distinguir autenticação de autorização | RF08 |
| `frontend/public/perfil.html` | Nascimento somente leitura; campos de senha atual, portfólio, tags e banner | Tornar utilizáveis os fluxos seguros e os requisitos do artista | RF08 |
| `frontend/public/js/main.js` | Carrega catálogo/tags atuais, envia portfólio/banner/tags e senha atual; não envia medalha/score | Compatibilizar a tela existente com o contrato seguro | RF08 |
| `backend/src/test/java/com/portifolio/service/PerfilCompletoServiceTest.java` | 17 testes unitários de cálculo | Cobrir campos obrigatórios, opcionais, blanks e credenciais | RF08 |
| `backend/src/test/java/com/portifolio/controller/PerfilEdicaoRf08IntegrationTest.java` | 18 testes de integração com PostgreSQL/Testcontainers | Cobrir endpoints, persistência, propriedade, senha, tags, transações e RF06 | RF08, RF06 |
| `backend/src/test/java/com/portifolio/controller/VagaControllerRf03IntegrationTest.java` | Fixture de perfil completo passou a possuir portfólio e tag; expectativa não autenticada atualizada para `401` | Manter o teste anterior alinhado à regra oficial, sem removê-lo ou enfraquecê-lo | RF03, RF08 |

Nenhum endpoint paralelo foi criado e as URLs/métodos existentes foram preservados.

## 4. Regra final de `perfil_completo`

### ARTISTA

O valor é `true` somente quando todas as condições são verdadeiras:

```text
nome preenchido
AND dataNascimento presente
AND telefone preenchido
AND email preenchido
AND (senha local preenchida OR googleId preenchido)
AND biografia preenchida
AND localização preenchida
AND urlPortfolio preenchida
AND quantidade de tags >= 1
```

`bannerUrl`, foto, nível de medalha e score não participam do cálculo.

### CONTRATANTE

O valor é `true` somente quando:

```text
nome preenchido
AND dataNascimento presente
AND telefone preenchido
AND email preenchido
AND (senha local preenchida OR googleId preenchido)
AND biografia preenchida
AND localização preenchida
```

`nomeEmpresa`, `tipoPerfil`, `bannerUrl` e foto não participam do cálculo. Portanto, pessoa física pode ter perfil completo sem empresa.

O backend ignora qualquer tentativa de definir manualmente `perfilCompleto`. O recálculo usa as entidades persistidas e funciona nos dois sentidos. Ao remover a última tag de um artista completo, a flag volta imediatamente para `false`.

### `ultima_atualizacao`

Foi preservada a semântica anterior de registrar atualização normal do perfil de artista. Além disso, o serviço central garante timestamp na transição `false → true`. Nenhuma coluna foi criada.

## 5. Autorização final

| Recurso | Quem pode editar | Identidade usada | Proteções |
| --- | --- | --- | --- |
| `/api/usuarios/me` | usuário autenticado | JWT/contexto por `AuthenticatedUserResolver` | não recebe ID do cliente |
| `/api/usuarios/{id}` | somente o próprio usuário | JWT comparado ao ID da rota | campos protegidos e troca de senha bloqueados na rota legada |
| `/api/perfis-artistas/{id}` | o próprio usuário do tipo ARTISTA | JWT comparado ao ID da rota e ao `usuarioId` do payload | bloqueia outro artista e tipo cruzado |
| `/api/perfis-contratantes/{id}` | o próprio usuário do tipo CONTRATANTE | JWT comparado ao ID da rota e ao `usuarioId` do payload | bloqueia outro contratante e tipo cruzado |

- Sem autenticação: `401`.
- Autenticado sem permissão/propriedade/tipo: `403`.
- Recurso realmente inexistente: `404`.
- E-mail duplicado: `409`.
- Regra de negócio inválida: `422`, conforme o padrão existente.

## 6. Troca de senha e sessões

- Se `novaSenha` estiver ausente ou blank, o hash atual permanece inalterado.
- Conta local exige `senhaAtual`.
- A senha atual é verificada exclusivamente por `BCryptPasswordEncoder.matches`.
- A nova senha é validada pelo DTO e persistida somente após `passwordEncoder.encode`.
- Senha e hash não fazem parte de nenhuma response.
- Conta exclusivamente Google recebe `422` e não ganha senha local por esse fluxo.
- Após a troca, `RefreshTokenService.invalidarTodosDoUsuario` marca todos os refresh tokens daquele usuário como inativos dentro da transação.

### Limitação dos Access Tokens já emitidos

O mecanismo atual revoga refresh tokens, mas JWTs de acesso já emitidos são autocontidos e não consultam uma blacklist ou versão de sessão. A configuração atual é `jwt.expiration=86400000`, portanto um Access Token antigo pode permanecer válido por até 24 horas após a troca de senha.

Uma invalidação imediata futura exigiria conferir, em cada requisição, uma versão de credencial/sessão do usuário ou uma blacklist de tokens. Isso requer decisão arquitetural e provavelmente suporte estrutural/persistente; não foi introduzido nesta tarefa porque migrations e tabelas novas são proibidas.

## 7. Frontend

```text
Frontend alterado: SIM
```

| Arquivo | Alteração | Motivo | Alternativa backend considerada |
| --- | --- | --- | --- |
| `frontend/public/perfil.html` | Nascimento readonly; senha atual; URL de portfólio; seletor múltiplo de tags; banner | A tela não conseguia executar troca segura de senha nem preencher os requisitos oficiais do artista | Aceitar troca sem senha atual seria inseguro; inventar portfólio/tag no backend violaria o RF08 |
| `frontend/public/js/main.js` | Carrega e seleciona tags existentes, envia os campos editáveis e limpa ambas as senhas | Preservar o fluxo existente com o contrato seguro | Preservar silenciosamente campos ocultos não permite completar um novo perfil e não oferece controle ao usuário |

Não houve redesign, alteração de CSS, nova rota ou mudança na estrutura geral da tela. O frontend não foi alterado por preferência: os dois ajustes eram necessários para tornar o RF08 utilizável sem enfraquecer o backend.

Não foi possível executar `npm build` porque Node/npm não estão instalados/disponíveis neste ambiente. A validação funcional do contrato foi feita pelos testes HTTP/backend.

## 8. Banco

```text
Banco alterado: NÃO
```

Não foram alterados:

- tabelas;
- colunas;
- tipos ou enums;
- constraints;
- relacionamentos;
- índices;
- migrations;
- scripts SQL.

Confirmações individuais:

- `database/sos_artistas.sql`: conteúdo normalizado idêntico ao anexo oficial;
- `database/schema-test.sql`: idêntico ao anexo oficial;
- `database/migration_rf03.sql`: conteúdo normalizado idêntico ao anexo oficial;
- diferenças de hash bruto em dois arquivos decorrem somente de finais de linha;
- `spring.jpa.hibernate.ddl-auto=validate` foi mantido;
- nenhum `create` ou `create-drop` foi introduzido.

## 9. Testes

### Resultado final

| Métrica | Resultado |
| --- | ---: |
| Testes existentes antes | 47 |
| Novos testes RF08 | 35 |
| Total final | 82 |
| Aprovados | 82 |
| Falhas | 0 |
| Erros | 0 |
| Ignorados | 0 |
| Resultado Maven | `BUILD SUCCESS` |

Comando final executado:

```bash
mvn test
```

Os 35 novos testes são 17 unitários e 18 de integração. A cobertura inclui os 40 grupos mínimos solicitados por meio de casos parametrizados e múltiplas asserções: blanks/nulos, campos opcionais, propriedade, tipo cruzado, IDs divergentes, tag inexistente, duplicidade, rollback, e-mail duplicado, limites, nascimento imutável, privacidade, BCrypt, senha antiga/nova, conta Google, revogação de refresh token e regressão RF06.

As integrações utilizaram PostgreSQL 18 real via Testcontainers, `database/schema-test.sql` oficial e `ddl-auto=validate`.

### Smoke test HTTP real

Foi iniciado um backend real na porta isolada `58088`, ligado a um PostgreSQL 18 descartável inicializado pelo schema oficial. Resultado:

- cadastro: `201`;
- login: `200`;
- edição do perfil de artista: `200`;
- após portfólio + uma tag: `perfilCompleto=true`;
- após remover a última tag: `perfilCompleto=false`;
- troca de senha: `200`;
- login com senha nova: `200`;
- refresh tokens ativos após troca: `0`.

O contêiner temporário `palco-rf08-smoke` foi parado e removido ao final. Nenhum contêiner ou banco já existente foi alterado.

## 10. Pendências e dependências

### RF16 — Foto de perfil

O modelo e o `AvatarService` preservam URL/fallback de avatar, mas não existe upload seguro completo de arquivos. Implementar armazenamento, validação e processamento pertence ao RF16 e não foi criado artificialmente no RF08.

### RF23 — Tags

O RF08 permite selecionar apenas tags já existentes. Administração/criação do catálogo de tags continua fora deste escopo; IDs desconhecidos retornam `404` e não geram criação silenciosa.

### RF35 — Verificação de e-mail

O schema/modelo atual não possui status confirmado de e-mail ou fluxo seguro de reverificação. A edição de e-mail mantém unicidade, mas não implementa RF35 nem ativa conta automaticamente. Quando RF35 existir, a mudança de e-mail deverá disparar a política oficial de reverificação.

### RF36 — Consentimento de responsável

Dados existentes do responsável são preservados e não são expostos publicamente. Data de nascimento não pode ser alterada pelo RF08. Não há status de consentimento no modelo atual; nenhum fluxo paralelo foi inventado.

### Access Token

Access Tokens já emitidos podem durar até 24 horas após a troca de senha. Refresh tokens são revogados imediatamente. A solução completa depende da decisão arquitetural descrita na seção 6.

### Ferramenta de frontend

Node/npm não estavam disponíveis para executar o build do frontend neste ambiente. Não há pendência conhecida de contrato, mas o build deve ser executado quando o runtime estiver disponível.

## 11. Conclusão

```text
CONCLUÍDO
```

O RF08 foi auditado antes das alterações, finalizado sobre o banco oficial sem mudança estrutural, protegido contra IDOR e manipulação de campos, integrado ao RF06 e validado por 82 testes aprovados mais smoke test HTTP real. As limitações registradas pertencem a RF16, RF23, RF35, RF36 ou à estratégia futura de revogação imediata de Access Tokens e não impedem o comportamento obrigatório do RF08.

# REGISTRO PARA RELATÓRIO SEMANAL

## Data

15/08/2026

## Objetivo da tarefa

Finalização do RF08 — Edição de Perfil.

## RF/RNF trabalhados

- RF08 — edição de cadastro e perfis, completude, senha, autorização e privacidade;
- RF06 — regressão da elegibilidade para candidatura;
- RF33 — revogação dos refresh tokens após troca de senha;
- RF34 — preservação da resolução atual de avatar;
- dependências auditadas: RF16, RF23, RF35 e RF36.

## Backend alterado

SIM. Regra central de `perfil_completo`, edição segura de usuário, perfis de artista/contratante, validação de URLs, propriedade/tipo, senha com BCrypt, revogação de refresh tokens, privacidade de responses e status `401` para não autenticado.

## Frontend alterado

SIM. Alterações mínimas em `perfil.html` e `main.js` para senha atual, portfólio, tags, banner e nascimento somente leitura, sem redesign.

## Banco alterado

NÃO. Scripts, schema, migrations, tabelas, colunas, enums, constraints, índices e relacionamentos preservados.

## Funcionalidades concluídas ou avançadas

- edição segura de nome, telefone e e-mail;
- data de nascimento imutável;
- edição de perfis próprios com bloqueio de tipo cruzado;
- completude oficial de artista e contratante;
- tags existentes, únicas e transacionais;
- promoção e rebaixamento automático de `perfil_completo`;
- troca de senha com senha atual e BCrypt;
- revogação de sessões renováveis;
- respostas públicas sem dados privados;
- integração imediata com candidatura do RF06.

## Bugs encontrados e corrigidos

- completude parcial/incorreta;
- empresa indevidamente tratável como requisito;
- IDOR nas rotas legadas e perfis;
- senha alterável sem senha atual;
- refresh tokens mantidos após troca;
- medalha/score editáveis pelo artista;
- URLs sem validação server-side;
- dados privados em responses públicas;
- não autenticado retornando `403` em vez de `401`.

## Segurança

Identidade derivada do JWT, checagem de propriedade e tipo, senha atual validada com BCrypt, hashes nunca retornados, refresh tokens revogados, dados privados sanitizados e campos protegidos ignorados/bloqueados.

## Decisões técnicas

- centralizar completude em um único Service;
- manter todas as atualizações relacionadas dentro de transações;
- preservar rotas existentes e validar seus IDs;
- não alterar banco;
- não inventar tag, empresa, foto ou estado de ativação;
- registrar a limitação de JWT de acesso autocontido.

## Testes e resultados

47 testes anteriores + 35 novos = 82 aprovados, 0 falhas, 0 erros, 0 ignorados, `BUILD SUCCESS`. PostgreSQL 18/Testcontainers, schema oficial, `ddl-auto=validate` e smoke HTTP real aprovados.

## Pendências

- upload seguro de foto no RF16;
- administração do catálogo no RF23;
- reverificação de e-mail no RF35;
- consentimento formal no RF36;
- estratégia futura para invalidar imediatamente Access Tokens;
- executar build do frontend em ambiente com Node/npm.

## Próximos passos

1. Disponibilizar Node/npm e executar `npm build`/teste da tela de perfil.
2. Planejar RF16, RF35 e RF36 sem alterar retroativamente a regra de completude.
3. Avaliar versão de sessão/token para revogação imediata de JWTs de acesso.

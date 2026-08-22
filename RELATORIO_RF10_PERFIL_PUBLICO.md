# Relatório técnico — RF10: Visualização de Perfil Público

## 1. Objetivo

Auditar e finalizar o RF10 do PJI Palco com um perfil público seguro, compartilhável e acessível sem JWT para `ARTISTA` e `CONTRATANTE`, preservando banco, fluxos privados e requisitos futuros fora do MVP.

## 2. Estado inicial

### Existente

- entidades `Usuario`, `PerfilArtista`, `PerfilContratante` e `Tag`;
- edição privada de perfis do RF08;
- `ContratantePublicoResponse` e mapeamento público do contratante no RF05;
- `AvatarService` do RF34;
- campos `fotoPerfil`, `bannerUrl`, `urlPortfolio` e associação de tags;
- `spring.jpa.hibernate.ddl-auto=validate`;
- página privada `perfil.html`.

### Correto

- prioridade do avatar personalizado sobre a foto do usuário;
- fallback de `nomeEmpresa` para o nome do usuário no contrato público do RF05;
- endpoints de edição protegidos por autenticação e propriedade;
- DTOs privados separados por perfil.

### Parcial

- o fallback DiceBear era determinístico, mas usava o ID sequencial diretamente no seed;
- o banco possui `portfolio_arquivos` e `visualizacoes_perfil`, porém não há integração ativa de Entity/Repository/Service nem visibilidade pública por item;
- o banco não possui preferências públicas por campo nem consentimento confiável de publicação para menor.

### Ausente

- endpoint público específico de perfil;
- DTO público de artista;
- página pública direta e compartilhável;
- testes específicos do RF10.

Baseline real antes das alterações: **213 testes executados, 213 aprovados, 0 falhas, 0 erros, 0 ignorados, BUILD SUCCESS**.

## 3. Auditoria por camada

### Entity

As entidades existentes já forneciam os campos profissionais necessários. Não foram alteradas. `Usuario` também contém dados estritamente privados, razão pela qual não é serializado na rota pública. Não existe Entity ativa para portfólio ou analytics.

### DTO

`PerfilArtistaResponse` e `PerfilContratanteResponse` são contratos de fluxo privado e não foram reutilizados. `ContratantePublicoResponse`, consolidado no RF05, foi preservado e passou a implementar o marcador `PerfilPublicoResponse`. Foi criado somente `ArtistaPublicoResponse`, com whitelist explícita.

### Repository

Foram acrescentadas consultas públicas específicas com `@EntityGraph`: artista carrega `usuario` e `tags`; contratante carrega `usuario`. Nenhuma consulta é executada por tag.

### Service

`PerfilPublicoService` centraliza seleção por tipo, whitelist, regra de menor, perfil inexistente e mapeamento. `AvatarService` continua sendo a única origem da regra de avatar.

### Controller

Foi criado um único controller público em `/api/perfis/publicos`, sem duplicar endpoints por papel.

### Security

Somente `GET /api/perfis/publicos/*/*` recebeu `permitAll`. Os endpoints privados existentes permanecem autenticados.

### Frontend

`perfil.html` foi identificado como tela privada de edição e foi preservado. Como não havia página pública adequada, foi criada a página mínima `perfil-publico.html` usando os tokens, cores, tipografia e componentes existentes.

### Testes

Foram criados 12 testes de integração do endpoint, 3 testes unitários do avatar e 6 casos Jest compatíveis. A suíte Jest não executou por falha ambiental do npm; sintaxe e comportamento renderizado foram validados por caminhos independentes.

## 4. Contrato público

Endpoint:

```http
GET /api/perfis/publicos/{tipo}/{id}
```

- `tipo`: somente `ARTISTA` ou `CONTRATANTE`;
- `id`: `usuarioId` numérico já adotado pelo modelo;
- sem JWT: `200` para perfil adulto existente e publicável;
- tipo inválido: `400 Bad Request` no formato global;
- perfil inexistente, tipo divergente ou menor sem consentimento comprovável: `404 Not Found`.

O contrato público é formado apenas por DTOs de whitelist e nunca contém a Entity `Usuario` aninhada.

## 5. ARTISTA

Campos públicos retornados:

- `usuarioId`;
- `nomeExibicao`;
- `biografia`;
- `localizacao`;
- `urlPortfolio`;
- `bannerUrl`;
- `avatarUrl`;
- `tags`, com `id` e `nome`.

Adultos com `perfilCompleto=false` continuam consultáveis; opcionais ausentes não são inventados.

## 6. CONTRATANTE

Foi reutilizado `ContratantePublicoResponse`, com:

- `usuarioId`;
- `nomeExibicao`;
- `nomeEmpresa` opcional;
- `tipoPerfil`;
- `biografia`;
- `localizacao`;
- `bannerUrl`;
- `avatarUrl`.

Quando `nomeEmpresa` é nulo ou branco, `nomeExibicao` usa o nome do usuário, preservando o comportamento do RF05.

## 7. Privacidade

Não fazem parte do contrato JSON público:

- senha ou hash BCrypt;
- e-mail e telefone;
- data de nascimento ou idade exata;
- `googleId`;
- token/expiração de recuperação e refresh tokens;
- nome, telefone ou e-mail do responsável;
- endereço residencial;
- consentimentos, flags administrativas ou informações legais privadas;
- `perfilCompleto`;
- medalha, score ou timestamp interno.

O banco atual não possui preferências de visibilidade por campo; o MVP aplica uma whitelist fixa de informações profissionais públicas.

## 8. Menores

Não foi encontrada informação persistida e confiável que comprove autorização do responsável para publicar o perfil. A existência dos campos cadastrais do responsável não equivale a consentimento do RF36.

Por segurança, qualquer perfil associado a usuário com menos de 18 anos retorna `404`, sem distinguir perfil privado de perfil inexistente e sem expor dado pessoal. A publicação restrita após consentimento depende do RF36.

## 9. Avatar e banner

O `AvatarService` preserva a prioridade:

1. foto definida no perfil pelo RF08;
2. foto do usuário, inclusive origem Google;
3. fallback DiceBear.

O fallback agora usa SHA-256 de um namespace fixo de aplicação e do ID interno, produzindo seed hexadecimal determinístico e opaco. Não usa PII, segredo, persistência ou o ID sequencial em claro. `bannerUrl` é opcional; o frontend usa gradiente de marca quando não há banner seguro.

## 10. Tags

Somente tags efetivamente associadas ao artista são retornadas, ordenadas sem diferenciar maiúsculas/minúsculas. A consulta usa `EntityGraph` para `usuario` e `tags`. O teste com 20 tags limita a execução a no máximo 2 statements, impedindo crescimento de uma consulta por item.

## 11. Portfólio

Embora a tabela `portfolio_arquivos` exista no SQL, ela não possui modelo/backend ativo nem flag pública por item. Expor seus registros diretamente seria inseguro e incorporaria o RF16 ao RF10.

A galeria completa depende do RF16; o RF10 atual expõe o portfólio disponível na estrutura existente, por meio de `urlPortfolio`. Como não há lista de mídia, a paginação do RNF12 não se aplica. Não foram implementados upload, galeria, iframe ou embed do RF18.

## 12. Medalhas

> RF29 não foi implementado e medalhas/conquistas não fazem parte do RF10 MVP atual.

Campos legados de medalha e score foram preservados no banco/modelo, mas não aparecem no DTO, na página, na ordenação ou na regra de visibilidade.

## 13. URL pública

Formato implementado:

```text
perfil-publico.html?tipo=ARTISTA&id=123
perfil-publico.html?tipo=CONTRATANTE&id=123
```

A página lê os parâmetros da própria URL e consulta a API diretamente. Não depende de login, storage ou navegação anterior. O refresh foi validado no navegador mantendo URL, título e perfil.

## 14. Frontend

**Frontend alterado: SIM.**

Arquivos:

- `frontend/public/perfil-publico.html`: estrutura pública sem dados pessoais;
- `frontend/public/css/perfil-publico.css`: layout consistente, fallback visual, desktop e mobile;
- `frontend/public/js/perfil-publico.js`: busca sem JWT, estados, tabs, URL segura e renderização com `textContent`;
- `frontend/src/rf10-perfil-publico.test.js`: 6 casos Jest compatíveis.

A alteração foi necessária porque a única página de perfil existente era a edição privada, dependente de sessão e de endpoints privados. Essa tela e `main.js` permaneceram intactos.

QA no navegador integrado:

- artista e contratante renderizados em URL direta;
- refresh preservou o perfil;
- avatar, banner e tags carregaram;
- aba Portfólio alterou painel sem reload e sem mudança da URL;
- 404 apresentou estado seguro;
- nenhum dado privado apareceu no DOM;
- desktop e viewport 390 x 844 sem overflow horizontal (`scrollWidth = clientWidth = 390`);
- zero mensagens relevantes de erro ou warning no console.

## 15. Banco

**Banco alterado: NÃO.**

Nenhuma Entity, migration, tabela, coluna, enum, constraint, índice, FK ou SQL foi modificado. `ddl-auto=validate` foi preservado.

Hashes SHA-256 finais iguais ao baseline:

| Arquivo protegido | SHA-256 |
|---|---|
| `database/sos_artistas.sql` | `1711CE1FBDEDB3BA97BBACC7D820FBF7C31A78E87BFADF20FB7FE2967E0666AD` |
| `database/schema-test.sql` | `202C34E7BABE0300C45FC0EF2ED96EC380A78780CED89FBDCBA7B2245CE79671` |
| `database/migration_rf03.sql` | `242A56CB887EB79E5F0EC2D09BF8592F473D8EC5288543063672BB558513249C` |
| `database/migration_rf25_motivo.sql` | `8920B5A1531E029B2AFE66C387A6A108AD9B326D1661E828EDFF53E7C9721B6C` |
| `backend/src/test/resources/db/schema-test.sql` | `202C34E7BABE0300C45FC0EF2ED96EC380A78780CED89FBDCBA7B2245CE79671` |

Também permaneceram iguais `application.properties`, `login.html`, `main.js` e `perfil.html`.

## 16. Segurança

- leitura pública permitida somente para o novo `GET`;
- whitelist por DTO, não filtragem improvisada no controller;
- perfil de menor ocultado com 404;
- tipo divergente não acessa a Entity errada;
- endpoints privados `GET/PUT /api/perfis-artistas/{id}` continuam retornando 401 sem JWT;
- conteúdo de usuário é inserido com `textContent`, sem `innerHTML`, `document.write` ou HTML vindo do banco;
- links externos aceitam apenas `http:`/`https:` e usam `target="_blank"` com `rel="noopener noreferrer"`;
- não foi criado log de negócio com e-mail, telefone, token, IP ou responsável.

## 17. Performance

O perfil público de artista é carregado por uma consulta específica com `EntityGraph(usuario, tags)`. O contratante usa `EntityGraph(usuario)`. O teste objetivo com 20 tags confirma teto de 2 statements, independentemente do número de tags. Não há lista de mídia ativa; logo, não há N+1 nem paginação aplicável ao portfólio atual.

## 18. Testes baseline

Comando:

```powershell
.\mvnw.cmd '-Dspring.jpa.show-sql=false' test
```

Resultado antes das alterações:

- executados: 213;
- aprovados: 213;
- falhas: 0;
- erros: 0;
- ignorados: 0;
- resultado: `BUILD SUCCESS`;
- duração: 01:32 min.

A primeira tentativa ocorreu com Docker Desktop indisponível e falhou na inicialização do Testcontainers. Após iniciar a infraestrutura, o baseline acima foi obtido; a falha inicial não foi classificada como regressão.

## 19. Novos testes RF10

Backend — 15 testes aprovados:

- 12 integrações: visitante/autenticado, artista, contratante, fallback de empresa, whitelist, adulto incompleto, menores, 404, tipo inválido, avatar, anti-N+1 e regressão de segurança;
- 3 unitários do avatar: prioridade da foto de perfil, prioridade da foto do usuário e fallback opaco/determinístico.

Frontend — 6 casos criados:

- URL direta e XSS como texto;
- aba sem navegação/requisição adicional;
- contratante sem Portfólio;
- 404;
- parâmetros inválidos sem chamada à API;
- bloqueio de sinks HTML e protocolos perigosos.

O Jest não foi declarado aprovado: `npm ci` falhou no contêiner com `Exit handler never called!`; a tentativa com outra versão do npm foi bloqueada por `UNABLE_TO_VERIFY_LEAF_SIGNATURE`. Não foi desativada a validação TLS. `node --check public/js/perfil-publico.js` passou e a validação funcional real no navegador passou.

## 20. Regressão final

Resultado final do Maven:

- executados: **228**;
- aprovados: **228**;
- falhas: **0**;
- erros: **0**;
- ignorados: **0**;
- resultado: **BUILD SUCCESS**;
- duração: **02:08 min**.

Foram preservadas as coberturas de RF03, RF04, RF05, RF06, RF07, RF08, RF09, RF25, RF31, autenticação/JWT, cadastro e perfis. Nenhum teste correto foi removido.

## 21. Conflitos encontrados

- **Medalhas:** o texto antigo do RF10 conflita com o adiamento do RF29; decisão: não implementar nem expor.
- **Privacidade individual:** não há flags por campo; decisão: whitelist profissional fixa, sem alterar banco.
- **Menores:** “qualquer visitante” conflita com privacidade por padrão; decisão posterior prevalece e perfil retorna 404 sem consentimento persistido.
- **Portfólio:** existe tabela futura, mas não integração/visibilidade ativa; decisão: somente `urlPortfolio`, sem incorporar RF16.
- **Analytics:** a tabela isolada não constitui infraestrutura operacional nem política de coleta; decisão: não registrar visualização improvisada.

## 22. Pendências/dependências futuras

- RF36: consentimento confiável do responsável e eventual whitelist pública restrita para menor;
- RF16: modelo, visibilidade, paginação e galeria pública de itens de portfólio;
- RF18: mídia externa, se futuramente aprovada;
- RNF09: analytics com governança e ausência de PII;
- infraestrutura frontend: corrigir cadeia TLS/npm para executar os 6 testes Jest criados.

Nenhuma dessas pendências bloqueia o núcleo atual do RF10.

## 23. Conclusão

**Classificação: RF10 CONCLUÍDO.**

Há evidência de acesso sem JWT, suporte a artista e contratante, URL direta/refresh, whitelist sem dados privados, privacidade máxima para menor, avatar e banner, tags sem N+1, portfólio existente, frontend responsivo, banco intacto, testes direcionados aprovados e regressão completa com `BUILD SUCCESS`.

### Registro para relatório semanal futuro

| Item | Registro |
|---|---|
| Data | 22/08/2026 |
| Objetivo | Finalizar visualização segura e compartilhável de perfil público |
| RF trabalhado | RF10 |
| Backend alterado | Sim — endpoint, DTO público de artista, serviço, consultas, segurança e avatar |
| Frontend alterado | SIM — página pública mínima, CSS, JS e teste |
| Banco alterado | NÃO |
| Privacidade | Whitelist fixa; menor sem consentimento comprovável retorna 404 |
| Medalhas | Fora do MVP; RF29 não implementado |
| Testes | Baseline 213/213; 15 novos backend; regressão 228/228; Jest bloqueado; sintaxe e browser aprovados |
| Pendências | RF36, RF16, RF18, RNF09 e infraestrutura npm/TLS |
| Próximos passos | Implementar somente após os RFs e políticas correspondentes serem definidos |

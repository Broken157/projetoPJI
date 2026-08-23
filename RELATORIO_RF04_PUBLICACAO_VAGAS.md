# RELATÓRIO TÉCNICO — RF04: PUBLICAÇÃO DE VAGAS

## 1. Objetivo e escopo

Auditar e finalizar exclusivamente o backend do RF04 — Publicação de Vagas/Projetos, preservando o banco oficial, o frontend e os requisitos RF03, RF06, RF07 e RF08 já implementados.

O trabalho seguiu a ordem obrigatória: **auditar → identificar lacunas → alterar somente o necessário → testar → documentar**.

## 2. Estado inicial

### Já existia

- endpoint `POST /api/vagas`;
- resposta `201 Created` em caso de sucesso;
- DTO específico `VagaRequest`, baseado nos campos editáveis já consolidados no RF07;
- autenticação JWT obrigatória no endpoint;
- autorização por tipo de usuário dentro de `VagaService`;
- associação entre `Vaga` e `Tag` pela tabela `tags_vaga`;
- criação transacional;
- status inicial `ABERTA` e data de publicação definidos no Service;
- validações Bean Validation para os campos obrigatórios e limites do schema;
- resolução de tags existentes por uma única consulta Spring Data;
- tratamento padronizado para `400`, `403`, `404` e conflitos.

### Estava correto

- visitantes eram bloqueados com `401` pela camada Spring Security;
- artistas autenticados eram bloqueados com `403` pelo Service;
- o perfil de contratante era localizado pelo ID do usuário autenticado;
- `status` e `dataPublicacao` não faziam parte do DTO editável e eram sobrescritos pelo servidor;
- remuneração negativa já era rejeitada e zero era permitido;
- modelo de trabalho era validado pelo enum `ModeloTrabalho`;
- título, descrição, requisitos, remuneração, forma de pagamento, cidade, estado, modelo e contrato eram obrigatórios;
- endereço e benefícios eram opcionais;
- a criação e a associação de tags estavam no mesmo método `@Transactional`;
- tag inexistente não era criada nem ignorada silenciosamente;
- o Controller apenas encaminhava a operação ao Service.

### Estava parcial

- `contratanteId` era obrigatório no payload e o Service comparava esse valor com o JWT. Isso impedia criação para terceiro, mas ainda tornava a publicação dependente de um ID controlado pelo cliente;
- o estado validava somente o tamanho, aceitando caracteres não alfabéticos;
- IDs de tags aceitavam zero ou números negativos até a consulta ao repositório;
- não havia normalização server-side própria da publicação;
- havia apenas cobertura incidental do POST em uma classe do RF03, sem testes dedicados de propriedade, campos controlados, tags e rollback.

### Faltava

- suíte de integração dedicada ao RF04;
- prova automatizada de que `contratanteId` adulterado é ignorado;
- prova de que `id`, `status` e `dataPublicacao` enviados não controlam a entidade;
- prova de rollback sem vaga ou junção residual para tag inexistente;
- prova da regra real de tags ausentes ou vazias.

## 3. Auditoria por camada

| Camada | Classificação inicial | Resultado da auditoria |
|---|---|---|
| Entity | Completo | `Vaga` representa todos os campos suportados pelo schema, possui proprietário obrigatório, fotos e relação Many-to-Many com tags. |
| Enum | Completo | `StatusVaga` e `ModeloTrabalho` refletem os tipos persistidos. Forma de pagamento e tipo de contrato são `varchar` no banco, portanto não foi inventado enum. |
| DTO | Parcial | Campos e limites estavam corretos, mas `contratanteId` era obrigatório; UF e IDs de tags precisavam de validação mais precisa. |
| Repository | Completo | Foram reutilizados `findById`, `findAllById` e `save`, sem SQL concatenado. |
| Service | Parcial | Autorização, transação, status e tags estavam corretos; a dependência do `contratanteId` do cliente precisava ser removida. |
| Controller | Completo | O POST existente foi preservado e já retornava `201`. |
| Segurança | Completo com lacuna de propriedade | `401` e `403` estavam corretos; a associação usava o usuário autenticado, mas o payload ainda participava da decisão por comparação. |
| Testes | Parcial | Baseline estava verde, porém o RF04 não possuía classe dedicada nem cobertura suficiente dos riscos críticos. |

## 4. Alterações realizadas

| Arquivo | Alteração | Motivo |
|---|---|---|
| `backend/src/main/java/com/portifolio/dto/VagaRequest.java` | `contratanteId` deixou de ser obrigatório e foi mantido como campo legado sem autoridade. | Preservar compatibilidade com o frontend e eliminar dependência de propriedade controlada pelo cliente. |
| `backend/src/main/java/com/portifolio/dto/VagaAtualizacaoRequest.java` | Validação alfabética de UF e validação positiva dos IDs de tags. | Rejeitar dados semanticamente inválidos antes da persistência. |
| `backend/src/main/java/com/portifolio/service/VagaService.java` | Removida a comparação com `request.contratanteId`; adicionada normalização restrita ao fluxo de publicação. | Tornar o JWT a única fonte de propriedade e não depender de JavaScript para normalização. |
| `backend/src/test/java/com/portifolio/controller/VagaPublicacaoRf04IntegrationTest.java` | Nova classe com 11 métodos e 19 execuções de integração. | Comprovar os critérios do RF04 em PostgreSQL real. |

O Controller, as entidades, enums e repositórios foram reaproveitados sem alterações.

## 5. Critérios do RF04

| Critério | Antes | Alteração | Depois | Evidência |
|---|---|---|---|---|
| Endpoint de publicação funcional | Completo | Nenhuma | Completo | `POST /api/vagas`, teste de criação e `201`. |
| Somente CONTRATANTE publica | Completo, pouco coberto | Testes dedicados | Completo | Sem JWT → `401`; ARTISTA → `403`; CONTRATANTE → `201`. |
| Proprietário derivado do JWT | Parcial | Removida comparação com ID do payload | Completo | Teste envia ID de terceiro e a vaga pertence ao autenticado. |
| Cliente não escolhe proprietário | Parcial | Campo legado ignorado | Completo | O campo pode ser adulterado ou omitido sem alterar o dono. |
| Status inicial controlado | Completo, pouco coberto | Teste dedicado | Completo | Payload tenta `CANCELADA`; entidade e resposta permanecem `ABERTA`. |
| ID e data controlados pelo servidor | Completo, sem prova | Teste dedicado | Completo | Valores injetados no JSON são ignorados e os persistidos são gerados pelo servidor. |
| Campos obrigatórios | Parcialmente coberto | Teste parametrizado | Completo | Nove combinações inválidas retornam `400` sem persistência. |
| Remuneração | Completo | Teste dedicado | Completo | Zero aceito; negativa rejeitada; `numeric(10,2)` respeitado. |
| Modelo de trabalho | Completo | Teste dedicado | Completo | Valor fora do enum retorna `400`. |
| Estado | Parcial | `@Pattern` alfabético e normalização | Completo | UF não alfabética retorna `400`; minúsculas são persistidas em maiúsculas. |
| Tags múltiplas | Completo, sem prova | Teste dedicado | Completo | Duas tags existentes são associadas. |
| Tag inexistente | Completo, sem prova | Teste dedicado | Completo | Retorna `404`; nenhuma tag é criada. |
| ID de tag inválido | Parcial | `@Positive` | Completo | Zero retorna `400` antes da persistência. |
| Tags ausentes/vazias | Implícito | Regra documentada e testada | Completo | Ambas são aceitas e produzem vaga sem tags. |
| Transação e rollback | Completo, sem prova | Teste de banco | Completo | Falha com tag inexistente deixa zero vagas e zero registros em `tags_vaga`. |
| Privacidade da resposta | Completo, sem prova RF04 | Teste dedicado | Completo | Resposta não contém senha, token, hash, e-mail ou telefone. |
| Banco preservado | Completo | Nenhuma | Completo | Manifesto SHA-256 antes/depois idêntico. |
| Frontend preservado | Completo | Nenhuma | Completo | Manifesto SHA-256 antes/depois idêntico. |

## 6. Propriedade e JWT

O Spring Security valida o token antes de permitir o POST. No Service, `AuthenticatedUserResolver` obtém o e-mail do `SecurityContext`, localiza o usuário persistido e `exigirContratanteAtual()` confirma o papel `CONTRATANTE`.

O perfil é então localizado exclusivamente por:

`perfilContratanteRepository.findById(usuarioAutenticado.getId())`

O campo `contratanteId`:

- existia e era obrigatório;
- continua no DTO apenas porque o frontend atual ainda o envia;
- agora é opcional;
- é totalmente ignorado pela regra de propriedade;
- pode ser removido do contrato em versão futura, depois de atualizar os consumidores.

Assim, um contratante A pode enviar o ID de B, mas a vaga será associada a A. Um artista não chega à etapa de criação e recebe `403`.

## 7. Transação e tags

`VagaService.criar()` permanece anotado com `@Transactional`. A operação lógica executa:

1. resolução do usuário autenticado;
2. validação do papel;
3. localização do perfil do contratante;
4. preenchimento e normalização dos campos;
5. resolução das tags com `tagRepository.findAllById`;
6. comparação da quantidade encontrada com a quantidade solicitada;
7. definição de `ABERTA` e da data atual;
8. persistência da vaga e da relação `tags_vaga`.

IDs não positivos são rejeitados com `400`. Se um ID positivo não existir, a API retorna `404`. A transação não deixa vaga nem junção parcial, conforme consulta direta ao PostgreSQL no teste de rollback.

Tags foram mantidas opcionais porque:

- `VagaRequest` já as tratava como opcionais;
- o schema não possui `NOT NULL`, quantidade mínima ou constraint equivalente na relação;
- o requisito não define quantidade mínima de categorias.

Tornar uma tag obrigatória seria inventar regra não respaldada pelas fontes de verdade.

## 8. Banco

**Banco alterado: NÃO.**

> Nenhuma tabela, coluna, enum, tipo, constraint, índice, migration ou script SQL foi alterado.

Os hashes dos arquivos em `database/` e de `backend/src/test/resources/db/schema-test.sql` foram comparados antes e depois e permaneceram idênticos. A configuração `spring.jpa.hibernate.ddl-auto=validate` também permaneceu inalterada.

## 9. Frontend

**Frontend alterado: NÃO.**

> Nenhum arquivo HTML, CSS ou JavaScript foi alterado.

O frontend continua enviando `contratanteId`. O backend preserva esse campo no DTO para compatibilidade, mas não confia nele. A remoção futura do campo no JavaScript pode ser feita em outra tarefa sem impacto na segurança atual.

## 10. Segurança

- **Autenticação:** ausência de JWT retorna `401` na camada Spring Security.
- **Autorização:** usuário autenticado do tipo ARTISTA recebe `403`.
- **Propriedade:** derivada somente do usuário persistido correspondente ao JWT.
- **Manipulação de IDs:** `contratanteId` é ignorado; `id` da vaga é gerado; IDs de tags são validados.
- **Campos controlados:** `status` e `dataPublicacao` não são aceitos como campos editáveis e o Service define os valores efetivos.
- **Injeção:** criação usa métodos Spring Data com parâmetros, sem concatenação de entrada do usuário.
- **Privacidade:** `VagaResponse` não expõe senha, token, hash, e-mail, telefone nem dados do responsável legal.
- **Endereço:** somente o endereço opcional recebido para a vaga é persistido; nenhum endereço privado do contratante é copiado automaticamente.

## 11. Testes

### Baseline

- Comando funcional: `.\\mvnw.cmd -o '-Dmaven.repo.local=C:\\Users\\masca\\.m2\\repository' '-Dspring.jpa.show-sql=false' test`
- Total: **111**
- Aprovados: **111**
- Falhas: **0**
- Erros: **0**
- Ignorados: **0**
- Resultado: **BUILD SUCCESS**

A execução inicial no sandbox encontrou `PKIX path building failed` ao consultar o Maven Central. Sem editar o projeto, a suíte foi repetida fora do sandbox, em modo offline, usando o cache Maven local já existente; esse é o baseline válido registrado.

### Novos testes RF04

- Classe: `VagaPublicacaoRf04IntegrationTest`
- Métodos novos: **11**
- Execuções novas: **19** — um método parametrizado cobre nove campos obrigatórios.
- Resultado direcionado: **19 aprovados**, 0 falhas, 0 erros, 0 ignorados, `BUILD SUCCESS`.

Cenários cobertos:

- `401`, `403` e publicação por contratante;
- propriedade pelo JWT com ID de terceiro no payload;
- omissão do campo legado `contratanteId`;
- status, ID e data controlados pelo servidor;
- nove campos obrigatórios inválidos;
- remuneração zero e negativa;
- modelo de trabalho e UF inválidos;
- múltiplas tags;
- tag inexistente;
- ID de tag não positivo;
- tags ausentes ou vazias;
- rollback da vaga e da junção;
- perfil de contratante ausente;
- privacidade da resposta;
- normalização server-side.

### Final

- Total: **130**
- Aprovados: **130**
- Falhas: **0**
- Erros: **0**
- Ignorados: **0**
- Ambiente: PostgreSQL 18.4 via Testcontainers
- Resultado: **BUILD SUCCESS**

## 12. Regressões

| Área | Execuções | Resultado |
|---|---:|---|
| RF03 — listagem e busca | 27 | Aprovado |
| RF04 — publicação | 19 | Aprovado |
| RF06 — candidaturas | 20 | Aprovado |
| RF07 — edição de vagas | 15 | Aprovado |
| RF08 — edição de perfil | 23 | Aprovado |
| Serviços de perfil e tamanho de página | 25 | Aprovado |
| Integração/autenticação de usuário | 1 | Aprovado |
| **Total** | **130** | **BUILD SUCCESS** |

Os testes RF04 também exercitam diretamente a autenticação e a identidade derivada do JWT.

## 13. Conflitos encontrados

1. O documento oficial marca o RF04 como “Não Desenvolvido”, mas o código já possuía endpoint, Service, transação, validações e integração com o frontend. A decisão foi auditar e corrigir apenas as lacunas, sem reimplementar.
2. O requisito textual menciona redirecionamento para login. Em API REST, o backend retorna `401`/`403`; redirecionamento é responsabilidade complementar do frontend, que foi preservado.
3. O requisito cita tipos de contrato para seleção, mas o banco armazena `tipo_contrato` e `forma_pagamento` como `varchar`, sem tabelas ou enums oficiais. Foram mantidas strings obrigatórias com limite de 100 caracteres.
4. O requisito menciona categorias relevantes, mas não define quantidade mínima e o schema permite ausência de tags. A regra existente de tags opcionais foi mantida.
5. O frontend envia `contratanteId`, enquanto a segurança exige propriedade pelo JWT. O campo foi preservado apenas para compatibilidade e passou a ser ignorado.

## 14. Pendências

Não há pendência essencial de backend para o RF04.

Fora do escopo desta tarefa:

- remover `contratanteId` do payload do frontend após versionar o contrato da API;
- criar catálogo de tipos de contrato somente se houver decisão de produto e autorização para mudança de banco;
- configurar HTTPS na infraestrutura de implantação;
- implementar fluxos de rascunho, pausa, encerramento ou notificações.

## 15. Conclusão

**Classificação: RF04 CONCLUÍDO.**

O endpoint de publicação está funcional, restrito a contratantes, com proprietário derivado exclusivamente do JWT, status inicial `ABERTA`, campos controlados pelo servidor, validações server-side, tags transacionais, rollback comprovado, privacidade preservada e regressão completa aprovada. Banco e frontend permaneceram inalterados.

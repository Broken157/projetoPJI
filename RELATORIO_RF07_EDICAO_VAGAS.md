# RELATÓRIO — RF07: EDIÇÃO DE VAGAS

## 1. Estado antes da tarefa

A implementação não foi refeita. O fluxo existente foi auditado e classificado antes das correções.

| Área | Estado inicial | Evidência / problema |
| --- | --- | --- |
| Controller | PARCIAL | `PUT /api/vagas/{id}` já existia, porém reutilizava o DTO de criação, que expunha `contratanteId` e `status`. |
| Service | PARCIAL | Havia busca da vaga, validação do proprietário, atualização, restauração do status e transação. Ainda exigia que `contratanteId` do cliente coincidisse com o JWT. |
| Repository | JÁ IMPLEMENTADO | `VagaRepository` e `TagRepository` utilizavam Spring Data/JPA e parameter binding, sem concatenação de input em SQL/JPQL. |
| DTO | PARCIAL | Limites físicos principais já estavam representados, mas o DTO misturava criação e edição, aceitava `status` e não rejeitava remuneração negativa. |
| Autorização | JÁ IMPLEMENTADO | O Service diferenciava CONTRATANTE de ARTISTA e exigia o proprietário da vaga. |
| Propriedade | PARCIAL | A decisão principal usava o usuário do JWT, mas o fluxo ainda dependia desnecessariamente de `contratanteId` enviado pelo cliente. |
| Tags | JÁ IMPLEMENTADO | IDs eram resolvidos no catálogo; conjunto evitava duplicatas; `null` preservava e lista vazia removia todos os vínculos. |
| Validações | PARCIAL | Obrigatoriedade, tamanhos, precisão/escala e enums existiam; faltava a regra server-side contra remuneração negativa já indicada pelo frontend de publicação (`min=0`). |
| Transação | JÁ IMPLEMENTADO | `VagaService.atualizar` já possuía `@Transactional`. |
| Candidaturas | JÁ IMPLEMENTADO | A edição não acessava `CandidaturaRepository` nem alterava a associação; faltava prova automatizada campo a campo. |
| Frontend | PARCIAL | O fluxo de edição existente consumia o endpoint e preservava tags no payload, mas não oferece gerenciamento visual de tags. |
| Testes | PARCIAL | Existia teste genérico de atualização, sem cobertura específica de IDOR, rollback, campos protegidos, tags e histórico de candidaturas. |

## 2. Problemas encontrados

### Autenticação

Nenhum defeito encontrado. O `SecurityConfig` exige autenticação para `PUT /api/vagas/{id}` e retorna `401` sem credencial válida.

### Autorização

A regra estava corretamente no Service: apenas usuário do tipo `CONTRATANTE` pode gerenciar vagas. ARTISTA recebe `403`.

### IDOR / propriedade

Não foi encontrada uma IDOR explorável, pois a propriedade já era comparada com o usuário resolvido do JWT. Porém, o endpoint ainda exigia e comparava `contratanteId` do payload. Isso tornava um campo protegido parte indevida do contrato de edição e fazia o servidor depender de uma identidade fornecida pelo cliente.

### Validações

Faltava impedir remuneração negativa no backend. O frontend de publicação já possui `min="0"`, evidenciando a regra atual, mas uma chamada direta à API conseguia ultrapassá-la.

### Tags

Não foi encontrado defeito funcional no algoritmo existente. Faltavam testes que comprovassem substituição, adição, remoção, deduplicação, preservação por omissão/null, limpeza por lista vazia e rollback com ID inexistente.

### Campos protegidos e status

O DTO compartilhado expunha `status`. O Service restaurava o status persistido após copiar os demais campos, portanto a transição arbitrária não era efetivada, mas o contrato não deixava essa proteção explícita. `id`, proprietário e data de publicação não eram copiados.

### Transações

O limite transacional estava correto, mas não havia teste real demonstrando rollback conjunto de `vagas` e `tags_vaga`.

### Candidaturas

Não foi encontrada chamada de cancelamento, exclusão ou atualização de candidaturas no fluxo de edição. Faltava teste comparando todos os campos relevantes antes e depois.

### Regressões

Não foram identificadas regressões em RF03, RF06, RF08, autenticação ou no cancelamento existente após as alterações.

## 3. Alterações realizadas

| Arquivo | Alteração | Motivo | RF/RNF |
| --- | --- | --- | --- |
| `backend/src/main/java/com/portifolio/dto/VagaAtualizacaoRequest.java` | Novo DTO exclusivo para detalhes editáveis; ignora campos desconhecidos/protegidos; valida limites, tag nula e remuneração não negativa. | Separar edição de criação e retirar identidade/status do contrato efetivo do PUT. | RF07, RNF07, RNF08 |
| `backend/src/main/java/com/portifolio/dto/VagaRequest.java` | Passou a estender o DTO de detalhes e manteve apenas `contratanteId` obrigatório para o POST existente. | Preservar compatibilidade do RF04 sem duplicar validações. | RF04, RF07 |
| `backend/src/main/java/com/portifolio/controller/VagaController.java` | O PUT passou a receber `VagaAtualizacaoRequest`; URL, método e response foram preservados. | Campos protegidos não fazem parte do contrato de edição. | RF07, RNF08 |
| `backend/src/main/java/com/portifolio/service/VagaService.java` | A atualização deixou de consultar `contratanteId` do payload e continua exigindo proprietário pelo JWT/recurso persistido; o preenchimento passou a aceitar o DTO de detalhes. | Eliminar dependência de identidade controlada pelo cliente e impedir transferência de propriedade. | RF07, RNF08 |
| `backend/src/test/java/com/portifolio/controller/VagaEdicaoRf07IntegrationTest.java` | Adicionados 15 cenários de integração em PostgreSQL 18/Testcontainers. | Provar autorização, validações, tags, rollback, campos protegidos, status e candidaturas. | RF07, RNF02, RNF07, RNF08, RNF10 |

Nenhum Repository, Entity, enum, serviço de candidatura, fluxo de RF25/RF31, arquivo de frontend ou arquivo SQL foi alterado.

## 4. Contrato final do endpoint

### Método e URL

```http
PUT /api/vagas/{id}
Authorization: Bearer <JWT>
Content-Type: application/json
```

### Request

Campos editáveis suportados:

```json
{
  "titulo": "Músico para evento",
  "descricao": "Apresentação de duas horas",
  "requisitos": "Portfólio atualizado",
  "remuneraValor": 2500.75,
  "formaPagamento": "Transferência",
  "cidade": "Campinas",
  "estado": "SP",
  "enderecoCompleto": "Rua Exemplo, 10",
  "beneficios": "Transporte",
  "modeloTrabalho": "PRESENCIAL",
  "tipoContrato": "Temporário",
  "tagIds": [1, 2],
  "categoria": "Música",
  "experiencia": "Pleno",
  "dataLimiteCandidatura": "2030-12-20",
  "abrangencia": "regional",
  "fotos": ["https://exemplo.com/foto.jpg"]
}
```

Obrigatórios: `titulo`, `descricao`, `requisitos`, `remuneraValor`, `formaPagamento`, `cidade`, `estado`, `modeloTrabalho` e `tipoContrato`.

Campos protegidos/ignorados no PUT: `id`, `contratanteId`, proprietário, `status`, `dataPublicacao`, candidaturas e qualquer campo de candidatura. O proprietário é obtido exclusivamente da vaga persistida e comparado com o usuário autenticado.

### Response

Mantido o `VagaResponse` existente, com `id`, proprietário, detalhes, status persistido, data de publicação, tags, fotos e demais campos da vaga.

### Códigos HTTP

| Código | Situação |
| --- | --- |
| `200` | Proprietário autenticado e payload válido. |
| `400` | Campo obrigatório/limite/formato/enum/remuneração inválido. |
| `401` | Requisição sem autenticação válida. |
| `403` | ARTISTA ou CONTRATANTE que não é proprietário. |
| `404` | Vaga inexistente ou tag informada inexistente. |

## 5. Autorização

Somente o CONTRATANTE proprietário pode editar. O filtro JWT popula o `SecurityContext`; `AuthenticatedUserResolver` resolve o usuário pelo e-mail autenticado. O Service carrega a vaga pelo ID da URL e compara `vaga.contratante.usuarioId` com o ID do usuário autenticado.

`contratanteId` enviado no JSON não participa da decisão. Mesmo um valor pertencente a outro contratante é ignorado, não transfere a vaga e não permite IDOR. A regra permanece na camada Service, independentemente do JavaScript.

## 6. Tags

- Os IDs são consultados em `TagRepository.findAllById`.
- Se a quantidade encontrada for diferente da quantidade solicitada, a operação retorna `404` e lança exceção dentro da transação.
- `Set<Long>` e `Set<Tag>` eliminam IDs/vínculos duplicados.
- IDs válidos substituem o conjunto associado em `tags_vaga`.
- `tagIds` ausente ou `null`: preserva as tags atuais.
- `tagIds: []`: remove todos os vínculos da vaga, comportamento compatível com o banco, que não exige ao menos uma tag.
- Não são criadas tags novas e o catálogo global não é alterado.
- `tags_artista` não é acessada pelo fluxo.
- O teste de rollback enviou título alterado e tags `[válida, 999999]`; o PostgreSQL confirmou título original e conjunto original de tags após o erro.

## 7. Candidaturas

A edição não chama `CandidaturaService`, não acessa `CandidaturaRepository` e não possui cascade de `Vaga` para candidaturas.

O teste específico criou uma candidatura antes do PUT e comparou após a edição:

- quantidade;
- ID;
- `vaga_id`;
- `artista_id`;
- status;
- mensagem de apresentação;
- link de portfólio;
- data da candidatura.

Todos permaneceram idênticos. O registro não foi excluído, recriado ou atualizado.

## 8. Status

O DTO efetivo de atualização não declara `status`. Campos extras são ignorados e, adicionalmente, o Service guarda e restaura o status persistido durante a edição. Foram testadas tentativas de transição em vaga `PAUSADA` e edições de detalhes em `PAUSADA`, `ENCERRADA` e `CANCELADA`; o status original foi preservado em todos os casos.

RF07 não ganhou máquina de estados nem restrição de edição por status. RF25/RF31 continuam responsáveis por cancelamento, pausa, reabertura e encerramento.

Ambiguidade de interface: a tela de detalhes existente oculta o botão de edição quando a vaga está `CANCELADA`, embora o RF07 não proíba explicitamente editar detalhes nesse estado. O backend não impõe essa restrição e o frontend não foi alterado, conforme o limite de escopo solicitado.

## 9. Banco

**Banco alterado: NÃO**

| Item | Alterado? |
| --- | --- |
| Tabelas | NÃO |
| Colunas | NÃO |
| Tipos | NÃO |
| Enums | NÃO |
| Constraints | NÃO |
| Índices | NÃO |
| Relacionamentos | NÃO |
| Migrations | NÃO |
| Scripts SQL | NÃO |

Verificação SHA-256 antes/depois:

- `database/sos_artistas.sql`: `A68A3265E33F20C5F4EEB6B8A4EBBCE1BCD61A9048E0D86DA9EA551421BFEA90`;
- `database/schema-test.sql`: `63BDD4B4D75791FE4B116F9DFBAD71902C88743E4C075F956565E330EE1D4C2D`;
- `database/migration_rf03.sql`: `242A56CB887EB79E5F0EC2D09BF8592F473D8EC5288543063672BB558513249C`.

Os hashes permaneceram idênticos. `backend/src/test/resources/db/schema-test.sql` também possui hash `63BDD4...D4C2D`, igual ao schema oficial usado nos testes. Permanece configurado:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

## 10. Frontend

**Frontend alterado: NÃO**

Os hashes SHA-256 de todos os arquivos do frontend foram comparados antes e depois e permaneceram idênticos.

O fluxo atual já envia corretamente o payload ao `PUT /api/vagas/{id}` e preserva `tagIds` recebidos no GET. Os campos extras legados `contratanteId` e `status` são aceitos como desconhecidos e ignorados pelo DTO de edição, preservando integração sem confiar neles.

Necessidade futura de frontend: permitir selecionar, adicionar e remover tags da vaga usando o contrato `tagIds` já suportado pelo backend. Isso não foi implementado porque exigiria nova interface e deve seguir o Figma.

```text
Novas telas: NÃO
Novos campos visuais: NÃO
Novos componentes: NÃO
Redesign: NÃO
Alteração do Figma: NÃO
```

## 11. Testes

Comando final:

```powershell
.\mvnw.cmd '-Dspring.jpa.show-sql=false' test
```

| Métrica | Resultado |
| --- | ---: |
| Baseline anterior | 87 |
| Novos testes RF07 | 15 |
| Total | 102 |
| Aprovados | 102 |
| Falhas | 0 |
| Erros | 0 |
| Ignorados | 0 |

**BUILD SUCCESS** em 15/08/2026, com PostgreSQL 18.4 real via Testcontainers, schema oficial e `ddl-auto=validate`.

Os 15 novos cenários cobrem: `401`, ARTISTA `403`, outro contratante `403`, proprietário `200`, `404`, IDOR, todos os detalhes editáveis, campos protegidos, status, edição nos status existentes, obrigatórios, tamanhos, precisão, valor negativo, enum inválido, substituição/adição/remoção/deduplicação de tags, semântica null/ausente/vazia, rollback, `tags_artista`, catálogo e candidatura integral.

Smoke HTTP separado em processo externo: não executado. O fluxo HTTP completo foi exercitado por MockMvc atravessando Security Filter Chain, Controller, validação, Service, JPA e PostgreSQL real; a suíte existente também manteve seu teste com servidor HTTP em porta aleatória.

## 12. Pendências

1. Frontend futuro: gerenciamento visual de tags, sob responsabilidade do integrante de interface/Figma.
2. Decisão de produto futura: confirmar se o botão de edição deve permanecer oculto para vaga `CANCELADA`; o backend não inventa essa restrição.

Não há pendência de backend necessária para concluir o RF07.

## 13. Conclusão

**CONCLUÍDO**

Somente o proprietário edita; ARTISTA e outro contratante são bloqueados; identidade vem do JWT; campos protegidos e status não são manipuláveis pelo PUT; detalhes e tags são validados e persistidos atomicamente; rollback foi comprovado; candidaturas permanecem integralmente intactas; banco e frontend não foram alterados; 102 testes passaram sem regressão.

# REGISTRO PARA RELATÓRIO SEMANAL

## Data

15/08/2026

## Objetivo

Finalização do RF07 — Edição de Vagas.

## RF/RNF trabalhados

RF07; regressão de RF03, RF04, RF06, RF08 e RF25 existente; RNF02, RNF07, RNF08 e RNF10 no contexto da edição.

## Backend alterado

Criado DTO específico de edição; separado o campo de identidade usado na criação; removida dependência de `contratanteId` no PUT; adicionada validação de remuneração não negativa; mantidas propriedade, status e transação; adicionada suíte de integração RF07.

## Frontend alterado

NÃO

## Banco alterado

NÃO

## Funcionalidades concluídas ou avançadas

Edição segura de todos os detalhes existentes, atualização transacional de tags, semântica de omissão/null/lista vazia, proteção de status/proprietário/data/ID e preservação do histórico de candidaturas.

## Bugs encontrados e corrigidos

Dependência indevida de `contratanteId` fornecido pelo cliente no contrato de edição; exposição de `status` no DTO compartilhado; ausência de validação server-side para remuneração negativa; ausência de cobertura de rollback e histórico.

## Segurança

Autenticação obrigatória, autorização por tipo, propriedade obtida da vaga persistida, identidade derivada do JWT, campos protegidos ignorados e ausência de concatenação de input em query.

## Decisões técnicas

Reutilizar Service, repositories e transação existentes; criar DTO de atualização como base dos campos comuns; preservar endpoint/response e compatibilidade do frontend; não criar máquina de estados nem alterar RF25/RF31.

## Testes e resultados

15 novos testes; total de 102; 102 aprovados; 0 falhas; 0 erros; 0 ignorados; BUILD SUCCESS; PostgreSQL 18.4/Testcontainers; schema oficial; `ddl-auto=validate`.

## Pendências

Interface futura de tags e decisão visual sobre edição de vaga cancelada.

## Próximos passos

Responsável pelo frontend implementar o gerenciamento visual de tags conforme Figma e equipe de produto esclarecer a disponibilidade do botão de edição para vaga cancelada.

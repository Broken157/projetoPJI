# RELATÓRIO — VALIDAÇÃO FINAL DO RF08

## Estado encontrado

```text
PARCIAL
```

A auditoria encontrou o seguinte estado antes desta validação:

| Questão auditada | Estado encontrado |
| --- | --- |
| Campos na entidade `Usuario` | Existiam: `nomeResponsavel`, `telefoneResponsavel` e `emailResponsavel` |
| Campos no banco oficial | Existiam em `usuarios`, com limites de 150, 20 e 150 caracteres |
| Campos no DTO de atualização `/api/usuarios/me` | Ausentes |
| Atualização pelo `UsuarioService` | Ausente |
| Atualização somente da própria conta | Já protegida pelo JWT/contexto e pelas verificações de propriedade |
| Validações server-side | Existiam no cadastro, mas não no contrato de atualização do RF08 |
| Tratamento de adulto | Não exigia responsável artificialmente |
| Privacidade em responses públicas | Já correta: dados privados eram omitidos para terceiros |
| Testes de edição do responsável | Ausentes |

Portanto, a estrutura, a autenticação, a propriedade e a privacidade já estavam corretas, mas a edição dos dados do responsável legal ainda não estava implementada no contrato principal do RF08.

## Alterações realizadas

| Arquivo | Alteração | Motivo |
| --- | --- | --- |
| `backend/src/main/java/com/portifolio/dto/UsuarioAtualizacaoRequest.java` | Adicionados `nomeResponsavel`, `telefoneResponsavel` e `emailResponsavel`, com `@Size` e `@Email` | Expor o contrato privado de atualização e aplicar validação server-side |
| `backend/src/main/java/com/portifolio/service/UsuarioService.java` | Atualização condicional dos dados do responsável para menores; validação do estado final; preservação de valores omitidos e de dados existentes de adultos | Completar o RF08 sem alterar nascimento, banco ou outros RFs |
| `backend/src/test/java/com/portifolio/controller/PerfilEdicaoRf08IntegrationTest.java` | Cinco testes novos e reforço do teste de privacidade pública | Comprovar persistência, validações, autenticação, propriedade, adulto, privacidade e regressão de completude |

Nenhum Controller, Repository, entidade, configuração de segurança, regra de `perfil_completo`, fluxo do RF06 ou endpoint novo precisou ser alterado.

### Contrato final

Endpoint recomendado:

```http
PUT /api/usuarios/me
Authorization: Bearer <JWT>
Content-Type: application/json
```

Campos adicionais aceitos no payload privado:

```json
{
  "nomeResponsavel": "Nome do responsável",
  "telefoneResponsavel": "11999999999",
  "emailResponsavel": "responsavel@exemplo.com"
}
```

O payload continua exigindo também `nome`, `telefone` e `email`, conforme o contrato preexistente de `UsuarioAtualizacaoRequest`.

Para usuário menor de 18 anos:

- os três dados do responsável podem ser atualizados;
- campo omitido preserva o valor persistido atual, permitindo atualização parcial segura;
- valor enviado como blank é rejeitado;
- o estado final precisa possuir nome, telefone e e-mail do responsável preenchidos;
- e-mail inválido e limites superiores ao banco são rejeitados pelo DTO;
- os dados são persistidos na mesma transação da atualização do usuário.

Para usuário adulto:

- responsável não se torna obrigatório;
- dados existentes não são apagados automaticamente;
- campos de responsável não passam a interferir em `perfil_completo`.

A rota legada `PUT /api/usuarios/{id}` também aplica a mesma regra para compatibilidade, mas continua exigindo que o ID pertença ao usuário autenticado. O frontend futuro deve preferir `/api/usuarios/me`.

## Segurança

### Autenticação

- A identidade é obtida do JWT e do `SecurityContext` por `AuthenticatedUserResolver`.
- `PUT /api/usuarios/me` sem autenticação retorna `401`.
- Nenhum `usuarioId` enviado pelo cliente é usado nessa rota.

### Propriedade

- O usuário atualiza somente sua própria conta em `/api/usuarios/me`.
- A rota legada compara o ID da URL com o usuário autenticado.
- Tentativa autenticada de alterar outra conta retorna `403`.
- Os testes confirmam que a tentativa não modifica os dados persistidos.

### Validações

- nome do responsável: máximo de 150 caracteres;
- telefone do responsável: máximo de 20 caracteres;
- e-mail do responsável: formato válido e máximo de 150 caracteres;
- para menores, o estado final não pode conter qualquer um dos três campos nulo ou blank;
- validações são executadas no backend, independentemente de JavaScript.

### Privacidade

- A própria conta pode receber os dados do responsável em `GET /api/usuarios/me` e na resposta privada da atualização.
- Consulta/listagem de terceiros continua usando a response pública sanitizada.
- Nome, telefone e e-mail do responsável, data de nascimento, telefone e e-mail particular não são expostos para terceiros.
- O teste de privacidade foi reforçado com valores realmente preenchidos, evitando um falso positivo provocado apenas por campos nulos.
- Nenhum dado do responsável foi adicionado a logs.

## Banco

```text
Banco alterado: NÃO
```

Não foram alterados:

- tabelas;
- colunas;
- tipos;
- enums;
- constraints;
- índices;
- relacionamentos;
- migrations;
- `sos_artistas.sql`;
- `schema-test.sql`;
- `migration_rf03.sql`;
- qualquer outro script SQL.

Os hashes SHA-256 dos três scripts foram registrados antes da implementação e comparados novamente ao final: permaneceram idênticos. A propriedade abaixo continua mantida:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

## Frontend

```text
Frontend alterado: NÃO
```

```text
Necessidade futura de frontend identificada: SIM
```

Todos os arquivos de `frontend/` tiveram hashes SHA-256 comparados antes e depois e permaneceram byte a byte idênticos.

Adaptação futura necessária pelo responsável do frontend:

1. consultar `GET /api/usuarios/me`;
2. quando `dataNascimento` indicar menor de 18 anos, exibir campos privados para `nomeResponsavel`, `telefoneResponsavel` e `emailResponsavel`;
3. enviar esses campos no `PUT /api/usuarios/me`, junto de `nome`, `telefone` e `email` já exigidos;
4. exibir os erros `400` de formato/tamanho e `422` de obrigatoriedade retornados pelo backend;
5. não enviar nem exibir esses dados em páginas públicas.

Nenhuma alteração de HTML, CSS ou JavaScript foi realizada nesta tarefa.

## `perfil_completo` e RF06

O `PerfilCompletoService` não foi alterado. Dados do responsável não foram adicionados como critério de completude profissional.

Os testes confirmam que:

- editar o responsável de um contratante menor completo mantém `perfilCompleto=true`;
- regras de completude de artista e contratante permanecem aprovadas;
- a regressão RF06 continua verificando artista incompleto, conclusão do perfil, candidatura permitida e novo bloqueio após remoção de requisito obrigatório.

## Testes

| Métrica | Resultado |
| --- | ---: |
| Testes antes | 82 |
| Testes adicionados | 5 |
| Total final | 87 |
| Aprovados | 87 |
| Falhas | 0 |
| Erros | 0 |
| Ignorados | 0 |
| Resultado | `BUILD SUCCESS` |

Comando final executado:

```bash
mvn test
```

Infraestrutura utilizada:

- PostgreSQL 18 real via Testcontainers;
- `database/schema-test.sql` oficial;
- `spring.jpa.hibernate.ddl-auto=validate`.

Cobertura adicionada e reforçada:

1. menor autenticado atualiza nome, telefone e e-mail do próprio responsável;
2. resposta privada contém os dados atualizados;
3. dados persistem corretamente no PostgreSQL;
4. nome, telefone e e-mail blank são rejeitados;
5. e-mail em formato inválido é rejeitado pelo DTO;
6. ausência de autenticação retorna `401`;
7. tentativa de alterar conta alheia retorna `403` e não persiste alterações;
8. dados preenchidos do responsável não aparecem na response pública de terceiros;
9. edição não altera `perfil_completo`;
10. adulto não passa a exigir responsável;
11. dados existentes de adulto não são apagados;
12. toda a suíte anterior, inclusive RF06, continua aprovada.

## Correção documental

O relatório anterior associou incorretamente:

```text
RF23 — Tags
```

Essa referência estava errada. O RF23 oficial corresponde ao **Sistema de Notificações em Tempo Real**. Administração de catálogo de tags não deve ser registrada como RF23.

Nenhuma funcionalidade do RF23, nenhuma notificação e nenhuma infraestrutura em tempo real foram implementadas nesta tarefa.

## Dependências futuras

- Se o RF36 estabelecer consentimento ou reconfirmação após mudança do e-mail do responsável, esse comportamento deverá ser integrado futuramente à atualização existente.
- Esta tarefa não criou token de responsável, status de consentimento, verificação de e-mail, tabela ou migration.
- RF35 e RF36 permanecem fora do escopo.

## Conclusão

```text
CONCLUÍDO
```

Todos os critérios obrigatórios de backend desta validação final do RF08 estão implementados e testados. A única necessidade restante é a integração visual futura pelo responsável do frontend, que não bloqueia a conclusão do contrato backend e foi deliberadamente preservada fora desta tarefa.

# REGISTRO PARA RELATÓRIO SEMANAL

## Data

15/08/2026

## Objetivo

Validação final do RF08 — dados do responsável legal.

## Backend alterado

SIM. DTO e Service de atualização de usuário passaram a permitir edição segura e validada dos dados do responsável legal de menores, com cobertura de integração.

## Frontend alterado

NÃO.

## Banco alterado

NÃO.

## Testes

82 testes anteriores + 5 novos = 87 testes aprovados, 0 falhas, 0 erros, 0 ignorados e `BUILD SUCCESS`, usando PostgreSQL/Testcontainers e `ddl-auto=validate`.

## Correções

- concluída a edição backend de nome, telefone e e-mail do responsável legal;
- adicionadas validações condicionais para menores;
- preservado o comportamento de adultos;
- reforçada a privacidade das responses públicas;
- corrigida documentalmente a associação indevida entre RF23 e tags: RF23 é Notificações em Tempo Real.

## Pendências

- frontend deverá futuramente consumir os três campos privados em `/api/usuarios/me`;
- eventual consentimento/reconfirmação após mudança do e-mail do responsável pertence ao RF36.

## Próximo passo

RF07 — Edição de Vagas.

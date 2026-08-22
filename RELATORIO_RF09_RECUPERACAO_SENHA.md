# Relatório técnico — RF09: Recuperação de Senha

## 1. Objetivo

Auditar e completar, sem alteração de banco ou frontend, o backend de recuperação de senha: solicitação sem enumeração de usuários, token seguro e expirável, redefinição por POST/body, BCrypt, uso único e revogação das sessões renováveis.

## 2. Estado inicial

Baseline executado antes das edições com PostgreSQL 18/Testcontainers:

- total: 198;
- aprovados: 198;
- falhas: 0;
- erros: 0;
- ignorados: 0;
- resultado: `BUILD SUCCESS`.

## 3. Auditoria por camada

| Camada | Estado inicial | Evidência/impacto |
| --- | --- | --- |
| Banco | correto para a persistência | `usuarios.token_recuperacao varchar(255)` e `token_expiracao timestamp` já existiam |
| `Usuario` | correto | os dois campos, senha nullable e `googleId` já estavam mapeados |
| `UsuarioRepository` | parcial | havia busca por e-mail, mas não por hash de recuperação |
| Auth/service RF09 | ausente | não havia geração, validação ou redefinição por token |
| Controller RF09 | ausente | não havia `/forgot-password` nem `/reset-password` |
| Segurança | parcial | `/api/auth/**` estava público de forma ampla |
| Refresh tokens | correto/reutilizável | hashes persistidos e revogação global por usuário já existiam |
| E-mail | ausente | nenhum SMTP, `MailService`, `JavaMailSender` ou provedor configurado |
| Rate limit | ausente | nenhuma infraestrutura reutilizável foi encontrada |
| Frontend | parcial/incompatível | `login.html` aponta para `recuperar-senha.html`, mas a página e o JS não existem |
| Testes RF09 | ausente | nenhuma suíte específica existia |

## 4. Banco

As colunas oficiais necessárias já existiam nos schemas oficial e de teste. O backend reutiliza `token_recuperacao` para o hash SHA-256 e `token_expiracao` para o horário limite.

Nenhuma tabela, coluna, enum, migration ou relacionamento foi alterado. `spring.jpa.hibernate.ddl-auto=validate` foi preservado.

## 5. Fluxo de solicitação

Endpoint público específico:

```http
POST /api/auth/forgot-password
Content-Type: application/json
```

```json
{"email":"usuario@email.com"}
```

Para e-mail existente, inexistente e Google-only, o retorno é sempre:

```json
{
  "mensagem": "Se o e-mail estiver cadastrado, você receberá as instruções em breve."
}
```

O HTTP é `200` nos três casos e a resposta não contém ID, tipo, token, hash, expiração ou dados pessoais. E-mail inexistente não cria nem altera registro.

Sem uma implementação real de `PasswordRecoveryEmailSender`, a conta local recebe a resposta genérica, mas nenhum token é criado. Isso evita persistir um token que não pode ser entregue e impede alegação falsa de envio interno.

## 6. Fluxo Google-only

Google-only é identificado exatamente por `senha == null` e `googleId != null`. Não é criado token nem alterada senha. Quando existir um adaptador real de e-mail, o serviço chamará a orientação para “Entrar com Google”; sem adaptador, mantém a resposta genérica e não finge envio.

Conta híbrida, com senha local e `googleId`, continua apta ao fluxo local porque não é Google-only.

## 7. Token e segurança

- geração com `SecureRandom` de 32 bytes, equivalentes a 256 bits;
- codificação Base64 URL-safe sem padding (43 caracteres);
- persistência exclusiva do SHA-256 hexadecimal (64 caracteres);
- token bruto existe somente em memória e é entregue apenas à abstração de e-mail;
- token bruto/hash nunca são retornados pela API nem escritos em log;
- validade de uma hora usando horário do servidor;
- nova solicitação substitui hash e expiração anteriores;
- token anterior, expirado, inválido ou já usado recebe o mesmo erro previsível.

## 8. Redefinição

Endpoint público específico:

```http
POST /api/auth/reset-password
Content-Type: application/json
```

```json
{
  "token": "token-bruto-recebido-pelo-usuario",
  "novaSenha": "nova-senha"
}
```

O token é aceito somente no body. Query parameter sem token no body resulta em `400`.

A política de senha foi reutilizada: valor não vazio e máximo de 72 caracteres, limite técnico do BCrypt já usado no RF08/cadastro. Não foi inventada política diferente. A senha é persistida com BCrypt; após sucesso, hash de recuperação e expiração são limpos, tornando o token de uso único.

Tokens inválido, expirado e já utilizado retornam `404` com a mensagem uniforme `Token de recuperação inválido ou expirado.`, sem produzir `500` previsível.

## 9. Sessões/refresh tokens

O serviço reutiliza `RefreshTokenService.invalidarTodosDoUsuario`. Na mesma transação da redefinição são executados:

1. BCrypt da nova senha;
2. limpeza do hash de recuperação;
3. limpeza da expiração;
4. revogação de todos os refresh tokens persistidos.

Teste integrado comprovou que refresh token anterior não pode ser reutilizado. Outro teste forçou falha PostgreSQL na revogação e comprovou rollback da senha, da limpeza do token e da alteração dos refresh tokens.

Access tokens JWT já emitidos continuam válidos até a expiração porque o projeto não mantém blacklist/versionamento de access token. O requisito solicitava revogação das sessões renováveis existentes, que foi atendida.

## 10. E-mail

Não existe infraestrutura real de envio. Foi criado somente o contrato `PasswordRecoveryEmailSender`, sem implementação de produção e sem adicionar dependência ou provedor externo.

Quando um adaptador real for configurado:

- conta local poderá receber o token bruto exclusivamente pelo canal de recuperação;
- conta Google-only poderá receber orientação de login Google;
- falha de entrega limpa o token recém-criado e mantém a resposta externa genérica.

No estado atual, nenhum e-mail é enviado. Por esse motivo, o fluxo de produção ainda não entrega o segredo ao usuário e o RF09 não pode ser classificado como concluído.

## 11. Alterações realizadas

| Arquivo | Alteração | Motivo |
| --- | --- | --- |
| `dto/ForgotPasswordRequest.java` | DTO de e-mail validado | contrato específico da solicitação |
| `dto/ResetPasswordRequest.java` | token e nova senha no body | impedir query/path e reutilizar política de senha |
| `dto/PasswordRecoveryResponse.java` | resposta mínima com mensagem | evitar vazamento de dados |
| `service/PasswordRecoveryEmailSender.java` | contrato sem implementação de produção | preparar integração sem inventar provedor |
| `service/PasswordRecoveryService.java` | solicitação, hash, expiração, reset, limpeza e revogação | implementar núcleo seguro do RF09 |
| `repository/UsuarioRepository.java` | busca por hash de recuperação | localizar token sem armazenar texto puro |
| `controller/AuthController.java` | dois endpoints RF09 | expor contratos oficiais |
| `config/SecurityConfig.java` | substituição de `/api/auth/**` por rotas POST explícitas | liberar somente endpoints necessários |
| `controller/RecuperacaoSenhaRf09IntegrationTest.java` | 10 testes PostgreSQL/Testcontainers | provar segurança e regressões específicas |

## 12. Segurança e privacidade

- resposta indistinguível para conta local, inexistente e Google-only;
- nenhum token, hash, expiração, ID ou tipo de usuário na resposta;
- token aleatório de 256 bits e hash SHA-256 persistido;
- BCrypt para senha;
- erro uniforme para token inválido/expirado/usado;
- rotas públicas limitadas aos POSTs de autenticação conhecidos;
- ausência de token em query parameter;
- transação e rollback comprovados;
- nenhum token bruto em logs.

Risco residual: não existe rate limiting. Não foi instalada infraestrutura nova por estar fora do escopo autorizado; a resposta genérica reduz enumeração, mas não substitui proteção contra abuso volumétrico.

## 13. Banco alterado: NÃO

Hashes antes/depois permaneceram idênticos:

- `database/sos_artistas.sql`: `1711CE1FBDEDB3BA97BBACC7D820FBF7C31A78E87BFADF20FB7FE2967E0666AD`;
- `database/schema-test.sql` e schema do backend: `202C34E7BABE0300C45FC0EF2ED96EC380A78780CED89FBDCBA7B2245CE79671`;
- `database/migration_rf03.sql`: `242A56CB887EB79E5F0EC2D09BF8592F473D8EC5288543063672BB558513249C`;
- `database/migration_rf25_motivo.sql`: `8920B5A1531E029B2AFE66C387A6A108AD9B326D1661E828EDFF53E7C9721B6C`;
- `application.properties`: `50FE579F18C5C9249D89DB5548F77BB32B9C325D236222EFB5639333E690DAC3`.

## 14. Frontend alterado: NÃO

Nenhum HTML, CSS ou JavaScript foi editado. `frontend/public/js/main.js` permaneceu com SHA-256 `126361C4B5F335C7185261E979D7CC2E0D013749E955EBF1B84BCD88EDE8B8D1`.

O link atual em `login.html` aponta para uma página inexistente. A criação das telas de solicitação/redefinição e a decisão segura para transporte do token no navegador exigem autorização de frontend.

## 15. Testes baseline

- total: 198;
- aprovados: 198;
- falhas: 0;
- erros: 0;
- ignorados: 0;
- `BUILD SUCCESS`.

## 16. Novos testes RF09

A suíte dedicada passou com 10 testes:

1. respostas indistinguíveis para local, inexistente e Google-only;
2. inexistente sem alteração de dados;
3. hash SHA-256 e validade próxima de uma hora;
4. nova solicitação invalida token anterior;
5. BCrypt, login antigo/novo, limpeza e uso único;
6. erro uniforme para token inválido e expirado;
7. rejeição de token somente em query parameter;
8. revogação de refresh token anterior;
9. rollback real quando a revogação falha;
10. falha de entrega sem token ativo e com resposta genérica.

Resultado: 10 aprovados, 0 falhas, 0 erros, 0 ignorados, `BUILD SUCCESS`.

## 17. Testes finais/regressões

- total final: 208;
- aprovados: 208;
- falhas: 0;
- erros: 0;
- ignorados: 0;
- resultado: `BUILD SUCCESS`.

Passaram RF01/RF02, perfil e senha RF08, logout/refresh/JWT, RF03–RF07, RF09, RF25 e RF31. Nenhum teste correto foi removido ou desabilitado.

## 18. Conflitos encontrados

1. O RF09 exige link por e-mail, mas proíbe token em query parameter. O frontend de recuperação não existe; portanto, não foi inventado `?token=...`. Uma futura integração pode usar fragmento client-side ou estado equivalente e enviar o token somente no body do POST.
2. A mensagem oficial diz que instruções serão recebidas, mas não existe serviço de e-mail. A API preserva a mensagem genérica por segurança, enquanto o backend não registra envio nem gera token sem canal real.
3. O link “Esqueceu a senha?” já existe, porém aponta para arquivo ausente. Ele não foi corrigido porque frontend deve ser preservado nesta tarefa.

## 19. Pendências

- configurar uma implementação real e segura de `PasswordRecoveryEmailSender`/SMTP;
- definir link compatível sem query parameter e criar as páginas frontend de solicitação/redefinição mediante autorização;
- adicionar rate limiting compartilhado quando houver infraestrutura/RNF correspondente;
- opcionalmente, avaliar invalidação imediata de access tokens JWT por versionamento, decisão arquitetural fora do RF09 atual.

Não há pendência de banco.

## 20. Conclusão

**RF09 PARCIAL.**

O núcleo backend está implementado e comprovado: resposta genérica, proteção contra enumeração, Google-only, token seguro e hash, uma hora, POST/body, BCrypt, uso único, limpeza, refresh revogado, transação, rollback e regressão completa. Porém, sem infraestrutura real de e-mail o token não pode ser entregue ao usuário; além disso, o frontend de recuperação não existe. Pelos critérios oficiais, essas lacunas impedem declarar **RF09 CONCLUÍDO NO BACKEND** em produção.

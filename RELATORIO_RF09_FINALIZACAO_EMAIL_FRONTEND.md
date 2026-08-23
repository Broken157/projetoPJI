# PJI Palco — Finalização do RF09: Recuperação de Senha com E-mail e Frontend

## 1. Objetivo

Concluir o RF09 sem reescrever seu núcleo já validado, adicionando exclusivamente a integração SMTP configurável e as páginas de recuperação e redefinição de senha. O banco de dados, o schema de teste e as demais telas foram preservados.

## 2. Estado inicial

O baseline foi executado antes das alterações com `backend\mvnw.cmd '-Dspring.jpa.show-sql=false' test`:

- 208 testes executados;
- 208 aprovados;
- 0 falhas;
- 0 erros;
- 0 ignorados;
- `BUILD SUCCESS`.

O núcleo existente já possuía endpoints públicos explícitos, resposta genérica anti-enumeração, tratamento de conta inexistente e Google-only, token aleatório de 256 bits, persistência exclusiva de SHA-256, expiração em uma hora, BCrypt, uso único e revogação de refresh tokens.

## 3. Auditoria de e-mail

Foram auditados `PasswordRecoveryEmailSender`, `PasswordRecoveryService`, `AuthController`, os DTOs RF09, `SecurityConfig`, `pom.xml` e `application.properties`.

Achados:

- existia somente a abstração `PasswordRecoveryEmailSender`;
- não existia implementação de produção;
- `spring-boot-starter-mail` não estava presente;
- a falha do sender já limpava token e expiração, mas não possuía log interno;
- sem sender, o serviço já respondia genericamente e não gerava token inutilizável;
- `forgot-password` e `reset-password` já eram públicos de forma explícita, sem liberar `/api/auth/**`.

## 4. Implementação SMTP

Foi adicionado o starter oficial `spring-boot-starter-mail` e criada a implementação `SmtpPasswordRecoveryEmailSender`, baseada em `JavaMailSender` e `SimpleMailMessage`.

O bean só é registrado quando `spring.mail.host` e `app.mail.from` possuem conteúdo, correspondendo às variáveis `MAIL_HOST` e `MAIL_FROM`. Sem essa configuração, o serviço mantém resposta genérica e não cria token.

Para conta local, o e-mail contém explicação, validade de uma hora, link de redefinição e orientação para ignorar a mensagem. Para conta Google-only, envia somente orientação para usar “Entrar com Google”.

## 5. Configuração por ambiente

As propriedades foram adicionadas sem credenciais reais:

```properties
spring.mail.host=${MAIL_HOST:}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=${MAIL_SMTP_AUTH:true}
spring.mail.properties.mail.smtp.starttls.enable=${MAIL_STARTTLS:true}
app.mail.from=${MAIL_FROM:}
app.frontend.base-url=${FRONTEND_BASE_URL:http://localhost:5500}
```

Nenhuma senha, token SMTP, app password ou credencial pessoal foi adicionada ao repositório.

## 6. Segurança do token

O núcleo foi preservado:

- geração com `SecureRandom`, 32 bytes/256 bits;
- token bruto não é persistido;
- somente SHA-256 é armazenado;
- validade de uma hora;
- substituição do token anterior em nova solicitação;
- limpeza após redefinição;
- uso único;
- nova senha com BCrypt;
- refresh tokens anteriores revogados.

O token bruto existe apenas durante a geração, no conteúdo do e-mail, no fragmento aberto pelo usuário, na memória da página e no body do POST.

## 7. Link de recuperação

O link é montado como:

```text
${FRONTEND_BASE_URL}/redefinir-senha.html#token=<TOKEN_BRUTO>
```

Não existe `?token=` nem path parameter. Testes do sender confirmam o fragmento `#token=` e rejeitam conceitualmente a variante em query.

## 8. Frontend — Recuperação

Foi criada `frontend/public/recuperar-senha.html`, reutilizando `base.css`, `componentes.css`, `login.css`, navbar, card, input e botão existentes.

O formulário:

- exige e-mail e valida formato básico;
- envia `POST /api/auth/forgot-password`;
- usa body JSON `{ "email": "..." }`;
- exibe a mensagem genérica retornada pelo backend;
- não consulta se a conta existe;
- oferece retorno ao login.

O `login.html` já apontava para essa página e permaneceu inalterado.

## 9. Frontend — Redefinição

Foi criada `frontend/public/redefinir-senha.html`, com nova senha, confirmação, botão, mensagem, retorno ao login e solicitação de novo link.

O script:

- lê `window.location.hash`;
- extrai `token` em memória;
- remove o fragmento com `history.replaceState`, sem recarregar;
- não usa `localStorage`, `sessionStorage`, cookie ou IndexedDB;
- bloqueia o envio quando o token está ausente;
- exige campos preenchidos e valores iguais;
- envia `POST /api/auth/reset-password` com `{ "token": "...", "novaSenha": "..." }` no body;
- não realiza login automático;
- mostra mensagem simples para token inválido ou expirado.

## 10. Conta Google-only

O serviço continua retornando a mesma mensagem genérica. Nenhum token é criado. Com SMTP configurado, o sender envia orientação para acessar a conta por “Entrar com Google”, sem link de redefinição e sem informação sensível adicional.

O comportamento é coberto pelo fake de integração e pelo teste unitário do sender.

## 11. Alterações realizadas

| Arquivo | Alteração | Motivo |
| ------- | --------- | ------ |
| `backend/pom.xml` | Adicionado `spring-boot-starter-mail` | Infraestrutura SMTP oficial do Spring |
| `backend/src/main/resources/application.properties` | Propriedades SMTP e URL do frontend por ambiente | Configuração sem segredos hardcoded |
| `backend/src/main/java/com/portifolio/service/PasswordRecoveryEmailSender.java` | Comentário atualizado | Refletir a integração concreta existente |
| `backend/src/main/java/com/portifolio/service/PasswordRecoveryService.java` | Log seguro de falhas, somente com tipo da exceção | Observabilidade sem token ou credenciais |
| `backend/src/main/java/com/portifolio/service/SmtpPasswordRecoveryEmailSender.java` | Arquivo criado | Envio SMTP para conta local e Google-only |
| `backend/src/test/java/com/portifolio/service/SmtpPasswordRecoveryEmailSenderTest.java` | Arquivo criado, 2 testes | Conteúdo, destinatário, assunto e link seguro |
| `backend/src/test/java/com/portifolio/service/SmtpPasswordRecoveryEmailSenderConfigurationTest.java` | Arquivo criado, 2 testes | Bean ausente/presente conforme configuração |
| `backend/src/test/java/com/portifolio/controller/RecuperacaoSenhaRf09IntegrationTest.java` | Adicionado 1 teste e fortalecido o fake | Garantir ausência de token bruto nos logs |
| `frontend/public/recuperar-senha.html` | Arquivo criado | Tela de solicitação do RF09 |
| `frontend/public/redefinir-senha.html` | Arquivo criado | Tela de redefinição do RF09 |
| `frontend/public/js/recuperacao-senha.js` | Arquivo criado | Integração mínima das duas telas com a API |
| `frontend/src/rf09-recuperacao-senha.test.js` | Arquivo criado, 5 testes | Cobertura compatível com o Jest existente |
| `RELATORIO_RF09_FINALIZACAO_EMAIL_FRONTEND.md` | Arquivo criado | Evidências e documentação técnica |

`AuthController`, DTOs, `SecurityConfig`, `login.html`, `main.js` e CSS compartilhado foram auditados e não precisaram ser alterados.

## 12. Banco

**Banco alterado: NÃO.**

Hashes SHA-256 finais, iguais ao baseline:

| Artefato | SHA-256 |
| --- | --- |
| `database/sos_artistas.sql` | `1711CE1FBDEDB3BA97BBACC7D820FBF7C31A78E87BFADF20FB7FE2967E0666AD` |
| `database/schema-test.sql` | `202C34E7BABE0300C45FC0EF2ED96EC380A78780CED89FBDCBA7B2245CE79671` |
| `backend/src/test/resources/db/schema-test.sql` | `202C34E7BABE0300C45FC0EF2ED96EC380A78780CED89FBDCBA7B2245CE79671` |
| `database/migration_rf03.sql` | `242A56CB887EB79E5F0EC2D09BF8592F473D8EC5288543063672BB558513249C` |
| `database/migration_rf25_motivo.sql` | `8920B5A1531E029B2AFE66C387A6A108AD9B326D1661E828EDFF53E7C9721B6C` |

`spring.jpa.hibernate.ddl-auto=validate` foi preservado.

## 13. Frontend

**Frontend alterado: SIM — AUTORIZADO.**

Foram criadas somente as duas páginas RF09, um script específico e o teste correspondente. `login.html`, `main.js`, CSS compartilhado e demais páginas permaneceram inalterados. Os hashes preservados são:

- `frontend/public/login.html`: `1F452044E6950FFADB59764E41CF6B42FB50EB6A7DCD4BC0306C85B2ED920AED`;
- `frontend/public/js/main.js`: `126361C4B5F335C7185261E979D7CC2E0D013749E955EBF1B84BCD88EDE8B8D1`.

## 14. Testes backend

Testes RF09/SMTP direcionados:

- os 10 testes RF09 originais permaneceram aprovados;
- 1 novo teste de log seguro foi adicionado ao teste de integração;
- 4 novos testes unitários SMTP foram adicionados;
- no estado final, 15 testes diretamente relacionados ao RF09/SMTP estão aprovados.

O teste de integração continua cobrindo resposta indistinguível, conta inexistente, Google-only, SHA-256/uma hora, substituição, BCrypt/uso único/login, token inválido e expirado, rejeição de query parameter, revogação, rollback e falha de entrega.

## 15. Testes de e-mail

Os testes com mock/fake, sem Internet e sem credenciais, confirmam:

- destinatário e remetente;
- assunto esperado;
- explicação, validade de uma hora e orientação para ignorar;
- fragmento `#token=`;
- ausência de `?token=`;
- orientação Google-only sem link;
- bean SMTP ausente sem host/remetente;
- bean presente com configuração;
- resposta genérica e limpeza de token/expiração na falha;
- token bruto ausente da resposta e dos logs capturados.

## 16. Testes frontend

Foram criados 5 testes Jest para e-mail obrigatório, POST/mensagem genérica, leitura e remoção do fragmento, divergência de senhas, token no body e fora da URL, sucesso, token ausente, erro expirado e ausência de persistência no navegador.

**Teste frontend automatizado executado: NÃO.**

Tentativas:

1. `npm test -- --watchAll=false --runInBand` no host: `npm` não está no PATH.
2. Node 22 Docker com `npm ci`: `npm error Exit handler never called!`.
3. Node 20 Docker com `npm ci`: mesmo erro e `react-scripts: not found` porque a instalação não foi concluída.

Não foi instalado framework novo e os testes não são declarados aprovados. A sintaxe do novo script foi validada com `node --check`, resultado `exit 0`.

Validação renderizada no Browser interno:

- páginas e títulos corretos;
- DOM não vazio e sem overlay;
- console sem erros ou avisos relevantes;
- formulário desktop renderizado;
- fragmento removido da URL;
- senhas divergentes bloqueiam a chamada e exibem mensagem;
- token ausente desabilita o botão;
- controles principais visíveis em viewport móvel.

## 17. Teste manual

Checklist documentado para ambiente com SMTP de teste configurado:

- [ ] abrir `login.html`;
- [ ] clicar “Esqueceu a senha?”;
- [ ] confirmar `recuperar-senha.html`;
- [ ] enviar um e-mail de teste;
- [ ] confirmar a mensagem genérica;
- [ ] abrir o link recebido;
- [ ] confirmar `redefinir-senha.html` sem fragmento visível após a carga;
- [ ] preencher e confirmar a nova senha;
- [ ] confirmar a mensagem de sucesso;
- [ ] fazer login com a nova senha.

O checklist E2E com entrega SMTP real não foi executado porque nenhuma credencial real deve ser incluída no projeto.

## 18. Regressão completa

Comando final:

```powershell
.\mvnw.cmd '-Dspring.jpa.show-sql=false' test
```

Resultado:

- 213 testes executados;
- 213 aprovados;
- 0 falhas;
- 0 erros;
- 0 ignorados;
- `BUILD SUCCESS`;
- tempo Maven: 1 min 33 s.

Foram preservadas as regressões de RF01, RF02, RF03, RF04, RF05, RF06, RF07, RF08, RF09, RF25, RF31, JWT, refresh e logout.

## 19. Segurança e privacidade

- resposta externa permanece anti-enumeração;
- rotas públicas continuam listadas explicitamente;
- CORS não foi ampliado;
- nenhuma credencial foi hardcoded;
- token bruto não é persistido nem logado;
- nova senha não é logada;
- link usa fragmento, que não é enviado no request HTTP da página;
- token segue no body do POST;
- falha SMTP não revela conta existente e desativa o token recém-criado.

## 20. Variáveis de ambiente necessárias

| Variável | Obrigatória para envio | Finalidade |
| --- | --- | --- |
| `MAIL_HOST` | Sim | Host SMTP e ativação do sender |
| `MAIL_PORT` | Não; padrão 587 | Porta SMTP |
| `MAIL_USERNAME` | Conforme provedor | Usuário SMTP |
| `MAIL_PASSWORD` | Conforme provedor | Senha/token SMTP |
| `MAIL_FROM` | Sim | Remetente e ativação do sender |
| `MAIL_SMTP_AUTH` | Não; padrão `true` | Autenticação SMTP |
| `MAIL_STARTTLS` | Não; padrão `true` | STARTTLS |
| `FRONTEND_BASE_URL` | Não; padrão `http://localhost:5500` | Base do link de redefinição |

## 21. Limitações

- nenhum provedor SMTP real foi acionado nos testes automatizados, conforme exigência de não depender de Internet/credenciais;
- a suíte Jest não pôde ser executada pelo erro interno do npm documentado;
- o fluxo completo com entrega real deve ser verificado no ambiente que possuir SMTP de teste/produção;
- em 390 px, a navbar compartilhada existente gera rolagem horizontal, embora o formulário RF09 permaneça visível e utilizável; o CSS global não foi alterado para não ampliar o escopo.

## 22. Pendências

Não bloqueiam o RF09 e permanecem fora do escopo:

- rate limiting global;
- blacklist/versionamento de access token;
- redesign e correção responsiva global da navbar;
- RF23;
- OAuth adicional;
- reparo da instalação/npm do frontend no ambiente de desenvolvimento.

Registro para relatório semanal futuro: frontend alterado de forma autorizada; banco preservado; infraestrutura SMTP adicionada; 5 testes backend novos; 5 testes frontend criados, mas bloqueados pelo npm; validação renderizada aprovada; regressão backend com 213 testes e `BUILD SUCCESS`.

## 23. Conclusão

**RF09 CONCLUÍDO.**

A implementação funcional e configurável de e-mail existe, sem credenciais commitadas; o link usa fragmento, as duas telas foram adicionadas, o núcleo seguro permaneceu preservado e os testes backend/regressões finalizaram com `BUILD SUCCESS`. A impossibilidade ambiental de executar Jest e o checklist SMTP real estão registrados sem serem apresentados como aprovados.

# RF01/RF24 — username e cadastro Google unificado — 02/10/2026

## 1. Resumo executivo

Implementado o cadastro convencional RF01 com username obrigatório, documentos, validações compartilhadas de idade/papel/subtipo/telefone/responsável e perfil inicial transacional. Primeiro acesso Google passa a emitir somente contexto temporário; a persistência definitiva ocorre em uma rota própria, após concluir RF01. RF26 e RF27 conservam seus mecanismos e transições.

Testes focados finais: **113 total / 113 passed / 0 failures / 0 errors / 0 skipped, BUILD SUCCESS**. Suíte completa: **842 total / 824 passed / 1 failure / 0 errors / 17 skipped, BUILD FAILURE exclusivamente pelo RF06 conhecido**. As 33 failures e o 1 error de cadastro/autenticação do baseline desapareceram; foram adicionados 51 testes.

**Frontend alterado nesta tarefa: NÃO. Banco/SQL alterados: NÃO. RF06 alterado: NÃO. Commit/push/staging: NÃO.**

## 2. Fontes, versão e auditoria anterior

- Checkout: `C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline`; branch `integracao-recuperada-2026-09-15`; HEAD `a6164e38b177c765638d61eb0d68c4b46f79f052`.
- Documento lido: `C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt`. Confirmados **45 RFs, RF01–RF45, e 19 RNFs, RNF01–RNF19**. SHA-256: `3e7ca8fa7e78a1967cf5f86c86f6a60f736879b02cfbd8f9d195040fea873183`.
- Lidos RF01 integral, RF02, RF24–RF27 e RNF01/08/10/19; considerados os limites relacionados de RF09/RF29. Lidos o relatório database04 e os relatórios RF26, RF27 e correção de raio. Referências históricas nesses relatórios não foram usadas como estrutura ativa.
- Estrutura única: `database04/palco-database`. ZIP oficial conferido novamente: `52b1c4af06d79a7efae47e6fa320b4a0a32359efaae1d40e2a73d06129c6df2b`.
- Consulta inicial: `graphify query "RF01 cadastro username AuthService Google RF24 EmailVerificationService GuardianConsentService database04" --budget 2400`; o subgrafo orientou a leitura de DTOs, controller, serviços, entidades, repositories, política de acesso, testes e SQL database04.
- Baseline do working tree registrado por hashes antes da implementação. O diff contra HEAD inclui trabalho anterior e não deve ser atribuído integralmente a esta tarefa.
- Baseline funcional informado e corroborado no relatório database04: **791 / 739 / 34 / 1 / 17** (total/passed/failures/errors/skipped).

## 3. Conflito documental RF24 encontrado

O documento revisado ainda contém, na linha 2189:

> Se não existir, criar conta provisória PENDENTE_TIPO_PERFIL com nome, e-mail verificado, foto e googleId, mantendo senha local nula.

A descrição (linha 2145), saída (2209) e decisões sobre acesso provisório/eliminação em 30 dias (2243–2245) também pressupõem essa persistência. O conflito foi explicitado antes da implementação e o usuário reafirmou expressamente a precedência da decisão funcional posterior.

O arquivo está fora do checkout e não é um documento versionado deste repositório. Não foi editado. Não foi encontrado arquivo apropriado de decisões consolidadas já rastreado. Esta seção e a seguinte constituem o registro explícito da decisão nesta entrega, sem reescrever relatórios históricos.

## 4. Decisão consolidada RF24 — fluxo unificado Google

**Google validado no backend → contexto temporário seguro → conclusão dos campos RF01 → validação do contexto e dos dados no servidor → uma transação cria Usuario, perfil e associações.**

Google substitui senha local e confirmação de propriedade do e-mail. Não substitui username, idade, tipo/subtipo, telefone, CPF/CNPJ, área principal, responsável ou nome de entidade quando aplicável.

Nenhum novo usuário recebe `PENDENTE_TIPO_PERFIL`. Adulto apto é criado em `ATIVA`, ainda com `perfilCompleto=false`; artista de 14–17 anos é criado em `PENDENTE_CONSENTIMENTO`, sem sessão normal, e segue RF27. Estados legados pendentes continuam bloqueados, sem migração automática.

**Atualização formal necessária no RF24:** substituir a descrição/etapa/saída de criação provisória por emissão de contexto sem linha em `usuarios`; incluir username entre as entradas de conclusão; substituir “acesso de conta provisória” por “contexto restrito à conclusão”; retirar a regra de eliminação em 30 dias para novos cadastros, que não criam contas abandonadas. Preservar as demais regras de verificação do provedor, linking, idade, documentos, consentimento, avatar, senha nula e RF25. A política de eventual saneamento de contas legadas não foi implementada.

## 5. Conflitos técnicos encontrados e tratamento

| Evidência anterior | Tratamento nesta tarefa |
|---|---|
| Usuario mapeava username obrigatório, mas DTO/cadastro não o recebiam | Campo validado e persistido explicitamente nos dois fluxos |
| CPF/CNPJ existiam em Usuario, sem validação/entrada no cadastro | Validação de formato e dígitos via Hibernate Validator, exigência por subtipo e unicidade |
| Telefone validava somente presença/tamanho | Validação de formato brasileiro com DDD, sem serviço de propriedade |
| AuthService exigia raio; database04 permite NULL | Cadastro inicial não exige nem preenche raio; testes HTTP provam NULL persistido |
| Google criava usuário provisório; dados de conclusão vinham na autenticação | Separação entre autenticação Google e conclusão RF01 |
| Política Google ligava acesso ao perfil profissional completo | Adulto com RF01 inicial persistido pode acessar, mantendo RF08 incompleto; contas realmente incompletas continuam barradas |
| Hibernate podia registrar detalhes PostgreSQL com valores da linha | Desabilitada a categoria de log de erros JDBC que imprime esses detalhes; respostas continuam sanitizadas |

## 6. Arquivos backend alterados

Produção (paths relativos a `backend/src/main/java/com/portifolio`):

| Arquivo | Finalidade |
|---|---|
| `dto/CadastroDadosRequest.java` (novo) | Campos comuns RF01, username, telefone, documentos e responsável |
| `dto/CadastroRequest.java`, `dto/CadastroResponse.java` | Cadastro local herda dados comuns; resposta inclui username |
| `dto/GoogleAuthRequest.java`, `dto/GoogleAuthResponse.java` | Autenticação apenas por ID Token/rememberMe; resposta de contexto |
| `dto/GoogleCadastroRequest.java` (novo) | Dados de conclusão e contexto; rejeita campos extras de identidade/estado |
| `validation/CadastroValidator.java` (novo) | Regras comuns de negócio e unicidade prévia |
| `service/AuthService.java` | Criação transacional compartilhada, linking e conclusão Google |
| `service/google/GoogleRegistrationContextService.java` (novo) | Assinatura/validação do contexto de 10 minutos |
| `service/google/GoogleLinkLock.java` | Reuso do lock PostgreSQL, chave de e-mail com caixa normalizada |
| `controller/AuthController.java`, `config/SecurityConfig.java` | Rota pública específica de conclusão, com verificação própria e origem |
| `repository/UsuarioRepository.java` | Consultas de unicidade de username/CPF/CNPJ |
| `security/GoogleAccountAccessPolicy.java` | Distingue cadastro inicial de completude profissional |
| `security/UserDetailsServiceImpl.java` | Usa a mesma política ao validar acesso autenticado |
| `realtime/ChatRealtimeService.java`, `realtime/NotificacaoRealtimeService.java` | Apenas injeção da política compartilhada; nenhuma funcionalidade de chat/notificação nova |
| `model/Usuario.java` | Apenas retirada do comentário superado; mapeamento database04 preservado |

Configuração: `backend/src/main/resources/application.properties`, somente categoria `logging.level.org.hibernate.orm.jdbc.error=OFF`.

Testes alterados: `AuthControllerRf01Rf02IntegrationTest`, `EmailVerificationRf26IntegrationTest`, `GoogleAuthRf24HardeningIntegrationTest`, `GuardianConsentRf27IntegrationTest`, `UsuarioControllerIT.java` (classe `UsuarioControllerIntegrationTest`) e `RealtimeAccountAccessTest`.
Novos: `CadastroRf01Rf24IntegrationTest`, `GoogleRegistrationContextServiceTest` e `support/CadastroFixtures`.

## 7. Frontend e banco

**Frontend alterado: NÃO.** Inspeção limitada a `palco-comunidades-agenda/src/pages/registration/contracts.js`, `src/lib/auth.js` e `src/pages/AuthScreens.jsx`. O payload atual não inclui username/documentos; Google está desabilitado/não configurado. A compilação frontend não depende estaticamente dos DTOs Java, portanto não foi necessária alteração técnica.

**Banco alterado: NÃO.** Nenhum arquivo SQL, tabela, coluna, enum, constraint, índice, trigger, função, procedure, seed ou migration recebeu alteração. Testes usam DML de fixture em PostgreSQL descartável inicializado exclusivamente pelo pacote database04 e `ddl-auto=validate`. Não foi operado o banco local de desenvolvimento. SQL histórico permaneceu intacto.

## 8. Contrato de username

Database04: `usuarios.username varchar(30) UNIQUE NOT NULL`; CHECK `username ~ '^[a-zA-Z0-9._]+$'`.

- Backend aceita exatamente letras ASCII maiúsculas/minúsculas, números, ponto e sublinhado, de **1 a 30 caracteres**, sem espaços.
- O símbolo `@` é apresentação visual, não integra o valor armazenado. Não se inventou tamanho mínimo maior nem regex diferente.
- Não há trim, lowercase, derivação do e-mail, geração automática ou reserva antecipada de username.
- **Diogo e diogo são diferentes**, conforme UNIQUE comum case-sensitive. Nenhum índice case-insensitive foi criado.
- Duplicidade pré-detectada: 409 contextual; corrida que alcança a constraint: 409 sanitizado do handler existente, nunca SQL bruto.
- Username é identificador público retornado no cadastro, não ID interno e não credencial. Login continua e-mail/senha. Edição futura pertence ao RF08.

## 9. Cadastro convencional

`POST /api/auth/cadastro` recebe nome, e-mail, senha e dados RF01. Após validação de campos/regras, cria Usuario, perfil inicial, área principal/associações e responsável quando cabível na mesma transação. Responde **201**, sem senha/hash/documentos/responsável/JWT.

A conta nasce em `PENDENTE_VERIFICACAO_EMAIL`, `emailVerificado=false`, `perfilCompleto=false`. RF26 gera token, persiste apenas hash e envia. Falha de entrega inicial mantém a política anterior: 503 e rollback de todo cadastro.

## 10. Idade e papéis

Idade calculada no backend a partir de nascimento e Clock. Futuro ou ARTISTA <14: 400. ARTISTA 14–17 exige responsável; adulto segue fluxo adulto. CONTRATANTE <18: 400. ADMIN/MODERADOR não podem autocadastrar-se. Estúdio/Produtora exigem representante adulto conforme regra já expressa no RF24 revisado.

Foi preservado o padrão existente de 400 para essas regras de cadastro; não houve remodelagem global para 422. Subtipo de contratante ausente/incompatível conserva 422 do adaptador existente.

## 11. Responsável e privacidade

Menor exige nome, telefone válido e e-mail válido de responsável. E-mail deve ser diferente do menor, comparado sem diferença de caixa e com trim somente na comparação. Associação normalizada é criada junto ao usuário. Respostas de cadastro/conclusão não expõem os dados do responsável.

O consentimento real permanece RF27: informar dados não equivale a autorizar. Não foi implementada alteração de perfil público nem descoberta.

## 12. Senha local — RNF01/RNF19

Cadastro convencional reutiliza `PasswordPolicy.encode` e BCrypt, sem trim/normalização da senha. Mantidos limites e categorias RNF19, bem como a ressalva já existente de **72 bytes** do BCrypt para senhas UTF-8 multibyte (422 seguro, sem persistência parcial). Testes cobrem categorias inválidas e preservação de espaços.

Google-only permanece com `senha=NULL`, protegido pelo vínculo Google oficial. Não há senha gerada automaticamente nem aplicação RNF19 a quem não cria senha local.

## 13. Subtipos, documentos, área e telefone

| Perfil | Dados específicos |
|---|---|
| ARTISTA_SOLO, DUPLA, BANDA, GRUPO_ARTISTICO | CPF do titular/responsável e uma área principal |
| ESTUDIO, PRODUTORA_EMPRESA | CNPJ, representante >=18 e uma área principal |
| CONTRATANTE PESSOA_FISICA | CPF e idade >=18 |
| CONTRATANTE SETOR_PUBLICO, SETOR_PRIVADO, ONG | CNPJ, nomeEntidade e idade >=18 |

CPF/CNPJ: formato numérico puro ou máscara convencional, dígitos verificadores, rejeição de sequência repetida; armazenamento sem máscara. Documento opcional adicional, quando informado, também é validado. Nenhuma consulta governamental foi criada.

Cadastro de ARTISTA cria exatamente a área principal escolhida. Não cria funções/especializações/múltiplas áreas, não exige foto/portfólio/raio e não marca perfil completo. Nome de entidade de contratante é persistido em `perfis_contratantes.nome_empresa`; bio continua opcional, sem endereço inventado.

Contrato técnico de telefone: DDD brasileiro de dois dígitos e número de oito/nove dígitos, com representação compacta ou parênteses/espaço/hífen e prefixo 55/+55 opcional, até 20 caracteres. O requisito anterior não especificava regex; esse formato é documentado explicitamente. Não há SMS, OTP, WhatsApp ou comprovação de propriedade. Telefone permanece privado.

## 14. Google — usuário existente

`POST /api/auth/google` valida o ID Token pelo verificador oficial já existente (assinatura, issuer, audience, expiração e e-mail verificado). Depois resolve e bloqueia e-mail/sub no servidor. Tokens inválidos e e-mail inválido recebem 401 genérico.

Mesmo sub reutiliza exatamente o usuário. Mesmo e-mail verificado pode vincular conta local sem substituir hash, papel, nome ou perfil. Sub e e-mail apontando contas diferentes, e-mail divergente do vínculo ou tentativa de substituir outro googleId são rejeitados sem merge.

Google pode concluir apenas a verificação de e-mail. `PENDENTE_CONSENTIMENTO`, `PENDENTE_TIPO_PERFIL` legado e `BLOQUEADA` não recebem acesso normal. Campos adicionais de cadastro enviados à autenticação não alteram a conta existente.

## 15. Google — novo usuário e API de conclusão

| Etapa | Contrato |
|---|---|
| `POST /api/auth/google` | `{idToken, rememberMe}`; 200 `AGUARDANDO_DADOS`, nomeGoogle/emailGoogle/fotoGoogle, contexto e contextoExpiraEmSegundos=600; sem ID persistido, statusConta, access token ou refresh |
| `POST /api/auth/google/cadastro` | `{contexto, username, tipoUsuario, dataNascimento, telefone, cpf/cnpj, tipoPerfilArtistico/ tipoPerfilContratante, areaPrincipalId, nomeEntidade, dados do responsável quando aplicáveis}` |
| Conclusão adulta | 201, `AUTENTICADO`, `ATIVA`, JWT; perfil profissional continua incompleto |
| Conclusão de menor | 201, `AGUARDANDO_DADOS`, `PENDENTE_CONSENTIMENTO`; convite RF27, sem acesso normal |
| Dado inválido | 400; subtipo contratante incompatível 422 conforme padrão existente |
| Contexto inválido/adulterado/expirado | 401 genérico |
| Username/e-mail/documento em conflito ou conclusão já realizada | 409 sem detalhes SQL |

`rememberMe` é vinculado ao contexto na primeira etapa. Conclusão não aceita e-mail, sub/googleId, emailVerified, nome/foto de provedor, senha, status ou outros campos extras como autoridade: rejeita-os. Respostas com contexto/sessão usam `Cache-Control: no-store`; origem é validada pelo mecanismo existente.

## 16. Proteção do contexto temporário

Reutilizadas JJWT 0.12.6, a configuração de segredo já existente, Clock e locks transacionais PostgreSQL. **Nenhuma tabela/coluna nova.**

- JWS HS256 com validade de **10 minutos**, issuer/finalidade próprios, jti aleatório e verificação server-side de assinatura, finalidade e datas.
- Chave específica derivada por HMAC-SHA256 do segredo configurado e da finalidade `palco:google:cadastro:v1`. JWT normal não valida como contexto, nem contexto valida como JWT de sessão.
- Conteúdo limitado à identidade Google validada (sub, e-mail, nome, foto quando disponível) e rememberMe; não carrega CPF/CNPJ, senha, nascimento ou responsável.
- Estado é stateless entre instâncias com o mesmo segredo. A conclusão exige ausência de conta para sub/e-mail sob locks transacionais; depois do primeiro commit, replay e outros contextos da mesma identidade retornam 409 e não emitem outra sessão.
- As constraints únicas oficiais continuam sendo a última barreira de concorrência. Falha/rollback permite tentar novamente enquanto o contexto ainda for válido.
- JWS é assinado, não cifrado: o próprio cliente pode ler os dados da sua identidade. Deve ser mantido em memória, enviado no corpo via HTTPS e nunca colocado em URL/log/localStorage.
- Não existe revogação individual do contexto antes da criação. Expiração curta e rotação do segredo limitam esse caso; revogação distribuída específica exigiria infraestrutura futura. Exclusão/recriação e política de linking após mudança de e-mail são escopos próprios.

## 17. Ausência de persistência provisória e transação

Testes verificam contagem de `usuarios` antes/depois de autenticação Google isolada, inclusive com payload antigo preenchido e chamadas concorrentes: **zero novas linhas**. Username não é reservado nessa etapa.

Conclusão grava Usuario, perfil e área/responsável na mesma transação. Teste injeta falha ao salvar perfil depois de inserir Usuario e confirma rollback integral, inclusive associação; o mesmo contexto pode completar depois. Concorrência de dois contextos válidos para o mesmo Google resulta em **uma criação 201 e um conflito 409**.

## 18. Linking, acesso e avatar

A autorização é relida do banco em login, JWT, refresh e canais em tempo real. A política compartilhada permite RF01 adulto inicial válido sem confundir `perfilCompleto=false` com uma pendência RF08. Perfil inicial ausente, área/subtipo/documento ausentes em conta Google incompleta continuam sem acesso; contas legadas aptas conservam o comportamento anterior.

Foto validada do Google é gravada em `usuarios.foto_perfil_url` na criação definitiva. Se ausente, cadastro funciona e AvatarService fornece fallback. Linking não substitui uma foto já existente. Nenhum upload foi implementado.

## 19. RF26 preservado

Cadastro convencional com username continua pendente, sem login normal. Geração/hash/validade/uso único/reenvio/rate-limit/SMTP e rollback inicial não foram reimplementados nem flexibilizados. Confirmação adulta ativa; confirmação do menor inicia RF27. Google novo verificado não chama o envio RF26 nem gera token de verificação. Linking seguro pode satisfazer RF26 de conta convencional, preservando outras pendências.

## 20. RF27 preservado

Convencional menor: cadastro definitivo com username → RF26 → `PENDENTE_CONSENTIMENTO` → convite → autorização/recusa conforme serviço existente.

Google menor: contexto → dados RF01 completos → cadastro definitivo pendente de consentimento → convite RF27 → autorização. Repetir login Google antes do consentimento não recria convite nem emite sessão. Consentimento não exige raio nem marca RF08 completo. Falha SMTP do responsável conserva a política RF27 de conta pendente e reenvio; não equivale ao rollback de falha SMTP inicial RF26.

## 21. Segurança, concorrência e observabilidade

- Validação no servidor e DTOs distintos impedem promover identidade/estado enviados pelo frontend.
- Unicidade não depende só de pre-check: teste força a consulta a não detectar username ocupado e comprova 409 pela constraint real, sem conta/perfil extra.
- Lock de e-mail compartilhado entre cadastro local e Google preserva comparação case-insensitive de e-mail; username continua case-sensitive.
- RF25 permanece em refresh cookie HttpOnly/Secure/SameSite=Strict, nunca refresh no JSON; só após cadastro apto.
- Não foram adicionados logs de senha, BCrypt, documentos, responsável, JWT, ID Token, contexto ou RF26. Erros JDBC detalhados são suprimidos para evitar que PostgreSQL revele valores da linha; o handler global existente continua retornando 409 genérico.
- Privacidade comprovada nas respostas de cadastro/conclusão; nenhuma mudança na superfície pública de perfis.
- HTTPS, configuração real Google e SMTP precisam de homologação de ambiente. Não se declarou cobertura percentual RNF10 nem conformidade jurídica automática.

## 22. Testes focados finais

| Classe | Total | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| AuthControllerRf01Rf02IntegrationTest | 16 | 16 | 0 | 0 | 0 |
| EmailVerificationRf26IntegrationTest | 11 | 11 | 0 | 0 | 0 |
| GoogleAuthRf24HardeningIntegrationTest | 19 | 19 | 0 | 0 | 0 |
| GuardianConsentRf27IntegrationTest | 10 | 10 | 0 | 0 | 0 |
| UsuarioControllerIntegrationTest | 1 | 1 | 0 | 0 | 0 |
| CadastroRf01Rf24IntegrationTest (novo) | 47 | 47 | 0 | 0 | 0 |
| GoogleRegistrationContextServiceTest (novo) | 4 | 4 | 0 | 0 | 0 |
| GuardianConsentServiceTest | 3 | 3 | 0 | 0 | 0 |
| RealtimeAccountAccessTest | 2 | 2 | 0 | 0 | 0 |
| **Total** | **113** | **113** | **0** | **0** | **0** |

Comando no diretório `backend`:

```powershell
.\mvnw.cmd '-Dtest=AuthControllerRf01Rf02IntegrationTest,EmailVerificationRf26IntegrationTest,GoogleAuthRf24HardeningIntegrationTest,GuardianConsentRf27IntegrationTest,UsuarioControllerIntegrationTest,CadastroRf01Rf24IntegrationTest,GoogleRegistrationContextServiceTest,GuardianConsentServiceTest,RealtimeAccountAccessTest' '-Dspring.test.mockmvc.print=NONE' test
```

**BUILD SUCCESS**, exit 0, 1m22s. A execução focada anterior passou 110/110 antes das três provas adicionais. Log: `backend/target-maven/rf01-focused-final.log`. A flag de impressão somente evita despejar payloads de autenticação; não desabilita testes ou assertions.

A primeira tentativa de compilação no sandbox não conseguiu usar corretamente o ambiente/classes. Execução autorizada fora do sandbox com JDK 21.0.11 passou; não foram “corrigidas” classes não relacionadas por esse erro de ambiente.

## 23. Maven completo

Executado em `backend`, com JDK **21.0.11** e PostgreSQL 18 descartável/Testcontainers, mantendo o carregamento oficial database04 e `ddl-auto=validate`:

```powershell
.\mvnw.cmd test '-Dspring.test.mockmvc.print=NONE'
```

Terminou em **6m07s**, exit **1**, **BUILD FAILURE**. Não houve erro adicional de cadastro, Google, consentimento, recuperação, JWT, sessão persistente ou canais em tempo real. Os **59 XMLs Surefire** confirmam os totais; nenhum teste crítico foi desabilitado. Os 17 testes locais/oficiais condicionais continuam skipped, sem habilitação artificial.

Única falha: `CandidaturaControllerRf06IntegrationTest.constraintRealTrataDuplicidadeMesmoSePreConsultaNaoEncontrar`, linha 463: **esperava 409, recebeu 201**. É exatamente a expectativa legada de UNIQUE global vaga/artista, expressamente fora do escopo. O teste e a implementação RF06 foram preservados.

Evidências: `backend/target-maven/rf01-full.log`, `backend/target-maven/database04/surefire-reports/` e `evidencias/rf01-rf24-2026-10-02/maven-completo.json`.

## 24. Totais e comparação

| Execução | Total | Passed | Failures | Errors | Skipped | Resultado |
|---|---:|---:|---:|---:|---:|---|
| Baseline database04 | 791 | 739 | 34 | 1 | 17 | BUILD FAILURE |
| Focados finais | 113 | 113 | 0 | 0 | 0 | BUILD SUCCESS |
| Completo final | **842** | **824** | **1** | **0** | **17** | BUILD FAILURE somente RF06 |
| Delta completo versus baseline | **+51** | **+85** | **−33** | **−1** | **0** | Cadastro/autenticação recuperados |

Distribuição anterior → final: AuthController RF01/RF02 **9→0 failures**; RF26 **9→0**; Google **5→0**; RF27 **10→0**; UsuarioController **1→0 errors**; RF06 **1→1 failure**, sem intervenção.

## 25. Conflitos remanescentes e RF06

A expectativa legada de UNIQUE global `(vaga_id, artista_id)` em RF06 é incompatível com a recandidatura revisada. Nenhum ajuste nesse requisito, serviço, repository ou teste foi feito. Não foi criada constraint para obter verde. Essa é a única falha da suíte completa, identificada na seção 23.

O texto formal RF24 ainda precisa da atualização descrita na seção 4. Escopos de RF08, descoberta/perfil público e demais RFs não foram implementados. Relatórios históricos database04/RF26/RF27 conservam seus relatos anteriores.

## 26. Riscos e limites

1. SMTP real, Google real e navegação não foram homologados; testes de integração usam verificador Google controlado e remetentes de captura, além da regressão do adaptador oficial existente.
2. Envio de e-mail dentro da transação mantém a janela preexistente entre entrega e commit. Outbox exigiria escopo estrutural distinto.
3. Contexto é bearer de curta duração; HTTPS e armazenamento somente em memória do frontend são necessários. Após expirar, repetir autenticação Google. Antes do commit, tentativas inválidas não “consomem” o contexto.
4. Rate-limit RF26/RF27 permanece como já implementado. Nenhuma nova infraestrutura de limitação distribuída foi adicionada.
5. Documento válido significa formato/dígitos/unicidade, não identidade civil ou propriedade governamental; consentimento prova controle do e-mail informado, não parentesco.
6. Metas de cobertura de linhas RNF10 não foram medidas. A conclusão deste escopo não equivale a suíte global verde nem entrega completa da plataforma.
7. O working tree contém muitas alterações anteriores. Fazer staging global misturaria tarefas.

## 27. Pendências frontend — integração de 14–21/10

1. Adicionar campo visual @username aos cadastros ARTISTA/CONTRATANTE; enviar valor sem @, sem lowercase/derivação automática; tratar 400 e 409.
2. Incluir CPF/CNPJ e nomeEntidade conforme subtipo; enviar tipo artístico/contratante, nascimento, telefone, uma área principal e responsável quando necessário.
3. Remover obrigatoriedade de raio do cadastro; RF08 fica para edição profissional futura.
4. Configurar provedor Google no login/cadastro. Enviar ID Token oficial a `/api/auth/google`; não construir identidade Google localmente.
5. Para `AGUARDANDO_DADOS` com contexto, abrir conclusão RF01; manter contexto em memória por até 600 s, sem sessão/painel normal.
6. Enviar somente dados RF01 e contexto a `/api/auth/google/cadastro`. Não reenviar e-mail/sub/nome/foto/emailVerified/senha/status nem rememberMe nessa etapa.
7. Tratar 201 adulto com sessão; 201 menor com espera/reenvio RF27; 401 de contexto exige nova autenticação Google; 409 exige correção de conflito ou novo login.
8. Manter rememberMe na primeira etapa, cookies RF25 com credenciais de navegador, estado RF26 no convencional e fallback de avatar RF29.
9. Homologar jornada real ponta a ponta, CORS/HTTPS, SMTP e uso real do provedor. Nenhuma integração visual foi antecipada nesta entrega.

## 28. Conclusão de RF01 backend

**SIM, RF01 pode ser considerado concluído no backend dentro dos critérios desta tarefa.** Username obrigatório/formato/tamanho/case/unicidade/persistência, cadastro convencional, papéis/idade/responsável, senha RNF19, CPF/CNPJ, telefone, área principal, transação e regressões RF26/RF27 estão cobertos e verdes. Nenhuma alteração de banco ocorreu.

Isso não declara o frontend integrado nem a suíte global verde. O requisito geral de BUILD SUCCESS do RNF10 permanece condicionado à resolução separada do RF06; a conclusão deste escopo observa a exceção explícita autorizada pelo usuário para esse único failure legado.

## 29. Estado do RF24 revisado

**Concluída a parte revisada de primeiro cadastro Google no backend.** O token é validado no servidor; conta existente é reutilizada/vinculada com as pendências preservadas; usuário novo não é persistido incompleto; contexto não autentica endpoints normais; dados RF01 e username são exigidos antes do commit; senha permanece nula; RF26 não é repetido; menor segue RF27.

RF24 como entrega integral ainda depende de configuração/homologação Google real, integração das telas e atualização formal do documento. Não foi implementado um Authorization Code Flow novo; foi preservado o contrato de ID Token e o verificador oficial já existentes nesta instalação.

## 30. Preservação, Graphify e Git

Comparação por SHA-256 com o baseline: **21 arquivos existentes alterados e 7 arquivos novos de backend/testes**. Nenhum arquivo existente foi removido. Foram verificados **570 arquivos frontend e 139 arquivos SQL**, todos preservados; serviço, repository e teste RF06 também permanecem com os mesmos bytes. Evidência com lista específica: `evidencias/rf01-rf24-2026-10-02/preservacao-e-delta.json`. O arquivo `evidencias/database04-2026-10-02/spring-final.log` estava indisponível para hash no baseline e não é usado como prova de preservação; nenhum arquivo funcional depende dessa exceção.

O ZIP e cada entrada do pacote foram comparados novamente: **46/46 idênticos, zero divergências**. Evidência: `evidencias/rf01-rf24-2026-10-02/database04-preservado.json`.

`graphify update .` concluiu com exit 0, extração AST sem LLM/API: **5559 nós, 17969 arestas, 332 comunidades**. Atualizou grafo/HTML/relatório/manifestos/cache e backup datado: **10 derivados existentes modificados e 30 novos**. O HTML usa visão agregada porque o grafo excede 5000 nós. Nenhuma reetiquetagem com LLM foi solicitada; nomes de comunidades podem precisar de revisão posterior. Esses derivados são separados dos 28 arquivos de código/testes.

Verificação dos logs finais de focados/completo: **0 JWTs/contextos assinados, 0 hashes BCrypt, 0 dumps de body e 0 detalhes de linhas/chaves PostgreSQL** detectados pelos padrões auditados. Os relatórios JSON de evidência contêm apenas metadados/totais, sem tokens ou propriedades sensíveis das JVMs.

`git status --short`, `git diff --stat` e `git diff --check` executados no fechamento; diff-check exit 0, sem erros. O diff total contra HEAD registra **126 arquivos, 3861 inserções e 1856 exclusões**, incluindo as alterações anteriores; untracked não aparecem no diff-stat. A lista específica desta tarefa está em `preservacao-e-delta.json` e a saída integral dos três comandos em `evidencias/rf01-rf24-2026-10-02/git-final.txt`.

## 31. Grupos lógicos para checkpoint futuro

Somente sugestão; nenhum staging/commit/push:

1. Backend RF01/RF24: DTOs, validação, AuthService, contexto Google, controller/configuração e política compartilhada.
2. Regressões: fixtures RF01, novos testes de integração/contexto e atualização dos cinco grupos obrigatórios; adaptação de construtores no teste realtime.
3. Documentação: este relatório, adição ao registro semanal e evidências sanitizadas.
4. Derivados Graphify: grafo/relatório/HTML/manifesto/cache/backup em grupo separado, revisando o que é apropriado versionar.
5. Manter fora desses grupos os arquivos RF06, SQL, frontend e demais alterações preexistentes, salvo checkpoint próprio autorizado.

## 32. Próximo passo

Integrar os contratos frontend entre 14–21/10 e homologar Google/SMTP reais. Tratar a expectativa RF06 em tarefa separada de recandidatura, sem adicionar unicidade física global incompatível. Atualizar formalmente o RF24 e só então avaliar checkpoint controlado.

## 33. Registro semanal — 02/10/2026

Objetivo: fechar RF01 no backend e adequar primeiro cadastro Google. RFs: RF01 + parte cadastral RF24; regressão RF02/RF26/RF27, com proteção RF09/RF25/RF29 e RNF01/08/10/19.

Backend: username, documentos, telefone, regras comuns, contexto Google, conclusão transacional, política de acesso e testes. Frontend: **NÃO**. Banco: **NÃO alterado**. Segurança: unicidade real, identidade Google assinada, idade/responsável, rollback, privacidade e separação de sessão/contexto.

Testes: focados 113/113; completos registrados nas seções 23–24. Pendências: integração frontend, homologação Google/SMTP, atualização RF24 e RF06 separado. Registro também acrescentado a `REGISTRO_SEMANAL_2026-10-02.md`, preservando o relato anterior.

## Referências técnicas consultadas

- Context7: `resolve_library_id` → `/jwtk/jjwt`; `query_docs` sobre verificação de JWS, assinatura, finalidade e expiração. Resultado documental de `main`; versão instalada **0.12.6**, validada por compilação/testes, sem atualização de dependência. [JJWT oficial](https://github.com/jwtk/jjwt).
- CPF/CNPJ reaproveitam as constraints da biblioteca já presente; referência [Hibernate Validator 9.0 — country specific constraints](https://docs.hibernate.org/validator/9.0/reference/en-US/html_single/#section-builtin-constraints). A referência documental não substitui a versão efetivamente instalada no runtime.

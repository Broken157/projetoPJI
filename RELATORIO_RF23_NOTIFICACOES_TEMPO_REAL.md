# PJI Palco — Relatório técnico do RF23: Sistema de Notificações em Tempo Real

## 1. Objetivo

Auditar e finalizar exclusivamente o RF23, adaptando o backend à tabela oficial `notificacoes`, oferecendo persistência, consulta paginada, controle de leitura, WebSocket/STOMP autenticado, fallback SSE seguro e integração mínima com os fluxos reais RF06, RF25, RF31 e com o painel RF11. RF24, RF27, RF22, convites e qualquer mensageria externa permaneceram fora do escopo.

## 2. Estado inicial

Antes de qualquer alteração, a suíte completa foi executada com `mvnw.cmd '-Dspring.jpa.show-sql=false' test`: 242 testes executados, 242 aprovados, 0 falhas, 0 erros, 0 ignorados e `BUILD SUCCESS`.

A auditoria inicial encontrou a tabela SQL oficial, mas nenhuma camada Java funcional de notificações. O `pom.xml` não possuía WebSocket/STOMP; o `SecurityConfig` protegia as APIs por JWT, mas não tinha handshake `/ws`; RF06, RF25 e RF31 já tinham operações transacionais reais; e o RF11 ainda expunha notificações e mensagens como indisponíveis.

## 3. Auditoria do banco

Estrutura real em `database/sos_artistas.sql`:

| Coluna | Tipo | Restrições/default |
|---|---|---|
| `id` | `bigserial` | chave primária, implicitamente não nula |
| `usuario_destino_id` | `bigint` | `NOT NULL`, FK para `usuarios(id)`, `ON DELETE CASCADE` |
| `tipo_notificacao` | `tipo_notificacao_enum` | `NOT NULL` |
| `mensagem_alerta` | `text` | `NOT NULL` |
| `link_contexto` | `varchar(255)` | `NOT NULL` |
| `lida` | `boolean` | `DEFAULT false`; o SQL não declara `NOT NULL` |
| `data_criacao` | `timestamp` | `DEFAULT current_timestamp`; o SQL não declara `NOT NULL` |

O enum PostgreSQL oficial aceita apenas `candidatura`, `mensagem`, `convite`, `edital` e `salvo`. Não há índice explícito próprio da tabela além do índice criado pela PK, nem constraint de unicidade adicional. O banco não oferece tipos específicos para mudança/cancelamento de vaga; por isso os eventos atuais ligados a candidaturas usam o valor oficial `candidatura` e são diferenciados por mensagem e link seguros. Nenhum tipo, coluna, índice, FK, constraint, migration ou schema foi alterado.

## 4. Auditoria por camada

- **Entity:** `Notificacao` mapeia os nomes SQL reais e a FK de destinatário; `TipoNotificacaoConverter` adapta o enum Java aos valores minúsculos do enum PostgreSQL.
- **Repository:** pagina por destinatário com ordenação `dataCriacao DESC, id DESC`, conta não lidas, localiza somente por ID+destinatário e marca todas por update em lote.
- **DTO:** `NotificacaoResponse`, `NotificacaoPaginaResponse` e `NotificacaoNaoLidaCountResponse`; nenhum `Usuario`, e-mail, telefone, token ou senha é exposto.
- **Service:** `NotificacaoService` resolve o usuário exclusivamente pelo e-mail autenticado; `NotificacaoPersistenceService` persiste em `REQUIRES_NEW` e publica somente o objeto público persistido.
- **Controller:** `NotificacaoController` oferece os quatro contratos REST e SSE autenticados.
- **Security:** `SecurityConfig` permite apenas o handshake HTTP GET `/ws`/`/ws/**`; toda API REST continua autenticada. A abertura do handshake não autentica o canal STOMP.
- **WebSocket:** `WebSocketConfig`, `StompJwtChannelInterceptor`, `NotificacaoRealtimeGateway` e `NotificacaoRealtimeService` implementam endpoint, broker simples, autenticação e destino privado.
- **SSE:** `NotificacaoSseService` mantém emitters separados pelo e-mail autenticado e remove conexões encerradas/expiradas.
- **Eventos:** `NotificacaoEvento`, `NotificacaoPersistida` e `NotificacaoEventoListener` desacoplam os services de domínio e só reagem após commit.
- **Integrações existentes:** `CandidaturaService`, `VagaService` e `DashboardService` receberam somente os pontos mínimos do RF23.
- **Frontend:** `frontend/public/js/main.js` e `frontend/public/css/dashboard.css` receberam integração mínima, sem redesign nem alteração de HTML.
- **Testes:** quatro classes novas de RF23, um teste unitário de isolamento de falha e ajustes regressivos em RF11/VagaService.

Arquivos backend criados/alterados: `backend/pom.xml`; `SecurityConfig`; `WebSocketConfig`; `StompJwtChannelInterceptor`; `NotificacaoController`; três DTOs; `Notificacao`; `TipoNotificacao`; `TipoNotificacaoConverter`; `NotificacaoRepository`; três classes de evento; `NotificacaoService`; `NotificacaoPersistenceService`; três classes/gateways realtime; `CandidaturaService`; `VagaService`; `DashboardService`; e os testes RF23/RF11 relacionados.

## 5. Contrato REST

| Método e rota | Resultado | Observações |
|---|---|---|
| `GET /api/notificacoes?page=0&size=20` | `200` com envelope paginado | apenas notificações do JWT |
| `GET /api/notificacoes/nao-lidas/count` | `200` com contagem real | consulta ao banco |
| `PATCH /api/notificacoes/{id}/lida` | `204` | própria notificação; alheia/inexistente retorna `404` |
| `PATCH /api/notificacoes/lidas` | `204` | update em lote apenas do autenticado |
| `GET /api/notificacoes/stream` | `200 text/event-stream` | JWT em `Authorization`, nunca em query |

`usuarioId` não é aceito como fonte de identidade. Paginação inválida é recusada pela validação de parâmetros e ausência de JWT produz `401`.

## 6. Persistência

Os services RF06/RF25/RF31 publicam um `NotificacaoEvento` dentro da transação de domínio. `NotificacaoEventoListener` usa `@TransactionalEventListener(phase = AFTER_COMMIT)`, evitando notificação falsa quando a operação principal sofre rollback. Depois do commit, `NotificacaoPersistenceService` abre uma transação `REQUIRES_NEW`, valida o destinatário persistido, monta a Entity e salva antes de tentar qualquer push.

Falha de persistência é isolada do negócio já commitado e registrada sem dados sensíveis. Falha de WebSocket/SSE nunca remove a notificação salva. Usuário offline encontra posteriormente os dados pelo GET REST.

## 7. Lida/não lida

Toda nova Entity inicia com `lida=false`; a data é preenchida no backend e também é compatível com o default oficial. A contagem usa consulta dedicada ao banco. Marcar uma notificação já lida é idempotente e retorna `204`; notificação de terceiro não é revelada, retornando `404`. A listagem pública preserva somente `id`, `tipo`, `mensagem`, `link`, `lida` e `dataCriacao`.

## 8. WebSocket/STOMP

Foi adicionada somente a dependência oficial `spring-boot-starter-websocket`. O endpoint é `/ws`, o broker simples atende `/queue`, o prefixo de destinos de usuário é `/user` e a assinatura admitida é exatamente `/user/queue/notificacoes`. O cliente não escolhe usuário nem tópico parametrizado. Frames `SEND` do navegador são rejeitados porque RF23 é somente push servidor→cliente.

A entrega usa `SimpMessagingTemplate.convertAndSendToUser(emailPersistido, "/queue/notificacoes", dto)`. As origens permitidas são reutilizadas de `app.cors.allowed-origins`; não foi usado `*`.

## 9. Autenticação WebSocket

No frame STOMP `CONNECT`, o interceptor exige exatamente um header `Authorization: Bearer <JWT>`. `JwtService` valida formato, assinatura e expiração; `UserDetailsService` confirma que o usuário ainda existe; então um `Principal` com o e-mail persistido é associado à sessão. Sem token, Bearer malformado, token inválido ou expirado são rejeitados antes de uma sessão privada funcional. `SUBSCRIBE` fora do destino privado e qualquer `SEND` do cliente também são rejeitados.

O JWT não aparece na URL do WebSocket, no SSE, em links, em Entity ou em logs. A busca estática encontrou somente o header seguro do SSE e testes negativos que confirmam rejeição de `?token=`.

## 10. SSE

O fallback é `GET /api/notificacoes/stream`, autenticado pelo filtro JWT HTTP normal. Como `EventSource` nativo não permite configurar `Authorization`, o frontend usa `fetch()` streaming com `Authorization: Bearer ...`, interpreta os eventos `notificacao` e reutiliza o mesmo DTO e processamento visual do WebSocket. Cada emitter é registrado pelo e-mail autenticado; eventos de A não são enviados a B. Erro, timeout ou desconexão removem somente o emitter afetado e não interferem na persistência.

## 11. Eventos integrados

### RF06

Após uma candidatura válida ser salva e a transação confirmar, o proprietário persistido da vaga recebe exatamente uma notificação. Mensagem: nova candidatura para o título da vaga, sem dados privados do artista. Link interno: `dashboard-contratante.html`. Duplicidade ou candidatura inválida não cria alerta.

### RF31

Transições válidas `ABERTA→PAUSADA`, `PAUSADA→ABERTA` e `ABERTA/PAUSADA→ENCERRADA` publicam após commit uma notificação por artista candidato distinto. O próprio contratante não é destinatário; candidaturas são preservadas. Transição inválida não gera evento.

### RF25

Cancelamento válido publica, após commit, uma notificação por artista candidato distinto. A regra existente que altera candidaturas para `CANCELADA_POR_VAGA` permanece a única responsável pelo status; o RF23 não o altera de novo. Rollback do cancelamento não produz notificação falsa.

## 12. Eventos não integrados

- **RF24/mensagens:** não implementado; a infraestrutura central pode receber evento futuro, mas não há Entity, sala, contador ou WebSocket de chat Java criado pelo RF23.
- **RF27/salvos:** não implementado.
- **Convites:** a auditoria não encontrou fluxo funcional de convite no backend; nenhum comportamento foi inventado.
- **RF22/editais/comunidades:** não implementado.

Os valores do enum Java correspondem ao enum SQL oficial para permitir integração futura, mas `MENSAGEM`, `CONVITE`, `SALVO` e `EDITAL` não são gerados atualmente.

## 13. Integração RF11

`DashboardService` agora informa `notificacoes.disponivel=true` e descreve alertas reais. O JavaScript consulta a API real e substitui o conteúdo do módulo por badge, cinco itens recentes e ações de leitura. `mensagens.disponivel=false` permanece inalterado e o dashboard continua exibindo “Em breve: mensagens dependem da implementação do RF24”. Portanto, RF11 continua **PARCIAL**.

## 14. Segurança

- Identidade derivada do JWT e do usuário persistido, nunca de `usuarioId` enviado pelo cliente.
- Listagem, contagem e updates limitados ao destinatário autenticado; tentativa de marcar notificação alheia recebe `404`, evitando IDOR e enumeração.
- WebSocket autenticado no `CONNECT`; handshake HTTP aberto apenas para permitir upgrade.
- Destino `/user/queue/notificacoes`, vinculado ao `Principal`; não há tópico público por ID.
- JWT somente em headers, nunca em URL/query, Entity, DTO público ou log.
- Links são construídos pelo backend e o frontend aceita somente caminhos internos permitidos; `javascript:`, URL externa e variantes inseguras não navegam.
- Mensagens são inseridas com `textContent`. Uma notificação de QA contendo `<img src=x onerror=alert(1)>` foi exibida como texto, sem elemento injetado e sem execução.
- Não há log de `Authorization`, JWT, refresh token, senha ou texto privado desnecessário.

## 15. Performance

- Paginação obrigatória via `Pageable`: padrão 20, máximo 50.
- Ordenação estável por `dataCriacao DESC, id DESC`.
- Não existe `findAll()` ilimitado no contrato REST.
- Marcar todas usa `UPDATE` em lote, sem carregar Entities.
- Teste com 50 notificações mediu no máximo 4 statements preparados, independentemente do número de itens, fornecendo evidência anti-N+1.
- Testes reais de STOMP e SSE mediram a entrega a partir do envio e exigiram menos de 5 segundos; ambos passaram. O tempo exato não foi impresso para evitar transformar a suíte em benchmark frágil.

## 16. Banco

**Banco alterado: NÃO.**

Hashes SHA-256 finais, iguais aos registrados antes da implementação:

- `database/sos_artistas.sql`: `1711CE1FBDEDB3BA97BBACC7D820FBF7C31A78E87BFADF20FB7FE2967E0666AD`
- `database/schema-test.sql`: `202C34E7BABE0300C45FC0EF2ED96EC380A78780CED89FBDCBA7B2245CE79671`
- `database/migration_rf03.sql`: `242A56CB887EB79E5F0EC2D09BF8592F473D8EC5288543063672BB558513249C`
- `database/migration_rf25_motivo.sql`: `8920B5A1531E029B2AFE66C387A6A108AD9B326D1661E828EDFF53E7C9721B6C`
- `backend/src/test/resources/db/schema-test.sql`: `202C34E7BABE0300C45FC0EF2ED96EC380A78780CED89FBDCBA7B2245CE79671`

`application.properties`, incluindo `spring.jpa.hibernate.ddl-auto=validate`, também permaneceu inalterado: `5BD3913FF472646150F28443436195057B2B8C0722B2EA149ED5DA06D4CEF462`.

## 17. Frontend

**Frontend alterado: SIM**, apenas para notificações:

- `frontend/public/js/main.js`: consulta paginada/contagem, marcação individual/em lote, badge, preview, popup discreto, cliente STOMP com JWT no `CONNECT`, refresh já existente em 401, fallback SSE com header, whitelist de links internos e renderização segura.
- `frontend/public/css/dashboard.css`: estilos do badge, lista, estados lida/não lida, botões e popup; regra responsiva do item de notificação dentro do breakpoint existente de 760 px.

Nenhum HTML, navbar global, chat, mensagem ou redesign foi introduzido.

## 18. Baseline

| Momento | Executados | Aprovados | Falhas | Erros | Ignorados | Resultado |
|---|---:|---:|---:|---:|---:|---|
| Antes do RF23 | 242 | 242 | 0 | 0 | 0 | `BUILD SUCCESS` |
| Depois do RF23 | 261 | 261 | 0 | 0 | 0 | `BUILD SUCCESS` |

Comando final: `mvnw.cmd '-Dspring.jpa.show-sql=false' test`. Ambiente: Windows, Java 26 executando código `release 21`, Spring Boot 4.0.6, PostgreSQL 18.4 via Testcontainers/Docker Desktop.

## 19. Novos testes RF23

Foram adicionados **19 testes RF23**:

- `NotificacaoRf23IntegrationTest`: 11 testes REST/persistência/domínio cobrindo autenticação, isolamento A/B, DTO, paginação padrão/máxima, contagem, lida/IDOR/idempotência, RF06 válido/inválido/duplicado/rollback, falha de persistência pós-commit, RF31, RF25 e anti-N+1.
- `StompJwtChannelInterceptorTest`: 4 testes de `CONNECT` e autenticação/token.
- `NotificacaoEventoListenerTest`: 1 teste de isolamento quando a camada real-time falha.
- `NotificacaoWebSocketRf23IntegrationTest`: 3 testes de WebSocket privado, segurança/expiração e SSE.

Também foram atualizados `DashboardRf11IntegrationTest` para a disponibilidade real do RF23 e `VagaServiceTamanhoTest` para a nova dependência de publicação, sem reduzir cobertura anterior.

## 20. Testes WebSocket/SSE

O teste WebSocket inicia servidor real em porta aleatória, conecta dois usuários, envia `CONNECT Authorization: Bearer`, assina `/user/queue/notificacoes` e confirma que somente o destinatário recebe. Sem header, token inválido e token expirado são rejeitados. A entrega válida é medida com limite de 5 segundos.

O teste SSE usa HTTP real com `Authorization`, mantém dois streams autenticados, publica um DTO para A, confirma evento próprio em A em menos de 5 segundos e ausência de bytes em B. A API REST e o teste de persistência offline comprovam recuperação posterior. O endpoint com `?token=` sem header recebe `401`.

## 21. Testes frontend

`node` e `npm` não estão instalados no ambiente; `node --check` e `npm test -- --watchAll=false` foram tentados e falharam por executável inexistente. Nenhum framework foi instalado e TLS não foi desabilitado.

Foi executado QA automatizado no navegador real contra frontend e backend locais. Resultado: badge atualizado por uma candidatura real, popup discreto, preview, marcação individual, marcação em lote, reload consultando estado persistido, RF24 ainda indisponível, ausência de elemento HTML injetado, ausência de link `javascript:` e nenhum erro no console. A tela desktop não apresentou overflow.

O navegador expôs um recurso de viewport e aceitou a solicitação de 390×844, mas manteve `innerWidth=1280`; portanto, **não foi possível confirmar visualmente 360 px e 390 px nesta execução**. A inspeção estática confirmou breakpoint `max-width: 760px`, painéis em uma coluna, item de notificação em coluna e `overflow-wrap`, mas isso não é contabilizado como QA visual móvel.

## 22. Regressão final

Suíte completa: **261 executados, 261 aprovados, 0 falhas, 0 erros, 0 ignorados, `BUILD SUCCESS`**, em 2 min 36 s. A regressão incluiu RF03, RF04, RF05, RF06, RF07, RF08, RF09, RF10, RF11, RF25, RF31, autenticação JWT, refresh/logout e os 19 testes RF23.

O Surefire registrou ao final um aviso de encerramento forçado do JVM fork após 30 s, associado ao fechamento de contextos HTTP long-lived; o processo saiu com código 0, todos os relatórios foram concluídos e o resultado Maven foi `BUILD SUCCESS`. Não houve falha funcional ou teste ignorado.

## 23. Conflitos e decisões

- O banco usa enum PostgreSQL fechado, não `VARCHAR`. Para cumprir “banco não alterado”, mudanças/cancelamento de vaga usam o tipo oficial `candidatura`; mensagem e link distinguem o evento. Criar `VAGA_PAUSADA` etc. exigiria migration proibida.
- O evento de domínio é publicado durante a operação, mas processado somente `AFTER_COMMIT`; assim não há push sobre rollback.
- A persistência pós-commit ocorre em `REQUIRES_NEW`; falha nela não desfaz o negócio já confirmado, e falha no push não desfaz a notificação.
- O handshake `/ws` é permitido no HTTP exclusivamente para o upgrade; a autorização real ocorre no frame STOMP `CONNECT`.
- SSE usa `fetch()` streaming, não `EventSource`, para manter JWT no header.
- A dependência WebSocket foi obtida mantendo TLS ativo; foi usado o trust store `Windows-ROOT`, sem flags inseguras.

## 24. Pendências

- Reexecutar a QA visual em viewports reais de 360 px e 390 px quando o controlador de viewport estiver operacional; o desktop e as regras CSS foram validados, mas a evidência visual móvel ficou indisponível.
- Avaliar futuramente atualização do conversor Jackson usado somente no teste WebSocket, pois a classe atual está marcada como deprecated pelo Spring, sem impacto funcional presente.
- O aviso de shutdown do Surefire pode ser eliminado futuramente com isolamento mais agressivo dos contextos HTTP/SSE; não afeta os 261 resultados.
- RF24/mensagens é o próximo passo funcional; RF11 permanece parcial até ele existir.

Registro para o relatório semanal futuro (sem gerar relatório semanal agora): data `2026-08-22`; RF23; backend alterado: sim; frontend alterado: sim; banco alterado: não; eventos: RF06/RF31/RF25; WebSocket e SSE: funcionais; segurança: JWT em headers, destino privado e IDOR bloqueado; testes: 261/261; RF11: parcial; pendências: QA móvel direta e RF24; próximo passo: RF24.

## 25. Conclusão

**RF23 CONCLUÍDO.**

Os critérios essenciais foram comprovados: tabela oficial mapeada sem alteração, persistência antes do push, recuperação offline, REST paginado, contagem real, marcações idempotentes, isolamento por usuário, RF06/RF31/RF25 após commit, WebSocket/STOMP real, JWT exclusivamente no `CONNECT`, rejeição de conexão inválida, destino privado, SSE autenticado, falha de entrega isolada, badge/lista/popup seguros, integração RF11, 19 novos testes, regressão 261/261 e `BUILD SUCCESS`. A limitação de evidência visual móvel é uma pendência de QA, não uma ausência do comportamento funcional obrigatório do RF23.

# PJI Palco — Relatório Técnico do RF24: Sistema de Mensagens

## 1. Objetivo

Auditar e implementar o núcleo do chat privado direto ARTISTA ↔ CONTRATANTE, reutilizando integralmente o WebSocket/STOMP/JWT do RF23, sem alterar o schema oficial e sem implementar RF16, RF30 ou RF36 completos.

## 2. Estado inicial

Antes de qualquer edição, o projeto não possuía Entity, repository, service, controller ou página funcional para as tabelas de chat. O interceptor do RF23 autenticava o `CONNECT`, mas rejeitava todo `SEND`. O dashboard RF11 informava mensagens indisponíveis. A página `mensagens.html` não existia.

## 3. Auditoria do banco

- `salas_chat`: `id bigserial` PK e `data_criacao timestamp DEFAULT CURRENT_TIMESTAMP`; o timestamp não é declarado `NOT NULL`.
- `participantes_chat`: `sala_id bigint` FK para `salas_chat(id) ON DELETE CASCADE` e `usuario_id bigint` FK para `usuarios(id) ON DELETE CASCADE`; PK composta (`sala_id`, `usuario_id`), tornando ambos obrigatórios. Não há constraint que limite a dois participantes nem unicidade canônica do par.
- `mensagens_chat`: `id bigserial` PK; `sala_id bigint NOT NULL` FK com cascade; `remetente_id bigint NOT NULL` FK com cascade; `texto_mensagem text` nullable; `url_anexo varchar(255)` nullable; `lida boolean DEFAULT false`; `data_envio timestamp DEFAULT CURRENT_TIMESTAMP`. `lida` e `data_envio` não são declarados `NOT NULL`.
- Índices explícitos de chat: apenas os produzidos pelas PKs. Não há índice específico para histórico, não lidas ou atividade recente.
- Não há tabela de anexos, flag/timestamp de edição, flag/status de exclusão nem estrutura de anonimização.

## 4. Auditoria RF23

Foram preservados `/ws`, prefixo de aplicação `/app`, broker privado `/queue`, prefixo de usuário `/user`, JWT obrigatório no frame STOMP `CONNECT`, `Principal`, origins existentes, eventos pós-commit, notificações persistidas e o enum oficial `MENSAGEM`. Não foi criado segundo socket, broker ou interceptor.

## 5. Arquitetura do chat

`ChatService` é a autoridade única para REST e STOMP. O fluxo resolve o usuário pelo e-mail autenticado, valida a sala e seus dois participantes, valida o texto, persiste com `saveAndFlush`, publica eventos transacionais e somente após commit entrega o evento privado e cria a notificação RF23. A lista e o histórico usam projections/queries específicas.

## 6. Salas

`POST /api/chat/salas` cria ou reutiliza uma conversa. `GET /api/chat/salas?page=0&size=20` lista apenas salas do JWT, com máximo 50, ordenação por última atividade e ID, participante público, avatar, preview e não lidas. Autoconversa e papéis iguais retornam erro de negócio.

## 7. Segurança e participantes

A identidade nunca vem do payload. REST usa o principal autenticado e STOMP usa `Principal.getName()`. Sala, histórico, envio, leitura, edição e exclusão exigem participação; para reduzir enumeração, acesso alheio retorna 404. Uma sala válida do RF24 precisa ter exatamente dois participantes e papéis ARTISTA/CONTRATANTE.

## 8. Chat protegido de menor

Se qualquer integrante tiver menos de 18 anos, a criação exige candidatura persistida entre o artista e uma vaga pertencente àquele contratante. A regra vale independentemente de quem inicia. Convites ou consentimento artificial não foram inventados. Dependências futuras do RF36 permanecem fora do escopo.

## 9. Persistência das mensagens

`POST /api/chat/salas/{salaId}/mensagens` e `/app/chat/salas/{salaId}/mensagens` convergem para o mesmo método. Texto nulo, vazio, apenas espaços, acima de 4.000 caracteres ou igual ao placeholder reservado é rejeitado. A mensagem recebe sala, autor do JWT, horário do servidor e `lida=false`. Falha de persistência não gera push nem notificação; falha posterior do broker é isolada e o histórico continua disponível.

## 10. WebSocket/STOMP

O destino de envio permitido é exclusivamente `/app/chat/salas/{id}/mensagens`. A assinatura privada é `/user/queue/chat`. O interceptor continua aceitando `/user/queue/notificacoes`, rejeita assinaturas públicas, destinos arbitrários, `SEND` de notificações, IDs inválidos e frames sem principal. Os eventos carregam `salaId` e tipos de nova mensagem, leitura, edição ou exclusão.

## 11. Status de leitura

`PATCH /api/chat/salas/{salaId}/lidas` atualiza em lote apenas mensagens da sala, não lidas, recebidas pelo usuário autenticado; mensagens próprias não são marcadas por esse usuário. O autor recebe recibo privado com os IDs. Entrega pelo socket não marca leitura; o frontend chama a operação ao abrir/visualizar a conversa. `GET /api/chat/nao-lidas/count` usa contagem agregada no banco.

## 12. Edição em 15 minutos

`PATCH /api/chat/mensagens/{id}` permite somente ao autor editar mensagem não excluída até 15 minutos após `data_envio`, usando `LocalDateTime.now()` do servidor. Testes cobrem 14m59s, acima de 15 minutos, outro participante, terceiro e mensagem excluída. O texto atualizado é persistido e propagado em tempo real.

## 13. Exclusão de mensagem

`DELETE /api/chat/mensagens/{id}` permite somente ao autor e não executa hard delete. Como o schema não tem flag/status, o conteúdo é substituído por `Mensagem excluída pelo autor` e `url_anexo` é limpo; o registro, autor, sala, horário e sequência histórica permanecem. A decisão é compatível com o schema, mas elimina o texto original e não equivale a uma trilha de auditoria completa.

## 14. Anexos

Existe somente `url_anexo varchar(255)`, sem tabela, MIME real, nome, tamanho, ownership, ACL ou infraestrutura segura de upload/download. Esse campo isolado não atende áudio, imagem e documento com segurança. Anexos não foram expostos nem implementados e dependem de evolução coordenada do schema/RF16.

## 15. RF30 / anonimização

`remetente_id` é `NOT NULL` com `ON DELETE CASCADE`, e participantes também são apagados em cascade. Portanto, a estrutura não permite remetente nulo/anonimizado preservando o histórico. Não foi criada simulação no DTO ou frontend. **RF24 está funcional para usuários ativos, mas a integração futura com RF30 exige adequação coordenada da estrutura de anonimização.**

## 16. Integração RF23

Cada nova mensagem confirmada gera uma única notificação persistida para o outro participante, `tipo=MENSAGEM`, texto genérico sem conteúdo privado e link `mensagens.html?sala={id}`. O remetente não recebe notificação própria. A persistência e o push de notificação continuam no fluxo pós-commit do RF23; WebSocket de notificações, SSE e autenticação regressaram sem falha.

## 17. Integração RF11

`mensagens.disponivel` passou a `true` e `quantidadeNaoLidas` vem de consulta agregada real. O atalho abre `mensagens.html`. O frontend atualiza o badge ao receber notificação `MENSAGEM`. Os demais módulos do dashboard não foram redesenhados e RF11 não é reclassificado por esta tarefa.

## 18. Frontend

Foi criada página mínima com lista de conversas, preview, badge, histórico, estado de conexão, envio REST como fallback, envio/eventos STOMP, leitura, edição, exclusão, texto via `textContent` e disclaimer visível. O perfil público ganhou uma única ação contextual para iniciar conversa entre papéis opostos; o backend continua aplicando a regra de menor. Arquivos de `public` foram espelhados mecanicamente em `build` porque Node/npm não está disponível.

## 19. Segurança/XSS/IDOR

O teste `<img src=x onerror=alert(1)>` confirmou persistência e retorno literais; o browser exibiu o texto sem criar imagem/script. Não há `innerHTML` para mensagens. Testes A/B/C bloqueiam histórico, envio, leitura, edição e exclusão por terceiro. Spoofing de `remetenteId` não altera o autor derivado do JWT.

## 20. Performance/paginação/N+1

Salas e mensagens usam padrão 20, máximo 50, página zero e ordenação estável. Há testes com 22 salas e 23 mensagens, incluindo página seguinte. A lista de 20 salas, participante, preview e contagem permaneceu em teto constante de quatro statements Hibernate, sem repository em loop. O schema sem índices dedicados pode exigir otimização autorizada em escala maior.

## 21. Banco

**Banco alterado: NÃO.** Nenhum SQL, migration, coluna, enum, constraint ou índice foi modificado. Os hashes SHA-256 dos seis arquivos protegidos permaneceram idênticos ao baseline.

## 22. Frontend

**Frontend alterado: SIM.** Arquivos fonte: `frontend/public/mensagens.html`, `frontend/public/css/mensagens.css`, `frontend/public/js/mensagens.js`, `frontend/public/js/main.js`, `frontend/public/perfil-publico.html`, `frontend/public/css/perfil-publico.css` e `frontend/public/js/perfil-publico.js`. Os sete equivalentes em `frontend/build` foram sincronizados byte a byte.

## 23. Baseline

Comando anterior às alterações: `.\mvnw.cmd '-Dspring.jpa.show-sql=false' test`. Resultado real: 261 testes executados, 261 aprovados, 0 falhas, 0 erros, 0 ignorados, `BUILD SUCCESS`, 2min52s. O aviso conhecido de encerramento tardio do Surefire por contextos HTTP/SSE ocorreu após os resultados.

## 24. Novos testes RF24

Foram adicionados 19 testes ao total final: 15 cenários REST/integrados de chat, dois cenários adicionais do interceptor STOMP, um WebSocket real e um listener pós-commit. Cobertura: autenticação, papéis, menor com/sem candidatura, reuso nos dois sentidos, concorrência, paginação, preview, spoofing, XSS, offline, IDOR, leitura, dashboard, edição, exclusão, rollback, notificação e anti-N+1.

## 25. Testes WebSocket

Teste real em porta aleatória: **SIM**. Três clientes STOMP autenticados por JWT no `CONNECT` assinaram `/user/queue/chat`; A enviou via `/app/chat/salas/{id}/mensagens`, A e B receberam em menos de cinco segundos, C não recebeu, e mensagem/notificação foram conferidas no PostgreSQL Testcontainers. A regressão RF23 de WebSocket/SSE também passou.

## 26. Testes frontend/QA

Teste frontend automatizado Node/Jest: **NÃO**, pois `node` e `npm` não estão instalados; nenhum resultado foi inventado. QA browser local com banco descartável: **SIM** para login A/B/C, conversa, WebSocket sem reload, reload/histórico, leitura/recibo, edição, exclusão, terceiro isolado, XSS literal, disclaimer e badge RF11. Desktop foi inspecionado. O controle de viewport não aplicou 360/390 de forma confiável (continuou reportando cerca de 1910 CSS px), logo esses dois viewports não são declarados validados.

## 27. Regressão

Comando final: `.\mvnw.cmd '-Dspring.jpa.show-sql=false' test`. Resultado: **280 testes executados, 280 aprovados, 0 falhas, 0 erros, 0 ignorados, `BUILD SUCCESS`**, em 3min06s. RF03, RF04, RF05, RF06, RF07, RF08, RF09, RF10, RF11, RF23, RF25, RF31, JWT, refresh/logout e serviços existentes permaneceram aprovados. O aviso não bloqueante de shutdown do Surefire permaneceu.

## 28. Conflitos e decisões

- Sem unique constraint canônica do par, foi usado `pg_advisory_xact_lock` com chave ordenada antes de consultar/criar; isso protege concorrência por este service, inclusive entre instâncias ligadas ao mesmo PostgreSQL. Escritas externas ao service ainda podem duplicar.
- Sem estado de edição, a interface não mostra `Editada`; inferência temporal ou memória volátil seria incorreta.
- Sem estado de exclusão, foi escolhido placeholder persistido em vez de hard delete.
- `url_anexo` sozinho foi considerado suporte insuficiente e inseguro.
- FKs com cascade impedem implementar `Usuário Removido` honestamente.

## 29. Pendências

Evolução futura, mediante autorização de banco: estado/timestamp de edição; exclusão lógica auditável; estrutura segura de anexos/RF16; anonimização coordenada com RF30; vínculo/consentimento futuro do RF36; índices de atividade/histórico/não lidas após medição; teste frontend automatizado quando Node/npm existir; QA responsivo real em 360 e 390 px.

## 30. Conclusão

**Classificação: RF24 PARCIAL.** O núcleo funcional e seguro para usuários ativos está implementado e validado: sala direta, reuso concorrente, JWT, IDOR, menor com candidatura, persistência, tempo real RF23, offline, leitura, edição dentro da janela, exclusão por placeholder, RF23, RF11, frontend e regressão 280/280. A classificação não é `CONCLUÍDO` porque requisitos oficiais obrigatórios — marcador persistente `Editada`, anexos seguros e compatibilidade de anonimização com RF30 — não podem ser representados pelo schema oficial sem alteração proibida. Data: 22/08/2026. Próximo passo: aprovar uma evolução coordenada do schema para fechar essas três limitações e então repetir testes/QA responsivo.

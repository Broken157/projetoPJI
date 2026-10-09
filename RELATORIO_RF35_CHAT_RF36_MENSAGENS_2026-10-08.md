# RF35 — Chat direto e recorte Mensagens do RF36 — 08/10/2026

Relatório final validado. Escopo: backend/API/testes, sem alteração de database05, frontend, index Git ou outros RFs completos.

## 1. Resumo executivo

O database05 comporta o núcleo do chat direto: dupla, texto, leitura do destinatário, edição com data, exclusão lógica, original restrito, referência de anexo privado e anonimização do remetente. Não é necessário criar SQL para essas capacidades. Foram acrescentados busca por nome e filtros de leitura; corrigidos o processamento de leitura da sala, a contagem de excluídas e a proteção de conteúdo com moderação existente. A autorização de menor deixou de usar notificação/link textual como prova de convite.

**RF35 global: PARCIAL.** A prova relacional de convite permanece bloqueada por C03; o produtor de denúncia de mensagem, a política de evidência/versionamento/retenção e o fechamento global RF22 continuam dependências. **RF36 Mensagens: consolidado no recorte validado; RF36 global: PARCIAL.** Outros produtores/categorias e RF52 não foram implementados.

| Recorte | Estado final e limite |
|---|---|
| A — Sala direta | Consolidada para ARTISTA ↔ CONTRATANTE aptos; sala anônima remanescente serve histórico |
| B — Menor | Proteção por consentimento/conta/candidatura consolidada; convite depende C03 |
| C — Texto/histórico | Consolidado, persistente e paginado |
| D — Leitura | Consolidada; somente recebidas, excluídas fora da pendência, lotes de até 50 IDs |
| E — Lista/busca | Consolidada, filtros e paginação com contagem coerente |
| F — Edição | Consolidada para janela/autoria/marca real; evidência complexa depende RF18 |
| G — Exclusão | Consolidada como soft delete/placeholder e preservação de registros já relacionados |
| H — Evidência denunciada | Proteção de referências existentes consolidada; fluxo RF18 e política completos pendentes |
| I — Anexos | Suportados: imagem, áudio e documento PDF sob política existente; vídeo rejeitado |
| J — Realtime | Contrato privado STOMP autenticado e AFTER_COMMIT preservado |
| K — RF36 Mensagens | Persistência atômica com a mensagem, destinatário correto e entrega privada |
| L — RF22 | Anonimização já existente validada nos cenários suportados; RF22 global PARCIAL |

## 2. Branch e HEAD

Branch: `integracao-recuperada-2026-09-15`. HEAD inicial/final: `7379e94cd8c27757426976a7194ce759464b880f` (`feat: consolida RF26 RF27 e dados do RF53`). `git fetch fork` concluído; divergência `fork/integracao-recuperada-2026-09-15...HEAD`: **0/0**. Sem staging/commit/push. Checkpoint, log de 15 commits e manifestos estão em `evidencias/chat-rf35-rf36-2026-10-08/`.

Alterações preexistentes: README, `database/02_tables/04_vagas.sql`, 25 arquivos rastreados de `palco-comunidades-agenda` e arquivos não rastreados anteriores. Nenhuma delas pertence ao delta desta tarefa. Manifesto inicial: **1287 arquivos**; proteção individual por SHA-256 fora dos nove Java autorizados.

A execução começou em 08/10 e prosseguiu em 09/10/2026 (America/Sao_Paulo). O nome do arquivo e o registro semanal de 08/10 seguem o pedido; timestamps reais de cada fase constam dos JSONs de execução.

## 3. Fontes

Fonte funcional: `C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt`, SHA-256 **08FB838914D57F1C6B54F3CFDC73DB5FE1F7CD511B4DFCA5A736DC094CA20EDA**. Foram consultados integralmente RF18/RF22/RF27/RF35/RF36/RF42/RF44/RF45/RF52 e RNF05/06/08/09/10/17; interfaces atuais RF06/RF25/RF53 pelo código e testes. Extrato preservado em `requisitos-auditados.txt`.

Referências históricas: auditoria estrutural de 08/10, relatórios RF06/RF23/RF28/RF45, RF09/RF12/RF25/RF53, RF26/RF27/RF53 e RF22. Conclusões derivadas do código/database05 atual, não de rótulos documentais ou memória histórica.

## 4. Database05 auditado

Fonte estrutural exclusiva: `database05/palco-database/`. ZIP oficial SHA-256 **6158C813929AC0DB9B3B12030E8B9D84C3B647611986DD6D60B2FC50D0F97EBF**. **46/46 arquivos** correspondem byte a byte às entradas do pacote; nenhuma diferença. `database05-comparacao.json` guarda os resultados. Testes utilizam `OfficialPostgreSQLContainer`, PostgreSQL 18 e inicialização do pacote completo com **ddl-auto=validate**; nenhum schema alternativo/H2/patch de enum.

## 5. Estado inicial RF35

Já corretos: identidade Principal, papéis, ACL, lock PostgreSQL por dupla, paginação 20/50, ordem histórica, edição até 15 minutos, marcas físicas, storage privado, soft delete, persistência transacional e AFTER_COMMIT. Parciais/incorretos: busca/filtros ausentes; leitura de sala alterava só 50 recebidas por chamada; excluídas entravam nos contadores; proteção considerava reporte, mas não registro de moderação; convite por `link_contexto` era usado como autoridade para menor. O banco resolve os quatro primeiros pontos; C03 impede prova segura de convite.

| Capacidade inicial | Classificação | Evidência/ação |
|---|---|---|
| Sala/reuso/concorrência | JÁ CORRETA | Dupla ordenada, transação e advisory lock existentes |
| Autoria/ACL textual e de download | JÁ CORRETA | Principal e validações no ChatService |
| Proteção por consentimento/candidatura | JÁ CORRETA | Policies atuais e FKs profissionais reais |
| Prova de convite para menor | INCORRETA | JOIN de link textual; retirada da autorização, C03 estrutural permanece |
| Histórico/paginação | JÁ CORRETA | Projeção e ordem data+id,20/50 |
| Busca nome/filtros | AUSENTE | Implementação somente backend com campos já existentes |
| Leitura/contador | PARCIAL | Só primeiro lote e incluía excluída; corrigido |
| Marca física/janela de edição | JÁ CORRETA | editada/data_edicao e Clock/15min inclusive |
| Soft delete/evidência existente | PARCIAL | Não verificava moderacao_conteudo; guarda ampliado |
| Produtor RF18 MENSAGEM | AUSENTE | API não aceita alvo; dependência preservada, sem fluxo paralelo |
| Anexos privados atuais | JÁ CORRETA | Referência real, validator e storage/ACL existentes |
| Realtime/alerta MENSAGEM | JÁ CORRETA | Transação e listeners AFTER_COMMIT |
| Cleanup/entrega durável | DÍVIDA TÉCNICA | Callbacks existentes sem reconciliação/outbox durável |
| Política de retenção/versionamento completo | FORA DO ESCOPO | RF18/RF22/D03/D12 permanecem decisões pendentes |
| RF52 e demais categorias RF36 | FORA DO ESCOPO | Não implementados nesta tarefa |

## 6. Estado inicial RF36 Mensagens

Produtor real em `ChatService.persistirMensagem`: mensagem e notificação MENSAGEM na mesma transação. Destinatário derivado da dupla, alerta mínimo e link para a sala. `NotificacaoPersistida` dispara entrega AFTER_COMMIT; WS/SSE têm falhas isoladas. Infraestrutura existente foi preservada; não se reimplementou RF36 completo.

## 7. Arquivos auditados

Camadas diretas: ChatController, ChatWebSocketController, ChatService, ChatAnexoStorage, SalaChat, ParticipanteChat/Id, MensagemChat, DTOs Chat*, SalaChatRepository, ParticipanteChatRepository, MensagemChatRepository e projeções Chat*. Realtime: WebSocketConfig, StompJwtChannelInterceptor, ChatEventoPosCommit/Listener, ChatRealtimeService/Gateway. Notificações: NotificacaoEvento/Listener/Persistida, NotificacaoPersistenceService/Service/Repository/Controller, TipoNotificacao/Converter, NotificacaoRealtimeService e NotificacaoSseService. Interfaces: MenorAutorizadoPolicy, GoogleAccountAccessPolicy, JwtService/filtro/UserDetailsService, CandidaturaService/Repository, ConviteVagaService/Repository, DenunciaRequest/Service/Repository, ExclusaoContaService e ArquivoPortfolioValidator. Testes reais listados na evidência Maven focada e no item 58.

## 8. Arquivos alterados

Cinco Java de produção: `controller/ChatController.java`, `service/ChatService.java`, `repository/ParticipanteChatRepository.java`, `repository/MensagemChatRepository.java`, `repository/DenunciaRepository.java`, sob `backend/src/main/java/com/portifolio/`.

Quatro Java de testes: `ChatRf24IntegrationTest.java`, `ChatRf35IntegrationTest.java`, `ConviteVagaRf42IntegrationTest.java`, `DashboardRf11IntegrationTest.java`, sob `backend/src/test/java/com/portifolio/controller/`. Novo relatório e evidências próprias. Não alterados Entity/Enum/DTO físico, autenticação global, RF42 produtor, RF22 orquestrador, RF44 ou infraestrutura de storage/realtime.

## 9. salas_chat

PK `id bigserial`; `data_criacao timestamp default current_timestamp`. Sem tipo de sala e sem UNIQUE físico do par. Não há ligação com comunidade. Exclusividade da dupla é assegurada pelo backend sob advisory lock transacional; a base não impede inserções externas que ignorem esse protocolo.

## 10. participantes_chat

PK composta `(sala_id, usuario_id)`; FKs reais para sala/usuário, ambas ON DELETE CASCADE; índice por usuário. O schema não restringe cardinalidade a dois. API cria exatamente dois e exige papéis compatíveis. Histórico remanescente com um participante após RF22 é permitido; novo envio exige destinatário. Lista/contador recusam cardinalidades maiores que dois; isso não implementa RF52.

## 11. mensagens_chat

PK `id bigserial`; `sala_id NOT NULL` com FK/CASCADE; `remetente_id` nullable com FK/**ON DELETE SET NULL**. `texto_mensagem text`, `url_anexo varchar(255)`, `lida`, `data_envio`, `editada`, `data_edicao`, `texto_original`, `excluida`, `data_exclusao`. CHECK exige texto ou anexo não nulo; validação adicional rejeita vazios. Índices existentes `(sala_id,data_envio DESC)` e remetente. `lida` global é suficiente para destinatário único da dupla, não para leitura coletiva por usuário. Sem timestamp físico de leitura ou versionamento completo; nenhum foi inventado.

## 12. Estrutura de anexos

`url_anexo` guarda uma referência real `sala/UUID.ext`, criada pelo servidor e vinculada à mensagem. É semanticamente compatível com esse campo. Tipo/MIME são obtidos da extensão validada pela política existente; tamanho é obtido dos bytes lidos com limite; nome de download é `anexo.ext`. Não se exige conservar nome original do cliente, e ele não é gravado nem exposto. Não existe tabela de metadados rica nem coleção de anexos; o contrato atual usa um arquivo por mensagem. Essa limitação não impede imagem/áudio/PDF atuais. Nenhum reaproveitamento de `portfolio_arquivos`, JSON ou campo estranho.

## 13. Estrutura de edição

`editada` e `data_edicao` são campos reais do database05/JPA e chegam ao DTO. `texto_original` é restrito e não integra DTO público. Não há prefixo textual ou marca apenas na JVM. O slot original não é histórico ilimitado de versões e não prova qual versão cada denúncia posterior capturou.

## 14. Estrutura de exclusão

`excluida` e `data_exclusao` são persistidos. DTO/preview exibem placeholder e escondem anexo. Sem reporte/moderação, corpo pode virar placeholder e anexo é removido após commit; com referência existente, corpo/original/arquivo permanecem restritos. Mensagem física e ordenação histórica continuam.

## 15. Interface RF18

Database05 possui `tipo_conteudo_enum.MENSAGEM` e `reportes_usuario`/`moderacao_conteudo` com `conteudo_id` polimórfico; **não há FK real para mensagens_chat**. API `DenunciaRequest.TipoAlvo` suporta VAGA/COMUNIDADE/PERFIL_ARTISTA/PERFIL_CONTRATANTE e **não produz denúncia MENSAGEM**. Não foi ampliada nesta tarefa. Fixtures DML de reporte/moderação existentes exercitam proteção do chat; não são alegadas como entrega do fluxo RF18. Estados/política de retenção/versionamento e acesso administrativo permanecem RF18/D03/D12.

## 16. Interface RF22

`DELETE /api/usuarios/me` já existe, confirma senha local, revoga sessões e orquestra exclusão/anonimização. Chat é anonimizado com remetente nulo e remoção da participação; conteúdo remanescente aparece como Usuário Removido. Conteúdo reportado/moderado fica oculto, preservado no storage privado. Fluxo local suportado é validado por `ExclusaoContaRf22IntegrationTest`; não criado nem alterado nesta tarefa. RF22 continua PARCIAL: Google-only, recursos sem política, retenção e cleanup durável/reconciliação não estão concluídos.

## 17. Criação/reutilização da sala

`criarOuReutilizarSala` reconsulta atores, valida dupla e bloqueia a chave ordenada. Consulta exige exatamente os dois IDs antes de reutilizar. RF45 chama o mesmo serviço com artista derivado da candidatura, após ownership da vaga e pertinência da candidatura. Payload arbitrário não substitui esse artista; no endpoint direto, alvo informado apenas identifica o participante permitido e passa por todas as regras.

## 18. Concorrência de sala

`pg_advisory_xact_lock(hashtextextended(chave,0))` serializa check/insert da dupla em PostgreSQL, inclusive entre processos que adotem o mesmo protocolo. Teste concorrente real existente verifica uma sala. Não foi adicionada UNIQUE nem prometida proteção contra escrita administrativa externa/legada fora do serviço.

## 19. Papéis

Somente ARTISTA ↔ CONTRATANTE. Consigo mesmo, artistas entre si, contratantes entre si e papéis administrativos são rejeitados. ADM/MODERADOR não se tornam participantes comuns; nenhum poder de comunidade autoriza chat direto.

## 20. Ownership

Service confirma participação a cada leitura, envio, alteração, leitura de recibo e download. Autoria de edição/exclusão também é conferida. Sala/mensagem/anexo alheios retornam 404 conforme ocultação de existência; conta sem papel/acesso normal retorna 403. O JWT fornece identidade, nunca um remetente do payload.

## 21. Proteção de menor

Reuso de `MenorAutorizadoPolicy` e `GoogleAccountAccessPolicy`. Menor exige idade permitida, conta ATIVA e responsável com consentimento vigente. Uma sala já criada não substitui essa validação; estado PENDENTE_CONSENTIMENTO impede novo uso normal, inclusive JWT antigo. O teste de sessões/refresh vigente é parte da regressão focada.

## 22. Candidatura

FK artista→vaga→contratante fornece contexto profissional real entre a dupla. O contrato já existente considera interação persistida, não impõe candidatura ATIVA nova a cada mensagem nem remove interação histórica de forma arbitrária. Testes confirmam proprietário/candidato correto e resposta do menor; candidaturas de outro contratante não concedem acesso.

## 23. Convite

**C03 — BLOCKER ESTRUTURAL específico.** RF42 persiste CONVITE em notificações, com destinatário e link; contratante e vaga não são relações tipadas do convite. `ConviteVagaRepository.existeInteracao` faz JOIN por concatenação de `link_contexto`. ChatService deixou de usar esse método como autorização de menor. Menor pode receber convite conforme RF42, mas só essa notificação não libera conversa. Menor capacidade futura: registro relacional de contratante/artista/vaga/data, com integridade e identidade/deduplicação apropriadas. Nenhum SQL criado. Convite continua distinto de candidatura.

## 24. Banco de Talentos

Membership isolada não autoriza chat com menor. Fixture insere relação real e testa bloqueio nos dois sentidos; candidatura posterior autoriza. Para adultos permanece a regra geral do chat, sem requisito novo de Banco/candidatura.

## 25. Favoritos

Favorito isolado não é candidatura, convite ou consentimento. Teste insere favorito real e prova bloqueio da dupla menor; nenhum filtro/favorito cria autorização adicional.

## 26. Envio

REST e STOMP convergem em `ChatService.enviarMensagem`. Reconsulta de conta, participação, dupla/menor, conteúdo, gravação e flush precedem entrega. Texto até 4000 caracteres é limite técnico existente; vazio/reservado é recusado, anexo válido permite texto opcional. Texto com HTML é dado literal, não markup confiável executado pelo servidor.

## 27. Persistência

Mensagem e notificação persistem na mesma transação lógica. Falha da notificação faz rollback da mensagem; anexo recém-gravado é limpo no rollback. Commit ocorre antes dos gateways. Falha de WS/SSE não apaga mensagem ou notificação. Sem broker externo, outbox novo ou alteração de transação global.

## 28. Histórico

Somente participante pode consultar; DTO mínimo com left join do remetente permite histórico anônimo. Ordenação `dataEnvio DESC,id DESC`; consulta e count paginados, não coleção ilimitada. Datas físicas de envio/edição são preservadas, sem reconstrução por prefixos.

## 29. Paginação

Salas, busca e histórico: padrão 20/máximo 50; page negativa, size0/51 e tamanho excessivo rejeitados. Salas ordenadas por última atividade derivada da última mensagem/data da sala, DESC, com ID DESC; histórico por data+ID. API preserva page/size/totalElements/totalPages/hasMore. Não se inventou coluna de atividade.

## 30. Leitura

Marca apenas recebidas não excluídas. Uma chamada cobre IDs até o máximo encontrado no início, em lotes de até 50. Novos IDs acima desse limite continuam pendentes; não se persegue envio concorrente sem limite. Repetição é idempotente; concorrência testada soma apenas atualizações reais. Recibos são privados, após commit, com até 50 IDs por evento. A própria mensagem não é marcada pelo remetente.

## 31. Contagem não lida

Mesmo critério em contador e lista/filtro: destinatário participante, remetente diferente ou removido, `lida=false`, `excluida=false`, sala com um ou dois participantes. Excluída não é pendência nem passa a lida artificialmente. Não conta sala alheia, própria mensagem ou sala coletiva inválida para RF35.

## 32. Busca

`GET /api/chat/salas?nome=...` busca apenas nome do outro participante, case-insensitive. Trim, parâmetro SQL e escape literal de `!`, `%`, `_` (`ESCAPE '!'`); barra invertida permanece literal. Não pesquisa e-mail, telefone, CPF, username ou responsável. Usuário Removido pode ser buscado como projeção anônima, sem dados antigos.

## 33. Filtros Todas/Lidas/Não lidas

`leitura=TODAS|LIDAS|NAO_LIDAS`, com TODAS por padrão; valores desconhecidos retornam400. LIDAS significa zero recebidas pendentes; NAO_LIDAS significa ao menos uma. Responder sem ler não transforma sala em lida. Sala vazia ou com apenas mensagens próprias é lida. Predicados de página/count são equivalentes e combinam com nome/paginação. Não foram criados enums físicos ou novas categorias RF36.

## 34. Edição 15 minutos

Somente autor; bloqueia excluída; comparação `dataEnvio.plusMinutes(15).isBefore(agora)` rejeita apenas após o limite. Clock do servidor/teste, sem sleep longo. Marca editada/dataEdicao reais persistidas e exibidas no histórico/realtime. Lock pessimista serializa alterações; duas edições válidas resultam no último conteúdo gravado, sem promessa de versionamento/merge de edições.

## 35. Exclusão

Autor pode excluir a própria sem prazo adicional, de forma idempotente. Mensagem permanece com flag/data e placeholder; data do primeiro delete não muda em retry. Participantes não recebem evidência interna e download comum de excluída é bloqueado. Conteúdo referido em reporte/moderação não é sobrescrito/limpo.

## 36. Evidência denunciada

Consulta agora considera reporte **ou moderação** MENSAGEM. Primeiro original protegido fica em `texto_original`; soft delete oculta texto/arquivo ao usuário comum, preservando os dados já relacionados. Não existe endpoint novo de moderação. Um único slot original não garante versões intermediárias de múltiplas denúncias; produtor atual de denúncia de mensagem ausente e política/versionamento/retenção permanecem dependências. Não se declara prova jurídica nem garantia de captura concorrente de qualquer denúncia futura.

## 37. Anexos

Suporte existente comprovado e preservado: JPG/JPEG/PNG, MP3 e PDF. Arquivo sem texto pode ser mensagem; texto sem arquivo segue envio textual. Vídeo MP4 e demais extensões fora da política são recusados. RF16 fornece apenas validator compartilhável, sem uso da tabela/semântica de portfólio para armazenar chat.

## 38. Validação MIME

`ArquivoPortfolioValidator` compara extensão/MIME declarado e bytes: decodificação/CRC de imagem, assinatura PDF e frames MP3. Limites existentes imagem5MiB/PDF10MiB/áudio20MiB; nome seguro sem caminhos/controles e bytes limitados. Arquivo executável/MIME falso/path traversal não persiste mensagem nem arquivo. Não foi criada lista nova nem alegada varredura antivírus; download é attachment com nosniff/sandbox.

## 39. ACL de arquivo

`GET /api/chat/mensagens/{id}/anexo` exige JWT e participação, não basta conhecer ID. Referência server-side sala/UUID/ext, storage fora das raízes públicas e verificações de traversal/links. DTO expõe endpoint protegido, não referência/caminho físico. Content-Disposition usa nome genérico, Cache-Control no-store, nosniff e CSP sandbox. Anexo excluído oculto mesmo quando retido para evidência.

## 40. WebSocket

Endpoint nativo `/ws`, allowlist CORS atual. Handshake rejeita query. Identidade estabelecida no CONNECT; serviço de envio repete ACL/regras profissionais. Realtime é entrega complementar, não persistência.

## 41. STOMP auth

Authorization Bearer no header STOMP CONNECT, exatamente um. Sem token/inválido/expirado/sem identidade recusado. SEND/SUBSCRIBE revalidam token e conta persistida; revogação ou pendência não é contornada por conexão já aberta. JWT não vai em query; autenticação global não foi afrouxada.

## 42. Destinations

SUBSCRIBE apenas `/user/queue/chat` e `/user/queue/notificacoes`, resolvidos pelo Principal. `/queue` direto, `/topic` e destination com outro usuário são recusados. SEND apenas `/app/chat/salas/{id}/mensagens`; o ID identifica sala e não concede ACL. Entrega por `convertAndSendToUser`, com checagem de conta atual pelo gateway.

## 43. Realtime after commit

`ChatEventoListener` e entrega de `NotificacaoPersistida` usam AFTER_COMMIT. Rollback não publica mensagem; falha de gateway isolada preserva persistência. Não há retry durável/exactly-once; queda após commit pode exigir reload REST. Falha no callback/lookup após commit não desfaz o banco. Não se adicionou idempotência global por texto.

## 44. SSE

Fallback de notificações existente, `/api/notificacoes/stream`, JWT header e identidade obtida do Principal. Query com usuário/token é recusada. Emissores separados por ID do titular; outro usuário não recebe a mensagem. Falha SSE/WS não elimina registro. Não foi criado stream de chat coletivo nem reimplementada infraestrutura RF36.

## 45. RF36 Mensagens

Categoria física `MENSAGEM`, não alias de outro tipo. Nova mensagem cria alerta mínimo “Você recebeu uma nova mensagem.” somente ao outro participante; não contém corpo completo. Contexto `/mensagens?sala=id` exige ACL ao abrir. Editar/excluir/marcar leitura não produz novos alertas MENSAGEM. Recorte consolidado sem concluir categorias restantes.

## 46. Notificação persistente

Gravação MANDATORY na transação do produtor. Uma chamada válida grava uma mensagem e um alerta; duas chamadas intencionais iguais gravam dois de cada. Evento `NotificacaoPersistida` serve entrega, não faz nova inserção quando entregue. Guard de identidade por transação no listener genérico não é garantia global de transporte. Sem UNIQUE nova nem promessa de deduplicar retries HTTP sem chave contratual.

## 47. Privacidade

DTOs limitados a sala/id, identificação visual pública mínima, texto permitido/placeholder, datas, leitura e URL autenticada. Sem e-mail/telefone/CPF/CNPJ/nascimento/responsável/condições afirmativas/JWT/refresh/original restrito. Alertas e logs não incluem corpo sensível. Notificação não concede acesso.

## 48. Menores

Consentimento e contexto profissional são reavaliados em sala existente; Banco/favorito/busca não bastam. JWT anterior à pendência é rejeitado pela política atual; refresh não restitui acesso normal. Histórico não expõe dados adicionais do menor. Convite sem relações reais continua incapaz de autorizar contato.

## 49. RF44

Nenhum e-mail ao responsável por mensagem comum. RF44 permanece com eventos já classificados, como candidatura; não foi ampliado por inferência. Regressão RF44 e dados atuais do responsável incluídos no foco, sem mudar produtor.

## 50. Performance/N+1

Histórico e salas usam projeções/consultas próprias com count; não fazem lookup de perfil por linha. Testes medem teto constante para 20/22 salas filtradas e 50 mensagens. Não foi criado índice nem feito benchmark de100usuários/p95/p99. Custo de filtros/count em grande volume e marcação de histórico extenso ainda requer homologação representativa; nenhum resultado de carga foi inventado.

## 51. Concorrência

Sala: advisory lock por dupla. Alteração de mensagem: row lock existente. Leitura: update condicional idempotente em lotes, teste real com dois workers/73 recebidas. Receipts não viram notificação nova. Nenhum versionamento/constraint novo; último edit válido prevalece, com autoria preservada.

## 52. Transações

Criação da sala/participantes, mensagem/notificação e alteração de conteúdo permanecem transacionais. Limpeza de upload após rollback; delete físico de anexo comum somente após commit. Storage/banco não têm atomicidade distribuída; crash pode deixar arquivo privado órfão, dependência operacional/RF22 de reconciliação, sem nova arquitetura.

## 53. Logs

Chat e listeners registram falha técnica sanitizada/IDs, não token, Authorization, texto integral, anexo binário ou dados de responsável. Exceções de arquivo são genéricas, sem path. `spring.jpa.show-sql` permanece false por padrão. Logs de teste/evidência não são substituto de trilha durável de moderação ou homologação RNF09 global.

## 54. Compatibilidade API

Todos endpoints/DTOs existentes mantidos; overload de listarSalas de três argumentos preservado. Novos parâmetros opcionais `nome` e `leitura`. Leitura PATCH agora cobre recebidas atuais em mais de um lote; excluídas deixam de contar. Restrição deliberada de menor por convite documental é correção de autorização exigida pela baseline. RF45 ownership/IDs derivados e RF42 envio/sem candidatura automática preservados.

## 55. Database alterado

**NÃO.** database05 intacto; ZIP/46 arquivos verificados. Banco legado conserva apenas delta preexistente. Nenhum enum/tabela/coluna/FK/constraint/índice/trigger/procedure/function/seed alterado. DML de fixtures ocorre apenas em PostgreSQL descartável.

## 56. Frontend alterado

**NÃO pela tarefa.** `frontend/` sem delta; 25 alterações rastreadas e arquivos não rastreados anteriores de `palco-comunidades-agenda/` preservados por hash. Não foi feito QA/renderização/browser nem alteração de integração visual.

## 57. Migration/SQL

**NÃO.** Nenhum arquivo SQL/migration novo ou modificado pela tarefa; consultas parametrizadas nos repositories Java usam estruturas oficiais existentes. Index Git preservado; sem staging/commit/push/reset/clean/stash.

## 58. Testes novos/alterados

ChatRf35IntegrationTest: busca literal/nome privado, filtros combinados/leitura real, página/count/ordem/N+1, excluída, sala coletiva indevida, moderação, vídeo, envios iguais intencionais, JWT pendente, leitura/edição concorrentes; teste antigo de lote ajustado para sala completa. ChatRf24IntegrationTest: Banco/favorito/busca/convite textual isolados não autorizam menor; candidatura real libera dupla. ConviteVagaRf42IntegrationTest: três instâncias antigas de autorização por convite ajustadas para bloqueio, preservando envio/notificação/privacidade. Testes antigos não removidos; nenhum novo skip.

DashboardRf11IntegrationTest: apenas a expectativa das duas instâncias ARTISTA/CONTRATANTE do contador RF35 foi corrigida de 2 para 1. A fixture continha uma mensagem excluída e o teste contava essa evidência oculta como pendência. Não houve alteração de código de dashboard, fixture ou remoção de assertions de privacidade/ausência de efeitos colaterais.

## 59. Testes focados

**BUILD SUCCESS: 689 total /689 passed /0 failures /0 errors /0 skipped; 23 suites; 412 segundos.** Exit0.

Classes do foco inicial: `StompJwtChannelInterceptorTest`, `AvisoResponsavelRf44IntegrationTest`, `CandidatosVagaRf45IntegrationTest`, `CandidaturaControllerRf06IntegrationTest`, `ChatRf24IntegrationTest`, `ChatRf35IntegrationTest`, `ConviteVagaRf42IntegrationTest`, `DadosResponsavelRf26Rf27Rf53IntegrationTest`, `DenunciaRf14Rf18IntegrationTest`, `ExclusaoContaRf22IntegrationTest`, `GuardianConsentRf27IntegrationTest`, `ModeracaoRf18IntegrationTest`, `NotificacaoRf23IntegrationTest`, `NotificacaoRf36IntegrationTest`, `SessoesSegurancaRf12Rf25Rf53IntegrationTest`, `ChatEventoListenerTest`, `NotificacaoEventoListenerTest`, `ChatWebSocketRf24IntegrationTest`, `NotificacaoSsePoolIntegrationTest`, `NotificacaoWebSocketRf23IntegrationTest`, `GenericEndpointsSecurityIntegrationTest`, `JwtAuthenticationIntegrationTest`, `GuardianConsentServiceTest`.

Classes efetivamente executadas e duração por suite em `maven-focado-resumo.json`. Conjunto cobre RF35, RF36 Mensagens, RF45, RF42, RF06, RF27/menor, RF25/estado/refresh, interfaces RF18/RF22, STOMP/WS/SSE, notificações e segurança. Não se extrapola teste de entrega simples para benchmark RNF05.

**Revalidação após ajuste do contador no teste de dashboard: 134 total /134 passed /0 failures /0 errors /0 skipped;2 suites;88,4 segundos;BUILD SUCCESS.** Comando e classes em `maven-correcao-dashboard-execucao.json` e `maven-correcao-dashboard-resumo.json`. Os dois focos cobrem24 classes distintas e732 casos distintos (91 casos de ChatRf35 repetidos), sem somar repetição como teste novo.

## 60. mvn test

Comando `.\mvnw.cmd test`, JDK21, PostgreSQL18/Testcontainers, database05/validate. **BUILD SUCCESS: 1887 total /1870 passed /0 failures /0 errors /17 skipped;82 suites;682.7 segundos;exit0.** Execução de 2026-10-09T00:13:13.1674073-03:00 até 2026-10-09T00:24:35.8774591-03:00. Log/resultado: `maven-completo.log`, `maven-completo-resumo.json`, `maven-completo-execucao.json`.

Primeira tentativa completa:1887 total /1868 passed /2 failures /0 errors /17 skipped,733,0s, BUILD FAILURE. As duas falhas foram exclusivamente a expectativa antiga de contador no teste DashboardRf11IntegrationTest acima. Log/resumo preservados como `maven-completo-tentativa-1.*`; correção focada e regressão completa repetidas, sem ocultar a tentativa anterior.

## 61. Total/passed/failures/errors/skipped

| Execução | Total | Passed | Failures | Errors | Skipped | Resultado |
|---|---:|---:|---:|---:|---:|---|
| Baseline anterior, referência | 1855 | 1838 | 0 | 0 | 17 | BUILD SUCCESS histórico |
| Focada desta tarefa | 689 | 689 | 0 | 0 | 0 | BUILD SUCCESS |
| Revalidação chat/dashboard | 134 | 134 | 0 | 0 | 0 | BUILD SUCCESS |
| Completa desta tarefa | 1887 | 1870 | 0 | 0 | 17 | BUILD SUCCESS |

**Aumento de32 execuções:** ChatRf24IntegrationTest15→19 (+4) e ChatRf35IntegrationTest63→91 (+28). Mesmas82 suites; nenhuma removida/reduzida. Os17 skips condicionais permanecem nas mesmas suites e estão identificados com motivo em `regressao-comparacao.json`; nenhum skip novo. RF42 e dashboard mantiveram suas contagens, com expectativas antigas ajustadas ao contrato atual.

## 62. Semgrep

**Semgrep 1.178.0: 388 arquivos /60 regras /~100.0% das linhas parseadas /0 findings /0 blocking /0 errors;exit0;51.6 segundos.** Todos os nove Java do delta cobertos (`missingJavaFiles=[]`).

Docker oficial com digest imutável `sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b`, p/java local de SHA-256 `5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1`, mounts read-only, rede desativada, metrics off e fontes main+tests. Sem instalação nativa Windows ou desativação TLS. Revisão manual cobre IDOR, arquivos/paths, XSS como texto, logs, WebSocket e dados de menores. **0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.**

## 63. Graphify

MCP inicial acessível: **6948 nós /24588 arestas /361 comunidades, 91% EXTRACTED e9% INFERRED**. Consulta orientou ChatService/controllers/repositories, policies, notificações, RF45/RF42/RF22. Código e database05 decidiram conclusões. `graphify update .` AST concluiu com exit0 em 38,3 segundos: **6966 nós /24688 arestas /374 comunidades;91% EXTRACTED /9% INFERRED**. MCP final confirmou estatísticas e o novo teste de filtros em ChatRf35IntegrationTest. Avisos:45 SQLs sem parser tree_sitter_sql,75 arquivos não classificados e107 comunidades renomeadas por hub (361 rótulos salvos para374 comunidades). Sem parser instalado, rotulação LLM ou contorno do Windows App Control. O update é do grafo de código; não certifica extração semântica do novo relatório/documentos. Logs e verificações MCP preservados na pasta de evidências.

## 64. Blockers

1. **C03:** prova de convite para chat de menor exige relação confiável contratante/artista/vaga/data; notificação com URL não atende. O backend contém essa autorização.
2. **Dependência RF18 backend/política:** API de denúncia MENSAGEM ausente; referência polimórfica sem FK; guarda existente preserva corpo/original/arquivo, mas não entrega o fluxo completo ou versionamento por denúncia. Definir política antes de alegar fechamento.
3. **Dependência RF22/RNF06:** anonimização local funciona; confirmação Google-only, políticas de alguns recursos, evidência/retenção e cleanup durável/reconciliação continuam pendentes. Não são falsamente atribuídas à ausência dos campos de chat.

Não há blocker físico confirmado para edição/marca, soft delete ou anexos privados atuais de um arquivo. RF52 depende estrutura coletiva própria e permanece fora do escopo.

## 65. Riscos

Garantia de sala única vale para writes sob o protocolo de lock, não para inserção externa. Realtime é best-effort pós-commit, sem outbox/exactly-once. Última edição válida prevalece; não há merge/versionamento integral. Um original não equivale a snapshots de todas as denúncias. Cleanup não é durável diante de crash. Texto livre pode conter dados pessoais mesmo após anonimização estrutural; política de retenção não é certificação jurídica. Sem benchmark de grande volume.

## 66. Pendências e revisão manual final

Fechar C03 no pacote oficial; tratar produtor/política RF18; fechar RF22 nos domínios restantes; homologar volume e integração visual em tarefa própria. Nenhum bloqueio foi contornado por schema/enum/JSON/usuário fictício.

| Pergunta obrigatória | Resposta baseada no código/testes |
|---|---|
| Remetente vem do JWT? | Sim, Principal e reconsulta no service; payload não manda autoria |
| Target arbitrário consegue abrir sala? | Só alvo que satisfaça dupla/conta/contexto; RF45 deriva o artista da candidatura |
| Sala de terceiro acessível? | Não,404 |
| Mensagens de terceiro acessíveis? | Não,404 |
| Dois requests duplicam sala? | Não no protocolo transacional testado; inserção externa não tem UNIQUE do par |
| Menor aleatório pode ser contatado? | Não |
| Candidatura válida permite contexto? | Sim, relação persistida entre a dupla |
| Banco sozinho permite menor? | Não |
| Favorito permite menor? | Não |
| JWT antigo contorna PENDENTE_CONSENTIMENTO? | Não; refresh também revalida conta |
| Lista paginada? | Sim |
| Histórico paginado? | Sim |
| Size máximo50? | Sim |
| Existe N+1? | Não relevante nas listagens verificadas; teto medido, sem benchmark global |
| Autor edita só própria? | Sim |
| Janela15min inclusive? | Sim, Clock e caso exatamente900segundos |
| Exclusão destrói evidência? | Não nos reportes/moderações já relacionados; política/captura futura completa pendente |
| Mensagem denunciada preservável? | Sim fisicamente e no guard; API de denúncia MENSAGEM ainda não existe |
| Anexos representáveis? | Sim, referência real + storage privado + metadados derivados da política/bytes |
| Vídeo aceito como anexo? | Não |
| Download valida participante? | Sim |
| Path físico vaza? | Não no DTO/erro/download |
| Mensagem persiste antes de realtime? | Sim e commit precede entrega |
| Falha realtime perde mensagem? | Não |
| JWT WebSocket vai em query? | Não,query rejeitada |
| Usuário consegue subscribe alheio? | Não,allowlist privada e Principal |
| Notificação vai ao destinatário? | Sim,apenas outro participante |
| Notificação concede sala? | Não,ACL permanece |
| RF52 foi misturado? | Não |
| Dados do responsável aparecem? | Não |
| Condição afirmativa aparece? | Não |
| Banco alterado? | Não pela tarefa |
| Frontend alterado? | Não pela tarefa; deltas anteriores preservados |

## 67. Conclusão RF35 sala

Consolidada: dupla, roles, reuse/concorrência sob lock, ACL e integração RF45. Sala anônima mantém histórico, não envio sem destinatário.

## 68. Conclusão RF35 mensagens

Consolidadas texto, histórico, paginação, leitura/contador e busca/filtros, com DTO e persistência atuais. Realtime complementar.

## 69. Conclusão RF35 menores

Proteção consolidada por políticas de conta/consentimento e candidatura real. Convite não autoriza enquanto C03 faltar; Banco/favorito/busca isolados não autorizam.

## 70. Conclusão RF35 edição

Autoria, limite inclusive, marca/data física e lock consolidados. Evidência complexa e política integral RF18 permanecem dependências explícitas.

## 71. Conclusão RF35 exclusão

Soft delete/placeholder, idempotência, proteção de registros já reportados/moderados e cleanup pós-commit consolidados. Não é entrega global de moderação/retenção.

## 72. Conclusão RF35 anexos

Suportados e validados no contrato existente: imagem, áudioMP3 e documentoPDF. Vídeo rejeitado. ACL/download privado e paths protegidos; não criado storage novo ou metadado falso.

## 73. Conclusão RF35 RF22

Histórico Usuário Removido funciona para cenários RF22 suportados, sem usuário fake ou ID substituto no remetente do chat. RF22 inteiro permanece PARCIAL e sua conclusão não foi alterada.

## 74. Conclusão RF35 total

**PARCIAL**, com núcleo direto textual/conversas consolidado e anexos/edição/exclusão existentes suportados. C03 e dependências de denúncia/evidência/RF22 impedem conclusão integral. Não se mascara esse limite com um “100%”.

## 75. Conclusão RF36 Mensagens

**CONSOLIDADO no recorte:** alerta persistente MENSAGEM, destinatário correto, mínimo seguro, commit antes de WS/SSE, falha isolada e clique com ACL. Não promessa de exactly-once global.

## 76. Conclusão RF36 total

**PARCIAL**, conforme baseline. Infraestrutura existente preservada; cobertura completa de outras categorias/produtores não pertence à tarefa.

## 77. Próximo passo

Priorizar pacote oficial mínimo C03; auditar/implementar produtor de denúncia de mensagem e política de evidência na tarefa RF18; continuar fechamento RF22 separadamente. Backend deve adaptar-se ao schema atual quando suficiente. Integrar a interface dos novos parâmetros apenas quando houver tarefa frontend autorizada.

## 78. Registro semanal — 08/10/2026

Objetivo: consolidar RF35 direto e somente Mensagens RF36, no checkpoint7379e94, preservando database05/frontend. Dependências: RF06/18/22/25/27/42/44/45/52; RNF05/06/08/09/10/17. Delta: cinco Java de produção e quatro de teste, novo relatório/evidências; sem SQL/migration, index/commit/push. Entregas: busca/filtros, leitura/counters e preservação sob moderação, contenção de prova textual de convite. Segurança: JWT/Principal, roles/ownership/IDOR, menor, destinations privadas, arquivo privado e minimização. Resultados reais nos itens59–63; blockers/riscos nos64–66; RF35/RF36 globais PARCIAIS.

**Preservação final:** 1287 arquivos no manifesto inicial;1278 protegidos;0 alterações fora do escopo;0 arquivos novos inesperados;0 fontes Java modificadas após os testes. HEAD/branch preservados;index hash idêntico e cached vazio;database05 e frontend sem delta da tarefa;baseline/ZIP intactos. Banco legado e25 deltas anteriores do frontend operacional preservados. **git diff --check: exit0.** Evidências em `preservacao-final.json`, `status-final.txt`, `diff-check.txt`. Nenhum staging/commit/push.

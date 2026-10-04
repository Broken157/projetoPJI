# RF36 — sistema de notificações em tempo real — 04/10/2026

## 1. Objetivo

Auditar e concluir o núcleo backend RF36 e integrar os produtores disponíveis, mantendo database05 oficial, frontend e históricos. Resultado técnico após as validações abaixo: **RF36 backend CONCLUÍDO**. Integração visual e eventos de produtores futuros continuam separados.

## 2. Requisitos consultados

Fonte oficial atual: `C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt`, SHA-256 `3e7ca8fa7e78a1967cf5f86c86f6a60f736879b02cfbd8f9d195040fea873183`. RF06, RF11, RF13, RF19, RF22, RF23, RF28, RF35, RF36, RF41, RF42, RF44; RNF02, RNF05, RNF06, RNF07, RNF08, RNF09, RNF10, RNF17. Consultados os relatórios atuais RF06, RF13/RF36, RF19, vagas RF23/RF28, RF42, RF35, RF22 e sincronização database05. Nenhum histórico foi reescrito.

## 3. Checkpoint e HEAD

Checkout `C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline`; branch `integracao-recuperada-2026-09-15`. HEAD confirmado ao vivo: `c67da401104d9a44ad0684a2fd7b7ea2773c9a3f` — checkpoint RF22. Upstream e fork apontavam para o mesmo commit; após fetch autorizado de fork, divergência **0/0**. Index vazio; sem alterações RF22/RF13/backend/database05/scripts soltas. README, SQL histórico em database/ e frontend já tinham deltas; foram preservados. Manifesto inicial: 1.425 arquivos, dos quais 1.414 protegidos após excluir apenas os onze Java existentes previstos no patch; novo teste fora do manifesto inicial.

## 4. Graphify inicial

Graphify MCP consultado antes da exploração direta das fontes; HTTP `http://127.0.0.1:8765/mcp` confirmado por initialize + graph_stats antes das alterações Java. **6.153 nós / 21.049 arestas / 349 comunidades**, 91% EXTRACTED, 9% INFERRED, 0% AMBIGUOUS. Queries de núcleo/produtores tiveram limite de orçamento (77/140 e 80/244 nós mostrados); fonte/testes confirmaram as conclusões. Evidências em `evidencias/rf36-2026-10-04/graphify-inicial-*.json` e `graphify-mcp-inicial-*.txt`.

## 5. Database05

Fonte ativa: `database05/palco-database/`; database04 é histórico. PostgreSQL 18 real/Testcontainers, pacote oficial montado por `PALCO_TEST_DATABASE05_PATH`, `ddl-auto=validate`. Somente DML de fixtures em containers descartáveis. Sem H2, patch, migration, schema auxiliar, criação/atualização por Hibernate ou mudança de scripts/bootstrap/SQL.

## 6. Enum oficial completo

`tipo_notificacao_enum`, na ordem física: **CANDIDATURA, MENSAGEM, CONVITE, EDITAL, SALVO, BANCO_DE_TALENTOS**. Java corresponde exatamente. Não existe SEGUIDOR ou VAGA_CANCELADA. RF28 conserva CANDIDATURA, conforme código consolidado; não foi criado novo tipo.

## 7. Estrutura notificacoes

PK bigserial id; usuario_destino_id bigint NOT NULL, FK usuarios ON DELETE CASCADE; tipo enum NOT NULL; mensagem_alerta **text NOT NULL**; link_contexto varchar(255) NOT NULL; lida boolean DEFAULT false e data_criacao timestamp DEFAULT current_timestamp (SQL permite NULL nesses dois últimos campos, runtime atual escreve valores). Índice `(usuario_destino_id,lida)`. Sem coluna de ator, preferências, outbox ou UNIQUE de deduplicação. Nenhum limite artificial de 255 foi aplicado à mensagem text.

## 8. Matriz inicial de eventos

Criada **antes** do patch Java: `evidencias/rf36-2026-10-04/matriz-eventos-inicial.md`, grupos A–K com ator, destino, tipo físico, link, privacidade, idempotência e transação. Lacuna adicional de cleanup comprovada depois em `lacuna-cleanup-sse.md`. Rastreabilidade dos 75 cenários do prompt: `cobertura-rf36.md`.

| Produtor disponível | Situação inicial → final |
|---|---|
| RF06 criar/reaplicar | Persistência pós-commit → mesma transação obrigatória; link canônico do dono |
| RF06 retirada consolidada | Correto; preservado e regredido |
| RF13 Banco | Correto; preservado e regredido |
| RF42 convite | Correto; preservado e regredido |
| RF35 mensagem | Correto; preservado e regredido |
| RF19 salvo perfil/vaga | Evento já existia → persistência obrigatória corrigida no caminho compartilhado |
| RF28 cancelamento | Correto transacionalmente; link canônico atualizado; regressão |
| RF23 pausar/reabrir/encerrar/autoencerrar | Eventos existentes preservados; persistência compartilhada corrigida |
| RF41/OBRA/EDITAL futuro | Não implementados por antecipação; fronteira documentada |

## 9. Arquitetura final

Operação real do produtor → NotificacaoEvento síncrono dentro do TX → NotificacaoPersistenceService **MANDATORY** → INSERT + flush → NotificacaoPersistida → listener **AFTER_COMMIT** → gateway revalida existência/email/aptidão → fila WS privada e pool SSE privado. RF13/RF42/RF35/RF28/retirada já persistiam MANDATORY e publicavam NotificacaoPersistida: não foram reimplementados. Removido o método REQUIRES_NEW pós-commit, sem chamada legítima remanescente. Banco não falha silenciosamente como sucesso; transporte pode falhar isoladamente.

## 10. Central

GET `/api/notificacoes` autenticado, persistente, somente destino do JWT revalidado. DTO de seis campos: id, tipo, mensagem, link, lida, data; nenhuma Entity/usuário/responsável serializado.

## 11. Listagem

Ownership no filtro de banco; terceiro não vê conteúdo alheio. Seis tipos oficiais, mensagens, links e estado de leitura preservados. Testes explícitos de identidade inválida/expirada/inapta/removida e de whitelist estruturada.

## 12. Paginação

Padrão 20, máximo 50; page não negativo e size positivo; timestamp DESC + id DESC desempata. Teste de página com data empatada e isolamento. Teste existente de 50 itens mantém **≤4 consultas Hibernate**, sem N+1 significativo na central. Esse limite não é benchmark global dos demais RFs.

## 13. Contador

GET `/api/notificacoes/nao-lidas/count`, resposta `{count}`. COUNT PostgreSQL por destino + lida=false; não lista entidades para contar, não cria contador redundante. Índice físico preservado; leitura reduz count e leitura de todas o zera.

## 14. Lida individual

PATCH `/{id}/lida`: somente owner, 204, idempotente. Alheia/inexistente devolvem o mesmo 404/mensagem genérica. Não cria alerta e não entrega realtime de leitura.

## 15. Marcar todas

PATCH `/lidas`: bulk UPDATE por usuário do JWT e lida=false, flush/clear; payload/query usuarioId não tem autoridade. Testados zero, uma e cinco notificações, mistura de lidas/não lidas, repetição e terceiro simultâneo. Não carrega histórico completo.

## 16. Autenticação REST

JwtAuthFilter exige Authorization Bearer válido e revalida ID/email/estado persistidos; central/count/leitura/stream autenticados. Não há POST genérico autorizado para forjar destino/tipo/mensagem/link/ator: tentativa retorna 405 e zero INSERT. Contratos administrativos legítimos não foram removidos.

## 17. Autenticação STOMP

Authorization Bearer no CONNECT; assinatura/expiração/ID positivo/email/conta persistida verificados. Teste de WebSocket nativo confirma **ERROR + fechamento** para ausência, token inválido/expirado, usuário inexistente/inapto. Somente `/user/queue/notificacoes` e `/user/queue/chat`; assinatura de fila alheia produz ERROR e fechamento. SUBSCRIBE/SEND revalidam token e estado, conforme infraestrutura existente.

## 18. Query-token

Upgrade `/ws` já rejeita qualquer query, inclusive com CONNECT válido; regredido. Stream SSE passou a rejeitar query mesmo com header válido (400; sem header válido, filtro responde 401). Sem cookie/token alternativo ou JWT em URL.

## 19. Token expirado

Expirado não entra no CONNECT nem REST/stream. Renovação continua em POST `/api/auth/refresh`, antes de o cliente reconectar. Refresh token atual é opaco UUID, incompatível com parser de JWT de acesso; WS não renova tokens. RF02/RF25 não foram alterados. Não se acrescentou timer de desconexão por expiração para sessões passivas já abertas; a fronteira testada é CONNECT e revalidação das ações existentes.

## 20. SSE

Fallback unidirecional por Authorization header e ID persistido. Pool separado por usuário, timeout atual de 30 minutos, callbacks completion/timeout/error removem emitter. Não havia limite de conexões adicional e nenhum foi inventado. A correção de cleanup remove o emitter antes de finalizar com erro e tolera contexto já finalizado pelo container; log debug genérico sem PII. Testes reais de fechamento/erro exigem pool zerado; timer completo de 30 minutos foi inspecionado por fonte, sem esperar 30 minutos em teste.

## 21. Isolamento

Sete produtores reais por HTTP, duas sessões WS/SSE e terceiro isolado; ID/tipo/link/mensagem comparados ao registro persistido. SSE usa sentinela própria para provar que o próximo evento do terceiro não foi o evento alheio. Teste de seis streams com Hikari máximo de duas conexões confirma scalar lookup sem manter conexão OSIV ocupada e entrega privada. Authorization de cada contexto continua independente do alerta.

## 22. AFTER_COMMIT

NotificacaoPersistida é entregue somente após commit. Evento cru passou a persistir síncrono no TX do produtor; não chama diretamente o gateway. Teste consulta PostgreSQL dentro do TX, encontra a linha e confirma zero entrega; após commit confirma uma entrega.

## 23. Rollback

Falha do domínio ou persistência obrigatória da notificação desfaz ambos; rollback explícito não entrega. Candidatura e ambos os alvos salvos têm testes de falha real com zero domínio/notificação e zero gateway. RF13/RF42/RF35/RF28 mantêm testes de atomicidade próprios. O teste antigo que aceitava candidatura sem alerta após erro de banco foi atualizado para o contrato atômico exigido, mantendo assertions e aumentando a exigência.

## 24. Falha WS

Falha do broker não desfaz operação/alerta commitados; SSE ainda é tentado. Falha de gateway pós-commit é isolada pelo listener. Logs desse caminho contêm somente ID ou mensagem genérica, sem causa/payload/PII.

## 25. Falha SSE

Falha SSE não desfaz operação/alerta e não impede WS. Falha simultânea dos dois preserva consulta da central. Emissor removido também quando a finalização do container já ocorreu; assert de cleanup não foi retirado.

## 26. Entrega ≤5 segundos

WebSocket real ativo, subscription privada, API HTTP real e comparação com notificação persistida. Sete produtores: candidatura, Banco, convite, mensagem, salvo perfil, salvo vaga e cancelamento. Tempos finais: **CANDIDATURA 60 ms; BANCO 69 ms; CONVITE 131 ms; MENSAGEM 79 ms; SALVO_PERFIL 57 ms; SALVO_VAGA 79 ms; CANCELAMENTO 48 ms**. Prontidão é verificada por probe de transporte não persistido antes da medição; removidos sleeps fixos do teste WS/SSE. Medição inclui chamada de API; é evidência local, sem promessa de latência de produção sob carga.

## 27. CANDIDATURA

Destino vem do dono persistido da vaga; ator do JWT. Criação/reaplicação válida participa do TX com alerta; tentativa inválida/duplicada não publica. Link `/vagas/{id}/gerenciar` corresponde à rota real de gestão do dono e ao backend RF45 autorizado. Mensagem só usa título público da vaga, sem apresentação/portfólio/experiência privada. Retirada já definida permanece; não foram criados ACEITA/REJEITADA.

## 28. BANCO_DE_TALENTOS

Primeira entrada real RF13 → uma notificação ao contratante, genérica, link `/perfis/ARTISTA/{id}`. MANDATORY, GET/repetição sem alerta, owner/menor/consentimento já guardados. RF17 pesquisa não notifica. Produtor preservado; somente regressão.

## 29. CONVITE

RF42 mantém registro lógico em notificacoes, owner/Banco/vaga aberta e lock existentes. Primeiro convite uma CONVITE, repetição reutiliza identidade persistida; link `/vagas/{id}`. Nenhuma candidatura automática. Produtor/repositório RF42 não alterados.

## 30. MENSAGEM

Envio real RF35 persiste mensagem e MENSAGEM na mesma transação, destino outro participante; alerta genérico sem corpo de chat, link `/mensagens?sala={id}`. Edição/leitura/exclusão não gera nova notificação. Contratos RF35/RF24 e segurança da sala preservados.

## 31. SALVO

Schema oferece SALVO e RF19 oferece PERFIL_ARTISTA/VAGA. Produtor já publicava evento mínimo ao dono; agora persistência ocorre antes do commit. INSERT ON CONFLICT determina primeira inserção; repetição, GET e remover sem “desalvou”. Auto-salvo é regra existente permitida, sem auto-alerta. Mensagens constantes não identificam ator; links `/perfis/ARTISTA/{id}` ou `/vagas/{id}` vêm do domínio. Preferências inexistentes no schema/contrato atual foram documentadas, sem tabela/API nova. OBRA permanece indisponível na API.

## 32. RF41/SEGUIDOR

Sem produtor/tabela/API atual e sem SEGUIDOR no enum. **Pendência condicionada ao RF produtor futuro**, não defeito do núcleo RF36. RF41 não foi implementado nesta tarefa.

## 33. RF28/cancelamento

Mantida representação consolidada CANDIDATURA. Cancelamento autorizado atualiza status, log, candidaturas ativas e notificações na mesma transação; conserva históricos retirados. Conjunto de destinatários do código consolidado preservado. Link atual `/vagas/{id}`; terceiro não candidato isolado. Regressão cobre erro no log/segunda notificação e falhas de transporte.

## 34. RF23

Pausa/reabertura/encerramento manual/autoencerramento já possuem alertas explícitos no código atual; foram preservados, com correção do caminho transacional compartilhado e link. Nenhum novo evento de estado foi inventado. Lotes/keyset e regras de prazo não alterados.

## 35. RF44

Email ao responsável legal continua em seu canal próprio; integração existente preservada. Não há nova notificação interna ao responsável, email/consentimento no DTO, nem duplicação de cada evento do menor para o responsável.

## 36. RF22

Checkpoint preservado. Exclusão/retenção/contextos/cascade seguem a política implementada; conta removida não autentica, gateway revalida existência/aptidão e não entrega a destino inexistente/inapto. Regressão RF22 inteira incluída, sem alterar sua matriz ou declarar concluídas suas pendências próprias.

## 37. Privacidade

Asserts estruturados dos seis campos do DTO, valores conhecidos de email/telefone/nascimento/senha e campos CPF/CNPJ/responsável/tokens/experiência; mensagens privadas de chat/candidatura não aparecem. Links internos sem URL externa/token. Ausência de “@” isoladamente não foi usada como única prova. Auditoria adicional das cópias de evidência: **zero arquivos com valores locais conhecidos verificados (1 valores ≥8 caracteres) ou JWT assinado**. Isso verifica valores locais conhecidos/JWT assinado, não representa scan amplo de secrets nem conclusão jurídica/LGPD.

## 38. link_contexto

Rotas canônicas confirmadas por fonte frontend e contratos backend, sem editar frontend. Destino deve obter acesso normal na rota; o alerta não concede autorização. Candidatura → gestão da vaga do dono; Banco/salvo perfil → perfil publicável; convite/salvo vaga/cancelamento/estado → vaga; mensagem → sala autorizada. Nenhum link/tipo/mensagem de notificação vem do cliente. Registros históricos não foram migrados.

## 39. Idempotência

Identidade pertence ao produtor: locks/estado RF06, INSERT RF13, convite lógico RF42, mensagem real RF35, ON CONFLICT RF19 e transições RF28/RF23. Mesmo objeto NotificacaoEvento no mesmo TX é registrado por identidade na sincronização transacional e processado uma vez; eventos distintos com mensagem igual continuam válidos. GET/PATCH/reconexão não inserem. Reentrega NotificacaoPersistida não insere segunda linha; pode repetir transporte e não oferece garantia distribuída exatamente uma vez. Sem UNIQUE novo ou deduplicação por texto.

## 40. Arquivos de produção

Seis Java existentes modificados, dentro de `backend/src/main/java/com/portifolio/`: `event/NotificacaoEventoListener.java`, `service/NotificacaoPersistenceService.java`, `controller/NotificacaoController.java`, `service/CandidaturaService.java`, `service/VagaService.java`, `realtime/NotificacaoSseService.java`. Núcleo/listagem/count/DTO/repositório/enum e produtores RF13/RF42/RF35 permaneceram iguais. Sem produção nova não rastreada nesta tarefa.

## 41. Arquivos de teste

Cinco existentes ampliados/adaptados: `controller/NotificacaoRf23IntegrationTest.java`, `controller/VagaCancelamentoRf25IntegrationTest.java`, `event/NotificacaoEventoListenerTest.java`, `realtime/NotificacaoWebSocketRf23IntegrationTest.java`, `realtime/NotificacaoSsePoolIntegrationTest.java`. Novo não rastreado: `controller/NotificacaoRf36IntegrationTest.java` (34 cenários). Paths sob `backend/src/test/java/com/portifolio/`. Nenhum assert eliminado para obter verde; WS/SSE passaram de 10 para 24 cenários e o cleanup passou a exigir zero emitters.

## 42. Banco alterado

**NÃO.** Nenhum SQL, snapshot ativo/histórico, bootstrap, seed, migration ou script de banco foi editado nesta tarefa. SQL histórico já sujo na chegada permaneceu byte a byte igual ao baseline.

## 43. Frontend alterado

**NÃO.** 770 arquivos de frontend protegidos iguais ao baseline, incluindo deltas anteriores. Recursos copiados pelo Maven apenas em diretórios gerados ignorados do backend. Sino/popup/estilos/cores/React/reconexão/SSE/STOMP clients não alterados.

## 44. Database05 46/46

ZIP oficial `C:\Users\masca\Downloads\palco-database05.zip`, SHA-256 `6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf`. **46/46 arquivos byte a byte iguais, mesmos paths, zero divergentes/ausentes/adicionais**. Database04: 47 arquivos e cinco scripts históricos preservados. `snapshot-final-byte-a-byte.json`, `preservacao-final.json`, `fontes-testadas-final.json`.

## 45. Focados

**660 total / 660 passed / 0 failures / 0 errors / 0 skipped**, 25 classes reais, BUILD SUCCESS/exit 0, **05:31 min**, término 04/10/2026 14:55:18 -03:00. Comando em backend: `./mvnw.cmd test '-Dspring.test.mockmvc.print=NONE' '-Dtest=<lista real>'`; lista em `classes-focadas.json`. Sem clean. Núcleo/WS/SSE/interceptor/RF06/RF13/RF17/RF19/RF28/RF42/RF35/RF22/JWT/segurança/database05/mapeamento/menor/prazo incluídos.

Tentativas anteriores preservadas: diagnostico-01/02/03/04 pararam na compilação dentro do sandbox, zero testes; processo compilador separado e diretório gerado novo não resolveram. Diagnóstico filtrado de classpath também falhou sem executar testes. Classes existiam e javap as lia; fora do sandbox, a mesma bateria compilou. Causa interna da restrição não confirmada, sem imputar App Control/JDT. diagnostico-05: 65 total/64 passed/0 failures/1 error/0 skipped, erro real do novo teste cleanup SSE; corrigido sem retirar assert. Não somar rodadas sobrepostas ao total final.

## 46. Maven completo

**1390 total / 1373 passed / 0 failures / 0 errors / 17 skipped, 67 classes; BUILD SUCCESS, exit 0; 08:48 min; 2026-10-04T15:05:07-03:00**. Comando autorizado exato em backend: `./mvnw.cmd test '-Dspring.test.mockmvc.print=NONE'`, sem clean, sem parâmetros de fork/diretório alternativo na rodada final; executado fora do sandbox necessário à compilação. XMLs apenas desta execução, cópias sem bloco properties para evitar guardar ambiente sensível. **Os 17 skips condicionais são exatamente os mesmos do checkpoint RF22; zero novos skips**, comparados por classe/nome em `skips-final.json`. Fontes SHA-256 após focados continuam iguais após validações finais.

## 47. Semgrep

Docker oficial `semgrep/semgrep@sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b`, versão **1.178.0**; ruleset p/java local oficial em cache SHA-256 `5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1`. **349 arquivos Java / 60 regras / ~100.0% linhas parseadas / 0 findings / 0 blocking / 0 errors / exit 0, 29.2 s**. Escopo main+test Java, `--no-git-ignore`, inclui teste RF36 não rastreado; mount read-only, network none, metrics off. Sem Semgrep nativo Windows, TLS desabilitado, instalação ou alteração de App Control/Defender. **Não foi executado secrets scan.**

Primeira rodada preservada em `semgrep-java-primeiro-*`: engine exit 0, 276 arquivos de produção, 60 regras, ~100% parse, zero findings/errors/blocking, 446,3 s; a checagem de cobertura falhou porque o filtro padrão ignorou test/. A rodada final usa `.semgrepignore` sem exclusões montado **somente no container**, conforme [documentação oficial](https://docs.semgrep.dev/ignoring-files-folders-code), mantendo arquivos/configuração do checkout intactos. Paths do container são normalizados ao path real backend/src para confirmar cobertura de cada Java alterado e do novo teste. Não somar scans sobrepostos nem tratar o flag de intenção includesUntracked da primeira rodada como prova de cobertura; os seis paths ausentes daquela rodada ficaram explícitos no JSON.

**0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.**

## 48. Graphify final

`graphify update .` para atualização AST depois do Java, **exit 0, 26.1 s; 6214 nós / 21299 arestas / 356 comunidades; 91% EXTRACTED / 9% INFERRED / 0% AMBIGUOUS**. MCP HTTP final initialize + graph_stats confirma o grafo; get_node reconhece o novo NotificacaoRf36IntegrationTest na fonte atual, linha 47, grau 47 (`graphify-rf36-no-final.json`). Não executado label, sem benchmark/cobertura de compreensão semântica. Limites de parser SQL e inferências existentes são documentados, sem instalar parser nem tentar contornar App Control. Graphify é navegação complementar às fontes/testes; nenhuma mudança de segurança Windows.

## 49. Riscos

Sem outbox/fila durável: queda do processo entre commit e entrega pode perder popup/realtime, embora a central persistida permaneça recuperável. Transporte pode repetir; nenhuma garantia distribuída exatamente uma vez. Links continuam sujeitos à autorização/estado atual do alvo, que pode mudar depois de emitido o alerta. Deltas antigos de frontend/README/database histórico continuam no working tree e não integram o patch RF36.

## 50. Limitações

Latência local com sete sessões/produtores funcionais, sem teste de carga/P95 ou benchmark de produção. Timeout SSE de 30 minutos auditado por fonte; testes exercitam cleanup de clientes fechados/erro. Sem novo limite de conexões/preferências/esquema/outbox. Não há validação visual frontend nesta tarefa. Logs de testes negativos podem conter WARN/ERROR esperados de transporte/conexão encerrada; métricas Surefire finais distinguem isso de erro de teste.

## 51. Eventos condicionados a RF futuro

RF41/novo seguidor, OBRA quando RF16/RF40 oferecerem produtor e contexto operacionais, EDITAL quando o produtor RF14 existir: **pendência condicionada ao RF produtor futuro**. Tipo EDITAL físico foi preservado, sem gerar alerta artificial. RF44 é email ao responsável e não uma nova notificação interna.

## 52. RF36 final

**CONCLUÍDO no backend para núcleo e produtores atualmente disponíveis.** Critérios A–O: central persistente, lida individual/todas, count, STOMP seguro, SSE seguro, persistência independente do transporte, AFTER_COMMIT, falhas isoladas, entrega local ≤5s, links/privacidade, integração de produtores atuais, database05 intacto e testes verdes. RF41 não desenvolvido não bloqueia o núcleo por condicionamento oficial; RF19 SALVO e RF28 disponíveis foram cobertos, sem esconder produtor obrigatório.

## 53. Impacto nos demais RFs

RF06/SALVO/eventos RF23 passam a exigir commit atômico com alerta; falha de banco se propaga, enquanto transporte continua isolado. RF13/RF42/RF35/RF28/retirada corretos foram regredidos. RF22/matriz, RF02/RF25, RF44 e regras de menores/ownership mantidos. Não altera a conclusão própria dos outros relatórios nem conclui seus escopos futuros.

## 54. Pendências e inspeção final

Nenhum blocker do núcleo disponível. Pendências condicionais/operacionais: produtores futuros, integração visual e confiabilidade durável de transporte apenas se requerida em etapa própria. Inspeção git: **git diff --check exit 0; index vazio; git status/stat/name-status documentados em evidências finais, com avisos LF/CRLF separados de erros de whitespace**. Index vazio; nenhum staging/commit/push/reset/restore/clean/stash. Delta desta tarefa: seis Java de produção, cinco testes existentes, um teste novo e este relatório/evidências; onze arquivos existentes modificados dentro do escopo, 1.414 protegidos sem mudança. Teste/relatório não rastreados também passaram por verificação própria de whitespace porque git diff não inclui untracked.

## 55. Próximo passo

Revisar o patch e integrar visualmente central/badge/popup/reconexão em tarefa específica autorizada. Produtores futuros devem reutilizar o contrato transacional seguro quando seus RFs forem disponibilizados; nenhuma implementação antecipada aqui.

## Registro semanal — 04/10/2026

| Campo | Registro |
|---|---|
| Objetivo | Auditar/concluir RF36, preservando núcleo e corrigindo lacunas reais |
| RF/RNF | RF06/11/13/19/22/23/28/35/36/41/42/44; RNF02/05/06/07/08/09/10/17 |
| Backend alterado | SIM — seis Java e testes descritos acima |
| Frontend alterado | NÃO |
| Banco alterado | NÃO |
| Database05 | 46/46 byte a byte iguais; zero divergentes/ausentes/adicionais |
| Central | Persistente, paginada, somente próprio usuário, DTO mínimo |
| Lidas | Uma/todas idempotentes, owner e bulk SQL |
| Badge | COUNT SQL por destino/lida=false |
| WebSocket | CONNECT JWT, ERROR/close negativos, fila privada, ≤5s local |
| SSE | Header, isolamento, cleanup real; finalização tolera contexto encerrado |
| Candidatura | Atômica com alerta e link de gestão do dono |
| Banco de Talentos | Produtor correto preservado/regredido |
| Convite | Primeiro lógico único; sem candidatura automática |
| Mensagem | Outro participante; sem corpo privado |
| Salvos | Perfil/vaga; primeira inserção, sem ator privado, sem “desalvou” |
| Seguidores | Condicionado RF41 futuro; não implementado |
| Cancelamento de vaga | RF28 CANDIDATURA consolidado, histórico/transação preservados |
| Privacidade | Whitelist/valores privados conhecidos e links internos |
| Testes | Focados 660/660; completo 1390 total / 1373 passed / 0 failures / 0 errors / 17 skipped, 67 classes; BUILD SUCCESS, exit 0; 08:48 min; 2026-10-04T15:05:07-03:00 |
| Semgrep | 349 arquivos Java / 60 regras / ~100.0% linhas parseadas / 0 findings / 0 blocking / 0 errors / exit 0, 29.2 s |
| Graphify | exit 0, 26.1 s; 6214 nós / 21299 arestas / 356 comunidades; 91% EXTRACTED / 9% INFERRED / 0% AMBIGUOUS |
| RF36 final | CONCLUÍDO backend, núcleo e produtores disponíveis |
| Eventos futuros | RF41/OBRA/EDITAL condicionados aos produtores |
| Pendências | Sem blocker; integração visual e limites operacionais documentados |
| Próximo passo | Revisão do patch; frontend/eventos futuros em escopo próprio |

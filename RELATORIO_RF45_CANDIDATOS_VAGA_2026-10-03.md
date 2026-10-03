# RF45 — Visualização e contato com candidatos da vaga — 03/10/2026

**Estado: RF45 backend continua concluído após a correção final.** Focados 163/163, Maven completo 914 total/897 passed/0 failures/0 errors/17 skipped e Semgrep Java final sem findings/erros. Integração visual e RF16 por projetos permanecem pendentes. Escopo: backend, testes e evidências. Banco/SQL/frontend preservados; sem staging, commit ou push.

Checkout: `C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline`.
Branch: `integracao-recuperada-2026-09-15`. HEAD inicial: `5e9efbc`.

## 1. Objetivo

Permitir ao CONTRATANTE proprietário consultar candidatos atuais de sua vaga, acessar o perfil público e criar/reabrir a conversa RF35. Preservar o histórico e todas as regras de criação, retirada, recandidatura, concorrência e notificações RF06. Não introduzir aceite/rejeição formal.

## 2. Requisitos e fontes consultados

Documento oficial: `C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt`. Consultados RF45 integral, RF06, RF05, RF10, RF35, RF36 e fronteira RF44; RNF02, RNF07, RNF08 e RNF09. RF45: linhas 3798–3866; RF06: 593–685; RF10: 981 em diante; RF35: 3022 em diante.

Na correção final de 03/10, consultados novamente RF10 (981–1059), RF16 (1373–1461), RF27 (2438–2516) e RF44 (3729–3797), juntamente com a decisão funcional consolidada do usuário. Autorização inicial RF27 libera funcionalidades permitidas; PUBLICADO é público e RASCUNHO privado, sem nova aprovação manual por publicação. RF44 é aviso informativo posterior. A regra persistida que bloqueia consentimento revogado foi mantida como defesa vigente; não se criou um fluxo novo de revogação.

Lidos os relatórios RF06, RF01/RF24, sincronização database04 e registro semanal de 02/10, além da auditoria RF06/RF45 de 28/09. As falhas de cadastro/UNIQUE e restrições de snapshots antigos nesses históricos não descrevem o checkpoint atual. Não foram reescritos esses documentos.

Auditados controllers, DTOs, services, repositories, entidades, JWT/REST/STOMP, políticas de acesso, testes e SQL oficial aplicável. O cliente foi somente lido para verificar o contrato. A skill `pji-backend-audit` orientou a rastreabilidade; prevaleceram as instruções desta tarefa, inclusive database04 e Maven sem clean.

## 3. Estado inicial

Confirmados os checkpoints `5e9efbc`, `30c78ea` e `b73e205`. O working tree já tinha README, SQL histórico, numerosas alterações de frontend e artefatos não rastreados. Nenhum desses deltas foi atribuído ao RF45 ou revertido.

Baseline RF06 documental: 867 total / 850 passed / 0 failures / 0 errors / 17 skips, BUILD SUCCESS. Não se confundiu esse resultado anterior com a validação desta tarefa. Manifesto inicial: 1.143 arquivos; 569 classificados como frontend e 143 como banco/SQL. Evidências em `evidencias/rf45-2026-10-03/arquivos-iniciais.json` e `git-status-inicial.txt`.

## 4. Auditoria do que já existia

`GET /api/vagas/{id}/candidaturas` já delegava a `CandidaturaService.listarPorVaga`, com usuário do SecurityContext, papel, ownership, DTO paginado, página de IDs e carregamento em lote. Query e count incluíam todos os estados/tentativas. A ordenação era compatibilidade de funções, atualização do perfil e ID; foi preservada.

RF06 já contava todas as tentativas e serializava criação/retirada. `VagaService.buscarPorId` já escolhia a última por ID e o cancelamento preservava estados históricos. PUT formal do contratante já retornava 422; não foi reimplementado.

RF35 já criava/reutilizava salas por par sob advisory lock PostgreSQL, identificava o ator pelo Principal e restringia histórico/envio/leitura/edição/exclusão aos participantes/autores. REST e STOMP usam o mesmo ChatService. CONNECT valida JWT; SUBSCRIBE permite somente filas privadas; SEND aceita somente rota de mensagem. Entrega ocorre após commit e falha realtime é isolada. Faltava validar a autorização vigente do menor alvo ao abrir/reabrir sala.

RF10 já usava DTO público de lista branca e cidade/UF, sem experiência ou responsável, mas barrava todos os menores. Isso bloqueava diretamente “Ver perfil” do candidato autorizado e contrariava o requisito revisado.

Auditoria adicional RF16: `PortfolioArquivoService` e `PortfolioVideoService` implementam o MVP de mídias ligadas diretamente ao artista. `portfolio_arquivos`/`embeds_externos`, entidades e rotas não possuem projeto, status de publicação ou vínculo projeto→mídia. Database04 declara `status_projeto_portfolio_enum`, mas nenhuma tabela/coluna usa esse enum. Galerias existentes não substituem projetos RF16. Portanto não há RASCUNHO/PUBLICADO operacional a exercitar neste checkout. Essa lacuna preexistente foi registrada, sem criar tabelas, rotas ou estados improvisados nesta correção. O aviso RF44 atual é de candidatura; não há evento de publicação de portfólio.

## 5. Arquivos de produção alterados

Paths relativos ao checkout:

| Arquivo | Alteração |
|---|---|
| `backend/src/main/java/com/portifolio/repository/CandidaturaRepository.java` | Filtro da tentativa atual antes de paginação/count, no caminho oficial e fallback legado |
| `backend/src/main/java/com/portifolio/service/CandidaturaService.java` | Papel antes de consultar vaga, chamada filtrada legado e username na projeção; mutações RF06 intactas |
| `backend/src/main/java/com/portifolio/dto/CandidaturaVagaResponse.java` | Campo público aditivo `username` |
| `backend/src/main/java/com/portifolio/security/MenorAutorizadoPolicy.java` — novo | Idade 14–17, ARTISTA, conta ATIVA e consentimento persistido vigente |
| `backend/src/main/java/com/portifolio/service/PerfilPublicoService.java` | Permitir menor autorizado no fluxo público existente, mantendo DTO restrito |
| `backend/src/main/java/com/portifolio/service/ChatService.java` | Revalidar proteção do menor nos dois participantes antes de criar/reutilizar sala |
| `backend/src/main/java/com/portifolio/service/PortfolioAccessService.java` | Correção final remove o bloqueio etário geral incorreto; reutiliza RF10/RF27 e conta apta. O arquivo volta ao conteúdo do checkpoint, sem delta final contra HEAD |

Arquivos desta correção final: `PortfolioAccessService.java`, `PortfolioRf16IntegrationTest.java`, `CandidatosVagaRf45IntegrationTest.java`, este relatório e evidências em `evidencias/rf45-2026-10-03/correcao-portfolio-menor/`. `PerfilPublicoService`/`MenorAutorizadoPolicy` foram auditados e permanecem como implementados no RF45. Não houve edição adicional de RF06/chat, frontend, SQL ou documentos históricos.

## 6. Comportamento implementado

O proprietário recebe somente a última linha do par vaga/artista se ela estiver PENDENTE ou EM_ANALISE. Retirada, cancelamento e estados formais/terminais são históricos. Leitura não grava candidatura, não modifica status/data/ID, não gera notificação/aviso RF44 nem consome tentativa. Não foi adicionada exigência de vaga ABERTA para consultar a lista; regras de criação/retirada continuam RF06. Menor autorizado pode abrir o perfil público e usar a mesma conversa RF35 com o contratante ao qual está vinculado profissionalmente.

## 7. Endpoints e contrato final

| Ação | Endpoint/contrato |
|---|---|
| Candidatos atuais | `GET /api/vagas/{id}/candidaturas?page=0&size=20`, JWT do proprietário; 200 com `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`, `hasNext`, `hasPrevious` |
| Perfil | `GET /api/perfis/publicos/ARTISTA/{artistaId}`, público, DTO RF10 |
| Conversar | `POST /api/chat/salas`, JWT e `{"usuarioDestinoId":artistaId}`; 201 com `salaId` e resumo público do outro participante, inclusive no reuso |

Nenhuma rota nova, nenhum módulo paralelo de chat, nenhum parâmetro de ownership aceito do cliente. `artistaId` já é o identificador necessário às duas ações. `username` é público, sem `@` armazenado, aditivo; a navegação existente por ID permanece compatível.

## 8. Autenticação

SecurityConfig exige JWT para candidatos e chat. JwtAuthFilter valida assinatura/validade, subject/ID e acesso persistido da conta. CandidaturaService resolve o usuário pelo SecurityContext, enquanto ChatController usa Principal. Anônimo/token inválido: 401. Não foi alterado o filtro nem a política global de autenticação.

## 9. Autorização

Listagem exige CONTRATANTE no service. ARTISTA recebe 403 inclusive com ID de vaga inexistente. O ID da vaga apenas identifica o recurso; o usuário do token permanece autoridade. Parâmetro extra `contratanteId` não concede acesso. Erros não são convertidos em lista vazia bem-sucedida.

## 10. Ownership

Após verificar papel e existência, compara-se `vaga.contratante.usuarioId` ao ID persistido do autenticado; divergência: 403. Todas as consultas operacionais usam exclusivamente a vaga autorizada. Testes inserem candidatos em outra vaga do mesmo dono e em vaga de outro contratante, sem vazamento. Sala alheia continua 404 pelo guard de participação RF35.

## 11. Definição de candidatura atual e legados

Regra: `status IN ('PENDENTE','EM_ANALISE') AND NOT EXISTS linha posterior do mesmo par com id maior`. Aplicada tanto ao SELECT paginado quanto ao COUNT. RETIRADA/CANCELADA_POR_VAGA/ACEITA/REJEITADA/BLOQUEADA não integram a visão operacional.

Decisão conservadora explícita para dados legados inconsistentes: duas ativas → somente a de maior ID; uma ativa antiga com linha posterior terminal → não ressuscitar a tentativa antiga. ID identifica a tentativa conforme o histórico RF06 já ordenado por ID; datas não são reescritas. Isso é uma regra de leitura, não saneamento, e não muda a regra RF06 de bloquear criação se existir qualquer ativa. Status NULL também não é ativo. Casos legados estão parametrizados nos testes.

## 12. Recandidatura

RETIRADA + PENDENTE/EM_ANALISE retorna somente a segunda linha e `totalElements=1`. O filtro ocorre no PostgreSQL antes de LIMIT/OFFSET/count. Não é filtro de UI nem paginação em memória; não produz página incompleta por descartar histórico depois da consulta.

## 13. Histórico RF06

Nenhuma linha é apagada, reaproveitada ou reativada. IDs, timestamps, status e conteúdo histórico permanecem. Testes comparam todas as colunas antes/depois, consultam a tentativa antiga pelo endpoint histórico do artista e verificam conflito ao tentar criação indevida. A suíte RF06 exerce primeira/segunda tentativa, terceira bloqueada, concorrência, retirada e notificações/RF44. Métodos de criação/retirada e VagaService não receberam edição funcional.

## 14. DTO/projeção

Reutilizado `CandidaturaVagaResponse` e seu envelope. Retorna identificação da candidatura/artista, username, nome público, avatar, cidade/UF formatada, biografia/URL profissional, IDs de funções e compatibilidade, status/data da tentativa atual. Mensagem/link opcionais legados foram mantidos para compatibilidade e não tornados obrigatórios. Não se embute perfil completo, entidades JPA, responsável ou sala criada automaticamente na listagem.

## 15. Privacidade e correção necessária RF10

DTOs excluem CPF/CNPJ, telefone/e-mail privado, nascimento, senha/hash, tokens, consentimento, responsável, endereço completo, autodeclarações e experiência. A experiência por área não é projetada nem no candidato nem no perfil público. Nenhum novo campo privado foi acrescentado.

Para o acesso público de 14–17 anos, a política exige ARTISTA, ATIVA e responsável com data de consentimento não nula e sem revogação. Ausência/pendência/revogação/conta bloqueada/idade abaixo de 14 continua 404 no perfil e 422 ao abrir sala. Contratante menor permanece privado. Adultos preservam o contrato anterior RF10; sua homologação integral e demais abas não foram ampliadas.

A mudança RF10 é pequena, coberta e diretamente necessária ao RF45. Um teste RF27 e uma assertion pública do RF13 tinham a premissa anterior de privacidade total mesmo após autorização; foram adequados ao requisito revisado sem flexibilizar consentimento ou elegibilidade do Banco de Talentos.

Contrato consolidado: **“Para artista menor de 14–17 anos com autorização vigente e conta apta, projetos PUBLICADOS do RF16 e suas mídias são públicos conforme RF10/RF16. RASCUNHOS permanecem privados. Experiência, dados pessoais e dados do responsável continuam restritos. A publicação não exige nova aprovação manual do responsável; RF44 prevê aviso informativo posterior quando o evento correspondente estiver implementado.”**

A implementação anterior RF45 havia acrescentado uma restrição etária geral em `PortfolioAccessService` para satisfazer a expectativa antiga de um teste. Essa interpretação era incorreta e foi retirada. O guard continua consultando o perfil público RF10/RF27, inclusive em acesso direto ao arquivo, e exigindo conta apta. Menor sem autorização, consentimento pendente/revogado ou conta bloqueada continua sem exposição pública. Gestão/edição/exclusão continuam restritas ao proprietário; upload, MIME, tamanho, URLs seguras e headers permanecem intactos.

Limite da evidência: o MVP existente disponibiliza arquivos/vídeos diretamente, sem projeto ou etapa separada de publicação. Os testes desta correção validam essa mídia pública e o acesso privado do titular; não provam visibilidade de projetos PUBLICADOS nem isolamento de RASCUNHOS inexistentes no modelo atual. Não se adicionou RF16 por projetos nem RF40, e não se criou aprovação manual por conteúdo. A regra consolidada acima deve ser aplicada quando o fluxo por projetos for implementado.

## 16. Reuso RF35 e decisão sobre o relatório antigo

`artistaId` retornado na lista alimenta o endpoint já existente. Os testes percorrem candidato listado → sala → mesma sala em repetição e sentido inverso, inclusive menor autorizado. O contrato 201 no reuso foi preservado. Conta/consentimento do menor são relidos inclusive ao reabrir sala existente. Menor sem vínculo com aquele contratante não permite contato aleatório.

A auditoria de 28/09 propunha contexto obrigatório vaga+candidatura. O pedido atual manda reutilizar o endpoint adequado e não adicionar regras além de RF45/RF35. RF35 permite conversa direta adulta e exige interação profissional anterior para menor, sem obrigatoriedade de candidatura ainda ativa para continuar conversa. Portanto não se criou rota contextual redundante nem se restringiram outras origens legítimas do chat adulto. Para menor, mantém-se a prova de candidatura persistida em vaga daquele contratante, inclusive histórica, agora somada à autorização vigente. A listagem RF45 em si continua somente atual.

Anexos, anonimização RF22, retenção e homologação integral RF35 não foram implementados por esta tarefa. Não se declara RF35 integralmente concluído.

## 17. Paginação/ordenação

Página zero, size padrão 20, teto server-side 50; valores maiores continuam limitados a 50, conforme contrato existente. Negativo/zero inválido/tipo malformado/overflow: 400. Não há sort livre nem novos filtros.

Ordenação preservada: número de funções coincidentes DESC, atualização do perfil DESC NULLS LAST, candidatura ID ASC como desempate único. Fallback legado mantém data DESC/ID DESC. Testes verificam contagem filtrada, páginas repetidas iguais, páginas diferentes sem duplicata e limite de 50. Alterações simultâneas na lista/perfil entre requisições podem deslocar páginas de OFFSET; não foi criada sessão de snapshot ou cursor novo.

## 18. Queries, desempenho e N+1

Reutilizado o plano existente: página de IDs no banco e busca detalhada em lote somente dessa página, com EntityGraph das relações profissionais. O COUNT usa o mesmo predicado atual. Vaga/IDs de funções são vinculados por parâmetros; estados e ORDER BY são constantes do servidor. Sem concatenação de entrada do usuário e sem SELECT ilimitado seguido de paginação em memória.

O EntityGraph evita carregamento de funções por candidato; DTO é mapeado dentro de transação de leitura. Não foi criado um segundo repository/projeção duplicada. O lote limitado reutiliza entidades internas existentes; dados internos não passam à resposta. A ausência de índice composto par+ID pode custar mais em vagas com histórico volumoso: não foi adicionado índice; teste de consultas mede N+1, não um benchmark de produção.

## 19. HTTP

| Cenário | Código |
|---|---:|
| Consulta do proprietário, inclusive vazia | 200 |
| Ausência/token inválido | 401 |
| Papel incorreto/outro contratante | 403 |
| Vaga inexistente para contratante | 404 |
| Paginação malformada/negativa/inválida | 400 |
| Sala criada/reutilizada RF35 | 201 |
| Menor sem autorização/vínculo profissional para abertura | 422 |
| Sala alheia | 404 |
| PUT formal do contratante | 422, contrato RF06 preservado |

Handler global continua fornecendo erro sanitizado, sem stacktrace/detalhes SQL.

## 20. Banco alterado: NÃO

Database04 é a única fonte ativa. Nenhum SQL, enum, migration, índice, constraint, seed, init, procedure, função, trigger ou snapshot recebeu escrita. `ddl-auto=validate` mantido. Testes usam somente bancos PostgreSQL descartáveis inicializados com o pacote oficial, com fixtures/limpeza DML; não operam o banco de desenvolvimento. Não existe impedimento estrutural comprovado para RF45.

Conferência atual: **46/46 arquivos do pacote idênticos ao ZIP, zero divergências e zero arquivos adicionais**. ZIP SHA-256 `52b1c4af06d79a7efae47e6fa320b4a0a32359efaae1d40e2a73d06129c6df2b`. Evidência: `database04-preservado.json`. Comparação do manifesto inicial registra zero alterações de frontend/banco/SQL e zero arquivos removidos (`preservacao-e-delta.json`).

Na correção final, o novo baseline preserva os 1.146 arquivos existentes antes do ajuste e registra somente os quatro arquivos autorizados alterados. Os **569 arquivos frontend e 143 banco/SQL** conferidos têm zero diferenças e nenhuma remoção. Evidências em `correcao-portfolio-menor/arquivos-antes-correcao.json` e `correcao-portfolio-menor/preservacao-e-delta.json`. A lacuna RF16 por projetos está demonstrada em `correcao-portfolio-menor/auditoria-limites-rf16.json`; não houve mudança estrutural para contorná-la.

## 21. Frontend alterado: NÃO

Nenhum arquivo frontend foi editado, formatado, movido ou revertido. Nenhum npm/build/test de frontend. A etapa Maven copia recursos existentes para o diretório de build backend, sem editar as origens. As alterações locais anteriores permanecem preservadas.

## 22. Alterações fora do backend

Somente este relatório e `evidencias/rf45-2026-10-03/` para baseline, logs e resultados. Registro semanal inserido abaixo, sem sobrescrever registros anteriores. A atualização AST Graphify foi tentada por exigência AGENTS, mas o runtime Windows foi bloqueado; derivados não foram tratados como código/versionamento obrigatório. Nenhuma configuração de segurança do Windows foi alterada.

## 23. Testes adicionados

Nova classe `backend/src/test/java/com/portifolio/controller/CandidatosVagaRf45IntegrationTest.java`: **40 casos**, preservados na correção. Inclui 401/403/404/200, parâmetros inválidos, página vazia, primeira ativa, cinco terminais, duas recandidaturas, dados legados duplicados/terminais posteriores, outras vagas/donos, contagem/ordem/páginas/limite, N+1, privacidade, menor autorizado 14/17, autorização ausente/pendente/revogada/bloqueada/idade inferior, criação/reuso RF35 e recusa de ACEITA/REJEITADA. Agora o candidato de 14/17 acessa perfil e mídia de vídeo pública pelo fluxo real; a autorização é revalidada após revogação no perfil, abas de mídia e abertura/reuso de chat. Usa `OfficialPostgreSQLContainer`, database04 e PostgreSQL real.

## 24. Testes existentes alterados

- `ChatRf24IntegrationTest.java`: fixture do teste de menor recebe consentimento válido; mantém bloqueio sem interação e criação com candidatura. Sem retirar testes.
- `GuardianConsentRf27IntegrationTest.java`: menor pendente continua 404; após decisão real autorizadora, perfil público 200 com campos privados ausentes. Fluxo real de consentimento permanece exercitado.
- `TalentoRf13IntegrationTest.java`: mantém menor fora da busca/Banco de Talentos (total zero), ajusta a consulta pública para RF10 revisado e acrescenta assertions de privacidade. A limpeza existente passa a ocorrer também antes de cada cenário, evitando que artistas do seed interfiram no primeiro teste; nenhuma regra de produção RF13 mudou.
- `PortfolioRf16IntegrationTest.java`, nesta correção: substitui a premissa antiga de 404 geral pelo contrato de publicidade de mídia do menor autorizado de 14/17, mantendo acesso privado do titular e ownership. Acrescenta cinco cenários de autorização/conta inválida e um de revogação após acesso público bem-sucedido; verifica bytes, Range, vídeo seguro, ausência de campos privados e manutenção do consentimento inicial após disponibilizar conteúdo. Mantém os cenários adultos, validação de upload, limites, rollback e acesso de terceiros. Total da classe passa de 19 para 26 casos.

`CandidaturaControllerRf06IntegrationTest` e `PerfilPublicoRf10IntegrationTest` não foram editados. Nenhum teste foi desabilitado/convertido em skip; nenhum SQL de fixture estrutural alterado. Os critérios de projeto PUBLICADO/RASCUNHO não são registrados como testes aprovados, pois o modelo/fluxo correspondente ainda não existe; acesso privado do proprietário às mídias reais é coberto.

| Critério solicitado na correção | Evidência e limite |
|---|---|
| A/B — menor autorizado 14/17 | Perfil 200, mídia existente visível, bytes públicos/Range e vídeo seguro; dados privados/experiência ausentes. Projeto PUBLICADO não é representado no MVP |
| C — RASCUNHO privado para terceiros | Não executável neste checkout: não existe projeto/estado/rota de rascunho. Regra consolidada preservada como obrigação do RF16 por projetos, sem alegar aprovação |
| D — menor sem autorização | Cinco cenários RF16 e cinco RF45 mantêm perfil/abas/download bloqueados conforme o endpoint |
| E — revogação | Conteúdo antes público volta a 404, incluindo download por ID/Range; linhas e bytes permanecem armazenados. Perfil/chat RF45 também revalidam |
| F — proprietário | Acesso privado às suas mídias confirmado; terceiros continuam sem editar/excluir/acessar a rota privada. Rascunho não existe para validar |
| G — adulto | Cenários RF16 adultos existentes, segurança de upload/URLs, ownership e limites mantidos |
| H — RF45/RF06/RF35 | 40 RF45, 57 RF06 e 15 chat, incluindo candidato menor listado, perfil e sala criada/reutilizada; nenhuma edição adicional RF06/RF35 |

## 25. Bateria focada

**Correção final: 163 total / 163 passed / 0 failures / 0 errors / 0 skipped, BUILD SUCCESS, exit 0**, **02:12 min**, término **2026-10-03T09:59:05-03:00**. As sete classes requeridas e seus totais: `CandidatosVagaRf45IntegrationTest` 40, `PerfilPublicoRf10IntegrationTest` 12, `PortfolioRf16IntegrationTest` 26, `GuardianConsentRf27IntegrationTest` 10, `GuardianConsentServiceTest` 3, `ChatRf24IntegrationTest` 15, `CandidaturaControllerRf06IntegrationTest` 57. PostgreSQL/Testcontainers/database04 oficial e `ddl-auto=validate` mantidos.

Comando em `backend`: `.\mvnw.cmd '-Dtest=<as sete classes acima, separadas por vírgula>' '-Dspring.test.mockmvc.print=NONE' test`. Conferidos somente os sete XMLs correspondentes; a soma coincide com o resumo Maven. Evidências em `correcao-portfolio-menor/focados.log`, `.exit`, `focados-totais.json` e `focados-reports/`, com propriedades de runtime removidas das cópias XML. Resultados abaixo pertencem à etapa anterior e não substituem essa revalidação.

**Bateria RF45 anterior à correção: 270 total / 270 passed / 0 failures / 0 errors / 0 skipped, BUILD SUCCESS, exit 0**, **03:50 min**, encerrada às **09:13:53 de 03/10/2026**, America/Sao_Paulo. Soma dos 17 XMLs selecionados coincide com o resumo Maven; não foram incluídos resultados antigos de classes fora da seleção.

JDK **21.0.11**, Spring Boot **4.0.6**, PostgreSQL real via `postgres:18-alpine`/Testcontainers **2.0.5**, pacote database04 validado e `ddl-auto=validate`. Docker estava desligado no primeiro diagnóstico e foi iniciado para executar os testes. Não foi usada alternativa H2 nem banco histórico/local de desenvolvimento.

| Classe | Passed/total |
|---|---:|
| CandidatosVagaRf45IntegrationTest | 40/40 |
| CandidaturaControllerRf06IntegrationTest | 57/57 |
| ChatRf24IntegrationTest | 15/15 |
| ChatWebSocketRf24IntegrationTest | 1/1 |
| ChatEventoListenerTest | 1/1 |
| PerfilPublicoRf10IntegrationTest | 12/12 |
| TalentoRf13IntegrationTest | 42/42 |
| VagaPrazoRf23Rf06IntegrationTest | 9/9 |
| VagaCancelamentoRf25IntegrationTest | 30/30 |
| NotificacaoRf23IntegrationTest | 11/11 |
| NotificacaoWebSocketRf23IntegrationTest | 3/3 |
| NotificacaoSsePoolIntegrationTest | 1/1 |
| GuardianConsentRf27IntegrationTest | 10/10 |
| GuardianConsentServiceTest | 3/3 |
| JwtAuthenticationIntegrationTest | 4/4 |
| GenericEndpointsSecurityIntegrationTest | 26/26 |
| Database04BootstrapIntegrationTest | 5/5 |
| **Total** | **270/270** |

Comando em `backend`: `.\mvnw.cmd '-Dtest=<as 17 classes acima, separadas por vírgula>' '-Dspring.test.mockmvc.print=NONE' test`. A propriedade apenas suprime dumps de respostas MockMvc, sem desabilitar testes. Evidências: `focados-consolidacao.log`, `.exit`, `focados-totais.json` e `focados-reports/`; nas cópias XML somente o bloco de propriedades de runtime foi removido, preservando métricas/casos/assertions/logs.

Teste N+1: página com 20 candidatos/funções aprovada dentro do teto de 10 statements e sem aumento proporcional em relação a 2 candidatos. Isso valida carregamento constante nos cenários, sem alegar benchmark de carga.

Bateria adicional anterior, sob a premissa posteriormente corrigida: **71/71, 0 failures/errors/skipped, BUILD SUCCESS, exit 0, 01:22 min**, término às **09:26:27**. Classes: `CandidatosVagaRf45IntegrationTest` 40, `PerfilPublicoRf10IntegrationTest` 12 e `PortfolioRf16IntegrationTest` 19. Há casos repetidos entre baterias; não somar execuções como quantidade única. Evidências preservadas: `privacidade-portfolio.log`, `.exit`, `privacidade-portfolio-totais.json` e `privacidade-portfolio-reports/`.

Diagnósticos preservados: primeira execução compilou, mas Docker estava desligado (18 total, 14 erros de infraestrutura, antes dos cenários de integração). Após iniciar Docker, a primeira bateria (228 total, 9 failures) identificou limpeza insuficiente de salas na nova fixture e uma premissa antiga RF27. A bateria ampliada seguinte (270 total, 1 failure) demonstrou interferência do seed no primeiro cenário RF13 após mudar o nome do teste. Corrigida a limpeza inicial, sem alteração de banco/produção para esses erros de fixture. Logs `focados.log`, `focados-final.log` e `focados-validacao.log` preservam os diagnósticos; o resultado final é separado.

## 26. Maven completo

**Correção final: 914 total / 897 passed / 0 failures / 0 errors / 17 skipped, BUILD SUCCESS, exit 0**, **07:09 min**, término **2026-10-03T10:08:35-03:00**. Comando em `backend`: `.\mvnw.cmd test '-Dspring.test.mockmvc.print=NONE'`, após carregar `scripts/database04/environment.ps1` e definir `PALCO_TEST_DATABASE04_PATH` para o pacote oficial. JDK 21.0.11, PostgreSQL/Testcontainers/database04 e `ddl-auto=validate`, sem Maven clean. A propriedade somente suprime dumps MockMvc.

Conferidos **60 XMLs da execução atual**, cuja soma coincide com o resumo Maven. Os mesmos 17 skips condicionais permanecem: 4 `CurrentLocalSchemaIntegrationTest` e 13 `OfficialLocalApiIntegrationTest`. Evidências finais em `correcao-portfolio-menor/maven-completo.log`, `.exit`, `completo-totais.json` e `completo-reports/`. Somente propriedades de runtime foram removidas das cópias XML; resultados/casos/logs foram mantidos. Os resultados seguintes são anteriores à decisão consolidada e foram preservados como histórico.

Controle de privacidade das evidências finais da correção: zero tokens assinados e zero valores locais conhecidos de JWT_SECRET/DB_PASSWORD encontrados. Isso é conferência restrita de evidências, não scan secrets completo; registro em `correcao-portfolio-menor/evidencias-privacidade.json`.

**Bateria anterior à correção: 907 total / 890 passed / 0 failures / 0 errors / 17 skipped, BUILD SUCCESS, exit 0**, em **07:26 min**, término **2026-10-03T09:35:09-03:00**. Comando executado em `backend`: `.\mvnw.cmd test '-Dspring.test.mockmvc.print=NONE'`, após carregar `scripts/database04/environment.ps1` e definir `PALCO_TEST_DATABASE04_PATH` para o pacote oficial local. A propriedade apenas suprime dumps MockMvc. JDK 21.0.11, PostgreSQL real/Testcontainers, database04 e `ddl-auto=validate`; sem Maven clean.

Na execução anterior à correção, conferidos **60 XMLs Surefire**, produzidos em `backend/target-maven/database04/surefire-reports`, cuja soma coincidia com o resumo Maven. Resultados antigos em outros diretórios não entraram na contagem. Evidências anteriores preservadas: `maven-completo-final.log`, `.exit`, `completo-final-totais.json` e `completo-final-reports/`. Nas cópias XML, somente o bloco de propriedades de runtime foi removido; casos, métricas e logs foram preservados. O controle anterior das 105 evidências textuais locais encontrou zero tokens assinados e zero valores locais conhecidos de JWT_SECRET/DB_PASSWORD; isso não substitui scan secrets (`evidencias-privacidade.json`).

Primeira regressão completa anterior: **907 total / 889 passed / 1 failure / 0 errors / 17 skipped**, BUILD FAILURE, 07:04 min, término 09:22:17. Única falha: `PortfolioRf16IntegrationTest.menorAtivoComConsentimentoSomentePrivado`, listagem pública recebeu 200 contra uma expectativa antiga de 404. A revisão funcional posterior confirmou que a expectativa geral estava errada para menor autorizado; nesta correção o teste foi alinhado à decisão e o bloqueio etário retirado. Log `maven-completo.log` preservado como diagnóstico histórico, sem alegar que essa expectativa antiga era requisito válido.

## 27. Totais

| Execução | Total | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Baseline RF06 anterior, documental | 867 | 850 | 0 | 0 | 17 |
| Focados RF45 anteriores à correção | 270 | 270 | 0 | 0 | 0 |
| Regressão adicional anterior de privacidade/portfólio | 71 | 71 | 0 | 0 | 0 |
| Maven completo anterior à correção | 907 | 890 | 0 | 0 | 17 |
| Focados da correção de publicidade RF16 | 163 | 163 | 0 | 0 | 0 |
| Maven completo após correção de publicidade RF16 | 914 | 897 | 0 | 0 | 17 |

Os 17 skips condicionais anteriores permanecem com as mesmas condições: 4 `CurrentLocalSchemaIntegrationTest` e 13 `OfficialLocalApiIntegrationTest`. Não foram usados para mascarar falhas.

## 28. Semgrep e Graphify

**Semgrep Java da correção final concluído via Docker após o Maven, exit 0**: versão **1.178.0**, **258 arquivos**, **60 regras**, **0 findings / 0 erros**, **233,18 s**. Confirmada inclusão de `PortfolioAccessService.java` com a restrição geral removida e de `MenorAutorizadoPolicy.java`. Percentual de linhas parseadas não foi publicado por esta execução. O scan anterior à correção, também sem findings/erros, de 273,28 s foi preservado como histórico.

Imagem oficial `semgrep/semgrep:latest`, digest `sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b`. Ruleset `p/java` baixado por `curl.exe --fail --location https://semgrep.dev/c/p/java` no Windows e montado localmente. SHA-256 do ruleset: `5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1`. Instalação nativa não utilizada; TLS não desabilitado. O repositório foi montado somente para leitura. Não foi repetido o scan secrets; os resultados históricos da recuperação RF06 não são apresentados como novo scan RF45.

Comando: `docker run --rm --mount type=bind,source=<checkout>,target=/src,readonly semgrep/semgrep:latest semgrep scan --config /src/evidencias/rf45-2026-10-03/semgrep-java.yml --metrics=off --disable-version-check --json /src/backend/src/main/java`. Evidências finais da correção em `correcao-portfolio-menor/semgrep-java.json`, `semgrep-java-saida.log`, `semgrep-java.exit` e `semgrep-resumo.json`; ruleset local `semgrep-java.yml` reaproveitado com SHA-256 conferido. Os scans anteriores em `semgrep-final.json/.exit`, `semgrep-final-saida.log`, `semgrep-final-resumo.json` e `semgrep-java.json/.log/.exit` permanecem preservados, separados da correção atual.

“0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.”

**Graphify não executado/atualizado por bloqueio do Windows App Control** ao carregar `_bz2.pyd`. A consulta inicial desta correção também não chegou a executar a análise. Após a orientação expressa do usuário, nenhuma nova tentativa foi feita, inclusive de atualização. **Nenhum contorno de segurança foi aplicado**; Microsoft Defender/App Control, Python e DLLs não foram alterados. As tentativas da etapa anterior estão em `graphify-update.log/.exit` e `graphify-update-final.log/.exit`; o grafo permanece sem atualização confirmada. A auditoria usa fontes, testes e código atual. Isso não impede a conclusão se código, testes, Maven e demais validações estiverem verdes.

## 29. Riscos

Volume elevado de histórico pode exigir avaliação de plano/índice pelo grupo em tarefa própria; OFFSET não garante snapshot entre páginas sob mudanças concorrentes; salas existentes seguem as políticas RF35 de participação e histórico. O histórico antigo de inconsistências é lido conservadoramente, sem reparo. Dados textuais profissionais/legados podem conter conteúdo fornecido pelo próprio usuário; esta tarefa não cria moderação textual nova.

No portfólio, não confundir a mídia disponibilizada diretamente pelo MVP com projeto PUBLICADO: o modelo atual não permite salvar um RASCUNHO separado. A futura implementação RF16 deve impedir tanto listagem quanto acesso direto às mídias de rascunhos por terceiros, inclusive para adultos, e manter ownership/moderação. Esta correção não modifica a moderação/validação vigente e não homologa o fluxo ainda ausente.

## 30. Limitações

RF16 por projetos permanece preexistente e incompleto: ausência de tabela/estado operacional e vínculo projeto→mídia impede validar “projeto PUBLICADO visível”, “RASCUNHO privado para terceiros” e “proprietário acessa seu RASCUNHO”. Não foi inventado estado em memória, reaproveitado campo/galeria nem criada migration para simular cobertura. A correção retira a interpretação incorreta e valida as mídias disponíveis no MVP; não declara RF16 revisado concluído.

Backend RF45 não equivale à entrega visual integrada. RF10 e RF35 completos incluem outras frentes fora do escopo; só as dependências necessárias foram corrigidas/reutilizadas. SMTP real/RF44 integral, anexos/anonimização/retenção RF35, RF17/RF37/RF42, metas percentuais de cobertura e desempenho sob carga não foram homologados. Graphify permanece indisponível por política do ambiente. Fallback de taxonomia legado não foi executado contra snapshot histórico; a validação funcional usa exclusivamente database04.

## 31. Pendências

Cliente `palco-comunidades-agenda/src/pages/vacancies/OfficialApplications.jsx`: remover seletor/requisições de análise formal, adicionar “Conversar” usando `artistaId` e RF35, apresentar username e consumir lista atual. `src/pages/Inbox.jsx` já usa `usuarioDestinoId`; reutilizar esse fluxo. O backend não remove controles renderizados no frontend, por isso essa integração visual exige tarefa autorizada futura. Verificar navegação real de perfil/menor e mensagens após integração.

Atualizar o derivado Graphify somente em outra tarefa/ambiente autorizado; nenhuma nova tentativa nesta correção. Nenhuma decisão de mudança estrutural de database04 é necessária para o RF45 implementado.

Para o RF16 por projetos, avaliar em tarefa própria a estrutura mínima de projeto (artista, título, capa e status) e vínculo de suas mídias, com decisão explícita do grupo antes de qualquer mudança de banco. A definição funcional continua PUBLICADO público/RASCUNHO privado. **“publicação de conteúdo por menor autorizado deve gerar aviso informativo RF44 quando esse evento for implementado.”** Não implementar RF40 nem aprovação manual por publicação como parte desta correção.

Achado preexistente fora do fluxo RF45: `ChatService.validarDupla` rejeita papéis iguais, mas não exige explicitamente o par ARTISTA/CONTRATANTE para contas ADMIN/MODERADOR adultas. O caminho RF45 valida CONTRATANTE e fornece ARTISTA; não depende dessa lacuna. Registrar para correção específica RF35, sem ampliar esta tarefa. Não foi declarada segurança integral de todas as origens genéricas do chat.

## 32. Conclusão

**RF45 backend continua concluído após a correção no escopo autorizado**, com ownership no service, lista atual paginada, histórico RF06 preservado, projeção profissional restrita e reuso do perfil/conversa existentes. Focados **163/163** e Maven completo **914 total/897 passed/0 failures/0 errors/17 skipped**, ambos BUILD SUCCESS; Semgrep Java final **0 findings/0 erros, exit 0**. A decisão correta de publicidade RF16 foi registrada e a restrição etária geral retirada. O baseline funcional RF06 continua concluído e suas regras de criação/retirada permanecem intactas. RF16 por projetos/RASCUNHO não é declarado concluído; seus critérios ainda não são executáveis neste MVP.

Banco/SQL/frontend não alterados. A entrega integral da interface RF45 permanece pendente de integração futura; não se declara RF10/RF35 completos nem homologação visual. A indisponibilidade do Graphify foi registrada sem contornar a política do Windows e sem impedir a verificação direta das fontes/testes.

## 33. Próximo passo recomendado

Revisar o delta específico validado e o contrato deste relatório. Integrar a tela de candidatos em tarefa própria, preservando desenho e removendo ações formais; homologar candidato adulto/menor, perfil/chat e erros de permissão ponta a ponta. Não realizar staging global sobre o working tree existente.

## Registro pronto para consolidação semanal

**Data:** 03/10/2026.
**Objetivo:** concluir RF45 backend, com candidatos atuais, perfil público e conversa RF35.
**RF/RNF:** RF45; dependências RF06/RF05/RF10/RF16/RF35/RF36/RF27 e fronteira RF44; RNF02/RNF07/RNF08/RNF09.
**Backend alterado:** consulta/count de candidatos, username público, proteção compartilhada do menor, acesso público autorizado e abertura/reuso RF35. Correção final remove o bloqueio geral do portfólio de menor autorizado, reaproveitando RF10/RF27 e conta apta; PUBLICADO público/RASCUNHO privado é o contrato consolidado, com fluxo por projetos ainda pendente no MVP.
**Frontend alterado:** NÃO. **Banco alterado:** NÃO.
**Funcionalidades concluídas/avançadas:** RF45 backend concluído, com filtro antes de paginação, ownership, preservação de duas tentativas e perfil/chat reaproveitados; integração visual pendente.
**Bugs corrigidos:** histórico misturado com candidatos atuais; perfil de menor autorizado indisponível; abertura de chat sem revalidar consentimento do menor alvo; interpretação incorreta que tornava todo portfólio público de menor autorizado indisponível.
**Decisões técnicas:** última linha por ID somente se ativa; terminais históricos; matching/ordenação/API paginada preservados; mesma sala/endpoint RF35; menor 14–17 somente autorizado.
**Segurança/autorização:** identidade JWT, dono no service, binding SQL, DTO sem dados privados/experiência, menor com consentimento vigente e vínculo profissional, participação nas salas.
**Testes/resultados:** focados da correção 163/163, 0 failures/errors/skipped, BUILD SUCCESS (02:12 min); Maven completo 914 total/897 passed/0 failures/0 errors/17 skipped, BUILD SUCCESS (07:09 min). 40 casos RF45 e 26 RF16 executados; nenhum novo skip. Semgrep Java final via Docker: 258 arquivos/60 regras/0 findings/0 erros, exit 0 (233,18 s); resultados anteriores preservados nas seções 25–28. Graphify indisponível pelo App Control, sem nova tentativa ou contorno.
**Pendências:** integração visual, RF16 por projetos/RASCUNHO e evento informativo RF44 de publicação; Graphify indisponível; demais RFs completos fora deste escopo.
**Próximos passos:** revisar delta específico validado e integrar frontend em tarefa própria.

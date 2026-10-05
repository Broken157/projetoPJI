# RF44 — Avisos ao Responsável Legal

Data: 05/10/2026. **RF44 global PARCIAL; candidatura CONCLUÍDA; publicação CONDICIONADA/BLOQUEADA por ausência de produtor real.** Banco e frontend não alterados. As validações verdes não removem a dependência funcional de publicação.

## 1. Objetivo

Auditar e implementar o aviso informativo por e-mail para ARTISTA autorizado de 14–17 anos, depois de ação relevante concluída. Ação e consentimento não dependem de nova aprovação do responsável. Escopo operacional atual: candidatura RF06.

## 2. Requisitos

Lidos integralmente no arquivo oficial atual: RF01/02/06/10/16/18/27/35/36/40/44 e RNF06/07/08/09/10; cópia e origem/hash em requisitos-consultados.txt/requisitos-sha256.json. Consultados relatórios RF06, RF27, RF16, RF18, RF35, RF36, RF11 e sincronização database05. Histórico foi confrontado com Java/schema atuais. Matriz de critérios/testes e auditoria A–W precedem edição de produção.

## 3. HEAD e checkpoint

HEAD inicial/final **e5c3e03b5c41317fb5fe1103cb5bd84f0c00d55a**, branch integracao-recuperada-2026-09-15. Commit RF11 presente, upstream/fork iguais, fetch autorizado confirmado, divergência 0/0. A primeira tentativa do fetch falhou por schannel no sandbox; repetição autorizada passou, sem desabilitar TLS. Índice vazio; nenhum delta RF11 solto no backend. Working tree histórico sujo preservado. Nenhum add, commit, push, reset, clean, stash ou restore destrutivo.

## 4. Graphify inicial

MCP HTTP: **6365 nós / 21886 arestas / 343 comunidades**, confiança EXTRACTED 91%, INFERRED 9%, AMBIGUOUS 0%. Encontrados CandidaturaService, evento/listener, sender/interface, responsáveis/RF27/policy, identidade e fronteiras de Portfólio. Grafo orientou a auditoria; fontes oficiais/Java/testes decidiram. Resultados iniciais preservados nesta pasta de evidências.

## 5. Database05

Fonte ativa exclusiva database05/palco-database; database04 histórico. Responsáveis normalizados com usuario_id único, contato e consentimento/data/flag existentes. Não existe tabela oficial de tentativas RF44/outbox/eventId. Logs de exclusão LGPD/cancelamento de vaga têm finalidade própria e não foram reaproveitados. Não adicionar tabela, índice, migration ou campos de consentimento para o aviso.

## 6. Estado inicial RF44

Já havia evento de candidatura, AFTER_COMMIT, interface e sender SMTP real condicional, corpo mínimo e isolamento de RuntimeException. Gaps: email no evento antes de commit; ausência de revalidação persistida; definição duplicada de menor autorizado; conta ATIVA inconsistente podia candidatar sem autorização; correlação só pela vaga; envio síncrono e link sem validação da base. Auditoria inicial A–W documenta estado anterior e decisão de correção.

## 7. Infraestrutura de e-mail

Reutilizados GuardianApplicationNoticeSender/JavaMailSender e condição SMTP existente, sem trocar provedor ou modificar RF09/RF26/RF27. Remetente/host/credenciais continuam nas properties existentes com variáveis de ambiente; nenhuma senha/token adicionada. Sender real só existe quando host/remetente estão configurados. RF44 permanece ativo: ausência de sender registra SMTP_INDISPONIVEL e preserva a ação. Testes capturam a abstração, sem SMTP externo.

## 8. Evento de candidatura

AvisoResponsavelCandidaturaEvento agora transporta somente candidaturaId/artistaId/vagaId. Emissão única por candidatura realmente persistida e autorizada; nenhuma entidade/contacto/consentimento/payload no evento. AtomicBoolean pertence à instância e protege somente sua tentativa local; não cria identidade persistente ou cache global.

## 9. Listener

AvisoResponsavelCandidaturaListener continua TransactionalEventListener AFTER_COMMIT, sem fallback fora de transação. Agenda tarefa no executor específico RF44. Rejeição/indisponibilidade do executor gera FALHA_AGENDAMENTO sanitizada e não vaza para response. Não cria notificação interna para responsável ou chama CandidaturaService para enviar.

## 10. Sender e executor

GuardianApplicationNoticeService revalida contexto persistido e chama a interface existente. GuardianNoticeConfig cria dois workers e capacidade de 100 tarefas em memória, com rejeição controlada e encerramento aguardado por até cinco segundos. Bean defaultCandidate=false e Qualifier mantêm o executor restrito ao RF44; teste confirma que não assume o executor do Spring MVC. Não foi habilitado Async global nem criado outbox/dispatcher durável.

## 11. AFTER_COMMIT e recursos

Ordem validada: policy/validações RF06 → saveAndFlush → evento → commit → executor → leitura readOnly REQUIRES_NEW → encerramento da leitura → sender. Teste comprova zero envio antes do commit e zero envio no rollback; durante sender não há transação nem recurso JDBC/JPA vinculado à thread. SMTP lento não retém response. Context7: [Spring TransactionalEventListener](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/event/TransactionalEventListener.html) e [Spring Boot 4.0.3](https://github.com/spring-projects/spring-boot/blob/v4.0.3/documentation/spring-boot-docs/src/docs/antora/modules/reference/pages/features/task-execution-and-scheduling.adoc). Projeto Boot 4.0.6; comportamento confirmado nos testes locais.

## 12. Responsável correto

Destinatário deriva do vínculo persistido do artista da candidatura. Revalida os três IDs, existência da ação, propriedade, policy, vínculo reverso, contato não vazio/formato/limite via validação RF01 existente e diferença para email do menor. Email/body/query/responsavelId do cliente não controlam destinatário. Contato inválido/inconsistente resulta em falha segura, sem endereço alternativo improvisado ou correção do cadastro.

## 13. Idade

Reutilizada MenorAutorizadoPolicy com Clock existente: somente ARTISTA 14–17, ATIVA, consentimento vigente. Abaixo de 14 é bloqueado antes da persistência mesmo com fixture de estado inconsistente; data ausente é recusada defensivamente pela policy. Sem segunda definição etária no listener/sender.

## 14. Consentimento RF27

RF27 permanece autorização inicial; RF44 informa ação posterior. Sem token/link de autorização, novo aceite/recusa, mudança de conta ou consentimento por envio. Flag persistida de consentimento continua com semântica existente. Não foi criado fluxo de revogação posterior. Registro inválido/inconsistente no processamento não recebe aviso cegamente.

## 15. Candidatura RF06

Mantidos contrato POST /api/candidaturas, perfil completo, ABERTA/prazo, locks, uma ativa, duas tentativas históricas e retirada lógica. Único gate funcional acrescentado é a reutilização da policy para menores inconsistentes antes de criar. Nova candidatura real publica um evento RF44 com seu ID próprio; bloqueios não geram aviso. Resposta não ganha dados do responsável.

## 16. Adulto

18/40 anos candidatam normalmente e não produzem evento, tentativa RF44 ou envio, mesmo com vínculo histórico de responsável. A emissão não consulta contato do responsável para montar aviso de adulto. Contexto adulto injetado artificialmente também é recusado pelo processamento.

## 17. Menor não autorizado

Sem vínculo/sem consentimento/recusa, mesmo com ATIVA inconsistente, candidatura retorna 403 e produz zero efeitos. Estados inaptos com JWT antigo retornam 401 conforme barreira existente. Abaixo de 14 retorna 403. Listener/handler revalidam estado persistido de eventos inconsistentes; nenhuma exceção cria fallback.

## 18. Recandidatura

Primeira retirada permite segunda candidatura com novo ID e novo aviso legítimo. Mesmo artista/vaga/endereço não é chave de deduplicação. Duas retiradas preservam histórico; terceira tentativa e duplicidade ativa são 409, sem aviso adicional. Retirada não gera RF44.

## 19. Concorrência

Duas chamadas com largada coordenada produzem 201/409, uma candidatura real, um envio lógico e uma RF36 de criação. Reutilizado controle transacional RF06; nenhum lock de candidatura paralelo RF44/UNIQUE novo. AtomicBoolean cobre callbacks concorrentes da mesma instância de evento no processo.

## 20. Rollback

Teste envolve POST válido em transação externa e força rollback: candidatura/notificação não persistem, executor não agenda envio e sender/log de tentativa RF44 permanecem vazios. Não é teste de falha após commit confundida com rollback anterior.

## 21. Falha SMTP

Exceção no sender é registrada somente por categoria técnica. HTTP continua 201; candidatura persiste PENDENTE, uma RF36 permanece e não há outra candidatura. Testes também cobrem SMTP ausente e rejeição de agendamento. SUCESSO significa retorno da abstração/SMTP, não prova de chegada à caixa postal.

## 22. Retry

Não há retry automático/durável ou reenvio HTTP RF44 implementado. Uma tentativa por evento no fluxo normal. Teste de repetição simulada somente do handler de envio mantém mesmo ID e uma RF36; não constitui política operacional de retry. Não existe chamada de reenviar email para recriar candidatura. Entrega não garantida.

## 23. Idempotência

Garantia RF06 de ação única sob concorrência + único produtor/listener + proteção da MESMA instância do evento no fluxo local. Nova instância com os mesmos IDs, replay, múltiplos processos ou reinício não têm deduplicação durável de email; repetição pode reenviar, mas não recria ação. Não deduplicar por assunto/email nem ocultar nova recandidatura.

## 24. Registro técnico

Log observacional com evento=CANDIDATURA, candidaturaId, resultado INICIO/SUCESSO/FALHA/FALHA_AGENDAMENTO e categoria técnica. Timestamp/thread/logger vêm da configuração de logging atual. Não é registro funcional em banco, recibo de entrega nem auditoria durável garantida. Ausência estrutural documentada; eventual exigência de persistência requer decisão/pacote oficial próprios.

## 25. Logs e RNF09

Sem email completo, telefone, nome, CPF, token, conteúdo de candidatura, link secreto ou mensagem/stacktrace da exceção nos logs RF44. Testes injetam erro com sentinelas pessoais e confirmam ausência/throwable nulo. Nenhuma configuração global de logging/biblioteca SMTP foi relaxada; não se afirma que todo log de toda biblioteca seja sanitizado por esta mudança.

## 26. Privacidade

Evento somente técnico; dados do responsável confinados à leitura interna e destinatário do sender. DTO HTTP de candidatura e RF36 permanecem sem contato/consentimento do responsável. Testes verificam serialização, destinatário arbitrário ignorado, payload e logs. Sem dados privados do menor, experiência ou consentimento acrescentados a canais públicos.

## 27. Conteúdo do e-mail

Assunto neutro Aviso de candidatura — Palco. Corpo informa candidatura do adolescente e que nenhuma nova aprovação é necessária. Somente contexto público da vaga quando seguro; sem nome completo, apresentação, portfólio, experiência, remuneração, CPF, telefone, nascimento, email do menor ou token. CC/BCC ausentes; destinatário é somente responsável válido.

## 28. Link

Rota real pública /vagas/{id}, comprovada por frontend vigente e SecurityConfig/GET público RF05. Sem acesso à conta do menor/JWT/consentimento/IDOR. Base vem exclusivamente da configuração do servidor: somente HTTPS com host, sem userinfo/query/fragmento ou subrota. Base insegura/inespecífica é omitida e aviso continua. Nove configurações negativas testadas; frontend não foi alterado. Homologação do host público real permanece operacional.

## 29. RF36 separado

RF36 de candidatura continua para contratante proprietário; RF44 é email externo ao responsável. Responsável não ganha usuário/notificação interna/tipo novo. Falha SMTP e repetição simulada do envio não duplicam RF36. Dados do responsável não entram em notificacoes.

## 30. Publicação de conteúdo

**Nenhum produtor ARTISTA operacional de publicação revisada encontrado.** Upload/embeds individuais podem ficar publicamente acessíveis conforme policy/moderação, mas não equivalem à ação PUBLICAR. CommunityController apenas consulta comunidades; publicação de vaga é de CONTRATANTE adulto. Não há evento pós-publicação RF16/RF40 que possa ser conectado legitimamente.

## 31. RF16

Continua **BLOQUEADO POR ESTRUTURA**: sem projeto/capa/status/vínculo de múltiplas mídias no pacote oficial. Controller oferece upload/lista/leitura/exclusão de arquivo e cadastro/edição de embed. Inspeção/teste estrutural confirma ausência de PUBLICAR e colunas fundamentais. Nenhum projeto/endpoint/DDL criado para simular publicação.

## 32. RF40

Continua futuro/sem produtor social operacional no backend atual. Não foi implementado antecipadamente. Quando existir ação validada de publicação do ARTISTA, deverá integrar aviso pós-commit com o mesmo princípio de privacidade e isolamento, adaptado ao contrato real.

## 33. Ações triviais

GET de candidatura/vaga/listas e retirada não produzem novos avisos. Edição simples, upload individual, embed, login, navegação, chat, Banco, salvos e perfil público não foram conectados ao RF44. Nenhuma interpretação de upload como publicação para obter conclusão artificial.

## 34. Ações futuras

Somente candidatura e publicação estão no mínimo oficial. Convite, Banco, seguir, chat, retirada ou outros avisos dependerão de decisão funcional expressa. Nenhuma autorização nova por ação e nenhum monitoramento genérico do menor.

## 35. Arquivos de produção

Seis Java: quatro modificados (CandidaturaService, AvisoResponsavelCandidaturaEvento, AvisoResponsavelCandidaturaListener, SmtpGuardianApplicationNoticeSender) e dois novos (GuardianApplicationNoticeService, config/GuardianNoticeConfig). Interface, RF27, propriedades, SecurityConfig, entidades e infraestrutura compartilhada SMTP permanecem intactos. Escopo exato em escopo-autorizado.json.

## 36. Arquivos de teste

Cinco Java: RF06 existente adaptado para aguardar aviso assíncrono e drenar antes da limpeza; novos AvisoResponsavelRf44IntegrationTest, AvisoResponsavelCandidaturaListenerTest, SmtpGuardianApplicationNoticeSenderTest e SmtpGuardianApplicationNoticeSenderConfigurationTest. RF44 específico: **67 casos executados**. Nenhum teste desabilitado/novo skip. Primeira rodada: 118/117 passed, 1 failure/0 errors por candidatura do seed na fixture inicial; corrigida isolação. Rodada ampliada anterior: 578/577 passed, 0 failures/1 error por pressupor applicationTaskExecutor; teste corrigido para verificar executor real do Spring MVC. Artefatos de ambas preservados; não somados aos resultados finais.

## 37. Banco alterado

**NÃO.** Nenhum arquivo SQL/schema/enum/índice/FK/migration/procedure/trigger/entity/bootstrap/script operacional alterado. Fixtures/limpeza somente nos PostgreSQL descartáveis dos testes, com snapshot oficial e ddl-auto=validate. Banco de desenvolvimento não operado nesta tarefa.

## 38. Frontend alterado

**NÃO.** 770 arquivos protegidos iguais ao baseline, incluindo deltas históricos. Nenhum build/install/teste de frontend, tela/botão/modal/banner/aprovação/painel do responsável criado. RF44 é backend/email.

## 39. Database05 46/46

ZIP oficial C:/Users/masca/Downloads/palco-database05.zip, SHA-256 **6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf**. Inicial/final: **46/46 idênticos byte a byte; zero divergentes/ausentes/adicionais**. Database04 (47 arquivos) e cinco scripts históricos preservados. 1436 arquivos protegidos sem delta; 370 fontes/recursos testados sem mudança posterior. Históricos documentais e trecho RF11 semanal preservados.

## 40. Focados

**578 total / 578 passed / 0 failures / 0 errors / 0 skipped; BUILD SUCCESS**. Classes: 24. [INFO] Total time:  04:31 min.

Uma bateria final única de 24 classes; lista real em classes-focadas.txt. RF44/RF06/RF27/RF01/RF02/RF36/RF22/RF09/RF26/JWT/security/bootstrap/mapping/RF11/RF16/RF18/RF45/RF42 e sender compartilhado RF09. Evidências maven-focado-validado.log/.exit/-totais.json/-reports; rodadas anteriores permanecem diagnósticas.

## 41. Maven completo

**1560 total / 1543 passed / 0 failures / 0 errors / 17 skipped; BUILD SUCCESS**. Classes: 73. [INFO] Total time:  09:46 min.

Executado depois do foco: backend/./mvnw.cmd test -Dspring.test.mockmvc.print=NONE, via runner de evidências com ambiente database05. Baseline RF11 1493/1476/17 é somente comparação histórica; resultado acima medido nesta tarefa. Mesmos 17 skips nominalmente comparados: CurrentLocalSchemaIntegrationTest (4) e OfficialLocalApiIntegrationTest (13); zero novos, nenhuma classe antiga ausente ou quantidade antiga reduzida. Delta 67 casos, exclusivamente novas classes RF44. Proveniência database05 e relatórios sanitizados registrados.

## 42. Semgrep

Versão 1.178.0; 366 arquivos; 60 regras; parse ~100.0%; findings 0; blocking 0; errors 0; exit 0; 50.4 s.

Docker oficial fixado por digest, p/java oficial em cache montado readOnly, network none; inclui todos os Java de produção/teste e os novos untracked. Native Windows não utilizado, TLS não desabilitado. Nenhum secrets scan amplo nesta tarefa. **0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.**

## 43. Graphify final

Exit 0; 27.6 s; **6453 nós / 22287 arestas / 359 comunidades**; EXTRACTED 91%, INFERRED 9%, AMBIGUOUS 0%.

graphify update . AST-only seguido de confirmação MCP HTTP e reconhecimento do serviço/listener RF44. Sem label, graphify-mcp.exe, parser novo, reinstalação Python ou contorno Windows App Control/Defender. Relações inferidas não são prova exclusiva do comportamento; Java/testes confirmam. Logs/exit/stat/get_node/query finais preservados.

## 44. Riscos

Executor volátil pode perder tarefas em interrupção/falha do processo ou rejeitá-las quando saturado; registro de falha não oferece recuperação durável. SMTP pode aceitar e falhar posteriormente sem recibo de entrega inequívoco. Nova instância/replay pode reenviar email; ação permanece única. Contato/estado são revalidados na leitura pós-commit, sem lock mantido durante SMTP; mudanças posteriores à leitura não têm atomicidade com canal externo. Não prometer exactly-once/entrega garantida.

## 45. Limitações

Sem SMTP real/inbox externo, carga/p95/p99, medição percentual RNF10 ou persistência durável de tentativa. Logs não substituem trilha funcional em banco. Publicação RF16/RF40 indisponível; requisitos globais de RNF09/LGPD/retenção não declarados concluídos. Mantidos 17 testes locais condicionais anteriores. RF18 continua PARCIAL; suas fronteiras estruturais não foram ampliadas.

## 46. RF44 final

**PARCIAL globalmente.** Critérios A–I/K–M do prompt sustentados para candidatura por código, testes e inspeções; J depende de publicação real. Foco/Maven/Semgrep/Graphify verdes não autorizam declarar RF44 integral CONCLUÍDO sem esse produtor.

## 47. Parte candidatura

**CONCLUÍDA no backend**, com autorização existente, ação realmente persistida, pós-commit, responsável correto, mínimos dados, isolamento de falhas, registro técnico sanitizado, concorrência/recandidatura e regressões. A entrega real do provedor permanece homologação operacional; não exigir nova aprovação.

## 48. Parte publicação

**CONDICIONADA/BLOQUEADA pelo produtor futuro.** Não há produtor RF16/RF40 de ARTISTA nem teste de publicação fictícia. A inspeção estrutural foi executada; rascunho, republicação e falha SMTP de publicação não foram alegados como fluxo implementado.

## 49. Pendências

Pacote oficial RF16 e produtor real RF16/RF40; futura integração pós-publicação. Homologar SMTP/remetente/host HTTPS público sem credenciais no repositório. Se exigida recuperação/auditoria durável, decidir estrutura oficial e política de retry sem repetir ação. RF16 BLOQUEADO/RF18 PARCIAL/RF17 pendências anteriores permanecem. **git diff --check exit 0, stdout vazio; índice vazio; HEAD preservado.**

## 50. Próximo passo

Tratar produtor de publicação em tarefa própria após estrutura/contrato oficial; então conectar RF44 e validar publicação real. Esta tarefa não altera banco/frontend nem prepara commit. Relatório, registro semanal e evidências estão prontos para revisão; nenhuma autorização operacional externa é presumida.

# RF06 — Candidatura, retirada e recandidatura — 02/10/2026

**Estado final:** RF06 backend concluído dentro do escopo solicitado. Testes focados: 159/159 verdes. Maven completo: 867 total, 850 passed, 0 failures, 0 errors, 17 skips condicionais, BUILD SUCCESS. Database04/frontend preservados. RF44 parcial; RF45 e integração frontend pendentes. Sem staging, commit ou push.

Checkout: `C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline`. Branch: `integracao-recuperada-2026-09-15`. HEAD: `a6164e38b177c765638d61eb0d68c4b46f79f052`. Database04 é a única fonte ativa. Esta sessão não realizou staging, commit, push, merge, reset, clean, restore ou stash.

## 1. Resumo

O backend mantém uma candidatura ativa por vaga/artista e no máximo duas tentativas históricas. A primeira retirada permite nova linha PENDENTE; a segunda retirada torna a terceira tentativa um conflito 409 com “Limite de recandidatura atingido”. As linhas, IDs e datas permanecem preservados. Autorização deriva do JWT, e nova candidatura exige perfil completo e vaga ABERTA/no prazo. Notificações do contratante e aviso informativo ao responsável foram verificados sem SMTP real. RF44 permanece parcial e RF45/integração frontend permanecem pendentes.

## 2. Estado inicial encontrado

A auditoria precedeu qualquer edição: `git status`, `git branch --show-current`, `git diff --stat` e `git diff --check`. A branch estava correta e o working tree continha 294 entradas curtas; o diff dos arquivos rastreados apresentava 129 arquivos, 4.119 inserções e 1.945 exclusões. `diff --check` inicial: exit 0. Existiam alterações de backend, frontend, infraestrutura, snapshots históricos, relatórios e arquivos novos; elas não foram atribuídas a esta sessão.

Foram registrados hashes de 1.079 arquivos antes da edição. Evidências: `evidencias/rf06-2026-10-02/git-status-inicial.txt`, `git-diff-stat-inicial.txt` e `arquivos-iniciais.json`. A comparação com esse manifesto distingue a continuação do trabalho herdado.

## 3. Implementação herdada da sessão interrompida

Já estavam implementados: opcionalidade real de mensagem/link em DTO/entity; consulta do histórico ordenada por ID; lock advisory PostgreSQL por par; lock pessimista da vaga; duplicidade ativa/limite de duas tentativas; retirada apenas ABERTA/PAUSADA; refresh das entidades após obter o lock na retirada; preservação de histórico; escolha da última candidatura no detalhe da vaga; cancelamento apenas das candidaturas ativas no RF28; criação do evento/listener/sender do aviso RF44; adaptação dos testes antigos, inclusive substituição da premissa de UNIQUE global.

Também já havia bloqueio das transições formais do contratante no PUT legado, sem exclusão da rota nem dos enums. A auditoria reconheceu esse comportamento anterior, compatível com a decisão revisada de retirar estados formais da jornada, e o preservou. A primeira execução nesta sessão validou o código herdado: 137 testes focados, 137 passed, 0 failures/errors/skipped; RF06 tinha 40 casos passando.

## 4. O que esta sessão completou

A alteração de produção ficou em dois arquivos: `CandidaturaService.java`, restringindo o aviso ao responsável a 14–17 anos/conta ATIVA/consentimento válido/e-mail não vazio; e `AvisoResponsavelCandidaturaListener.java`, isolando também falhas ao obter o sender e registrando envio bem-sucedido sem e-mail ou dados pessoais. Falta de SMTP continua apenas gerando registro técnico após commit.

Em `CandidaturaControllerRf06IntegrationTest.java`, foram acrescentados/reforçados 17 casos executados: falha SMTP, falha realtime/recuperação da notificação, validações bloqueadas sem avisos, perfil completo sem nova exigência de foto/portfólio/raio, limites etários, estados terminais, duplicidade EM_ANALISE, corrida de recandidatura, catálogo sem índice único global, identidade da segunda tentativa no detalhe e novo timestamp. A classe passou de 40 para 57 casos. Nenhum outro arquivo de produção herdado foi reescrito nesta sessão.

## 5. RF06 consultado

Fonte funcional: `C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt`, RF06 nas linhas 593–685; RF44 nas linhas 3729–3797. SHA-256: `3e7ca8fa7e78a1967cf5f86c86f6a60f736879b02cfbd8f9d195040fea873183`.

Foram lidos os relatórios RF01/RF24 e sincronização database04 de 02/10, o registro semanal e a auditoria RF06/RF45 de 28/09. A auditoria antiga aponta impedimentos de snapshots históricos; eles não foram reaplicados ao database04 atual. O grafo Graphify orientou a localização, e as conclusões foram conferidas no código atual.

## 6. Estrutura database04

`database04/palco-database/02_tables/04_vagas.sql`: `candidaturas.id` é PK; `vaga_id`/`artista_id` são FKs; mensagem e link aceitam NULL; cada linha tem status e data próprios; não existe UNIQUE global vaga/artista. O catálogo real do teste de concorrência confirmou ausência de índice único secundário nessa tabela.

Enum oficial: PENDENTE, EM_ANALISE, ACEITA, REJEITADA, BLOQUEADA, RETIRADA e CANCELADA_POR_VAGA. O tipo de notificação CANDIDATURA já existe. O responsável usa tabela normalizada com `usuario_id` único, consentimento/data/revogação. Não foram criados objetos SQL.

## 7. Banco alterado: NÃO

Nenhum SQL, migration, tabela, coluna, enum, constraint, índice, trigger, procedure ou função foi alterado/criado. `spring.jpa.hibernate.ddl-auto=validate` permanece tanto no padrão quanto no profile local. Os testes inicializam bancos PostgreSQL descartáveis com o init/seed oficial existente e usam DML de fixtures; o banco de desenvolvimento não recebeu escrita desta tarefa.

## 8. Frontend alterado: NÃO

521 arquivos de frontend existentes no manifesto inicial permaneceram iguais. Não houve build, instalação de dependências, teste de frontend ou redesign nesta sessão. As alterações anteriores do working tree foram preservadas. O contrato foi somente auditado.

## 9. Autenticação/autorização

O filtro de segurança exige autenticação. `AuthenticatedUserResolver` resolve o usuário pelo SecurityContext/JWT e reconsulta a identidade persistida. O service exige ARTISTA; IDs extras enviados no POST não definem ator nem status. Anônimo recebe 401, contratante 403 e vaga inexistente 404. Outro artista recebe 404 ao acessar/retirar candidatura alheia. Leitura/listagem continuam limitadas ao artista ou proprietário da vaga.

## 10. Perfil completo

RF06 verifica somente `Boolean.TRUE.equals(usuario.getPerfilCompleto())`; false retorna 422. Não exige novamente foto, URL do portfólio nem raio. O teste dedicado remove esses campos na fixture e mantém explicitamente a flag true, demonstrando que RF06 consome a flag; isso não altera nem homologa o cálculo de completude do RF08/trigger oficial. RF08 não foi modificado.

## 11. Estado da vaga/prazo

O service carrega a vaga sob lock pessimista e exige ABERTA. RASCUNHO, PAUSADA, ENCERRADA e CANCELADA retornam 422 para criação. `VagaPrazoPolicy`, já compartilhada com RF23, bloqueia a data vencida inclusive no próprio dia, sem esperar o agendador encerrar a vaga. Prazo ausente/futuro permite. A regressão RF23 cobre corrida com encerramento e notificações sem duplicação.

## 12. Primeira candidatura

POST `/api/candidaturas` mínimo: `{"vagaId":123}`. Sucesso retorna 201, novo ID, timestamp e status PENDENTE. Mensagem/link ausentes permanecem NULL no banco e JSON; valores legados continuam aceitos, com limites de 2.000/255 caracteres quando informados. Nenhum valor fictício substitui ausência. Status ou artista extra do cliente não prevalece.

## 13. Definição de ativa

PENDENTE e EM_ANALISE são ativas. Havendo qualquer uma para o par, retorna 409: “Você já possui uma candidatura ativa para esta vaga.” O teste parametrizado confirma ambos os estados e uma única linha persistida. Novas linhas nascem somente PENDENTE.

## 14. Concorrência e lock

Dentro de `@Transactional`, o service obtém `pg_advisory_xact_lock(hashtextextended(cast(:chave as text), 0))`, chave `rf06:<vagaId>:<artistaId>`, seguindo o padrão existente em ChatService/GoogleLinkLock. Em seguida, bloqueia a vaga, valida status/prazo, verifica ativa, conta histórico e persiste. A retirada usa a mesma ordem e atualiza as entidades carregadas previamente com `refresh`, evitando decisão baseada em estado anterior à espera.

As duas threads dos testes ficam prontas antes de liberar o início. Tanto na primeira candidatura quanto na segunda tentativa, duas requisições produzem 201/409; há exatamente uma ativa, uma notificação de criação e, na recandidatura, duas linhas totais. A garantia é PostgreSQL/transacional, não `synchronized` da JVM nem UNIQUE global. O lock da vaga também mantém a coordenação com RF23/RF28, embora serialize outros artistas dessa vaga durante a transação.

## 15. Retirada

DELETE `/api/candidaturas/{id}` retorna 204 e realiza mudança lógica para RETIRADA. PUT legado do próprio artista também permite RETIRADA e retorna 200, conferindo IDs como vínculos imutáveis, sem usá-los como autoridade. Apenas PENDENTE/EM_ANALISE e vaga ABERTA/PAUSADA permitem retirada. Vaga ENCERRADA/CANCELADA ou candidatura terminal retorna 422. ID/data/mensagem/link e linha persistida são preservados.

## 16. Recandidatura

Com uma única linha RETIRADA, sem ativa, vaga ABERTA/no prazo e perfil completo, o POST cria segunda linha PENDENTE. ID e timestamp são novos. A primeira continua RETIRADA com a mesma data. O detalhe `/api/vagas/{id}` aponta para a tentativa mais recente, evitando erro de consulta de resultado único quando existem duas linhas.

## 17. Limite de duas tentativas

O histórico completo do par é contado sob lock. Após duas tentativas, nova criação retorna 409 com “Limite de recandidatura atingido para esta vaga.” O teste percorre 201 → retirada → 201 → retirada → 409 e confirma exatamente duas linhas RETIRADA; a tentativa bloqueada não produz notificação ou e-mail. Dados legados com duas ou mais linhas também não são apagados para liberar nova tentativa.

## 18. Preservação histórica

Retirada não usa DELETE físico, não reativa a linha antiga e não modifica data histórica. Falhas e bloqueios não criam linha extra. As duas tentativas permanecem disponíveis ao backend para limite e histórico do artista. A filtragem da visão operacional do contratante pertence ao RF45 e não foi implementada.

## 19. Estados legados

EM_ANALISE continua elegível como ativa/retirável; ACEITA, REJEITADA, BLOQUEADA, RETIRADA e CANCELADA_POR_VAGA não podem ser retiradas como ativas. Testes verificam 422 sem alteração de estado/data ou notificação. Leitura dos estados legados permanece; não houve alteração do enum SQL nem limpeza histórica. Não há nova criação formal ACEITA/REJEITADA.

## 20. RF28

Preservado o código já encontrado: cancelar a vaga converte apenas PENDENTE/EM_ANALISE em CANCELADA_POR_VAGA e mantém as linhas históricas/terminais. A classe histórica `VagaCancelamentoRf25IntegrationTest` cobre o RF28 revisado e passou com 30 casos. Esta sessão não reimplementou cancelamento nem modificou VagaService.

## 21. RF36

Criação bem-sucedida publica `NotificacaoEvento` para o usuário proprietário da vaga, tipo CANDIDATURA. O listener existente processa após commit, persiste em `REQUIRES_NEW` e tenta entrega pelo gateway SSE/WebSocket. Falha realtime é capturada e não reverte candidatura; a notificação continua consultável via GET `/api/notificacoes`. Bloqueios não publicam evento.

A retirada preserva a arquitetura anterior de persistência da notificação na transação atual e entrega após commit; falha de persistência nessa retirada reverte a mudança de status, conforme o teste existente. Não foi trocado o comportamento transacional entre esses dois caminhos.

## 22. RF44 PARCIAL — evento de candidatura

Artista autorizado de 14–17 anos, conta ATIVA e responsável com consentimento vigente/e-mail válido gera evento informativo após criar candidatura. `AFTER_COMMIT` entrega pelo sender SMTP condicional existente; não pede nova aprovação. Falha de envio/provisionamento do sender ou ausência de SMTP é isolada e registrada sem e-mail/dados pessoais, sem rollback. Adultos, inclusive com registro histórico de responsável, não geram aviso.

Testes usam sender mockado: 14/17 anos enviam; adultos de 18/40 não; falha de envio mantém candidatura/notificação; perfil incompleto, vaga pausada, prazo vencido, duplicidade ativa e limite não enviam. SMTP real, publicação de conteúdo e política durável de retry não foram homologados/implementados. RF44 integral permanece pendente.

## 23. Confirmação explícita

O request backend não tem boolean de confirmação. No cliente integrado, `OfficialVacancies.jsx` abre `ApplicationDialog`; `OfficialApplications.jsx` exibe o modal “Confirmar candidatura”, a pergunta sobre registrar interesse e o botão final “Confirmar candidatura”. O POST só ocorre no submit final. Essa sequência satisfaz a confirmação explícita no contrato atual; não foi acrescentado um segundo mecanismo backend. Evidência estática de código, sem nova execução de navegador nesta tarefa.

## 24. Privacidade

Resposta usa DTO de candidatura, sem responsável, consentimento, senha, token ou documento. O teste do menor confirma ausência de e-mail do responsável no JSON. A autoridade é do JWT, recursos alheios continuam protegidos e o aviso usa apenas informações mínimas e link de contexto. Os logs novos contêm ID da vaga e classe da falha, sem destinatário ou conteúdo SMTP. Valores sensíveis de erro JDBC não foram reativados.

## 25. Transações

Criação/retirada são transacionais; lock advisory é liberado no fim da transação. Criação persiste/flush antes de retornar DTO e agenda os efeitos após commit. Falha de validação/duplicidade/limite não produz efeitos externos. Não foi criado outbox nem serviço de retry. A persistência pós-commit da notificação de criação segue a arquitetura atual: falha nesse armazenamento é registrada pelo listener e não desfaz uma candidatura já commitada.

## 26. Erros HTTP

| Cenário | HTTP |
|---|---:|
| Anônimo | 401 |
| Contratante tenta criar / artista tenta transição formal | 403 |
| Vaga/candidatura inexistente ou candidatura de outro artista | 404 |
| Ativa duplicada / terceira tentativa | 409 |
| Perfil incompleto / vaga não ABERTA ou vencida / retirada inválida | 422 |
| Campos opcionais acima dos limites | 400 |
| Criação / retirada DELETE / retirada PUT | 201 / 204 / 200 |

## 27. Teste antigo substituído

`constraintRealTrataDuplicidadeMesmoSePreConsultaNaoEncontrar` era a falha conhecida do baseline e exigia uma UNIQUE global incompatível com recandidatura. A sessão interrompida já o havia substituído por `concorrenciaSubstituiPremissaDeUniqueGlobalEPermiteUmaAtiva`. Esta continuação reforçou a largada coordenada, conferiu o catálogo sem índice único secundário e contou ativas no banco. Foi acrescentada a corrida da segunda tentativa. A proteção contra duplicidade/concorrência permanece verificada com o contrato revisado; o teste não foi convertido em skip.

## 28. Testes adicionados/alterados

Somente a classe RF06 foi editada nesta continuação: 40 → 57 casos executados. Além das coberturas da seção 4, os testes herdados mantêm 401/403/404/422, JWT adulterado no payload, nulidade real, tamanhos legados, ownership/listagens, retirada via PUT/DELETE e rollback da retirada. Não foi alterada qualquer condição de skip ou fixture estrutural SQL.

## 29. Testes focados

JDK 21.0.11, Spring Boot 4.0.6, PostgreSQL 18.4/Testcontainers `postgres:18-alpine`, banco descartável `palco_test_manu04`, database04 original, `ddl-auto=validate`, isolamento READ_COMMITTED. O sandbox não acessava Docker/cache; a execução autorizada com acesso ao runtime funcionou, sem adaptação de código para erro de ambiente.

| Classe | Casos |
|---|---:|
| CandidaturaControllerRf06IntegrationTest | 57 |
| VagaPrazoRf23Rf06IntegrationTest | 9 |
| VagaCancelamentoRf25IntegrationTest | 30 |
| NotificacaoRf23IntegrationTest | 11 |
| NotificacaoWebSocketRf23IntegrationTest | 3 |
| NotificacaoSsePoolIntegrationTest | 1 |
| GuardianConsentRf27IntegrationTest | 10 |
| GuardianConsentServiceTest | 3 |
| JwtAuthenticationIntegrationTest | 4 |
| GenericEndpointsSecurityIntegrationTest | 26 |
| Database04BootstrapIntegrationTest | 5 |
| **Total/passed** | **159/159** |

Resultado: **0 failures / 0 errors / 0 skipped, BUILD SUCCESS, exit 0**, 3:02 min. Comando: `backend/.\mvnw.cmd '-Dtest=CandidaturaControllerRf06IntegrationTest,VagaPrazoRf23Rf06IntegrationTest,VagaCancelamentoRf25IntegrationTest,NotificacaoRf23IntegrationTest,NotificacaoWebSocketRf23IntegrationTest,NotificacaoSsePoolIntegrationTest,GuardianConsentRf27IntegrationTest,GuardianConsentServiceTest,JwtAuthenticationIntegrationTest,GenericEndpointsSecurityIntegrationTest,Database04BootstrapIntegrationTest' test`.

Evidências: `focados-final.log`, `focados-final.exit`, `focados-finais-totais.json` e `focados-selecionados-reports/` sob `evidencias/rf06-2026-10-02/`. A pasta `focados-finais-reports` preserva também XMLs antigos de outras classes; os totais focados consideram exclusivamente as 11 classes selecionadas, não esses resultados antigos.

## 30. Maven completo

Comando solicitado em `backend`: `.\mvnw.cmd test`, com o mesmo ambiente JDK 21/database04/Testcontainers. **BUILD SUCCESS, exit 0**, 6:55 min; término em 02/10/2026 às 22:59:46, America/Sao_Paulo. Não foram desabilitados testes nem alteradas condições. A falha histórica RF06 desapareceu com a substituição da premissa de UNIQUE global pelo contrato concorrente revisado.

Evidências: `evidencias/rf06-2026-10-02/maven-completo.log`, `maven-completo.exit`, `completo-final-reports/` e `completo-final-totais.json`. A soma dos XMLs foi confrontada com o resumo final do Maven; resultados idênticos.

## 31. Total/passed/failures/errors/skipped

| Execução | Total | Passed | Failures | Errors | Skipped | Resultado |
|---|---:|---:|---:|---:|---:|---|
| Baseline anterior informado e documentado no RF01/RF24 | 842 | 824 | 1 | 0 | 17 | BUILD FAILURE conhecido RF06 |
| Código herdado, focados executados nesta sessão | 137 | 137 | 0 | 0 | 0 | BUILD SUCCESS |
| Focados finais | 159 | 159 | 0 | 0 | 0 | BUILD SUCCESS |
| **Completo final** | **867** | **850** | **0** | **0** | **17** | **BUILD SUCCESS** |

Delta completo versus baseline informado: +25 casos (+8 já presentes na sessão herdada e +17 nesta continuação), +26 passed e −1 failure. Os 17 skips são os mesmos 4 casos de `CurrentLocalSchemaIntegrationTest` e 13 de `OfficialLocalApiIntegrationTest`, condicionados às propriedades abaixo; não contam como cobertura executada.

Os skips condicionais permanecem nas duas classes que exigem propriedades JVM explícitas: `palco.current-db-tests=true` e `palco.official-db-tests=true`. Não foram habilitados, removidos ou convertidos em skips novos para obter verde.

## 32. Semgrep

**Atualização documental em 03/10/2026:** recuperação posterior registrada com as evidências fornecidas pelo usuário, sem nova execução de scans ou Maven nesta atualização. RF06 backend continua concluído; os resultados Maven e as conclusões de banco/frontend permanecem inalterados.

**Histórico da tentativa inicial:** o scan focado em 11 arquivos, com 111 regras `p/java` + `p/secrets`, falhou no motor nativo, exit 2, `semgrep-core exited with code 3236495362`. Os artefatos `semgrep-final.log` e `semgrep-final.exit` preservam essa ocorrência; `semgrep-final.json` contém marcador de saída ausente, não JSON de achados. Essa tentativa não produziu resultado válido e não deve ser confundida com os scans posteriores bem-sucedidos.

**Causa e recuperação:** o Semgrep nativo do Windows continuou inviável porque o Windows App Control bloqueia o executável pysemgrep/semgrep. Foi adotado Semgrep via imagem Docker oficial. Como o container não conseguia baixar rulesets de `semgrep.dev` por cadeia de certificado não confiável, os rulesets foram baixados pelo Windows e montados localmente no container. **A verificação TLS NÃO foi desabilitada.**

| Execução posterior | Ruleset / escopo | Regras | Linhas parseadas | Resultado |
|---|---|---:|---|---|
| Java | `p/java`; 257 arquivos rastreados pelo Git | 60 | ~100% | 0 findings; execução concluída com sucesso |
| Secrets bruto | 2.271 arquivos | Não informado | Não informado | 181 findings, auditados conforme classificação abaixo |
| Secrets acionável | 960 arquivos rastreados pelo Git, após as exclusões explícitas abaixo | 42 executadas | ~100% | 0 findings; 0 blocking; execução concluída com sucesso |

A auditoria dos **181 findings do scan secrets bruto** mostrou predominância de hashes BCrypt de seeds, hashes BCrypt e dados fictícios em evidências/logs de testes e autodetecção das próprias expressões existentes no ruleset temporário. **Esses 181 achados não representam 181 credenciais reais vazadas.** A classificação do ruído antecedeu o scan acionável; não foi necessário alterar código do Palco para reduzir esse conjunto.

O scan secrets acionável excluiu explicitamente a regra `generic.secrets.security.detected-bcrypt-hash.detected-bcrypt-hash` e os seguintes caminhos de ruído já classificado:

- `evidencias/**`
- `graphify-out/**`
- `semgrep-java.tmp.yml`
- `semgrep-secrets.tmp.yml`

O resultado de 0 findings/0 blocking corresponde ao escopo com essas exclusões, não ao scan bruto. **0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.**

**Padrão operacional do workflow do projeto:** Semgrep passa a ser executado via Docker; a instalação nativa do Windows não deve ser usada neste ambiente. Nenhum código do Palco foi alterado para corrigir o problema da ferramenta. Esta atualização modifica somente documentação e não altera arquivos de configuração do workflow, testes, frontend, banco ou SQL.

## 33. Microauditoria do logging JDBC

Mantido `logging.level.org.hibernate.orm.jdbc.error=OFF`. A configuração restringe uma categoria de log; não captura exceções, altera transações, suprime handlers nem muda `ddl-auto=validate`. `ApiExceptionHandler` continua traduzindo integridade para 409 sanitizado. Os testes RF01 exercitam constraint real de username e falhas injetadas/rollback; RF06/RF28 exercitam falha de persistência e transporte. As inicializações atuais mostram Hikari, versão JDBC/PostgreSQL e EntityManagerFactory iniciado após validate.

As categorias de erro de Spring/EntityManagerFactory/validação de schema permanecem habilitadas; a fonte desses diagnósticos é independente da categoria JDBC silenciada. Não foi provocado novo startup inválido nem alterado schema para testar logging. O diagnóstico negativo de schema no relatório database04 é evidência histórica anterior, não uma nova execução. Não se afirma que todo log de qualquer biblioteca seja livre de dados pessoais. Dentro desta microauditoria, não há razão demonstrada para reativar os detalhes sensíveis ou refatorar logging.

## 34. Riscos e limites

As garantias de uma ativa/duas tentativas são do caminho Java transacional. A procedure oficial `sp_enviar_candidatura` possui prechecks próprios e não usa o mesmo advisory lock; não foi encontrada chamada Java a ela e ela permaneceu intacta. Escrita direta por outra aplicação deve respeitar a mesma serialização antes de ser homologada.

Entrega SMTP real e retries não foram verificados. O listener após commit mantém a candidatura em caso de falha; indisponibilidade de persistência da notificação pode exigir recuperação operacional. Semgrep foi recuperado posteriormente via Docker: scans Java e secrets acionável concluídos com 0 findings, conforme o escopo e as exclusões da seção 32; a instalação nativa permanece bloqueada pelo Windows App Control. O contrato visual ainda é legado; a validação desta entrega é backend e não uma homologação completa da jornada no navegador.

## 35. Pendências RF45

Não foram implementados filtro operacional só da candidatura vigente, paginação/contagem filtradas, controles visuais revisados nem início de chat contextual. Leitura dos estados/histórico continua possível. A desativação herdada das transições formais no PUT foi preservada, sem remoção de endpoints ou enum. RF45 permanece pendente.

## 36. Integração frontend futura

O modal existente confirma a candidatura, mas ainda exige mensagem/link no formulário. O botão no detalhe é bloqueado por qualquer `minhaCandidaturaId`, inclusive depois de retirada; não apresenta o ciclo revisado de recandidatura. A interface de gestão ainda oferece análise/aceite/rejeição, que o backend atual rejeita com 422. Em tarefa própria, integrar POST mínimo, elegibilidade de segunda tentativa, mensagem do limite, retirada ABERTA/PAUSADA e RF45, mantendo JWT como autoridade. Nenhuma dessas correções visuais foi antecipada aqui.

## 37. Confirmação do database04 intacto

ZIP oficial: `C:\Users\masca\Downloads\palco-database04.zip`, SHA-256 `52b1c4af06d79a7efae47e6fa320b4a0a32359efaae1d40e2a73d06129c6df2b`. Comparação direta ZIP → pacote: **46/46 arquivos idênticos, 0 divergências, 0 arquivos adicionais**. Fingerprint validado pelo bootstrap: `db8f05cabadfd3935b7c703b7b21252f170fa529c0d30e7e61755b46d7fd5f39`.

Manifesto inicial → atual: 663 arquivos protegidos comparados, 0 divergências, incluindo 521 arquivos de frontend e 138 arquivos SQL (os grupos se sobrepõem). Evidências: `database04-preservado.json`, `fronteiras-preservadas.json` e `delta-desta-sessao.json`. Nenhum snapshot histórico foi usado para a inicialização dos testes.

## 38. RF06 pode ser considerado concluído?

**SIM, backend RF06 concluído no escopo desta tarefa.** Autorização, perfil completo, ABERTA/prazo, campos opcionais, uma ativa, locks PostgreSQL, retirada lógica ABERTA/PAUSADA, segunda linha distinta, terceira tentativa 409, histórico/legado, notificações e aviso informativo ao responsável passaram. Maven completo encerrou com BUILD SUCCESS/0 failures/0 errors e os hashes confirmam preservação do database04/frontend.

Isso não declara RF44 integral, RF45, SMTP real ou a integração frontend concluídos. O aviso RF44 corresponde exclusivamente à candidatura e foi testado com sender mockado. A confirmação explícita foi auditada no modal existente; a jornada visual revisada continua pendente.

## 39. Próximo passo para checkpoint Git

Revisar este relatório, os três arquivos de código/teste alterados nesta continuação e o delta RF06 já herdado, separando os demais trabalhos anteriores. Evidências finais de `git status --short`, `git diff --stat` e `git diff --check` ficam em `evidencias/rf06-2026-10-02/git-*-final.*`; **diff --check final: exit 0; index sem alterações**. Nenhum arquivo foi staged/commitado automaticamente. Um checkpoint depende de autorização posterior do usuário e seleção consciente do conjunto de alterações.

O Graphify foi atualizado conforme o AGENTS.md local: atualização AST, exit 0, 5.588 nodes/18.125 edges/321 comunidades. Os derivados/backups em `graphify-out` devem ser revisados separadamente e não representam nova funcionalidade.

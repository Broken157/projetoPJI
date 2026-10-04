# RF42 — Convite para Vaga — 03/10/2026

**RF42 backend: CONCLUÍDO.** RF13 backend concluído no checkpoint; RF17, RF35 e RF36 globais permanecem PARCIAIS. Banco/frontend não alterados. Sem staging, commit ou push.

Checkout: C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline.
Branch: integracao-recuperada-2026-09-15.
Evidências: evidencias/rf42-2026-10-03/.

## 1. Objetivo

Permitir que CONTRATANTE convide ARTISTA do seu Banco RF13 para conhecer vaga própria ABERTA. Convite persistente/idempotente, confirmado, com notificação e link; sem candidatura automática, aceite/rejeição ou obrigação de resposta. Integrar somente o reconhecimento dessa interação no chat protegido RF35.

## 2. RF/RNFs consultados

Fonte oficial: C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt. RF42, RF13, RF17, RF06, RF23, RF28, RF35, RF36, RF27 e RF44 integrais. RNF02/RNF05/RNF06/RNF07/RNF08/RNF09/RNF10/RNF17 para binding, performance, privacidade, integridade, autorização, logs, testes e paginação. Hash atual em checkpoint-inicial.json.

Consultados os relatórios RF13 participação, RF17, RF13/RF36, sincronização database05, RF06, RF45 e vagas RF04/RF07/RF23/RF28. Históricos de database04 e antigas pendências RF13 não substituem o checkpoint atual; nenhum relatório anterior reescrito. Skill pji-backend-audit aplicada anteriormente nesta conversa: fontes atuais e fronteiras explícitas prevalecem sobre orientações antigas de snapshot/comando.

## 3. HEAD/checkpoint

**228eb4e1d05523180b3c106f35f3938397e52dad**, feat: conclui RF13 com notificacao do banco de talentos, confirmado em HEAD, upstream e git ls-remote do fork. Database05 checkpointado em ef570e1; RF13/RF36 checkpointado em 228eb4e. Backend e configuração/snapshot de sincronização sem delta anterior; index vazio.

Working tree anterior contém README, SQL histórico, frontend e artefatos locais; preservados por manifesto e não atribuídos ao RF42. Evidências: git-status-inicial.txt, git-log-inicial.txt, checkpoint-inicial.json e arquivos-iniciais.json.

## 4. Graphify inicial

MCP HTTP existente em 127.0.0.1:8765/mcp consultado antes de editar Java. Grafo inicial **5884 nós**. Queries 302–305 mapearam participação/Talento, Vaga/repository/candidatura, persistência/listener/chat e SecurityConfig/AuthenticatedUserResolver.

Query 303 foi truncada (26/39 nós); as demais retornaram seus nós/arestas completos acima do orçamento solicitado. Limites registrados; grafo orientou a navegação, e código/SQL completos pertinentes fundamentaram a decisão. Sem repair, label ou launcher MCP.

## 5. Auditoria database05

Fonte única ativa database05/palco-database/. Tabelas relevantes: banco_talentos com PK do par; vagas com owner/status/prazo; notificacoes com destinatário/tipo/mensagem/link/lida/data. CONVITE já existe no enum PostgreSQL/Java. Ddl-auto=validate mantido, PostgreSQL real/Testcontainers; sem H2, DDL auxiliar ou migration.

Seed oficial não contém CONVITE de vaga. Nenhuma tabela/relação própria de convite encontrada no pacote de 46 arquivos, nem trigger que implemente RF42.

## 6. Estrutura existente para convite

**A:** não há entidade/tabela própria. **B:** não há RF42 parcial anterior. **C:** services/repositories de convite anteriores tratam consentimento RF27, sem convite profissional. **D:** notificacoes consegue representar o contrato mínimo por tipo/destinatário/contexto, com coordenação transacional backend e consulta por igualdade exata.

NotificacaoController permite GET/PATCH de leitura, sem criação de tipos/contextos pelo cliente. Produtores atuais não utilizavam CONVITE. RF07 não transfere owner; API não apaga vaga publicada. Marcar lida não remove/reescreve contexto. Decisão anterior à implementação em auditoria-contrato.md.

## 7. Decisão técnica de persistência

Identidade: CONTRATANTE proprietário da vaga + ARTISTA destinatário + VAGA. Representação: notificacoes.usuario_destino_id, tipo_notificacao=CONVITE e link_contexto=/vagas/{id}. O owner é derivado da vaga persistida, sem campo de autoridade do cliente. Nenhum dado escondido em mensagem.

Consulta sob lock da vaga antes de persistir. Somente primeira ocorrência chama persistência/evento; repetição retorna notificacaoId original. Backend exclusivo de emissão e contrato canônico são requisitos desse desenho.

## 8. Notificação como registro técnico

**SIM.** O registro Notificacao CONVITE é simultaneamente o convite lógico RF42 e o alerta da central RF36. Não há segunda entidade/transação que possa deixar convite sem alerta. Timestamp/ID e estado lida permanecem históricos; leitura não revoga o convite.

## 9. Justificativa

RF42 não exige aceite, rejeição, resposta, expiração própria ou estado do convite. Tipo/destino/contexto oficial bastam ao fluxo atual. Lookup usa igualdade do contexto gerado por Long validado; RF35 usa JOIN por igualdade com contexto calculado da vaga, sem parsing/cast de URL recebida nem busca no texto.

Ausência de UNIQUE específico compensada pelo lock PostgreSQL já existente, não por synchronized. Link sem FK é um limite explícito: recurso ausente/rascunho ou owner diferente não prova interação. Novos produtores CONVITE, transferência de owner ou retenção devem reavaliar esse contrato.

## 10. Arquivos de produção

| Arquivo | Mudança |
|---|---|
| backend/src/main/java/com/portifolio/controller/ConviteVagaController.java | Novo GET de seleção e POST confirmado |
| backend/src/main/java/com/portifolio/dto/ConviteVagaRequest.java | vagaId positivo/obrigatório e confirmado obrigatório |
| backend/src/main/java/com/portifolio/dto/ConviteVagaResponse.java | artistaId/vagaId/notificacaoId |
| backend/src/main/java/com/portifolio/dto/ConviteVagaOpcao.java | id/titulo da vaga para seleção |
| backend/src/main/java/com/portifolio/repository/ConviteVagaRepository.java | Consulta do registro e prova profissional RF35 |
| backend/src/main/java/com/portifolio/service/ConviteVagaService.java | Autorização, seleção, lock/idempotência e persistência |
| backend/src/main/java/com/portifolio/config/SecurityConfig.java | Liberar somente POST RF42 para CONTRATANTE |
| backend/src/main/java/com/portifolio/service/ChatService.java | Aceitar prova RF42 no guard profissional do menor |

Seis arquivos novos e dois modificados. RF13/RF17, VagaService/Repository/prazo, enum, RF06, notificações/realtime compartilhados preservados.

## 11. Arquivos de teste

Nova ConviteVagaRf42IntegrationTest.java com PostgreSQL oficial; ampliada NotificacaoWebSocketRf23IntegrationTest.java com CONVITE por POST RF42 real. Casos CANDIDATURA e BANCO_DE_TALENTOS anteriores preservados. Nenhum skip/condição/assertion antigo removido.

## 12. Endpoints

GET /api/talentos/{artistaId}/convites/vagas: seleção paginada para membro autorizado. POST /api/talentos/{artistaId}/convites: enviar confirmado. Padrão do namespace/identificadores RF17, controller próprio sem mudar DTOs/busca RF17.

/api/vagas/minhas inclui estados diversos; /api/talentos/contextos é contexto de busca profissional. Nenhuma das duas garante o filtro ABERTA/no prazo com autorização do alvo RF42. Novo GET reutiliza as Specifications de vagas existentes.

## 13. Payload

POST: {"vagaId":123,"confirmado":true}. ARTISTA alvo vem do path; CONTRATANTE vem do JWT/resolução persistida. Extras ownerId/contratanteId/usuarioId/tipo/status não são lidos como autoridade. Resposta mínima: artistaId, vagaId e notificacaoId, sem entidades completas.

## 14. Autenticação

Anônimo/token inválido: 401 pelo padrão global. JwtAuthFilter valida token/subject/id e UserDetailsService revalida conta persistida. AuthenticatedUserResolver identifica o dono; service também exige usuário autenticado. JWT antigo não contorna papel/estado atual.

## 15. Autorização

CONTRATANTE exclusivamente, ativo e com acessoNormalPermitido segundo a política existente. ADMIN/MODERADOR/ARTISTA rejeitados. Service revalida papel/conta independentemente do matcher HTTP. Artista alvo exige perfil público/artista apto e política atual do menor; nenhum privilégio implícito de administração.

## 16. Ownership Banco RF13

BancoTalentosRepository.participa(dono JWT, artista path) é autoridade. Perfil público, Salvo, candidatura ou outro Banco não substituem esse vínculo. Erro 404 genérico sem revelar relação alheia. RF42 não insere/remove/copia membership.

## 17. Ownership vaga

Após lock, comparar vaga.contratante.usuarioId ao ID persistido do JWT. Vaga alheia/inexistente retorna 404 genérico. DTO não admite troca de owner; rotina RF07 não transfere a vaga. Query RF35 associa somente vaga daquele contratante.

## 18. Estado ABERTA

POST exige ABERTA depois de obter PESSIMISTIC_WRITE. RASCUNHO/PAUSADA/ENCERRADA/CANCELADA rejeitados com 422. Seleção só retorna ABERTA. Repetição também revalida elegibilidade; não envia convite novo para vaga tornada inelegível.

## 19. Data limite

Reutilizada VagaPrazoPolicy. Prazo null/futuro elegível; data igual/anterior a hoje inelegível, mesmo se scheduler ainda não mudou ABERTA. GET usa VagaSpecifications.prazoAindaValido(prazo.hoje()). Nenhuma nova definição de relógio/abertura; GET/POST RF42 não alteram status ou disparam encerramento.

## 20. Listagem de vagas

Envelope Pagina existente: content/page/size/totalElements/hasMore/contexto. Default 0/20, tamanho 1–50; inválidos 400. ORDER BY id ASC determinístico. Count/filtro/paginação no PostgreSQL, opções id/titulo apenas. Não acessa associações/coleções lazy por item, nem filtra páginas em memória. Sem dados de remuneração/endereço privado/contatos.

## 21. Confirmação

@NotNull: ausente/null 400; false 422 conforme RF13; somente true emite. Body/formato/ID inválidos 400. Abrir seleção/consultar RF17/perfil não envia convite. GET é readOnly e não publica eventos.

## 22. Primeira emissão

HTTP 201; uma Notificacao CONVITE no ARTISTA, com ID/data/lida=false/contexto real. Mensagem: “Um contratante convidou você para conhecer uma vaga.” Sem convite automático para outro membro ou outro Banco. Não há operação de aceite/rejeição.

## 23. Idempotência

Mesmo artista/vaga, owner derivado: HTTP 200 com a mesma resposta/notificacaoId. Preserva ID/data/lida, não cria segundo alerta e não entrega novamente. Vaga/ARTISTA diferentes são identidades distintas. Marcar lida/todas não recria o convite.

## 24. Concorrência

VagaRepository.findByIdForUpdate, PESSIMISTIC_WRITE, mesma linha bloqueada por RF23/RF28/RF06. Checagem/insert sob transação, depois de esperar o lock, impede duplicação entre processos/instâncias. Não há check-then-insert livre ou synchronized.

O lock serializa convites da mesma vaga, mesmo para artistas distintos; transação curta, sem rede externa/realtime nela. Teste de duas chamadas reais com barreira repetido três vezes; resultados confirmados na seção 40.

## 25. Notificação CONVITE

Tipo oficial existente, sem alteração de enum. NotificacaoPersistenceService.persistirNaTransacaoAtual (MANDATORY) grava/flush; evento NotificacaoPersistida publicado uma vez. Central relê o enum normalmente e restringe destinatário pelo JWT. Persistência independe de sessão ativa/entrega.

## 26. Link_contexto

/vagas/{id}: rota real consolidada no cliente integrado e frontend oficial, com API RF05 /api/vagas/{id}. Identifica a vaga diretamente, sem candidatura automática. Igualdade exata do contexto é o contrato técnico; não aceita URL externa, query/contexto alternativo ou texto da mensagem como autorização.

## 27. After-commit

Listener existente entregar(NotificacaoPersistida), AFTER_COMMIT. Dentro da transação há registro flushado e zero entregas; commit habilita callback. Rollback depois do flush apaga convite/alerta e não entrega. Falha saveAllAndFlush aborta a transação; retentativa depois de rollback pode criar.

## 28. WebSocket

Fila privada /user/queue/notificacoes, JWT no Authorization do CONNECT. Novo caso real abre assinantes destino/isolado, cria participação RF13 e envia POST RF42, conferindo tipo/link/id/mensagem persistidos. Teste local de entrega ≤5 segundos; não equivale a homologação de carga RNF05.

## 29. SSE

/api/notificacoes/stream com Bearer em header, sem JWT em query. Novo caso real recebe CONVITE decorrente do POST RF42 no artista e não no isolado. Transporte/serialização existentes preservados; regressão do pool de conexões incluída.

## 30. Falha realtime

Falhas WebSocket/SSE/gateway são isoladas após commit; convite/alerta permanecem na central, resposta 201 preservada. Falha WS não impede tentativa SSE. Nenhuma nova infraestrutura de retry/outbox; repetição idempotente não reenvia alerta.

## 31. Candidatura não criada

**NÃO.** Nenhuma chamada CandidaturaService, inserção, mudança de status ou consumo de tentativa. Testes com candidatura existente em todos os estados oficiais comparam linhas completas antes/depois. Convite seguido de RF06 percorre candidatura, retirada, única recandidatura e bloqueio da terceira tentativa, mantendo um convite.

RF42 não dispensa completude/status/prazo RF06. Ambiguidade oficial sobre artista já candidato registrada: convite literal permitido uma vez, mantendo candidatura intacta; nenhum novo veto funcional inventado.

## 32. Interação RF35

Guard profissional do menor mantém candidatura existente como prova e admite, adicionalmente, ConviteVagaService.interacaoProfissionalValida. Query cruza CONVITE destinado àquele ARTISTA com contexto canônico de vaga daquele CONTRATANTE, excluindo rascunho. Não interpreta mensagem nem usa membership isolado.

Convite lido e vaga posteriormente PAUSADA/ENCERRADA/CANCELADA preservam interação histórica, sem exigir candidatura ativa. Papéis/acesso das contas e consentimento atuais revalidados. Outro contratante/artista/contexto genérico não aproveita o registro. Não cria sala automaticamente ao convidar.

## 33. Menor

ARTISTA 14–17 ativo, autorizado com consentimento atual e sem revogação pode receber convite. Menor inapto/abaixo de 14/sem responsável/consentimento inválido é ocultado/rejeitado. Mensagem/contexto/DTO não incluem nascimento, responsável, consentimento, CPF, e-mail, telefone ou experiência privada.

Antes de interação válida, membership sozinho não libera chat; após convite válido, mesmo contratante pode iniciar. Revogação/conta inapta continuam bloqueando. Não implementado novo mecanismo de revogação; apenas respeitado o estado persistido de proteção existente.

## 34. RF44

Não ampliado. Convite não dispara e-mail ao responsável, nem nova obrigação externa. RF44 inicial trata publicação/candidatura; RF42 não altera seus senders/listeners, regras ou histórico.

## 35. Banco alterado: NÃO

Sem SQL/schema/constraints/enums/índices/functions/triggers/seed/migrations/init/patch, scripts ou banco de desenvolvimento alterados. DML e limpeza exclusivamente nos containers descartáveis database05. Configuração/validate preservados; database04 somente histórico.

## 36. Frontend alterado: NÃO

Somente auditoria de rota/link/card/API. Sem npm/build/install/test/format, modal ou redesign. Os 770 arquivos frontend anteriores protegidos por hash; alterações anteriores mantidas sem atribuição ao RF42. Integração visual fica para tarefa própria.

## 37. Database05 46/46

**46/46 arquivos idênticos byte a byte**, mesmos paths; 0 ausentes, 0 divergentes, 0 adicionais. ZIP e SHA-256 oficiais confirmados. Os 1402 arquivos protegidos permanecem iguais ao início: 770 frontend, 264 Java de produção fora do delta autorizado, 47 database04 e 5 scripts04, além das demais fontes/configurações/históricos. As 342 fontes Java testadas permanecem inalteradas após os testes. Escopo final: somente três fontes existentes autorizadas modificadas e sete fontes RF42 novas, sem mudança inesperada; HEAD original e index vazio. Proveniência dos testes confirma database05 ativo, nenhuma inicialização/JDBC database04. Evidências preservacao-final.json, fontes-testadas-final.json, escopo-final.json, requisitos-final.json e maven-proveniencia.json. As 139 evidências textuais verificadas no fechamento não contêm o segredo runtime conhecido com pelo menos oito caracteres nem JWT assinado (privacidade-evidencias.json); isso não constitui auditoria absoluta de segredos.

ZIP original C:\Users\masca\Downloads\palco-database05.zip, SHA-256 6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf. Comparação direta dos bytes descomprimidos, incluindo arquivo vazio, mesmos paths, sem normalização Git. Resultado em snapshot-final-byte-a-byte.json; arquivos protegidos e fontes testadas reconferidos no fechamento.

## 38. Testes novos

Nova suíte RF42 cobre autenticação/papel/conta/IDs adulterados; membership e outras relações; proprietário/status/prazo; confirmação/paginação/whitelist; persistência/central/lida; repetição/corridas; candidatura/RF06; transação/rollback/falhas; menor e prova do chat. Dois cenários reais CONVITE acrescentados ao WebSocket/SSE existente. A matriz criterios-aceitacao.md associa os 79 critérios mínimos aos testes/inspeções correspondentes.

Diagnóstico preservado: primeira tentativa falhou na compilação do teste novo por método SSE notificar inexistente, 17,121 s, sem executar testes. Corrigido para entregar existente; nenhum código de produção alterado para isso, nenhuma assertion retirada. Evidências focados-tentativa-1-compilacao.* separadas dos resultados finais.

Segunda tentativa: 593 total/592 passed/1 failure/0 errors/0 skipped, 25 classes, 05:26 min. Payload do novo caso de atualização RF08 omitia raioAtuacao e recebeu 422; completado com valor oficial LOCAL/cidade/UF. Assertion HTTP 200 preservada; nenhuma regra de perfil alterada. Diagnóstico em focados-tentativa-2.log/.exit/-resumo.json. O resultado final abaixo é uma rodada posterior sobre as fontes corrigidas.

## 39. Regressões

25 classes em focados-classes.txt: RF42, participação RF13, busca RF17, publicação/gestão/prazo/cancelamento/listagem vagas, RF06, RF45, central/listener/WebSocket/SSE, chat REST/STOMP, RF27, perfil público, segurança/JWT, bootstrap/schema/SQL oficial. RF13/RF17 fontes/testes preservados; demais integrações abrangidas pelo Maven completo.

## 40. Focados

**593 total / 593 passed / 0 failures / 0 errors / 0 skipped**, 25 classes, BUILD SUCCESS, exit 0, 05:35 min; finalizado 03/10/2026 20:45:18 -03. RF42 77/77; RF13 68/68; RF17 86/86; realtime RF23 ampliado 7/7. As três corridas concorrentes produziram uma 201/uma 200, um registro e uma entrega por par.

Comando -Dtest com as 25 classes e -Dspring.test.mockmvc.print=NONE test. Nenhum skip; XMLs somente posteriores ao início da rodada. Evidências focados.log/.exit, focados-totais.json e focados-reports/.

## 41. Maven completo

**1227 total / 1210 passed / 0 failures / 0 errors / 17 skipped**, 64 classes, BUILD SUCCESS, exit 0, 08:00 min; finalizado 03/10/2026 20:54:23 -03. Evidências maven-completo.log/.exit, maven-completo-totais.json e maven-completo-reports/.

Comando .\mvnw.cmd test '-Dspring.test.mockmvc.print=NONE', sem clean/filtro; scripts/database05/environment.ps1 e PALCO_TEST_DATABASE05_PATH. PostgreSQL/Testcontainers reais, validate. Os 17 skips condicionais conhecidos permaneceram nas mesmas duas classes/quantidades, conforme regressao-comparacao.json; condições antigas intactas.

## 42. Totais

Focados 593/593, sem skips; completo 1227 total/1210 passed/0 failures/0 errors/17 skipped. Delta sobre o checkpoint: +77 casos RF42 e +2 CONVITE WebSocket/SSE, +1 classe; nenhum skip novo ou falha mascarada.

Baseline checkpoint RF13/RF36: 1148 total/1131 passed/0 failures/0 errors/17 skipped, 63 classes. Resultados atuais prevalecem sobre previsão. Skips históricos: CurrentLocalSchemaIntegrationTest 4, OfficialLocalApiIntegrationTest 13. Não reivindicada porcentagem de cobertura RNF10.

## 43. Semgrep

**Semgrep 1.178.0 via Docker oficial, p/java: 272 arquivos Java de produção, 60 regras, ~100,0% das linhas parseadas, 0 findings, 0 blocking, 0 errors, exit 0; 227,8 s.** Execução concluída com sucesso. Alvo backend/src/main/java; --no-git-ignore inclui as seis fontes RF42 novas sem rastreamento Git, sem staging. paths.scanned confirmou os oito arquivos de produção alterados/novos. Evidências semgrep-java.json/-saida.log/-resumo.json/-execucao.json e Executar-Semgrep.ps1.

Docker oficial fixado em digest sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b. p/java local SHA-256 5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1, cache oficial baixado pelo Windows com TLS verificado, proveniência anterior registrada. Sem rede, checkout read-only, sem Semgrep nativo Windows ou TLS desabilitado.

0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.

## 44. Graphify final

**graphify update . concluído, exit 0, 24,0 s; MCP HTTP final confirmou 5971 nós, 20214 arestas e 340 comunidades**, EXTRACTED 91%/INFERRED 9%/AMBIGUOUS 0%. AST 293/293 arquivos não cacheados. Avisos: 75 arquivos não classificados; 45 SQL sem contribuição por tree_sitter_sql ausente; 333 labels antigos para 340 comunidades, 83 nomes ajustados por hub. Nenhum parser instalado/label executado/contorno aplicado. HTML agregado em 340 comunidades por ultrapassar 5000 nós (1127 arestas entre comunidades). Evidências graphify-update.log/-execucao.json, graphify-mcp-final-402.txt e graphify-final-resumo.json; artefatos graphify-out atualizados.

graphify update . AST depois das mudanças Java. Sem label, graphify-mcp.exe, instalação de parser, reparo de App Control ou contorno de segurança. Parser SQL/labels anteriores são limites; SQL auditado diretamente, testes PostgreSQL sustentam compatibilidade.

## 45. Riscos

Registro técnico depende do contexto canônico e da emissão exclusivamente autorizada no backend. Sem FK de link/UNIQUE de convite; protocolo de lock deve ser usado por futuros emissores. API atual não transfere/apaga vaga publicada nem permite apagar/forjar convite; evolução de retenção/owner/produtores exige revisão.

Lock da vaga serializa artistas daquela vaga; não houve teste de carga p95/p99. Realtime best effort, sem retry/outbox. Profile/contexto pode perder disponibilidade pública depois; isso não dispara candidatura nem apaga registro. Snapshot oficial preserva suas dívidas anteriores.

## 46. Limitações

Sem homologação frontend, carga representativa, SMTP real ou implementação global RF35/RF36. Convite não oferece resposta/aceite/rejeição/expiração independente. 17 testes locais condicionais não executados permanecem uma limitação constatada na rodada completa. Graphify AST não renova semântica dos documentos.

## 47. RF42 final

**CONCLUÍDO no backend**, após auditoria estrutural, implementação, 593 focados verdes, Maven completo BUILD SUCCESS, Semgrep sem findings/erros e Graphify atualizado/confirmado. Seleção própria, confirmação, autorização, persistência/idempotência concorrente e integração mínima RF35/RF36 validadas. Nenhum blocker estrutural restante para o contrato mínimo atual; limites de evolução explicitados.

Conclusão limitada ao backend e ao contrato técnico autorizado, com requisitos rastreados em criterios-aceitacao.md. Não se declara entrega visual ou alteração de estrutura de banco.

## 48. Impacto RF35

Prova de convite RF42 passa a permitir a interação profissional daquele contratante/menor autorizado, somada à revalidação atual. Apenas esse guard foi ampliado; salas/mensagens/participação/edição/retenção/anexos não foram ampliados. RF35 global permanece PARCIAL pelas demais pendências.

## 49. Impacto RF36

CONVITE oficial passa a ser emitido por RF42 e aceito pela central, lida/lidas e transportes existentes. Nenhum enum/mapper/infra paralelo novo. RF36 global permanece PARCIAL; RF13 backend continua concluído.

## 50. Pendências

Nenhuma pendência bloqueante no backend RF42 dentro deste escopo. Sem evidência de homologação visual/carga representativa; entregas fora deste contrato permanecem separadas.

Integração visual RF42, demais entregas RF35/RF36 e decisão RF17 sobre experiência privada do menor em tarefas próprias. Nenhum checkpoint pelo agente; working tree amplo requer seleção específica numa futura autorização.

## 51. Próximo passo

Revisar os oito arquivos de produção, dois arquivos de teste e evidências deste delta. Em tarefa posterior autorizada, integrar seleção/confirmação/link ao frontend e continuar pendências globais sem confundir convite com candidatura.

Inspeções finais: git status --short, git diff --stat, git diff --check, git diff --name-status -- backend; saídas/códigos em git-inspecao-final.json e git-*-final.txt. Todas as quatro inspeções retornaram exit 0; git diff --check sem saída de erro de whitespace. Stderr contém apenas avisos usuais LF/CRLF. Diffs globais incluem alterações anteriores, preservadas; os arquivos RF42 novos permanecem sem staging.

## Registro semanal

**Data:** 03/10/2026.
**Objetivo:** RF42 Convite para Vaga.
**RF/RNF:** RF42/RF13/RF17/RF06/RF23/RF28/RF35/RF36/RF27/RF44; RNF02/RNF05/RNF06/RNF07/RNF08/RNF09/RNF10/RNF17.
**Backend alterado:** seis arquivos novos RF42; matcher POST e guard RF35; suíte nova e dois casos realtime.
**Frontend alterado:** NÃO.
**Banco alterado:** NÃO.
**database05:** 46/46 idêntico byte a byte ao ZIP oficial; zero ausentes/divergentes/adicionais; database04 histórico intacto.
**Funcionalidade concluída/avançada:** convite confirmado, próprio/membro/ABERTA/no prazo, registro CONVITE e idempotência.
**Autorização/ownership:** identidade JWT persistida; Banco próprio, artista acessível, vaga própria.
**Notificação:** CONVITE persistido na transação; AFTER_COMMIT/realtime existentes; contexto /vagas/{id}.
**Candidatura:** NÃO criada/alterada; RF06 permanece ação independente.
**Menor/chat:** convite profissional permite chat daquele contratante, respeitando conta/consentimento atual; nenhuma exposição privada/RF44 novo.
**Testes:** focados 593/593 sem skips; Maven 1227 total/1210 passed/0 failures/0 errors/17 skipped, BUILD SUCCESS.
**Semgrep:** Docker 1.178.0 p/java, 272 arquivos/60 regras/~100% parseado/0 findings/0 blocking/0 errors, exit 0.
**Graphify:** update AST exit 0, 24,0 s; MCP HTTP confirmou 5971 nós/20214 arestas/340 comunidades, limites de SQL/labels registrados.
**RF42 final:** CONCLUÍDO no backend, sem entrega visual reivindicada.
**RF13:** backend CONCLUÍDO, preservado.
**RF17:** PARCIAL, preservado.
**RF35:** reconhecimento de convite integrado; global PARCIAL.
**RF36:** emissão RF42 integrada; global PARCIAL.
**Pendências:** frontend e demais RF35/RF36/decisão RF17; nenhuma mudança de banco aplicada.
**Próximo passo:** revisão/checkpoint específico pelo usuário; frontend em tarefa própria.

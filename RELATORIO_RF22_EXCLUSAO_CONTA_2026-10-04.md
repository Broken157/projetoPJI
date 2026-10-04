# RF22 — Exclusão de conta — 04/10/2026

## 1. Objetivo e situação

**RF22 PARCIAL.** Implementada exclusão real pela API para contas com senha local e domínios cuja política pode ser aplicada com segurança no database05. Cenários sem confirmação/política/isolamento adequado são recusados antes de qualquer escrita. A conclusão global não é autorizada pelo simples sucesso dos cenários suportados.

Evidências desta tarefa: `evidencias/rf22-2026-10-04/`. Nenhum relatório histórico foi reescrito.

## 2. Fontes e requisitos

Fonte oficial atual: `C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt`, SHA-256 `3e7ca8fa7e78a1967cf5f86c86f6a60f736879b02cfbd8f9d195040fea873183`. Consultados RF22, RF35, RF06, RF18, RF23, RF27, RF36 e RNF06/07/08/09/10/17/19. Confirmar senha existente não equivale a criar/trocar senha sob RNF19.

Relatórios RF35, sincronização database05, RF06, RF13/RF36, RF42, RF45, RF26, bloqueio RF02/RF26 e storage RF16 forneceram histórico. Seus totais antigos e referências a database04 não são apresentados como validação atual RF22. Código vigente, pacote database05 e PostgreSQL dos testes decidem o contrato.

## 3. Checkpoint e escopo anterior

Checkout `C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline`; branch `integracao-recuperada-2026-09-15`; HEAD/upstream **ae52249e1f3a98a34dc5c2129d35b465f6b974da**, `feat: avanca RF35 com sistema de mensagens`. Index vazio e zero deltas soltos em backend/database05/database04/scripts antes de editar. RF35 checkpointado.

README, SQL histórico em `database/` e frontend já estavam alterados, além de artefatos não rastreados anteriores. Preservados sem reset/restore/stash/clean. Manifesto inicial: 1420 arquivos; 1411 protegidos, excluindo somente nove Java existentes previstos nesta tarefa. `checkpoint-inicial.json`, `git-status-inicial.txt`, `arquivos-iniciais.json`, `escopo-autorizado.json`.

## 4. Graphify inicial

Graphify MCP HTTP existente consultado antes de editar: **6074 nós / 20676 arestas / 341 comunidades**, 91% EXTRACTED, 9% INFERRED, 0% AMBIGUOUS. Consultas mapearam Usuario/Controller/Service/Repository, JWT/refresh, perfis, candidaturas, chat, denúncias, vagas, portfolio/storage e exclusão LGPD. Queries amplas truncadas: 49/92 e 54/56 nós mostrados; não tratadas como leitura integral.

Respostas integrais: `graphify-inicial-0.json` a `graphify-inicial-2.json`. Busca posterior da procedure encontrou três versões históricas ambíguas, sem nó database05. Isso reforça a necessidade de ler SQL atual diretamente; grafo não homologa schema nem anonimização.

## 5. Database05 e todas as relações

Fonte ativa única `database05/palco-database/`, 43 tabelas e 57 FKs. Testcontainers usa PostgreSQL real e `PALCO_TEST_DATABASE05_PATH`; `ddl-auto=validate` conservado. Sem H2, migration, patch ou DDL complementar. Database04 é histórico.

Auditoria dos dez DDLs, seed, procedure de exclusão e triggers relevantes. Inventário anterior utilizado apenas como índice, revalidado pelo catálogo vivo na nova suíte. Todas as 57 FKs, inclusive catálogos e referências indiretas, registradas em [fks-auditadas.md](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/rf22-2026-10-04/fks-auditadas.md) / `fks-auditadas-inicial.json`: tabela, coluna, nullabilidade real, destino e ON DELETE. Distribuição: 44 CASCADE, 7 SET NULL e 6 NO ACTION. PK composta torna NOT NULL mesmo colunas sem declaração inline explícita.

## 6. NOT NULL/CASCADE e conta reservada oficial

`candidaturas.artista_id`, `vagas.contratante_id`, `reportes_usuario.denunciante_id`, ambos os atores de `denuncias_plagio`, `log_vagas_canceladas.cancelado_por_id` e `moderacao_conteudo.autor_id` são NOT NULL. Excluir os pais em cascata destruiria histórico necessário.

**O seed oficial já autoriza ID 0 para RF22 e cria ambos os perfis.** Sua utilização não inventa usuário fake, owner nem referência fora do pacote. O serviço verifica identidade neutra da conta, ausência de Google/avatar, CPF fictício oficial e perfis neutros; não cria/repara essa estrutura. Conta reservada ausente/incompatível: 422 sem escrita. Autenticação normal, perfil público/descoberta e candidato operacional ID 0 ficam impedidos. Nenhuma alteração no registro reservado do banco de desenvolvimento.

Não há blocker universal das FKs acima enquanto essa estrutura oficial íntegra existir. Sem ela, o requisito afetado fica bloqueado; nunca substituir por CASCADE cego ou usuário improvisado. Identidade compartilhada ID 0 não permite reconstruir agrupamentos de pessoas já apagadas; IDs/datas/linhas de tentativas permanecem.

## 7. Procedure oficial auditada

`04_procedures/sp_excluir_conta_lgpd.sql` **não é chamada**. Protege ID 0, reatribui referências ao seed e remove parte dos dados, mas aceita motivo livre no comprovante; não confirma senha/JWT; não gerencia arquivos; conserva apresentação/link das candidaturas e anexos pessoais; não limpa todos os alvos polimórficos; não separa corretamente rascunho de publicada; tampouco implementa uma política restrita completa de evidência.

A orquestração Java usa DML parametrizado por domínio. O SQL oficial permanece byte a byte intacto. Existência da procedure e inicialização bem-sucedida não significam validação funcional RF22.

## 8. Matriz antes do código

[Matriz completa das 43 tabelas](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/rf22-2026-10-04/matriz-antes-do-codigo.md) criada **antes da produção**: ação oficial DELETE/ANONYMIZE/RETAIN, física database05, backend necessário, risco e teste correspondente. Complementada pelas 57 FKs. Inclui campos de perfil/experiência/integrantes, dados do menor, storage e vínculos polimórficos. Original preservado; [complemento da matriz](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/evidencias/rf22-2026-10-04/matriz-complemento-vagas-e-evidencia.md) registra o refinamento posterior para status NULL, dependências adicionais do rascunho e interação sob moderação, antes da validação final.

| Ação | Aplicação |
|---|---|
| DELETAR | Usuário por último; perfis/identificadores; responsável; refresh; taxonomia/experiência; autodeclarações; portfolio/embeds/agenda/gamificação; salvos/visualizações/membership/interações; URLs pessoais; rascunho sem histórico |
| ANONIMIZAR | Candidatura para artista reservado oficial e apresentação/link NULL; mensagem com remetente NULL; denunciante/denunciado/autor de processo para ID 0; moderador NULL; contratante de publicada/cancelador para ID 0 |
| RETER necessário | Tentativas/IDs/datas/status, vagas publicadas em estado final, cancelamentos sem motivo pessoal, chat remanescente, evidência previamente reportada restrita e comprovante sem PII |
| RECUSAR antes de escrever | Google-only sem confirmação própria; autoria comunidade/edital; portfólio/galeria com evidência que perderia estrutura/isolamento; rascunho com histórico; mídia local desconhecida; estrutura reservada inválida |

## 9. Contrato API

**DELETE /api/usuarios/me**, JWT Bearer atual obrigatório. Corpo: `senhaAtual`, `motivo` opcional até 500 caracteres. IDs/flags extras não determinam o alvo. ID vem do JWT verificado e email do Principal; o serviço fixa ambos no SELECT da conta, impedindo que reutilização de email escolha outra identidade.

Resposta 200: somente `comprovanteHash`, `dataExclusao`, `status` (`CONCLUIDA` ou `CONCLUIDA_COM_LIMPEZA_PENDENTE`). Não inclui usuário, motivo, email, nome, CPF/CNPJ, IP, credenciais ou detalhes SQL. Motivo aceito por compatibilidade, mas não copiado para log permanente. Não existe consulta posterior que dependa da conta apagada.

401 para JWT/conta inválidos ou repetição após exclusão; 400 para corpo/senha ausentes/inválidos; 403 senha incorreta; 422 cenário recusado; 409 falha transacional com mensagem genérica. `DELETE /api/usuarios/{id}` e hard deletes genéricos de perfis continuam negados, inclusive ADMIN. Métodos legados `UsuarioService.deletar*` continuam contidos.

## 10. Confirmação forte e Google

Senha local comprovada por **BCryptPasswordEncoder.matches**, sem comparar hashes manualmente ou impor política de senha nova à confirmação. DTO não gera toString com senha. Senha errada/ausente não altera nenhum domínio nem comprovante.

Conta Google com senha local pode confirmá-la. **Google-only retorna 422**, mesmo com JWT, `confirmado=true` ou token de outra finalidade. Infraestrutura atual de Google/cadastro/email/RF09/consentimento não dispõe de confirmação RF22 específica. Nenhuma senha fictícia ou código inseguro criado. Gap funcional real mantém RF22 parcial.

## 11. Bloqueio, JWT e sessão atual

Após validar confirmação, políticas e plano de arquivos, BLOQUEADA é gravada antes da anonimização/limpeza e eliminada no mesmo commit da conta. Filtros JWT reconsultam ID/email e acesso persistido; após commit, JWT antigo não autentica. Conta reservada não autentica.

**Limite MVCC:** a atualização não é visível globalmente antes do commit; leitura/requisição já em curso pode observar a versão anterior. Não foi criado bloqueio global de leitores públicos nem prometida revogação instantânea entre todas as instâncias. Commit único e rollback completo foram priorizados; este critério de simultaneidade estrita permanece pendente de desenho específico. Não existe estado intermediário comprometido.

## 12. Refresh, login e realtime

Todos os refresh tokens são apagados explicitamente; SET NULL isolado deixaria registros e não cumpriria RF22. Tokens de terceiros permanecem. Remember-me usa esses registros; não há outro armazenamento persistente de sessão auditado.

Após exclusão real: refresh, login convencional, API privada e novo CONNECT WebSocket falham; SUBSCRIBE/SEND da sessão antiga são revalidados e negados. Gates de entrega realtime existentes reconsultam usuário/estado. Não foi emitida notificação de exclusão a terceiros nem criado canal RF36 novo.

## 13. Concorrência e repetição

PostgreSQL `pg_advisory_xact_lock` por `rf22:{subjectId}` e FOR UPDATE da conta. Perfis, vagas, arquivos e mensagens próprios bloqueados antes do planejamento; FK/locks coordenam os vínculos relacionais correspondentes. Não depende de synchronized Java. A prova de concorrência cobre duas exclusões da mesma conta; não homologa todos os interleavings com novas escritas em alvos polimórficos sem FK.

Duas requisições simultâneas: uma 200, outra 401 sem novo log; repetição com JWT antigo também 401. Exatamente um resultado lógico/comprovante, sem 500. Comprovante não guarda vínculo pessoal para permitir replay autenticado após apagar a conta.

## 14. Transação e rollback

TransactionTemplate sobre o transaction manager vigente; somente DML no schema existente, sem delete JPA genérico. Referências descartáveis são limpas ainda com os pais presentes, depois históricos preparados, relações/perfis removidos e usuário apagado por último. Comprovante pertence à mesma transação.

Injeções de falha antes da anonimização, no meio da limpeza e no delete final: 409 controlado, snapshot completo das 43 tabelas idêntico ao anterior, conta/perfil/refresh/históricos preservados e arquivos ainda presentes. Não existe conta permanentemente bloqueada ou comprovante falso após rollback.

## 15. Dados pessoais, menor e perfil

Perfis, CPF/CNPJ, nascimento/telefone/email/username, raio/disponibilidade, experiência por área, integrantes, banners/URLs, responsável/contactos/consentimento, autodeclarações e exposição são eliminados com suas linhas. Responsável e refresh removidos explicitamente, pois seus SET NULL não apagariam PII.

RF10 não retorna o perfil apagado; RF37 busca por username devolve zero; token antigo não recupera dados privados. Arquivos de perfil/portfolio deixam de ser servidos pelo contrato normal. Avatar/banner atuais são URLs de provedor externo; referência é eliminada, sem tentar apagar conteúdo remoto. Caminho local desconhecido provoca recusa antecipada.

## 16. Taxonomia, catálogo e gamificação

Associações área/função/especialização e experiência do titular removidas explicitamente; catálogos oficiais compartilhados permanecem. Sem funções improvisadas ou associação órfã. Agenda, conquistas, medalhas e ranking eliminados; não se inventou retenção dessas classificações pessoais.

## 17. Banco de Talentos, salvos e notificações

Banco RF13 eliminado nos dois lados do par, membership de comunidade do titular removido. Salvos próprios e de terceiros apontando para perfil/obra/rascunho apagado removidos antes dos pais. Itens/interações de galerias envolvendo obras eliminadas são tratados inclusive em galeria de terceiro, preservando o contêiner desse terceiro.

Notificações do titular e contextos canônicos de perfil, arquivos, salas afetadas e vagas próprias removidos. CONVITE RF42 deixa de representar interação com contratante apagado; nenhum convite/candidatura novo criado. Links arbitrários legados e PII livre sem vínculo estruturado não são detectados por análise semântica; limite de limpeza do modelo atual.

## 18. Candidaturas RF06/RF45

Reatribuição ao perfil artista ID 0 **já autorizado pelo seed** antes de apagar o artista. Apresentação e link pessoal NULL; IDs, status, timestamps e todas as tentativas preservados. Não há UNIQUE global inventado. Vaga/terceiros e regras de usuários ativos permanecem.

RF45 filtra ID 0 antes de página/count nas duas consultas; histórico anônimo não vira candidato operacional convidável. Não se tentou preservar identidade recuperável por hash de usuário/email/CPF. As regressões RF06 de candidatura/retirada/recandidatura permanecem obrigatórias; anonimização não equivale a oferecer recandidatura à conta inexistente.

## 19. Vagas RF23 e cancelamento

RASCUNHO sem candidatura, log de cancelamento, reporte ou moderação pode ser fisicamente removido; rascunho com qualquer desses vínculos é recusado. Status NULL também é recusado: o campo é nullable e esse estado não pode cair silenciosamente no cascade. Publicadas permanecem, sem cascade de candidaturas de terceiros: owner reservado, ENCERRADA; CANCELADA conserva seu estado final. Título/descrição/requisitos/tipo de contrato neutros; endereço/benefícios/experiência nulos; fotos pessoais removidas. Classificação profissional, remuneração e localização agregada da vaga histórica conservadas.

Registro de cancelamento conserva ID/vaga/data, cancelador ID 0 e motivo NULL quando o titular. Acesso histórico da vaga continua submetido ao RF05 vigente: somente participante/proprietário para estados fora do feed. Não foi tornado público o histórico fechado nem aberto endpoint de reabertura para ID 0.

## 20. Chat real RF35

Exclusão real RF22 pela API, sem DELETE SQL de usuário no cenário de integração: participação do titular removida, mensagem necessária com remetente NULL, sala remanescente acessível, “Usuário Removido”, ID/avatar NULL, sem email/username na DTO. Histórico/datas/IDs necessários permanecem; envio novo sem outro participante é recusado.

Sala afetada sem participante e sem reporte/moderação é removida, incluindo mensagem/arquivo que não precisa mais servir a alguém. Salas alheias não são selecionadas para exclusão. Texto necessário não reportado pode permanecer; texto_original desnecessário eliminado. Texto livre pode conter PII — não há prova de anonimização semântica absoluta.

## 21. Anexos e evidência reportada

Anexo pessoal não reportado perde URL/metadata de acesso e arquivo é limpo AFTER_COMMIT. Mensagem contendo apenas anexo recebe texto neutro para satisfazer CHECK oficial, sem nova coluna. Mensagens reportadas/moderadas previamente preservam texto/evidência privados, com remetente NULL, excluida=true/data; DTO/lista usa placeholder e download comum retorna 404.

Arquivo reportado permanece em storage privado existente, sem acesso por static/URL pública. Não foi inventado módulo de moderação nem declarado prazo/base jurídica homologados. Portfólio/galeria cuja evidência perderia estrutura/isolamento é recusado antes de alterar. Evidência em texto livre sem referência estruturada exige política/revisão específica.

## 22. Denúncias e moderação

Denunciante e denunciado de plágio anonimizados usando ID 0 oficial, sem destruir processo de terceiro. Reportes conservam alvo/ID/data com denunciante reservado; autor de moderação reservado e moderador NULL (NO ACTION exigia tratamento explícito). Conteúdo de evidência necessário permanece restrito às APIs vigentes do processo, sem manter a conta apagada.

Não há varredura semântica de nomes/PII nos textos/provas nem homologação jurídica da retenção. Uma denúncia sobre obra/galeria que seria apagada pode bloquear esse cenário. Sem banco modificado ou interpretação de ausência de FK polimórfica como autorização para apagar prova.

## 23. Comunidades, editais e domínios sem política

Membro simples é eliminado. Titular criador de comunidade ou publicador de edital recebe 422, sem alterações: RF22 não define o destino do conteúdo/arquivos/autoria/terceiros. SET NULL é fisicamente possível, mas não decide se conteúdo pessoal deve permanecer. Nenhuma retenção ampla foi inventada por conveniência.

Retificações/editais/comunidade de terceiros não são apagados pelo simples membership. Galerias próprias sem evidência podem ser eliminadas; com reporte/moderação, recusa segura. Esses cenários não contam como RF22 concluído.

## 24. Storage e filesystem

Storages reais auditados: `app.portfolio.storage-root` (default `./storage/portfolio`) e `app.chat.storage-root` (`./storage/chat`). Referências servidor UUID/dono ou UUID/sala; normalização, prefixo, raiz e symlinks verificados antes de escrever no banco. Nenhum path do corpo da exclusão aceito. URLs externas eliminadas como referências, sem leitura/download/delete remoto.

Plano de arquivos construído antes da limpeza relacional. Remoção só AFTER_COMMIT; rollback nunca apaga arquivo. Falha física não mente sobre rollback: SQL continua concluído, comprovante existe e resposta sinaliza `CONCLUIDA_COM_LIMPEZA_PENDENTE`. Log técnico registra apenas classe da falha.

**Limitação:** não há outbox/fila durável de reconciliação no pacote atual. Crash entre commit e cleanup ou falha operacional pode deixar arquivo privado órfão; reconciliação com volume/backup operacional é necessária. Não há atomicidade distribuída. Esse risco não é mascarado por status SQL nem por teste verde.

## 25. Comprovante LGPD

Tabela oficial sem FK usuário suporta hash char(64) e data. Hash SHA-256 de 32 bytes SecureRandom próprios da operação, independente de PII; não hash previsível de CPF/email. Motivo persistido NULL; nenhuma identidade/credential/IP no log. Comprovante retornado somente no response final.

Exatamente um log por exclusão concorrente bem-sucedida; falha transacional não deixa log. Requisição recusada não deixa log. Não foi criada ligação pessoal para recuperar recibo depois via conta inexistente.

## 26. Segurança e limites de privacidade

Credenciais não são lançadas em log; DTO não possui toString sensível. SQL parametrizado; nomes de tabelas internos fixos. Erros de orquestração devolvem mensagem genérica, sem stacktrace/SQL/causas/dados de terceiros. Cleanup físico não registra paths/nomes/conteúdo.

Textos de mensagens/evidências e links legados podem conter PII fornecida antes pelo usuário. Retirar identificação estrutural/DTO não prova ausência absoluta de PII nesses textos ou backups externos. Política jurídica/prazos e proteção de volumes permanecem pendências explícitas.

Verificação local das evidências compara valores de segredos efetivamente carregados do ambiente (com pelo menos oito caracteres) e padrão de JWT assinado, sem imprimir esses valores. `evidencias-privacidade.json` registra a quantidade conferida e os resultados; zero ocorrências na verificação executada. Esse controle não é scan amplo de secrets/PII nem declaração de anonimização absoluta. Propriedades Surefire com informações de ambiente foram retiradas das cópias documentais; casos, resultados e saídas dos testes foram conservados.

## 27. Arquivos de produção

Dez Java: novos `ExclusaoContaService`, `ExclusaoContaRequest`, `ExclusaoContaResponse`; alterados `UsuarioController`, `SecurityConfig`, `GoogleAccountAccessPolicy`, `MenorAutorizadoPolicy`, `CandidaturaRepository`, `PortfolioStorageService`, `ChatAnexoStorage`. Sem alteração de entidades, schema, bootstrap, regras RF06 de tentativas ou serviços de notificações.

## 28. Testes novos e ajustes existentes

Nova `ExclusaoContaRf22IntegrationTest`, **49 casos**, PostgreSQL/Testcontainers/database05 real. Titularidade/IDs extras/admin, confirmação senha/Google, sessões/JWT/refresh/login/WS real e sessão antiga, menor/taxonomia/dados, membership/salvos/galeria, todos os sete status de candidatura, quatro estados publicados, rascunhos, denúncias/NO ACTION, API RF22→chat anônimo/anexos/evidência, recusa segura, seed reservado, rollback três etapas, after-commit/erro físico, concorrência, validate/catálogo/57 FKs sem órfãos. Cinco casos de refinamento comprovam recusa de status NULL, rascunho com log/reporte/moderação e interação própria em galeria de terceiro moderada.

`GenericEndpointsSecurityIntegrationTest` e `UsuarioControllerIT` ajustam apenas a expectativa da rota /me sem corpo (400), conservando proibição 403 das rotas genéricas e assertions de preservação. Nenhum caso/skip/assertion funcional removido. `UsuarioServiceDeletionContainmentTest` permanece intacto.

Diagnóstico 1: compilou, Docker engine desligado; 1 erro de infraestrutura antes dos cenários, 26,802 s. Docker existente iniciado, sem alterações de segurança. Diagnóstico 2: 44 total/32 passed/12 failures/0 errors/0 skipped, 01:06 min; nova suíte usou rota/nome de campo errados e tentou acesso anônimo a vaga fechada. Corrigidos para contratos reais; reforçada ordem das tentativas para exercitar candidato reservado ativo. Logs/resultado anteriores preservados, sem atribuir esses erros ao banco.

## 29. Focados

**747 total / 747 passed / 0 failures / 0 errors / 0 skipped, 29 classes distintas, consolidação final pela última execução de cada classe.** Rodadas iniciais complementares, ambas BUILD SUCCESS/exit 0: 691/691 em 05:57 min (27 classes; término 12:46:05 -03:00) e 51/51 em 54,026 s (DenunciaRf14Rf18IntegrationTest e VagaCancelamentoRf25IntegrationTest; término 12:51:26 -03:00). Antes do refinamento, a soma era 742/742. Reforço posterior: 100/100 em 01:28 min, BUILD SUCCESS/exit 0, término 13:05:01 -03:00, nas classes RF22 (49), denúncia (21) e cancelamento (30). A consolidação substitui as versões anteriores dessas classes; não soma os 100 novamente. Evidências focados-reforco* e focados-finais-consolidados.json. A seleção inicial continha nomes inexistentes RememberMeSessionIntegrationTest/DenunciaIntegrationTest; não produziram casos. Remember-me/refresh/logout estão efetivamente cobertos em AuthControllerRf01Rf02IntegrationTest; denúncia foi completada pelo nome real na segunda rodada. Evidências focados*.log/.exit/-totais.json/-reports/ e focados-consolidados.json. Todas as fontes da bateria preservadas para a regressão completa.

## 30. Maven completo

Regressão **final**, sobre as fontes atuais após o reforço de cinco cenários: **1342 total / 1325 passed / 0 failures / 0 errors / 17 skipped**, 66 classes, **BUILD SUCCESS**, exit 0, 09:57 min; término **04/10/2026 13:16:06 -03:00**. Comando `./mvnw.cmd test '-Dspring.test.mockmvc.print=NONE'`, sem clean. Totais extraídos dos XMLs Surefire desta execução, sem somar arquivos antigos. Evidências `maven-completo-final.log`, `maven-completo-final.exit`, `maven-completo-final-totais.json` e `maven-completo-final-reports/`.

Os 17 skips são condicionais históricos: CurrentLocalSchemaIntegrationTest (4) e OfficialLocalApiIntegrationTest (13). Nenhum skip novo. A primeira regressão completa também ficou verde (**1337 total / 1320 passed / 0 failures / 0 errors / 17 skipped**, 09:31 min, término 13:02:06 -03:00), mas antecede o reforço e não substitui o resultado final. Seus artefatos `maven-completo*` foram preservados. PostgreSQL/Testcontainers carregou somente database05; fixtures DML descartáveis, `ddl-auto=validate` e catálogo/FKs reais. As 348 fontes Java da versão final foram registradas em `fontes-testadas.json` para a conferência posterior.

## 31. Semgrep Docker

**Semgrep 1.178.0, p/java, 276 arquivos Java de produção / 60 regras / ~100,0% das linhas parseadas / 0 findings / 0 blocking / 0 errors / exit 0**, execução concluída com sucesso em 467,0 s. Todos os dez arquivos de produção alterados/criados cobertos; nenhum ausente. `--no-git-ignore` inclui os novos Java ainda não rastreados, sem staging. Evidências `semgrep-java.json`, `semgrep-java-saida.log`, `semgrep-java-resumo.json`, `semgrep-java-execucao.json` e `semgrep-java.exit`.

Imagem Docker oficial `semgrep/semgrep@sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b`; montagem somente leitura e `--network none`. Ruleset oficial p/java previamente baixado pelo Windows com TLS verificado, cache auditado SHA-256 `5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1`, montado localmente. A verificação TLS não foi desabilitada. Workflow Semgrep neste ambiente usa Docker; a instalação nativa Windows não deve ser usada devido ao bloqueio do Windows App Control. Nenhum código do Palco foi alterado para corrigir a ferramenta. Este scan é Java; não se declara scan amplo de secrets nesta tarefa.

0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.

## 32. Graphify final

`graphify update .` **concluído, exit 0, 46,1 s**. MCP HTTP existente em `http://127.0.0.1:8765/mcp` confirmou **6153 nós / 21049 arestas / 349 comunidades**, 91% EXTRACTED, 9% INFERRED, 0% AMBIGUOUS. Consulta do nó ExclusaoContaService aponta para `backend/src/main/java/com/portifolio/service/ExclusaoContaService.java`, linha 29, degree 22. Evidências `graphify-update.log/.exit`, `graphify-update-execucao.json`, `graphify-mcp-final-initialize.txt`, `graphify-mcp-final-902.txt`, `graphify-final-resumo.json` e `graphify-final-servico.json`.

AST atualizado; não foi executado `graphify label` nem launcher `graphify-mcp.exe`. Avisos preservados: 45 SQLs sem extração porque tree_sitter_sql não está instalado, e comunidades alteradas com rótulos semânticos anteriores. Não se instalaram dependências nem se contornou Windows App Control. O grafo atualizado não substitui a auditoria direta do database05, testes e matriz, nem prova retenção/anonimização jurídica.

## 33. Preservação banco/frontend/fontes

**Banco alterado: NÃO. Frontend alterado nesta tarefa: NÃO.** Comparação final byte a byte com `C:/Users/masca/Downloads/palco-database05.zip`, SHA-256 `6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf`: **46/46 idênticos, 0 divergentes, 0 ausentes, 0 adicionais**. Nenhuma edição de SQL/database05/database04/bootstrap/scripts/configuração de schema. DML somente nas fixtures de PostgreSQL descartável; banco de desenvolvimento não modificado.

Manifesto protegido: **1411 arquivos, zero alterações**, incluindo **770 arquivos de frontend**, **47 arquivos database04 e seus 5 scripts**, e 266 Java de produção fora do escopo. As alterações anteriores de README/database histórico/frontend permanecem iguais ao baseline. **348 fontes Java finais, zero divergências após os testes**, também após Semgrep/Graphify. HEAD/upstream continuam ae52249e1f3a98a34dc5c2129d35b465f6b974da, branch preservada e index vazio; nenhum delta inesperado entre arquivos existentes ou novos Java.

Evidências: `snapshot-final-byte-a-byte.json`, `preservacao-final.json`, `delta-final.json`, `fontes-testadas-final.json`, `escopo-final.json`. Conferência independente da matriz: 43/43 tabelas do DDL, sem omissões/adicionais (`matriz-cobertura-final.json`). Os arquivos novos não aparecem no diff de arquivos rastreados; são verificados separadamente e não foram adicionados ao index.

## 34. Critérios bloqueados e alternativas

| Critério | Alternativa auditada / motivo da recusa ou limite | Próxima decisão mínima |
|---|---|---|
| Google-only | JWT/flag não é confirmação adicional; RF09/RF26/contexto de cadastro têm outra finalidade | Mecanismo específico de confirmação forte com finalidade/uso único/expiração, antes de liberar |
| Autoria comunidade/edital | SET NULL suportado, mas não resolve conteúdo público/arquivos/terceiros; deletar tudo seria indevido | Política por domínio; aplicar backend-only quando suficiente |
| Portfólio/galeria disputado | CASCADE destruiria referência/evidência; manter perfil/metadata pessoal seria retenção ampla | Política/armazenamento restrito com vínculo e prazo; pacote completo futuro se nova estrutura necessária |
| Seed reservado ausente | NOT NULL/CASCADE impede anonimização; não criar fake ou reconstruir seed | Restaurar ambiente somente por pacote oficial completo autorizado, sem patch desta tarefa |
| Estado NULL/rascunho com dependências | Física permitida RF23 não autoriza perder candidatura/log/reporte/moderação | Decisão para dado legado; nenhum cascade cego |
| Revogação pública em execução | MVCC não expõe bloqueio não commitado; commit separado quebraria rollback completo | Coordenação de leitores/acesso durante processamento e múltiplas instâncias |
| Cleanup durável | Callback resolve operação normal, sem fila após crash | Reconciliação operacional; outbox/registro de cleanup exigiria snapshot futuro completo |
| Texto/retenção jurídica | Anonimização estrutural não prova texto livre sem PII ou prazo/base homologados | Política específica e revisão autorizada; não prometer conformidade jurídica definitiva |

Nenhum SQL/migration foi aplicado para resolver pendências. Se for necessária mudança estrutural, solicitar **novo pacote completo da Manuela**, preservando proveniência e validação, em tarefa própria. Não propor como “necessário” tornar FKs nullable quando o ID 0 oficial já atende o cenário suportado; nullable/SET NULL seria uma alternativa de desenho futuro, não correção local autorizada.

## 35. RF22 e RF35 reavaliados

**RF22 continua PARCIAL.** Contrato completo não é atendido para todos os usuários/domínios e critérios de simultaneidade/retenção. Os cenários seguros implementados são entrega concreta; recusas/limitações não são contadas como conclusão global.

**RF35:** a dependência “exclusão real RF22 pela API → histórico remanescente anônimo” foi removida para os cenários suportados, com teste integrado novo. Não concluir RF35 automaticamente: exclusão universal, política de retenção/evidência e demais limites próprios registrados no relatório histórico continuam. RF35 global permanece PARCIAL; relatório histórico preservado.

## 36. Próximo passo

Revisar o delta local/evidências sem staging global. Definir confirmação Google-only, política de autoria/evidência/retenção, coordenação de acesso durante processamento e reconciliação física. Integrar modal/contrato ao frontend em tarefa futura autorizada. Nenhum commit/push/staging realizado.

## Registro semanal — 04/10/2026

| Campo | Registro |
|---|---|
| Objetivo/RF/RNF | Implementar/auditar RF22; RF35/06/18/23/27/36; RNF06/07/08/09/10/17/19 |
| Backend alterado | SIM: dez Java de produção e três de teste; orquestração/DTO/segurança/storage/regressões |
| Frontend alterado | NÃO |
| Banco alterado | NÃO; apenas DML de fixtures em PostgreSQL descartável |
| Database05 | Única fonte ativa, validate; 46/46 idênticos ao ZIP, zero divergentes/ausentes/adicionais |
| Confirmação | Senha BCrypt; Google-only recusado até confirmação própria |
| Sessões | Refresh apagado, JWT/login/WS antigos sem acesso após commit |
| Deletado | Conta/perfis/PII/menor/classificações/arquivos pessoais/relações sem retenção |
| Anonimizado | Candidaturas/processos/vagas para seed ID 0; chat remetente NULL |
| Retido | Histórico necessário/evidência restrita/comprovante não identificável |
| Chat/RF35 | API real → histórico anônimo; anexo comum removido/reportado restrito; RF35 global PARCIAL |
| Candidaturas | Tentativas/IDs/datas/status preservados, apresentação/link removidos; ID 0 fora de candidatos |
| Vagas | Publicadas mantidas finais/anônimas, CANCELADA preservada, rascunho seguro eliminado |
| Denúncias | Processo preservado, atores anonimizados; obra/evidência sem isolamento adequado recusada |
| Arquivos | AFTER_COMMIT; falha sinaliza cleanup pendente, sem alegar transação distribuída |
| Log/comprovante | Hash aleatório SHA-256 e data, motivo NULL, sem PII |
| Rollback | Três falhas reais, snapshot integral e arquivos preservados |
| Testes | Focados finais 747/747; Maven 1342 total, 1325 passed, 0 failures, 0 errors, 17 skips históricos; BUILD SUCCESS |
| Semgrep | Docker oficial 1.178.0, p/java, 276 arquivos, 60 regras, 0 findings/errors, exit 0 |
| Graphify | update AST exit 0; MCP HTTP confirmou 6153 nós, 21049 arestas, 349 comunidades |
| RF22 final | PARCIAL |
| RF35 reavaliado | Dependência da exclusão real removida nos cenários suportados; global permanece PARCIAL |
| Pendências/próximo passo | Google-only, domínios/evidência/retenção, acesso durante execução, reconciliação; seção 34 |

## Inspeção final

Executados os quatro comandos solicitados, todos exit 0: `git status --short`, `git diff --stat`, `git diff --check`, `git diff --name-status -- backend`. **git diff --check: exit 0, nenhum erro de whitespace; houve apenas avisos da configuração vigente sobre conversão futura LF para CRLF.** Configuração/arquivos não foram alterados para silenciar esses avisos. Logs integrais git-status-final.txt, git-diff-stat-final.txt, git-diff-check-final.txt e git-diff-backend-final.txt; códigos em git-inspecao-final.json. O diff backend contém os nove Java existentes previstos. Quatro Java novos, relatório e evidências continuam não rastreados, sem staging; os cinco arquivos novos de código/relatório foram conferidos separadamente sem whitespace. Deltas anteriores de README/database histórico/frontend preservados. HEAD/upstream e index verificados em escopo-final.json: sem staging, commit ou push.

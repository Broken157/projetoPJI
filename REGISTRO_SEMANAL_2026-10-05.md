# Registro semanal — 05/10/2026

- Data: 05/10/2026.
- Objetivo: auditar/concluir RF11 — Painel Principal.
- RF/RNF: RF02/03/06/08/11/13/17/19/23/28/35/36/42/45/27/37; RNF02/05/06/07/08/09/10/13/17, conforme limites do relatório.
- Backend alterado: SIM; 11 Java de produção e dois testes, incluindo três Java novos.
- Frontend alterado: NÃO; 770 arquivos e deltas anteriores protegidos.
- Banco alterado: NÃO; nenhum SQL/schema/índice/enum/entity/script operacional/dependência; fixtures em PostgreSQL descartável e validate.
- Database05: 46/46 idênticos ao ZIP oficial, zero divergentes/ausentes/adicionais; database04 histórico intacto.
- Dashboard artista: identidade mínima, candidaturas próprias limitadas e contadores reais.
- Perfil incompleto: flag persistida RF08 sinalizada sem bloquear painel ou autorizar candidatura RF06.
- Recomendações: único matching consolidado no serviço de vagas RF03; área → funções → especializações, prazo/moderação, publicação/ID, explicação real e sem engajamento.
- Candidaturas: próprias com status real; contratante usa vigência RF45 sem retiradas antigas/duplicação.
- Dashboard contratante: vagas próprias, resumo por estados, candidatos, mensagens, notificações e Banco privado.
- Vagas próprias: projeções/count SQL; prazoVencido não muda status no GET.
- Candidatos: vagas próprias ABERTA/PAUSADA e candidatura atual; DTO mínimo seguro.
- Mensagens: COUNT RF35 apenas recebidas em salas próprias; sem marcar leitura ou carregar histórico/anexo.
- Notificações: COUNT RF36 do destinatário, sem criar evento/marcar leitura.
- Banco de Talentos: count de memberships próprio e preview RF17; GET não cria/remove vínculo ou sala.
- Privacidade: menor somente autorizado/ATIVA; sem responsável/experiência privada/PII/evidência de chat.
- Performance: preview máximo 5; consultas não crescem de 1→5. JPA artista 10/10 e contratante 15/14; spy JDBC 16/16, incluindo sobrecargas. Carga/p95/p99 e cobertura percentual não medidos.
- Testes: focados 836/836; completo 1493 total/1476 passed/0 failures/0 errors/17 skips históricos idênticos; BUILD SUCCESS. RF11 43/43. Diagnósticos anteriores preservados.
- Semgrep: Docker 1.178.0, p/java, 360 arquivos/60 regras/~100% parse/0 findings/0 blocking/0 errors/exit 0; não comprova sozinho autorização ou ausência de segredos.
- Graphify: AST update exit 0; MCP HTTP 6365 nós/21886 arestas/343 comunidades, 91% EXTRACTED/9% INFERRED/0% AMBIGUOUS; DashboardService atualizado reconhecido, sem label/contorno de segurança.
- RF11 final: backend CONCLUÍDO; integração/homologação visual pendente.
- Pendências: interface dos novos campos; RF17 parcial e bloqueios RF16/RF18 preservados.
- Próximo passo: revisão e checkpoint controlado pelo usuário; sem staging/commit/push nesta tarefa.

Relatório: RELATORIO_RF11_DASHBOARD_2026-10-05.md.


## RF44 — Avisos ao Responsável Legal

- Data: 05/10/2026.
- Objetivo: auditar/implementar RF44.
- RF/RNF: RF01/02/06/10/16/18/27/35/36/40/44; RNF06/07/08/09/10.
- Backend alterado: SIM; seis Java de produção e cinco testes, no escopo RF44.
- Frontend alterado: NÃO.
- Banco alterado: NÃO.
- Database05: 46/46 idênticos; database04 e históricos preservados.
- Menor autorizado: policy RF27 existente reutilizada antes da candidatura e no processamento.
- Candidatura: CONCLUÍDA; ação persistida, destinatário correto, concorrência/recandidatura protegidas.
- Publicação: CONDICIONADA/BLOQUEADA; sem produtor real RF16/RF40.
- AFTER_COMMIT: callback agenda executor próprio; leitura readOnly encerrada antes do SMTP.
- Email: sender/JavaMailSender existentes; corpo mínimo e link público apenas com base HTTPS segura.
- Falha/retry: falha isolada/logada; sem retry durável; repetição somente do envio não recria ação.
- Privacidade: sem contatos/consentimento no evento/HTTP/RF36/logs; dados mínimos no email.
- Logs: registro técnico observacional, correlacionado por candidatura/resultado/categoria; sem tabela nova.
- Testes focados: **578 total / 578 passed / 0 failures / 0 errors / 0 skipped; BUILD SUCCESS**. Classes: 24. [INFO] Total time:  04:31 min.
- Maven completo: **1560 total / 1543 passed / 0 failures / 0 errors / 17 skipped; BUILD SUCCESS**. Classes: 73. [INFO] Total time:  09:46 min. Mesmos 17 skips históricos.
- Semgrep: Versão 1.178.0; 366 arquivos; 60 regras; parse ~100.0%; findings 0; blocking 0; errors 0; exit 0; 50.4 s. Docker p/java, incluindo untracked, TLS preservado; nenhum secrets scan amplo.
- Graphify: Exit 0; 27.6 s; **6453 nós / 22287 arestas / 359 comunidades**; EXTRACTED 91%, INFERRED 9%, AMBIGUOUS 0%. MCP HTTP confirmado, AST-only, nenhum contorno de segurança.
- RF44 final: PARCIAL global; candidatura CONCLUÍDA; publicação depende de produtor real.
- Pendências: pacote/produtor RF16/RF40, SMTP real e eventual política durável em tarefa própria.
- Próximo passo: integrar somente publicação real quando existir; não antecipar banco/frontend.
- Git: diff check exit 0/saída vazia, índice vazio; HEAD preservado; sem staging/commit/push.
- Relatório: RELATORIO_RF44_AVISOS_RESPONSAVEL_2026-10-05.md.


## RF39 — Certificados Profissionais

- Data: 05/10/2026.
- Objetivo: auditar/implementar RF39 somente se estruturalmente suportado.
- RF/RNF: RF10/16/18/22/27/30/38/39/44; RNF05/06/07/08/09/10/17.
- Backend alterado: NÃO; Java de produção e testes preservados.
- Frontend alterado: NÃO.
- Banco alterado: NÃO.
- Database05: 46/46 idênticos byte a byte ao ZIP oficial, zero divergentes/ausentes/adicionais; database04 intacto.
- Persistência certificado: AUSENTE; somente arquivo genérico.
- Título: AUSENTE; nome_original não é título profissional.
- Instituição: AUSENTE.
- Data: AUSENTE; data_upload não é data profissional. Emissão/conclusão/futuro pendentes.
- Arquivos: genéricos PDF até 10 MiB e JPG/JPEG/PNG até 5 MiB; MP3 não atende RF39.
- Múltiplos certificados: NÃO representados; 1:N arquivos genéricos não constitui RF39.
- Ownership: JWT/proprietário genérico regredido, sem recurso certificado.
- Perfil público: sem bloco/lista RF39 real.
- Menor: política específica de documento binário público pendente; nenhum OCR/novo consentimento/mudança do portfólio.
- RF18: PARCIAL; sem alvo CERTIFICADO/ARQUIVO; guard de galeria cobre apenas vínculo real.
- RF22: regressão existente preservada; integração futura de vínculo/cleanup/evidência necessária.
- RF16/RF38: projetos e currículo principal permanecem separados e bloqueados por estrutura.
- RF44: não ampliado para eventos de certificado.
- Testes focados: **291 total / 291 passed / 0 failures / 0 errors / 0 skipped; BUILD SUCCESS**. 14 classes; duração 02:34 min; exit 0.
- Maven completo: **1560 total / 1543 passed / 0 failures / 0 errors / 17 skipped; BUILD SUCCESS**. 73 classes; duração 10:29 min; exit 0. Mesmos 17 skips históricos nominalmente comparados.
- Semgrep: não executado nesta tarefa porque não houve delta Java de produção.
- Graphify: MCP HTTP 6453 nós/22287 arestas/359 comunidades, 91% EXTRACTED/9% INFERRED/0% AMBIGUOUS; hash intacto/código conferido, sem update necessário.
- RF39 final: BLOQUEADO POR ESTRUTURA.
- Blockers: identidade/metadados/data/vínculo comprobatório/cardinalidade profissional ausentes.
- Pendências: pacote oficial completo, Data/futuro, privacidade do menor, alvo/evidência RF18 e lifecycle RF22.
- Próximo passo: decisões funcionais/schema oficial e reauditoria antes de implementar; nenhuma migration/SQL aplicada.
- Git: diff check exit 0/saída vazia; índice vazio, HEAD preservado; sem staging/commit/push.
- Relatório: RELATORIO_RF39_CERTIFICADOS_PROFISSIONAIS_2026-10-05.md.

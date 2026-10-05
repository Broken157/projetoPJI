# RF38 — Currículo Profissional — 05/10/2026

**RF38 BLOQUEADO POR ESTRUTURA. Produção e testes Java não alterados; banco e frontend preservados.** Auditoria concluída com regressões do comportamento existente. Publicidade do currículo de menor depende também de decisão funcional.

## 1. Objetivo

Auditar RF38 e implementar somente com persistência inequívoca. **RF38 BLOQUEADO POR ESTRUTURA**: database05 não representa currículo principal. Nenhuma implementação parcial foi criada sobre PDF genérico. Auditoria e regressões verificam o sistema atual; não entregam o recurso ausente.

## 2. Requisitos

Fonte oficial atual: C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt. Leitura integral de RF10/16/18/22/27/30/38/39/44 e RNF05/06/07/08/09/10/17; extrato e SHA-256 em requisitos-consultados.txt/requisitos-origem.json. RF38 exige PDF principal único, proprietário autenticado, data de atualização, substituição/exclusão e exposição pública compatível com privacidade. A decisão consolidada proíbe inferir currículo de PDF genérico.

Consultados relatórios RF16 por projetos, portfólio individual, RF18, RF22, RF44, sincronização database05 e auditoria pós-decisões, com origem/hash/extratos preservados. Não foi encontrado relatório backend específico RF10; consultados relatórios históricos de perfil e revalidados PerfilPublicoService/DTO/testes atuais. Históricos não sobrepõem RF revisado ou schema ativo.

## 3. HEAD/checkpoint

HEAD inicial/final **40d8a6ebff77e7408b846473da20c66e8fde682d**, branch integracao-recuperada-2026-09-15; commit **40d8a6e feat: avanca RF44 avisos ao responsavel** presente. Upstream e fork iguais ao HEAD; divergência **0/0** após fetch. Primeiro fetch falhou por schannel no sandbox; repetição autorizada passou sem alteração TLS/Windows. Índice vazio e nenhum delta RF44 solto em backend/database05/database04/scripts. Working tree histórico sujo preservado: deltas preexistentes não são atribuídos ao RF38. Manifesto inicial de 1449 arquivos.

## 4. Graphify inicial

**6453 nós / 22287 arestas / 359 comunidades**, EXTRACTED 91%, INFERRED 9%, AMBIGUOUS 0%. MCP HTTP confirmado por initialize/tools/call em http://127.0.0.1:8765/mcp. Consultas mapearam model/repository/service/controller/DTO, validator/storage/ownership, perfil público, MenorAutorizadoPolicy, RF18/RF22. Pesquisa currículo retornou seções documentais com referências INFERRED ao portfólio, sem componente RF38. Queries amplas truncadas foram complementadas por consultas focadas e Java; ausência no grafo não foi usada como prova isolada.

## 5. Database05

Fonte ativa exclusiva database05/palco-database. Todos os **46 arquivos** lidos e inventariados com hash/linhas/declarações/referências, incluindo tipos, dez DDLs, funções, procedures, queries, triggers, init, seed e arquivo adicional do pacote. Identificadas **43 tabelas / 24 enums**, confrontados com bootstrap PostgreSQL real da regressão. Database04 permanece histórico. Nenhuma escrita no banco de desenvolvimento: fixtures e limpeza ocorreram apenas nos bancos descartáveis Testcontainers.

## 6. Estruturas auditadas

Inspeção completa em [auditoria inicial A–K](evidencias/rf38-2026-10-05/auditoria-rf38-inicial.md) e auditoria-estrutural.json. Avaliadas estruturas genéricas candidatas, não apenas nomes.

| Item | Resposta |
|---|---|
| A. Tabela específica | NÃO |
| B. Coluna/enum/papel CURRICULO | NÃO |
| C. FK de artista para principal | NÃO |
| D. 1:1/UNIQUE de principal | NÃO; vínculo genérico 1:N |
| E. Flag principal/ativo | NÃO |
| F. Data específica | NÃO; data_upload é genérica |
| G. Separar currículo/PDF/projeto/certificado | NÃO |
| H. Substituição/exclusão | Infraestrutura genérica parcial; domínio RF38 ausente |
| I. Moderação de currículo | NÃO; alvo não representado |
| J. Integração RF22 | Genérica existente; currículo futuro |
| K. Consulta pública inequívoca | NÃO |

galerias_virtuais/itens_galeria não representam currículo; url_portfolio é URL geral do perfil; OBRA em Salvos não cria o domínio. status_projeto_portfolio_enum isolado não identifica currículo e não resolve RF38.

## 7. portfolio_arquivos

DDL oficial 02_tables/05_portfolio_e_midias.sql: **id, artista_id, url_arquivo, nome_original, tamanho_bytes, tipo_mime, data_upload**. FK artista_id → perfis_artistas(usuario_id), ON DELETE CASCADE; índice idx_portfolio_arquivos_artista comum, sem UNIQUE. PortfolioArquivo espelha esses sete campos. Não há principal, papel, ativo, referência de currículo ou relacionamento alternativo equivalente.

## 8. PDFs atuais

PDF é um dos formatos individuais do portfólio, junto de imagem e MP3. DTO retorna tipo=PDF derivado de tipoMime; isso classifica formato, não função profissional. Lista por artista pode conter vários PDFs; regressão paginada existente insere 51 registros genéricos. Primeiro/último/mais recente, nome “curriculo”, MIME ou extensão não selecionam currículo corretamente.

## 9. Semântica CURRICULO

**AUSENTE** no schema e nos 287 Java de produção auditados. Nenhum Entity/Repository/Service/Controller/DTO específico RF38. Inventário de tabelas/colunas/enums/índices/FKs e referências foi confrontado com Java e rotas. Não existe distinção segura para preservar PDF comum ao substituir um currículo.

## 10. Cardinalidade

Um ARTISTA → muitos portfolio_arquivos; galerias também podem associar múltiplos arquivos. Nenhuma referência principal 0..1 semanticamente RF38. Não converter lista paginada em currículo nem impor currículo como projeto RF16.

## 11. Unicidade

**Um currículo principal ativo não é garantível pelo modelo atual.** Índice por artista não é UNIQUE. PESSIMISTIC_WRITE do DELETE protege um registro genérico, não cria identidade/cardinalidade de currículo. Backend locking só será avaliável após existir recurso semanticamente representado; não foi criado lock/cache/constraint de contorno.

## 12. Data de atualização

data_upload registra criação/upload do arquivo; perfis_artistas.ultima_atualizacao também muda com upload/exclusão de outras mídias. Nenhuma representa inequivocamente a última atualização do currículo inexistente. Futuramente, data_upload do novo arquivo poderá compor atualização se houver referência explícita e contrato de substituição; isso não foi aplicado. Sem inferência por mtime, Last-Modified ou filename.

## 13. Storage

PortfolioStorageService mantém raiz privada app.portfolio.storage-root, referência servidor artista/UUID.ext, gramática/prefixo/normalização e bloqueio de symlink/path traversal. Gravação temporária, force e tentativa de ATOMIC_MOVE com fallback. A raiz não é usada como banco de currículo. Conteúdo passa pelo controller controlado; nenhum diretório público ou sistema paralelo criado.

## 14. Validator

ArquivoPortfolioValidator: **PDF_LIMIT = 10 * 1024 * 1024 = 10 MiB**. Valida extensão, MIME application/pdf, tamanho declarado/lido, nome seguro e prefixo %PDF-. O contrato genérico também aceita JPG/JPEG/PNG e MP3; eventual RF38 terá de restringir apenas PDF reutilizando a política. Assinatura **não prova** documento completo, ausência de malware, JavaScript interno ou dados privados; fluxo não interpreta conteúdo nem executa OCR/sanitização semântica.

## 15. Autenticação

Gestão genérica atual exige JWT e usuário persistido. /api/portfolio/** é restrito a ARTISTA; rotas públicas GET explícitas dispensam login. Nenhuma API RF38 foi criada. Contratos e regressões atuais foram preservados, incluindo 401 para ausência de autenticação.

## 16. Autorização

PortfolioAccessService exige ARTISTA com conta ATIVA; CONTRATANTE/ADMIN/MODERADOR não recebem propriedade automática. Público usa publicabilidade RF10/RF27. Nenhuma autoridade de currículo foi atribuída a artistaId do cliente; regras específicas RF38 permanecem futuras.

## 17. Ownership

Upload genérico deriva artistaAtual; leitura privada/DELETE comparam proprietário persistido. Repositório bloqueia apenas registro do próprio artista; estrangeiro é ocultado com 404 no contrato atual. Princípio reutilizável futuramente, sem alegar que existe currículo para aplicar ownership.

## 18. Endpoint atual

PortfolioController /api/portfolio oferece POST /arquivos, GET /me/arquivos, GET /publico/artistas/{artista}/arquivos, GET /arquivos/{id}/conteudo, GET /publico/arquivos/{id}/conteudo e DELETE /arquivos/{id}, além de embeds individuais. Não há endpoint RF38, seleção principal, substituição de currículo ou campo de currículo em ArtistaPublicoResponse.

## 19. Endpoint proposto/implementado

**Nenhum implementado ou contrato final escolhido.** Criar API sem referência persistente seria simulação. Após pacote oficial, seguir convenções atuais de gestão própria e conteúdo público controlado; não copiar cegamente os caminhos conceituais do prompt.

## 20. Acesso público

PDF genérico de artista publicável pode ser baixado sem login pela rota controlada, mas não como currículo identificável. Content-Type, Content-Length, PDF attachment, filename validado, nosniff, CSP sandbox e no-store existentes. Não expõe storagePath. Perfil público continua sem campo currículo e funciona sem documento; nenhuma definição de perfilCompleto alterada.

## 21. RF10

Lista branca atual em PerfilPublicoService/ArtistaPublicoResponse evita dados privados estruturados; publicabilidade reutiliza MenorAutorizadoPolicy. RF10 admite experiência pública de adulto e restringe a do menor. DTO sem experiência **não garante** que bytes de PDF público não a contenham. Ausência de currículo não bloqueia renderização principal.

## 22. Conflito para menor

**PENDÊNCIA FUNCIONAL explícita.** RF38: “Currículo é público.” RF10: “Para ARTISTA adulto, experiência pode ser pública; para menor, experiência permanece privada.”

Fonte oficial não esclarece currículo público de menor ou compatibilização de PDF opaco com essa privacidade. Não há autorização específica do documento no RF27 nem interpretação/remoção segura de experiência no validador. Não expor novo currículo RF38 de menor por inferência, inventar consentimento ou sanitização.

**Contrato atual preservado:** portfólio genérico permite PDF público de menor autorizado, comprovado pelo teste menorAutorizadoPodeExibirMidiasDoPortfolioMvpSemNovaAprovacao. Não foi bloqueado ou alterado nesta auditoria; esse comportamento não é evidência de currículo RF38. Conteúdo interno potencialmente privado permanece risco que exige decisão funcional, além da estrutura. Não declarar regra definitiva de currículo do menor antes da decisão.

## 23. RF27

Autorização inicial de 14–17, estado apto e responsável permanecem com semântica vigente. Menor autorizado pode ter perfil publicável; consentimento de uso não foi convertido em consentimento para publicar currículo ou dados privados. Nenhum token, estado, decisão, revogação ou fluxo novo.

## 24. RF16

Permanece **BLOQUEADO POR ESTRUTURA**, com arquivos/embeds individuais existentes. Currículo é domínio independente: não exigir projeto nem usar projeto/galeria chamado Currículo. O futuro pacote RF38 deve poder representar documento principal sem depender de workaround RF16.

## 25. RF18

Permanece **PARCIAL**. tipo_conteudo_enum contém VAGA/COMUNIDADE/GALERIA/MENSAGEM; sem ARQUIVO/CURRICULO. Moderação administrativa atual opera VAGA/COMUNIDADE. Validação preventiva de bytes/metadados existe; guard de evidência impede DELETE de arquivo ligado a GALERIA reportada/moderada com 409. Isso não é moderação direta de currículo. Futuro alvo, histórico e preservação dependem de estrutura oficial; nenhum enum inventado.

## 26. RF22

Orquestração atual trata registros genéricos do portfólio e planeja cleanup privado AFTER_COMMIT; falha física sinaliza CONCLUIDA_COM_LIMPEZA_PENDENTE, sem fingir rollback SQL. Evidência que perderia vínculo/isolamento pode recusar exclusão antes de escrever. Futuro currículo deverá integrar referência/lifecycle, cleanup, anonimização/retenção quando necessária e regras de evidência, sem órfãos. Nenhuma alteração RF22; sua conclusão parcial anterior preservada.

## 27. RF39

Certificado é outro domínio, com múltiplos documentos, título/instituição/data profissionais. Não reaproveitar arquivo de certificado ou metadados fictícios para currículo. Separação auditada; RF39 não foi implementado.

## 28. Upload

Nenhum upload RF38 implementado. Contrato genérico preservado e regredido: extensão/MIME/assinatura/tamanho/nome, autenticação, ownership, persistência e compensação de falha. Testes de PDF no limite de 10 MiB e bytes/MIME inválidos pertencem à infraestrutura existente; não comprovam currículo principal.

## 29. Substituição

**AUSENTE para RF38.** Upload cria outro registro genérico; sem principal inequívoco não é possível substituir sem risco de apagar PDF comum. Filesystem + PostgreSQL não são XA. Futuro contrato deve validar/gravar novo PDF, manter antigo se falhar antes do commit, atualizar referência/data, limpar antigo em momento seguro e registrar compensações/reconciliação. ATOMIC_MOVE não torna o conjunto banco+storage atomicamente perfeito.

## 30. Exclusão

DELETE genérico próprio existente com lock, remoção e restauração em rollback, além de guard relacional de evidência. Não existe DELETE do currículo, nem consulta de ausência específica depois dele. Não apagar PDF por nome/MIME/posição. Futuro lifecycle deverá distinguir exclusão principal e conservar arquivos comuns/terceiros/evidência.

## 31. Concorrência

Não existe concorrência RF38 operacional a testar. Lock no arquivo genérico não garante no máximo um principal, porque principal não existe. Nenhum teste de implementação fictícia, UNIQUE ou lock novo. Futuro modelo precisará representar o recurso e permitir prova concorrente de unicidade e substituição.

## 32. Privacidade

Sem nova exposição ou DTO/log de documento, storagePath, CPF, telefone, nascimento, responsável ou consentimento. RF44 não recebeu evento de upload: não há decisão que o torne relevante automaticamente. Rótulo frontend “portfólio ou currículo” na candidatura é link RF06, sem persistência RF38. Auditoria limitada das evidências verifica valores locais conhecidos e JWT assinado; não é scan amplo de secrets/PII. PDF opaco pode conter dados privados mesmo com metadata segura.

## 33. Arquivos de produção

**Nenhum alterado.** 287 Java, configurações, entidades, scripts operacionais e dependências preservados. Não criou tabela/coluna/enum/FK/índice, migration, JSON/cache/diretório como banco, endpoint ou regra pública RF38.

## 34. Arquivos de teste

**Nenhum Java de teste alterado/criado.** Usadas 14 classes existentes; nova inspeção está apenas em evidencias/rf38-2026-10-05/Auditar-Estrutura.ps1, com inventário verificável, matriz A–K e assertions estruturais que exigem reavaliação se o contrato mudar. Os 371 hashes de fontes testadas permaneceram idênticos. Sem assertions relaxadas, testes removidos ou novos skips.

## 35. Frontend alterado

**NÃO.** 770 arquivos e deltas anteriores preservados por hash. Inspeção estática de perfil/portfólio e rótulos de link RF06; nenhuma tela, mock, aba, botão, HTML/JS/React/CSS ou build frontend alterado/executado. Registro semanal solicitado está neste relatório; o arquivo semanal preexistente permanece intacto.

## 36. Banco alterado

**NÃO.** Database05, 47 arquivos database04 e cinco scripts database04 históricos intactos. Nenhuma execução DDL/migration/patch/H2/ddl-auto=create/update/create-drop. Testcontainers inicializa exclusivamente o pacote oficial e faz DML de fixture/cleanup em PostgreSQL descartável, sem alterar banco de desenvolvimento.

## 37. Database05 46/46

ZIP oficial SHA-256 **6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf**. Comparação byte a byte inicial/final: **46/46 idênticos / 0 divergentes / 0 ausentes / 0 adicionais**. Evidência snapshot-final-byte-a-byte.json. Nenhum arquivo original aplicado/reescrito.

## 38. Testes focados

**291 total / 291 passed / 0 failures / 0 errors / 0 skipped; BUILD SUCCESS**. 14 classes; [INFO] Total time:  02:49 min. Exit 0.

14 classes: controller.DenunciaRf14Rf18IntegrationTest, controller.ExclusaoContaRf22IntegrationTest, controller.GuardianConsentRf27IntegrationTest, controller.ModeracaoRf18IntegrationTest, controller.PerfilPublicoRf10IntegrationTest, controller.PortfolioMultipartHttpIntegrationTest, controller.PortfolioRf16IntegrationTest, Database05BootstrapIntegrationTest, OfficialSchemaMappingIntegrationTest, security.GenericEndpointsSecurityIntegrationTest, security.JwtAuthenticationIntegrationTest, service.GuardianConsentServiceTest, service.PortfolioStorageServiceTest, validation.ArquivoPortfolioValidatorTest. PostgreSQL 18/Testcontainers, PALCO_TEST_DATABASE05_PATH e ddl-auto=validate; bootstrap e mapeamento oficial inclusos. Resultados são regressão das capacidades atuais, não teste de currículo ausente. Execução separada da suíte completa; não somar rodadas sobrepostas.

## 39. Maven completo

**1560 total / 1543 passed / 0 failures / 0 errors / 17 skipped; BUILD SUCCESS**. 73 classes; [INFO] Total time:  10:34 min. Exit 0.

Medido nesta tarefa, sem copiar total RF44. Comparação nominal com RF44: 17 skips idênticos, zero novos, nenhuma classe ausente ou contagem anterior alterada. controller.CurrentLocalSchemaIntegrationTest: 4; controller.OfficialLocalApiIntegrationTest: 13. Proveniência confirma inicialização database05 e nenhuma inicialização/URL database04. Sem Maven clean. Artefatos/logs/XML sanitizados de cada rodada preservados; green não entrega persistência RF38 ausente.

## 40. Semgrep

**NÃO EXECUTADO nesta tarefa**, conforme condição do prompt: nenhum Java de produção alterado. Não reutilizar o resultado histórico RF44 como scan RF38 e não alegar scan secrets. Semgrep nativo Windows não usado; TLS/Defender/App Control não alterados. Se houver implementação futura, usar Docker oficial p/java incluindo arquivos novos.

## 41. Graphify final

MCP HTTP confirmado ao final com os mesmos 6453 nós, 22287 arestas, 359 comunidades e confiança 91% extraída/9% inferida/0% ambígua. Hash do graph.json preservado. **graphify update não executado/não necessário:** nenhum Java mudou e as 370 fontes indexadas no fechamento RF44 correspondem ao código do HEAD atual, checkpoint RF44 sem delta. Documentação nova RF38 não foi indexada. Sem label/parser novo/graphify-mcp.exe/contorno de segurança.

## 42. Riscos

Eleger PDF por heurística pode mostrar/apagar documento errado, ocultar outro PDF ou permitir vários principais. Documento opaco pode expor experiência/PII de menor e adulto. Prefixo %PDF-/headers não eliminam malware ou conteúdo ativo. Banco+filesystem têm janelas de compensação/crash; cleanup pode falhar. Alvos polimórficos RF18 não garantem vínculo/moderação de currículo inexistente. Não declarar integridade, privacidade jurídica ou atomicidade absoluta por teste verde.

## 43. Limitações

Sem recurso RF38, testes não comprovam upload/substituição/exclusão/concorrência de currículo principal. Sem OCR, parser especializado, antivirus, análise semântica de PDF, carga/p95/p99 ou percentual de cobertura RNF10 medido. 17 testes locais condicionais históricos mantidos. RF10/27 e regressão de DTO não garantem ausência de PII em binário. Pendências anteriores RF16/RF18/RF17/RF44 preservadas.

## 44. Estrutura mínima necessária — conceitual

Não aplicada e sem SQL/migration/Entity proposta final. Pacote oficial deve representar currículo principal, ARTISTA proprietário, referência segura ao arquivo PDF, no máximo um principal ativo, criação/última atualização e lifecycle inequívoco de substituição/exclusão; separar PDF comum, projeto e certificado e integrar moderação/evidência/RF22.

| Alternativa conceitual | Vantagem | Risco/decisão necessária |
|---|---|---|
| Recurso de currículo dedicado 0..1 por ARTISTA, ligado a arquivo genérico | Identidade e principal explícitos; metadata profissional separada | Ownership consistente entre vínculos, impedir reutilização indevida/órfãos e definir lifecycle/cleanup |
| Papel profissional explícito aprovado na estrutura existente | Reutiliza storage/metadata e reduz representação paralela | Distinguir documentos comuns/certificados/projetos, limitar principal sob concorrência e mapear moderação/retenção sem quebrar portfólio |

Nenhuma forma física/tabela/constraint final foi escolhida. Locking backend pode ajudar após identidade persistente existir; não substitui semântica. Solicitar pacote completo à responsável pelo banco com essas capacidades, além da decisão funcional sobre documentos de menores.

## 45. RF38 final

**BLOQUEADO POR ESTRUTURA.** Critérios A/B não são possíveis no database05: persistência inequívoca e principal único ausentes. Infraestrutura genérica reutilizável não constitui RF38 parcialmente implementado. Pendência adicional I: política oficial de currículo público de menor; J: alvo/moderação no nível necessário. Database05 preservado e regressões verdes atendem preservação, não conclusão do requisito.

## 46. Pendências

Novo pacote oficial com semântica/cardinalidade/datas/lifecycle de currículo; decisão oficial RF10 × RF38 para menor e regra de conteúdo/PII no documento; integração futura RF18/evidência e RF22/cleanup; implementação/testes reais somente depois. Nenhuma solicitação enviada a terceiros. **Inspeção final: git diff --check exit 0, stdout vazio; HEAD preservado; indice vazio; backend sem delta** Sem staging, commit ou push.

## 47. Próximo passo

Revisão funcional e pacote oficial completo com capacidades mínimas; reauditar RF38 antes de escolher API/DTO e implementar. Currículo não depende de inventar projeto RF16. Com suporte real, testar principal único/concorrência, substituição e compensações, ownership, PDF, público autorizado, RF18/RF22; depois Semgrep Docker e Graphify update, mantendo banco/frontend no escopo explicitamente autorizado.

## Registro semanal — RF38

- Data: 05/10/2026.
- Objetivo: auditar RF38; implementar somente se estruturalmente suportado.
- RF/RNF: RF10/16/18/22/27/30/38/39/44; RNF05/06/07/08/09/10/17.
- Backend alterado: NÃO; produção e testes preservados.
- Frontend alterado: NÃO.
- Banco alterado: NÃO.
- Database05: 46/46 idênticos ao ZIP, zero divergentes/ausentes/adicionais.
- Persistência currículo: AUSENTE; arquivo genérico 1:N sem papel/principal.
- PDF: validação genérica existente, até 10 MiB; não cria currículo.
- Unicidade: principal RF38 não representado/garantível.
- Ownership: genérico JWT/owner existente; sem novo recurso.
- Perfil público: preservado, sem campo/rota de currículo.
- Menores: conflito RF10 × RF38 pendente; nenhum novo acesso de currículo.
- RF18: PARCIAL, sem alvo direto de arquivo/currículo.
- RF22: regressão preservada; lifecycle do currículo futuro.
- Testes focados: **291 total / 291 passed / 0 failures / 0 errors / 0 skipped; BUILD SUCCESS**. 14 classes; [INFO] Total time:  02:49 min. Exit 0.
- Maven: **1560 total / 1543 passed / 0 failures / 0 errors / 17 skipped; BUILD SUCCESS**. 73 classes; [INFO] Total time:  10:34 min. Exit 0.
- Semgrep: não executado; nenhum Java de produção mudou.
- Graphify: MCP HTTP 6453/22287/359; código atual indexado, sem update necessário.
- RF38 final: BLOQUEADO POR ESTRUTURA.
- Blockers: identidade persistente/cardinalidade principal ausentes.
- Pendências: pacote oficial, política de PDF do menor e moderação/lifecycle.
- Próximo passo: decidir e reauditar antes de implementar; nenhum SQL ou migration.


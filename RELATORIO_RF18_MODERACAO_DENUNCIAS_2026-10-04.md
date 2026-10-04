# RF18 — Moderação automática e denúncia de conteúdo

Data: 04/10/2026. Checkout: `C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline`.

**RF18: PARCIAL.** Foram implementados prevenção determinística, reforço das denúncias existentes, moderação auditável e ocultação de VAGA/COMUNIDADE pública, além de preservação de arquivos vinculados a galerias denunciadas/moderadas. O database05 não representa com segurança denúncia geral/moderação de PERFIL, ARQUIVO ou EMBED individuais; esses produtores atuais impedem declarar o RF18 completo. Os produtores futuros foram mantidos fora da implementação.

## 1. Objetivo

Auditar o estado atual, implementar somente lacunas sustentadas pelo database05 e validar prevenção, denúncias, autorização, ocultação e preservação de evidência. Sem banco/SQL, frontend, staging, commit ou push; sem API paga ou contorno de segurança.

## 2. Requisitos e fontes oficiais

Fonte atual: `C:/Users/masca/OneDrive/Área de Trabalho/-/trabalhosAula/tecnico/pji/rf e rnf.txt`, inventariada em `evidencias/rf18-2026-10-04/Inventariar-Fontes.ps1` e `rf-rnf-consultados.txt`. RF08/RF10, RF14, RF15 fora do MVP, RF16, RF17/RF45, RF18, RF22, RF27, RF30, RF31, RF35, RF36, RF40 e RF44 foram confrontados com os produtores reais. RNF02/05/06/07/08/09/10/17 aplicados como segurança SQL, desempenho, privacidade, autorização, auditoria, testabilidade e paginação.

Relatórios históricos consultados: RF16 por projetos, RF16 individual, RF22, RF35, RF36, RF45, RF19, sincronização database05, integração de denúncias/RF14 e portfólio. Não foram reescritos nem usados como prova de execução atual. RF31 no histórico de testes de gestão de vagas tem numeração anterior; não comprova implementação do RF31 revisado de comunidades.

## 3. HEAD e checkpoint

HEAD inicial: `3d0d80988954170ed30caa0eea725f3c4238992e`, `docs: audita bloqueio estrutural do RF16`; branch `integracao-recuperada-2026-09-15`. Fetch de `fork` concluído; divergência 0/0; upstream igual ao HEAD. Index inicialmente vazio e sem alterações backend/database05/database04/scripts pertencentes ao RF16. O checkpoint RF36 antecedente também permanece no histórico.

O baseline tinha alterações históricas em README, SQL histórico e frontend, além de arquivos não rastreados. Seus hashes foram registrados antes da edição. Essas alterações não pertencem ao RF18 e foram preservadas. Estado final: HEAD preservado, index vazio; git status --short, git diff --stat, git diff --check e git diff --name-status -- backend com exit 0. diff --check sem erros (avisos LF/CRLF não são erros de whitespace). Novos arquivos não rastreados constam de escopo-autorizado.json; nenhum staging/commit/push..

## 4. Graphify inicial

MCP HTTP acessível e usado na auditoria inicial: **6214 nós, 21299 arestas, 356 comunidades; EXTRACTED 91%, INFERRED 9%, AMBIGUOUS 0%**. Consultas de denúncia, portfólio, exclusão e dependências complementaram a leitura direta. Algumas referências SQL do grafo apontavam para snapshots históricos; a fonte estrutural decisiva foi o SQL atual do database05, nunca uma associação inferida do grafo.

Respostas iniciais preservadas nos arquivos `rf18.initial.*.json`, `rf18.second.*.json`, `rf18.audit.*.json` da pasta de evidências.

## 5. Database05

Snapshot oficial: `C:/Users/masca/Downloads/palco-database05.zip`, SHA-256 `6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf`. Foram inventariados 46 arquivos e 43 tabelas. PKs, FKs, nullabilidade, enums, defaults, constraints, ON DELETE e gatilhos relevantes estão em `inventario-46-arquivos.json/md` e `auditoria-estrutura-database05.md`. Nenhum arquivo SQL foi editado nesta tarefa.

## 6. Estruturas de denúncia

| Estrutura | Representação real | Limite |
|---|---|---|
| `reportes_usuario` | PK; denunciante FK obrigatório; tipo enum, conteudo_id, motivo varchar(150), descrição e timestamp | Sem FK do alvo, status, UNIQUE ou histórico de decisões |
| `denuncias_plagio` | Denunciante e perfil denunciado com FKs; tipo de violação, descrição, status, datas e URLs opcionais | Denúncia de autoria do perfil; URL de prova sem FK de arquivo; sem trilha de transições |
| `tipo_conteudo_enum` | VAGA, COMUNIDADE, GALERIA, MENSAGEM | Não contém PERFIL, ARQUIVO, EMBED, PROJETO, EVENTO ou PUBLICACAO |

O conteúdo_id polimórfico não é confiado sem validação de tipo, domínio e visibilidade no servidor. Os ON DELETE CASCADE exigem os guards e anonimização do RF22; não provam preservação automática.

## 7. Estruturas de moderação

`moderacao_conteudo` possui PK própria, tipo/conteudo_id, autor FK obrigatório, moderador FK opcional, `APROVADO/BLOQUEADO/SOB_ANALISE`, justificativa, contestação, datas e score. Não há UNIQUE por alvo: o novo fluxo grava uma linha por ação e usa a maior PK como estado efetivo. A leitura considera o último status nulo como SOB_ANALISE, conservando ocultação.

Esse uso fornece trilha por alvo no backend, sem coluna ou tabela nova. Não é prova jurídica imutável, não impede alteração por administrador direto do PostgreSQL e não cria um processo RF14 completo. A identidade do ator vem do JWT, não do corpo da requisição.

## 8. Matriz dos conteúdos auditados

Matriz inicial criada antes da produção em `matriz-conteudos-rf18.md`. Resultado final:

| Conteúdo / RF produtor | Existe/publicidade | Prevenção | Denúncia / alvo | Moderação / auditoria | RF18 final |
|---|---|---|---|---|---|
| Perfil / RF08/RF10 | SIM; conforme RF27 | Texto público e URLs na escrita | Plágio por perfil com FK; denúncia geral sem tipo compatível | Ocultação auditável específica não representada | PARCIAL |
| Vaga / RF04/RF07 | SIM; feed/detalhe/similares | Texto puro, limites e URLs; revalidação ao publicar | VAGA pública real, JWT | Ações por alvo; último estado restringe leitura | OK no recorte atual |
| Arquivo / PORT1/RF16 legado | SIM; artista publicável | Validador atual de upload/storage e metadados | Sem alvo direto; vínculo seguro por itens_galeria quando existente | DELETE protegido via GALERIA; moderação direta indisponível | PARCIAL |
| Embed / PORT2/RF30 | SIM; artista publicável | Allowlist HTTPS/canonicalização e legenda | Sem alvo EMBED direto | Ocultação e prova sem vínculo inequívoco | PARCIAL |
| Comunidade / RF31 histórico | SOMENTE leitura; pública para autenticado | Sem produtor POST/PUT atual para conectar | COMUNIDADE pública com autor real | Ocultação na consulta e ações auditáveis | OK no recorte de leitura; produtor FUTURO |
| Galeria histórica | Estrutura relacional; não criado novo módulo | Nenhum produtor novo | GALERIA existente preservada como vínculo de evidência | Não convertida em arquivo/projeto | Integração limitada à preservação |
| Evento público | Sem produtor correspondente | — | Sem tipo EVENTO | — | FUTURO/BLOQUEADO |
| Projeto / RF16 revisado | AUSENTE | — | Sem agregado e relações de mídias | — | BLOQUEADO pelo RF16 |
| Publicação / RF40 | AUSENTE | — | Sem produtor/modelo | — | FUTURO |
| Mensagem privada / RF35 | SIM; restrita aos participantes | Validação privada existente | Reporte MENSAGEM conforme política própria | Preservação/exclusão lógica existentes | Fora do filtro público RF18; regressão |

## 9. Estado inicial

Havia registro privado de denúncia de vaga/plágio de perfil, validadores fortes de upload/embeds e proteções de chat/RF22. Faltavam prevenção compartilhada de texto público, deduplicação concorrente, ações administrativas representáveis, gates públicos de moderação e consulta de evidência relacional no DELETE individual de arquivo.

## 10. Validações reaproveitadas

`ArquivoPortfolioValidator`, `VideoPortfolioValidator`, `PortfolioStorageService`, `PortfolioAccessService`, política de publicação e prazo de vagas, resolução JWT e `PerfilPublicoService` continuam responsáveis pelos contratos atuais. Não foi criado validador paralelo de MIME, upload ou allowlist externa.

## 11. Gaps corrigidos e não corrigidos

Corrigidos no recorte representável: texto ativo/controles/spam objetivo; metadados; denúncia de COMUNIDADE pública já legível; alvo privado/inexistente; duplicata; permissões administrativas; estados auditáveis; ocultação antes da paginação; DELETE com vínculo real de galeria.

Permanecem: denúncia geral e moderação específica de perfil; alvos individuais de arquivo/embed; prova sem FK; histórico/contestação/medidas RF14; produtor RF16 por projetos. Detalhes nas seções 47–51. Não foram criadas equivalências de domínio ou status simulados.

## 12. Arquivos de produção

Lista exata em `escopo-autorizado.json`; 21 arquivos Java de produção, incluindo 6 novos:

- Novos: `validation/ConteudoPublicoValidator`, `model/ModeracaoConteudo`, `repository/ModeracaoConteudoRepository`, `service/ModeracaoConteudoService`, `dto/ModeracaoRequest`, `controller/ModeracaoController`.
- Integrações: `SecurityConfig`, `CommunityController`, `DenunciaController`, `DenunciaRequest`, `DenunciaRepository`, `DenunciaService`, `VagaRepository`, `VagaSpecifications`, `VagaService`, `PerfilArtistaService`, `PerfilContratanteService`, `PortfolioArquivoService`, `PortfolioVideoService`, `UsuarioService`, `AuthService`.

Não foram alterados pom, propriedades, dependências, scripts operacionais, SQL, frontend, chat ou orquestração RF22.

## 13. Arquivos de teste

Novos: `ModeracaoRf18IntegrationTest` e `ConteudoPublicoValidatorTest`. Ajustes necessários em `DenunciaRf14Rf18IntegrationTest` (COMUNIDADE válida, mas inexistente → 404) `VagaServiceTamanhoTest` (novas dependências do construtor) e `UsuarioServicePasswordPolicyTest` (mock da nova dependência). Nenhum assert foi removido para obter sucesso, nenhum caso foi desabilitado e nenhum skip novo foi criado.

## 14. Prevenção determinística

O validador compartilhado rejeita HTML em campos de texto puro usando parser JDK e guarda lexical para markup truncado; rejeita esquemas ativos, controles invisíveis/bidirecionais, excesso de links e repetição mecânica. Biografia até 5000; descrição/requisitos/benefícios de vaga até 20000; legenda 255; nome/título/motivo 150; complemento de denúncia 2000; justificativa administrativa 1000.

Limites de texto longo, 256 caracteres iguais consecutivos e 20 links são decisões técnicas preventivas novas deste recorte RF18, não números atribuídos ao documento funcional. Não há lista de opiniões/palavras artísticas proibidas, classificação moral ou aprovação manual obrigatória. Violações retornam erro contextual antes da persistência.

## 15. Validação, normalização, sanitização e escape

Validação aceita/rejeita; normalização anterior de espaços/localização e canonicalização de embeds permanecem nos respectivos serviços. Não se remove HTML silenciosamente nem se reescreve obra artística. Parser JDK + verificação de abertura incompleta evita depender somente de regex para HTML.

O backend fornece texto puro e rejeita markup ativo nos campos integrados; isso não substitui escape contextual no consumidor. Texto `&lt;script&gt;` é literal e não deve ser decodificado e inserido em innerHTML. Nenhum frontend foi modificado ou declarado validado visualmente nesta tarefa.

## 16. URLs

URLs de portfólio, banner e fotos têm validação estrutural HTTPS sem userinfo/credenciais ou porta arbitrária; rotas internas somente onde autorizadas, `/assets/`, `assets/` legado ou `/api/`, sem traversal, barras invertidas ou percent-encoding. Embeds usam a allowlist e canonicalização do PORT2: YouTube, Vimeo e Spotify, sem iframe enviado pelo usuário. Domínios parecidos/subdomínios indevidos não são aceitos pelo validador de embed.

Links genéricos de portfólio não são uma allowlist de reputação. Não houve fetch de URL, resolução remota, download de mídia ou mecanismo SSRF novo.

## 17. Uploads

Regras existentes preservadas: JPG/JPEG/PNG até 5 MiB, PDF até 10 MiB, MP3 até 20 MiB; extensão/MIME/bytes coerentes; storage por referência UUID com guards de caminhos. Vídeo por link externo, sem upload direto. Headers/download existentes preservados.

**Assinatura `%PDF-` não prova ausência de JavaScript, malware ou conteúdo ofensivo em PDF.** Não foi instalado antivírus, parser/serviço pago ou alegada inspeção completa de PDF. Os testes de MIME/extensão/tamanho/assinatura e upload válido foram regredidos.

## 18. Spam e abuso

Prevenção objetiva por limites de tamanho, controles, mais de 20 links e mais de 256 repetições do mesmo caractere não branco. Não existe banimento por opinião, análise semântica paga ou quota persistente nova. Deduplicação da denúncia reduz repetição acidental, sem ser apresentada como rate limit ou proteção absoluta contra abuso distribuído.

## 19. Fluxo de denúncia

`POST /api/denuncias`: profissional ativo com JWT; VAGA ou COMUNIDADE pública real em `reportes_usuario`; plágio de perfil publicável em `denuncias_plagio`. Retorna 201/Location no novo registro e 200/mesmo ID em duplicata. Lista/detalhe próprios seguem privados.

Reportes gerais mantêm status null, porque a tabela não tem status. Não há notificação RF36/RF44 nova nem bloqueio automático causado apenas pela denúncia.

## 20. Autenticação e identidade

Denunciante e ator administrativo são resolvidos do JWT validado e usuário atual ativo. Campos adicionais de identidade/status/data no JSON não comandam a persistência. Anônimo/token inválido/conta inapta/papel indevido são rejeitados. SecurityConfig e service aplicam a restrição administrativa; não se libera CRUD global de usuários ou comunidades.

## 21. Alvo real

Tipo e ID são conferidos em seu domínio. Vaga RASCUNHO, inclusive do próprio dono, não vira alvo de denúncia pública. Comunidade privada, sem autor válido, inexistente ou oculta por moderação não é denunciável pelo novo fluxo público. Perfil continua passando pelo tipo, conta ativa e política RF10/RF27.

Administração só opera VAGA/COMUNIDADE com autor real e condição de publicação pública. GALERIA/MENSAGEM/PERFIL/ARQUIVO/PROJETO não foram aceitos como aliases administrativos.

## 22. Motivo e complemento

Motivo geral obrigatório até 150, sem catálogo moral inventado; plágio exige enum oficial e descrição detalhada. Complemento até 2000; texto ativo/controles/spam objetivo são rejeitados. Consultas parametrizadas preservam texto literal de tentativas SQL sem executar comandos.

Texto livre pode conter PII fornecida pelo próprio usuário: minimização de DTOs não é detecção absoluta de dados pessoais ou segredos. O relato completo fica restrito ao denunciante no fluxo atual; metadados administrativos não incluem complemento, identidade/contato do denunciante ou URLs de prova.

## 23. Deduplicação

Lock transacional PostgreSQL por denunciante/tipo/ID (`pg_advisory_xact_lock` com chave vinculada), válido entre instâncias, serializa verificação + inserção. Reporte geral idêntico (motivo/complemento) nos últimos 60 segundos reutiliza ID. Plágio idêntico reutiliza ID enquanto RECEBIDA/EM_ANALISE; estados terminais permitem novo relato. Autor, tipo, alvo ou conteúdo diferentes não são bloqueados por UNIQUE global.

Sem coluna/constraint/cache em memória novo. Não há estado de processo geral para comparar; a janela não é fingida como status. Teste concorrente exige exatamente uma linha e respostas 201/200.

## 24. Privacidade

Lista/detalhe próprios preservam filtro de denunciante. Administração lista metadados de VAGA/COMUNIDADE com paginação e sem relato/prova completa, contato, CPF ou identidade do denunciante. Histórico contém somente ação/ator interno/estado/justificativa/data e exige equipe ativa. Nenhum DTO administrativo foi exposto como rota pública.

Sem novos logs de JWT, relato privado ou responsável. Cópias XML de evidência removem properties de ambiente; verificação limitada das evidências: 179 arquivos verificados, zero valores locais conhecidos >=8 caracteres e zero JWTs assinados; não é scan amplo de secrets ou prova de ausência absoluta.

## 25. Matriz ADMIN/MODERADOR

Política conservadora adotada para os novos endpoints; não presume equivalência de poderes nem implementa papéis locais RF31.

| Ação | ADMIN ativo | MODERADOR ativo | Usuário profissional |
|---|---|---|---|
| Listar metadados representáveis / histórico restrito | SIM | SIM | NÃO |
| SOB_ANALISE / BLOQUEADO | SIM | Sem histórico ou se último ator é o próprio | NÃO |
| APROVADO / superar ação de outro ator | SIM | NÃO | NÃO |
| DELETE administrativo / prova privada integral / alterar RF14 | Não implementado | Não implementado | Não implementado |
| Denunciar alvo público | Não forja denunciante | Não forja denunciante | Próprio JWT |

Última ação histórica sem ator também exige ADMIN para revisão. A propriedade de análise é uma regra do backend sobre o histórico existente, não uma coluna/estado novo ou alegação de contrato RF14 implementado.

## 26. Estados e transições

RF14: RECEBIDA, EM_ANALISE, PROCEDENTE, IMPROCEDENTE, ENCERRADA, mantidos sem endpoint novo de alteração/contestação. Moderação pública: APROVADO, BLOQUEADO, SOB_ANALISE. No fluxo novo, somente mudança entre estados oficiais diferentes; repetição → 409; estado desconhecido → 400; falta de autoria/publicação → 422; ator indevido → 403.

Sem ação prévia, publicação válida é aprovada implicitamente, sem fila manual obrigatória. Status nulo em linha histórica oculta o conteúdo como SOB_ANALISE. Restauração grava nova ação; não apaga decisão anterior.

## 27. Ocultação

Última ação BLOQUEADO/SOB_ANALISE impede feed/detalhe/similares/recomendação pública de vaga e leitura de comunidade. Filtros aplicados em SQL/Criteria antes do LIMIT/cursor e nas consultas de total, sem preencher página por filtragem posterior em memória.

Dono mantém acesso de gerenciamento da própria vaga; isso não a recoloca no feed. Status de negócio da vaga não é alterado por moderação. Comunidade privada continua regida pelas regras de leitura atuais; não foi implementado novo produtor ou privacidade OCULTA.

## 28. Auditoria administrativa

`POST /api/moderacao/{VAGA|COMUNIDADE}/{id}/acoes` grava tipo/ID real, autor derivado da entidade, ator JWT, justificativa, estado e datas do servidor. Lock FOR UPDATE no alvo serializa decisões. `GET /api/moderacao/{tipo}/{id}/historico` devolve ações paginadas por ID descendente; `GET /api/moderacao/denuncias` lista somente metadados suportados.

Sem ação crítica invisível, sem endpoint que edite/apague histórico e sem uso de log comum como única auditoria. Sem comprovação jurídica de imutabilidade ou trilha RF14 completa.

## 29. Preservação de evidência

DELETE individual do arquivo verifica `itens_galeria.arquivo_id → galeria_id → reportes_usuario/moderacao_conteudo GALERIA`. Com vínculo real: **409**, preservando registro, associação e bytes; não inicia o DELETE físico. O catch conserva ConflictException sem convertê-la em 500. Ownership/lock/compensação existentes permanecem.

Guard conservador considera todo reporte/moderação associado, porque reportes gerais não têm status de encerramento e não existe política de descarte automática. URL textual de prova não é FK: não foi criado parsing de URL nem convertido conteúdo_id de GALERIA em arquivo_id.

## 30. RF14

Registro, enum, descrição/status/datas e consultas próprias de plágio preservados. Sem promessa jurídica de autoria/anterioridade (RF15 fora do MVP). Histórico de decisões, medidas/documentos, notificação e contestação única não foram inventados em tabela sem trilha própria. Denúncia geral de assédio em perfil não é convertida silenciosamente em plágio.

## 31. RF16

Portfólio individual existente continua funcionando; prevenção de metadados/legendas e guard relacional de evidência integrados. Projeto, capa, múltiplas mídias do agregado, RASCUNHO/PUBLICADO e respectivos produtores não existem no modelo atual e não foram criados. **RF18 de projeto completo condicionado ao futuro RF16.**

## 32. RF22

Orquestração não modificada. Regressão de exclusão/anonimização/guards/rollback e retenção justificada nos cenários suportados. Tabelas com CASCADE não tornam retenção automática; fluxo RF22 atual continua responsável pela proteção. Não se declara uma nova solução de LGPD/retenção jurídica definitiva.

## 33. RF27 e RF44

Perfil/portfólio público e denúncia de perfil respeitam publicabilidade/consentimento e dados restritos atuais. Sem responsável, consentimento, nascimento ou experiência privada nos novos DTOs de equipe. Não foi criado aviso de denúncia ao responsável nem monitoramento de menor.

## 34. RF30

Allowlist, HTTPS, providers/IDs/canonicalização e iframe produzido pelo backend preservados. Legenda validada como texto puro na criação/edição. Embed não é Projeto RF16 nem novo alvo individual suportado no enum de moderação.

## 35. RF35

Nenhuma alteração de ChatService/controllers/STOMP/SQL. Filtro de texto público não foi conectado a mensagens privadas. Política existente de denúncia, participantes, exclusão lógica e evidência restrita regredida; sem monitoramento automático novo.

## 36. RF31

Integração limitada às comunidades públicas já presentes e legíveis: validar alvo, registrar denúncia, ocultar leitura e auditar ação. Não foram implementadas criação/edição, papéis locais, convites, eventos, OCULTA ou consolidação do RF31 revisado. Prevenção do futuro produtor condicionada à sua implementação.

## 37. RF40

Sem produtor/modelo de publicação social; nenhum endpoint/tabela fictício criado. Integração da prevenção/denúncia/moderação condicionada ao RF40 e ao schema autorizado.

## 38. Paginação e estabilidade

Denúncias próprias: default 20/max 50, ordenação data/categoria/ID. Administração/histórico: default 20/max 50, offset seguro em long, ordenação com ID de desempate. Comunidade conserva contrato existente default 12/max 50; vaga conserva cursor/size e total do feed. Moderação é parte do predicado anterior ao limite; não se usa filtro Java após paginação para ocultar.

## 39. Segurança do alvo polimórfico

Valores SQL vinculados; tipos administrativos allowlist fixa; consultas por domínio selecionadas pelo servidor. Não existe tabela escolhida por parâmetro HTTP, alias entre arquivo/galeria/projeto ou confiança em dono/ator fornecido. IDs inválidos, domínio errado e contexto privado são regredidos. Ausência de FK continua sendo uma limitação para writers externos ao backend e exclusão/recriação de IDs.

## 40. Banco alterado

**NÃO.** Nenhum database05/database04/SQL/migration/patch/bootstrap/enum/constraint alterado; nenhum banco de desenvolvimento iniciado ou escrito. INSERT/TRUNCATE das fixtures existem somente nos bancos descartáveis dos testes com SQL oficial. O novo mapping parcial lê tabela existente com ddl-auto=validate.

## 41. Frontend alterado

**NÃO.** Frontends, assets e deltas anteriores preservados por hash. Maven apenas copia recursos existentes para diretórios de build ignorados conforme pom atual; nenhuma implementação visual ou validação de navegador integra esta entrega.

## 42. Comparação dos 46 arquivos

**46/46 arquivos idênticos byte a byte; 0 ausentes, 0 divergentes, 0 adicionais**. Evidência: `snapshot-final-byte-a-byte.json`, `preservacao-final.json`, `delta-final.json`, `fontes-testadas-final.json`. O último identifica mudanças depois dos testes; o manifesto inicial permite separar todas as alterações desta tarefa dos deltas históricos.

## 43. Testes focados

**26 classes / 646 total / 646 passed / 0 failures / 0 errors / 0 skipped; BUILD SUCCESS; exit 0; duração 05:01 min**. Classes/totais/duração por XML sanitizado em `maven-focado-final-totais.json` e `maven-focado-final-reports`. Incluem RF18, denúncia, upload/embeds/storage, RF22, RF35, RF10, segurança/JWT, bootstrap/mapping oficial, publicação/edição/detalhe/feed/gestão de vagas, dashboard, RF08, cadastro/Google e unidade de tamanho/policy de senha.

A primeira execução focada teve 637 total / 629 passed / 8 failures / 0 errors / 0 skipped, por 7 regressões do caminho legado de foto assets/... e 1 mock incompleto. Corrigidos contrato interno e mock mantendo asserts; a execução final acrescentou casos de metadados, nome de arquivo e publicação de rascunho. O mapa dos 68 cenários do prompt, com distinção entre testes e blockers, consta de evidencias/rf18-2026-10-04/cobertura-cenarios-rf18.md.

## 44. Maven completo

**69 classes / 1464 total / 1447 passed / 0 failures / 0 errors / 17 skipped; BUILD SUCCESS; exit 0; duração 09:24 min**. Comando: `.\mvnw.cmd test '-Dspring.test.mockmvc.print=NONE'`, PALCO_TEST_DATABASE05_PATH, PostgreSQL 18/Testcontainers real, init/seed oficiais, ddl-auto=validate; sem H2/DDL auxiliar/database04.

Comparação nominal com RF16 confirmou exatamente os mesmos 17 skips: CurrentLocalSchemaIntegrationTest (4) e OfficialLocalApiIntegrationTest (13). Nenhuma classe anterior ausente; 2 novas classes RF18 e 74 novos casos; zero novos skips. 45 inicializações e 88 ocorrências JDBC database05 no log, zero database04.. Evidências: `maven-completo.log`, `maven-completo-totais.json`, XMLs sanitizados, `regressao-comparacao.json`, `maven-proveniencia.json`. Falhas eventualmente encontradas em validação intermediária são registradas separadamente; resultado final corresponde às fontes efetivamente testadas.

## 45. Semgrep

**Semgrep 1.178.0 / p/java / 357 arquivos / 60 regras / ~100.0% parse / 0 findings / 0 blocking / 0 errors / exit 0; duração 40.8 s; 26/26 fontes Java do delta cobertas**. Imagem oficial Docker `semgrep/semgrep@sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b`; ruleset oficial p/java com SHA-256 `5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1` conferido, montado localmente; main/test e Java não rastreado incluídos. Mounts read-only e rede none, sem uso nativo Windows, sem desativar TLS e sem mudança de ignore do repositório.

**0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.** Não foi declarado scan de secrets novo nem reutilizado resultado histórico como execução RF18.

## 46. Graphify final

**graphify update .: exit 0, 31.8 s; MCP HTTP final: 6319 nós / 21666 arestas / 343 comunidades; EXTRACTED 91%, INFERRED 9%, AMBIGUOUS 0%. Novo ModeracaoConteudoService confirmado pelo get_node MCP**. AST-only via `graphify update .`, seguido de confirmação Graphify MCP HTTP. Sem semantic labeling, parser SQL instalado, reinstalação Python ou contorno do Windows App Control. Se o ambiente bloquear a execução, preservar evidência e registrar limitação sem contornar a política.

## 47. Riscos

Ausência de FK do alvo polimórfico e de prova; sem status geral; inferência do estado efetivo por maior PK; campo de status legado nulo; retenção conservadora de todas as ações de galeria; nenhuma inspeção profunda de PDF. Transação banco/storage existente continua com compensação de rollback, sem garantia de atomicidade contra queda de processo/SO.

Regras preventivas podem rejeitar markup literal em campos contratados como texto puro; mensagens são contextuais e não há remoção silenciosa. Escape no consumidor continua obrigatório. Writers externos ao backend podem violar convenções de auditoria ou introduzir conteúdo sem prevenção.

Não foi medido o SLO RNF05 de 100 usuários/p95/p99 nem cobertura percentual JaCoCo: sucesso de testes não comprova essas metas. Avisos Hibernate HHH90003004 aparecem em fluxos existentes da suíte; o recorte novo aplica a ocultação no predicado antes do limite e não usa filtragem Java de itens moderados. Comunidade histórica sem criador não recebe autor inventado para ação; requer saneamento/modelo oficial separado.

## 48. Limitações estruturais

| Blocker de produtor atual | Motivo concreto | Resultado |
|---|---|---|
| PERFIL: denúncia geral/moderação auditável | Enum não possui tipo; plágio é outro contrato | RF18 PARCIAL |
| ARQUIVO/EMBED: denúncia/moderação direta | Enum e relações não representam alvo individual | RF18 PARCIAL |
| Prova de arquivo em RF14 | URL sem FK inequívoca | Sem guard genérico de prova; blocker documentado |
| Ciclo RF14/contestação | Campos de estado/medida sem histórico de decisões/contestação por ação | Não implementado sem ampliar modelo |

Guard de galeria só cobre arquivos realmente associados. Não promete proteger toda mídia usada como prova por URL nem bloquear DELETE de embed sem vínculo seguro.

## 49. Produtores futuros

Projeto RF16 bloqueado; publicação RF40, produtor de comunidades/eventos RF31 e selo RF15 fora desta tarefa. A prevenção compartilhada pode ser reutilizada quando esses produtores e sua representação persistente forem aprovados. Ausência futura não é usada para esconder lacuna de produtor atual obrigatório.

## 50. RF18 final

**PARCIAL.** Recorte sustentado pelo schema implementado e submetido às validações descritas; núcleo determinístico sem dependência paga. Não se declara RF18 CONCLUÍDO porque perfis/arquivos/embeds atuais ainda não têm denúncia/moderação/evidência integralmente representáveis. Maven/Semgrep verdes não removem essa limitação funcional.

## 51. Pendências

Definir formalmente modelo de alvo/evidência e auditoria RF14 para produtores atuais, revisar retenção/descarte e controle de restauração com a governança do projeto. Qualquer alteração de schema exige tarefa própria autorizada; não houve migration nesta entrega. Conectar produtores futuros quando existirem. Validar posteriormente consumidores/UX de equipe sem alegar frontend implementado.

## 52. Próximo passo

Decidir estrutura oficial para denúncia/moderação de PERFIL/ARQUIVO/EMBED, vínculo de prova e histórico RF14 antes de nova implementação. Manter RF16 por projetos separado e respeitar o snapshot oficial. Nenhum commit/staging/push realizado.

## Registro semanal

| Campo | Registro |
|---|---|
| Data / objetivo | 04/10/2026 — auditar/implementar RF18 |
| RF/RNF | RF18 e integrações/limites da seção 2; RNF02/05/06/07/08/09/10/17 |
| Backend alterado | SIM; 21 produção + 5 testes, lista exata em escopo-autorizado.json |
| Frontend / banco alterado | NÃO / NÃO; baseline preservado |
| Database05 | **46/46 arquivos idênticos byte a byte; 0 ausentes, 0 divergentes, 0 adicionais** |
| Moderação preventiva | Texto puro e spam objetivo; upload/URL validators existentes |
| Denúncias / privacidade | VAGA/COMUNIDADE pública + plágio existente; JWT e consultas próprias; metadata mínima de equipe |
| Admin/moderador / auditoria | Matriz distinta; ação com ator/data/justificativa e histórico por alvo |
| Portfólio | Guard relacional de galeria; sem Projeto ou alvo individual novo |
| Chat / menor | Política privada e RF27 preservados; sem novo aviso/monitoramento |
| Testes focados | **26 classes / 646 total / 646 passed / 0 failures / 0 errors / 0 skipped; BUILD SUCCESS; exit 0; duração 05:01 min** |
| Maven | **69 classes / 1464 total / 1447 passed / 0 failures / 0 errors / 17 skipped; BUILD SUCCESS; exit 0; duração 09:24 min** |
| Semgrep | **Semgrep 1.178.0 / p/java / 357 arquivos / 60 regras / ~100.0% parse / 0 findings / 0 blocking / 0 errors / exit 0; duração 40.8 s; 26/26 fontes Java do delta cobertas** |
| Graphify | **graphify update .: exit 0, 31.8 s; MCP HTTP final: 6319 nós / 21666 arestas / 343 comunidades; EXTRACTED 91%, INFERRED 9%, AMBIGUOUS 0%. Novo ModeracaoConteudoService confirmado pelo get_node MCP** |
| RF18 final | PARCIAL |
| Blockers / pendências | Produtores atuais sem tipo/vínculo seguro; prova/histórico RF14; retenção e governança |
| Próximo passo | Decisão estrutural oficial, depois implementação autorizada; sem staging/commit/push |

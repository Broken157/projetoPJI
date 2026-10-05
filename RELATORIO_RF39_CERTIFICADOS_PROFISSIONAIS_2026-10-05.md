# Relatório RF39 — Certificados Profissionais

Data: 05/10/2026. Projeto Palco. Checkout: C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline.
**RF39: BLOQUEADO POR ESTRUTURA.** Auditoria e regressões concluídas; implementação não iniciada por ausência de persistência inequívoca no database05.

Evidências: [auditoria inicial](evidencias/rf39-2026-10-05/auditoria-inicial.md), [inventário estrutural](evidencias/rf39-2026-10-05/auditoria-estrutural.json), [focados](evidencias/rf39-2026-10-05/maven-focado-totais.json), [Maven completo](evidencias/rf39-2026-10-05/maven-completo-totais.json), [preservação](evidencias/rf39-2026-10-05/preservacao-final.json) e [Git final](evidencias/rf39-2026-10-05/git-final-resumo.json).

## 1. Objetivo

Auditar múltiplos certificados profissionais públicos com título, instituição, data e arquivo PDF/imagem, CRUD próprio e integração RF10/RF18/RF22/RF27. Implementar somente se semanticamente sustentado pelo banco oficial. A infraestrutura de upload é existente; a identidade profissional do certificado não é representada.

## 2. Requisitos e fontes

Fonte oficial atual: C:/Users/masca/OneDrive/Área de Trabalho/-/trabalhosAula/tecnico/pji/rf e rnf.txt, SHA-256 **3E7CA8FA7E78A1967CF5F86C86F6A60F736879B02CFBD8F9D195040FEA873183**. Lidos integralmente RF10/16/18/22/27/30/38/39/44 e RNF05/06/07/08/09/10/17; extratos e origem em requisitos-consultados.txt/requisitos-origem.json.

RF39 exige título, instituição, “Data”, arquivo PDF/imagem, múltiplos certificados, somente proprietário altera e dados privados ausentes. Sua decisão consolidada exige confirmação da estrutura específica antes de migration. RNF17: página padrão 20/máximo 50; RNF05/07/08/09/10: performance, integridade, autorização, minimização de logs e regressão, sem autorização automática de DDL.

Consultados RF38, RF16 por projetos e individual, RF18, RF22, RF44, sincronização database05, relatórios históricos de perfil/portfólio, AUDITORIA_BANCO_POS_DECISOES, MAPA_INTEGRACAO_FRONTEND_API e MAPA_OFICIAL_TELAS_FRONTEND; hashes em relatorios-consultados.json. Não localizado relatório backend RF10 específico; contratos/testes atuais de perfil são a autoridade. Relatórios antigos não comprovam o estado atual nem foram reescritos.

## 3. HEAD e checkpoint

HEAD inicial/final **eec3d1b36eea44d39dea4f0cafa787aedc4a6690**, branch integracao-recuperada-2026-09-15, upstream idêntico. Checkpoint eec3d1b contém somente RELATORIO_RF38_CURRICULO_PROFISSIONAL_2026-10-05.md. git fetch fork concluiu exit 0; divergência fork/HEAD **0/0**. Primeira tentativa no sandbox falhou SEC_E_NO_CREDENTIALS; execução autorizada concluiu sem mudar TLS/Windows.

Índice vazio; backend/database05/database04/scripts sem delta inicial. Working tree histórico deliberadamente sujo preservado. Manifesto inicial: 1450 arquivos; protegido: 1449, excluindo apenas o registro semanal autorizado para append RF39. Nenhum delta Java RF38 solto. Sem staging/add/commit/push/reset/clean/stash/restore/checkout destrutivo ou Maven clean.

## 4. Graphify inicial

MCP HTTP usado antes da decisão: **6453 nós / 22287 arestas / 359 comunidades; EXTRACTED 91%, INFERRED 9%, AMBIGUOUS 0%**. Consultas de certificado/certificate/certification/credencial/instituição, portfólio/validator/storage/ownership/perfil/moderação/exclusão/evidência preservadas em graphify-auditoria-*.json e graphify-http-inicial-*.txt.

As referências de certificado são seções de documentação histórica, não Entity/Repository/Service/Controller RF39. “Credencial” de autenticação Google não é certificado profissional. Grafo orientou a cadeia técnica; schema integral, Java e regressões decidiram. Ausência lexical ou nó ausente não foi usada como prova isolada.

## 5. Database05

Pacote ativo database05/palco-database; database04 histórico. Inspeção integral dos **46 arquivos**, incluindo init, enums, dez DDLs, índices/constraints, rotinas, triggers, seed e demais arquivos oficiais. Inventário: **43 tabelas / 24 enums**. Hashes, definições completas, referências e mappings de todos os controllers registrados em auditoria-estrutural.json; seeds não foram transcritos na auditoria.

## 6. Auditoria estrutural — A–L

| Pergunta | Resultado |
|---|---|
| A. Tabela específica de certificados | NÃO |
| B. Coluna/enum/papel CERTIFICADO | NÃO |
| C. Owner + título + instituição + data + arquivo | NÃO; entity atual é arquivo genérico |
| D. FK segura certificado → arquivo | NÃO; há arquivo → perfil artista |
| E. ARTISTA 1:N CERTIFICADOS | NÃO; existe 1:N arquivos genéricos |
| F. Vários certificados com metadados | NÃO; vários arquivos são permitidos |
| G. Data profissional distinta de data_upload | NÃO |
| H. Diferenciar certificado/currículo/PDF/imagem/projeto | NÃO |
| I. Metadados suficientes para editar certificado | NÃO |
| J. DELETE específico seguro RF39 | NÃO; DELETE genérico existente não identifica certificado |
| K. Publicidade inequívoca RF39 | NÃO; publicidade de mídia genérica existe |
| L. Moderação/evidência RF39 | NÃO; alvo direto não representado |

Decisão anterior a qualquer implementação de produção registrada em auditoria-inicial.md. Script estrutural exige reavaliação se inventário/representação mudar; classificação semântica resulta da inspeção das candidatas, não apenas da busca textual.

## 7. Tabelas candidatas

portfolio_arquivos oferece arquivo proprietário, MIME/bytes/nome/timestamp. galerias_virtuais/itens_galeria oferecem galeria e seus arquivos, sem instituição/data profissional/identidade de certificado. Título de galeria não é título RF39. perfis_artistas.url_portfolio e ultima_atualizacao são genéricos do perfil. embeds_externos é RF30, não documento comprobatório. Enum isolado de status de projeto não cria agregado RF16 ou RF39.

## 8. portfolio_arquivos

DDL 02_tables/05_portfolio_e_midias.sql: **id, artista_id, url_arquivo, nome_original, tamanho_bytes, tipo_mime, data_upload**. FK artista_id → perfis_artistas(usuario_id), ON DELETE CASCADE; índice comum por artista, sem UNIQUE que limite arquivos a um. PortfolioArquivo mapeia esses sete campos; PortfolioArquivoRepository consulta página por artista e faz lock do arquivo próprio.

URL/ref de storage e data_upload não fornecem título/instituição/data profissional. Nenhuma metadata improvisada em filename, JSON, cache, galeria, projeto ou arquivo local.

## 9. Semântica CERTIFICADO

**Ausente** em tabelas, colunas, enums e cadeia backend. Tipo IMAGEM/PDF/AUDIO do DTO é derivado do MIME e indica formato. Filename contendo certificado não cria papel profissional. Ocorrências de INSTITUICAO_PUBLICA/PRIVADA em TipoContratante são aliases de setor, sem relação com instituição emissora.

Não há certificado autenticado/verificado, instituição validada, selo RF15, prova jurídica ou serviço externo de verificação.

## 10. Cardinalidade 1:N

ARTISTA 1:N ARQUIVOS existe; **ARTISTA 0..N CERTIFICADOS com metadados não existe**. Capacidade de vários PDFs/imagens não atende RF39. Não foi imposto limite funcional ou UNIQUE por artista. RF38 principal 0..1 não foi reutilizado.

## 11. Título

Obrigatório no RF39, sem campo persistente atual. nome_original é nome de upload, não título profissional. ConteudoPublicoValidator pode ser reutilizado futuramente para null/blank/tamanho/markup/controles; limite específico deve ser escolhido e documentado após modelo oficial. Não inventado catálogo, título automático ou campo no DTO.

## 12. Instituição

Obrigatória, pública e profissional; **sem persistência atual**. Futuro texto puro obrigatório com limite técnico e prevenção RF18. Não consultar API, validar existência real da instituição ou afirmar autenticidade. Não reutilizar tipo de contratante como instituição emissora.

## 13. Data profissional

**Sem campo atual.** RF39 declara somente “Data”, sem consolidar emissão/conclusão/futuro/expiração. data_upload e ultima_atualizacao não equivalem à data profissional. Pendentes semântica fina e política para data futura; formato/data válida serão validados na futura implementação. Nenhuma regra de validade/expiração ou nome definitivo foi inventada.

## 14. Arquivo

PDF e JPG/JPEG/PNG genéricos são suportados. MP3 é aceito no portfólio, mas não no RF39; futuro contrato deve restringir a PDF/imagem já autorizada, sem vídeo/DOCX/HTML/executável renomeado. Ausente vínculo inequívoco arquivo → certificado. Não converter automaticamente todos os PDFs/imagens.

## 15. Storage

PortfolioStorageService existente: root privado fora de código/build/static, owner + UUID, referência validada, normalização/traversal, symlink/NOFOLLOW_LINKS, temporário + force + move e fallback sem ATOMIC_MOVE. DTO expõe rota backend controlada, sem storagePath/caminho absoluto/ref interna. Listagem não lê bytes; conteúdo usa endpoint separado.

Upload genérico compensa rollback; DELETE genérico faz remoção física antes do commit e tenta restaurar backup. RF22 usa cleanup AFTER_COMMIT. Nenhum segundo storage/pasta pública, XA ou promessa de recuperação durável/atomicidade absoluta.

## 16. Validator

Constantes atuais de ArquivoPortfolioValidator: **IMAGE_LIMIT 5 MiB / PDF_LIMIT 10 MiB / AUDIO_LIMIT 20 MiB**. Validador existente regredido; não copiado parser. Camada RF39 PDF/imagem não implementada porque domínio ausente. ConteudoPublicoValidator atualmente valida nome público de upload (150 caracteres), não título/instituição inexistentes.

## 17. MIME, extensão, bytes e headers

Servidor compara extensão, MIME, tamanho declarado/bytes lidos e nome seguro. PDF: prefixo %PDF-; PNG: assinatura/chunks/CRC e ImageIO; JPEG: markers e ImageIO. Essas validações não provam ausência de malware, conteúdo ativo, PII ou autenticidade.

PortfolioController preservado: PDF attachment, outros tipos inline, Content-Type/Length, filename validado, X-Content-Type-Options nosniff, Content-Security-Policy sandbox e no-store. Não há endpoint RF39 novo.

## 18. Autenticação

Infraestrutura genérica usa JWT → usuário persistido via AuthenticatedUserResolver → perfil artista. PortfolioAccessService exige ARTISTA com conta ATIVA no database05. Anônimo 401; outros papéis 403. ADMIN/MODERADOR não recebem ownership. Nenhuma autoridade derivada de artistaId arbitrário do cliente.

## 19. Autorização

Operações genéricas restringem papel e proprietário; leitura pública usa PerfilPublicoService/RF10/RF27 e situação da conta. Acesso direto por ID de arquivo também exige perfil publicável. Essas regras regredidas não constituem autorização de um recurso certificado inexistente.

## 20. Ownership

PortfolioArquivo.artista e consulta bloquearProprio(id,dono) são persistentes; arquivo alheio retorna 404 conforme padrão. Futuro certificado deve garantir owner e consistência do vínculo ao arquivo, inclusive troca/DELETE, sem confiar em JSON/artistaId. Nenhum ownership RF39 implementado.

## 21. CRUD

**CRUD RF39 AUSENTE.** PortfolioController/PortfolioArquivoService oferecem upload, listar próprios/públicos, conteúdo e DELETE genéricos; não metadados profissionais, edição ou substituição de certificado. Mappings de todos os controllers auditados, sem endpoint RF39.

## 22. Criação

Nenhuma criação RF39. Upload atual valida arquivo/nome antes de storage e saveAndFlush, com cleanup em rollback. Futuro certificado precisará validar metadata + PDF/imagem antes da persistência final e manter vínculo/owner consistentes, compensando falhas sem afirmar XA.

## 23. Edição

Não é possível editar título/instituição/data inexistentes. Futuro contrato deve atualizar coerentemente metadata, manter ID do certificado e preservar arquivo quando omitido. Não transformar edição em novo certificado silenciosamente. Não usado PUT de embed como CRUD profissional.

## 24. Substituição

**Ausente para RF39.** Novo upload genérico cria outro arquivo. Modelo futuro: validar/gravar novo, persistir referência/data técnica, commit e cleanup antigo seguro; falha pré-commit mantém antigo; cleanup falho não destrói novo e exige reconciliação. Não remover arquivo antigo antes de validar novo.

## 25. Exclusão

Existe DELETE do arquivo próprio com lock, compensação de rollback e guard de evidência de galeria (409). Não é DELETE de certificado porque papel/vínculo faltam. Não apagar todos PDFs/imagens, currículo, projeto ou arquivo por heurística. Guard cobre apenas vínculo real detectável; sem referência segura não prometer preservação integral.

## 26. Listagem privada

Há página de arquivos próprios: default 20/máximo 50, ordem dataUpload DESC/id DESC. **Sem lista privada de certificados.** Futuro RF39 deve separar metadata/conteúdo e ter paginação estável; data profissional DESC/id DESC é alternativa técnica, não ordem oficial consolidada.

## 27. Listagem pública

Arquivos genéricos paginados conforme publicabilidade do artista; não há lista RF39. Futuro público não exigirá login para perfil publicável e deverá evitar N+1/lista ilimitada/bytes na metadata. Perfil não publicável não deve expor documento por enumeração de IDs.

## 28. RF10

ArtistaPublicoResponse atual contém somente dados profissionais básicos permitidos, sem coleção/campo de certificados. Rotas de portfólio não distinguem RF39. RF10 prevê certificados conforme disponibilidade; ausência não bloqueia perfil. Certificados não foram adicionados ao perfilCompleto/RF08.

## 29. Menor e privacidade

**Pendência funcional/privacidade específica.** As fontes consultadas permitem perfil de menor autorizado com dados restritos, mas não consolidam como expor certificado binário sem dados privados. Título/instituição/data profissionais não tornam PDF/imagem seguro: pode conter nome civil, CPF, matrícula, endereço, QR, identificadores, assinatura e dados de terceiros.

Não aplicado OCR, limpeza de documento, novo consentimento ou regra definitiva prematura. Comportamento geral atual de PDF/imagem do portfólio de menor autorizado **preservado**. Não declarar privacidade RF39 resolvida pela whitelist do DTO ou pelo consentimento inicial.

## 30. RF27

Policy de menor/consentimento existente reutilizada na análise e regredida. Consentimento inicial libera funcionalidades permitidas, mantém experiência/dados restritos privados e não é autorização automática específica para qualquer binário. Sem novo fluxo, revogação, aviso ou consentimento inventado.

## 31. RF18

Prevenção existente por ConteudoPublicoValidator/upload é reutilizável, mas tipo_conteudo_enum contém **VAGA/COMUNIDADE/GALERIA/MENSAGEM**, sem CERTIFICADO/ARQUIVO/EMBED. Moderação direta atual implementada no recorte VAGA/COMUNIDADE, não certificado. RF18 permanece PARCIAL; não mapear certificado como GALERIA/VAGA nem criar enum.

## 32. Denúncia, moderação e evidência

ModeracaoConteudoRepository.arquivoComEvidencia usa vínculo real itens_galeria → galeria denunciada/moderada para proteger DELETE. Não cria alvo individual de arquivo/certificado, não interpreta URL livre como FK nem garante guarda de toda prova binária. Futuro RF39 depende do mesmo pacote oficial de alvo/evidência/retencão RF18, sem sistema paralelo.

## 33. RF22

Orquestração atual planeja limpeza de arquivos genéricos antes da deleção relacional, executa físicos AFTER_COMMIT e sinaliza CONCLUIDA_COM_LIMPEZA_PENDENTE se falhar. Cenários com evidência cuja estrutura/isolamento se perderia são recusados antes de escrever. Regressão preservada; não implementado RF22 inteiro nem declarada nova retenção jurídica.

Certificado futuro terá de integrar owner/vínculo/cleanup, evitar órfãos e preservar somente evidência autorizada. Não presumir que CASCADE resolve filesystem, moderação ou obrigação de retenção.

## 34. RF16

Projeto com capa/status/múltiplas mídias continua **BLOQUEADO POR ESTRUTURA**, independentemente de RF39. Certificado não exige projeto/capa/RASCUNHO/PUBLICADO e não será galeria renomeada. Infraestrutura individual preservada sem alterar a conclusão do RF16 revisado.

## 35. RF38

Currículo principal 0..1 continua **BLOQUEADO POR ESTRUTURA** no checkpoint documental anterior. Certificados 0..N são outro domínio. Storage pode ser compartilhado no futuro, sem compartilhar papel/cardinalidade ou converter documentos automaticamente. Relatório RF38 intacto.

## 36. RF44

Global PARCIAL; candidatura concluída e publicação depende de produtor real. Criar/editar/excluir certificado não foi conectado a aviso ao responsável, porque não há decisão funcional adicional. Upload genérico não foi tratado como publicação RF44. Sem envio externo nesta auditoria.

## 37. Frontend

Auditoria estática read-only. MAPA_OFICIAL_TELAS_FRONTEND.md:101 classifica PO-16 como aba/categoria visual parcial no Gabriel; MAPA_INTEGRACAO_FRONTEND_API.md:232/435 documenta falta de API/metadados. Revalidação atual confirma lacuna backend, sem usar demo/local como integração. Sem React/CSS/JS/layout/mock/botão/build/frontend novo.

## 38. Arquivos de produção

**Nenhum alterado.** 287 Java, entidades/configurações/scripts operacionais/dependências protegidos. Nenhum endpoint/serviço/DTO/schema/migration/índice/FK/enum/SQL novo ou workaround. Delta desta tarefa: este relatório, append RF39 no registro semanal e evidências em evidencias/rf39-2026-10-05/.

## 39. Arquivos de teste

**Nenhum Java de teste alterado/criado.** Reutilizadas 14 classes reais localizadas antes da execução; inspeção estrutural em PowerShell/documentação, sem teste Java fictício. Manifesto de **371 fontes testadas** (366 Java e cinco recursos) preservado antes/depois. Sem assertions relaxadas, testes removidos ou novos skips.

## 40. Banco alterado

**NÃO.** Database05 oficial, 47 arquivos database04 e cinco scripts database04 históricos intactos. Sem SQL/DDL/migration/patch/H2/create/update/create-drop. Testcontainers inicializa somente o pacote oficial e realiza DML de fixture/cleanup no PostgreSQL descartável; banco de desenvolvimento não foi alterado.

## 41. Frontend alterado

**NÃO.** 770 arquivos e deltas históricos preservados por hash. Histórico original do registro semanal preservado byte a byte como prefixo; somente entrada RF39 acrescentada. Nenhum outro histórico reescrito.

## 42. Database05 — 46/46

ZIP oficial C:/Users/masca/Downloads/palco-database05.zip, SHA-256 **6158c813929ac0db9b3b12030e8b9d84c3b647611986dd6d60b2fc50d0f97ebf**. Comparação byte a byte inicial/final: **46/46 idênticos / 0 divergentes / 0 ausentes / 0 adicionais**. Evidência snapshot-final-byte-a-byte.json. 1449 arquivos protegidos sem divergência; 371 fontes testadas inalteradas.

## 43. Testes focados

**291 total / 291 passed / 0 failures / 0 errors / 0 skipped; BUILD SUCCESS**. 14 classes; duração 02:34 min; exit 0.

Classes: com.portifolio.controller.DenunciaRf14Rf18IntegrationTest, com.portifolio.controller.ExclusaoContaRf22IntegrationTest, com.portifolio.controller.GuardianConsentRf27IntegrationTest, com.portifolio.controller.ModeracaoRf18IntegrationTest, com.portifolio.controller.PerfilPublicoRf10IntegrationTest, com.portifolio.controller.PortfolioMultipartHttpIntegrationTest, com.portifolio.controller.PortfolioRf16IntegrationTest, com.portifolio.Database05BootstrapIntegrationTest, com.portifolio.OfficialSchemaMappingIntegrationTest, com.portifolio.security.GenericEndpointsSecurityIntegrationTest, com.portifolio.security.JwtAuthenticationIntegrationTest, com.portifolio.service.GuardianConsentServiceTest, com.portifolio.service.PortfolioStorageServiceTest, com.portifolio.validation.ArquivoPortfolioValidatorTest.

PostgreSQL 18/Testcontainers real, PALCO_TEST_DATABASE05_PATH e ddl-auto=validate, bootstrap/mapeamento oficial. Uma bateria final única; resultados não somados à suíte completa. Testes comprovam infraestrutura/ownership/público/storage/validators/denúncia/moderação/RF22 existentes, não o CRUD RF39 ausente. Logs/.exit/totais/XMLs sanitizados preservados.

## 44. Maven completo

**1560 total / 1543 passed / 0 failures / 0 errors / 17 skipped; BUILD SUCCESS**. 73 classes; duração 10:29 min; exit 0.

Medido novamente nesta tarefa. Comparação nominal com RF38: **17 skips idênticos; zero novos; nenhuma classe antiga ausente ou quantidade alterada**. CurrentLocalSchemaIntegrationTest: 4; OfficialLocalApiIntegrationTest: 13. Lista nominal em regressao-comparacao.json. Proveniência: 46 inicializações oficiais database05 / 90 ocorrências JDBC palco_test_manu05; zero inicializações/URLs database04. ddl-auto=validate.

Sem Maven clean. Avisos Hibernate de paginação com collection fetch em memória e demais avisos de runtime ficam nos logs; BUILD SUCCESS não mede carga, p95/p99, cobertura ou performance RF39. Nenhum resultado antigo apresentado como execução RF39.

## 45. Semgrep

**Semgrep não executado nesta tarefa porque não houve delta Java de produção.** Condição explícita do prompt; sem reaproveitar scan histórico como RF39. Instalação nativa Windows não utilizada; TLS, Defender e App Control preservados. Implementação futura exige Docker oficial p/java incluindo arquivos novos. Não foi declarado scan amplo de secrets.

## 46. Graphify final

MCP HTTP confirmado ao final: **6453 nós / 22287 arestas / 359 comunidades; EXTRACTED 91%, INFERRED 9%, AMBIGUOUS 0%**.

As 371 fontes testadas coincidem com o baseline RF38; esse checkpoint não mudou Java em relação ao RF44 que originou o grafo atual. **graphify update não executado/não necessário**, nenhum Java mudou. Hash graph.json inicial/final **A010B9CAC9B1E6379ED699BEB32993E5FD4C1FD12D4B8D7C01376A6BFF28D496**. Documentação nova RF38/RF39 não indexada; sem componente profissional RF39.

Sem graphify label, graphify-mcp.exe, parser novo, reinstalação Python ou contorno de App Control. Frescura limitada ao código conferido, não a toda documentação/SQL do grafo.

## 47. Riscos

Converter PDF/imagem genérico pode mostrar/apagar documento errado e perder metadados. data_upload não serve como data profissional. Owner duplicado entre certificado/arquivo pode permitir sequestro/órfão. Documento opaco pode conter PII de adulto/menor/terceiros. Validação estrutural e headers não autenticam certificado nem eliminam malware. Banco+filesystem têm janelas de crash/compensação/cleanup; guard de galeria não cobre alvo inexistente ou prova textual.

## 48. Limitações

Sem domínio, não há teste de criar/editar/substituir/excluir certificado ou de múltiplos certificados com metadados. Sem OCR, antivírus, autenticidade, consulta externa de instituição, homologação visual, benchmark/carga, p95/p99 ou percentual de cobertura RNF10 medido. 17 testes locais condicionais mantidos. Verificação limitada de evidências: zero valores locais conhecidos >=8 caracteres e zero JWTs assinados. XMLs copiados sem properties de ambiente; não é scan amplo de secrets/PII nem análise de binários.

Não afirmar conformidade jurídica, ausência absoluta de segredos/PII ou integridade universal por teste verde. Conclusões anteriores RF11/RF16/RF17/RF18/RF22/RF38/RF44 preservadas.

## 49. Estrutura mínima necessária — conceitual

Não implementada. Próximo pacote oficial deve representar **CERTIFICADO** com ID, ARTISTA proprietário, título, instituição, data profissional, referência segura a PDF/imagem, datas técnicas criação/atualização e lifecycle de edição/substituição/exclusão. Cardinalidade **ARTISTA 0..N CERTIFICADOS**, sem unicidade por artista que reduza a um.

Arquivo inequivocamente vinculado e owner consistente; separar currículo, projeto e mídia genérica. Considerar RF10 público/paginação, RF18 alvo/evidência, RF22 cleanup/retencão e política de documento de menor. Nenhuma tabela física/coluna/constraint/SQL/migration/Entity final escolhida.

## 50. Alternativas conceituais — não aplicadas

| Alternativa | Vantagens | Riscos e decisões |
|---|---|---|
| A. Recurso dedicado de certificados do artista, ligado por FK a arquivo genérico | Domínio e metadata explícitos, vários certificados, storage reutilizado | Lifecycle recurso/arquivo, ownership duplo, órfãos e alvo de moderação |
| B. Papel profissional explícito na estrutura de arquivo + metadata associada | Menos representação paralela, infraestrutura compartilhada | Misturar currículo/certificado/projeto, campos opcionais por tipo, constraints complexas e regressão do portfólio |

Manu/grupo definem o schema oficial em pacote completo. Esta auditoria não escolhe forma física ou aplica evolução estrutural local.

## 51. RF39 final

**BLOQUEADO POR ESTRUTURA.** Critérios A/C/D/E/F de conclusão não são possíveis: persistência inequívoca, título, instituição, data profissional e vínculo comprobatório ausentes. Infraestrutura genérica verde não é RF39 CONCLUÍDO ou parcialmente entregue. Pendências adicionais: privacidade de menor, data/futuro e moderação/evidência/lifecycle.

## 52. Pendências e inspeção final

Novo pacote oficial com domínio/metadados/cardinalidade/arquivo; definição de Data e futuro; política específica de privacidade do binário de menor; integração futura RF18/RF22; API/DTO/CRUD/listas/testes depois do suporte real. Nenhuma solicitação enviada a terceiros.

**git diff --check: exit 0, stdout vazio; HEAD preservado, índice vazio e git diff --name-status -- backend vazio.** Os quatro comandos finais solicitados concluíram exit 0; stderr LF/CRLF histórico preservado separadamente.

Somente documentação/evidência alterada nesta tarefa. Originais protegidos preservados, índice vazio, sem staging/commit/push. git diff não inclui arquivos novos untracked; verificador documental também cobre esses arquivos, sintaxe de scripts, ausência de placeholders e preservação do prefixo semanal.

## 53. Próximo passo

Submeter capacidades conceituais e decisões pendentes à governança do banco/grupo. Após pacote oficial autorizado, reauditar RF39 antes de implementar; testar metadados, vários certificados, JWT/owner, edição sem arquivo/substituição/rollback, PDF/imagem, leitura pública e menor, RF18/RF22 e paginação. Só com implementação real executar Semgrep Docker e atualizar Graphify; banco/frontend continuam sujeitos ao escopo explicitamente autorizado.

# RELATÓRIO RF03 + RF04 + RF05 + RF07 — 08/10/2026

## 1. Resumo executivo

O núcleo representável de RF03/RF04/RF05/RF07 foi consolidado sobre database05, com busca por título, filtros antes da paginação, faixa real, recortes pessoais por JWT, favoritos privados, taxonomia 3/3 e edição cadastral de ENCERRADA sem reabertura. Os quatro RFs permanecem **PARCIAIS frente à baseline integral**, por C15, D08, D09 e D14; o recorte implementado possui validação própria, sem simular capacidades ausentes.

Backend: SIM, somente os arquivos da seção 9. Frontend, database05, bancos legados, migrations/SQL, enums físicos e index: NÃO alterados pela tarefa. Alterações preexistentes foram preservadas. Testes focados: 426/426, BUILD SUCCESS. Regressão completa: BUILD SUCCESS — 1714 total / 1697 passed / 0 failures / 0 errors / 17 skipped, exit 0, 11:01 minutos. Semgrep: SUCESSO: 376 arquivos, 60 regras, ~100% das linhas parseadas, 0 findings, 0 blocking, 0 erros, exit 0. Graphify: SUCESSO: AST atualizado (exit 0) e MCP final confirmado.

## 2. Branch e HEAD inicial/final

Branch: `integracao-recuperada-2026-09-15`. HEAD inicial: `774a86775b27dd03db51217759738f132ec4ad82`; HEAD final: `774a86775b27dd03db51217759738f132ec4ad82`. Commit de referência: `774a867 feat: consolida RF37 busca de perfis`.

Checkpoint executado antes da primeira edição: status, branch, HEAD, log -15, fetch fork e rev-list do branch remoto versus HEAD: 0/0. Backend e index inicialmente sem delta. Nenhum add/commit/push/reset/clean/stash/checkout foi executado. Evidência: `evidencias/vagas-2026-10-08/checkpoint-inicial.json` e `status-inicial.txt`.

## 3. Fontes e integridade

Fonte funcional atual: `C:/Users/masca/OneDrive/Área de Trabalho/-/trabalhosAula/tecnico/pji/rf e rnf.txt`, SHA-256 `08FB838914D57F1C6B54F3CFDC73DB5FE1F7CD511B4DFCA5A736DC094CA20EDA`. RF03/RF04/RF05/RF07 e dependências RF06/RF19/RF23/RF28/RF36/RF42/RF45/RF47/RF48/RF54 consultados.

Fonte física exclusiva: `database05/palco-database/`. ZIP oficial `C:/Users/masca/Downloads/palco-database05.zip`, SHA-256 `6158C813929AC0DB9B3B12030E8B9D84C3B647611986DD6D60B2FC50D0F97EBF`, confirmado. OfficialPostgreSQLContainer carrega o snapshot inteiro e valida o manifesto do working tree `c68460169fcd2538fefd34a109ee3ea7640e553ce1882dd225d88d655229c134`; não utiliza schema paralelo/H2. Fonte de decisão: código e SQL atuais, não o campo Situação nem relatórios históricos. Preservação final: 1193 arquivos comparados; apenas 16 alterações Java autorizadas em arquivos existentes, 0 alterações inesperadas; 3 fontes Java novas. Todos os 19 hashes Java iguais à versão focada/escaneada; fonte funcional e ZIP íntegros; index idêntico.

## 4. Estado inicial RF03

Feed ABERTA, moderação, prazo, cursor e limite 20/50 já existiam. A busca também consultava contratante/empresa; filtro mínimo comparava valorMinimo; personalizações e filtros de datas faltavam. Funções/especializações usavam JOIN/DISTINCT; a primeira consulta carregava entities, com risco de consultas de relações por item. Matriz anterior à edição: `auditoria-inicial.md`.

## 5. Estado inicial RF04

Criação com JWT/CONTRATANTE, uma Área, vínculos profissionais, publicação/rascunho e requisitos neutros já existiam. DTOs aceitavam 5/5; duplicatas viravam Set; contrato/experiência eram texto livre. Extremo máximo omitido era copiado do mínimo; A_COMBINAR não rejeitava valores. A mensagem do blocker plural ainda citava database04.

## 6. Estado inicial RF05

Detalhe público de ABERTA e acesso contextual do owner/artista candidato já existiam, com DTO de contratante público e endereço restrito ao owner. Candidatura contextual não era lista de candidatos. Faltavam favorito do usuário, capa explícita e ultimaAtualizacao; contexto histórico por favorito ainda não autorizava o detalhe.

## 7. Estado inicial RF07

DTO de edição separado, lock pessimista, ownership, transação e reconciliação incremental já existiam. Edição de ENCERRADA era recusada; baseline atual permite alteração cadastral sem mudança de estado. Validação monetária/taxonomia compartilhava as limitações de RF04. Timestamp físico não era exposto pelo model/DTO.

## 8. Arquivos e fluxos auditados

Controller, service, repository, specifications, DTOs de criação/edição/busca/listagem/detalhe/status, Vaga, Funcao, Especializacao, Área, CategoriaAfirmativa e enums atuais; SecurityConfig/JwtAuthFilter/AuthenticatedUserResolver, ApiExceptionHandler, ConteudoPublicoValidator, TaxonomiaProfissional, VagaPrazoPolicy, ItemSalvoRepository/SalvoService, CandidaturaRepository e produtores de notificações de status.

SQL oficial: `01_types/01_enums.sql`, `02_tables/03_tags.sql`, `02_tables/04_vagas.sql`, `03_functions/fn_buscar_vagas.sql`, `04_procedures/sp_publicar_vaga.sql`, `sp_atualizar_vaga.sql`, `sp_cancelar_vaga.sql` e triggers de compatibilidade/timestamps. Testes relacionados listados nas seções 44/47. Graphify MCP orientou a localização inicial e final; referências reais foram lidas diretamente.

## 9. Arquivos alterados

**Produção (7 existentes + 2 novos):**

- `backend/src/main/java/com/portifolio/controller/VagaController.java`
- `backend/src/main/java/com/portifolio/dto/VagaAtualizacaoRequest.java`
- `backend/src/main/java/com/portifolio/dto/VagaBuscaFiltro.java`
- `backend/src/main/java/com/portifolio/dto/VagaResponse.java`
- `backend/src/main/java/com/portifolio/model/Vaga.java`
- `backend/src/main/java/com/portifolio/repository/specification/VagaSpecifications.java`
- `backend/src/main/java/com/portifolio/service/VagaService.java`
- NOVO `backend/src/main/java/com/portifolio/repository/VagaConsultaRepository.java`
- NOVO `backend/src/main/java/com/portifolio/validation/CatalogoVaga.java`

**Testes (9 existentes + 1 novo):**

- `backend/src/test/java/com/portifolio/controller/VagaControllerRf03IntegrationTest.java`
- `VagaPublicacaoRf04IntegrationTest.java`, `VagaEdicaoRf07IntegrationTest.java`, `VagaGerenciamentoRf31IntegrationTest.java`, `ModeracaoRf18IntegrationTest.java` no mesmo diretório
- `backend/src/test/java/com/portifolio/service/VagaPrazoRf23Rf06IntegrationTest.java`
- `backend/src/test/java/com/portifolio/service/VagaServiceTamanhoTest.java`
- `backend/src/test/java/com/portifolio/controller/NotificacaoRf23IntegrationTest.java`
- `backend/src/test/java/com/portifolio/security/GenericEndpointsSecurityIntegrationTest.java`
- NOVO `backend/src/test/java/com/portifolio/controller/VagasConsolidacaoIntegrationTest.java`

19 arquivos Java no escopo, incluindo 3 untracked. Documentação/evidência nova: este relatório e `evidencias/vagas-2026-10-08/`; atualização AST em `graphify-out/`. Nenhum arquivo de RF37/RF54/RF08 foi editado.

## 10. Conflitos baseline × banco × backend

| Conflito | Tratamento |
|---|---|
| Vaga 5/5 anterior × baseline 3/3 | DTO e serviço limitados a 3/3; ARTISTA permanece 5/5 |
| requisitos NOT NULL × campo funcional removido | Texto vazio técnico quando omitido; sem copiar descrição |
| sp_publicar_vaga exige requisitos preenchidos | Caminho atual usa JPA e adaptação neutra; procedure não chamada/alterada |
| sp_atualizar_vaga bloqueia ENCERRADA | JPA adapta edição cadastral mantendo estado/histórico; procedure não chamada/alterada |
| Valor único/cópia automática × faixa real | Extremos canônicos; alias explícito somente quando coerente |
| ESTADUAL × enum sem ESTADUAL | D09 pendente; sem alias REGIONAL |
| Benefícios TEXT × catálogo estruturado indefinido | D08; sem catálogo/filtro inventado |
| Afirmativa plural × categoria singular | C15; rejeição explícita sem truncamento |
| PESSOA_NEGRA × ETNICO_RACIAL; extras LGBTQIA | D14; não ativar/relabelar categorias legadas |
| Busca por contratante × título na baseline | Título somente; empresa removida com 422 |

## 11. Taxonomia 3/3

Uma Área obrigatória e existente; no máximo 3 Funções e 3 Especializações, IDs positivos/existentes e compatíveis. JsonSetter recebe lista antes da conversão para Set, preservando a possibilidade de rejeitar duplicatas. TaxonomiaProfissional e FuncaoRepository.buscarTaxonomia já consolidados em RF54 são reutilizados. Função deve pertencer à Área; especialização deve ser compatível com ao menos uma função selecionada. Seleções inválidas não são truncadas. Seleção omitida na edição preserva vínculos válidos; [] limpa; mudança de área/função remove somente especializações órfãs. Catálogo oficial não recebeu linhas.

## 12. Criação de Vaga

POST /api/vagas exige JWT e papel CONTRATANTE. contratanteId legado não concede propriedade; owner vem do SecurityContext. Criação aceita ABERTA/RASCUNHO; demais estados recebem 422. Uma transação persiste a vaga, funções e depois especializações, respeitando o trigger BEFORE INSERT. Valores normalizados, URL/texto validados e ownership não são delegados ao cliente.

## 13. RASCUNHO

Privado, sem dataPublicacao e fora do feed/recortes de terceiros. Respeita título/descrição/Área/localização/contrato/forma/abrangência obrigatórios físicos; pode omitir modelo e experiência ainda pendentes. Não foi criada persistência de rascunho incompleto que violaria NOT NULL. Publicação posterior exige complementação. Somente owner vê/edita/exclui; salvo de terceiro não transforma rascunho em público.

## 14. Publicação

ABERTA exige modelo válido e experiência canônica, além dos campos base e remuneração coerente. Data limite informada deve ser futura. PUBLICAR continua no gerenciamento existente de RF23, sem criação de candidatura nem convites. Nenhuma reimplementação integral da máquina de estados RF23 foi feita.

## 15. Data de publicação

Definida na criação ABERTA ou na transição RASCUNHO → ABERTA. Campo enviado pelo cliente não concede controle sobre a data. Edição comum/reabertura preservam a publicação original. Rascunho mantém null; filtros por dia usam intervalo fechado no início e aberto no início do dia seguinte.

## 16. Experiência

Persistência VARCHAR preservada, usando os códigos do enum Java NivelExperiencia já existente: SEM_EXPERIENCIA, INICIANTE, INTERMEDIARIO, EXPERIENTE, ESPECIALISTA. Escrita e filtro usam allowlist fechada; sem comparação lexicográfica nem enum físico novo. Texto arbitrário como Pleno é recusado. Leitura de histórico existente não altera os dados armazenados; correção de um legado na edição requer código vigente.

## 17. Tipos de contrato

Códigos persistidos em VARCHAR: CLT, PJ, FREELANCER, TEMPORARIO, ESTAGIO, PROJETO_EVENTO. Aliases finitos dos rótulos funcionais/legados conhecidos são aceitos e convertidos na escrita. Filtro considera código e aliases históricos equivalentes, sem aceitar texto livre como novo tipo. Os seis contratos foram testados. Não foi criada tabela/enum de contrato.

## 18. Abrangência

LOCAL e NACIONAL entregues para nova escrita. Enum físico/Java preservado: LOCAL, REGIONAL, NACIONAL, MUNICIPAL, INTERNACIONAL, REMOTO. ESTADUAL não existe; requisição com esse valor é recusada e **D09 permanece pendente**. Novas escritas legadas sem decisão são recusadas com 422/D09; leitura/filtro de valor físico existente e edição que mantém a abrangência legada não relabelam o histórico. Não foi decidido REGIONAL = ESTADUAL.

## 19. Modelo de trabalho

PRESENCIAL, HIBRIDO, REMOTO reutilizam enum existente. Os três valores foram testados; valor desconhecido recebe erro de contrato. Publicação exige modelo; rascunho pode aguardar preenchimento.

## 20. Localização

Cidade e UF são normalizadas; UF de escrita usa duas letras. Filtros cidade/estado são opcionais e não distinguem maiúsculas; UF inválida é recusada. Endereço completo não é exigido para publicação e permanece restrito aos contextos do owner. Sem raio novo ou inferência geográfica.

## 21. Remuneração e faixa

formaRemuneracao + valorMinimo + valorMaximo são o contrato principal. Demais formas exigem ambos os extremos não negativos e mínimo ≤ máximo, com precisão numeric(10,2). A_COMBINAR exige null/null; valores numéricos não são fabricados nem aceitos nessa escrita. DTO também não apresenta zero de legado como remuneração conhecida em A_COMBINAR.

remuneraValor continua alias explícito de valor fixo: preenche dois extremos iguais quando coerente; divergência com faixa canônica recebe 422. Na resposta, alias só é numérico para faixa fixa, nunca substitui uma faixa aberta.

## 22. Correção dos filtros de remuneração

**Bug de faixa corrigido: SIM.** Semântica: `vaga.valorMaximo >= filtroMinimo AND vaga.valorMinimo <= filtroMaximo`. Cada extremo opcional funciona isoladamente; contato nos limites conta como interseção. A_COMBINAR é excluída de filtro numérico, inclusive legado com zero físico.

A suspeita anterior de “máximo consulta valorMinimo” exigia interpretação: comparar valorMinimo ao teto do filtro é correto para interseção. O erro concreto estava no filtro mínimo, que comparava valorMinimo em vez de valorMaximo e excluía faixas amplas. Foram testados mínimo, máximo, ambos, limite igual e ausência de interseção.

## 23. Benefícios

**PARCIAL / D08.** TEXT legado lido/escrito com validação de conteúdo; catálogo funcional estruturado não está definido. Nenhum catálogo/enum, análise de substring ou filtro semântico foi inventado. GET com beneficios recebe 422 e mensagem D08. Não declarar benefícios estruturados entregues.

## 24. Prazo

Data limite opcional, sem prazo representado por null. ComPrazo é derivado da data, sem coluna nova. Publicação e prazo novo na edição exigem futuro; alteração cadastral de vaga histórica conserva prazo vencido quando ele não foi modificado. Prazo vencido novo é recusado. Feed usa a política existente de prazo; testes RF23/RF06 cobrem encerramento automático e impedimento de nova candidatura no contexto previsto.

## 25. Capa

Capa opcional derivada da primeira URL da coleção fotos_vaga, com ordem física existente (ordem zero na primeira posição JPA). capaUrl é campo de DTO, não coluna. URLs usam ConteudoPublicoValidator; sequência/fotos continuam disponíveis. Não foi implementado upload adicional nem reutilizado nome de arquivo como metadado.

## 26. Condição afirmativa e C15

Configuração continua singular conforme database05. MULHER/PCD podem ser representadas; seleção plural recebe 422/C15 e rollback, preservando todas as escolhas anteriores. Mensagem atualizada de database04 para database05. Sem CSV/JSON/texto/truncamento.

D14 permanece: PESSOA_NEGRA não foi equiparada a ETNICO_RACIAL e LGBTQIA não foi ativada como baseline. Novas escritas dessas categorias legadas recebem 422/D14; leitura histórica e manutenção do valor já existente não alteram enum/dados. RF48 integral e condições privadas de ARTISTA estão fora deste delta.

## 27. Feed público

GET /api/vagas geral retorna ABERTA, sem bloqueio de moderação e com prazo válido segundo VagaPrazoPolicy. RASCUNHO/PAUSADA/ENCERRADA/CANCELADA não entram no content público. Dados administrativos do owner permanecem em detalhe/gerenciamento/recorte próprio; autenticação do owner não transforma o feed geral em página administrativa.

## 28. Filtros RF03

Título (titulo/busca/q), Área, Funções, Especializações, cidade, UF, modelo, contrato, experiência, abrangência física, forma/faixa de remuneração, afirmativa/categoria representável, datas de publicação/limite e comPrazo. Todos compõem a consulta antes de limite/offset/count. OR entre IDs de um filtro, AND entre filtros; cadeia profissional validada e exigida na própria vaga por EXISTS.

Busca textual consulta somente título; LIKE escapa %, _ e barra. Bindings Criteria evitam concatenação SQL. Empresa removida, beneficios/D08 e tagIds obsoleto são recusados. Sort arbitrário, proprietário/identidade pela URL e parâmetros desconhecidos recebem 400. Ordenação estável fixa por ID crescente; não foi implementado algoritmo de recomendação.

## 29. Recortes pessoais

somenteMinhasVagas: CONTRATANTE do JWT, cinco estados, dados próprios autorizados. somenteCandidatei: ARTISTA do JWT, histórico de suas tentativas e vagas, sem limitar à ABERTA. somenteFavoritas: favoritos de VAGA do JWT, sem rascunho de terceiro; pode incluir histórico não público.

Sem JWT: 401; papel incorreto: 403. Minhas + candidatei é combinação incompatível; favoritos podem ser combinados com recorte legítimo. usuarioId/ownerId/contratanteId/artistaId não são autoridades e são recusados na query geral. Relação existente é consultada; nenhum fluxo novo de candidatura/favoritar foi implementado.

## 30. Favoritos

ItemSalvoRepository existente fornece os IDs salvos em lote, limitado aos IDs da página e ao usuário autenticado. favorito é booleano para usuário autenticado; omitido para anônimo. A não recebe favorito de B. Detalhe histórico pode ser acessado por favorito legítimo do próprio usuário, preservando moderação e bloqueio de rascunho. Não houve alteração de RF19, categoria física ou item salvo.

## 31. Históricos

Recortes pessoais não usam restrição ABERTA do feed geral. Tentativas repetidas não duplicam vaga porque EXISTS é a condição, não JOIN externo. Seção legada vagasCanceladasComCandidatura mantém cursor próprio e isolamento do artista; nos recortes pessoais essa seção fica vazia para evitar apresentação duplicada. Histórico não vira feed público.

## 32. Detalhes RF05

Resposta contém faixa, forma, classificação, experiência, contrato, localização pública, prazo, fotos/capa, timestamps e condição singular representável. contratantePublico é DTO próprio; candidato autenticado recebe apenas minhaCandidaturaId/status contextual. Lista de candidatos permanece no endpoint RF45 existente. favorito segue JWT; propriaDoContratante segue vínculo persistido. Nenhuma condição privada do artista é consultada ou exposta pelo filtro afirmativo.

## 33. Visibilidade e enumeração por ID

Rascunho de terceiro: 404 mesmo com salvo indevido. PAUSADA/ENCERRADA/CANCELADA: contexto legítimo de owner/candidatura/favorito, com moderação preservada. Sem esse contexto: 404. Detalhe ABERTA preserva o contrato público existente. Similares valida antes o acesso ao recurso de origem; origem não pública sem contexto não pode ser descoberta por essa rota.

## 34. Edição RF07

PUT usa VagaAtualizacaoRequest e lock pessimista. RASCUNHO/ABERTA/PAUSADA/ENCERRADA podem editar dados conforme validação; estado permanece o anterior, ignorando estado arbitrário do payload. CANCELADA continua final/não editável. Vaga encerrada não reabre, não reinicia candidatura e não reescreve dataPublicacao. Dados fechados permanecem fora do feed geral. UltimaAtualizacao é lida do trigger real após flush/refresh.

Correção comprovada adicional: edição somente de relações não disparava UPDATE da linha Vaga, mantendo timestamp antigo. O serviço marca ultimaAtualizacao na edição para forçar a atualização dos metadados; o trigger existente sobrescreve com o timestamp autoritativo do banco. flush/refresh retornam o valor físico. Campo não é controlável pelo payload. O teste mantém título/dataPublicacao e compara o timestamp efetivo com a linha física.

## 35. Ownership

Identidade é resolvida pelo JWT validado. Sem sessão recebe 401, papel indevido 403, terceiro editando recebe 403, recurso inexistente 404. ID fornecido pelo cliente não transfere propriedade nem seleção de favoritos/histórico. Contratante e ID da vaga são mantidos na edição. API privada e serviço aplicam controle, independentemente do frontend.

## 36. Histórico de candidaturas

Edição comum não chama salvar/remover candidaturas, nem altera tentativa, status, mensagem, link ou data. Teste compara a linha histórica completa antes/depois da edição ENCERRADA; regressões existentes de RF06, prazo e cancelamento foram executadas. Convite continua convite; RF42 não foi alterado. Máquina física PENDENTE/EM_ANALISE/RETIRADA/CANCELADA_POR_VAGA e demais estados não recebeu enum novo.

## 37. Transações e rollback

Validação de entrada antes da reconciliação de associações; categorias afirmativas/compatibilidade e limites avaliados antes do diff destrutivo. Funções novas são materializadas antes de novas especializações; especializações removidas/novas antes da retirada de função antiga; vínculos mantidos não desaparecem temporariamente. Erro provoca rollback integral. Testes verificam título, funções, especializações, categoria e publicação após rejeição. EntityManager.refresh lê timestamp gerado pelo banco.

## 38. Queries e N+1

VagaConsultaRepository seleciona IDs escalares ordenados/paginados no banco. Carga seguinte usa EntityGraph existente com funções/contratante/usuário/Área, apenas IDs da página; especializações e fotos usam BatchSize 50. Favoritos usam uma consulta em lote por seção/página. Classificação profissional, candidatura e favorito usam EXISTS; count aplica os mesmos filtros sem multiplicação.

Teste PostgreSQL compara statements de páginas 10 e 50 com 3 funções/3 especializações por vaga e favoritos: crescimento no máximo 1 e menos de 16 statements em 50, total=55 sem duplicação. Limite/offset SQL confirmado. Não é teste de carga/p95/100 usuários; não se afirma essa medição. Warnings de paginação em memória de RF19 existente não foram tratados como correção de RF03, nem suprimidos.

## 39. Paginação

Default 20, máximo 50, size acima de 50 limitado a 50; size≤0 é recusado com 400. Cursor ID crescente preservado; page opcional com offset limitado ao intervalo inteiro, sem combinar com cursor. totalElements considera o conjunto filtrado autorizado, independente do cursor. size+1 determina hasMore sem carregar todo o resultado. Páginas sem repetição e count com relações múltiplas foram testados.

## 40. Compatibilidade da API

Rotas existentes preservadas. Alias busca/titulo, faixaSalarialMin/Max e remuneraValor seguro mantidos; q e valorMinimo/Maximo adicionados como filtros equivalentes, com rejeição se aliases divergem. Null/omitido/[] da seleção têm semântica explícita. campos legacy owner/status/publicação não são autoridade.

Mudanças intencionais: empresa deixa de pesquisar contratante; sort/identidade/filtros desconhecidos recusados; size não positivo recusado; duplicatas e seleção >3 não deduplicadas/truncadas; contratos/experiência canônicos, faixa incompleta recusada. Frontend precisará adotar esse contrato em outra tarefa; nenhum arquivo frontend foi alterado aqui.

## 41. Database alterado

**NÃO pela tarefa.** Database05, fontes SQL, enums, constraints, índices, functions/procedures/triggers e catálogo preservados por hashes. A diferença preexistente em `database/02_tables/04_vagas.sql` continua intacta, sem reutilizá-la como fonte atual. `git diff --name-status -- database05`: sem saída. Mapeamento da coluna ultima_atualizacao já existente não constitui mudança de banco.

## 42. Frontend alterado

**NÃO pela tarefa.** Frontend versionado e cliente operacional palco-comunidades-agenda protegidos por manifesto. README e os 25 arquivos operacionais já modificados, além de assets/untracked anteriores, foram preservados. Build Maven copia recursos já existentes para target; não executa build/alteração do código frontend. `git diff --name-status -- frontend`: sem saída.

## 43. Migration/SQL alterados

**NÃO.** Nenhuma migration, arquivo SQL, seed ou schema paralelo foi criado/editado. Fixtures de integração inserem/removem dados normais somente no PostgreSQL descartável oficial, como autorizado. Não foram usados ddl-auto=create/update, ALTER TABLE ou evolução de enum. Index Git: intacto, cached vazio e SHA-256 igual ao checkpoint.

## 44. Testes criados e alterados

Nova classe VagasConsolidacaoIntegrationTest: 66 casos reais, incluindo contratos/experiência/modelos/formas, faixa e aliases, 3/3, quarta seleção/duplicação, rollback, datas, recortes, favoritos/detalhe, count/página/statement e edição histórica. Não substitui a regressão antiga.

Classes existentes editadas constam na seção 9: expectativas funcionais, fixtures de publicação e adaptação do construtor. OfficialPostgreSQLContainer e os recursos de testes não foram alterados; ddl-auto=validate e snapshot íntegro permanecem exigidos.

## 45. Testes antigos atualizados e motivos

RF03: busca por contratante substituída por título; parametro artistaId agora explicitamente rejeitado sem perder isolamento do histórico; size0 recusado; experiência de CRUD canônica e abrangência ativa. RF04: mensagem C15/database05, experiência válida na fixture e quarta especialização recusada. RF07: duplicatas recusadas antes do diff; contrato/experiência canônicos; ENCERRADA incluída nas edições permitidas, CANCELADA continua proibida.

RF23/prazo/moderação/notificações e teste positivo de segurança: fixtures de publicação recebem SEM_EXPERIENCIA obrigatório. VagaServiceTamanhoTest adapta três dependências novas e mantém cobertura default/max, substituindo default para números não positivos por rejeição. Nenhum cenário de ownership, histórico ou rollback foi removido para obter verde. Rodadas diagnósticas preservadas: compilação ajustou fixture do construtor; primeira execução detectou telefone NOT NULL da fixture nova e expectativas substituídas.

A primeira regressão completa executou 1713 testes e detectou duas fixtures de publicação/reabertura sem experiência, em NotificacaoRf23IntegrationTest e GenericEndpointsSecurityIntegrationTest. Ambas receberam SEM_EXPERIENCIA, preservando as asserções de notificação/segurança. Especialização inexistente agora é 404; associação existente incompatível é 422. Resultados finais das rodadas posteriores constam abaixo; a falha inicial não foi ocultada.

A rodada completa intermediária foi interrompida antes de terminar para verificar esse caso de metadados. Teste isolado reproduziu timestamp idêntico após alterar apenas vínculos; diagnóstico em maven-timestamp-diagnostico.log. A correção permaneceu no model/service já incluídos, sem SQL novo; o teste adicional integra a rodada focada e completa finais.

## 46. Comandos e ambiente

PowerShell, JDK `C:/Program Files/Java/jdk-21.0.12`, Maven Wrapper do projeto, Docker/Testcontainers PostgreSQL 18-alpine (servidor 18.4 observado). Backend atual usa Hibernate 7.2.12.Final; APIs Criteria atuais consultadas via Context7/Jakarta Persistence, sem atualização de dependências.

Comandos: checkpoint Git da seção 2; `.\\mvnw.cmd '-Dtest=<15 classes da seção 47>' test`; depois `.\\mvnw.cmd test`; `evidencias/vagas-2026-10-08/Executar-Semgrep.ps1`; Graphify instalado `graphify update .` e MCP final; diff --check, name-status backend/frontend/database05/database e cached/status no fechamento. Logs de cada etapa no diretório de evidência. Nenhuma ferramenta foi instalada/reinstalada; nenhum controle de segurança foi desativado.

## 47. Testes focados

**426 total / 426 passed / 0 failures / 0 errors / 0 skipped — BUILD SUCCESS**, 15 classes, 3:22 minutos. Evidências: `maven-focado-final.log`, `maven-focado-final.exit` (0), `maven-focado-final-resumo.json`.

| Classe | Casos |
|---|---:|
| CandidaturaControllerRf06IntegrationTest | 57 |
| ModeracaoRf18IntegrationTest | 38 |
| PerfilTaxonomiaRf54Rf08IntegrationTest | 27 |
| SalvoRf19IntegrationTest | 25 |
| VagaCancelamentoRf25IntegrationTest | 30 |
| VagaControllerRf03IntegrationTest | 31 |
| VagaDetalhesRf05IntegrationTest | 28 |
| VagaEdicaoRf07IntegrationTest | 17 |
| VagaGerenciamentoRf31IntegrationTest | 27 |
| VagaPublicacaoRf04IntegrationTest | 26 |
| VagasConsolidacaoIntegrationTest | 66 |
| VagaPrazoRf23Rf06IntegrationTest | 9 |
| VagaServiceTamanhoTest | 8 |
| NotificacaoRf23IntegrationTest | 11 |
| GenericEndpointsSecurityIntegrationTest | 26 |

## 48. Maven completo

BUILD SUCCESS — 1714 total / 1697 passed / 0 failures / 0 errors / 17 skipped, exit 0, 11:01 minutos. Executado após a rodada focada verde, sobre o manifesto Java `java-validado-manifesto.json`. Logs: `maven-completo.log`, `maven-completo.exit`, `maven-completo-execucao.json`, resumo de XMLs atuais em `maven-completo-resumo.json`. Resultados de relatórios antigos/target-runtime não foram somados como validação nova. Execução encerrada em 08/10/2026 às 18:20:55 (-03:00); os 77 XMLs considerados foram escritos depois do início desta rodada (18:09:51). Totais coincidem com o resumo do Maven; somente os 17 casos locais opcionais abaixo não foram executados.

## 49. Total / passed / failures / errors / skipped

| Execução nova | Total | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Focada final | 426 | 426 | 0 | 0 | 0 |
| Completa final | 1714 | 1697 | 0 | 0 | 17 |

Skipped condicionais preexistentes: 4 casos de CurrentLocalSchemaIntegrationTest porque palco.current-db-tests não foi definido; 13 casos de OfficialLocalApiIntegrationTest porque palco.official-db-tests não foi definido. Nenhum skip foi introduzido pela tarefa. Essas verificações opcionais contra bancos locais não foram executadas; a persistência do recorte foi validada no PostgreSQL/Testcontainers com o database05 oficial e ddl-auto=validate.

## 50. Semgrep

SUCESSO: 376 arquivos, 60 regras, ~100% das linhas parseadas, 0 findings, 0 blocking, 0 erros, exit 0. Workflow pelo Docker oficial, versão 1.178.0, imagem imutável `semgrep/semgrep@sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b`. Ruleset p/java local, 60 regras, SHA-256 `5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1`.

Backend main + test, incluindo fontes novas untracked; --no-git-ignore e ignore vazio montado somente dentro do container; mounts read-only e --network none. Instalação nativa Windows não utilizada. Rulesets foram previamente obtidos pelo Windows com TLS normal; TLS não foi desabilitado. Cobertura: 19/19 arquivos Java alterados cobertos, incluindo 3 novos untracked; missingJavaFiles vazio.

**0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.** Não foi executado scan secrets nesta tarefa. JSON/log/resumo/exit preservados no diretório de evidência.

## 51. Graphify

Inicial: Graphify MCP, 6655 nós; busca scoped de controller/service/repository/policy/DTOs/testes e dependências, armazenada em `graphify-mcp-inicial.json`. Final: SUCESSO: AST atualizado (exit 0) e MCP final confirmado. 6737 nós, 23334 arestas e 358 comunidades; 91% EXTRACTED/9% INFERRED. A extração SQL de 45 arquivos não contribuiu ao grafo porque tree_sitter_sql não está instalado; esses arquivos foram auditados diretamente. Rótulos semânticos não foram renovados com LLM; nenhuma ferramenta adicional foi instalada.

AST orienta navegação/dependências; código/SQL/testes decidem a conclusão. Não se usa o grafo como prova de execução das regras nem de aderência integral ao requisito.

## 52. Blockers

**C15:** database05 armazena uma categoria afirmativa, sem relação plural da vaga. Menor capacidade ausente: relação tipada vaga ↔ categorias com integridade; depende da decisão D14; nenhum SQL criado.

**D08:** catálogo/regra dos benefícios estruturados não definido. Sem definição, não é seguro inventar opções nem estrutura. Campo textual disponível não prova esse catálogo.

**D09:** sem decisão formal para ESTADUAL versus enum físico legado. Necessário decidir semântica/mapeamento antes de definir se evolução física mínima é necessária.

**D14:** representação de PESSOA_NEGRA e tratamento dos valores ETNICO_RACIAL/LGBTQIA precisam de decisão. Não reinterpretar dados históricos nem ativar categorias fora da baseline.

## 53. Riscos

Clientes antigos que enviam empresa, seleção 5/5, texto livre Pleno, faixa incompleta ou abrangência legada nova receberão erros intencionais. Integração visual não foi executada porque frontend está fora do escopo. Histórico legado não é reescrito; edição exige o contrato válido atual. Campo textual benefícios não oferece catálogo estruturado.

Queries foram medidas funcionalmente por statements; carga/latência p95 não foi medida. Warnings SQL/procedures antigos e paginação em memória fora do bloco não foram silenciados. Semgrep e Graphify não substituem testes de autorização/negócio. Nenhum índice novo foi proposto sem medição.

## 54. Pendências

Resolver C15/D08/D09/D14 no fórum funcional/novo pacote oficial; preservar categorias históricas. RF54 ainda depende de dados completos do catálogo quando sua fonte oficial não contém todas as opções; esta tarefa não inseriu dados oficiais faltantes. Sincronizar frontend em tarefa própria com contratos e limites vigentes.

RF06/RF19/RF23/RF28/RF36/RF42/RF45/RF47/RF48 completos não foram implementados nem reclassificados aqui; apenas interfaces/regressões necessárias foram preservadas. Recomendação/ranking não integra a busca. Confirmação final Git/manifesto: aprovado: 1193 arquivos protegidos comparados, 0 deltas inesperados, 19 hashes Java iguais à versão validada; database05/frontend sem delta, banco legado com apenas o delta preexistente, cached vazio e index com SHA-256 idêntico ao checkpoint.

## 55. Conclusão individual RF03

**PARCIAL na baseline integral; núcleo representável consolidado e validado.** Feed, título, filtros físicos, faixa/interseção, datas, 20/50, count sem duplicação e recortes/favoritos por JWT entregues. Benefício estruturado D08, ESTADUAL D09 e afirmativa plural/semântica C15/D14 impedem declarar 100% do RF03.

## 56. Conclusão individual RF04

**PARCIAL na baseline integral; criação/publicação/rascunho representáveis consolidados e validados.** JWT, 3/3, contratos/experiência, LOCAL/NACIONAL, faixa/A_COMBINAR, prazo/capa e transação funcionam. C15/D08/D09/D14 continuam explícitos. Banco oficial não foi ajustado por conveniência da implementação.

## 57. Conclusão individual RF05

**PARCIAL na baseline integral; detalhe do domínio representável consolidado e validado.** DTO correto de faixa/capa/timestamp, privacidade, contexto legítimo, favorito por JWT e candidatura contextual preservados. Detalhe não inventa benefício estruturado, alcance ESTADUAL ou categorias plurais ausentes; depende de C15/D08/D09/D14 para apresentar o domínio integral.

## 58. Conclusão individual RF07

**PARCIAL na baseline integral; edição representável consolidada e validada.** Owner, lock, 3/3, faixa, rollback/diff, edição ENCERRADA, timestamps e histórico preservados. CANCELADA continua final e edição não muda status. Edição das capacidades ausentes segue C15/D08/D09/D14; não foi implementada evolução de banco.

## 59. Próximo passo

Revisar este delta e contratos da API, resolver as quatro dependências com as decisões/menor pacote oficial necessário e só então completar o domínio integral. O núcleo atual pode seguir para integração autorizada com frontend; essa integração não ocorreu nesta tarefa. Não fazer staging/commit/push sem autorização posterior.

## 60. Registro semanal — 08/10/2026

Objetivo: consolidar RF03/RF04/RF05/RF07 sem alterar database05/frontend. Dependências consultadas: RF06/RF19/RF23/RF28/RF36/RF42/RF45/RF47/RF48/RF54.

Entrega real: busca por título e filtros paginados, recortes autenticados, favoritos privados, faixa canônica/interseção, taxonomia 3/3, experiência/contrato fechado, criação/rascunho/publicação, edição de ENCERRADA sem mudança de estado e preservação transacional. Backend: 9 arquivos de produção; testes: 10 arquivos, 66 novos casos. Código RF37/RF54/RF08 preservado.

Validação nova: focados 426/426; completo BUILD SUCCESS — 1714 total / 1697 passed / 0 failures / 0 errors / 17 skipped, exit 0, 11:01 minutos; Semgrep SUCESSO: 376 arquivos, 60 regras, ~100% das linhas parseadas, 0 findings, 0 blocking, 0 erros, exit 0; Graphify SUCESSO: AST atualizado (exit 0) e MCP final confirmado; diff --check aprovado (exit 0, sem saída); verificação adicional dos 19 Java e deste relatório também sem whitespace ou marcadores de conflito. Database05/frontend/bancos legados/migrations/index: preservados pela tarefa. Alterações preexistentes não foram removidas.

Decisões: owner JWT; feed público ABERTA; histórico pessoal separado; 3/3 Vaga; min/max principal; A_COMBINAR sem zero; requisitos físico neutro; capa derivada; relações por diff. Pendências C15/D08/D09/D14 mantidas. RF03/RF04/RF05/RF07 permanecem PARCIAIS frente à baseline integral. Nenhum commit/push.

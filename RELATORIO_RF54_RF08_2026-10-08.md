# RF54 + RF08 — catálogo profissional e perfil — 08/10/2026

## 1. Resumo executivo

Implementado e validado o núcleo profissional de RF08 e o contrato de catálogo RF54 sobre o database05: seleção por Área, Principal única, limites 5/5 por Área, compatibilidade oficial, experiência, reconciliação de vínculos e completude sem raio/foto/portfólio. Testes focados finais: **177/177, zero failures/errors/skipped, BUILD SUCCESS**. Regressão completa: **1600 total / 1583 passed / 0 failures / 0 errors / 17 skipped condicionais, BUILD SUCCESS**. Semgrep Docker: **372 arquivos / 60 regras / 0 findings/errors**. Graphify AST atualizado: **6616 nós / 22756 arestas / 360 comunidades**. Preservação e diff --check aprovados.

Não houve implementação de RF26/RF27/RF37/RF47/RF48/RF53 completos, nem alterações em banco/frontend/index. A carga integral de RF54 continua pendente do pacote oficial. Limites 3/3 de Vagas continuam no recorte RF04/RF07; não se declara concluída essa parte de RF54 nesta tarefa.

## 2. Branch e HEAD inicial/final

Branch `integracao-recuperada-2026-09-15`. HEAD inicial e final confirmado: `d708cf000f8f33f69929c6a6d13753343959c5b8` (`docs: consolida auditoria estrutural da baseline`). Ancestral `d708cf0` confirmado; `git fetch fork` concluído e divergência inicial `0/0`. Sem staging, commit ou push. Index vazio e byte a byte preservado (SHA-256 inicial/final AF729AE8035DFCFA8FD0E08D87BDD266DD6CFC6E84DEA41555A1C2AE525F9228).

Estado inicial e arquivos preexistentes estão em `evidencias/rf54-rf08-2026-10-08/status-inicial.txt`: README, `database/02_tables/04_vagas.sql`, alterações de `palco-comunidades-agenda` e documentação/artefatos não rastreados anteriores foram preservados. Backend inicialmente sem delta; index vazio. A auditoria anterior e a fonte RF/RNF não foram reescritas.

## 3. Fontes utilizadas

- RF/RNF completo: `C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt`. SHA-256 **08FB838914D57F1C6B54F3CFDC73DB5FE1F7CD511B4DFCA5A736DC094CA20EDA**, conferido antes de implementar.
- [Auditoria estrutural consolidada](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/AUDITORIA_DATABASE05_BASELINE_RF_RNF_2026-10-08.md>), especialmente matriz RF08/RF54, catálogo e fila imediata.
- Exclusivamente `database05/palco-database/`. ZIP `C:\Users\masca\Downloads\palco-database05.zip`: SHA-256 **6158C813929AC0DB9B3B12030E8B9D84C3B647611986DD6D60B2FC50D0F97EBF** confirmado. Bootstrap de cada PostgreSQL valida os **46 arquivos** pelo fingerprint **c68460169fcd2538fefd34a109ee3ea7640e553ce1882dd225d88d655229c134**, depois executa `init.sql`/seed oficiais sem patch.
- Código e testes atuais; Graphify MCP inicial: **6453 nós**, consulta aos modelos/serviços/taxonomia/testes orientou a navegação. Ausência/presença sempre confirmada diretamente nas fontes.
- Context7, `/spring-projects/spring-data-jpa`: documentação primária de [transações](https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/transactions.adoc), [locks](https://github.com/spring-projects/spring-data-jpa/blob/main/src/main/antora/modules/ROOT/pages/jpa/locking.adoc) e flush. Consultados limites de transação no service e lock pessimista por query.

## 4. Estado inicial do RF54

Estrutura suficiente: `areas_artisticas`, `funcoes`, `especializacoes`, `funcao_especializacao` e as três relações profissionais. Sete Áreas presentes; apenas **4 Funções, 4 Especializações e 4 relações** no seed oficial. Listagem pública de Áreas existente, Funções apenas lista plana pública/consulta por Área restrita ao contratante no Banco, Especializações públicas compatíveis já existentes. O déficit do catálogo integral é de **dados**, não de schema.

## 5. Estado inicial do RF08

Ownership por JWT/path já existente. DTO plano não representava múltiplas Áreas/experiência/especializações; service recusava edição multiárea/troca, exigia raio. Completude exigia portfólio/raio e aceitava apenas uma especialização compatível, mesmo havendo outras inválidas. Bio de CONTRATANTE já era opcional. Auditoria **EXISTE/PARCIAL/FALTA/LEGADO/NÃO DEVE SER ALTERADO** registrada antes do primeiro delta em `auditoria-inicial.md` na pasta de evidências.

## 6. Arquivos auditados

Modelos Usuario, PerfilArtista, PerfilContratante, AreaArtistica, Funcao, Especializacao, PerfilArtistaArea/Id e NivelExperiencia; requests/responses de perfil e catálogo; repositories de usuário/perfil/área/função/especialização; PerfilArtistaService, PerfilCompletoService, PerfilContratanteService, FuncaoService, TalentoService, UsuarioService e AuthService nas integrações diretas; controllers de perfil/área/função/talento; SecurityConfig, AuthenticatedUserResolver, JwtService e ApiExceptionHandler.

SQL oficial de taxonomia/perfis, UNIQUE parcial de Principal, FKs compostas, seed, `fn_verificar_perfil_completo`, procedimentos e triggers relevantes. Testes PerfilEdicaoRf08, PerfilCompletoService, AreaArtistica, CadastroRf01Rf24, ValidatedOfficialSql, OfficialSchemaMapping, bootstrap OfficialPostgreSQLContainer e testes condicionais locais/oficiais. RNF02/05/06/07/08/10/13/17 foram consultados; nenhuma recomendação ou classificação sensível nova foi implementada.

## 7. Arquivos alterados

**Backend alterado: SIM. 13 fontes de produção** (9 alteradas, 4 novas), mais 3 fontes de testes:

- [FuncaoController](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/controller/FuncaoController.java>) e [FuncaoService](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/service/FuncaoService.java>).
- [PerfilArtistaRequest](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/PerfilArtistaRequest.java>), [PerfilArtistaResponse](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/PerfilArtistaResponse.java>), novos [PerfilArtistaAreaRequest](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/PerfilArtistaAreaRequest.java>) e [PerfilArtistaAreaResponse](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/PerfilArtistaAreaResponse.java>).
- [FuncaoRepository](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/repository/FuncaoRepository.java>), [PerfilArtistaRepository](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/repository/PerfilArtistaRepository.java>) e [UsuarioRepository](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/repository/UsuarioRepository.java>).
- [PerfilArtistaService](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/service/PerfilArtistaService.java>), [PerfilCompletoService](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/service/PerfilCompletoService.java>), novo [PerfilProfissionalService](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/service/PerfilProfissionalService.java>) e nova [TaxonomiaProfissional](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/validation/TaxonomiaProfissional.java>).

**3 fontes de testes** na seção 21. Este relatório, evidências de execução e derivados Graphify são separados do código. Lista exata e hashes em `escopo-autorizado.json`/`fontes-final-sha256.json` na pasta de evidências.

## 8. RF54 — implementação realizada

Preservadas `GET /api/areas`, lista plana `GET /api/funcoes` e `GET /api/talentos/especializacoes?areaId=...&funcaoIds=...`. Acrescentada consulta pública `GET /api/funcoes?areaId=...&page=0&size=20`: resposta paginada, default 20/máximo 50, ordem estável por ID, DTO mínimo id/areaId/nome. IDs/nomes/relações vêm exclusivamente dos registros existentes. Não há endpoint de escrita de catálogo, aliases, texto livre convertido em entidade ou carga automática.

## 9. RF08 — implementação realizada

`areas` representa todas as Áreas desejadas. Cada item contém areaId, principal, nivelExperiencia, funcaoIds e especializacaoIds. Zero seleções por Área permitem edição incompleta; completar exige classificação principal coerente. Podem ser usadas as sete Áreas; não há limite artificial de secundárias. Biografia/cidade/UF/banner, subtipo, raio físico opcional e disponibilidade mantêm os fluxos existentes. Não se editam dados pessoais sensíveis por este DTO.

## 10. Área Principal

Uma seleção não vazia exige **exatamente uma Principal explícita**. Duas/nenhuma Principal são rejeitadas; secundária não é promovida silenciosamente. Remoção da Principal com Áreas remanescentes exige nova escolha na mesma operação. A seleção vazia representa edição incompleta, nunca perfil completo. Ao trocar, o backend demove a antiga e faz flush antes da promoção para respeitar a ordem do UNIQUE parcial; as etapas ficam invisíveis a outras transações até o commit. Não há commit intermediário nem remoção da constraint.

## 11. Limites 5 Funções / 5 Especializações

Centralizados em `TaxonomiaProfissional`: máximos absolutos por Área, aplicados na validação e no cálculo de coerência. Duas Áreas podem conter dez Funções no total. Nova lista com IDs duplicados é rejeitada; Set plano legado mantém normalização segura existente. Sexta opção é rejeitada antes de consulta/destruição de vínculos. Catálogo futuro 5/5 e especialização compartilhada foram testados em memória, sem inserir dados de catálogo no PostgreSQL.

## 12. Validação Área → Função → Especialização

Catálogo necessário é carregado em lote. IDs inexistentes retornam **404**, preservando o contrato existente de recurso de catálogo ausente; combinação, principal, duplicidade ou limite inválidos retornam **422**. Input malformado/paginação inválida segue **400** global. Toda Função deve pertencer à Área; toda Especialização deve ser admitida por pelo menos uma Função selecionada na mesma Área. Não basta a FK de especialização. Payload profissional novo e plano simultâneos são rejeitados para evitar ambiguidade.

## 13. Experiência

**experiência persistida por Área no database05** (`perfil_artista_area.nivel_experiencia`). Enum oficial preservado: SEM_EXPERIENCIA, INICIANTE, INTERMEDIARIO, EXPERIENTE, ESPECIALISTA. Valor informado atualiza a Área; omitido/nulo preserva experiência existente. Principal completa exige experiência; secundárias podem permanecer em edição. Sem experiência global ou coluna por Função.

## 14. Remoção/revalidação de vínculos

Reconciliação por Área, sem apagar e recriar todo o perfil. Áreas omitidas de `areas` são removidas com suas relações; outras Áreas preservam experiência, vínculos e timestamps. Listas omitidas preservam os vínculos atuais compatíveis; lista vazia remove aquela seleção. Se Funções mudam e especializações são omitidas, só as incompatíveis são podadas. Especialização expressamente enviada e incompatível é rejeitada. Especialização aceita por outra Função da mesma Área é mantida. Timestamp da Área só muda quando seu estado muda.

## 15. Cálculo de perfil_completo

Mantidos cadastro/identidade, CPF do ARTISTA, subtipo, CNPJ quando subtipo empresarial, biografia, cidade/UF e Principal com experiência/Função/Especialização. **Raio, foto e portfólio não são obrigatórios; disponibilidade false não invalida perfil**. Toda seleção principal/secundária deve respeitar limites e compatibilidade; uma especialização válida não encobre outra inválida. Bio CONTRATANTE segue opcional; localização/identidade e campos institucionais já exigidos permanecem.

Recalcula no servidor após flush do estado final. Não muda automaticamente o estado da conta. `fn_verificar_perfil_completo` e procedures oficiais permanecem intactos; o fluxo Java RF08 não chama o procedimento legado de substituição de vínculos. Não se afirma que escritas SQL externas passaram a obedecer aos novos limites de aplicação.

## 16. Segurança/ownership

Identidade resolve do JWT validado/SecurityContext. Path deve corresponder ao titular; usuarioId é opcional e, se informado, só verifica correspondência, sem conceder autoridade. Terceiro recebe 403, sem JWT 401, perfil inexistente 404 e duplicidade de criação 409. GET de diretórios privados permanece bloqueado; detalhes profissionais pertencem ao titular. DTO não retorna entidades JPA/CPF/CNPJ/e-mail/responsável/afirmativas. Medalha/score enviados continuam ignorados. Nenhuma regra depende de JavaScript.

Não foi acrescentada edição de username (inexistente no fluxo atual). Cadastro continua validando formato/unique; regressões RF01/RF24 foram executadas. E-mail direto de UsuarioService é pendência RF26, sem ampliação. Senha, responsável e afirmativas continuam nos RFs especializados.

Revisão final do delta:

| Pergunta | Conclusão verificada |
|---|---|
| Identidade vem do JWT? | Sim, resolver a partir do SecurityContext validado |
| Algum userId enviado concede autoridade? | Não; identificador opcional apenas confere correspondência ao titular |
| Terceiro consegue alterar perfil? | Não, casos negativos 403; sem JWT 401 |
| Função cruzada/especialização incompatível persistem pela API? | Não, validação integral antes do diff, 422 e rollback |
| Limites 5/5 podem ser burlados pelo payload/concorrência? | Não nos fluxos RF08 testados; limites por Área e serialização no usuário |
| Duas Principais persistem? | Não: validação Java + UNIQUE parcial preservado + troca testada em PostgreSQL |
| Completude exige raio/foto/portfólio? | Não, testes com todos ausentes; bio CONTRATANTE opcional |
| Disponibilidade false oculta por efeito deste delta? | Não; persistência/completude e regressão RF37 verificadas |
| DTO expõe CPF/CNPJ/responsável/afirmativas? | Não, whitelist profissional e testes de ausência; entidades não serializadas |
| Há N+1 óbvio no fluxo crítico? | GET profissional até 6 statements no cenário de duas Áreas; catálogo resolvido em lote |
| Alguma regra depende apenas de JavaScript? | Não, validações e completude executadas no backend |

## 17. Transações, concorrência e performance

Criar/editar: autenticar/autorizar → bloquear linha do usuário com PESSIMISTIC_WRITE → refresh do usuário carregado antes da espera → carregar perfil profissional → validar conteúdo/localização e toda seleção → reconciliar → flush → recalcular completude → commit. Sem transações novas/commits parciais. Qualquer exceção provoca rollback, inclusive após demissão/flush da antiga Principal.

Mesmo lock serializa edições do mesmo artista; não é criado índice. Testes concorrentes validam duas trocas simultâneas e tentativa de sexta opção. Teste determinístico segura a linha da conta, modifica telefone em outra transação enquanto a edição já leu o usuário e confirma preservação do telefone após desbloqueio.

EntityGraph do fluxo profissional carrega Área/Funções/Especializações e relações de compatibilidade; resolução de catálogo é em lote. Teste do GET com duas Áreas/quatro Funções impõe **até 6 statements SQL**, evitando consulta por item. Não é benchmark de carga, cache novo ou prova de performance de todos os outros RFs.

## 18. Compatibilidade da API

Campos e respostas planos mantidos; `funcaoIds` de resposta agrega Funções de todas as Áreas, como anteriormente, e `areaPrincipalId` continua disponível. Request plano edita a Área explicitamente indicada preservando secundárias; sem seleção profissional enviada, mantém relações. Novo `areas` acrescenta capacidade sem mudar endpoints. Scalars mantêm semântica PUT já existente; campos opcionais omitidos podem ser limpos conforme o contrato anterior. `nivelExperiencia` e listas por Área têm preservação explícita descrita nas seções 13/14.

Frontend antigo pode continuar usando o contrato plano, mas não oferece automaticamente edição multiárea/especializações/experiência. Interface futura deverá adotar `areas`; nenhuma integração visual foi declarada nesta tarefa. Compatibilidade entre ambos os formatos é coberta por testes.

## 19. Banco alterado: NÃO

database05, database legado, seeds, funções, procedures, triggers, enums, constraints, índices, migrations e SQL preservados. PostgreSQL dos testes é **descartável**, inicializado pelo pacote oficial; somente fixtures/transações de teste alteram esses bancos efêmeros. Mantido `spring.jpa.hibernate.ddl-auto=validate`. Nenhum SQL/migration/patch de banco criado. Verificação final: **795 arquivos protegidos sem diferença de hash**; database05 sem delta. `database/02_tables/04_vagas.sql` permanece apenas com o delta anterior, hash preservado. Requisitos/ZIP mantêm os hashes da seção 3.

## 20. Frontend alterado: NÃO

Nenhuma alteração desta tarefa em `frontend`, `palco-comunidades-agenda`, HTML/CSS/JS/React. O Maven copia recursos para seu diretório de build, sem construir/editar frontend. Deltas anteriores do frontend foram preservados por hash. Manifestos/resultados em `preservacao-inicial.json`, `preservacao-final.json` e `checkpoint-fontes-final.json` na pasta de evidências. Os **16 fontes Java** mantiveram seus hashes após testes/scan/update. `git diff --check`: **exit 0, sem erro de whitespace**; fontes/relatório novos também conferidos diretamente. Avisos CRLF do Git são warnings de conversão, não falhas de diff --check.

## 21. Testes alterados/adicionados

- Novo [PerfilTaxonomiaRf54Rf08IntegrationTest](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/PerfilTaxonomiaRf54Rf08IntegrationTest.java>): **27 cenários** PostgreSQL, catálogo reduzido/imutável, cascata/paginação, ownership/privacidade, múltiplas/sete Áreas, Principal, limites/duplicidades/IDs/hierarquia, remoção, rollback após flush, compatibilidade plana, criação, concorrência e consultas.
- Novo [PerfilProfissionalServiceTest](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/service/PerfilProfissionalServiceTest.java>): **12 cenários** de seleções futuras 5/5 por Área, duplicidades/limites, compatibilidade, poda compartilhada, timestamps/experiência e ambiguidade de formatos, com mocks.
- Alterado [PerfilCompletoServiceTest](<C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/service/PerfilCompletoServiceTest.java>): **29 cenários**. Retirada a expectativa antiga de portfólio obrigatório; opcionais ausentes/false explicitados. Acrescentados casos de mistura de especializações válida/inválida e Função cruzada em secundária. Nenhum teste removido para esconder falha; a instância parametrizada de portfólio obrigatório foi substituída pela cobertura de opcional.

Suíte nova PostgreSQL usa só as quatro Funções/especializações/relações oficiais; nenhum INSERT em tabelas de catálogo. Fixtures históricas de outras classes foram preservadas nos bancos descartáveis, sem se tornarem carga oficial de RF54. Testes existentes de perfil/cadastro foram preservados e executados.

## 22. Comandos executados

Checkpoint: status, branch, rev-parse, log -12, merge-base de d708cf0, fetch fork e rev-list. Get-FileHash RF/RNF/ZIP. Leituras Graphify MCP e Context7; auditoria direta de código/SQL/testes.

No diretório backend, JDK **21.0.12**:

```powershell
.\mvnw.cmd -Dtest=PerfilProfissionalServiceTest,PerfilCompletoServiceTest,PerfilTaxonomiaRf54Rf08IntegrationTest,PerfilEdicaoRf08IntegrationTest,AreaArtisticaIntegrationTest,ValidatedOfficialSqlIntegrationTest,CadastroRf01Rf24IntegrationTest test
.\mvnw.cmd test
```

Evidências separadas por execução. Primeira tentativa: erro de compilação dos testes novos, sem execução de cenários. Corrigidos campos de artista aplicados ao teste contratante e enum inexistente no teste. Segunda: 172 total/171 passed/0 failures/1 error; teste de exceção após flush esperava HTTP 500 que o handler existente não produz. Corrigido para capturar a exceção e verificar o rollback, sem alterar handler. Terceira: 176/176 verdes. Uma regressão completa intermediária foi interrompida pela revisão de concorrência antes do fechamento; não é resultado aprovado. Após refresh e teste determinístico, foco final: 177/177. Logs anteriores preservados, sem desabilitar testes ou modificar banco para passar.

Executado `& .\evidencias\rf54-rf08-2026-10-08\Executar-Semgrep.ps1`; o script preservado contém o comando Docker integral e montagem local do ruleset. Executado `graphify update .` pelo CLI já instalado, seguido de graph_stats/get_node pelo MCP HTTP. Git final executado: diff name-status database05/database/frontend, cached name-status, status --short e diff --check, mais comparação por hash das fronteiras. Saídas/exit e hashes preservados em evidências.

## 23. Resultado dos testes focados

**177 total / 177 passed / 0 failures / 0 errors / 0 skipped; BUILD SUCCESS, exit 0, 01:23 min.**

| Classe | Total |
|---|---:|
| AreaArtisticaIntegrationTest | 6 |
| CadastroRf01Rf24IntegrationTest | 47 |
| PerfilEdicaoRf08IntegrationTest | 28 |
| PerfilTaxonomiaRf54Rf08IntegrationTest | 27 |
| PerfilCompletoServiceTest | 29 |
| PerfilProfissionalServiceTest | 12 |
| ValidatedOfficialSqlIntegrationTest | 28 |

XMLs/resultados: `evidencias/rf54-rf08-2026-10-08/maven-focado-lock-final-reports/`, `maven-focado-lock-final-resumo.json`, log e exit correspondentes. Nenhuma contagem de relatório anterior foi reaproveitada como execução atual.

## 24. Resultado de mvn test

**BUILD SUCCESS, exit 0, 09:31 min. PostgreSQL 18.4/Testcontainers (`postgres:18-alpine`), schema oficial integral, ddl-auto=validate.** A soma dos **75 XMLs da execução final** confirma 1600 total/1583 passed/0 failures/0 errors/17 skipped. A seleção de XMLs usa timestamp posterior ao início desta execução, sem misturar resultados antigos. Evidências: `maven-completo.log/.exit`, `maven-completo-resumo.json`, `maven-completo-reports/`.

Regressões de cadastro/Google, menor/responsável, RF06/RF23/RF28/RF45, Banco/convites, perfil público/descoberta, favoritos, chat/realtime, moderação, notificações, Vagas, arquivos e mapeamento oficial passaram. `DescobertaPublicaRf37IntegrationTest.disponibilidadeNaoMudaDescoberta` confirma que false não remove da descoberta. Avisos preexistentes de fetch paginado de coleção em outros fluxos e deprecações foram mantidos, sem alterar configuração/teste para ocultá-los. A execução completa interrompida está identificada separadamente e não comprova aprovação.

## 25. Total de testes

**1600**, regressão completa final.

## 26. Passed

**1583**, regressão completa final (total menos skipped; failures/errors zero).

## 27. Failures

**0**, regressão completa final.

## 28. Errors

**0**, regressão completa final.

## 29. Skipped

**17**, todos condicionais preexistentes: **4** de CurrentLocalSchemaIntegrationTest exigem `palco.current-db-tests=true`; **13** de OfficialLocalApiIntegrationTest exigem `palco.official-db-tests=true`. Não foram habilitados contra bancos locais reais nem desabilitados artificialmente. Os testes novos PostgreSQL executaram, sem skips. Mensagens/contagens confirmadas nos XMLs atuais.

## 30. Semgrep e Graphify

**Semgrep 1.178.0: 372 arquivos Java, 60 regras p/java, ~100.0% das linhas parseadas, 0 findings, 0 blocking, 0 errors, exit 0, 34,4 s.** Todos os **16/16 arquivos do delta** constam em paths.scanned, incluindo não rastreados e testes. Imagem Docker oficial imutável `semgrep/semgrep@sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b`. Cache oficial p/java com SHA-256 **5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1**, conferido antes de executar, montado localmente. Volumes read-only/rede none, métricas off, sem alteração do ignore do repositório. TLS não foi desabilitado, instalação nativa Windows não foi usada e políticas de Windows/Defender não foram modificadas. Nenhum código do Palco foi alterado para corrigir ferramenta. Evidências: script, ruleset, JSON de scan, log, exit e resumo em `evidencias/rf54-rf08-2026-10-08/`.

**0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.** O scan desta tarefa é Java; não se declara novo scan secrets.

**Graphify update .: exit 0, 33,2 s, AST-only, sem LLM/label.** MCP final confirmou **6616 nós / 22756 arestas / 360 comunidades; EXTRACTED 91%, INFERRED 9%, AMBIGUOUS 0%**. get_node confirmou PerfilProfissionalService (L17) e TaxonomiaProfissional (L11) nos arquivos novos. Evidências: `graphify-update.log/.exit`, `graphify-update-resumo.json` e `graphify-mcp-final-0/1/2.json`. Artefatos gerados/cache/backups do grafo não são alterações de frontend/produto.

Avisos preservados: **45 SQLs sem extração**, pois tree_sitter_sql não está instalado; mudança de comunidades e rótulos semânticos anteriores. Não houve instalação de parser/Python/DLL, semantic labeling nem contorno de Windows App Control. Graphify é auxiliar e não substitui auditoria direta do database05 ou testes.

## 31. Riscos

Catálogo reduzido limita escolhas reais, mesmo com backend preparado. EntityGraph evita N+1 no fluxo crítico, mas fetch de conjuntos pode ampliar linhas internas; sem benchmark de produção. Proteção 5/5 é da aplicação: SQL externo/procedimentos preservados podem seguir regras físicas anteriores. API plana não transporta as novas escolhas detalhadas; futura integração frontend deve adotar `areas`. A conta é bloqueada por usuário durante a edição para proteger integridade; alto paralelismo de edição do mesmo usuário pode esperar o lock, sem prova de carga nesta tarefa.

## 32. Pendências

RF26 troca/verificação de e-mail; RF27 responsável/revalidação; RF48 opt-ins; RF53 central/configurações/username e demais operações especializadas permanecem em seus RFs. Nenhuma descoberta pública foi implementada/modificada; regressões existentes verificam disponibilidade false. RF47 não recebeu algoritmo ou score. Limites VAGA 3/3 e demais requisitos da Vaga pertencem à futura tarefa RF04/RF07. Não se amplia conclusão de RF08 para integrações sensíveis.

## 33. Dependências do próximo pacote oficial

Dados completos/homologados RF54: **60 Funções e 357 vínculos funcionais** da baseline comparada, preservando IDs/relações oficiais e resolvendo nomes divergentes explicitamente. Não é mudança necessária de schema para RF08/RF54. As capacidades C01–C20 da auditoria permanecem fora deste delta; não foram implementados Banco ativo, convite relacional, Projetos, Follow, Comunidades/Eventos ou outros produtores.

## 34. Conclusão separada

**RF54 — BACKEND/CONTRATO CONCLUÍDO, ESTRUTURA PREPARADA no escopo de catálogo e ARTISTA:** hierarquia, endpoints oficiais, limites centralizados e regressões aprovados sem hardcode/schema. Carga integral dos dados oficiais permanece pendente do próximo pacote do banco; regras 3/3 da Vaga continuam no recorte de RF04/RF07, sem afirmar entrega global de RF54 ou catálogo completo instalado.

**RF08 — NÚCLEO PROFISSIONAL CONCLUÍDO:** ownership, múltiplas Áreas, Principal, 5/5 por Área, hierarquia, experiência, remoção/revalidação, disponibilidade, raio opcional, completude e bio CONTRATANTE opcional validados. Testes focados/completos e Semgrep verdes, sem regressão crítica observada. Integrações de configurações sensíveis dependentes de RF26/RF27/RF48/RF53 não são declaradas concluídas.

## 35. Próximo passo recomendado

Receber/homologar carga oficial RF54 como dados; em tarefa separada, adaptar RF04/RF07 à baseline de Vagas 3/3 e ampliar os filtros previstos de Busca/Perfil/Banco com a taxonomia existente. Integração frontend somente em escopo autorizado. Não gerar SQL/migration para compensar catálogo incompleto.

## 36. Registro semanal — 08/10/2026

**Objetivo:** consolidar RF54/RF08 sobre o database05 sem alterar schema. **RF/RNF:** RF54, RF08, RNF02/05/06/07/08/10/13/17. **Backend:** 13 fontes de produção e 3 de testes listadas nas seções 7/21. **Frontend/banco:** NÃO alterados nesta tarefa.

**Funcionalidades/correções:** funções públicas por Área paginadas; edição multiárea, Principal explícita única, 5/5 por Área, hierarquia oficial, experiência, reconciliação/poda, timestamps preservados, completude sem raio/foto/portfólio e manutenção de bio CONTRATANTE opcional. **Decisões:** banco preservado, catálogo persistido sem hardcode/carga faltante, lock por usuário e refresh após espera, transação única e DTO compatível. **Testes:** focados finais 177/177; completo 1600 total/1583 passed/0 failures/0 errors/17 skipped, BUILD SUCCESS; Semgrep 372 arquivos/60 regras/0 findings/errors; Graphify AST exit 0, 6616/22756/360; 795 arquivos protegidos e index preservados, diff --check exit 0. **Pendências/próximo passo:** dados oficiais RF54 e RFs especializados; fila de Vagas/Busca/Perfil/Banco em tarefas separadas. Sem staging/commit/push.

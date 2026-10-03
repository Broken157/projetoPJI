# RF37 — Descoberta pública de perfis — 03/10/2026

**Estado: RF37 backend concluído no escopo autorizado.** Focados **346/346**; Maven completo **989 total/972 passed/0 failures/0 errors/17 skipped**, BUILD SUCCESS; Semgrep Java **261 arquivos/60 regras/0 findings/0 erros, exit 0**; Graphify update AST **exit 0**. Escopo: backend, testes e novas evidências. Banco/SQL/frontend e documentos históricos preservados; sem staging, commit ou push. Integração visual permanece pendente.

Checkout: `C:\Users\masca\Documents\pjiiiiii\projetoPJI-react00b-baseline`.
Branch: `integracao-recuperada-2026-09-15`. HEAD inicial: `8178355`.
Evidências: `evidencias/rf37-2026-10-03/`.

## 1. Objetivo

Permitir descoberta simples e pública de ARTISTA/CONTRATANTE e navegação ao detalhe RF10, sem login, filtros profissionais avançados ou ações privadas. Aplicar publicabilidade no PostgreSQL antes de count/paginação.

## 2. RF/RNFs consultados

Fonte oficial: `C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt`. RF37 integral (3216–3296), RF01 (120–227), RF08 (775–880), RF10 (981–1059), RF13 (1170–1245), RF17 (1462–1549), RF22 (1887–2046), RF27 (2438–2516), RF29 (2596–2669); RNF02 (3910–3953), RNF07 (4161–4212), RNF08 (4214–4273), RNF09 (4275–4326). A decisão consolidada RF45/RF10/RF27 sobre menor autorizado permanece vigente.

Lidos os relatórios recentes RF45, RF06, RF01/RF24, sincronização database04 e registro semanal de 02/10. As falhas antigas de cadastro/UNIQUE e o bloqueio anterior do Graphify são históricos; não descrevem automaticamente este checkpoint. Nenhum relatório antigo foi reescrito.

**Disponibilidade nos requisitos atuais:** RF08 (linha 829), RF13 (linha 1208) e RF37 (linha 3284) estão alinhados: Disponível para oportunidades = Não não remove o perfil da descoberta pública. A disponibilidade pode afetar elegibilidade/comportamento no Banco de Talentos, mas não a existência pública do perfil. Não existe conflito entre esses requisitos nesse ponto. RF29 descreve seed numérica, mas AvatarService atual já usa seed opaca; reutilizada implementação vigente. RF10 atual permitia adulto com conta não ATIVA: lacuna de publicabilidade corrigida minimamente porque bloquear apenas RF37 deixaria descoberta e destino inconsistentes.

## 3. Estado inicial

Backend limpo contra HEAD após checkpoint RF45. Working tree intencionalmente sujo: README, frontend, SQL histórico e numerosos untracked anteriores. Nenhum delta anterior foi revertido, limpo, armazenado em stash ou incluído automaticamente.

Baseline capturado antes das edições: 1.146 arquivos fingerprintados, status/diff/HEAD em evidências. Referência documental anterior RF45: 914 total/897 passed/0 failures/0 errors/17 skips, BUILD SUCCESS. Não confundir essa execução anterior com resultados RF37.

## 4. Auditoria Graphify

MCP Graphify via HTTP utilizado primeiro, com sucesso: **5.633 nós, 18.447 arestas, 328 comunidades; 91% EXTRACTED, 9% INFERRED, 0% AMBIGUOUS**. Query ampla retornou 1.736 nós encontrados com saída truncada; consultas menores e neighbors localizaram PerfilPublicoController/Service, DTOs RF10, repositories, MenorAutorizadoPolicy, TalentoController/Service, AvatarService, Usuario, perfis e taxonomia.

Os vínculos foram confirmados nas fontes atuais, testes e SQL oficial; grafo orienta navegação e não substitui prova funcional. Evidência: `graphify-auditoria-inicial.json`. Não foi iniciado graphify-mcp.exe nem alterada configuração MCP, Python, Defender ou App Control.

## 5. Componentes existentes reutilizados

Namespace público RF10, PerfilPublicoController/Service, MenorAutorizadoPolicy/Clock, repositories de perfis, DTOs RF10, AvatarService, enums oficiais, localização estruturada, handler global e padrão page/size (default 20, máximo 50). Reutilizado OfficialPostgreSQLContainer com pacote database04 e ddl-auto=validate.

Não existia endpoint de descoberta nesse namespace. TalentoController é autenticado/profissional e agrega disponibilidade, funções, especializações, experiência e recomendação; não foi transformado em RF37.

## 6. Arquivos de produção alterados

Todos relativos ao checkout:

| Arquivo | Mudança |
|---|---|
| `backend/src/main/java/com/portifolio/controller/PerfilPublicoController.java` | GET de descoberta no namespace existente, parâmetros públicos e rejeição de parâmetros não suportados |
| `backend/src/main/java/com/portifolio/service/PerfilPublicoService.java` | Leitura/projeção RF37; detalhe RF10 consulta o mesmo predicado de publicabilidade |
| `backend/src/main/java/com/portifolio/config/SecurityConfig.java` | permitAll somente GET /api/perfis/publicos, preservando matcher RF10 |
| `backend/src/main/java/com/portifolio/security/MenorAutorizadoPolicy.java` | Predicado Criteria compartilhado e Specification RF10, com mesmos limites etários/consentimento |
| `backend/src/main/java/com/portifolio/repository/PerfilArtistaRepository.java` | Specification com EntityGraph do detalhe RF10; métodos existentes conservados |
| `backend/src/main/java/com/portifolio/repository/PerfilContratanteRepository.java` | Specification com EntityGraph de usuario para RF10 |
| `backend/src/main/java/com/portifolio/repository/PerfilDescobertaRepository.java` — novo | Count/projeção escalar paginada, binding e EXISTS de área |
| `backend/src/main/java/com/portifolio/dto/FiltroDescobertaPublica.java` — novo | Validação e normalização restrita de filtros |
| `backend/src/main/java/com/portifolio/dto/PerfilDescobertaResponse.java` — novo | Card whitelist e envelope paginado |

Nenhuma mudança em criação/retirada/recandidatura RF06, RF45, chat, Banco de Talentos, portfólio ou identidade cadastral.

## 7. Arquivos de teste

`backend/src/test/java/com/portifolio/controller/DescobertaPublicaRf37IntegrationTest.java` — novo.
`backend/src/test/java/com/portifolio/controller/PerfilPublicoRf10IntegrationTest.java` — fixture adulta explicitamente ATIVA e regressão dos quatro estados não aptos nos dois papéis. Nenhum teste antigo removido, enfraquecido ou convertido em skip; limite de consultas RF10 preservado.

## 8. Endpoint final

`GET /api/perfis/publicos`, público para anônimo e autenticado. Exemplo:

```http
GET /api/perfis/publicos?q=João&tipo=ARTISTA&cidade=Campinas&estado=SP&areaId=1&page=0&size=20
```

Tipo/usuarioId retornados permitem usar `GET /api/perfis/publicos/{tipo}/{id}` existente, sem segundo identificador/URL absoluta.

## 9. Parâmetros finais

| Parâmetro | Contrato |
|---|---|
| q | Opcional, até 150 caracteres; trecho literal de nome público ou username. Prefixo @ restringe a username |
| tipo | ARTISTA ou CONTRATANTE; ausente admite ambos |
| cidade | Opcional, até 100 caracteres; igualdade sem distinção de maiúsculas |
| estado | UF com duas letras, normalizada para maiúsculas; segue padrão de validação existente |
| areaId | ID positivo da relação normalizada oficial; inexistente produz página vazia |
| page | Inteiro não negativo, default 0 |
| size | Inteiro 1–50, default 20 |

Espaços externos são retirados; texto vazio é ausência de filtro. Caracteres de controle/limites inválidos e offset além de Integer.MAX_VALUE recebem 400. Parâmetros desconhecidos/profissionais recebem 400, inclusive disponibilidade/funções/recomendação. Tipo CONTRATANTE + área: **400 explícito**. Área sem tipo restringe a ARTISTA.

## 10. Semântica da busca

AND entre filtros; q sem @ usa OR entre nomeExibicao público e username. Comparação textual sem distinção de maiúsculas, com acentos preservados, conforme LOWER vigente no projeto. Não instalada extensão unaccent; João e Joao podem não coincidir. %, _ e barra invertida são literais escapados e valores bindados.

Artista não possui coluna separada de nome artístico: `usuarios.nome` é nomeExibicao RF10, também pesquisável no RF37. Contratante usa nomeEmpresa público não vazio; na ausência, nome do usuário, como RF10. Quando empresa é o nome exibido, nome pessoal oculto não entra na busca livre. q não percorre biografia, integrantes, e-mail, telefone, responsável, CPF/CNPJ, endereço, experiência ou metadados privados.

## 11. Regra de perfil publicável

Uma única construção Criteria em `MenorAutorizadoPolicy.publicavel` é reutilizada no count, SELECT paginado RF37 e Specifications RF10. Exige papel público, conta **ATIVA**, nascimento compatível e perfil correspondente persistido. Não exige perfil_completo, disponibilidade ou associação a Banco de Talentos.

Adulto: nascimento no máximo hoje menos 18 anos. Menor: somente ARTISTA entre 14 e 17, responsável normalizado com data_consentimento preenchida e consentimento_revogado diferente de true. Data desconhecida/incompatível não satisfaz o predicado. Relógio da aplicação determina limites; não há idade inferida na resposta.

## 12. Artista adulto

ATIVA com perfil correspondente aparece mesmo incompleto, sem funções/área e indisponível para oportunidades. Autenticação, candidaturas e relações profissionais não influenciam a projeção. Perfil público não equivale a elegibilidade RF06/RF13/RF17.

## 13. Contratante

ATIVA, adulto, perfil CONTRATANTE correspondente. Nome público empresa/entidade quando presente, fallback RF10 caso contrário; username, avatar e cidade/UF. Nenhuma exposição de CNPJ, representante privado ou dados de contato. Internos ADMIN/MODERADOR não entram, mesmo com perfil legado.

## 14. Menor autorizado

ARTISTA 14–17 com conta apta e autorização vigente pode aparecer e abrir RF10 correspondente. Não há aprovação adicional por busca, opt-out novo, campos de idade ou experiência. Predicado persistente é derivado da política compartilhada RF45/RF10/RF27; abertura de chat continua utilizando a política existente.

## 15. Menor não autorizado

Sem responsável, data de consentimento, idade mínima, conta apta ou com revogação histórica: excluído antes de count/paginação; detalhe RF10 retorna 404 genérico. CONTRATANTE menor permanece privado. Revogação persistida é defesa vigente; nenhum fluxo novo de revogação foi implementado.

## 16. Disponibilidade para oportunidades

`disponivel_oportunidades=false` **continua aparecendo**, assim como true. Campo não está no WHERE, no contrato de filtro ou no card. RF08, RF13 e RF37 estão atualmente alinhados: Disponível para oportunidades = Não não remove o perfil da descoberta pública. A disponibilidade pode afetar elegibilidade/comportamento no Banco de Talentos, mas não a existência pública do perfil.

## 17. Separação RF37 × RF13/RF17

Não consulta banco_talentos, não exige vínculo artista/contratante e não revela pertença a Banco. Não busca função, especialização, experiência, raio, disponibilidade, vaga, compatibilidade, recomendação, ranking ou engajamento. APIs profissionais e suas restrições foram preservadas.

A leitura não cria chat, salvo, follow, convite, candidatura, inclusão em Banco ou notificação. Ações privadas continuam nos próprios RFs; nenhum estado personalizado depende do token.

## 18. DTO/projeção

Card com somente **usuarioId, tipo, username, nomeExibicao, avatarUrl, cidade, estado**. Username conserva case/valor persistido e vem sem @. Avatar reutiliza prioridade/fallback opaco de AvatarService, sem persistência adicional.

Envelope: content, page, size, totalElements, totalPages, first, last, hasNext, hasPrevious. Não são retornadas entidades, funções, lista profissional, experiência, scores, bio extensiva ou flags internas.

## 19. Campos explicitamente excluídos

E-mail/telefone, CPF/CNPJ, nascimento/idade exata ou aproximada, endereço completo/coordenadas, responsáveis/dados/consentimento, autodeclarações, senha/hash, tokens/googleId, flags privadas, experiência, disponibilidade, pertença a Banco, matching, histórico e objetos JPA.

Critérios privados necessários à autorização são apenas predicados/EXISTS; não integram SELECT de resultados. Experiência não é lida nem buscada para depois ser descartada no RF37.

## 20. Paginação

Count com exatamente os mesmos filtros e política; SELECT escalar limitado por setFirstResult/setMaxResults no PostgreSQL. Default 20, máximo 50; limites inválidos 400. Página sem resultados ou além do total é 200 com content vazio e total coerente. Não carrega universo de perfis na memória.

## 21. Ordenação

LOWER(nomeExibicao) ASC, usuarioId ASC para desempate. Ordem simples, explicável e determinística com dados estáveis; nenhuma relevância profissional/engajamento. Alterações concorrentes entre páginas com OFFSET não garantem snapshot estável.

## 22. Queries

Criteria Jakarta Persistence 3.2 já instalado; LEFT entity joins por chaves únicas usuario_id de perfis, EXISTS de área pela chave oficial perfil_artista_area, EXISTS de consentimento na política compartilhada. Count e SELECT gerados pela mesma construção, sem SQL concatenado com entradas do cliente.

q/tipo/cidade/estado/areaId usam parâmetros nomeados explicitamente bindados; LIMIT/OFFSET aplicados pelo ORM. Não há JOIN de funções/especializações/experiência nem Banco. Contrato de localização lê campos estruturados oficiais.

## 23. N+1/performance

Até duas queries para resultado: count + página; total zero/página fora do total dispensa SELECT. Projeção de sete escalares, sem fetch de Usuario/PerfilArtista ou coleção por resultado. Teste StatementInspector compara página 1/50, inspeciona SELECT e limite, sem fetch de experiência nem dependência de Banco.

LIKE com curinga inicial, LOWER e OFFSET podem custar em volume alto. Não foi adicionado índice/schema; análise de plano/carga é pendência própria. Teste de queries não equivale a benchmark.

## 24. SecurityConfig

permitAll explícito GET no root público; RF10 detalhe mantém matcher existente. Outros métodos e rotas privadas conservam autenticação/papel/ownership. JwtAuthFilter já ignora token inválido e permite seguir anônimo no endpoint público; não foi modificado. Token não define visibilidade, campos ou filtros.

## 25. Erros HTTP

200 para resultados/página vazia, sem 401/403 pela ausência de login. 400 para tipo, UF, paginação, filtros malformados/incompatíveis/avançados; handler global ErroResposta sanitizado. Detalhe de perfil inexistente/não publicável permanece 404 genérico, sem revelar existência. Falha interna real permanece responsabilidade dos handlers vigentes; não foi introduzida resposta com SQL/stacktrace.

## 26. Banco alterado: NÃO

Sem edição em SQL, database04/database02, init/seed, migrations, índices, enums, constraints, procedures, functions ou triggers. ddl-auto=validate mantido. Fixtures e limpeza DML apenas nos bancos descartáveis PostgreSQL/Testcontainers, carregados pelo pacote oficial. Banco de desenvolvimento não recebe DML da tarefa.

A conferência atual registra **46/46 arquivos idênticos ao ZIP oficial, zero divergências e zero arquivos adicionais**, SHA-256 do ZIP `52b1c4af06d79a7efae47e6fa320b4a0a32359efaae1d40e2a73d06129c6df2b`. Manifesto de preservação: **143 arquivos banco/SQL e 569 frontend sem diferenças/remoções; documentos históricos intactos**. Evidências: `database04-preservado.json` e `preservacao-e-delta.json`.

## 27. Frontend alterado: NÃO

Nenhuma edição/format/build/test npm. Maven copia recursos existentes para target-maven backend, sem editar origens. Deltas anteriores e documentos históricos preservados, com verificação de hashes ao fechamento. A integração visual RF37 permanece para tarefa autorizada futura.

## 28. Testes novos

Nova classe RF37: **71 casos aprovados**, cobrindo os 45 critérios solicitados em cenários parametrizados: anônimo/autenticado/token inválido; ambos/tipos/internos/estados; menores 14/17, consentimento, ausência, revogação, aniversários/idade inferior; whitelist/navegação RF10; nome artístico/empresa/username/case/acentos/cidade/UF/área; disponibilidade/Banco; nenhuma mutação; filtros combinados/texto permitido; paginação/count/ordem/duplicatas/limite; binding/inputs maliciosos; N+1 e ausência de leitura de experiência. Regressão RF10 adicional dos quatro estados não aptos, verificando ambos os papéis; classe RF10 passa a 16 casos.

## 29. Regressões

Focados incluem RF10, RF27, RF45, RF13, segurança/JWT, database04 bootstrap, avatar/perfil RF08, RF06, portfólio e chat. Regra RF06 permanece inalterada; RF13 continua profissional, inclusive política própria de elegibilidade. Nenhum teste convertido em skip.

## 30. Bateria focada

**346 total/346 passed/0 failures/0 errors/0 skipped, BUILD SUCCESS, exit 0**, **03:41 min**, término **2026-10-03T11:27:49-03:00**. Comando no backend: `.\mvnw.cmd '-Dtest=DescobertaPublicaRf37IntegrationTest,PerfilPublicoRf10IntegrationTest,GuardianConsentRf27IntegrationTest,GuardianConsentServiceTest,CandidatosVagaRf45IntegrationTest,TalentoRf13IntegrationTest,GenericEndpointsSecurityIntegrationTest,JwtAuthenticationIntegrationTest,Database04BootstrapIntegrationTest,AvatarServiceTest,PerfilEdicaoRf08IntegrationTest,CandidaturaControllerRf06IntegrationTest,PortfolioRf16IntegrationTest,ChatRf24IntegrationTest' '-Dspring.test.mockmvc.print=NONE' test`.

| Classe focada | Passed/total |
|---|---:|
| DescobertaPublicaRf37IntegrationTest | 71/71 |
| PerfilPublicoRf10IntegrationTest | 16/16 |
| GuardianConsentRf27IntegrationTest | 10/10 |
| GuardianConsentServiceTest | 3/3 |
| CandidatosVagaRf45IntegrationTest | 40/40 |
| TalentoRf13IntegrationTest | 42/42 |
| GenericEndpointsSecurityIntegrationTest | 26/26 |
| JwtAuthenticationIntegrationTest | 4/4 |
| Database04BootstrapIntegrationTest | 5/5 |
| AvatarServiceTest | 3/3 |
| PerfilEdicaoRf08IntegrationTest | 28/28 |
| CandidaturaControllerRf06IntegrationTest | 57/57 |
| PortfolioRf16IntegrationTest | 26/26 |
| ChatRf24IntegrationTest | 15/15 |
| **Total** | **346/346** |

Conferidos somente os 14 XMLs da seleção, soma igual ao resumo Maven; cópias sem bloco de propriedades de runtime, preservando casos/métricas/logs. Evidências: `focados.log`, `.exit`, `focados-totais.json` e `focados-reports/`. RF10 mantém seu limite de duas consultas no cenário existente. RF37 com páginas 1/50 confirmou **duas queries**, LIMIT no SQL e nenhum SELECT de campos privados/experiência.

Ambiente carregado por scripts/database04/environment.ps1; PALCO_TEST_DATABASE04_PATH aponta pacote oficial. A propriedade somente evita dumps MockMvc, sem desabilitar testes. Diagnóstico inicial: código/testes compilaram; Docker Desktop desligado impediu cenários, 18 total/0 failures/12 errors/0 skips, BUILD FAILURE, exit 1, 24,327 s. Runtime instalado iniciado em segundo plano; log original preservado em focados-docker-desligado.log/.exit.

## 31. Maven completo

**989 total/972 passed/0 failures/0 errors/17 skipped, BUILD SUCCESS, exit 0**, **08:06 min**, término **2026-10-03T11:38:27-03:00**. Comando: `.\mvnw.cmd test '-Dspring.test.mockmvc.print=NONE'`, após carregar ambiente database04 e definir PALCO_TEST_DATABASE04_PATH; sem clean ou filtro. JDK **21.0.11**, PostgreSQL **18-alpine** real, Testcontainers **2.0.5**, Spring Boot **4.0.6**, ddl-auto=validate.

Conferidos **61 XMLs da execução atual**, todos posteriores ao início da rodada, cuja soma coincide com resumo Maven. Sem misturar XMLs antigos de outros diretórios de build. Mantidos os mesmos 17 skips condicionais: **4 CurrentLocalSchemaIntegrationTest + 13 OfficialLocalApiIntegrationTest**; nenhuma condição/flag/teste alterado para obter verde. Evidências: `maven-completo.log`, `.exit`, `completo-totais.json` e `completo-reports/`; somente propriedades de runtime removidas das cópias XML.

Os **328 arquivos Java de produção/testes** registrados após focados mantêm hashes iguais no fechamento, confirmando que completo e Semgrep validam o código final. Controle restrito de privacidade das **95 evidências textuais** inspecionadas: zero tokens assinados e zero valores locais conhecidos de JWT_SECRET/DB_PASSWORD; isso não é scan secrets completo. Evidências: `codigo-validado-focados.json` e `evidencias-privacidade.json`.

## 32. Total/passed/failures/errors/skipped

| Execução | Total | Passed | Failures | Errors | Skipped | Resultado |
|---|---:|---:|---:|---:|---:|---|
| Baseline RF45 anterior, documental | 914 | 897 | 0 | 0 | 17 | BUILD SUCCESS |
| Diagnóstico RF37 com Docker desligado | 18 | 6 | 0 | 12 | 0 | BUILD FAILURE, infraestrutura |
| **RF37 focados, 14 classes** | **346** | **346** | **0** | **0** | **0** | **BUILD SUCCESS** |
| **RF37 completo, 61 classes** | **989** | **972** | **0** | **0** | **17** | **BUILD SUCCESS** |

São 71 casos novos RF37 e 4 parametrizações adicionais RF10: aumento de 75 em relação ao baseline RF45. Execuções focada/completa repetem casos; não somar como quantidade única. Os resultados históricos RF06 (867/850/0/0/17) e RF45 permanecem intactos nos documentos originais.

## 33. Semgrep

**Concluído após produção final: versão 1.178.0, 261 arquivos Java, 60 regras p/java, ~100% das linhas parseadas, 0 findings/0 blocking, 0 erros, exit 0**, duração **308,25 s**. Manifesto de paths confirma a inclusão dos três arquivos de produção novos, ainda untracked. Imagem Docker oficial fixada em `semgrep/semgrep@sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b`.

Cache oficial previamente baixado pelo Windows com TLS verificado foi copiado para evidências RF37, SHA-256 `5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1`. Montagem somente leitura; sem instalação nativa Windows, sem desabilitar TLS e sem alterar política de segurança. A instalação nativa não deve ser usada neste ambiente. Proveniência em `semgrep-ruleset-proveniencia.json`; resultado bruto em `semgrep-java-saida.log`/`semgrep-java.json`, resumo em `semgrep-resumo.json` e exit em `semgrep-java.exit`.

Comando no checkout:

```powershell
docker run --rm --mount "type=bind,source=<checkout>,target=/src,readonly" semgrep/semgrep@sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b semgrep scan --config /src/evidencias/rf37-2026-10-03/semgrep-java.yml --metrics=off --disable-version-check --json /src/backend/src/main/java
```

Nenhum código do Palco foi alterado para corrigir ferramenta. Não se declara scan secrets completo nesta tarefa.

**0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.**

## 34. Graphify update

**`graphify update .` concluído, exit 0**, conforme AGENTS.md e autorização RF37 atual. Atualização AST: **5.713 nós, 18.847 arestas, 328 comunidades**; derivados graph.json/graph.html/GRAPH_REPORT.md, cache e backup atualizados. A sugestão de label foi ignorada conforme instrução; nenhum graphify label/LLM executado. Evidências: `graphify-update.log` e `.exit`.

O MCP HTTP funcionou na auditoria inicial. Na reconferência posterior ao update, graph_stats retornou erro de transporte HTTP em `http://127.0.0.1:8765/mcp`; isso foi registrado em `graphify-mcp-verificacao-final.json`, sem reparo/reinício do servidor, configuração MCP, Python, Defender ou App Control. O sucesso CLI é comprovado pelo log/exit; não se apresenta a reconferência MCP como bem-sucedida. A indisponibilidade histórica RF45 não foi apagada. Nenhum contorno de segurança foi aplicado.

## 35. Riscos

Custo LIKE/LOWER/OFFSET em grandes bases; paginação sem snapshot entre requisições concorrentes; collation/case/acentos conforme PostgreSQL ativo; conteúdo público definido pelo próprio usuário ainda sujeito à moderação vigente. Sem nova política de moderação, índice ou mudança global de arquitetura.

## 36. Limitações

Validação backend não entrega frontend nem prova SMTP/provedores externos, carga, ausência absoluta de segredos ou todos os RFs completos. Os 17 skips condicionais anteriores permanecem identificados. RF16 por projetos/RASCUNHO, demais fases RF35 e filtros RF17 continuam suas pendências próprias.

## 37. Pendências

Integrar cliente à descoberta/navegação e homologar fluxo visual em tarefa futura. Avaliar plano/carga com volume representativo antes de propor índices ao grupo. MCP HTTP indisponível na reconferência final: aguardar ambiente funcional em tarefa própria, sem reparo nesta tarefa. Nenhuma mudança de banco necessária ao contrato RF37 implementado.

## 38. Conclusão

**RF37 backend concluído no escopo solicitado**, com descoberta anônima/autenticada equivalente, dois papéis públicos, conta apta, menor autorizado, filtros públicos, projeção escalar e paginação/count no PostgreSQL. Disponibilidade=false permanece visível, sem relação com Banco de Talentos. Resultado navega ao RF10, que agora compartilha o mesmo predicado e não expõe adulto inapto. RF06/RF45 preservados e suas regressões verdes.

Focados 346/346; completo 989/972/0/0/17, BUILD SUCCESS; Semgrep 261 arquivos/60 regras/0 findings/0 erros, exit 0; Graphify auditado inicialmente e atualizado via AST, exit 0. Erro de transporte MCP posterior registrado sem contorno de segurança. Banco/frontend/documentos anteriores intactos, sem staging/commit/push. Conclusão backend não inclui integração visual, benchmark/carga ou demais RFs completos.

## 39. Próximo passo recomendado

Após validação, revisar delta específico e integrar tela pública de descoberta em tarefa própria, preservando RF10, whitelist, navegação e autenticação das ações privadas. Não realizar staging global sobre o working tree existente.

## Registro pronto para consolidação semanal

**Data:** 03/10/2026.
**Objetivo:** implementar descoberta pública simples RF37 e navegação RF10.
**RF/RNF trabalhado:** RF37; dependências RF01/RF08/RF10/RF13/RF17/RF22/RF27/RF29; RNF02/RNF07/RNF08/RNF09.
**Backend alterado:** endpoint público, projeção escalar, filtros/paginação, política Criteria compartilhada e guard de conta RF10.
**Frontend alterado:** NÃO. **Banco alterado:** NÃO.
**Funcionalidades concluídas/avançadas:** RF37 backend concluído; descoberta pública de dois papéis, menor autorizado, navegação RF10, disponibilidade=false preservada e independência do Banco.
**Bugs corrigidos:** RF10 adulto não verificava conta ATIVA; descoberta/detalhe agora compartilham predicado.
**Decisões técnicas:** namespace RF10; q apenas nome público/username; área só artista; Criteria/binding; teto 50 e ordem nome/ID. RF08, RF13 e RF37 estão alinhados: disponibilidade=Não preserva a descoberta pública e pode afetar elegibilidade/comportamento no Banco de Talentos.
**Segurança/privacidade:** whitelist sete campos, menor autorizado, conta apta, papel público, nenhum fetch de experiência RF37.
**Testes/resultados:** 71 casos RF37 + 4 parametrizações RF10; focados 346/346, 0 failures/errors/skips, 03:41 min; completo 989 total/972 passed/0 failures/0 errors/17 skips, BUILD SUCCESS, 08:06 min. Semgrep Docker 1.178.0, 261 arquivos/60 regras/0 findings/0 erros, exit 0. Graphify MCP inicial funcional, update AST exit 0 (5.713 nós/18.847 arestas); reconferência HTTP falhou sem reparo. Pacote database04 46/46 idêntico; frontend/banco/documentos anteriores preservados.
**Pendências:** integração visual/carga.
**Próximos passos:** revisar evidências/delta e integrar frontend em tarefa própria.

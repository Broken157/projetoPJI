# Baseline de testes verde — 28/09/2026

**Escopo:** apenas as duas failures da suíte existente. Foram lidos integralmente `rf e rnf.txt` revisado, `RELATORIO_RF02_RF26_BLOQUEIO_CONTA_NAO_ATIVA.md`, `RELATORIO_ESTABILIZACAO_TESTES_2026-09-27.md` e `AUDITORIA_DELTA_RFS_REVISADOS_2026-09-28.md`; as causas foram confirmadas no código e no dump histórico antes da edição. Checkout: `integracao-recuperada-2026-09-15`, HEAD `a6164e38b177c765638d61eb0d68c4b46f79f052`, com mudanças locais preexistentes preservadas.

## Diagnóstico, contrato e correção

| Failure | Diagnóstico e RF/RNF | Correção necessária |
|---|---|---|
| `PerfilEdicaoRf08IntegrationTest.contratanteSemBiografiaOuLocalizacaoFicaIncompleto` | O teste antigo exigia `perfil_completo=false` sem bio, embora o RF08 revisado (`rf e rnf.txt:831,863`) torne a bio do contratante opcional. O dump histórico já ignora a bio na função `fn_verificar_perfil_completo`, mas `PerfilCompletoService.calcularContratante` ainda a exigia, causando divergência em recálculos posteriores. A ausência de localização continua impedindo completude. Relaciona-se a RF08 e RNF10 (regressão atualizada ao contrato). | O teste de integração foi separado em **bio ausente + localização válida = completo** e **localização em branco + demais dados válidos = incompleto**. O primeiro também verifica a bio persistida e um recálculo pela edição de `/api/usuarios/me`. No serviço Java, foi removida somente a exigência antiga de bio. O teste unitário manteve as demais ausências obrigatórias e passou a cobrir bio nula/em branco como opcional. |
| `ManuDumpSchemaIntegrationTest.entityManagerFactorySobeComValidateEQuarentaETresTabelas` | A asserção `current_database() startsWith("palco_test_")` descrevia apenas o modo local opcional: nele `OfficialPostgreSQLContainer` cria e remove uma base com nome `palco_test_<UUID>`. No modo Docker efetivamente usado, o `PostgreSQLContainer` cria a base descartável `test`. Um prefixo, isoladamente, também não comprova a origem da conexão. Relaciona-se à integridade de teste de RNF07/RNF10. | A asserção agora exige helper em execução, URL JDBC da conexão real igual à URL do helper e `current_database()` igual ao nome que o helper criou. Assim, uma configuração Spring apontando para outro banco falha, enquanto Docker e modo local mantêm seus nomes próprios. As checagens de `validate`, 43 tabelas e tabelas essenciais foram preservadas. |

**Proteção e regra de produção:** a única alteração de produção foi retirar o requisito de bio que contraria o RF08 revisado. Continuam exigidos dados básicos, tipo de contratante, localização não vazia e CPF ou CNPJ/nome de empresa conforme o tipo. Nenhuma outra condição de completude foi relaxada. O teste de banco continua vinculado ao container descartável ou, se explicitamente selecionado, à base temporária criada pelo helper; não aceita uma URL de banco oficial/compartilhado por coincidência de nome.

## Arquivos alterados nesta tarefa

| Arquivo | Finalidade |
|---|---|
| `backend/src/main/java/com/portifolio/service/PerfilCompletoService.java` | Alinhar recálculo Java à bio opcional do RF08. |
| `backend/src/test/java/com/portifolio/controller/PerfilEdicaoRf08IntegrationTest.java` | Separar e validar os dois cenários; preservar as demais validações da classe. |
| `backend/src/test/java/com/portifolio/service/PerfilCompletoServiceTest.java` | Atualizar o caso parametrizado e provar bio opcional no cálculo Java. |
| `backend/src/test/java/com/portifolio/ManuDumpSchemaIntegrationTest.java` | Conferir a identidade da conexão e da base descartável, sem prefixo exclusivo do modo local. |
| `RELATORIO_BASELINE_TESTES_VERDE_2026-09-28.md` | Registrar auditoria, execução e limites. |

`ManuDumpSchemaIntegrationTest.java` já era não rastreado e os outros fontes já tinham edições locais anteriores; foram preservados. O índice local `graphify-out/` foi atualizado conforme `AGENTS.md`, sem efeito no produto. Não houve commit nem push.

## Execução e totais

| Etapa | Comando | Total | Passed | Failures | Errors | Skipped | Resultado |
|---|---|---:|---:|---:|---:|---:|---|
| Focada | `backend> .\mvnw.cmd '-Dtest=PerfilEdicaoRf08IntegrationTest,ManuDumpSchemaIntegrationTest' test` | 34 | 34 | 0 | 0 | 0 | `BUILD SUCCESS` |
| Completa | `backend> .\mvnw.cmd test` | 747 | 730 | 0 | 0 | 17 | `BUILD SUCCESS` |

Os 53 XMLs Surefire da execução completa confirmam **747/0/0/17**. Os 17 skips permanecem condicionais: 4 em `CurrentLocalSchemaIntegrationTest` e 13 em `OfficialLocalApiIntegrationTest`. Não foram ativadas `palco.current-db-tests` nem `palco.official-db-tests`.

**PostgreSQL/Testcontainers:** Docker Desktop operacional; imagem `postgres:18-alpine`, servidor PostgreSQL **18.4**, base `test` em porta efêmera, restaurada a partir do dump histórico pelo helper existente. O modo local opcional foi auditado no código, mas não executado nesta rodada. `spring.jpa.hibernate.ddl-auto=validate` foi mantido e verificado pela asserção; nenhum `create`/`update` de schema pelo Hibernate. O helper continua aplicando apenas sua preparação preexistente na base temporária.

**Limites de alteração:** backend de produção **sim**, apenas o predicado de bio do contratante; frontend **não**; banco oficial, schema versionado, dump, scripts SQL e migrations **não**. Testcontainers criou e descartou bases de teste; nenhum banco oficial foi renomeado ou modificado.

## Riscos e pendências

1. O perfil do contratante ainda usa `localizacao` como texto livre; os testes provam localização válida de exemplo (`São Paulo, SP`) e rejeição de valor em branco para completude, mas não comprovam validação estruturada de cidade/UF. O RF08 integral (inclusive `@username`, raio/portfólio do artista e áreas múltiplas) permanece fora desta tarefa.
2. O modo local opcional de PostgreSQL não foi executado; sua criação/remoção com nome temporário e sua proteção de URL foram auditadas no helper, enquanto a execução real validou Docker/Testcontainers.
3. Os 17 testes condicionais não são cobertura desta execução. A versão remota do banco oficial não foi homologada por este resultado; `validate` comprova apenas o schema restaurado no container.

## Registro semanal — 28/09/2026

| Campo | Registro |
|---|---|
| Objetivo | Estabilizar as duas failures finais antes de retomar RF26. |
| RF/RNF | RF08; RNF07 e RNF10. |
| Backend de produção | Sim: uma condição obsoleta de bio removida. |
| Testes/infraestrutura | Dois cenários RF08 separados; unitário atualizado; asserção de banco descartável vinculada ao helper. |
| Frontend/banco/schema/migrations | Não alterados. |
| Resultado | 747 testes; 730 passed; 0 failures; 0 errors; 17 skipped condicionais; `BUILD SUCCESS`. |
| Pendências | Validar cidade/UF estruturada e RF08 integral em escopo próprio; modo local opcional e skips continuam sem execução. |
| Próximo passo | Retomar RF26 usando este baseline verde, sem tratar os requisitos ainda pendentes como concluídos. |

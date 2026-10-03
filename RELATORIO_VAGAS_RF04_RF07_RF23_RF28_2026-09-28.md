# Relatório — Vagas RF04, RF07, RF23 e RF28 — 28/09/2026

## Objetivo e base auditada

Aplicar o ciclo de vida revisado de vagas sem alterar o banco e preservar a propriedade no servidor. Referências: RF04 (criação/publicação), RF07 (edição), RF23 (estados e prazo), RF28 (cancelamento), RNF08 (segurança da API) e RNF10 (testabilidade). O RF12 citado no pedido revisado trata de **Logout**; nenhuma regra de logout foi alterada.

Antes da mudança, `StatusVaga` já continha `RASCUNHO`, mas `VagaService.criar` o rejeitava; `atualizar` aceitava inclusive `ENCERRADA` e `CANCELADA`; `gerenciarStatus` não publicava rascunhos nem reabria encerradas; o `DELETE` só cancelava vagas publicadas. O cancelamento transacional, o encerramento automático por prazo e o filtro público para `ABERTA` já existiam e foram preservados. As consultas de detalhe e similares exigiram reforço para ocultar rascunhos até diante de uma candidatura histórica inconsistente.

## Contrato final

| Estado | Edição e ações do proprietário | Remoção |
|---|---|---|
| `RASCUNHO` | Editar e `PUBLICAR` → `ABERTA` | Exclusão física, sem motivo de cancelamento |
| `ABERTA` | Editar, `SUSPENDER` → `PAUSADA`, `ENCERRAR` → `ENCERRADA` | Cancelar → `CANCELADA`, com confirmação e motivo |
| `PAUSADA` | Editar, `REABRIR` → `ABERTA`, `ENCERRAR` → `ENCERRADA` | Cancelar → `CANCELADA`, com confirmação e motivo |
| `ENCERRADA` | `REABRIR` → `ABERTA`; sem edição livre prévia | Sem exclusão física ou cancelamento |
| `CANCELADA` | Nenhuma alteração funcional | Sem exclusão física |

`POST /api/vagas` aceita `status` ausente/`ABERTA` para publicação ou `RASCUNHO` para salvar. Outros estados enviados pelo cliente são rejeitados. `PUT /api/vagas/{id}` edita somente rascunho, aberta e pausada. `PATCH /api/vagas/{id}/status` centraliza `PUBLICAR`, `SUSPENDER`, `REABRIR` e `ENCERRAR`; aceita `dataLimiteCandidatura` apenas para publicar/reabrir. `DELETE /api/vagas/{id}` exclui fisicamente apenas rascunho; para aberta/pausada mantém o cancelamento RF28, que exige corpo com `confirmacao=true` e `motivo`.

Todas as operações administrativas carregam a vaga do banco e comparam seu contratante ao usuário autenticado. O `contratanteId` legado no corpo da criação continua sem autoridade. Visitantes e artistas não administram vagas; outro contratante recebe 403. O rascunho fica fora do feed e não pode ser usado como origem da consulta pública de similares; seu detalhe só é visível ao proprietário. Uma vaga publicada nunca volta a `RASCUNHO`.

Para publicar, o backend mantém validação de título, descrição, área, cidade/UF, tipo de contrato, abrangência, modalidade, remuneração e compatibilidade Área → Função → Especialização; os limites de até cinco funções e cinco especializações seguem no DTO. Experiência e data limite são opcionais. Requisitos separados são opcionais no RF04: quando ausentes, o campo histórico `requisitos NOT NULL` recebe texto vazio, sem conteúdo fictício. Valor único segue representado por mínimo = máximo; `A_COMBINAR` dispensa valor. `afirmativa=true` exige categoria do catálogo persistido; `false` não aceita categorias. O novo `GET /api/vagas/categorias-afirmativas` entrega IDs e nomes reais ao formulário autenticado.

Prazo informado deve ser posterior à data atual. A rotina `VagaPrazoService` continua encerrando `ABERTA` e `PAUSADA` vencidas de modo idempotente. `ENCERRADA` sem prazo anterior pode reabrir sem criar um; prazo vencido exige nova data futura. A mesma proteção impede reabrir `PAUSADA` com prazo vencido. O encerramento manual não apaga candidaturas. O cancelamento continua atualizando candidaturas, gravando log e notificações na transação e preservando o histórico.

## Limites do schema e frontend

O snapshot candidato D1 já contém o enum de cinco estados, mas `vagas` exige fisicamente título, descrição, requisitos, área, remuneração, cidade, UF, tipo de contrato e abrangência. Por isso o rascunho pode omitir modalidade e dados opcionais, mas **não** é um formulário vazio. Nenhum valor fictício foi criado para contornar `NOT NULL`; o texto vazio de requisitos representa a ausência real desse campo separado. O modo local legado opcional ainda bloqueia rascunhos quando seu enum não os suporta.

No cliente integrado `palco-comunidades-agenda`, o formulário preserva o layout e oferece salvar rascunho/publicar, modalidade obrigatória apenas para publicar, requisitos opcionais e seleção afirmativa por catálogo. O gerenciamento mostra ações por estado, solicita nova data quando o prazo venceu, usa confirmação/motivo para cancelar e confirmação para excluir rascunho. O frontend legado foi auditado como referência, sem criar uma segunda implementação do fluxo. O formulário integrado ainda não expõe seletores para funções/especializações, embora a API aceite e valide esses IDs; isto permanece uma pendência de interface.

## Arquivos alterados nesta tarefa

- Backend de produção: `backend/src/main/java/com/portifolio/service/VagaService.java`, `controller/VagaController.java`, `dto/VagaAtualizacaoRequest.java` e `dto/VagaStatusAcaoRequest.java`.
- Testes: `VagaPublicacaoRf04IntegrationTest.java`, `VagaEdicaoRf07IntegrationTest.java`, `VagaGerenciamentoRf31IntegrationTest.java`, `VagaCancelamentoRf25IntegrationTest.java` e `VagaServiceTamanhoTest.java` sob `backend/src/test/java/com/portifolio/`.
- Frontend integrado: `palco-comunidades-agenda/src/lib/api.js` e `src/pages/vacancies/{OfficialVacancies.jsx,VacancyForm.jsx,contracts.js,contracts.test.js}`.
- `graphify-out/` foi atualizado por `graphify update .`, conforme `AGENTS.md`.

Nenhum arquivo de `database/**`, dump, SQL, migration, tabela, coluna, enum, constraint, índice, trigger ou função foi alterado nesta tarefa. `spring.jpa.hibernate.ddl-auto=validate` permanece. O working tree já continha muitas mudanças locais, inclusive em `database/**`; elas foram preservadas. Não houve commit, push, reset, restore, stash ou alteração no banco oficial.

## Verificação

- Testes focados finais: **163 testes, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**. Incluídos os cinco grupos solicitados e, pelo impacto na privacidade, listagem RF03 e detalhe RF05. A primeira rodada focada detectou somente uma asserção de ordem do catálogo no teste novo; a asserção passou a verificar a presença do ID, sem assumir ordenação do seed.
- Contratos do frontend: `node --test src/pages/vacancies/contracts.test.js` — **16/16 passaram**.
- Cliente integrado: `npm run build` — **BUILD SUCCESS**. A primeira tentativa restrita encontrou acesso negado ao carregar a configuração do Vite; a execução com acesso ao workspace passou.
- `.\mvnw.cmd test` completo final: **779 total, 762 passed, 0 failures, 0 errors, 17 skipped condicionais, BUILD SUCCESS**. Log: `backend/target/vagas-full-final-2026-09-28.log`. Comparado ao baseline informado (771/754/0/0/17), foram adicionados oito testes líquidos, sem regressão observada. Uma execução completa anterior às duas proteções finais de privacidade também passou (778/0/0/17); o resultado de 779 é o que valida o código final.
- Os testes de integração usam `OfficialPostgreSQLContainer`, PostgreSQL 18 em banco `test` descartável via Testcontainers. O helper restaura o dump candidato e aplica a correção RF07 preexistente de `vagas.ultima_atualizacao` na cópia de testes; a versão remota oficial não foi homologada.

## Riscos, pendências e próximo passo

O cadastro de categorias afirmativas depende de dados reais no catálogo; sem eles, a opção “Sim” não pode ser publicada. A interface ainda não permite escolher funções/especializações, apesar da validação de API. O schema atual limita a edição de rascunhos realmente esparsos. A divergência de `ultima_atualizacao` entre D1 e o candidato local B1 segue fora deste escopo; a versão oficial remota precisa de verificação própria. Os 17 skipped condicionais não são cobertura desta execução e não foram habilitados artificialmente.

Nota operacional: a primeira compilação incremental dos testes falhou por classes ausentes no classpath; para recuperá-la foi executado `mvnw.cmd clean test` em `backend/`. O `clean` removeu o diretório gerado `backend/target`, inclusive logs antigos de RF26/RF27 ali guardados. Nenhum arquivo-fonte ou mudança versionada foi restaurado ou removido; os logs finais desta tarefa estão novamente em `target/`.

Próximo passo: completar os seletores de taxonomia no frontend e homologar o contrato do banco oficial remoto em tarefas próprias, mantendo a regra de não alterar schema nesta entrega.

## Registro semanal — 28/09/2026

| Campo | Registro |
|---|---|
| Objetivo | Fechar RF04, RF07, RF23 e RF28 no fluxo de vagas. |
| Backend | Ciclo de vida, propriedade, privacidade de rascunho, prazo e bloqueio de canceladas corrigidos. |
| Frontend | Ações por estado, rascunho, reabertura com prazo e cancelamento. |
| Banco/migrations | Nenhuma alteração nesta tarefa; `ddl-auto=validate`. |
| Testes | 163 focados, 16 contratos de frontend e build Vite verdes; `mvn test`: 779 total, 762 passed, 0 failures, 0 errors, 17 skipped, `BUILD SUCCESS`. |
| Pendências | Seletor visual de funções/especializações e homologação do banco remoto. |
| Próximo passo | Tratar as pendências acima sem misturar candidatura ou Banco de Talentos. |

# Fase 2 — Saneamento e Baseline

## Git

Upstream integration branch: `projetoPJI-10-08-ajustes`  
Upstream SHA inicial: `d351ff909266884446f78eac32364816c2b1e4b6`  
Checkpoint SHA: `fe4a18a638a47cdc50acb5680c999280f8c26545`  
Baseline branch: `sync/baseline-2026-08-25`  
Baseline base SHA: `d351ff909266884446f78eac32364816c2b1e4b6`  
Baseline final SHA: `858088939ba4cab7d59bdad484ade9688fae114e`  
Automation branch SHA antes desta atualização: `7ef77d1cf205252b48214ffd3db64c2e92230949`

## Reconciliação

Arquivos provenientes do checkpoint: 181 arquivos no delta final do baseline; a aplicação inicial reconciliou 181 caminhos porque o único item da interseção já possuía estado final equivalente.  
Conflitos detectados: 1 interseção — `RELATORIO_AUDITORIA_BACKEND_RF06_RF07_RF08.md`, removido nos dois lados.  
Conflitos resolvidos automaticamente: 1, preservando a remoção equivalente sem reintroduzir o arquivo.  
Conflitos bloqueados: 0.  
Mudanças novas do upstream preservadas: SIM; o baseline nasceu diretamente do HEAD atual auditado e nenhuma alteração posterior adicional existia além da remoção equivalente.

## SECURITY-JWT-001

Estado anterior: `jwt.secret=${JWT_SECRET:MinhaChaveSuperSecreta2026SosArtistas}`.  
Mudança realizada: fallback previsível removido; configuração passou a exigir `JWT_SECRET` externo e o teste WebSocket passou a ler o segredo apenas do ambiente.  
Novo estado: `jwt.secret=${JWT_SECRET}`; nenhum segredo JWT foi commitado.  
Teste: 280/280 testes backend aprovados com segredo efêmero fornecido somente ao processo.  
Status blocker: `CLOSED`.

Observação fora do escopo: o fallback de desenvolvimento em `spring.datasource.password` foi registrado, mas não alterado.

## FRONTEND-TEST-001

Node: `22.23.2` e `20.20.2`.  
npm: `10.9.8` e `10.8.2`; tentativa de obter `10.8.0` bloqueada por validação TLS.  
Método: imagens Node oficiais, containers efêmeros `--rm`, fonte do host montada somente para leitura e copiada para volume temporário.  
npm ci: `FAIL — BLOCKED_BY_ENVIRONMENT` após três tentativas.  
Suites: NÃO EXECUTADAS.  
Tests: NÃO EXECUTADOS.  
Passed: não aplicável.  
Failed: não aplicável; nenhum teste funcional iniciou.  
Skipped: não aplicável.  
Build: NÃO EXECUTADO.  
Status blocker: `OPEN`.

As tentativas 1 e 2 pararam no erro interno `Exit handler never called!` do npm; a tentativa 3 parou em `UNABLE_TO_VERIFY_LEAF_SIGNATURE`. Nenhuma correção funcional foi feita e nenhum `node_modules` ou `build` foi criado no host.

## Backend

Comando: `.\mvnw.cmd clean test '-Dspring.jpa.show-sql=false'`, com `JWT_SECRET` aleatório de 64 bytes disponível apenas ao processo.  
PostgreSQL: `18.4`, descartável via Testcontainers (`postgres:18-alpine`).  
ddl-auto: `validate`.  
Total: 280.  
Passed: 280.  
Failures: 0.  
Errors: 0.  
Skipped: 0.  
BUILD: `SUCCESS`.

## Banco

Nova alteração:  
NÃO

Alteração histórica RF25 preservada:  
SIM

## Frontend

Mudança funcional nova:  
NÃO

Testes executados:  
NÃO

## RF24

Status:  
PARCIAL

Banco alterado para RF24:  
NÃO

## PJI Guard

Resultado: `PASS DE ESCOPO COM REVIEW_REQUIRED ESPERADO` — 181 caminhos: 150 `ALLOW`, 31 `REVIEW_REQUIRED`, zero `BLOCK`. Banco limitado aos quatro arquivos históricos do RF25, 21 caminhos frontend idênticos ao checkpoint, dois caminhos adicionais somente para JWT e zero arquivo de automação no baseline.

## Estado da automação

phase: `BASELINE_SYNC`  
current_rf: `NONE_DURING_BASELINE_SYNC`  
pr_ready: `false`

Blockers:

- SECURITY-JWT-001: `CLOSED`
- FRONTEND-TEST-001: `OPEN — BLOCKED_BY_ENVIRONMENT`
- RF24-SCHEMA-001: `BLOCKED_BY_DATABASE`
- BASELINE-SYNC-001: `OPEN`; não bloqueia `pr_ready` ou o PR histórico, mas bloqueia RF novo, fase `DEVELOPMENT` e o próximo ciclo funcional.

## Push

Baseline enviado ao fork: NÃO — todos os gates obrigatórios não passaram.  
Automation atualizada no fork: NÃO — atualização administrativa mantida localmente pela mesma condição de gates.  
Original alterado:  
NÃO

PR aberto:  
NÃO

## Conclusão

`FASE2_PRONTA = NÃO`

`READY_FOR_TUESDAY_PR = NÃO`

Bloqueio exato: `FRONTEND-TEST-001` permanece aberto porque `npm ci` não concluiu nas três tentativas ambientais autorizadas; por isso testes e build frontend não foram executados. O baseline e a atualização administrativa permanecem apenas locais, sem PR, merge ou push.

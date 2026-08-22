# PJI Palco — Histórico semanal da automação

## 2026-W34

- Data do bootstrap: 2026-08-22.
- Fase operacional: `BASELINE_SYNC`.
- RF atual: `NONE_DURING_BASELINE_SYNC`.
- Checkpoint de referência: `fe4a18a638a47cdc50acb5680c999280f8c26545`.
- Integração oficial observada: `d351ff909266884446f78eac32364816c2b1e4b6`.
- Validação backend herdada do checkpoint: 280 executados, 280 aprovados, 0 falhas, 0 erros, 0 ignorados, `BUILD SUCCESS`, PostgreSQL 18/Testcontainers.
- Validação frontend herdada do checkpoint: `NOT_EXECUTED`.
- Bloqueios abertos: `SECURITY-JWT-001`, `FRONTEND-TEST-001`, `RF24-SCHEMA-001`, `BASELINE-SYNC-001`.
- Aprovação histórica registrada: `DB-RF25-001`; não reutilizável para alterações futuras.
- Trabalho desta semana: apenas bootstrap de políticas, estado, roadmap, guardas, runbook e instruções locais do Codex.
- Nenhuma RF nova iniciada, nenhum PR aberto, nenhum merge realizado e nenhuma automação agendada ativada.
- Publicação da Fase 1 em `pji-automation`: concluída no fork; fechamento administrativo confirmado em `7ef77d1cf205252b48214ffd3db64c2e92230949`.

### Fase 2 — saneamento e baseline

- Upstream auditado em `d351ff909266884446f78eac32364816c2b1e4b6`; branch local `sync/baseline-2026-08-25` criada diretamente desse SHA.
- Delta funcional do checkpoint reconciliado em commit honesto único; o relatório removido em ambos os lados não foi reintroduzido.
- `SECURITY-JWT-001` corrigido em `858088939ba4cab7d59bdad484ade9688fae114e`: fallback previsível removido, com segredo de teste fornecido somente ao processo.
- Backend reexecutado: 280 testes aprovados, zero falhas/erros/ignorados, `BUILD SUCCESS`, PostgreSQL 18.4 via Testcontainers e `ddl-auto=validate`.
- Frontend: três tentativas ambientais em containers Node oficiais; `npm ci` não completou e testes/build não foram executados. Nenhuma mudança funcional ou artefato no host.
- `FRONTEND-TEST-001` permanece aberto e bloqueia `pr_ready`; `RF24-SCHEMA-001` permanece bloqueado por banco; `BASELINE-SYNC-001` permanece aberto sem bloquear o futuro PR histórico.
- Como nem todos os gates passaram, nenhuma branch da Fase 2 foi enviada e nenhum PR/merge foi realizado.

### Fase 2B — preservação e recuperação do ambiente frontend

- A política foi corrigida para permitir push ao fork como backup/WIP mesmo com `pr_ready=false`; PR continua exigindo `pr_ready`, push ao upstream continua proibido e merge exige aprovação explícita.
- `sync/baseline-2026-08-25` foi preservada exclusivamente no fork com SHA local/remoto `858088939ba4cab7d59bdad484ade9688fae114e`; nenhum PR foi aberto.
- O host Windows alcançou `https://registry.npmjs.org/` com HTTP 200, sem proxy WinHTTP/WinINET ou variáveis de proxy; a cadeia TLS apresentou o interceptor local `Avast Web/Mail Shield Root`.
- Node.js portátil oficial `22.23.2` foi baixado em TEMP e verificado pelo `SHASUMS256.txt`: SHA-256 `1177B4137BA5ADAA56354AE40F1080C7450E8AE09CECB47DA459D1C52AC99F97`.
- Com `NODE_USE_SYSTEM_CA=1` somente no processo, `npm ping` e `npm ci --no-audit --no-fund` passaram na cópia temporária.
- Testes frontend executados: 4 suítes, 3 aprovadas e 1 reprovada; 13 testes, 11 aprovados e 2 reprovados em `rf09-recuperacao-senha.test.js`. Build frontend: `BUILD SUCCESS`.
- Nenhuma correção funcional foi realizada. `FRONTEND-TEST-001` permanece aberto, `pr_ready=false`, `FASE2_PRONTA=NÃO` e `READY_FOR_TUESDAY_PR=NÃO`.

O fechamento desta semana e a abertura de uma semana seguinte exigem decisão explícita conforme `.pji/POLICY.yaml`.

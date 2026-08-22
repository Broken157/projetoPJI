# FASE 2B — FRONTEND ENVIRONMENT RECOVERY

Baseline branch: `sync/baseline-2026-08-25`  
Baseline SHA local: `858088939ba4cab7d59bdad484ade9688fae114e`  
Baseline SHA remoto: `858088939ba4cab7d59bdad484ade9688fae114e`  
Baseline preservado no fork: SIM

Host registry connectivity: PASS — `Invoke-WebRequest` recebeu HTTP 200 de `https://registry.npmjs.org/`. Não havia variáveis `HTTP_PROXY`, `HTTPS_PROXY`, `NO_PROXY` ou `ALL_PROXY`; WinHTTP e WinINET estavam em acesso direto. O certificado apresentado tinha subject `CN=npmjs.org` e issuer `Avast Web/Mail Shield Root`, indicando inspeção TLS local confiada pelo Windows.

Node: `v22.23.2`  
npm: `10.9.8`  
Método: ZIP oficial Windows x64 em TEMP, verificado contra `SHASUMS256.txt`; frontend copiado para TEMP com `package.json` e `package-lock.json` idênticos ao baseline.  
SHA-256 do ZIP: `1177B4137BA5ADAA56354AE40F1080C7450E8AE09CECB47DA459D1C52AC99F97` — PASS.  
NODE_USE_SYSTEM_CA: `1`, somente no processo

npm ping: PASS (`PONG`)  
npm ci: PASS — 1.315 pacotes  
Suites: 4 total; 3 aprovadas; 1 reprovada  
Tests: 13 total  
Passed: 11  
Failed: 2 — `frontend/src/rf09-recuperacao-senha.test.js`  
Skipped: 0  
Build: PASS — `Compiled successfully`

FRONTEND-TEST-001: `OPEN — FRONTEND_TEST_FAILURE`. Os dois testes RF09 esperavam as mensagens “Se o e-mail estiver cadastrado” e “Senha redefinida com sucesso”, mas os respectivos elementos ficaram vazios. Nenhuma correção funcional foi autorizada ou realizada.

SECURITY-JWT-001:  
CLOSED

phase:  
BASELINE_SYNC

pr_ready:  
false

RF24-SCHEMA-001:  
BLOCKED_BY_DATABASE

BASELINE-SYNC-001:  
OPEN

Arquivos funcionais alterados:  
NENHUM

Banco alterado:  
NÃO

Frontend alterado:  
NÃO

Backend alterado:  
NÃO

Original alterado:  
NÃO

PR:  
NÃO

Push baseline ao fork:  
SIM

Push automação ao fork:  
SIM

`FASE2_PRONTA = NÃO`

`READY_FOR_TUESDAY_PR = NÃO`

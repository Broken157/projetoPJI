# RF19 — isolamento da dependência de teste `jsdom`

Execução: 29/09/2026. Referência do RF19: 28/09/2026.

## Diagnóstico

`palco-comunidades-agenda/src/pages/saved/ui.test.js` importava `JSDOM` de `../../../../frontend/node_modules/jsdom/lib/api.js`. Antes da correção, `jsdom` não constava do `package.json` nem do `package-lock.json` do frontend integrado, não existia em seu `node_modules`, e `require.resolve('jsdom')` falhava nesse diretório. O teste dependia da instalação do projeto irmão.

## Correção

| Arquivo | Alteração |
|---|---|
| `palco-comunidades-agenda/package.json` | Adicionada somente a `devDependency` exata `jsdom: 30.1.0`. |
| `palco-comunidades-agenda/package-lock.json` | Atualizado pelo `npm install`; 36 pacotes novos para essa dependência, sem mudança de versão dos pacotes que já estavam no lockfile. |
| `palco-comunidades-agenda/src/pages/saved/ui.test.js` | Substituído o caminho para o projeto irmão por `import {JSDOM} from 'jsdom'`. Nenhuma asserção ou lógica do teste foi alterada. |

`jsdom@30.1.0` declara Node `^22.22.2 || ^24.15.0 || >=26.0.0`; o ambiente verificado usa Node `v22.23.2`. O acesso ao npm usou a cadeia de certificados do Windows, mantendo a validação TLS; não houve alteração persistente da configuração do npm.

## Verificação

- `npm ci --no-audit --no-fund --fetch-retries=0`: **sucesso** em instalação local pelo lockfile.
- `npm ls jsdom --depth=0`: **jsdom@30.1.0**. `require.resolve('jsdom')` aponta para `palco-comunidades-agenda/node_modules/jsdom/lib/api.js`.
- Inspeção de `ui.test.js`, `package.json` e `package-lock.json`: sem `frontend/node_modules`, caminho externo ou `NODE_PATH`.
- `node --test src/pages/saved/ui.test.js`: **4/4 passaram, 0 failures**.
- `npm run build`: **sucesso**, 118 módulos Vite. O bundle de produção manteve os mesmos nomes de arquivo do build anterior à correção.
- Maven não foi executado, conforme o escopo.

**Comportamento funcional do RF19: inalterado. Backend: não alterado. Banco, schema, SQL e migrations: não alterados.** Não houve commit, push nem operações destrutivas de Git.

Pendência ambiental: a execução isolada do teste exige uma versão de Node aceita pelo `jsdom@30.1.0`; o Node local atende esse requisito. Não há pendência funcional identificada nesta microcorreção.

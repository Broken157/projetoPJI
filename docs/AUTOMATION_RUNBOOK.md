# Runbook da automação controlada — PJI Palco

## 1. Pré-voo obrigatório

Execute no worktree pretendido antes de qualquer mudança:

```powershell
git rev-parse --show-toplevel
git branch --show-current
git rev-parse HEAD
git remote -v
git status --short --branch
git fetch origin --prune
git fetch upstream --prune
```

Confirme:

- `origin` = `https://github.com/Broken157/projetoPJI.git`;
- `upstream` = `https://github.com/manugomesds/projetoPJI.git`;
- checkpoint = `fe4a18a638a47cdc50acb5680c999280f8c26545` no fork;
- nenhum checkpoint/arquivo de automação no original;
- branch atual compatível com a tarefa;
- worktree limpo ou mudanças existentes compreendidas e preservadas.

## 2. Carregar estado sem integrar a automação

Em um worktree funcional, leia a infraestrutura sem fazer merge:

```powershell
git show pji-automation:.pji/STATE.yaml
git show pji-automation:.pji/POLICY.yaml
git show pji-automation:.pji/APPROVALS.yaml
git show pji-automation:.pji/BLOCKERS.yaml
git show pji-automation:.pji/ROADMAP.yaml
```

Se `phase` for `BASELINE_SYNC`, pare qualquer implementação de RF. Auditoria e preparação de evidência podem continuar em modo read-only.

## 3. Executar status e validações

Use Python 3. No ambiente atual, `python` não estava no `PATH`; o bootstrap foi validado com o runtime empacotado pelo Codex:

```powershell
$PjiPython = 'C:\Users\masca\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
& $PjiPython scripts/pji_status.py
& $PjiPython scripts/pji_guard.py --self-test
& $PjiPython scripts/pji_validate.py
```

Em outro ambiente, substitua `$PjiPython` por um Python 3 confiável. Nenhuma biblioteca externa é necessária.

## 4. Classificar uma mudança antes de editar

Exemplos:

```powershell
& $PjiPython scripts/pji_guard.py --path 'docs/AUTOMATION_RUNBOOK.md'
& $PjiPython scripts/pji_guard.py --path 'database/schema-test.sql'
& $PjiPython scripts/pji_guard.py --path '.env'
& $PjiPython scripts/pji_guard.py --text 'spring.jpa.hibernate.ddl-auto=update'
& $PjiPython scripts/pji_guard.py --git-command 'git push upstream pji-automation'
```

Interpretação:

- `ALLOW`/código 0: nenhuma categoria protegida detectada; ainda exige fase e escopo autorizados.
- `REVIEW_REQUIRED`/código 2: pare antes de editar e obtenha aprovação explícita.
- `BLOCK`/código 3: não execute; a ação viola uma proibição ou inclui material que não pode ser commitado.
- `--self-test` retorna código 0 somente quando os exemplos determinísticos passam.

## 5. Aprovações

Antes de qualquer item de `.pji/POLICY.yaml:approval_required`:

1. descreva a necessidade, arquivos e impacto;
2. obtenha autorização explícita;
3. registre uma aprovação nova e exata em `.pji/APPROVALS.yaml`;
4. não reutilize `DB-RF25-001`, que é histórica;
5. valide o estado antes de começar.

RF24 permanece `BLOCKED_BY_DATABASE`. Não desenhe/aplique schema nem marque o requisito como completo sem aprovação nova.

## 6. Tratamento dos bloqueios

- `SECURITY-JWT-001`: não corrigir no bootstrap. Uma mudança significativa de JWT/Spring Security exige aprovação e regressão específica.
- `FRONTEND-TEST-001`: na fase seguinte, solicitar autorização de Docker e tentar Node em ambiente isolado; não alterar funcionalidade para fazer o teste passar.
- `RF24-SCHEMA-001`: manter RF24 parcial até aprovação e evidência de schema compatível.
- `BASELINE-SYNC-001`: nenhuma RF nova enquanto o checkpoint não for sincronizado por fluxo autorizado.

Fechar um bloqueio exige evidência reproduzível e atualização coerente de `BLOCKERS.yaml`, `STATE.yaml`, `ROADMAP.yaml` e `WEEKLY_LOG.md`.

## 7. Git seguro da infraestrutura

Somente na branch `pji-automation`:

```powershell
git status --short --branch
git diff --check
& $PjiPython scripts/pji_validate.py
git add .pji docs/AUTOMATION_ARCHITECTURE.md docs/AUTOMATION_RUNBOOK.md scripts/pji_guard.py scripts/pji_validate.py scripts/pji_status.py RELATORIO_BOOTSTRAP_AUTOMACAO_FASE1.md
git diff --cached --name-status
git commit -m 'automation: bootstrap controlled phase 1'
git push -u origin pji-automation
```

Nunca use `upstream` como destino. Nunca abra PR da infraestrutura. Nunca integre `pji-automation` em branch funcional.

## 8. Instalar a skill local específica do PJI

A instalação é local por worktree e deliberadamente não versionada:

```powershell
New-Item -ItemType Directory -Force '.agents/skills' | Out-Null
Copy-Item -Recurse -Force '.pji/codex-skill/pji-palco-control' '.agents/skills/pji-palco-control'
Add-Content -LiteralPath (git rev-parse --git-path info/exclude) -Value '.agents/skills/pji-palco-control/'
```

Antes de adicionar o padrão, confirme que ele ainda não existe para evitar duplicação. Valide a skill com o `quick_validate.py` do `skill-creator`. Reinicie/inicie uma nova tarefa do Codex nesse worktree para reconstruir a cadeia de descoberta.

## 9. Fechamento semanal

Atualize o histórico com fatos comprovados, comandos, resultados, decisões, bloqueios e pendências. Não inicie nova semana após `WEEK_CLOSED` sem autorização explícita.

## 10. Condições de parada

Pare e reporte, sem contornar:

- aprovação ausente;
- tentativa de alterar banco/frontend/segurança fora do escopo;
- branch/remotes/SHA divergentes;
- teste obrigatório indisponível ou falho;
- mudança funcional durante `BASELINE_SYNC`;
- qualquer ação que alcance upstream, checkpoint ou histórico de forma mutável;
- evidência insuficiente para declarar requisito, blocker ou PR como concluído.

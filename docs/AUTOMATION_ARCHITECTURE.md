# Arquitetura da automação controlada — PJI Palco

## Objetivo e limite da Fase 1

Esta arquitetura registra estado, política, aprovações, bloqueios, evidências e validações do PJI Palco sem executar RF automaticamente. A fase inicial é `BASELINE_SYNC`: até o bloqueio `BASELINE-SYNC-001` ser resolvido por um fluxo explicitamente autorizado, nenhuma RF nova pode começar.

Esta infraestrutura não altera comportamento da aplicação, banco, frontend, API ou segurança. Ela existe exclusivamente na branch `pji-automation` do fork.

## Separação de repositórios e branches

```text
original/upstream
└── projetoPJI-10-08-ajustes @ d351ff909266884446f78eac32364816c2b1e4b6
    └── não recebe arquivos de automação nem push direto

fork/origin
├── checkpoint/pre-automacao-2026-08-22 @ fe4a18a638a47cdc50acb5680c999280f8c26545
│   └── imutável
└── pji-automation (baseada no checkpoint)
    └── somente política, estado, roadmap, aprovações, histórico, validação e documentação
```

Invariantes:

- `checkpoint/pre-automacao-2026-08-22` não recebe commit, merge, rebase, reset ou force-push.
- `pji-automation` é publicada somente em `origin` e nunca é integrada ao original.
- arquivos desta arquitetura não entram em PR funcional.
- a branch oficial de integração não é modificada durante o bootstrap.

## Componentes

| Componente | Responsabilidade |
|---|---|
| `.pji/STATE.yaml` | Snapshot operacional legível por máquina. |
| `.pji/ROADMAP.yaml` | Registro conservador de estados comprovados; não agenda RF. |
| `.pji/POLICY.yaml` | Ações automáticas, ações que exigem aprovação e proibições. |
| `.pji/APPROVALS.yaml` | Aprovações exatas, históricas ou ativas, sem reutilização implícita. |
| `.pji/BLOCKERS.yaml` | Bloqueios, evidências, efeitos e condições de resolução. |
| `.pji/WEEKLY_LOG.md` | Histórico semanal auditável. |
| `scripts/pji_guard.py` | Classificador read-only de paths, conteúdo sensível e comandos Git. |
| `scripts/pji_validate.py` | Validador de estado, referências Git, escopo do diff e guard. |
| `scripts/pji_status.py` | Exibição humana ou JSON do estado atual. |

Os arquivos `.yaml` usam o subconjunto JSON compatível com YAML 1.2. Assim, o parsing é determinístico com a biblioteca padrão do Python, sem instalar dependências ou alterar builds.

## Máquina de estado e gates

Transições não são automáticas. Cada mudança de fase exige evidência atualizada em `pji-automation`, validação e, quando aplicável, aprovação explícita.

```text
BASELINE_SYNC
  └─ bloqueia NEW_RF e PR_READY enquanto BASELINE-SYNC-001 estiver OPEN

RF_PLANNING
  └─ só após baseline autorizado e sincronizado; escolhe RF com evidência

RF_IN_PROGRESS
  └─ somente escopo explicitamente autorizado

VALIDATION
  └─ testes e auditoria de regressão proporcionais ao risco

PR_READY
  └─ somente sem bloqueadores de PR e com evidência reproduzível

WEEK_CLOSED
  └─ uma nova semana exige autorização
```

O bootstrap permanece em `BASELINE_SYNC`; o diagrama define gates operacionais, não autoriza avançá-los.

## Decisão sobre instruções persistentes do Codex

A documentação oficial informa que o Codex carrega `AGENTS.md` por escopo global/projeto antes do trabalho e que instruções mais próximas do diretório atual prevalecem. Ela também informa que skills podem ser descobertas no escopo do repositório em `.agents/skills` e acionadas implicitamente quando a descrição corresponde à tarefa: [AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md) e [skills](https://learn.chatgpt.com/docs/build-skills).

| Opção auditada | Avaliação | Decisão |
|---|---|---|
| `AGENTS.md` local fora do Git | Um arquivo acima da raiz Git não pertence ao escopo de descoberta do projeto; um arquivo global afetaria outros projetos. | Rejeitada. |
| `AGENTS.md` local ignorado | Funciona por worktree, mas carrega todas as regras em toda tarefa e pode ser perdido em novo clone/worktree. | Não escolhida. |
| Configuração do Codex | Fallbacks e configuração em `~/.codex/config.toml` têm alcance de usuário e podem afetar repositórios não relacionados. | Rejeitada. |
| Skill específica do PJI | Tem descrição discriminante, divulgação progressiva e pode ficar local ao repositório, ignorada pelo Git. | **Escolhida.** |
| Instruções de automação/tarefa agendada | Seriam adequadas para execução recorrente, que está proibida nesta fase. | Rejeitada e não ativada. |

A fonte versionada da skill fica em `.pji/codex-skill/pji-palco-control/SKILL.md`, exclusiva de `pji-automation`. A instalação local fica em `.agents/skills/pji-palco-control/SKILL.md` dentro do worktree usado pelo Codex, com `.agents/skills/pji-palco-control/` no `.git/info/exclude` local. Assim:

- a regra é específica do PJI;
- nenhum `AGENTS.md` ou arquivo da skill aparece em PR ao original;
- nenhuma configuração global é criada;
- novo clone/worktree exige instalação local consciente pelo runbook;
- a skill orienta e bloqueia gates, mas não executa RF sozinha.

## Guard e classificação

O guard é read-only e aplica precedência `BLOCK` > `REVIEW_REQUIRED` > `ALLOW`.

- `BLOCK`: `.env`/segredos/chaves, artefatos de build, `ddl-auto=create|update`, reset destrutivo, force-push e push ao original, mutação explícita do checkpoint.
- `REVIEW_REQUIRED`: `database/**`, SQL/migrations, `frontend/**`, configuração sensível de autenticação/JWT, builds/dependências/Docker/CI, merge/rebase/pull.
- `ALLOW`: leitura/auditoria, documentação e scripts da automação, backend comum dentro de uma RF já autorizada, fetch e push não forçado explicitamente ao fork.

O resultado do guard não cria autorização. `ALLOW` significa apenas que nenhuma categoria protegida foi detectada; fase, escopo e aprovações continuam obrigatórios.

## Bloqueios iniciais

- `SECURITY-JWT-001`: regressão de fallback JWT previsível; bloqueia `PR_READY`; não corrigida no bootstrap.
- `FRONTEND-TEST-001`: frontend alterado sem suíte automatizada executada; bloqueia `PR_READY`.
- `RF24-SCHEMA-001`: RF24 permanece parcial e bloqueado por necessidade de banco.
- `BASELINE-SYNC-001`: checkpoint ainda não está integrado ao original; bloqueia RF nova e `PR_READY`.

## Modelo de confiança

O estado operacional não substitui o código nem os relatórios. Para qualquer afirmação:

1. decisão explícita vigente;
2. política, aprovação e bloqueio em `.pji`;
3. relatório técnico existente;
4. código e teste executável;
5. `UNKNOWN` quando a evidência não existe.

Nenhum resultado é promovido por inferência, e falhas não podem ser ocultadas.

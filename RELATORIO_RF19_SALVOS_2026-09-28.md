# RF19 — Sistema de Salvos — 28/09/2026

## Diagnóstico e contrato

O RF19 já tinha backend para `PERFIL_ARTISTA` e `VAGA`, estado, contador, coleção privada, paginação e notificação `SALVO`. O frontend integrado já mostrava “Meus Salvos”, o botão no perfil e no detalhe da vaga, e a estrela na tela de talentos. A auditoria deste checkout encontrou três lacunas: a estrela de talentos criava o salvo sem confirmação; o visitante do perfil público não recebia uma ação para entrar e salvar; e a API de “Meus Salvos” podia devolver título, contratante, localização e status de vaga que deixara de ser acessível. A projeção da lista também precisava preservar a regra do detalhe: candidatura histórica não libera rascunho alheio.

O contrato aplicado é RF19 com RF10, RF36, RNF06 (minimização), RNF08 (JWT e propriedade) e RNF17 (paginação). Salvar continua distinto de Seguir/RF41 e de adesão ao Banco de Talentos/RF13. O RF17 foi tocado apenas na estrela de Salvos da superfície já existente.

## Estrutura e comportamento reaproveitados

O dump histórico `palco.manu.dump` foi inspecionado somente por `pg_restore --schema-only`: `itens_salvos` possui PK `id`, FK `usuario_id → usuarios(id)` com `ON DELETE CASCADE`, `tipo_alvo`, `alvo_id`, `data_salvamento` com `CURRENT_TIMESTAMP`, unicidade `(usuario_id,tipo_alvo,alvo_id)` e índice em `usuario_id`. Os enums já contêm `PERFIL_ARTISTA`, `VAGA`, `OBRA` e a notificação `SALVO`. `alvo_id` é polimórfico, sem FK física para perfil/vaga; a validação e a ocultação de alvos indisponíveis ficam no backend. O tipo `OBRA` permanece rejeitado pela API enquanto não houver domínio de obra definido no escopo funcional.

O service obtém o ator de `AuthenticatedUserResolver`/JWT e exige conta ativa. `SecurityConfig` autentica `/api/salvos/**`. O POST consulta RF10 para perfil e RF05 para vaga, e a unicidade existente com `ON CONFLICT DO NOTHING` impede duplicidade concorrente. Só a inserção vencedora gera `NotificacaoEvento`; o listener persiste após commit e isola falha de WebSocket/SSE. O texto não identifica quem salvou. Não existe coluna de contador: `quantidadeSalvos` é calculada por `COUNT` para o perfil público, sem lista ou identidade dos salvadores. A coleção usa `PageRequest`, ordenação `dataSalvamento DESC, id DESC`, padrão 20, máximo 50, filtro por tipo, count no servidor e projeções de alvos em lote. O frontend integrado pede 12 itens por página.

## Alterações desta tarefa

| Arquivo | Motivo e resultado |
|---|---|
| `backend/src/main/java/com/portifolio/service/SalvoService.java` | Redige metadados e link quando um alvo salvo deixa de estar acessível; mantém placeholder removível. |
| `backend/src/main/java/com/portifolio/repository/SalvoAlvoRepository.java` | Não considera candidatura histórica como autorização para ver rascunho alheio; segue a mesma regra de `VagaService.buscarPorId`. |
| `backend/src/test/java/com/portifolio/controller/SalvoRf19IntegrationTest.java` | Verifica redação de vaga em rascunho/pausada/encerrada/cancelada e o caso de candidatura histórica com rascunho. |
| `palco-comunidades-agenda/src/pages/talents/OfficialTalentBank.jsx` | Exige confirmação antes de salvar pela estrela; remoção permanece direta. |
| `palco-comunidades-agenda/src/components/SaveButton.jsx` e `ProfileShell.jsx` | Visitante vê “Salvar perfil” e é encaminhado ao login; usuário autenticado mantém modal, estado e contador existentes. |
| `palco-comunidades-agenda/src/pages/saved/ui.test.js` | Exercita interação real dos componentes via Vite/DOM: confirmação/cancelamento, payload, remoção, contador, visitante, erro, vazio, filtro e paginação. |

`Saved.jsx`, `VacancySave` no detalhe da vaga e a API compartilhada do frontend foram reaproveitados sem alteração. Os arquivos `ProfileShell.jsx` e `OfficialTalentBank.jsx` já eram não rastreados no working tree inicial; seu conteúdo preexistente foi preservado. `graphify update .` regenerou artefatos de `graphify-out/` conforme `AGENTS.md`.

## API e autorização finais

| Operação | Contrato |
|---|---|
| `POST /api/salvos` | Corpo `{tipoAlvo,alvoId}`; 201 criado, 200 já salvo; ator vem do JWT. |
| `DELETE /api/salvos/{tipoAlvo}/{alvoId}` | 204 idempotente; a chave de remoção inclui o usuário do JWT. |
| `GET /api/salvos/estado?tipoAlvo=&alvoId=` | Estado privado e contador agregado somente para perfil público elegível. |
| `GET /api/salvos?tipoAlvo=&page=0&size=20` | Coleção apenas do ator, filtro opcional e metadados de página. |

O ID enviado pelo cliente identifica somente o alvo; `usuarioId`, `donoId` ou `criadorId` do corpo não concedem autoridade. O POST rejeita alvo inexistente ou privado pela política de RF10/RF05. Um salvo de alvo posteriormente oculto continua removível, mas sua resposta não entrega nome, localização, contratante, status, avatar, funções ou link. O contador não divulga quem salvou. Salvar/remover não altera vaga, candidatura, seguidores nem participação no Banco de Talentos.

## Verificação

- Foco backend após a última alteração: `SalvoRf19IntegrationTest` — **25 testes, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**, no PostgreSQL 18 descartável do Testcontainers. O foco ampliado anterior (`SalvoRf19`, RF10, RF23 e `OfficialLocalApiIntegrationTest`) executou 60 testes, 0 falhas, 0 erros e 13 skipped condicionais; ocorreu antes do último ajuste de projeção.
- Frontend integrado: `node --test src/pages/saved/ui.test.js` — **4/4**, 0 falhas. `npm run build` — **sucesso**, 118 módulos Vite.
- Suíte completa final `.\mvnw.cmd test`: **784 total, 767 passed, 0 failures, 0 errors, 17 skipped, BUILD SUCCESS** (exit code 0; 56 XMLs Surefire conferidos). O baseline era 782/765/0/0/17; os dois casos adicionais são a variação `RASCUNHO` e a candidatura histórica. Log: `backend/target/rf19-full-final.log`.
- Configuração JPA: `spring.jpa.hibernate.ddl-auto=validate`. O suporte de teste existente restaura o dump e aplica o ajuste RF07 já previsto somente em containers temporários; nenhum banco oficial/compartilhado recebeu escrita e nenhuma migration foi criada/alterada nesta tarefa. Os skips `palco.current-db-tests` e `palco.official-db-tests` não foram habilitados.
- `git diff --check` dos arquivos rastreados alterados: **sem erros de whitespace**.

## Limites e riscos

- RF10 ainda exclui todos os artistas menores de 18 anos da projeção pública, inclusive os que possam ser autorizados pelo RF27 revisado, e não oferece `@username` público. RF19 reutiliza essa política atual sem implementar RF10 inteiro nesta tarefa. A liberação pública do menor precisa ser resolvida no próprio RF10/RF27 antes de permitir seu salvamento.
- `OBRA` continua dependente da definição do domínio correspondente; o enum histórico sozinho não autoriza suporte de produto.
- A persistência da notificação após commit não possui outbox/retry durável se a própria gravação posterior falhar; falha só do realtime já é isolada.
- O dump inspecionado é candidato histórico; a homologação do banco remoto oficial permanece separada.
- O teste DOM integrado usa o `jsdom` disponível no projeto irmão `frontend/node_modules`. Para executar esse teste em instalação isolada apenas de `palco-comunidades-agenda`, será preciso disponibilizar a dependência de teste. Isso não afeta o build ou o produto.
- Não houve homologação visual/manual com API ativa nesta tarefa; testes de componente, integração de backend e build verificam o contrato automatizado.

## Status, registro semanal e próximo passo

**RF19 concluído para os alvos atualmente disponíveis: `PERFIL_ARTISTA` público conforme RF10 atual e `VAGA`.** `OBRA` é dependência futura, sem suporte artificial. A suíte completa permaneceu verde. Backend de produção: **alterado pontualmente**. Frontend integrado: **alterado pontualmente**. Banco/schema/SQL/dump/migrations: **nenhuma alteração nesta tarefa**. O working tree já continha muitas mudanças locais, inclusive em `database/` e recursos de teste; elas não foram revertidas, incorporadas a esta entrega, commitadas ou enviadas.

| Campo | Registro em 28/09/2026 |
|---|---|
| RF e objetivo | RF19: fechar confirmação, privacidade da lista e acesso do visitante à ação Salvar. |
| Segurança/ownership | JWT como autoridade; remoção e coleção por ator; alvos inacessíveis redigidos. |
| Backend/frontend/banco | Backend e frontend ajustados; banco não alterado. |
| Testes e resultado | Foco backend 25/25, frontend 4/4 e build Vite aprovados; Maven completo 784 total, 767 aprovados, 0 failures, 0 errors, 17 skipped, BUILD SUCCESS. |
| Pendências | RF10 para menores/@username; domínio `OBRA`; limite de outbox RF36; homologação visual e do banco remoto. |
| Próximo passo | Homologar visualmente Salvar/Salvo nas superfícies integradas com API ativa e tratar a visibilidade de menores no RF10/RF27 em tarefa própria. |

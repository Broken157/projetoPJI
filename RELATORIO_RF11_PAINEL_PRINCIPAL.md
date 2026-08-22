# PJI Palco — Auditoria e Finalização do RF11: Painel Principal

## 1. Objetivo

Auditar e implementar o núcleo sustentável do RF11 com um painel autenticado e diferente para ARTISTA e CONTRATANTE, reutilizando autenticação, perfis, vagas, candidaturas, perfil público, tags e logout já existentes. O trabalho foi limitado ao RF11: não foram implementados RF13, RF17, RF23, RF24, RF28, RF29, RF36, chat, WebSocket, STOMP, score, medalhas ou novas estruturas de banco.

## 2. Estado inicial

- **Existente:** login JWT, `AuthenticatedUserResolver`, proteção global de rotas, perfis de artista/contratante, `perfilCompleto`, vagas e tags, candidaturas, perfil público, avatar, logout e uma página `dashboard-contratante.html`.
- **Correto e reutilizado:** identidade pelo JWT, sessão frontend, logout RF12, DTOs públicos RF10, carregamento em lote `VagaRepository.findByIdIn`, avatar e regras de enum já consolidadas.
- **Parcial:** a página de dashboard era apenas do contratante e misturava dados reais com métricas falsas; o login do artista redirecionava para `perfil.html`.
- **Incorreto no mock:** números fixos, candidatos fictícios, percentuais sem regra, zeros falsos para mensagens/talentos/alcance e N+1 no frontend via uma chamada `/usuarios/{id}` por candidatura.
- **Ausente:** endpoint central do painel, dashboard de artista e camadas Java funcionais de notificações e mensagens. As tabelas futuras existem no SQL, mas não há Entity, Repository, Service, Controller nem geração real desses dados.

## 3. Auditoria por camada

| Camada | Evidência e decisão |
|---|---|
| Entity | `Usuario`, `PerfilArtista`, `PerfilContratante`, `Vaga`, `Candidatura` e `Tag` já sustentavam o núcleo. Nenhuma Entity nova foi necessária. |
| DTO | Entities não são expostas. Foram criados DTOs específicos e sanitizados do dashboard. |
| Repository | Consultas de matching, candidaturas recentes e contexto de tags foram adicionadas com parâmetros e paginação. |
| Service | `DashboardService` agrega módulos por papel e usa somente o usuário resolvido pelo JWT. |
| Controller | `DashboardController` expõe somente `GET /api/dashboard`; não existe endpoint por ID de usuário. |
| Security | `SecurityConfig` já protege toda rota não liberada; não precisou ser alterado. Sem JWT o endpoint retorna 401. |
| Frontend | O mock existente foi reaproveitado como painel único; login, sessão e logout existentes foram preservados. |
| Testes | Foi criada suíte RF11 com PostgreSQL real/Testcontainers e estatísticas Hibernate. |

## 4. Contrato do dashboard

`GET /api/dashboard?size=5` é privado, deriva a identidade do JWT e aceita apenas `size` entre 1 e 50. O padrão é 5. A resposta discrimina `tipoUsuario`, fornece nome público, avatar, `perfilCompleto`, disponibilidade explícita de notificações/mensagens e apenas os módulos do papel atual. Cada coleção usa `content`, `totalElements` e `hasMore`; módulos do outro papel são omitidos com `@JsonInclude(NON_NULL)`. Não são retornados senha, e-mail, telefone, nascimento, responsável, token, refresh token ou hash.

## 5. Dashboard ARTISTA

### Perfil completo

O valor é o `perfilCompleto` persistido pelo fluxo RF08; o RF11 não recalcula nem bloqueia. Artista incompleto recebe HTTP 200 e o frontend mostra aviso evidente com link para `perfil.html`.

### Vagas para Você

Exibe somente vagas ABERTA com pelo menos uma tag coincidente, em preview paginado, com título, nome público do contratante, localização, modelo, remuneração, tags, coincidências e link para detalhe. Sem tags, retorna coleção vazia e orientação para completar o perfil; não há randomização.

### Notificações

Retorna `{disponivel:false, mensagem:...RF23}`. Não existe contador falso nem link funcional para módulo inexistente.

### Mensagens

Retorna `{disponivel:false, mensagem:...RF24}`. Não existe contador falso nem implementação de chat.

## 6. Dashboard CONTRATANTE

### Candidaturas recentes

Mostra somente candidaturas ligadas às vagas ABERTA ou PAUSADA do contratante autenticado, ordenadas por `dataCandidatura DESC, id DESC`. O card contém apenas vaga, artista, avatar, status, data e link para o perfil público; mensagem de apresentação e link privado da candidatura não são agregados ao dashboard.

### Talentos sugeridos

Mostra preview de artistas adultos, publicáveis e com `perfilCompleto=true`, com nome, avatar, bio/localização/portfolio públicos, tags, quantidade coincidente e link RF10.

### Notificações

Módulo explicitamente indisponível até RF23.

### Mensagens

Módulo explicitamente indisponível até RF24.

## 7. Recomendação por tags

- **Vagas:** origem nas tags persistidas de `PerfilArtista`; fórmula `|tagsArtista ∩ tagsVaga|`; filtro `status='aberta'`; ordem por coincidências DESC, `data_publicacao` DESC e ID DESC.
- **Talentos:** origem nas tags dos artistas e no contexto do contratante; ordem por coincidências DESC, `ultima_atualizacao` DESC e ID ASC.
- As consultas agrupam e ordenam no PostgreSQL, usam parameter binding e `Pageable`.
- Zero tags/contexto produz módulo vazio. Score, medalha, ranking, popularidade e randomização não participam.

## 8. Vagas ativas

Foi adotada e testada a interpretação: **ABERTA e PAUSADA** ainda pertencem ao ciclo ativo; **ENCERRADA e CANCELADA** são finais. Essa regra alimenta candidaturas recentes e a união de tags usada no contexto do contratante. Para recomendações ao artista, a regra oficial mais restrita permanece: somente ABERTA.

## 9. Dependências RF23/RF24

O SQL contém `notificacoes`, `salas_chat`, `participantes_chat` e `mensagens_chat`, mas o backend não possui implementação Java funcional para produzir, consultar ou marcar esses dados. Estrutura de tabela isolada não comprova RF23/RF24. O RF11 não criou Entity, Repository, Service, endpoint, evento, WebSocket, STOMP ou contador artificial. A conclusão integral fica bloqueada por essas duas dependências.

## 10. Segurança

- Identidade obtida exclusivamente por `AuthenticatedUserResolver` a partir do e-mail do principal JWT.
- `GET /api/dashboard` sem JWT retorna 401; JWT inválido também retorna 401.
- Não existe `/api/dashboard/{usuarioId}` e um parâmetro extra `usuarioId` não altera o usuário resolvido.
- Propriedade das vagas é filtrada no banco por `vaga.contratante.usuarioId` autenticado.
- Artistas menores não são sugeridos; perfis incompletos não são expostos no módulo de talentos.
- DTOs aplicam lista branca e testes verificam ausência de dados privados.
- Frontend cria nós e usa `textContent` para conteúdo vindo da API, evitando `innerHTML` nesses módulos.

## 11. Performance

- Padrão 5 e máximo 50; nenhuma coleção usa `findAll()` ilimitado.
- Matching, filtro, agregação e ordenação são executados no banco.
- Paginação ocorre antes do carregamento detalhado de coleções.
- IDs ranqueados são hidratados em lote por `EntityGraph`, evitando repository dentro de loops.
- Estatísticas Hibernate comprovaram limite constante para 20 vagas; e para uma resposta com 20 candidaturas e 20 talentos. O número de statements ficou dentro dos limites objetivos da suíte e não cresceu por item.

## 12. Banco

**Banco alterado: NÃO.**

Não foram alterados tabelas, colunas, tipos, enums, constraints, índices, relacionamentos, migrations ou scripts. `spring.jpa.hibernate.ddl-auto=validate` permaneceu inalterado. Os hashes finais coincidem com a baseline para `sos_artistas.sql`, `schema-test.sql`, `migration_rf03.sql`, `migration_rf25_motivo.sql`, cópia de schema de testes e `application.properties`.

Na QA local, a migração RF25 já versionada foi aplicada apenas ao contêiner Docker de execução porque `docker-compose.yml` não a monta no primeiro bootstrap; isso não modificou arquivos nem o banco oficial.

## 13. Frontend

**Frontend alterado: SIM.**

- `frontend/public/dashboard-contratante.html`: mock substituído por painel único, estados de loading/erro/vazio, aviso de perfil, módulos por papel e logout reutilizado.
- `frontend/public/css/dashboard.css`: estilos do painel real e breakpoints responsivos.
- `frontend/public/js/main.js`: ambos os logins redirecionam ao dashboard; uma chamada `/dashboard`; render seguro; links e estados por papel; removidos números falsos e N+1 frontend.
- `frontend/public/detalhe-vaga.html`: página mínima autenticada para o link de detalhe das recomendações, reutilizando RF05.

`frontend/public/login.html` não foi alterado. Não houve redesign global.

## 14. Testes baseline

Antes de editar foi executado `mvnw.cmd '-Dspring.jpa.show-sql=false' test`: **228 executados, 228 aprovados, 0 falhas, 0 erros, 0 ignorados, BUILD SUCCESS**, em 01:45.

## 15. Novos testes RF11

`DashboardRf11IntegrationTest` possui **14 testes** com PostgreSQL 18 real. Cobre 401, JWT inválido, estruturas ARTISTA/CONTRATANTE, IDOR, perfil incompleto sem bloqueio, artista sem tags, somente vagas ABERTA, ranking e desempate, paginação/size, propriedade e status das candidaturas, contexto de vagas ativas, artistas completos/adultos, desempate por atualização, ausência de dados privados, RF23/RF24 indisponíveis, contratante sem contexto e anti-N+1 com 20 vagas, 20 candidaturas e 20 talentos. Execução isolada: 14/14 e `BUILD SUCCESS`.

## 16. Testes frontend

**Teste frontend executado: SIM, com limitação ambiental registrada.**

- `npm test -- --watchAll=false`: não executou porque `npm` não está instalado/no PATH.
- `node --check frontend/public/js/main.js`: não executou porque `node` não está instalado/no PATH.
- Não foi instalado framework novo e TLS não foi desabilitado.
- Validação renderizada no navegador: URL/título, DOM, estados, console, sobreposições, login dos dois tipos, redirecionamento, chamada JWT, cards ARTISTA/CONTRATANTE, detalhe de vaga, perfil público e ausência de logs de erro/aviso.
- Responsividade verificada em desktop 1280 px, 390 px e 360 px, sem overflow horizontal. A inspeção visual identificou compressão de cartões a 360 px; o breakpoint foi corrigido e revalidado com 249 px úteis de texto.

O uso do fluxo especializado de teste frontend determinou a validação renderizada, a inspeção de logs/DOM e a correção móvel; Jest não é declarado aprovado.

## 17. Regressão final

Comando definitivo: `mvnw.cmd '-Dspring.jpa.show-sql=false' test`.

Resultado: **242 testes executados, 242 aprovados, 0 falhas, 0 erros, 0 ignorados, BUILD SUCCESS**, em 01:54. Isso preserva os 228 testes anteriores e acrescenta 14 do RF11.

## 18. Conflitos e decisões

- `PerfilContratante` não tem tags nem área estruturada. Como não era permitido criar coluna, o contexto profissional do MVP é a união das tags das próprias vagas ABERTA/PAUSADA do contratante.
- “Vaga ativa” foi fixada como ABERTA/PAUSADA; estados finais não entram.
- RF23/RF24 possuem tabelas futuras, mas não implementação funcional; por isso são indisponíveis, não zeros.
- Matching não usa score/medalha e não expande RF13/RF17.
- Vaga já candidatada não é excluída automaticamente, pois o requisito não autoriza essa regra.
- A URL legada `dashboard-contratante.html` foi preservada para compatibilidade, mas a página agora atende ambos os papéis.

## 19. Pendências

- Implementar RF23 completo para notificações reais e então integrar não lidas ao painel.
- Implementar RF24 completo para mensagens reais e então integrar não lidas ao painel.
- Restaurar uma instalação local de Node/npm para executar Jest e `node --check`; a validação atual foi browser/E2E.
- Opcionalmente montar `migration_rf25_motivo.sql` no bootstrap do Docker em tarefa própria; não foi alterado aqui por estar fora do RF11 e do escopo de banco.

## 20. Conclusão

**RF11 PARCIAL.**

O núcleo do dashboard RF11 está concluído e validado: autenticação, identidade JWT, personalização por papel, perfil incompleto, vagas recomendadas, candidaturas próprias, talentos relevantes, paginação, privacidade, performance, frontend e regressão. A conclusão integral permanece bloqueada pelas dependências RF23 e RF24, conforme o próprio critério da tarefa.

### Registro para relatório semanal futuro

- **Data:** 22/08/2026.
- **Objetivo:** auditar e finalizar o núcleo sustentável do painel principal.
- **RF trabalhado:** RF11.
- **Backend alterado:** endpoint/serviço/DTOs/projeções/consultas e testes RF11.
- **Frontend alterado:** SIM.
- **Banco alterado:** NÃO.
- **Funcionalidades avançadas/concluídas:** painéis por papel, matching em banco, preview paginado, estados e navegação.
- **Decisões técnicas:** tags do contratante derivadas de vagas ativas; ABERTA/PAUSADA ativas; sem score/medalha/random.
- **Segurança:** JWT exclusivo, propriedade no banco, proteção IDOR, whitelist e menor protegido.
- **Testes:** baseline 228/228; RF11 14/14; final 242/242; QA browser desktop/390/360.
- **Dependências:** RF23 e RF24.
- **Pendências:** notificações/mensagens reais e ambiente Node/npm.
- **Próximos passos:** implementar RF23 e RF24 em tarefas próprias e reexecutar Jest quando Node/npm estiver disponível.

# Relatório RF06 + RF23 + RF28 + RF45 — 08/10/2026

## 1. Resumo executivo

A consolidação usa o database05 existente: criação e retirada de candidaturas, até uma recandidatura, ciclo da vaga, cancelamento auditável e consulta privada de candidatos. A decisão D10 é uma projeção explícita do enum físico, sem migration ou reescrita histórica. As conclusões e validações finais aparecem nas seções 54–66.

| RF | Conclusão do backend |
|---|---|
| RF06 | Concluído no contrato funcional atual |
| RF23 | Concluído no recorte de ciclo/prazo |
| RF28 | Concluído no recorte de cancelamento auditável |
| RF45 | Concluído no recorte de candidatos |

Focados finais 213/213; Maven completo 1767/1750/0 failures/0 errors/17 skipped, BUILD SUCCESS; Semgrep Docker 0 findings. Banco/frontend/index intactos nesta tarefa. Entrega da interface e pendências de dependências permanecem separadas.

## 2. Branch e HEAD inicial/final

Branch: `integracao-recuperada-2026-09-15`. HEAD inicial/final: `278d045ff9dea10933b8e16896e1b2a1cdda64c3`. `git fetch fork` executado no checkpoint; divergência `fork/integracao-recuperada-2026-09-15...HEAD`: 0/0. Backend e index inicialmente limpos. Preexistências em README, database legado e cliente operacional foram registradas e preservadas.

## 3. Fontes utilizadas

Fonte funcional: `C:\Users\masca\OneDrive\Área de Trabalho\-\trabalhosAula\tecnico\pji\rf e rnf.txt`, SHA-256 `08FB838914D57F1C6B54F3CFDC73DB5FE1F7CD511B4DFCA5A736DC094CA20EDA`. Fonte física exclusiva: `database05/palco-database/`. ZIP oficial `C:\Users\masca\Downloads\palco-database05.zip`, SHA-256 `6158C813929AC0DB9B3B12030E8B9D84C3B647611986DD6D60B2FC50D0F97EBF`. Código/testes atuais decidiram as conclusões; Graphify orientou a navegação. Nenhuma fonte funcional foi editada.

## 4. Estado inicial RF06

Já derivava o artista do JWT, exigia perfil completo oficial, ABERTA e prazo válido; protegia o par artista/vaga com advisory lock transacional e a vaga com lock de escrita; preservava duas tentativas e a retirada lógica. Faltavam confirmação explícita e projeção funcional dos estados físicos.

## 5. Estado inicial RF23

Publicação, pausa, reabertura, prazo e encerramento automático já existiam. A ação manual ENCERRAR aceitava PAUSADA, incompatível com as transições vigentes. Reabertura já preservava data de publicação e exigia prazo futuro quando o anterior estava vencido.

## 6. Estado inicial RF28

Cancelamento Java já registrava log e notificações na transação, mas permitia PAUSADA, negava ENCERRADA e convertia candidaturas ativas em CANCELADA_POR_VAGA. A auditoria física confirmou capacidade real para ator/motivo/timestamp.

## 7. Estado inicial RF45

Já existiam autorização por proprietário, paginação de IDs seguida de detalhes, DTO profissional, perfil público e chat idempotente. A consulta restringia candidaturas atuais e ordenava por compatibilidade; faltavam os filtros completos, favoritos, contexto separado da vaga e ação de conversa restrita à candidatura legítima.

## 8. Arquivos auditados

Camadas principais: CandidaturaController/VagaController; CandidaturaService/VagaService; CandidaturaRepository/VagaRepository; VagaPrazoPolicy/VagaPrazoService; DTOs e enums de candidatura/vaga; modelos Candidatura, PerfilArtista, PerfilArtistaArea e LogVagaCancelada; ItemSalvoRepository e leitura RF19; TaxonomiaProfissional e repositories RF54; ChatService/RF35; MenorAutorizadoPolicy; listeners de notificações e de aviso ao responsável/RF44; produtores RF42 e testes de regressão. SQL lido: `01_types/01_enums.sql`, `02_tables/04_vagas.sql`, tabelas de perfis/taxonomia/favoritos/chat/notificações e procedures enviar/retirar/cancelar. Matriz anterior à edição: [auditoria-inicial.md](evidencias/candidaturas-ciclo-2026-10-08/auditoria-inicial.md).

## 9. Arquivos alterados

26 arquivos Java: 13 de produção (3 novos) e 13 de testes. O backend estava limpo no checkpoint; estes são deltas desta tarefa. Scripts/JSON/logs/ruleset estão exclusivamente em `evidencias/candidaturas-ciclo-2026-10-08/`; relatório novo na raiz. Graphify atualizou seus artefatos gerados.

| Camada | Arquivo | Delta |
|---|---|---|
| Produção | [backend/src/main/java/com/portifolio/controller/VagaController.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/controller/VagaController.java) | Alterado |
| Produção | [backend/src/main/java/com/portifolio/dto/CandidatosVagaFiltro.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/CandidatosVagaFiltro.java) | Novo |
| Produção | [backend/src/main/java/com/portifolio/dto/CandidaturaCriacaoRequest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/CandidaturaCriacaoRequest.java) | Alterado |
| Produção | [backend/src/main/java/com/portifolio/dto/CandidaturaResponse.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/CandidaturaResponse.java) | Alterado |
| Produção | [backend/src/main/java/com/portifolio/dto/CandidaturaVagaResponse.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/CandidaturaVagaResponse.java) | Alterado |
| Produção | [backend/src/main/java/com/portifolio/dto/VagaCancelamentoRequest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/VagaCancelamentoRequest.java) | Alterado |
| Produção | [backend/src/main/java/com/portifolio/dto/VagaResponse.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/dto/VagaResponse.java) | Alterado |
| Produção | [backend/src/main/java/com/portifolio/model/PerfilArtistaArea.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/model/PerfilArtistaArea.java) | Alterado |
| Produção | [backend/src/main/java/com/portifolio/repository/CandidatosConsultaRepository.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/repository/CandidatosConsultaRepository.java) | Novo |
| Produção | [backend/src/main/java/com/portifolio/repository/CandidaturaRepository.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/repository/CandidaturaRepository.java) | Alterado |
| Produção | [backend/src/main/java/com/portifolio/service/CandidaturaService.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/service/CandidaturaService.java) | Alterado |
| Produção | [backend/src/main/java/com/portifolio/service/VagaService.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/service/VagaService.java) | Alterado |
| Produção | [backend/src/main/java/com/portifolio/validation/EstadoCandidaturaFuncional.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/main/java/com/portifolio/validation/EstadoCandidaturaFuncional.java) | Novo |
| Teste | [backend/src/test/java/com/portifolio/controller/AvisoResponsavelRf44IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/AvisoResponsavelRf44IntegrationTest.java) | Alterado |
| Teste | [backend/src/test/java/com/portifolio/controller/CandidatosVagaRf45IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/CandidatosVagaRf45IntegrationTest.java) | Alterado |
| Teste | [backend/src/test/java/com/portifolio/controller/CandidaturaControllerRf06IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/CandidaturaControllerRf06IntegrationTest.java) | Alterado |
| Teste | [backend/src/test/java/com/portifolio/controller/ConviteVagaRf42IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/ConviteVagaRf42IntegrationTest.java) | Alterado |
| Teste | [backend/src/test/java/com/portifolio/controller/DashboardRf11IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/DashboardRf11IntegrationTest.java) | Alterado |
| Teste | [backend/src/test/java/com/portifolio/controller/NotificacaoRf23IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/NotificacaoRf23IntegrationTest.java) | Alterado |
| Teste | [backend/src/test/java/com/portifolio/controller/PerfilEdicaoRf08IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/PerfilEdicaoRf08IntegrationTest.java) | Alterado |
| Teste | [backend/src/test/java/com/portifolio/controller/VagaCancelamentoRf25IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/VagaCancelamentoRf25IntegrationTest.java) | Alterado |
| Teste | [backend/src/test/java/com/portifolio/controller/VagaControllerRf03IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/VagaControllerRf03IntegrationTest.java) | Alterado |
| Teste | [backend/src/test/java/com/portifolio/controller/VagaDetalhesRf05IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/VagaDetalhesRf05IntegrationTest.java) | Alterado |
| Teste | [backend/src/test/java/com/portifolio/controller/VagaGerenciamentoRf31IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/controller/VagaGerenciamentoRf31IntegrationTest.java) | Alterado |
| Teste | [backend/src/test/java/com/portifolio/realtime/NotificacaoWebSocketRf23IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/realtime/NotificacaoWebSocketRf23IntegrationTest.java) | Alterado |
| Teste | [backend/src/test/java/com/portifolio/service/VagaPrazoRf23Rf06IntegrationTest.java](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/backend/src/test/java/com/portifolio/service/VagaPrazoRf23Rf06IntegrationTest.java) | Alterado |

## 10. Modelo físico de candidatura

`candidaturas`: id PK; vaga_id FK para vagas; artista_id FK para perfis_artistas; mensagem_apresentacao TEXT opcional; link_portfolio_candidatura VARCHAR(255) opcional; status enum com default PENDENTE; data_candidatura TIMESTAMP. Não existe UNIQUE global (vaga, artista), o que permite histórico de tentativas. Índices existentes por vaga e artista foram preservados.

## 11. Conflito ATIVA/RETIRADA × enum físico

Enum físico: PENDENTE, EM_ANALISE, ACEITA, REJEITADA, BLOQUEADA, RETIRADA, CANCELADA_POR_VAGA. ATIVA não existe fisicamente. Igualar todos esses valores a ATIVA/RETIRADA destruiria a semântica histórica; a adaptação distingue registros legados.

## 12. Decisão/projeção D10

`EstadoCandidaturaFuncional` centraliza a adaptação:

| Físico | Funcional | Legado |
|---|---|---|
| PENDENTE | ATIVA | Não |
| EM_ANALISE | ATIVA | Sim; compatibilidade com o conjunto operacional ativo preexistente, sem criar etapa de seleção |
| RETIRADA | RETIRADA | Não |
| ACEITA/REJEITADA/BLOQUEADA/CANCELADA_POR_VAGA ou NULL histórico | NULL | Sim; nenhuma equivalência binária inventada |

Novas candidaturas gravam PENDENTE e retornam ATIVA. RF06 autorizado mantém código físico no campo statusLegado quando necessário; RF45 não retorna esse código e apenas sinaliza registroLegado. RF05 separa statusMinhaCandidatura funcional do status da vaga. Nenhuma aprovação/rejeição/análise foi habilitada; enum e linhas legadas permaneceram intactos.

## 13. Primeira candidatura

POST `/api/candidaturas`: identidade do artista vem do JWT; confirmacao=true é obrigatória; perfil_completo deve ser verdadeiro; vaga deve estar ABERTA e dentro do prazo. Mensagem e link de portfólio continuam opcionais. A persistência e a notificação ao proprietário participam da transação. Conta/consentimento aplicam as políticas atuais.

## 14. Retirada

DELETE `/api/candidaturas/{id}` continua sendo retirada lógica. PUT legado permite somente RETIRADA pelo artista proprietário, com vínculos imutáveis. ABERTA/PAUSADA com prazo vigente permitem retirada; ENCERRADA/CANCELADA/RASCUNHO ou prazo vencido bloqueiam. Histórico e data original são preservados; retirada gera notificação transacional ao contratante.

## 15. Recandidatura

Somente depois da retirada da tentativa anterior, na mesma vaga ABERTA e dentro do prazo. Cria outra linha com novo ID/timestamp e PENDENTE físico; não reativa nem sobrescreve a primeira. Convite RF42 não participa dessa contagem e não cria candidatura.

## 16. Limite de tentativas

Máximo de duas tentativas por artista/vaga: candidatura inicial + uma recandidatura. Qualquer ativa PENDENTE/EM_ANALISE impede duplicação; duas linhas impedem uma terceira, mesmo após a segunda retirada ou reabertura. Contagem considera o histórico completo, incluindo estados legados; leitura RF45 não altera esse limite.

## 17. Concorrência

Mantidos `pg_advisory_xact_lock` por par e PESSIMISTIC_WRITE da vaga. RF23/RF28 usam o mesmo lock da vaga; retirada refresca vaga/candidatura após obter o lock. Testes cobrem duplicidade inicial, recandidatura concorrente, candidatar×retirar, candidatar×pausar/encerrar/cancelar/reabrir, retirar×encerrar e transição que obtém o lock antes da candidatura. Se candidatura confirma antes da transição, seu histórico permanece; se o fechamento confirma primeiro, a nova candidatura é recusada. A garantia vale para escritores que usam o fluxo Java; SQL externo/procedures legadas não receberam proteção adicional.

## 18. Perfil completo

Reutilizado `usuarios.perfil_completo`, calculado pelo mecanismo oficial/fluxo RF08. Não foram inventados campos obrigatórios nem exigidos foto, mensagem ou portfólio. Testes de RF08/RF54 exercitam os dados oficiais e a integração com elegibilidade.

## 19. Menor/RF44

Menor autorizado segue MenorAutorizadoPolicy, com consentimento persistido e vigente. AvisoResponsavelCandidaturaEvento é registrado durante a candidatura, mas o listener entrega somente AFTER_COMMIT. A infraestrutura revalida destinatário/consentimento; falha de entrega não desfaz candidatura confirmada. Aviso não é autorização extra. Retentativas duráveis/outbox permanecem limitações da infraestrutura RF44 e não foram implementadas neste recorte.

## 20. Máquina de estados RF23

| Origem | Ação | Destino |
|---|---|---|
| RASCUNHO | PUBLICAR | ABERTA |
| ABERTA | PAUSAR/SUSPENDER | PAUSADA |
| PAUSADA | REABRIR | ABERTA |
| ABERTA | ENCERRAR | ENCERRADA |
| ENCERRADA | REABRIR | ABERTA |
| ABERTA/ENCERRADA | cancelamento RF28 confirmado | CANCELADA |

CANCELADA é terminal. Ações inválidas retornam 422; ação desconhecida retorna 400. Publicação/edição RF04/RF07 preservam as validações existentes. O prazo automático é distinguido da ação manual.

## 21. Publicar

PUBLICAR somente de RASCUNHO, pelo dono autenticado. Validação de publicação e taxonomia já consolidada é reutilizada; publicação inicial define dataPublicacao. Não foi ampliado o domínio RF04 nem contornado seu catálogo/decisões pendentes.

## 22. Pausar

PAUSAR e alias SUSPENDER: somente ABERTA→PAUSADA. Bloqueia nova candidatura, preserva tentativas e permite retirada quando o prazo ainda estiver vigente. Não existe criação de resultado de seleção.

## 23. Reabrir PAUSADA

PAUSADA→ABERTA pelo proprietário. Se prazo antigo venceu, deve informar data futura; vaga sem prazo não recebe uma data artificial. Não reativa RETIRADA, não duplica ATIVA nem reinicia as duas tentativas.

## 24. Encerrar

ENCERRAR manual: apenas ABERTA→ENCERRADA. PAUSADA→ENCERRADA manual foi removida e é testada como 422. Antes de encerrar manualmente uma PAUSADA, o dono precisa reabri-la. Nenhuma candidatura é apagada ou convertida.

## 25. Reabrir ENCERRADA

ENCERRADA→ABERTA com as validações de prazo/publicação atuais; mantém publicação original e histórico das candidaturas. CANCELADA não pode ser reaberta. Uma retirada continua retirada.

## 26. Prazo automático

VagaPrazoPolicy e scheduler existentes foram preservados: encerram vencidas ABERTA/PAUSADA em lotes, com UPDATE condicional e idempotência; sem prazo/futura/finais permanecem. Prazo é exclusivo: no dia limite a candidatura já é recusada, conforme contrato e testes atuais. Esse encerramento automático de PAUSADA vencida não habilita a ação manual PAUSADA→ENCERRADA. Testes usam Clock fixo e concorrência real PostgreSQL.

## 27. Cancelamento RF28

DELETE de vaga publicada: owner, estado ABERTA/ENCERRADA, confirmação verdadeira e motivo válido. Persiste CANCELADA, log e notificações na mesma transação; erro obrigatório faz rollback completo. Vaga publicada não é apagada. Exclusão física preexistente de rascunho permanece outro fluxo, bloqueada se houver candidatura; não é tratada como cancelamento RF28.

## 28. PAUSADA → CANCELADA

Recusada com 422 conforme baseline. Teste antigo positivo foi substituído por cancelamento de ENCERRADA e a negativa PAUSADA foi preservada explicitamente. Não se manteve o legado para fazer testes passarem.

## 29. Motivo/confirmação

RF06: confirmacao=true, validada no DTO e no serviço. RF28: confirmacao=true e motivo não vazio; motivo limitado a 2000 caracteres e normalizado com trim. Corpo ausente, falso/null/vazio ou inválido não cancela. O limite é validação de backend sobre TEXT existente, sem alteração física.

## 30. Registro ator/motivo/timestamp

Representável e utilizado: `log_vagas_canceladas(vaga_id, cancelado_por_id, motivo, data_cancelamento)`, com FKs reais para vaga e usuário. LogVagaCancelada/repository existentes foram reutilizados. Ator vem do JWT; timestamp vem do servidor. Testes verificam persistência e rollback por falha de log/notificação; nenhuma simulação em log textual substitui essa auditoria.

## 31. Tratamento das candidaturas no cancelamento

Cancelamento não modifica status, ID, vínculo, mensagem, link ou data de nenhuma candidatura. Uma ATIVA pode permanecer ligada a vaga CANCELADA, com statusVaga separado. Teste parametriza/preserva todos os estados físicos; feed público omite vaga cancelada e o contexto legítimo continua disponível ao dono/candidato.

## 32. CANCELADA_POR_VAGA legado

Continua no enum e nos registros existentes, sem novas gravações impostas por cancelamento Java. Não vira requisito funcional ou sinônimo de REJEITADA. Procedure sp_cancelar_vaga permanece incompatível: aceita PAUSADA/RASCUNHO, recusa ENCERRADA e converte candidaturas. Não é chamada pelo fluxo Java consolidado. sp_retirar_candidatura também é menos estrita com estados históricos; sp_enviar_candidatura não substitui os locks/regras Java. Nenhum SQL foi editado.

## 33. Histórico

Todos os IDs/timestamps/tentativas existentes permanecem. RF06 conserva leitura autorizada/paginada de todas as tentativas; RF45 usa apenas a última por artista/vaga para a visão operacional. Histórico não desaparece com pausa, encerramento, cancelamento, reabertura ou consulta. RF22/retention ficam em seu escopo próprio.

## 34. Notificações

Produtores existentes de candidatura, retirada, mudança de estado e cancelamento foram reutilizados. Persistência transacional; entrega WS/SSE AFTER_COMMIT. Falha de persistência obrigatória faz rollback; falha de transporte não apaga o registro. RF44 usa o listener próprio após commit. Somente filtrar favoritos não notifica artista. A cobertura adicional para usuários que apenas favoritaram a vaga permanece pendência de produtores/categorias RF36: este recorte não implementa integralmente RF36 nem reutiliza categoria semanticamente errada.

## 35. RF45 autorização

GET `/api/vagas/{id}/candidaturas`: JWT obrigatório; somente CONTRATANTE proprietário. Visitante 401; ARTISTA/outro contratante 403; vaga inexistente 404. Owner de consulta/favoritos é resolvido no servidor. usuarioId/ownerId/contratanteId enviados como parâmetros são recusados, não usados como autoridade.

## 36. Filtros RF45

`busca`, `status`, `areaId`, `funcaoId`, `especializacaoId`, `dataInicio`, `dataFim`, `somenteFavoritas`, `page`, `size`. Nome público com correspondência literal case-insensitive; %, _ e barra invertida escapados; Criteria/binding parametrizado. Não pesquisa email, CPF, telefone, responsável, biografia, @username ou dados sensíveis. Parâmetros desconhecidos/repetidos, status de seleção, IDs não positivos, datas inválidas e busca >150 são recusados.

## 37. Ativas/Retiradas/Todas

TODAS é o default. Seleciona primeiro o maior ID por artista/vaga, antes dos filtros e da contagem. ATIVAS: última tentativa PENDENTE/EM_ANALISE projetada ATIVA, com statusVaga separado. RETIRADAS: última tentativa RETIRADA. TODAS inclui a última de cada artista e sinaliza legado sem código físico formal. Tentativa 1 RETIRADA + tentativa 2 ATIVA aparece uma vez em ATIVAS/TODAS e não aparece em RETIRADAS. Histórico completo segue no RF06 autorizado; filtros não ressuscitam tentativa antiga.

## 38. Filtros taxonômicos

Reutilizam RF08/RF54 e TaxonomiaProfissional: Área existe, Função pertence à Área, Especialização compatível com Função/Área. Qualquer Área válida do artista, inclusive secundária, pode corresponder. EXISTS exige a mesma cadeia profissional, sem duplicar linhas. Área sem Função pode corresponder ao filtro somente de Área. IDs inexistentes 404; cadeia incompatível 422. Não se filtram afirmativas ou experiência privada.

## 39. Período

Campo físico real: candidaturas.data_candidatura da última tentativa. dataInicio inclusiva às 00:00; dataFim inclusiva, implementada como `< início do dia seguinte`. Início maior que fim, formato inválido ou fim no extremo não representável são recusados. Não usa publicação/reabertura/retirada da vaga. Ordenação: dataCandidatura DESC, id DESC.

## 40. Favoritos

EXISTS em itens_salvos/PERFIL_ARTISTA do contratante autenticado proprietário. Flag favorito é carregada por uma leitura em lote para os artistas da página. Favoritos de outros usuários não correspondem nem expõem donos/quantidades. Combina com status, nome, taxonomia, período e count; nenhuma notificação é produzida pela leitura.

## 41. Privacidade do menor

Mesmo predicado persistente MenorAutorizadoPolicy.publicavel de RF10/RF37, aplicado antes de count/página. Menor autorizado pode aparecer; consentimento ausente/pendente/revogado, conta inativa ou idade abaixo do mínimo não conferem visibilidade. DTO omite DOB/idade exata, responsável/contatos, CPF, email/telefone privados, afirmativas e experiência. Perfil e chat revalidam consentimento; o vínculo de candidatura não amplia dados públicos.

## 42. DTO

CandidaturaVagaResponse retorna identificadores operacionais/públicos, nome/username/avatar, localização pública, IDs profissionais, data, status funcional, registroLegado, statusVaga, favorito, perfilUrl e conversaUrl. Campos profissionais de compatibilidade já existentes foram preservados; quantidade de funções coincidentes é contagem de IDs públicos, não score/ranking de seleção e não dirige a ordenação. Nenhuma Entity ou dado sensível é serializado. RF06 histórico possui statusLegado em contexto autorizado, separado do DTO RF45.

## 43. Ver perfil

perfilUrl aponta para `/api/perfis/publicos/ARTISTA/{artistaId}`, rota RF10 real. RF45 não copia o perfil completo nem cria autorização adicional. A rota pública mantém seu próprio controle de privacidade.

## 44. Conversar/RF35

POST `/api/vagas/{vagaId}/candidaturas/{candidaturaId}/conversa`: exige CONTRATANTE dono, vaga existente e candidatura pertencente àquela vaga; deriva artista da candidatura. Reutiliza ChatService.criarOuReutilizarSala, idempotência, membros autorizados e política atual de menores. Não aceita artista arbitrário como destino. Sala retornada pode ser reutilizada pelo endpoint RF35 existente. Nenhuma tabela/chat paralelo foi criado.

## 45. Paginação

Default page=0/size=20; tamanho máximo normalizado para 50, preservando padrão existente. page negativo, size<=0, erro de binding e offset fora do intervalo são recusados. Pagina IDs no banco; count usa o mesmo predicado. Detalhes/favoritos só para os IDs daquela página; ordenação estável data DESC/id DESC. Não existe retorno ilimitado da lista RF45.

## 46. N+1

IDs/count separados de EntityGraph de detalhes; favoritos em lote/EXISTS; especializações por @BatchSize(50). Não pagina join de coleções. Testes com 1×20, 2×20 e 10×50 candidatos medem consultas e contagem/páginas sem duplicação, incluindo duas áreas, funções, especializações e favoritos. Na reexecução focada e na suíte completa: 10 candidatos = 12 consultas; 50 candidatos = 13 consultas; universo 55, segunda página com 5, sem IDs duplicados. O aumento medido é de uma consulta em lote, não 40 consultas por candidato.. É prova de quantidade limitada de consultas no cenário exercitado, não benchmark de carga em produção.

## 47. Segurança

Identidades/ownership são do JWT; artista terceiro não retira (404 sem revelar registro), outro contratante não lista (403), ARTISTA não acessa lista completa (403). Fluxo Java impede duas ativas/terceira tentativa; duplicidades legadas não são apagadas e RF45 não as duplica. Convite não é candidatura. PAUSADA não cria candidatura e permite retirada vigente; ENCERRADA/CANCELADA bloqueiam retirada/criação. CANCELADA não reabre; cancelamento não cria seleção formal. Motivo/confirmação, privacidade, favoritos do owner e binding foram exercitados. Nenhuma regra depende exclusivamente do frontend. Ver limites de escritores SQL externos e contexto legado nas seções 17/61.

## 48. Database alterado SIM/NÃO

NÃO por esta tarefa. database05 íntegro e preservado; enum, schema, constraints, índices, procedures/triggers/functions e SQL oficiais intocados. O delta preexistente em `database/02_tables/04_vagas.sql` foi preservado byte a byte. Somente fixtures normais foram inseridas nos PostgreSQL descartáveis dos testes.

## 49. Frontend alterado SIM/NÃO

NÃO por esta tarefa. Tanto frontend quanto palco-comunidades-agenda e seus deltas preexistentes foram preservados; nenhum build/instalação/edição frontend foi feito. Contratos novos exigem integração posterior da interface.

## 50. Migration/SQL SIM/NÃO

NÃO. Nenhuma migration, arquivo SQL, seed ou alteração de enum/tabela/constraint/índice foi criada. Manifesto inicial: 1100 arquivos; 1077 fora do escopo Java autorizado comparados por SHA-256 sem alteração inesperada. Manifesto dos 26 Java testados/escaneados permaneceu igual após as ferramentas. Index SHA-256 igual ao inicial `54A1A0D661CCA46D6FDD9E17EFFF9DC7220F50F3E5FF2AE1E25ECF31922ECF76`; cached diff vazio. git diff --check: exit 0, saída vazia. Evidência: [preservacao-final.json](evidencias/candidaturas-ciclo-2026-10-08/preservacao-final.json).

## 51. Testes adicionados/alterados

53 casos novos: RF06 +15 (confirmação, reabertura/limite e concorrência); RF45 +38 (status, busca literal, filtros inválidos, cadeia taxonômica, períodos, favoritos, consentimento, conversa e consultas em lote). Classes existentes foram estendidas. As demais alterações são fixtures/expectativas de regressão relacionadas ao contrato; nenhuma classe foi removida ou desabilitada.

## 52. Testes antigos ajustados

PENDENTE/EM_ANALISE na API tornam-se ATIVA com metadado legado onde aplicável; leitura histórica preserva enum físico. Cancelamento não espera conversão para CANCELADA_POR_VAGA; ENCERRADA substitui o antigo sucesso PAUSADA e PAUSADA tem negativa explícita. Encerramento manual de PAUSADA é negado. RF05/RF45 usam data da tentativa/id descendentes, com coincidências públicas sem ranking. Fixtures HTTP passam confirmacao=true; fixture de menor público precisa consentimento real persistido. Timestamp de concorrência é comparado com o valor armazenado pelo PostgreSQL, não nanos em memória. Limites de consultas foram adaptados às leituras adicionais/lotes, preservando comparação entre tamanhos de página.

## 53. Comandos

Checkpoint: git status --short; git branch --show-current; git rev-parse HEAD; git log --oneline -15; git fetch fork; git rev-list --left-right --count fork/integracao-recuperada-2026-09-15...HEAD. Testes JDK 21.0.12: [Executar-Maven.ps1](evidencias/candidaturas-ciclo-2026-10-08/Executar-Maven.ps1) executa `.\mvnw.cmd test -Dtest=<classes reais>` e depois `.\mvnw.cmd test`. Usa build diretório database05 e classpath estável test-runtime já existentes, sem alterar pom. Semgrep: script nesta evidência, imagem oficial/p-java local, mounts read-only/network none. Graphify: `graphify update .`, seguido de MCP. Preservação: git diff --name-status nos escopos exigidos, cached e diff --check. Nenhum git add/commit/push.

## 54. Testes focados

As execuções diagnósticas foram preservadas, sem serem apresentadas como aprovação:

| Execução | Total | Passed | Failures | Errors | Skipped | Duração | Resultado |
|---|---:|---:|---:|---:|---:|---:|---|
| Diagnóstico inicial | 163 | 156 | 7 | 0 | 0 | 117,4 s | BUILD FAILURE; fixtures/expectativas legadas |
| Regressão ampla, 23 classes | 787 | 773 | 8 | 6 | 0 | 421,1 s | BUILD FAILURE; asserções/fixtures revistas |
| Reexecução corretiva final, 5 classes | 213 | 213 | 0 | 0 | 0 | 131,1 s | BUILD SUCCESS |

Reexecução final: CandidatosVagaRf45IntegrationTest (78), CandidaturaControllerRf06IntegrationTest (72), NotificacaoRf23IntegrationTest (11), VagaDetalhesRf05IntegrationTest (28), NotificacaoWebSocketRf23IntegrationTest (24). Na ampla, o produtor HTTP WS/SSE ainda omitia confirmacao; a falha interrompia a limpeza dos streams e os casos seguintes acusavam timeouts. Apenas a fixture recebeu confirmacao=true; assertions de privacidade/isolamento e transportes foram mantidas. A reexecução e o Maven completo posterior passaram. As 23 classes/dependências da ampla também foram executadas novamente e aprovadas dentro da suíte completa. Logs/exit/resumos de cada execução nesta evidência.

## 55. mvn test

Executado `.\mvnw.cmd test` após aprovação focada, sem seleção de classes e sem desabilitar testes. BUILD SUCCESS, exit 0; 77 suites; início 2026-10-08T19:11:47.0587359-03:00; fim 2026-10-08T19:23:34.4604274-03:00; duração do wrapper 707.4 s (Maven: 11:44 min). PostgreSQL 18/Testcontainers inicializado por init.sql/seed.sql oficiais database05; ddl-auto=validate e classpath test-runtime existentes preservados. [Log completo](evidencias/candidaturas-ciclo-2026-10-08/maven-completo.log) e [resumo XML atual](evidencias/candidaturas-ciclo-2026-10-08/maven-completo-resumo.json). Não é resultado reaproveitado da tarefa anterior.

## 56. Total/passed/failures/errors/skipped

Resultado final completo: **1767 total / 1750 passed / 0 failures / 0 errors / 17 skipped / BUILD SUCCESS**. Focados finais: **213/213**, 0 failures/errors/skipped. Os 17 skips são condicionais preexistentes: CurrentLocalSchemaIntegrationTest (4), depende de palco.current-db-tests=true; OfficialLocalApiIntegrationTest (13), depende de palco.official-db-tests=true. Essas propriedades opcionais não foram habilitadas; não se afirma validação nova de banco/API implantados. Somatório usa somente XMLs produzidos depois do início de cada execução.

## 57. Semgrep

Imagem Docker oficial imutável `semgrep/semgrep@sha256:32e459968daabe7ab86968184a29109b9564aa00392401156f9788452b42786b`; versão 1.178.0. Ruleset oficial p/java já disponível localmente, hash `5e652fa9ac09c9fac36bbadc3562a0c8eed1a47438a9d1f545e021ae3c5648b1` revalidado; 60 regras, 379 arquivos Java main/test, ~100,0% linhas parseadas, 0 findings, 0 blocking, 0 errors, exit 0; duração 46,2 s. Os 26 Java alterados, inclusive os 3 novos não rastreados, estão na lista de scanned; missingJavaFiles vazio. Mounts somente leitura, --network none, --no-git-ignore; ignore vazio só no container para não omitir src/test. Nenhuma alteração do ignore do projeto, instalação nativa Windows ou mudança de Defender/App Control; TLS não foi desabilitado. [Resumo](evidencias/candidaturas-ciclo-2026-10-08/semgrep-java-resumo.json), JSON bruto e log preservados.

**0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos.** Não foi alegado scan novo de secrets, pois este recorte exigiu p/java.

## 58. Graphify

MCP inicial: 6737 nós, 23334 arestas, 358 comunidades, 91% extraído/9% inferido. Após o delta, `graphify update .` AST incremental concluiu, exit 0, 36,1 s; reextraiu 312 arquivos não cacheados, sem LLM. MCP posterior confirmou **6789 nós / 23657 arestas / 360 comunidades**, mesma proporção 91%/9%, e localizou CandidatosConsultaRepository, EstadoCandidaturaFuncional, conversarComCandidato e testes RF45 novos.

Warnings preservados: 75 arquivos sem classificação/suporte foram ignorados; 45 SQL não extraídos por ausência de tree_sitter_sql; 358 rótulos anteriores para 360 comunidades, 103 nomes reconstruídos por hub. Nenhum parser foi instalado; graphify label/extração semântica LLM não foi executado. Consulta MCP limitada a 1400 tokens retornou recorte com aviso de truncamento, não um inventário completo. Código/SQL lidos diretamente sustentam as conclusões; grafo não substitui auditoria estrutural. Nenhum contorno de segurança Windows foi aplicado. Evidências: [update](evidencias/candidaturas-ciclo-2026-10-08/graphify-update.log) e [MCP final](evidencias/candidaturas-ciclo-2026-10-08/graphify-mcp-final.json).

## 59. Conflitos

D10: ausência física de ATIVA resolvida por projeção explícita, sem converter ACEITA/REJEITADA/CANCELADA_POR_VAGA em estado binário falso. RF28 PAUSADA→CANCELADA legado removido; ABERTA/ENCERRADA alinhadas. RF23 PAUSADA→ENCERRADA manual removido, distinto do encerramento automático por prazo. Auditoria do cancelamento existe realmente. Procedures SQL incompatíveis ficam registradas e intocadas; não são acionadas pelo fluxo novo. Ordenação RF45 por score anterior substituída por data/id e TODAS tem regra explícita de última tentativa.

## 60. Blockers

Nenhum blocker estrutural confirmado impede este recorte RF06/RF23/RF28/RF45 com database05. D10 não exige enum novo para o contrato operacional seguro adotado. C15/D08/D09/D14 do domínio de vagas, catálogo oficial faltante, demais produtores RF36 e outras capacidades da auditoria estrutural permanecem fora deste escopo; sua conclusão anterior não foi reescrita.

## 61. Riscos

Clientes que liam status físico precisam usar ATIVA/RETIRADA e registroLegado/statusVaga; frontend não foi adaptado. RF45 default TODAS e ordem por tentativa são mudanças explícitas de contrato. Dados legados ambíguos continuam sinalizados, sem limpeza automática. Escritores externos que ignoram locks/procedures atualizadas podem violar invariantes; não há constraint nova. Locks da vaga serializam operações concorrentes por vaga, sem prova de throughput em produção. Auditoria de cancelamento tem FKs/cascades oficiais; retention/RF22 segue decisão própria. RF44 não ganhou entrega durável/outbox. Campos físicos de dashboard fora do recorte não foram reescritos.

## 62. Pendências

Integração frontend posterior para confirmação, novos filtros/links e projeção funcional. Manter governança sobre procedures legadas e writers diretos. Definir/completar produtores de notificações para favoritos e cobertura RF36 fora do recorte, sem categoria incorreta. Evolução durável de RF44, retention/RF22 e demais decisões estruturais existentes ficam para tarefas específicas. Testes locais opcionais não habilitados não constituem prova de ambiente implantado.

## 63. Conclusão RF06

**CONCLUÍDO NO CONTRATO FUNCIONAL ATUAL DO BACKEND.** Identidade por JWT, confirmação, perfil completo, menores autorizados, criação somente ABERTA vigente, retirada lógica ABERTA/PAUSADA vigente, limite de duas tentativas/uma recandidatura, concorrência e histórico validados. D10 projeta ATIVA/RETIRADA com metadados de legado, sem resultado de seleção inventado e sem banco novo.

## 64. Conclusão RF23

**CONCLUÍDO NO RECORTE DE CICLO/PRAZO DO BACKEND.** Transições manuais vigentes, CANCELADA terminal, reabertura/prazo e publicação original preservados; pausa bloqueia nova candidatura e não apaga histórico. Encerramento automático de vencidas é idempotente e distinto da ação manual PAUSADA→ENCERRADA, que retorna 422. Regressões de vagas e candidaturas aprovadas.

## 65. Conclusão RF28

**CONCLUÍDO NO RECORTE DE CANCELAMENTO DO BACKEND.** ABERTA/ENCERRADA→CANCELADA; PAUSADA bloqueada; confirmação/motivo; auditoria física real com ator/motivo/timestamp; transação/rollback e preservação integral de candidaturas. Não existe blocker estrutural de auditoria. Cobertura adicional de produtores RF36 para favoritos permanece explicitamente fora deste recorte.

## 66. Conclusão RF45

**CONCLUÍDO NO RECORTE DE CANDIDATOS DO BACKEND.** Owner/JWT, nome público, TODAS/ATIVAS/RETIRADAS, cadeia Área/Função/Especialização (inclusive secundária), período real, favoritos privados/flag em lote, paginação 20/50, privacidade de menores, perfil público e conversa por candidatura legítima reutilizando RF35. Histórico não perdido/duplicado e consultas limitadas comprovadas. Frontend não implementado nesta tarefa.

## 67. Próximo passo

Revisar o delta e o relatório para integração posterior do frontend dentro de tarefa própria; seguir a fila funcional sem novo banco quando já representável. Este recorte não requer pacote estrutural adicional. Nenhum staging/commit/push foi feito ou será feito nesta execução.

## 68. Registro semanal — 08/10/2026

Objetivo: consolidar RF06/RF23/RF28/RF45 sem alterar database05/frontend. Dependências consultadas: RF03/RF04/RF05/RF07/RF08/RF10/RF19/RF27/RF35/RF36/RF42/RF44/RF48/RF54. Resultado: os quatro recortes de backend acima foram concluídos com 53 novos casos, focados finais 213/213, Maven completo 1767 total/1750 passed/0 failures/0 errors/17 skipped, Semgrep Docker 379 arquivos/60 regras/0 findings e atualização Graphify AST/MCP confirmada. database05, frontend e index preservados; nenhuma migration/SQL, staging, commit ou push.. Foram preservados enum/histórico e preexistências; evidências desta tarefa em `evidencias/candidaturas-ciclo-2026-10-08/`. Testes, Semgrep, Graphify, conflitos, riscos e pendências estão documentados acima, sem reutilizar validação antiga.

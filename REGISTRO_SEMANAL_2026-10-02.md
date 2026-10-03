# Registro semanal — 02/10/2026

**Objetivo:** migrar a fonte ativa do database02 para database04 integral, sincronizando somente compatibilidade backend, infraestrutura de teste e startup.

**Banco:** pacote da Manu completo, 46 arquivos idênticos ao ZIP; SHA-256 `52b1c4af06d79a7efae47e6fa320b4a0a32359efaae1d40e2a73d06129c6df2b`. Nenhum SQL oficial alterado. Fingerprint `db8f05cabadfd3935b7c703b7b21252f170fa529c0d30e7e61755b46d7fd5f39`. Criado `palco_dev_manu04` vazio no PostgreSQL 18; init/seed oficiais passaram com `ON_ERROR_STOP=1`, exit 0. Os bancos e snapshots anteriores foram preservados.

**Backend:** somente adaptações de compatibilidade: categoria afirmativa escalar, enums oficiais, plataforma obrigatória de embed, tradução do enum de denúncia e mapeamento do Banco de Talentos por contratante. Username obrigatório/único foi preservado; nenhum username provisório no cadastro, nenhuma RF nova. Spring concluiu `ddl-auto=validate` e iniciou em `localhost:8080` com o código final. Smoke: capacidades, 7 áreas, 4 categorias e listagem pública de vagas retornaram 200; recursos protegidos sem sessão e login inválido retornaram 401.

**Frontend:** não alterado nesta tarefa, confirmado por comparação de hashes com o baseline. Alterações locais anteriores foram preservadas.

**Testes:** Testcontainers passou a carregar somente database04 e o seed embutido no init, sem complemento estrutural. As sete classes focadas solicitadas passaram, 127/127 no resultado final. Reparo de denúncias/portfólio/talentos/mapeamento: 87/87; fixture Google incompleta: 1/1. Os testes legados de rollback que criavam constraints temporárias foram adaptados para injeção de falha por repository; DDL complementar ativo final: 0.

| Maven completo final | Quantidade |
|---|---:|
| Total | 791 |
| Passed | 739 |
| Failures | 34 |
| Errors | 1 |
| Skipped condicionais | 17 |

**Resultado:** `BUILD FAILURE`, exit 1. Dos conflitos, 33 failures e 1 error decorrem de cadastro/primeira conta Google sem username e seus fluxos dependentes. Uma failure comprova a ausência de UNIQUE vaga/artista quando a pré-consulta é forçada a não identificar uma candidatura existente. Nenhum desses testes foi apagado ou convertido em skip para obter verde.

**Decisão:** não misturar snapshots. Database04 substitui database02 integralmente como fonte ativa; histórico permanece fora da inicialização. SQL oficial não recebe patches locais, inclusive nas rotinas que apresentaram falha de execução.

**Limitação conhecida:** modelo afirmativo do database04 suporta uma categoria por vaga e não representa 50+. Combinações incompatíveis são rejeitadas com 422, sem descarte silencioso. RF04 revisado não foi totalmente homologado.

**Pendências:** implementar `@username` no RF01 em tarefa própria; consolidar a garantia/ciclo de candidatura com a Manu; devolver à Manu os defeitos demonstrados de `fn_listar_banco_talentos` (42804), `sp_atualizar_perfil` (42703) e `sp_publicar_vaga` (42P01). RF13/RF17 ainda precisam integrar a coleção persistente específica por contratante.

**Próximo passo:** resolver os bloqueios, obter regressão verde e realizar checkpoint Git controlado; depois retomar RFs. O ambiente inicia e permite trabalhar com contas existentes, mas não atingiu o critério de homologação completa. Nenhum commit/push nesta tarefa. Artefatos Graphify foram atualizados e devem ser revisados separadamente.

Relatório completo: [Sincronização database04](C:/Users/masca/Documents/pjiiiiii/projetoPJI-react00b-baseline/RELATORIO_SINCRONIZACAO_DATABASE04_2026-10-02.md).

## Entrega posterior — RF01 e cadastro Google RF24

**Objetivo:** fechar RF01 no backend e adequar o primeiro cadastro Google à decisão posterior de não persistir usuário incompleto. RFs: RF01 + parte cadastral RF24; regressão RF02/RF26/RF27.

**Backend:** username obrigatório e único conforme database04; validações compartilhadas de CPF/CNPJ, telefone, idade, papel/subtipo, responsável e área principal; criação de usuário/perfil/associações em transação. Google usa contexto assinado de 10 minutos, separado de sessão, e endpoint de conclusão. Adulto apto é ativado sem exigir RF08; menor permanece pendente de consentimento. Linking existente, BCrypt, RF26, RF27 e RF25 preservados.

**Frontend:** NÃO alterado. **Banco/SQL:** NÃO alterados; pacote oficial confirmado 46/46 idêntico ao ZIP. **RF06:** nenhum arquivo alterado. Sem commit, push ou staging.

**Segurança:** username sem geração/normalização automática; identidade Google validada no backend; contexto rejeita adulteração e não autentica APIs normais; replay pós-criação recebe conflito; validação de idade/responsável; unicidade garantida também pela constraint real; rollback e privacidade testados.

| Execução | Total | Passed | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Baseline database04 | 791 | 739 | 34 | 1 | 17 |
| Focados finais | 113 | 113 | 0 | 0 | 0 |
| Completo final | 842 | 824 | 1 | 0 | 17 |

**Resultado:** focados BUILD SUCCESS; completo BUILD FAILURE exclusivamente em `CandidaturaControllerRf06IntegrationTest.constraintRealTrataDuplicidadeMesmoSePreConsultaNaoEncontrar` (esperava 409, recebeu 201). As 33 failures e o 1 error de cadastro/autenticação desapareceram. Não foi adicionada UNIQUE vaga/artista nem alterado o teste RF06.

**Conclusão:** RF01 backend e parte revisada de primeiro cadastro Google concluídos dentro do escopo autorizado; plataforma/suíte global ainda não verdes. **Pendências:** integrar formulários e etapa Google entre 14–21/10, homologar Google/SMTP reais, atualizar formalmente o RF24 e tratar RF06/recandidatura em tarefa própria. Graphify atualizado; derivados separados do código.

Relatório: [RF01/RF24 — username e Google](RELATORIO_RF01_RF24_USERNAME_GOOGLE_2026-10-02.md).

## Entrega posterior — conclusão backend RF06

**Objetivo:** concluir RF06 retomando a sessão interrompida, após auditar o working tree sem reiniciar nem sobrescrever trabalho correto. **RF:** RF06. **Dependências exercidas:** RF23, RF28, RF36 e exclusivamente o evento de candidatura do RF44.

**Estado encontrado:** branch `integracao-recuperada-2026-09-15`, HEAD `a6164e38b177c765638d61eb0d68c4b46f79f052`, working tree com muitas alterações anteriores. A sessão interrompida já continha lock advisory PostgreSQL por vaga/artista, histórico de duas tentativas, retirada lógica, campos opcionais, adaptação de consultas/cancelamento, eventos de responsável e testes revisados. O primeiro teste nesta continuação confirmou 137/137 focados verdes, incluindo 40 casos RF06.

**Backend herdado preservado:** DTO/entity de candidatura opcionais; histórico em CandidaturaRepository; CandidaturaService com JWT/ARTISTA/perfil completo/ABERTA/prazo/lock/ativa/limite/retirada; detalhe da última tentativa e cancelamento de ativas em VagaService; evento/listener/interface/sender RF44. O PUT formal já estava desativado no trabalho herdado; estados SQL e leitura histórica permanecem.

**Esta continuação:** pequeno ajuste em CandidaturaService para aviso apenas de 14–17 anos com conta/consentimento/e-mail elegíveis; listener RF44 com isolamento do sender e registro técnico sem dados pessoais; 17 casos adicionais/reforçados na classe de integração RF06, agora 57. Foram comprovados falha realtime e recuperação da notificação, falha de e-mail sem rollback, bloqueios sem avisos, estados terminais, perfil consumindo somente a flag e concorrência da primeira/segunda candidatura.

**Fluxo validado:** primeira candidatura 201/PENDENTE → retirada 204 → segunda linha com ID/data novos 201/PENDENTE → retirada 204 → terceira tentativa 409/“Limite de recandidatura atingido”. Duas requisições concorrentes produzem 201/409 e uma única ativa, sem UNIQUE global. Histórico permanece. Contratante recebe notificação persistida; menor autorizado gera aviso informativo após commit, sem nova autorização.

**Frontend:** NÃO alterado nesta sessão; 521 arquivos comparados iguais. Modal existente satisfaz a confirmação explícita por submit final; formulário obrigatório legado e botão de recandidatura ainda precisam integração posterior. **Banco:** NÃO alterado; database04 46/46 arquivos idênticos ao ZIP oficial, 0 divergências/arquivos adicionais, fingerprint preservado, `ddl-auto=validate`. Nenhum SQL/migration/objeto estrutural alterado, nenhum snapshot histórico ativado.

| Execução RF06 | Total | Passed | Failures | Errors | Skipped | Resultado |
|---|---:|---:|---:|---:|---:|---|
| Baseline anterior RF01/RF24 | 842 | 824 | 1 | 0 | 17 | BUILD FAILURE RF06 conhecido |
| Focados finais | 159 | 159 | 0 | 0 | 0 | BUILD SUCCESS |
| **Maven completo final** | **867** | **850** | **0** | **0** | **17** | **BUILD SUCCESS, exit 0** |

Maven completo executado com `.\mvnw.cmd test`, JDK 21.0.11/PostgreSQL 18.4 descartável/Testcontainers; 6:55 min, término às 22:59:46 de 02/10/2026. Os 17 skips condicionais permaneceram com as mesmas propriedades/classes; nenhum teste foi desabilitado. A premissa antiga de UNIQUE global foi substituída por testes reais de concorrência, sem perda da proteção útil.

**Logging JDBC:** microauditoria concluída; categoria de detalhes sensíveis permanece OFF, sem desativar handlers/transações/validate ou diagnósticos de inicialização. **Semgrep — recuperação posterior registrada em 03/10/2026:** a falha inicial de 111 regras/11 arquivos, exit 2/código 3236495362, permanece como histórico. A instalação nativa continuou bloqueada pelo Windows App Control; o workflow passa a executar Semgrep via imagem Docker oficial, sem usar a instalação nativa neste ambiente. Rulesets baixados pelo Windows e montados localmente resolveram a cadeia de certificado não confiável do container, sem desabilitar TLS. Java (`p/java`): 257 arquivos rastreados pelo Git, 60 regras, ~100% das linhas parseadas, 0 findings, sucesso. Secrets bruto: 2.271 arquivos/181 findings; auditoria identificou predominância de BCrypt de seeds, hashes/dados fictícios de evidências/logs de testes e autodetecção das expressões do ruleset temporário; não são 181 credenciais reais vazadas. Secrets acionável: excluídos a regra `generic.secrets.security.detected-bcrypt-hash.detected-bcrypt-hash` e os caminhos `evidencias/**`, `graphify-out/**`, `semgrep-java.tmp.yml`, `semgrep-secrets.tmp.yml`; 960 arquivos rastreados pelo Git, 42 regras executadas, ~100% das linhas parseadas, 0 findings/0 blocking, sucesso. 0 findings é evidência complementar e não prova sozinho regras de autorização, segurança de negócio ou ausência absoluta de segredos. Evidências posteriores fornecidas pelo usuário; atualização somente documental, sem nova execução de scans/Maven nem alteração de código do Palco para corrigir a ferramenta. **Graphify:** atualização AST concluída, exit 0; derivados devem ser revisados separadamente.

**Conclusão:** RF06 backend concluído no escopo solicitado. **RF44:** PARCIAL, apenas candidatura, com sender mockado; SMTP real e outras ações/retries pendentes. **Pendências:** RF45 e integração frontend revisada. **Próximo passo:** revisão do relatório/delta e seleção consciente de checkpoint Git após autorização posterior. Sem staging, commit, push, merge, reset, clean, restore ou stash.

Relatório: [RF06 — candidatura e recandidatura](RELATORIO_RF06_CANDIDATURA_REAPLICACAO_2026-10-02.md). Evidências: `evidencias/rf06-2026-10-02/`.

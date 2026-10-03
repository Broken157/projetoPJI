# Registro semanal — 01/10/2026

**Objetivo:** sincronizar backend e testes com o snapshot database02 da Manu e recuperar uma base reproduzível.

**Banco:** palco-database02.zip completo como nova referência ativa. SHA-256 confirmado: `ff27b5d37cc22836c16f6c1ccc326057c1e88e7ec8a02f549f5ca7fe69096d48`. Os 46 arquivos foram preservados; única correção SQL local: vírgula da constraint verificar_formato_username em usuarios, linha 29. Nenhum outro SQL alterado.

**Ambiente:** palco_dev_manu02 criado em PostgreSQL 18.4 local, porta 5432, e em Compose com container/volume novos, porta 5434. Init completo passa com ON_ERROR_STOP=1; Compose saudável. São 43 tabelas, 279 colunas, 24 enums, 12 functions, 12 procedures, 3 triggers. Seed vazio: sem usuários ou áreas de desenvolvimento.

**Backend:** somente compatibilidade e infraestrutura. Username mapeado sem geração provisória, conforme decisão explícita; implementação correta fica para RF01 futura. Contratante persiste cidade/estado, preservando acessores textuais. Ddl-auto permanece validate.

**Frontend:** não alterado; nenhum build/teste frontend executado. Alterações preexistentes foram preservadas.

**Testes:**

| Execução | Total | Passed | Failures | Errors | Skipped | Build |
|---|---:|---:|---:|---:|---:|---|
| Focados, local descartável | 42 | 31 | 0 | 11 | 0 | FAILURE |
| Bootstrap, Testcontainers Docker real | 4 | 4 | 0 | 0 | 0 | SUCCESS |
| Maven completo, Docker real | 788 | 244 | 0 | 527 | 17 | FAILURE |

Os 17 testes condicionais permaneceram condicionais. Há 526 errors de contexto JPA em 33 classes, decorrentes da tabela ausente, e um error de fn_buscar_vagas.

**Decisão técnica:** D1/B1, dump histórico e migration RF07 deixam de ser fonte ativa. Desenvolvimento e testes usam exclusivamente o init completo database02. Nenhum snapshot antigo, constraint ou trigger histórico foi acrescentado para fazer testes passarem.

**Pendências reais após executar database02:**

- categorias_afirmativas e vagas_categorias_afirmativas ausentes; Hibernate não conclui validate.
- fn_buscar_vagas falha com 42P01, relação ausente.
- fn_listar_banco_talentos falha com 42703, perfis_artistas.nivel_medalha ausente.
- Seed vazio deixa catálogo de desenvolvimento vazio.
- Cadastro atual sem @username não atende a coluna obrigatória; provisório não autorizado e não gerado.

**Aptidão:** infraestrutura reproduzível; backend funcional bloqueado; suíte não verde. Nenhuma RF nova implementada, banco antigo apagado ou operação de publicação/limpeza Git realizada.

Relatório: RELATORIO_SINCRONIZACAO_DATABASE02_2026-10-01.md. Evidências: evidencias/database02-2026-10-01/.

# Relatório Técnico — RF05: Detalhes da Vaga e Sugestão de Talentos

## 1. Objetivo e escopo

Auditar e concluir o núcleo backend do RF05, preservando o banco oficial e o frontend. O escopo entregue compreende detalhes seguros da vaga, contexto da própria candidatura, listagem paginada de candidatos exclusiva do contratante proprietário e compatibilidade explicável por tags. Não fazem parte desta entrega gamificação, alterações de status, componentes visuais nem um banco amplo de talentos.

## 2. Estado inicial

### Existente

- `GET /api/vagas/{id}` e o DTO `VagaResponse`;
- autenticação JWT e resolução do usuário autenticado;
- flag `propriaDoContratante` calculada a partir do usuário autenticado e do relacionamento persistido;
- entidades e relacionamentos `Vaga`–`Tag`, `PerfilArtista`–`Tag` e `Candidatura`;
- endpoints de candidaturas do RF06;
- `GET /api/vagas/{id}/similares`, já implementado pelo RF03;
- enum `StatusVaga` com `ABERTA`, `PAUSADA`, `ENCERRADA` e `CANCELADA` — não existe `RASCUNHO` no modelo atual.

### Correto

- vaga inexistente já seguia o padrão global de `404 Not Found`;
- a propriedade da vaga não dependia de `contratanteId` enviado pelo cliente;
- RF03, RF04, RF06, RF07 e RF08 possuíam testes de integração e permaneceram reutilizados;
- o endpoint de similares do RF03 não precisou ser duplicado;
- `spring.jpa.hibernate.ddl-auto=validate` já estava configurado.

### Parcial

- os detalhes traziam os dados da vaga, tags, nome/ID do contratante e propriedade, mas não um bloco profissional público do contratante nem o contexto da candidatura do artista autenticado;
- `GET /api/vagas/{id}` localizava a vaga diretamente pelo ID, sem aplicar a política de visibilidade por status;
- as listagens existentes de candidaturas eram protegidas por identidade, porém não havia uma rota específica, paginada e ordenada para os candidatos de uma vaga;
- os DTOs de candidatura continham dados da candidatura, mas não o perfil profissional público, as tags coincidentes nem sua quantidade;
- listagens legadas de candidaturas podiam retornar resultados sem limite explícito.

### Ausente

- endpoint `GET /api/vagas/{id}/candidaturas` exclusivo do proprietário;
- compatibilidade e ordenação de candidatos por tags;
- paginação padrão 20/máximo 50 para esse fluxo;
- testes dedicados ao RF05 e teste objetivo contra N+1;
- sugestão de artistas externos às candidaturas.

O baseline real foi executado antes das alterações: **130 testes aprovados, 0 falhas, 0 erros e 0 ignorados**, com `BUILD SUCCESS`.

## 3. Auditoria por camada

| Camada | Estado inicial | Resultado da auditoria/ação |
| --- | --- | --- |
| Entity | Completo para o núcleo | Os relacionamentos necessários já existiam; nenhuma entidade foi alterada. |
| Enum | Completo | Estados atuais preservados; não há `RASCUNHO`. |
| DTO | Parcial | `VagaResponse` foi evoluído e foram criados DTOs específicos, públicos e sanitizados para contratante e candidatos. |
| Repository | Parcial | Adicionados carregamentos em lote/`EntityGraph`, consulta paginada por IDs e busca da candidatura do próprio artista. |
| Service | Parcial | Incluídas visibilidade por status, autorização da lista, paginação, compatibilidade e montagem dos contratos públicos. |
| Controller | Parcial | Preservado o endpoint de detalhes; criada apenas a rota específica inexistente e parametrizadas as listagens legadas. |
| Segurança | Parcial | Corrigido o contorno por ID de vagas não públicas e testada a proteção IDOR da lista de candidatos. |
| Testes | Ausente para RF05 | Criada suíte dedicada com 17 métodos e 21 execuções. |

## 4. Decisão de escopo sobre medalhas

> Medalhas, níveis, conquistas e score de engajamento não foram implementados porque o RF29 foi adiado para versão futura.

Os campos antigos relacionados a esse tema que já existem em outras partes do modelo foram preservados para não alterar banco ou contratos alheios ao RF05. Eles não foram adicionados ao novo contrato de candidatos, não participam do cálculo e não influenciam a ordenação.

## 5. Alterações realizadas

| Arquivo | Alteração | Motivo |
| --- | --- | --- |
| `backend/src/main/java/com/portifolio/dto/ContratantePublicoResponse.java` | Novo DTO profissional e sanitizado do contratante. | Expor somente dados públicos necessários nos detalhes. |
| `backend/src/main/java/com/portifolio/dto/CandidaturaVagaResponse.java` | Novo DTO de candidato com perfil público, candidatura e compatibilidade. | Evitar reutilização de DTOs que não atendiam ao RF05. |
| `backend/src/main/java/com/portifolio/dto/CandidaturaVagaPaginaResponse.java` | Novo envelope de paginação. | Informar página, tamanho, total e navegação sem lista ilimitada. |
| `backend/src/main/java/com/portifolio/dto/VagaResponse.java` | Adicionados contratante público e contexto da própria candidatura; habilitado `toBuilder`. | Completar os detalhes sem duplicar o DTO existente. |
| `backend/src/main/java/com/portifolio/repository/VagaRepository.java` | Busca detalhada com `EntityGraph`. | Carregar vaga, tags e contratante de modo controlado. |
| `backend/src/main/java/com/portifolio/repository/CandidaturaRepository.java` | Busca da própria candidatura, consultas paginadas, ordenação por compatibilidade e carregamento em lote. | Segurança contextual, RNF12 e prevenção de N+1. |
| `backend/src/main/java/com/portifolio/service/VagaService.java` | Política de visibilidade, usuário JWT, contratante público e contexto da candidatura. | Impedir descoberta indevida e centralizar regras no Service. |
| `backend/src/main/java/com/portifolio/service/CandidaturaService.java` | Autorização do proprietário, paginação, resposta pública, interseção de tags e limites legados. | Concluir o fluxo de candidatos com segurança e desempenho. |
| `backend/src/main/java/com/portifolio/controller/VagaController.java` | Adicionado `GET /api/vagas/{id}/candidaturas`. | Disponibilizar a lista específica da vaga sem duplicar rota existente. |
| `backend/src/main/java/com/portifolio/controller/CandidaturaController.java` | Adicionados `page` e `size` às listagens legadas. | Eliminar retornos ilimitados mantendo o formato de array já consumido. |
| `backend/src/test/java/com/portifolio/controller/VagaDetalhesRf05IntegrationTest.java` | Nova suíte de integração RF05. | Provar comportamento funcional, segurança, privacidade, paginação, ordenação e consultas. |
| `backend/src/test/java/com/portifolio/service/VagaServiceTamanhoTest.java` | Ajuste do construtor de teste para a nova dependência do Service. | Manter o teste unitário existente compilável, sem mudar sua regra. |

## 6. Detalhes da vaga

### Endpoint

`GET /api/vagas/{id}` foi preservado e continua exigindo autenticação, coerente com a configuração e o frontend atuais.

### Campos

A resposta mantém os campos existentes do modelo: ID, título, descrição, requisitos, remuneração, forma de pagamento, cidade, estado, endereço da oportunidade, benefícios, modelo de trabalho, tipo de contrato, categoria, status, data de publicação, tags e identificação do contratante. Foram acrescentados:

- `contratantePublico`: ID público, nome de exibição/empresa, tipo, biografia, localização, banner e avatar;
- `minhaCandidaturaId` e `statusMinhaCandidatura`: preenchidos somente para o próprio artista quando houver candidatura;
- `propriaDoContratante`: preservada e calculada pelo JWT/estado persistido.

Não são retornados pelo novo bloco público e-mail, telefone, nascimento, senha, tokens, dados de responsável ou endereço residencial.

### Status e visibilidade

- `ABERTA`: visível a qualquer usuário autenticado;
- `PAUSADA`, `ENCERRADA` e `CANCELADA`: visíveis ao proprietário ou ao artista que já possui candidatura;
- demais usuários recebem `404`, evitando confirmar a existência de uma vaga fora do feed;
- o proprietário mantém acesso administrativo;
- o candidato mantém o histórico, inclusive em vaga cancelada;
- ID inexistente retorna `404`.

Essa decisão preserva a regra consolidada do RF03 e não altera a máquina de estados.

## 7. Candidaturas

### Endpoint

`GET /api/vagas/{id}/candidaturas?page={page}&size={size}`

### Autorização

- visitante: `401 Unauthorized`;
- artista autenticado: `403 Forbidden`;
- outro contratante: `403 Forbidden`;
- vaga inexistente: `404 Not Found`;
- somente o contratante proprietário recebe `200 OK`.

A autorização ocorre no Service, comparando a vaga persistida ao usuário proveniente do JWT. Nenhum ID de proprietário fornecido pelo cliente é aceito como identidade.

### Paginação

- página padrão: `0`;
- tamanho padrão: `20`;
- tamanho máximo: `50`;
- `page < 0` ou `size < 1`: `400 Bad Request`;
- resposta contém conteúdo, página, tamanho, total de elementos, total de páginas e indicadores de primeira/última página.

As rotas legadas `GET /api/candidaturas` e `GET /api/candidaturas/minhas-vagas` também passaram a aceitar `page` e `size`, com os mesmos limites, preservando o array de resposta usado pelo frontend atual.

### Dados retornados por candidato

ID da candidatura, ID e nome do artista, biografia, localização pública, portfólio, avatar, IDs das tags, IDs das tags coincidentes, quantidade de coincidências, mensagem/link enviados, status e data da candidatura. Dados privados e dados adicionais de menores não são incluídos.

## 8. Compatibilidade por tags

### Fórmula

`quantidadeTagsCoincidentes = |tagsDaVaga ∩ tagsDoArtista|`

`tagsCoincidentes` contém os IDs da mesma interseção. Não existe percentual, score artificial ou fator oculto.

### Ordenação

1. maior `quantidadeTagsCoincidentes`;
2. perfil atualizado mais recentemente (`PerfilArtista.ultimaAtualizacao`), com valores nulos por último;
3. menor ID da candidatura como desempate final estável.

Se o artista não possuir tags, sua compatibilidade é zero. Se a vaga não possuir tags, todos recebem compatibilidade zero e os critérios 2 e 3 mantêm a ordem determinística.

## 9. Artistas sugeridos

Não existia no backend um fluxo distinto de artistas sugeridos que ainda não se candidataram. Ele não foi criado, pois caracterizaria expansão para o banco de talentos do RF13.

O RF05 entregue prioriza os candidatos reais da vaga, classificados por compatibilidade de tags. Uma futura sugestão externa permanece dependente do RF13, incluindo suas regras de perfis ativos/completos. O endpoint de vagas similares do RF03 foi preservado e não foi reimplementado.

## 10. Segurança

- **JWT:** identidade obtida exclusivamente da autenticação atual;
- **propriedade:** calculada pelo relacionamento persistido da vaga;
- **IDOR:** contratante B não acessa candidatos da vaga do contratante A (`403`);
- **visibilidade:** acesso direto por ID não revela vaga não aberta para artista sem candidatura (`404`);
- **privacidade:** DTOs específicos limitam campos de contratante e artista ao contexto profissional;
- **menores:** o cenário de integração confirma ausência de nascimento, telefone, e-mail, responsável, senha, hash e token;
- **binding:** as consultas usam parâmetros JPA; não há concatenação de input em SQL/JPQL.

## 11. Performance

A paginação é aplicada primeiro a uma consulta de IDs ordenados no banco. Em seguida, uma única consulta em lote com `EntityGraph` carrega candidatura, artista, usuário e tags do artista; a resposta é recomposta na ordem da página. Essa estratégia evita `JOIN FETCH` de múltiplas coleções na consulta paginada, produto cartesiano e uma consulta de tags por candidato.

O teste com 20 candidatos usa estatísticas do Hibernate e exige no máximo 8 statements preparados para toda a requisição, comprovando ausência de crescimento N+1 nesse cenário. A página padrão e o limite máximo também impedem carregamento ilimitado.

## 12. Banco

**Banco alterado: NÃO**

> Nenhuma tabela, coluna, enum, tipo, constraint, índice, migration ou script SQL foi alterado.

`ddl-auto=validate` permanece ativo. A comparação SHA-256 antes/depois confirmou os scripts SQL e o schema de teste idênticos.

## 13. Frontend

**Frontend alterado: NÃO**

> Nenhum arquivo HTML, CSS ou JavaScript foi alterado.

A comparação SHA-256 antes/depois confirmou todos os arquivos do frontend idênticos. O consumo dos novos campos e do endpoint específico é uma integração futura.

## 14. Testes

### Baseline

Comando:

```powershell
.\mvnw.cmd -o '-Dmaven.repo.local=C:\Users\masca\.m2\repository' '-Dspring.jpa.show-sql=false' test
```

- total: 130;
- aprovados: 130;
- falhas: 0;
- erros: 0;
- ignorados: 0;
- resultado: `BUILD SUCCESS`.

### Novos testes RF05

Classe: `VagaDetalhesRf05IntegrationTest`

- métodos de teste: 17 (15 testes diretos e 2 parametrizados);
- execuções: 21;
- aprovadas: 21;
- falhas: 0;
- erros: 0;
- ignoradas: 0;
- resultado direcionado: `BUILD SUCCESS`.

Cenários cobertos: detalhes e tags; contratante público e privacidade; `404`; propriedade; visitante; quatro estados; histórico do candidato; candidato e não candidato; autorizações `401/403/404`; privacidade de candidato menor; ordenação por coincidência/atualização/ID; artista/vaga sem tags; página padrão, máximo, primeira/última página e ausência de duplicação; parâmetros inválidos; limites nas rotas legadas; contagem de consultas anti-N+1.

### Final

O mesmo comando do baseline foi executado após as alterações:

- total: 151;
- aprovados: 151;
- falhas: 0;
- erros: 0;
- ignorados: 0;
- PostgreSQL/Testcontainers preservado (PostgreSQL 18.4);
- resultado: **`BUILD SUCCESS`**.

Durante o desenvolvimento, a primeira compilação direcionada detectou que o teste unitário `VagaServiceTamanhoTest` ainda construía `VagaService` com a assinatura anterior. O fixture foi ajustado para a nova dependência e todas as execuções subsequentes passaram; não houve falha funcional da aplicação.

## 15. Regressões

| Área | Execuções finais | Resultado |
| --- | ---: | --- |
| RF03 | 27 | Aprovado |
| RF04 | 19 | Aprovado |
| RF06 | 20 | Aprovado |
| RF07 | 15 | Aprovado |
| RF08 | 23 | Aprovado |
| Autenticação/JWT e perfil | Cobertura integrada na suíte | Aprovado |
| Suíte completa | 151 | `BUILD SUCCESS` |

## 16. Conflitos encontrados

| Conflito | Impacto | Decisão | Justificativa |
| --- | --- | --- | --- |
| Documento antigo cita RF29/medalhas/engajamento. | Poderia introduzir score, DTOs falsos ou alteração de banco. | Não implementar nem usar esses dados no RF05. | RF29 foi formalmente adiado. |
| “Sugestão de talentos” pode significar candidatos ou artistas externos. | Artistas externos ampliariam o escopo para RF13. | Classificar candidatos existentes; registrar sugestão externa como futura. | O núcleo solicitado é atendido sem simular RF13. |
| Endpoint de detalhes permitia localizar qualquer status por ID. | Contorno da ocultação de vagas não abertas. | Não abertas ficam restritas ao proprietário e ao artista já candidato; demais recebem `404`. | Preserva histórico e evita descoberta indevida. |
| Configuração atual exige autenticação para os detalhes. | Visitante recebe `401`, embora o texto use “usuário autorizado”. | Preservar a política existente. | Não houve requisito inequívoco para tornar a vaga pública nem necessidade do frontend atual. |
| Listagens legadas retornavam array sem paginação. | Risco de lista ilimitada; mudar para envelope quebraria o consumidor atual. | Adicionar paginação mantendo o array; usar envelope completo apenas no novo endpoint por vaga. | Atende ao limite com compatibilidade retroativa. |
| Campos antigos de medalha/score já existem em outra parte do modelo. | Removê-los exigiria alteração fora do RF05 e possivelmente de banco. | Preservar, sem expor ou usar no contrato/algoritmo RF05. | Respeita escopo, banco estável e regressões. |

## 17. Pendências

- integrar o frontend futuro aos novos campos de detalhe e ao endpoint paginado de candidatos;
- implementar sugestão de artistas externos somente no escopo futuro do RF13, com regra própria de perfis ativos/completos;
- qualquer remoção dos campos antigos ligados ao RF29 deve ser tratada em tarefa própria, com autorização explícita para migração/compatibilidade; não é necessária para o RF05 atual.

## 18. Conclusão

**RF05 CONCLUÍDO NO BACKEND.**

Há evidência automatizada para detalhes, `404`, tags, contratante público sanitizado, propriedade por JWT, visibilidade por status, contexto da própria candidatura, lista paginada exclusiva do proprietário, compatibilidade por tags, ordenação determinística, privacidade, IDOR e ausência de N+1 grave. A regressão terminou com **151/151 testes aprovados** e **`BUILD SUCCESS`**. Banco e frontend permaneceram inalterados. A sugestão de artistas externos foi corretamente mantida como dependência futura do RF13 e não bloqueia o núcleo concluído do RF05.

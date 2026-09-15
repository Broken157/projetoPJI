# Palco — Perfil, portfólios e navbar final

Front-end React em JavaScript/JSX, HTML e CSS, baseado em ImagensPJI.zip, ImagensPJI2.zip, PALCO - Versão Final.zip e na tela de login anterior.

## Executar

Requer Node.js 20.19 ou superior.

```bash
npm install
npm run dev
```

Abra http://127.0.0.1:5173. Para produção, execute `npm run build`; `npm run preview` serve a compilação.

## Telas e navegação

- `/perfil`: perfil de Luna Costa com os dois exemplos visuais fornecidos.
- `/perfil/vazio`: referência do perfil sem portfólios.
- `/portfolio/novo`: editor; acesse também por **Criar um portfólio**.
- `/portfolio/editar`: conteúdo que está sendo editado, ou rascunho salvo.
- `/galeria`: galeria de obras; o botão amarelo abre o pop-up de envio de fotos.
- `/login`: tela de login da entrega anterior, com a mesma navbar em variante de visitante.
- `/perfil/primeiro`: referência do perfil com um primeiro portfólio de mídias.
- `/portfolio/referencia`: editor preenchido com as imagens do novo conjunto; vídeo e áudio aparecem como referências sem arquivos de reprodução, pois o ZIP contém apenas SVGs.

No perfil, **Adicionar fotos de obras** abre a galeria e o pop-up de fotos. Selecione pelo menos cinco fotos para começar, por clique ou arrastando. Remova seleções individualmente antes de confirmar.

No editor, **Adicionar link externo** segue a referência: seleciona um PDF local que depois será aberto em nova aba. Não é um campo para colar URLs. Arquivos PDF válidos têm até 10 MB e assinatura `%PDF-`. Há pop-ups separados para seleção, erro e confirmação. **Submeter** adiciona o PDF ao editor. Use a engrenagem para trocar ou excluir; a exclusão sempre exige confirmação.

**Salvar como rascunho** grava o trabalho no navegador. **Publicar** abre “Estamos quase lá!”, com capa e título obrigatórios e descrição opcional. Após confirmar, o portfólio é adicionado ao perfil deste navegador. A primeira publicação efetiva abre o aviso de primeiro portfólio. Capas de trabalhos novos não recebem selo de autenticação: a interface mostra que estão aguardando análise. A concessão de selo depende de backend e administração. **Continuar rascunho** aparece em Gestão rápida quando existe rascunho. Os exemplos LA CANCION e MY SHEET BLUES usam as capas originais fornecidas; os PDFs correspondentes não vieram no ZIP, portanto a abertura informa essa limitação.

Imagens: JPG/JPEG/PNG até 5 MB. Áudios: MP3 até 20 MB. Vídeos: link HTTP/HTTPS válido e legenda opcional de até 200 caracteres. YouTube e Vimeo têm player incorporado; links diretos MP4/WebM/OGG usam player nativo. Outros links abrem externamente. Links malformados exibem o aviso de URL inválida. A disponibilidade do player depende do provedor e das permissões de incorporação. Imagens e áudios podem compor um portfólio com vários itens.

## Organização

- `index.html`: entrada HTML.
- `src/main.jsx`: inicialização React.
- `src/App.jsx`: perfil, editor, galeria e fluxos.
- `src/Login.jsx`: login da entrega anterior.
- `src/components/Navbar.jsx` e `Navbar.css`: navbar compartilhada, menus de Conexões e conta, responsividade e fechamento por Escape/clique fora.
- `src/components/Media.jsx`: capas, áudio e vídeo.
- `src/components/PublishDialog.jsx`: etapa final de publicação.
- `src/components/FirstPortfolioDialog.jsx`: aviso de primeiro portfólio.
- `src/components/DeletePortfolioDialog.jsx`: exclusão digitando EXCLUIR.
- `src/components/Modal.jsx`: pop-up com diálogo nativo, foco, Escape e restauração de foco.
- `src/components/Icon.jsx`: ícones de interface.
- `src/lib/storage.js`: armazenamento IndexedDB e validação.
- `src/portfolio.css` e `src/portfolio-v2.css`: estilos responsivos das telas.
- `src/lib/portfolio.js`: validação de metadados, URLs e confirmação de exclusão.
- `src/styles.css`: estilos do login.
- `src/fonts.css` e `public/fonts/`: fontes locais Inter, Anton e Lato.
- `public/assets/reference/`: imagens extraídas das referências.

## Identidade visual

Cores principais: `#9C0088`, `#0A0E2A`, `#FFBF00`, `#FFF9FF`. Inter nos controles e títulos, Lato em textos de leitura e Anton nos títulos de trabalhos. As fontes são servidas localmente.

## Escopo e integração

A entrega contém apenas front-end. IndexedDB guarda arquivos, portfólios e rascunhos neste navegador/dispositivo, inclusive após recarregar. Não há envio a servidor, autenticação real, sincronização entre dispositivos nem publicação externa de arquivos. Os contadores do perfil e informações de Luna são dados de referência. Páginas do menu que não estavam nas imagens exibem aviso, sem inventar conteúdo.

Para integrar um backend, substitua `readState` e `saveState` pelo serviço de dados e armazenamento de arquivos; integre os callbacks de Login.jsx à autenticação. O servidor deverá repetir todas as validações dos arquivos.

## Validação

Compilação de produção e verificações de renderização das rotas e regras de formato/tamanho. Não foi realizada conferência visual automatizada em navegador.

## Navbar e novos fluxos

A navbar de PALCO - Versão Final é usada em perfil, galeria, editor e login. No editor, os comandos Voltar, Salvar como rascunho e Publicar ficam numa barra de ações abaixo da navbar. Conexões abre Comunidades e Galeria Virtual; a conta abre Meu perfil, Mensagens, Denúncia, Solicitações, Suporte, Configurações e Sair. Funcionalidades sem telas fornecidas informam a limitação. Sair retorna ao login e não apaga os arquivos locais.

A exclusão de portfólios de mídias exige digitar exatamente EXCLUIR. PDFs mantêm o pop-up simples da referência anterior. Botões de reprodução funcionam para arquivos enviados e links válidos. Os exemplos visuais não simulam reprodução de arquivos que não vieram nos ZIPs.

## Login e dashboard — ImagensPJI3

A página inicial agora leva ao login. Após entrar, a dashboard dá acesso ao perfil, às mídias e aos portfólios anteriores. O menu **Início** retorna à dashboard e **Meu perfil** abre `/perfil`. **Sair** encerra a sessão de demonstração sem apagar os arquivos e rascunhos.

### Conta de demonstração

- E-mail: `luna@palco.demo`
- Senha inicial: `PalcoDemo123!`
- Na tela, **Conta de artista** ou **Conta de contratante** preenche os campos; depois clique em Entrar.

O modo demonstração é explicitamente identificado na interface. Valida as duas contas de demonstração, guarda a sessão na aba por até oito horas e permite testar recuperação e redefinição locais. Não oferece segurança ou autorização de backend. Não use senhas reais. A redefinição altera somente a credencial da conta selecionada neste navegador; depois disso, use a nova senha. O link de demonstração expira em 15 minutos e só pode ser usado uma vez. Nenhum e-mail é enviado.

### Novas rotas

- `/login`: login, erro de credenciais e mostrar/ocultar senha.
- `/dashboard`: recomendações, mensagens, notificações e acesso ao perfil.
- `/dashboard?painel=mensagens`: painel de mensagens de referência.
- `/recuperar-senha`: recuperação; use o e-mail de demonstração.
- `/redefinir-senha?token=...`: redefinição a partir do link obtido na recuperação.
- `/termos`: trechos de Termos de Uso presentes nas imagens.
- `/privacidade`: os mesmos trechos, com indicação de que o material chamado Política de privacidade contém Termos de Uso. Os itens 2 a 6 e uma política de privacidade separada não foram fornecidos.

O acesso direto à dashboard, perfil, galeria ou editor retorna ao login quando não há sessão. Isso é uma regra de navegação do protótipo, não uma proteção de dados do lado do servidor. Os dados de perfil continuam sendo os de Luna Costa das referências.

### Conectar a um backend próprio

O arquivo `src/lib/auth.js` centraliza a integração. Copie `.env.example` para `.env.local`, configure `VITE_AUTH_API_URL` e recompile. Essa variável é pública e contém somente a origem da API, nunca segredos. O servidor precisa fornecer:

| Método | Endpoint | Contrato |
| --- | --- | --- |
| GET | `/auth/session` | `{ user: { id, name, email } }`, ou 401 |
| POST | `/auth/login` | Recebe `{ email, password }`; retorna `{ user }` e cria sessão por cookie |
| POST | `/auth/logout` | Encerra a sessão |
| POST | `/auth/recover` | Recebe `{ email }`; envia o processo de recuperação real sem revelar existência da conta |
| POST | `/auth/reset` | Recebe `{ token, password }`; valida o token e redefine a senha |
| GET | `/auth/google` | Retorna `{ url }` HTTPS para iniciar a autenticação configurada no servidor |

Nenhum backend, envio de e-mail ou OAuth do Google foi implantado nesta entrega. Ao integrar, o servidor deve cuidar de autenticação, autorização por usuário, cookies de sessão HttpOnly/Secure, proteção CSRF, limites de tentativa e envio/validação de links. Perfil, arquivos e portfólios também precisam ser integrados ao servidor para uso real por múltiplos usuários. O armazenamento IndexedDB atual é local ao navegador.

### Dashboard

As três imagens de dashboard são posições de rolagem de uma única página, com navbar fixa. O resumo de portfólios usa os itens publicados localmente. As vagas, pessoas e notificações reproduzem os exemplos enviados; candidaturas são somente salvas neste navegador e mensagens não são enviadas. O login com Google informa a integração pendente no modo demonstração.

### Arquivos adicionados

- `src/pages/AuthScreens.jsx`: login, recuperação e redefinição.
- `src/pages/Dashboard.jsx`: dashboard e detalhes locais das referências.
- `src/pages/Terms.jsx`: texto dos trechos enviados.
- `src/lib/auth.js`: sessão de demonstração e adaptador de autenticação.
- `src/auth-dashboard.css`: estilos das novas telas.
- `public/assets/dashboard/`: imagens extraídas de ImagensPJI3.

Verificados: credenciais válidas e inválidas, sessão e saída, validação das senhas, redefinição com token válido/inválido, uso único, expiração, rotas públicas e renderização das páginas. Não foi feita inspeção visual automatizada no navegador.


## Dois tipos de conta — ImagensPJI4

A sessão agora inclui `role`, com valores `artista` ou `contratante`. O tipo de acesso vem da conta autenticada, e não de um seletor de permissão. Os botões na tela de login apenas preenchem os exemplos; confirme em **Entrar**.

| Conta | E-mail | Senha inicial |
| --- | --- | --- |
| Artista — Luna Costa | `luna@palco.demo` | `PalcoDemo123!` |
| Contratante — Bruno Correia | `bruno@palco.demo` | `PalcoDemo123!` |

Senhas redefinidas anteriormente para Luna são preservadas. Recuperação e redefinição usam o e-mail da conta correspondente, e alterar a senha de Bruno não altera a de Luna. Sessões antigas de Luna são reconhecidas e recebem o papel de artista.

### Rotas e capacidades

- `/dashboard`: dashboard de artista para Luna; dashboard de contratante para Bruno.
- `/contratante/perfil`: resumo da conta de Bruno.
- `/artistas/luna`: perfil já existente de Luna, em modo de consulta para o contratante.
- `/talentos`: banco de talentos do contratante, com pesquisa por nome, área e habilidades, filtro de área, favoritos e navegação horizontal.
- `/mensagens`: inbox com pesquisa de contatos, conversas, anexos e visualização de mídias, disponível para os dois tipos de conta.
- `/mensagens?contato=alanis`, `/mensagens?contato=shawn` etc.: abrir uma conversa específica.

O contratante não acessa o editor de portfólios nem os controles de edição de Luna. As operações de persistência do portfólio também verificam o papel de artista. Essa diferenciação é uma regra do protótipo; a autorização de produção precisa ser aplicada pelo backend em todos os endpoints.

### Dados locais

Portfólios antigos de Luna preservam a chave existente no IndexedDB. Preferências de Bruno (seguir, favoritos e banco de talentos ativo) e conversas de cada conta usam chaves separadas por `user.id`. A troca de conta não mistura o histórico local. Conversas reproduzem os exemplos e aceitam novas mensagens de demonstração; não se comunicam com outras pessoas, contas remotas ou outros dispositivos.

Os cartões repetidos de Luna, os indicadores e as notificações da dashboard do contratante são conteúdo visual de referência. Seguir Luna atualiza todos os cartões da mesma artista, pois representam a mesma identidade.

### Anexos do chat

O clipe abre **Imagens**, **Vídeo** e **Documento**. Depois de selecionar um arquivo, aparece o pop-up **Confirme a submissão do arquivo**, com prévia, nome, tamanho e botão **Submeter**. A mensagem só recebe o arquivo após essa confirmação. O menu de três pontos abre as mídias da conversa.

- Imagens JPG/JPEG/PNG: até 5 MB.
- Vídeos MP4/WebM: até 20 MB.
- Documentos PDF/TXT/DOC/DOCX: até 10 MB.

Imagens e vídeos têm visualização; PDFs podem ser abertos no visualizador; outros documentos são disponibilizados para download. Os arquivos ficam no IndexedDB da conta neste navegador.

### Integração futura

O backend de autenticação deve retornar `{ user: { id, name, email, role } }`. Valores de papel desconhecidos são rejeitados. Preferências, conversas e arquivos devem ser migrados para endpoints que autorizem operações por usuário antes de usar o produto em produção. Nenhum servidor, envio real de mensagens ou autenticação externa foi implantado nesta entrega.

Arquivos novos: `src/pages/HirerDashboard.jsx`, `TalentBank.jsx`, `Inbox.jsx`, `src/lib/hirer-data.js`, `useAccountData.js` e `src/hirer.css`.

Verificados nesta versão: autenticação dos dois usuários, preservação da senha de artista, isolamento da redefinição, roteamento por papel, filtros e favoritos, limites/formato de anexos e renderização das novas páginas. Build de produção concluído. Não houve inspeção visual automatizada em navegador.

## Comunidades, agenda e exclusão de conta (ImagensPJI5)

- `/comunidades`: festival, colaboradores e organizadora, participação, curtidas, compartilhamento por cópia de link e comentários com respostas.
- `/comunidade2`: discussão da mesma publicação, com ícones SVG acessíveis nas ações que estavam vazias na referência.
- `/agenda`: eventos da referência, criação de eventos por formulário, detalhes e remoção confirmada em modal. Salvar a comunidade adiciona o festival à agenda; removê-lo não apaga a publicação.
- `/conta/excluir`: motivo, confirmação EXCLUIR, aceite obrigatório, confirmação em modal e cancelamento da solicitação.

A navbar leva a Comunidades em Conexões, e a Minha agenda e Excluir conta no menu da conta. As duas contas possuem dados locais independentes, no IndexedDB. As datas dos exemplos foram preservadas; a agenda mostra todos os eventos, sem filtro fictício de mês ou contagem regressiva fixa. Comentários e participações são simulações locais e não chegam a outras pessoas. Solicitações de exclusão não apagam contas, não enviam e-mail e não têm processamento automático; exigem futura integração com backend. O prazo mostrado na referência é apenas parte do layout de demonstração. Não foi realizada verificação visual em navegador nesta atualização.

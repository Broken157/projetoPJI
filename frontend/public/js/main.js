/* Integração das telas Palco com a API Spring Boot existente. */
(function () {
  'use strict';

  var API_BASE = (window.PALCO_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');
  var CHAVE_SESSAO = 'palco.sessao';
  var CHAVE_RASCUNHO_VAGA = 'palco.vaga.rascunho';
  var paginaAtual = window.location.pathname.split('/').pop() || 'index.html';

  function lerSessao() {
    var bruto = sessionStorage.getItem(CHAVE_SESSAO) || localStorage.getItem(CHAVE_SESSAO);
    if (!bruto) return null;
    try {
      return JSON.parse(bruto);
    } catch (erro) {
      sessionStorage.removeItem(CHAVE_SESSAO);
      localStorage.removeItem(CHAVE_SESSAO);
      return null;
    }
  }

  function salvarSessao(dados) {
    sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(dados));
  }

  function limparSessao() {
    sessionStorage.removeItem(CHAVE_SESSAO);
    localStorage.removeItem(CHAVE_SESSAO);
    sessionStorage.removeItem(CHAVE_RASCUNHO_VAGA);
  }

  function mensagemDaApi(corpo, status) {
    if (corpo && corpo.mensagem) {
      var detalhes = Array.isArray(corpo.detalhes) && corpo.detalhes.length
        ? '\n' + corpo.detalhes.join('\n')
        : '';
      return corpo.mensagem + detalhes;
    }
    return 'Não foi possível concluir a operação (HTTP ' + status + ').';
  }

  async function renovarToken() {
    var sessao = lerSessao();
    if (!sessao || !sessao.refreshToken) return null;
    var resposta = await fetch(API_BASE + '/auth/refresh', {
      method: 'POST',
      headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: sessao.refreshToken })
    });
    if (!resposta.ok) return null;
    var corpo = await resposta.json();
    sessao.token = corpo.token;
    salvarSessao(sessao);
    return corpo.token;
  }

  async function api(caminho, opcoes) {
    var config = Object.assign({}, opcoes || {});
    var headers = Object.assign({ Accept: 'application/json' }, config.headers || {});
    var sessao = lerSessao();

    if (config.body && typeof config.body !== 'string') {
      headers['Content-Type'] = 'application/json';
      config.body = JSON.stringify(config.body);
    }
    if (sessao && sessao.token) {
      headers.Authorization = 'Bearer ' + sessao.token;
    }
    config.headers = headers;

    var resposta = await fetch(API_BASE + caminho, config);
    if (resposta.status === 401 && caminho !== '/auth/refresh' && !config._tentativaRenovada) {
      var tokenRenovado = await renovarToken();
      if (tokenRenovado) {
        config._tentativaRenovada = true;
        config.headers.Authorization = 'Bearer ' + tokenRenovado;
        resposta = await fetch(API_BASE + caminho, config);
      }
    }
    var tipo = resposta.headers.get('content-type') || '';
    var corpo = tipo.indexOf('application/json') >= 0 ? await resposta.json() : null;
    if (!resposta.ok) {
      throw new Error(mensagemDaApi(corpo, resposta.status));
    }
    return corpo;
  }

  function exigirSessaoContratante() {
    var sessao = lerSessao();
    if (!sessao || !sessao.token) {
      window.location.href = 'login.html';
      return null;
    }
    if (sessao.tipoUsuario !== 'CONTRATANTE') {
      alert('Esta área é exclusiva para contratantes.');
      window.location.href = 'login.html';
      return null;
    }
    return sessao;
  }

  function exigirSessao() {
    var sessao = lerSessao();
    if (!sessao || !sessao.token) {
      window.location.href = 'login.html';
      return null;
    }
    return sessao;
  }

  function escapar(valor) {
    return String(valor == null ? '' : valor)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function dataBrasileira(valor) {
    if (!valor) return 'Não informado';
    var partes = String(valor).substring(0, 10).split('-');
    return partes.length === 3 ? partes[2] + '/' + partes[1] + '/' + partes[0] : valor;
  }

  function dataIsoDeTexto(valor) {
    if (!valor) return null;
    var encontrada = String(valor).match(/(\d{2})\/(\d{2})\/(\d{4})/);
    if (encontrada) return encontrada[3] + '-' + encontrada[2] + '-' + encontrada[1];
    return /^\d{4}-\d{2}-\d{2}$/.test(valor) ? valor : null;
  }

  function moeda(valor) {
    var numero = Number(valor || 0);
    return numero.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  function numeroMonetario(valor) {
    var texto = String(valor || '').replace(/[^\d,.-]/g, '');
    if (texto.indexOf(',') >= 0) texto = texto.replace(/\./g, '').replace(',', '.');
    var numero = Number(texto);
    return Number.isFinite(numero) ? numero : 0;
  }

  function normalizarModelo(valor) {
    var texto = String(valor || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase();
    if (texto === 'HIBRIDO') return 'HIBRIDO';
    if (texto === 'REMOTO') return 'REMOTO';
    return 'PRESENCIAL';
  }

  function payloadDaVaga(vaga) {
    return {
      contratanteId: vaga.contratanteId,
      titulo: vaga.titulo,
      descricao: vaga.descricao,
      requisitos: vaga.requisitos,
      remuneraValor: Number(vaga.remuneraValor),
      formaPagamento: vaga.formaPagamento,
      cidade: vaga.cidade,
      estado: vaga.estado,
      enderecoCompleto: vaga.enderecoCompleto || null,
      beneficios: vaga.beneficios || null,
      modeloTrabalho: vaga.modeloTrabalho,
      tipoContrato: vaga.tipoContrato,
      status: vaga.status,
      tagIds: vaga.tagIds || [],
      categoria: vaga.categoria || null,
      experiencia: vaga.experiencia || null,
      dataLimiteCandidatura: vaga.dataLimiteCandidatura || null,
      abrangencia: vaga.abrangencia || null,
      fotos: vaga.fotos || []
    };
  }

  function aplicarDadosDaSessao() {
    var sessao = lerSessao();
    if (!sessao) return;
    document.querySelectorAll('.app-navbar__perfil').forEach(function (imagem) {
      if (sessao.avatarUrl) imagem.src = sessao.avatarUrl;
    });
  }

  function iniciarBotoesDeSenha() {
    document.querySelectorAll('[data-alvo]').forEach(function (botao) {
      botao.addEventListener('click', function () {
        var input = document.getElementById(botao.dataset.alvo);
        if (!input) return;
        var oculta = input.type === 'password';
        input.type = oculta ? 'text' : 'password';
        botao.setAttribute('aria-label', oculta ? 'Ocultar senha' : 'Mostrar senha');
        input.focus();
      });
    });
  }

  function iniciarFormularioLogin() {
    var form = document.getElementById('form-login');
    if (!form) return;
    if (new URLSearchParams(window.location.search).get('cadastro') === 'sucesso') {
      alert('Cadastro realizado com sucesso! Faça login.');
      history.replaceState({}, '', 'login.html');
    }

    form.addEventListener('submit', async function (evento) {
      evento.preventDefault();
      var email = form.querySelector('#email').value.trim();
      var senha = form.querySelector('#senha').value;
      if (!email || !senha) {
        alert('Preencha e-mail e senha para continuar.');
        return;
      }
      var botao = form.querySelector('[type="submit"]');
      botao.disabled = true;
      try {
        var resposta = await api('/auth/login', {
          method: 'POST',
          body: { email: email, senha: senha, rememberMe: false }
        });
        salvarSessao(resposta);
        window.location.href = 'dashboard-contratante.html';
      } catch (erro) {
        alert(erro.message);
      } finally {
        botao.disabled = false;
      }
    });
  }

  function iniciarDataDeNascimento() {
    var dia = document.getElementById('dia');
    var mes = document.getElementById('mes');
    var ano = document.getElementById('ano');
    if (!dia || !mes || !ano) return;
    var meses = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
      'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
    function adicionar(select, valor, texto) {
      var opcao = document.createElement('option');
      opcao.value = valor;
      opcao.textContent = texto;
      select.appendChild(opcao);
    }
    for (var d = 1; d <= 31; d++) adicionar(dia, d, d);
    meses.forEach(function (nome, indice) { adicionar(mes, indice + 1, nome); });
    var anoAtual = new Date().getFullYear();
    for (var a = anoAtual - 14; a >= anoAtual - 100; a--) adicionar(ano, a, a);

    function atualizarResponsavel() {
      if (!dia.value || !mes.value || !ano.value) return;
      var nascimento = new Date(Number(ano.value), Number(mes.value) - 1, Number(dia.value));
      var hoje = new Date();
      var idade = hoje.getFullYear() - nascimento.getFullYear();
      if (hoje < new Date(hoje.getFullYear(), nascimento.getMonth(), nascimento.getDate())) idade--;
      document.querySelectorAll('[data-responsavel]').forEach(function (campo) {
        campo.hidden = idade >= 18;
        var input = campo.querySelector('input');
        if (input) input.required = idade < 18;
      });
    }
    [dia, mes, ano].forEach(function (select) { select.addEventListener('change', atualizarResponsavel); });
  }

  function iniciarSeletores() {
    document.querySelectorAll('[data-seletor]').forEach(function (seletor) {
      var gatilho = seletor.querySelector('.seletor__gatilho');
      var lista = seletor.querySelector('.seletor__lista');
      var valor = seletor.querySelector('[data-seletor-valor]');
      var input = seletor.querySelector('[data-seletor-input]');
      var opcoes = seletor.querySelectorAll('.seletor__opcao');
      var escolha = document.querySelector('[data-perfil-escolha]');
      var nomeTag = document.querySelector('[data-perfil-nome]');
      var tituloInfo = document.querySelector('[data-perfil-titulo]');
      var descricaoInfo = document.querySelector('[data-perfil-descricao]');
      var removerTag = document.querySelector('.tag__remover');
      function abrir(aberto) {
        lista.hidden = !aberto;
        gatilho.setAttribute('aria-expanded', String(aberto));
      }
      function selecionar(opcao) {
        opcoes.forEach(function (item) { item.setAttribute('aria-selected', 'false'); });
        opcao.setAttribute('aria-selected', 'true');
        var texto = opcao.textContent.trim();
        if (input) input.value = texto;
        if (valor) valor.textContent = texto;
        if (escolha) {
          escolha.hidden = false;
          if (nomeTag) nomeTag.textContent = texto;
          if (tituloInfo) tituloInfo.textContent = texto;
          if (descricaoInfo) descricaoInfo.textContent = opcao.dataset.descricao || '';
        }
        abrir(false);
      }
      gatilho.addEventListener('click', function () { abrir(lista.hidden); });
      opcoes.forEach(function (opcao) {
        opcao.addEventListener('click', function () { selecionar(opcao); });
        opcao.addEventListener('keydown', function (evento) {
          if (evento.key === 'Enter' || evento.key === ' ') {
            evento.preventDefault();
            selecionar(opcao);
          }
        });
      });
      if (removerTag) removerTag.addEventListener('click', function () {
        if (input) input.value = '';
        if (valor) valor.textContent = 'Qual tipo de perfil você será dentro da aplicação...';
        if (escolha) escolha.hidden = true;
      });
      document.addEventListener('click', function (evento) {
        if (!seletor.contains(evento.target)) abrir(false);
      });
      document.addEventListener('keydown', function (evento) {
        if (evento.key === 'Escape') abrir(false);
      });
    });
  }

  function iniciarFormularioCadastro() {
    var form = document.getElementById('form-cadastro');
    if (!form) return;
    form.addEventListener('submit', async function (evento) {
      evento.preventDefault();
      if (!form.reportValidity()) return;
      var senha = form.querySelector('#senha').value;
      if (senha !== form.querySelector('#confirmar-senha').value) {
        alert('As senhas não conferem.');
        return;
      }
      if (!form.querySelector('#termos').checked) {
        alert('É preciso aceitar os Termos de Uso para continuar.');
        return;
      }
      var telefone = (form.querySelector('#ddd').value + form.querySelector('#telefone').value)
        .replace(/\D/g, '');
      if (!telefone) {
        alert('Informe o telefone para contato.');
        return;
      }
      var payload = {
        nome: (form.querySelector('#nome').value.trim() + ' ' + form.querySelector('#sobrenome').value.trim()).trim(),
        dataNascimento: form.querySelector('#ano').value + '-' +
          String(form.querySelector('#mes').value).padStart(2, '0') + '-' +
          String(form.querySelector('#dia').value).padStart(2, '0'),
        telefone: telefone,
        email: form.querySelector('#email').value.trim(),
        senha: senha,
        tipoUsuario: 'CONTRATANTE',
        tipoPerfilContratante: form.querySelector('[data-seletor-input]').value || null,
        nomeResponsavel: form.querySelector('#nome-responsavel').value || null,
        telefoneResponsavel: form.querySelector('#telefone-responsavel').value || null,
        emailResponsavel: form.querySelector('#email-responsavel').value || null
      };
      var botao = form.querySelector('[type="submit"]');
      botao.disabled = true;
      try {
        await api('/auth/cadastro', { method: 'POST', body: payload });
        window.location.href = 'login.html?cadastro=sucesso';
      } catch (erro) {
        alert(erro.message);
      } finally {
        botao.disabled = false;
      }
    });
  }

  function iniciarDrawer() {
    var drawer = document.querySelector('[data-drawer]');
    var overlay = document.querySelector('[data-drawer-overlay]');
    var abrir = document.querySelector('[data-drawer-abrir]');
    var fechar = document.querySelector('[data-drawer-fechar]');
    if (!drawer || !abrir) return;
    function alternar(aberto) {
      drawer.dataset.aberto = String(aberto);
      abrir.setAttribute('aria-expanded', String(aberto));
      if (overlay) overlay.hidden = !aberto;
      document.body.style.overflow = aberto ? 'hidden' : '';
    }
    abrir.addEventListener('click', function () { alternar(drawer.dataset.aberto !== 'true'); });
    if (fechar) fechar.addEventListener('click', function () { alternar(false); });
    if (overlay) overlay.addEventListener('click', function () { alternar(false); });
    document.addEventListener('keydown', function (evento) {
      if (evento.key === 'Escape') alternar(false);
    });
  }

  function iniciarModalExclusao() {
    var modal = document.querySelector('[data-modal-exclusao]');
    if (!modal) return;
    var input = modal.querySelector('[data-modal-input]');
    var motivo = modal.querySelector('[data-modal-motivo]');
    var confirmar = modal.querySelector('[data-modal-confirmar]');
    var vagaId = new URLSearchParams(window.location.search).get('id');
    function alternar(aberto) {
      modal.hidden = !aberto;
      document.body.style.overflow = aberto ? 'hidden' : '';
      if (aberto && input) {
        input.value = '';
        if (motivo) motivo.value = '';
        confirmar.disabled = true;
        input.focus();
      }
    }
    document.addEventListener('click', function (evento) {
      var botao = evento.target.closest('[data-abrir-modal], [data-excluir-vaga]');
      if (!botao) return;
      vagaId = botao.dataset.vagaId || botao.dataset.excluirVaga || vagaId;
      alternar(true);
    });
    if (input && confirmar) input.addEventListener('input', function () {
      confirmar.disabled = input.value.trim().toUpperCase() !== 'DELETAR';
    });
    if (confirmar) confirmar.addEventListener('click', async function () {
      if (!vagaId) {
        alert('Não foi possível identificar a vaga.');
        return;
      }
      if (!motivo || !motivo.value.trim()) {
        alert('Informe o motivo do cancelamento.');
        if (motivo) motivo.focus();
        return;
      }
      confirmar.disabled = true;
      try {
        await api('/vagas/' + encodeURIComponent(vagaId), {
          method: 'DELETE',
          body: { confirmacao: true, motivo: motivo.value.trim() }
        });
        alert('Vaga cancelada com sucesso.');
        window.location.href = 'minhas-vagas.html';
      } catch (erro) {
        alert(erro.message);
        confirmar.disabled = false;
      }
    });
    modal.addEventListener('click', function (evento) {
      if (evento.target === modal) alternar(false);
    });
  }

  function classeStatus(status) {
    if (status === 'ENCERRADA') return 'status--concluida';
    if (status === 'CANCELADA' || status === 'PAUSADA') return 'status--expirada';
    return 'status--selecao';
  }

  function rotuloStatus(status) {
    return { ABERTA: 'Em seleção', PAUSADA: 'Pausada', ENCERRADA: 'Concluída', CANCELADA: 'Cancelada' }[status] || status;
  }

  function htmlCardVaga(vaga, candidaturas) {
    var foto = vaga.fotos && vaga.fotos.length ? vaga.fotos[0] : 'assets/vaga-thumb-1.png';
    var total = candidaturas[vaga.id] || 0;
    var acoesMutaveis = vaga.status === 'CANCELADA' ? '' :
      '<a class="vaga-card__acao" href="editar-vagas.html?id=' + vaga.id + '" title="Editar vaga"><span class="sr-only">Editar vaga</span><img src="assets/botao-acao.svg" alt=""></a>' +
      '<button class="vaga-card__acao" type="button" data-excluir-vaga="' + vaga.id + '" data-vaga-id="' + vaga.id + '" title="Excluir vaga"><span class="sr-only">Excluir vaga</span><img src="assets/botao-acao.svg" alt=""></button>';
    return '<article class="vaga-card" data-status="' + escapar(vaga.status) + '" data-candidaturas="' + total + '" data-prazo="' + escapar(vaga.dataLimiteCandidatura || '') + '">' +
      '<img class="vaga-card__imagem" src="' + escapar(foto) + '" alt="">' +
      '<div class="vaga-card__conteudo"><div class="vaga-card__titulo-linha">' +
      '<h2 class="vaga-card__titulo">' + escapar(vaga.titulo) + '</h2>' +
      '<span class="vaga-card__categoria">' + escapar(vaga.categoria || vaga.tipoContrato) + '</span></div>' +
      '<p class="vaga-card__usuario">@' + escapar(vaga.nomeContratante || 'contratante') + '</p>' +
      '<p class="vaga-card__local">' + escapar(vaga.cidade + ', ' + vaga.estado) + '</p>' +
      '<p class="vaga-card__prazo">Prazo até ' + escapar(dataBrasileira(vaga.dataLimiteCandidatura)) + '</p></div>' +
      '<div class="vaga-card__lateral"><span class="status ' + classeStatus(vaga.status) + '">' + rotuloStatus(vaga.status) + '</span>' +
      '<div class="vaga-card__acoes">' +
      acoesMutaveis +
      '<a class="vaga-card__acao" href="detalhe-vaga-proprietario.html?id=' + vaga.id + '" title="Ver detalhes"><span class="sr-only">Ver detalhes da vaga</span><img src="assets/botao-acao.svg" alt=""></a>' +
      '</div></div></article>';
  }

  async function iniciarMinhasVagas() {
    var lista = document.querySelector('.vagas-lista');
    if (!lista || paginaAtual !== 'minhas-vagas.html') return;
    if (!exigirSessaoContratante()) return;
    try {
      var resultados = await Promise.all([
        api('/vagas/minhas?size=50'),
        api('/candidaturas/minhas-vagas').catch(function () { return []; })
      ]);
      var vagas = resultados[0].content || [];
      var porVaga = {};
      (resultados[1] || []).forEach(function (candidatura) {
        porVaga[candidatura.vagaId] = (porVaga[candidatura.vagaId] || 0) + 1;
      });
      lista.innerHTML = vagas.length
        ? vagas.map(function (vaga) { return htmlCardVaga(vaga, porVaga); }).join('')
        : '<p>Nenhuma vaga cadastrada.</p>';

      var filtros = Array.from(document.querySelectorAll('.filtro'));
      function combina(vaga, indice) {
        var status = vaga.status;
        var total = porVaga[vaga.id] || 0;
        if (indice === 1) return total === 0;
        if (indice === 2) return total > 0;
        if (indice === 3) return status === 'ABERTA';
        if (indice === 4) return status === 'ENCERRADA';
        if (indice === 5) return vaga.dataLimiteCandidatura && vaga.dataLimiteCandidatura < new Date().toISOString().substring(0, 10);
        return true;
      }
      filtros.forEach(function (filtro, indice) {
        var contador = filtro.querySelector('.filtro__contador');
        if (contador) contador.textContent = vagas.filter(function (vaga) { return combina(vaga, indice); }).length;
        filtro.addEventListener('click', function (evento) {
          evento.preventDefault();
          filtros.forEach(function (item) { item.removeAttribute('aria-current'); });
          filtro.setAttribute('aria-current', 'true');
          lista.innerHTML = vagas.filter(function (vaga) { return combina(vaga, indice); })
            .map(function (vaga) { return htmlCardVaga(vaga, porVaga); }).join('') || '<p>Nenhuma vaga neste filtro.</p>';
        });
      });
    } catch (erro) {
      lista.innerHTML = '<p>' + escapar(erro.message) + '</p>';
    }
  }

  async function iniciarDashboard() {
    if (paginaAtual !== 'dashboard-contratante.html') return;
    if (!exigirSessao()) return;
    var raiz = document.querySelector('[data-dashboard]');
    var carregando = document.querySelector('[data-dashboard-carregando]');
    var erroBox = document.querySelector('[data-dashboard-erro]');
    var conteudo = document.querySelector('[data-dashboard-conteudo]');
    var tentar = document.querySelector('[data-dashboard-tentar]');
    var websocket = null;
    var sseAbortController = null;
    var iniciandoFallback = false;

    function elemento(tag, classe, texto) {
      var node = document.createElement(tag);
      if (classe) node.className = classe;
      if (texto != null) node.textContent = texto;
      return node;
    }

    function link(texto, href, classe) {
      var ancora = elemento('a', classe || 'btn-dash btn-dash--secundario', texto);
      ancora.href = href;
      return ancora;
    }

    function limpar(node) {
      while (node && node.firstChild) node.removeChild(node.firstChild);
    }

    function renderModulo(seletor, titulo, estado) {
      var modulo = document.querySelector(seletor);
      limpar(modulo);
      var topo = elemento('div', 'modulo-status__topo');
      topo.appendChild(elemento('strong', '', titulo));
      topo.appendChild(elemento('span', 'modulo-status__selo', estado.disponivel ? 'Disponível' : 'Em breve'));
      modulo.appendChild(topo);
      modulo.appendChild(elemento('p', '', estado.mensagem));
    }

    function linkInternoSeguro(valor) {
      if (!valor || typeof valor !== 'string') return null;
      try {
        var url = new URL(valor, window.location.href);
        var permitidos = [
          '/dashboard-contratante.html',
          '/detalhe-vaga.html',
          '/detalhe-vaga-proprietario.html',
          '/perfil-publico.html',
          '/mensagens.html'
        ];
        return url.origin === window.location.origin && permitidos.indexOf(url.pathname) >= 0
          ? url.pathname.substring(1) + url.search
          : null;
      } catch (erro) {
        return null;
      }
    }

    function mostrarPopup(notificacao) {
      var popup = elemento('div', 'notificacao-popup', notificacao.mensagem);
      popup.setAttribute('role', 'status');
      popup.setAttribute('aria-live', 'polite');
      document.body.appendChild(popup);
      window.setTimeout(function () { popup.remove(); }, 5000);
    }

    async function marcarNotificacao(id) {
      await api('/notificacoes/' + encodeURIComponent(id) + '/lida', { method: 'PATCH' });
      await carregarNotificacoes();
    }

    function renderNotificacoes(pagina, contagem) {
      var modulo = document.querySelector('[data-dashboard-notificacoes]');
      limpar(modulo);
      modulo.classList.add('modulo-status--notificacoes');
      var topo = elemento('div', 'modulo-status__topo');
      topo.appendChild(elemento('strong', '', 'Notificações'));
      topo.appendChild(elemento('span', 'notificacao-badge', String(contagem || 0)));
      modulo.appendChild(topo);

      var acoes = elemento('div', 'notificacao-acoes');
      var marcarTodas = elemento('button', 'notificacao-acao', 'Marcar todas como lidas');
      marcarTodas.type = 'button';
      marcarTodas.disabled = !contagem;
      marcarTodas.addEventListener('click', async function () {
        await api('/notificacoes/lidas', { method: 'PATCH' });
        await carregarNotificacoes();
      });
      acoes.appendChild(marcarTodas);
      modulo.appendChild(acoes);

      var lista = elemento('div', 'notificacao-lista');
      var itens = (pagina && pagina.content) || [];
      if (!itens.length) {
        lista.appendChild(elemento('p', 'notificacao-vazia', 'Nenhuma notificação por enquanto.'));
      }
      itens.slice(0, 5).forEach(function (notificacao) {
        var item = elemento('article', 'notificacao-item' + (notificacao.lida ? '' : ' notificacao-item--nao-lida'));
        var seguro = linkInternoSeguro(notificacao.link);
        var mensagem = seguro
          ? link(notificacao.mensagem, seguro, 'notificacao-item__link')
          : elemento('span', 'notificacao-item__texto', notificacao.mensagem);
        item.appendChild(mensagem);
        if (!notificacao.lida) {
          var marcar = elemento('button', 'notificacao-item__marcar', 'Marcar como lida');
          marcar.type = 'button';
          marcar.addEventListener('click', function () { marcarNotificacao(notificacao.id); });
          item.appendChild(marcar);
        }
        lista.appendChild(item);
      });
      modulo.appendChild(lista);
    }

    async function carregarNotificacoes() {
      var resultados = await Promise.all([
        api('/notificacoes?page=0&size=5'),
        api('/notificacoes/nao-lidas/count')
      ]);
      renderNotificacoes(resultados[0], resultados[1].count);
    }

    function renderMensagens(estado) {
      var modulo = document.querySelector('[data-dashboard-mensagens]');
      limpar(modulo);
      var topo = elemento('div', 'modulo-status__topo');
      topo.appendChild(elemento('strong', '', 'Mensagens'));
      topo.appendChild(elemento('span', 'notificacao-badge', String(estado.quantidadeNaoLidas || 0)));
      modulo.appendChild(topo);
      modulo.appendChild(elemento('p', '', estado.mensagem));
      modulo.appendChild(link('Abrir mensagens', 'mensagens.html', 'notificacao-item__link'));
    }

    async function atualizarMensagens() {
      var contagem = await api('/chat/nao-lidas/count');
      renderMensagens({
        disponivel: true,
        mensagem: 'Converse com seus contatos profissionais com privacidade.',
        quantidadeNaoLidas: contagem.count
      });
    }

    function processarNotificacao(notificacao) {
      if (!notificacao || !notificacao.id) return;
      mostrarPopup(notificacao);
      carregarNotificacoes().catch(function () {});
      if (notificacao.tipo === 'MENSAGEM') atualizarMensagens().catch(function () {});
    }

    function tratarFrameStomp(frame) {
      var separador = frame.indexOf('\n\n');
      var cabecalho = separador >= 0 ? frame.substring(0, separador) : frame;
      var corpo = separador >= 0 ? frame.substring(separador + 2) : '';
      var comando = cabecalho.split('\n')[0];
      if (comando === 'CONNECTED' && websocket) {
        websocket.send('SUBSCRIBE\nid:rf23-notificacoes\ndestination:/user/queue/notificacoes\nack:auto\n\n\0');
      } else if (comando === 'MESSAGE') {
        try { processarNotificacao(JSON.parse(corpo)); } catch (erro) {}
      }
    }

    async function iniciarSse() {
      if (iniciandoFallback || sseAbortController) return;
      iniciandoFallback = true;
      try {
        var sessao = lerSessao();
        if (!sessao || !sessao.token) return;
        sseAbortController = new AbortController();
        var resposta = await fetch(API_BASE + '/notificacoes/stream', {
          headers: { Accept: 'text/event-stream', Authorization: 'Bearer ' + sessao.token },
          signal: sseAbortController.signal
        });
        if (resposta.status === 401) {
          var renovado = await renovarToken();
          if (renovado) {
            resposta = await fetch(API_BASE + '/notificacoes/stream', {
              headers: { Accept: 'text/event-stream', Authorization: 'Bearer ' + renovado },
              signal: sseAbortController.signal
            });
          }
        }
        if (!resposta.ok || !resposta.body) throw new Error('SSE indisponível.');
        var leitor = resposta.body.getReader();
        var decoder = new TextDecoder();
        var buffer = '';
        while (true) {
          var pedaco = await leitor.read();
          if (pedaco.done) break;
          buffer += decoder.decode(pedaco.value, { stream: true }).replace(/\r/g, '');
          var limite;
          while ((limite = buffer.indexOf('\n\n')) >= 0) {
            var evento = buffer.substring(0, limite);
            buffer = buffer.substring(limite + 2);
            var dados = evento.split('\n').filter(function (linha) {
              return linha.indexOf('data:') === 0;
            }).map(function (linha) { return linha.substring(5).trim(); }).join('\n');
            if (dados && dados !== 'ok') {
              try { processarNotificacao(JSON.parse(dados)); } catch (erro) {}
            }
          }
        }
      } catch (erro) {
        if (erro.name !== 'AbortError') window.setTimeout(iniciarSse, 3000);
      } finally {
        sseAbortController = null;
        iniciandoFallback = false;
      }
    }

    function iniciarWebSocket() {
      var sessao = lerSessao();
      if (!sessao || !sessao.token || !window.WebSocket) {
        iniciarSse();
        return;
      }
      var wsBase = API_BASE.replace(/^http/, 'ws').replace(/\/api$/, '');
      websocket = new WebSocket(wsBase + '/ws');
      websocket.onopen = function () {
        websocket.send(
          'CONNECT\naccept-version:1.2\nhost:' + window.location.host
          + '\nAuthorization:Bearer ' + lerSessao().token
          + '\nheart-beat:10000,10000\n\n\0'
        );
      };
      websocket.onmessage = function (evento) {
        String(evento.data).split('\0').filter(Boolean).forEach(tratarFrameStomp);
      };
      websocket.onerror = function () { if (websocket) websocket.close(); };
      websocket.onclose = function () { iniciarSse(); };
    }

    function tagsNoCard(card, tags, quantidade) {
      var area = elemento('div', 'dashboard-card__tags');
      area.appendChild(elemento('span', 'dashboard-card__match', quantidade + ' área(s) em comum'));
      (tags || []).slice(0, 4).forEach(function (tag) {
        area.appendChild(elemento('span', 'dashboard-card__tag', tag.nome));
      });
      card.appendChild(area);
    }

    function painel(titulo, descricao, secao, larguraTotal) {
      var bloco = elemento('section', 'painel' + (larguraTotal ? ' painel--largo' : ''));
      var topo = elemento('div', 'painel__topo');
      topo.appendChild(elemento('h2', 'painel__titulo', titulo));
      topo.appendChild(elemento('p', 'painel__descricao', descricao));
      bloco.appendChild(topo);
      var lista = elemento('div', 'painel__lista');
      lista.dataset.total = String(secao.totalElements || 0);
      lista.dataset.hasMore = String(Boolean(secao.hasMore));
      bloco.appendChild(lista);
      return { bloco: bloco, lista: lista };
    }

    function vazio(lista, mensagem) {
      lista.appendChild(elemento('p', 'dashboard-vazio', mensagem));
    }

    function renderVagas(secao) {
      var estrutura = painel(
        'Vagas recomendadas',
        'Oportunidades abertas ordenadas pelas áreas do seu perfil.',
        secao,
        true
      );
      if (!(secao.content || []).length) {
        vazio(estrutura.lista, 'Nenhuma vaga compatível no momento. Adicione áreas ao perfil para receber recomendações.');
      }
      (secao.content || []).forEach(function (vaga) {
        var card = elemento('article', 'dashboard-card');
        var dados = elemento('div', 'dashboard-card__conteudo');
        dados.appendChild(elemento('h3', 'dashboard-card__titulo', vaga.titulo));
        dados.appendChild(elemento(
          'p',
          'dashboard-card__texto',
          vaga.nomeContratante + ' · ' + vaga.cidade + '/' + vaga.estado + ' · ' + moeda(vaga.remuneraValor)
        ));
        tagsNoCard(dados, vaga.tags, vaga.quantidadeTagsCoincidentes);
        card.appendChild(dados);
        card.appendChild(link('Ver vaga', 'detalhe-vaga.html?id=' + encodeURIComponent(vaga.id), 'dashboard-card__link'));
        estrutura.lista.appendChild(card);
      });
      return estrutura.bloco;
    }

    function renderCandidaturas(secao) {
      var estrutura = painel(
        'Candidaturas recentes',
        'Somente inscrições recebidas nas suas vagas abertas ou pausadas.',
        secao,
        false
      );
      if (!(secao.content || []).length) vazio(estrutura.lista, 'Nenhuma candidatura recente em vaga ativa.');
      (secao.content || []).forEach(function (candidatura) {
        var card = elemento('article', 'dashboard-card');
        var avatar = elemento('img', 'dashboard-card__avatar');
        avatar.src = candidatura.avatarUrl;
        avatar.alt = '';
        var dados = elemento('div', 'dashboard-card__conteudo');
        dados.appendChild(elemento('h3', 'dashboard-card__titulo', candidatura.nomeArtista));
        dados.appendChild(elemento(
          'p',
          'dashboard-card__texto',
          candidatura.tituloVaga + ' · ' + String(candidatura.status).replace(/_/g, ' ')
        ));
        card.appendChild(avatar);
        card.appendChild(dados);
        card.appendChild(link(
          'Ver perfil',
          'perfil-publico.html?tipo=ARTISTA&id=' + encodeURIComponent(candidatura.artistaId),
          'dashboard-card__link'
        ));
        estrutura.lista.appendChild(card);
      });
      return estrutura.bloco;
    }

    function renderTalentos(secao) {
      var estrutura = painel(
        'Talentos sugeridos',
        'Artistas adultos com perfil completo e áreas compatíveis com suas vagas ativas.',
        secao,
        false
      );
      if (!(secao.content || []).length) vazio(estrutura.lista, 'Nenhum talento compatível no momento.');
      (secao.content || []).forEach(function (talento) {
        var card = elemento('article', 'dashboard-card');
        var avatar = elemento('img', 'dashboard-card__avatar');
        avatar.src = talento.avatarUrl;
        avatar.alt = '';
        var dados = elemento('div', 'dashboard-card__conteudo');
        dados.appendChild(elemento('h3', 'dashboard-card__titulo', talento.nomeExibicao));
        dados.appendChild(elemento('p', 'dashboard-card__texto', talento.localizacao || 'Localização não informada'));
        tagsNoCard(dados, talento.tags, talento.quantidadeTagsCoincidentes);
        card.appendChild(avatar);
        card.appendChild(dados);
        card.appendChild(link(
          'Ver perfil',
          'perfil-publico.html?tipo=ARTISTA&id=' + encodeURIComponent(talento.artistaId),
          'dashboard-card__link'
        ));
        estrutura.lista.appendChild(card);
      });
      return estrutura.bloco;
    }

    function renderMenu(tipo) {
      var menu = document.querySelector('[data-menu-contextual]');
      limpar(menu);
      var itens = tipo === 'CONTRATANTE'
        ? [['Minhas vagas', 'minhas-vagas.html'], ['Publicar vaga', 'publicar-vaga.html']]
        : [['Meu perfil', 'perfil.html']];
      itens.forEach(function (item) {
        var li = elemento('li');
        li.appendChild(link(item[0], item[1], 'navbar__link'));
        menu.appendChild(li);
      });
    }

    function renderDashboard(dados) {
      var artista = dados.tipoUsuario === 'ARTISTA';
      document.querySelector('[data-dashboard-tipo]').textContent = artista ? 'PAINEL DO ARTISTA' : 'PAINEL DO CONTRATANTE';
      document.querySelector('[data-dashboard-titulo]').textContent = artista ? 'Suas oportunidades' : 'Sua produção';
      document.querySelector('[data-dashboard-subtitulo]').textContent = artista
        ? 'Descubra vagas alinhadas ao seu perfil profissional.'
        : 'Acompanhe candidaturas e encontre pessoas para seus projetos.';
      document.querySelector('[data-dashboard-saudacao]').textContent = 'Olá, ' + dados.nomeExibicao;
      document.querySelector('[data-dashboard-resumo]').textContent = artista
        ? 'Estas recomendações consideram apenas vagas abertas com áreas em comum com o seu perfil.'
        : 'As candidaturas pertencem às suas vagas ativas; os talentos usam as áreas dessas vagas como contexto.';
      var avatar = document.querySelector('[data-dashboard-avatar]');
      avatar.src = dados.avatarUrl;
      avatar.alt = 'Avatar de ' + dados.nomeExibicao;
      document.querySelector('[data-dashboard-perfil-incompleto]').hidden = !artista || dados.perfilCompleto;
      if (!dados.notificacoes.disponivel) {
        renderModulo('[data-dashboard-notificacoes]', 'Notificações', dados.notificacoes);
      }
      if (dados.mensagens.disponivel) renderMensagens(dados.mensagens);
      else renderModulo('[data-dashboard-mensagens]', 'Mensagens', dados.mensagens);
      renderMenu(dados.tipoUsuario);

      var acoes = document.querySelector('[data-dashboard-acoes]');
      limpar(acoes);
      if (artista) {
        acoes.appendChild(link('Editar perfil', 'perfil.html', 'btn-dash btn-dash--primario'));
      } else {
        acoes.appendChild(link('Minhas vagas', 'minhas-vagas.html'));
        acoes.appendChild(link('Publicar vaga', 'publicar-vaga.html', 'btn-dash btn-dash--primario'));
      }

      var secoes = document.querySelector('[data-dashboard-secoes]');
      limpar(secoes);
      if (artista) {
        secoes.appendChild(renderVagas(dados.vagasRecomendadas));
      } else {
        secoes.appendChild(renderCandidaturas(dados.candidaturasRecentes));
        secoes.appendChild(renderTalentos(dados.talentosSugeridos));
      }
    }

    async function carregar() {
      raiz.setAttribute('aria-busy', 'true');
      carregando.hidden = false;
      erroBox.hidden = true;
      conteudo.hidden = true;
      try {
        var dados = await api('/dashboard?size=5');
        renderDashboard(dados);
        await carregarNotificacoes();
        iniciarWebSocket();
        carregando.hidden = true;
        conteudo.hidden = false;
      } catch (erro) {
        carregando.hidden = true;
        erroBox.hidden = false;
        document.querySelector('[data-dashboard-erro-texto]').textContent = erro.message;
      } finally {
        raiz.setAttribute('aria-busy', 'false');
      }
    }

    if (tentar) tentar.addEventListener('click', carregar);
    await carregar();
  }

  function preencherRequisitos(vaga) {
    var listas = document.querySelectorAll('.requisitos__lista');
    if (!listas.length) return;
    listas.forEach(function (lista) { lista.innerHTML = ''; });
    String(vaga.requisitos || '').split(/[;,]/).map(function (item) { return item.trim(); })
      .filter(Boolean).forEach(function (item) {
        listas[0].insertAdjacentHTML('beforeend', '<span class="requisitos__tag">' + escapar(item) + '</span>');
      });
  }

  async function iniciarDetalheVaga() {
    if (paginaAtual !== 'detalhe-vaga-proprietario.html' && paginaAtual !== 'confirmar-exclusao-vaga.html') return;
    if (!exigirSessaoContratante()) return;
    var id = new URLSearchParams(window.location.search).get('id');
    if (!id) return;
    try {
      var vaga = await api('/vagas/' + encodeURIComponent(id));
      document.title = vaga.titulo + ' — Palco';
      var titulo = document.querySelector('.vaga-detalhe__titulo');
      if (titulo) titulo.textContent = vaga.titulo;
      var categoria = document.querySelector('.vaga-card__categoria');
      if (categoria) categoria.textContent = vaga.categoria || vaga.tipoContrato;
      var area = document.querySelector('.tag-area');
      if (area) area.textContent = vaga.tipoContrato;
      var usuario = document.querySelector('.vaga-detalhe__usuario');
      if (usuario) usuario.textContent = '@' + (vaga.nomeContratante || 'contratante');
      var local = document.querySelector('.vaga-detalhe__local');
      if (local) local.textContent = vaga.cidade + ', ' + vaga.estado;
      var prazo = document.querySelector('.vaga-detalhe__prazo');
      if (prazo) prazo.textContent = 'Prazo até ' + dataBrasileira(vaga.dataLimiteCandidatura);
      var descricao = document.querySelector('.vaga-detalhe__descricao');
      if (descricao) descricao.textContent = vaga.descricao;
      var celulas = document.querySelectorAll('.vaga-detalhe__tabela tbody td');
      var valores = [vaga.tipoContrato, vaga.modeloTrabalho, vaga.formaPagamento, moeda(vaga.remuneraValor), vaga.experiencia || 'Não informado'];
      celulas.forEach(function (celula, indice) { if (valores[indice] != null) celula.textContent = valores[indice]; });
      preencherRequisitos(vaga);
      var editar = document.querySelector('a[title="Editar vaga"]');
      if (editar) {
        editar.href = 'editar-vagas.html?id=' + vaga.id;
        editar.hidden = vaga.status === 'CANCELADA';
      }
      document.querySelectorAll('[data-abrir-modal]').forEach(function (botao) {
        botao.dataset.vagaId = vaga.id;
        botao.hidden = vaga.status === 'CANCELADA';
      });

      var relacionadas = await api('/vagas?size=4').catch(function () { return { content: [] }; });
      var lateral = document.querySelector('.vagas-relacionadas');
      if (lateral) lateral.innerHTML = (relacionadas.content || []).filter(function (item) { return item.id !== vaga.id; }).slice(0, 3).map(function (item) {
        return '<article class="vaga-mini"><div class="vaga-mini__cabecalho"><h2 class="vaga-mini__titulo">' + escapar(item.titulo) + '</h2>' +
          '<span class="vaga-mini__categoria">' + escapar(item.categoria || item.tipoContrato) + '</span></div>' +
          '<p class="vaga-mini__local">' + escapar(item.cidade + ', ' + item.estado) + '</p>' +
          '<p class="vaga-mini__prazo">Prazo até ' + escapar(dataBrasileira(item.dataLimiteCandidatura)) + '</p>' +
          '<p class="vaga-mini__resumo">' + escapar(item.descricao).substring(0, 100) + '</p>' +
          '<a class="vaga-mini__cta" href="detalhe-vaga-proprietario.html?id=' + item.id + '">VER VAGA <span aria-hidden="true">&rarr;</span></a></article>';
      }).join('');
    } catch (erro) {
      alert(erro.message);
    }
  }

  async function iniciarDetalheVagaPublica() {
    if (paginaAtual !== 'detalhe-vaga.html') return;
    if (!exigirSessao()) return;
    var id = new URLSearchParams(window.location.search).get('id');
    var carregando = document.querySelector('[data-vaga-carregando]');
    var erro = document.querySelector('[data-vaga-erro]');
    var conteudo = document.querySelector('[data-vaga-conteudo]');
    if (!id || !/^\d+$/.test(id)) {
      carregando.hidden = true;
      erro.hidden = false;
      document.querySelector('[data-vaga-erro-texto]').textContent = 'Identificador de vaga inválido.';
      return;
    }
    try {
      var vaga = await api('/vagas/' + encodeURIComponent(id));
      document.title = vaga.titulo + ' — Palco';
      document.querySelector('[data-vaga-status]').textContent = rotuloStatus(vaga.status);
      document.querySelector('[data-vaga-titulo]').textContent = vaga.titulo;
      document.querySelector('[data-vaga-empresa]').textContent = vaga.nomeContratante;
      document.querySelector('[data-vaga-resumo]').textContent =
        vaga.cidade + '/' + vaga.estado + ' · ' + vaga.modeloTrabalho + ' · ' + moeda(vaga.remuneraValor);
      document.querySelector('[data-vaga-descricao]').textContent = vaga.descricao;
      document.querySelector('[data-vaga-requisitos]').textContent = vaga.requisitos;
      var tags = document.querySelector('[data-vaga-tags]');
      (vaga.tagIds || []).forEach(function (tagId) {
        var tag = document.createElement('span');
        tag.className = 'dashboard-card__tag';
        tag.textContent = 'Área #' + tagId;
        tags.appendChild(tag);
      });
      carregando.hidden = true;
      conteudo.hidden = false;
    } catch (falha) {
      carregando.hidden = true;
      erro.hidden = false;
      document.querySelector('[data-vaga-erro-texto]').textContent = falha.message;
    }
  }

  function requisitosDaEdicao(form) {
    return Array.from(form.querySelectorAll('.requisitos__tag')).map(function (tag) {
      var clone = tag.cloneNode(true);
      clone.querySelectorAll('button').forEach(function (botao) { botao.remove(); });
      return clone.textContent.trim();
    }).filter(Boolean).join(', ');
  }

  async function iniciarEdicaoVaga() {
    var form = document.getElementById('form-editar-vaga');
    if (!form) return;
    if (!exigirSessaoContratante()) return;
    var id = new URLSearchParams(window.location.search).get('id');
    if (!id) {
      alert('Informe a vaga que será editada.');
      window.location.href = 'minhas-vagas.html';
      return;
    }
    try {
      var vaga = await api('/vagas/' + encodeURIComponent(id));
      form.dataset.vagaOriginal = JSON.stringify(vaga);
      form.elements.titulo.value = vaga.titulo;
      form.elements.responsavel.value = '@' + (vaga.nomeContratante || 'contratante');
      form.elements.localizacao.value = vaga.cidade + ', ' + vaga.estado;
      form.elements.dataLimiteCandidatura.value = vaga.dataLimiteCandidatura ? 'Prazo até ' + dataBrasileira(vaga.dataLimiteCandidatura) : '';
      form.elements.descricao.value = vaga.descricao;
      form.elements.tipoContrato.value = vaga.tipoContrato;
      form.elements.modeloTrabalho.value = vaga.modeloTrabalho;
      form.elements.formaPagamento.value = vaga.formaPagamento;
      form.elements.remuneraValor.value = moeda(vaga.remuneraValor);
      form.elements.experiencia.value = vaga.experiencia || '';
      var categoria = form.querySelector('.vaga-card__categoria');
      if (categoria) categoria.textContent = vaga.categoria || vaga.tipoContrato;
      preencherRequisitos(vaga);
      var listaRequisitos = form.querySelector('.requisitos__lista');
      if (listaRequisitos) {
        listaRequisitos.querySelectorAll('.requisitos__tag').forEach(function (tag) {
          tag.classList.add('requisitos__tag--editavel');
          tag.insertAdjacentHTML('beforeend', '<button class="requisitos__remover" type="button" aria-label="Remover requisito">&times;</button>');
        });
        listaRequisitos.insertAdjacentHTML('beforeend', '<button class="requisitos__adicionar" type="button" data-adicionar-requisito>Adicionar</button>');
        listaRequisitos.addEventListener('click', function (evento) {
          var remover = evento.target.closest('.requisitos__remover');
          if (remover) remover.closest('.requisitos__tag').remove();
          if (evento.target.closest('[data-adicionar-requisito]')) {
            var novo = window.prompt('Informe o novo requisito:');
            if (novo && novo.trim()) {
              evento.target.closest('[data-adicionar-requisito]').insertAdjacentHTML('beforebegin',
                '<span class="requisitos__tag requisitos__tag--editavel">' + escapar(novo.trim()) +
                '<button class="requisitos__remover" type="button" aria-label="Remover requisito">&times;</button></span>');
            }
          }
        });
      }
    } catch (erro) {
      alert(erro.message);
      return;
    }

    form.addEventListener('submit', function (evento) {
      evento.preventDefault();
      var original = JSON.parse(form.dataset.vagaOriginal);
      var local = form.elements.localizacao.value.split(',');
      var estado = local.length > 1 ? local.pop().trim().toUpperCase() : original.estado;
      var cidade = local.join(',').trim() || original.cidade;
      var rascunho = payloadDaVaga(original);
      rascunho.titulo = form.elements.titulo.value.trim();
      rascunho.descricao = form.elements.descricao.value.trim();
      rascunho.requisitos = requisitosDaEdicao(form) || original.requisitos;
      rascunho.remuneraValor = numeroMonetario(form.elements.remuneraValor.value);
      rascunho.formaPagamento = form.elements.formaPagamento.value.trim();
      rascunho.cidade = cidade;
      rascunho.estado = estado.substring(0, 2);
      rascunho.modeloTrabalho = normalizarModelo(form.elements.modeloTrabalho.value);
      rascunho.tipoContrato = form.elements.tipoContrato.value.trim();
      rascunho.experiencia = form.elements.experiencia.value.trim() || null;
      rascunho.dataLimiteCandidatura = dataIsoDeTexto(form.elements.dataLimiteCandidatura.value);
      sessionStorage.setItem(CHAVE_RASCUNHO_VAGA, JSON.stringify({ id: Number(id), dados: rascunho }));
      window.location.href = 'editar-vagas-2.html?id=' + id;
    });
  }

  async function iniciarEdicaoMidia() {
    var form = document.getElementById('form-editar-midia');
    if (!form) return;
    if (!exigirSessaoContratante()) return;
    var id = new URLSearchParams(window.location.search).get('id');
    if (!id) return;
    var vaga;
    try {
      vaga = await api('/vagas/' + encodeURIComponent(id));
      form.elements.endereco.value = vaga.enderecoCompleto || '';
      form.elements.abrangencia.value = vaga.abrangencia || 'regional';
      var fotos = form.querySelectorAll('[data-vaga-foto]');
      fotos.forEach(function (foto, indice) {
        if (vaga.fotos && vaga.fotos[indice]) foto.src = vaga.fotos[indice];
        function editarFoto() {
          var novaUrl = window.prompt('Informe a URL da foto:', foto.getAttribute('src'));
          if (novaUrl && novaUrl.trim()) foto.src = novaUrl.trim();
        }
        foto.addEventListener('click', editarFoto);
        foto.addEventListener('keydown', function (evento) { if (evento.key === 'Enter') editarFoto(); });
      });
    } catch (erro) {
      alert(erro.message);
      return;
    }
    form.addEventListener('submit', async function (evento) {
      evento.preventDefault();
      var salvo = sessionStorage.getItem(CHAVE_RASCUNHO_VAGA);
      var rascunho = salvo ? JSON.parse(salvo) : null;
      var payload = rascunho && String(rascunho.id) === String(id) ? rascunho.dados : payloadDaVaga(vaga);
      payload.enderecoCompleto = form.elements.endereco.value.trim() || null;
      payload.abrangencia = form.elements.abrangencia.value;
      payload.fotos = Array.from(form.querySelectorAll('[data-vaga-foto]')).map(function (foto) {
        return foto.getAttribute('src');
      }).filter(Boolean);
      try {
        await api('/vagas/' + encodeURIComponent(id), { method: 'PUT', body: payload });
        sessionStorage.removeItem(CHAVE_RASCUNHO_VAGA);
        alert('Vaga atualizada com sucesso.');
        window.location.href = 'detalhe-vaga-proprietario.html?id=' + id;
      } catch (erro) {
        alert(erro.message);
      }
    });
  }

  async function carregarTags(container) {
    if (!container) return;
    try {
      var tags = await api('/tags');
      container.innerHTML = tags.map(function (tag) {
        return '<label class="requisitos__tag"><input type="checkbox" name="tagId" value="' + tag.id + '"> ' + escapar(tag.nome) + '</label>';
      }).join('') || '<span>Nenhuma tag cadastrada.</span>';
    } catch (erro) {
      container.innerHTML = '<span>' + escapar(erro.message) + '</span>';
    }
  }

  function iniciarPublicacaoVaga() {
    var form = document.getElementById('form-publicar-vaga');
    if (!form) return;
    var sessao = exigirSessaoContratante();
    if (!sessao) return;
    carregarTags(form.querySelector('[data-tags-vaga]'));
    form.addEventListener('submit', async function (evento) {
      evento.preventDefault();
      if (!form.reportValidity()) return;
      var dados = new FormData(form);
      var payload = {
        contratanteId: sessao.id,
        titulo: dados.get('titulo').trim(),
        descricao: dados.get('descricao').trim(),
        requisitos: dados.get('requisitos').trim(),
        remuneraValor: Number(dados.get('remuneraValor')),
        formaPagamento: dados.get('formaPagamento').trim(),
        cidade: dados.get('cidade').trim(),
        estado: dados.get('estado').trim().toUpperCase(),
        enderecoCompleto: dados.get('enderecoCompleto').trim() || null,
        beneficios: dados.get('beneficios').trim() || null,
        modeloTrabalho: dados.get('modeloTrabalho'),
        tipoContrato: dados.get('tipoContrato').trim(),
        tagIds: dados.getAll('tagId').map(Number),
        categoria: dados.get('categoria').trim() || null,
        experiencia: dados.get('experiencia').trim() || null,
        dataLimiteCandidatura: dados.get('dataLimiteCandidatura') || null,
        abrangencia: dados.get('abrangencia') || null,
        fotos: dados.getAll('foto').map(function (url) { return url.trim(); }).filter(Boolean)
      };
      try {
        var vaga = await api('/vagas', { method: 'POST', body: payload });
        alert('Vaga publicada com sucesso.');
        window.location.href = 'detalhe-vaga-proprietario.html?id=' + vaga.id;
      } catch (erro) {
        alert(erro.message);
      }
    });
  }

  async function iniciarPerfil() {
    var form = document.getElementById('form-perfil');
    if (!form) return;
    var sessao = lerSessao();
    if (!sessao) {
      window.location.href = 'login.html';
      return;
    }
    var usuario;
    var perfil;
    var contratante = sessao.tipoUsuario === 'CONTRATANTE';
    try {
      usuario = await api('/usuarios/me');
      perfil = await api(contratante ? '/perfis-contratantes/' + usuario.id : '/perfis-artistas/' + usuario.id);
      form.elements.nome.value = usuario.nome || '';
      form.elements.dataNascimento.value = usuario.dataNascimento || '';
      form.elements.telefone.value = usuario.telefone || '';
      form.elements.email.value = usuario.email || '';
      form.elements.biografia.value = perfil.biografia || '';
      form.elements.localizacao.value = perfil.localizacao || '';
      form.elements.bannerUrl.value = perfil.bannerUrl || '';
      if (contratante) {
        form.elements.nomeEmpresa.value = perfil.nomeEmpresa || '';
        form.elements.tipoPerfil.value = perfil.tipoPerfil || '';
        document.querySelectorAll('[data-campo-artista]').forEach(function (campo) { campo.hidden = true; });
      } else {
        document.querySelector('[data-campo-empresa]').hidden = true;
        document.querySelector('[data-campo-tipo-perfil]').hidden = true;
        form.elements.urlPortfolio.value = perfil.urlPortfolio || '';
        var tagsDisponiveis = await api('/tags');
        var tagsAtuais = new Set((perfil.tagIds || []).map(Number));
        form.elements.tagIds.innerHTML = tagsDisponiveis.map(function (tag) {
          return '<option value="' + tag.id + '"' + (tagsAtuais.has(Number(tag.id)) ? ' selected' : '') + '>' +
            escapar(tag.nome) + '</option>';
        }).join('');
      }
    } catch (erro) {
      alert(erro.message);
      return;
    }
    form.addEventListener('submit', async function (evento) {
      evento.preventDefault();
      if (!form.reportValidity()) return;
      try {
        var perfilPayload = contratante ? {
          usuarioId: usuario.id,
          nomeEmpresa: form.elements.nomeEmpresa.value.trim() || null,
          tipoPerfil: form.elements.tipoPerfil.value.trim() || null,
          biografia: form.elements.biografia.value.trim(),
          localizacao: form.elements.localizacao.value.trim(),
          bannerUrl: form.elements.bannerUrl.value.trim() || null
        } : {
          usuarioId: usuario.id,
          biografia: form.elements.biografia.value.trim(),
          localizacao: form.elements.localizacao.value.trim(),
          urlPortfolio: form.elements.urlPortfolio.value.trim() || null,
          bannerUrl: form.elements.bannerUrl.value.trim() || null,
          tagIds: Array.from(form.elements.tagIds.selectedOptions).map(function (opcao) { return Number(opcao.value); })
        };
        await api((contratante ? '/perfis-contratantes/' : '/perfis-artistas/') + usuario.id, {
          method: 'PUT', body: perfilPayload
        });
        var atualizado = await api('/usuarios/me', {
          method: 'PUT',
          body: {
            nome: form.elements.nome.value.trim(),
            dataNascimento: form.elements.dataNascimento.value,
            telefone: form.elements.telefone.value.trim(),
            email: form.elements.email.value.trim(),
            novaSenha: form.elements.novaSenha.value || null,
            senhaAtual: form.elements.senhaAtual.value || null
          }
        });
        var emailAlterado = atualizado.email !== sessao.email;
        sessao.nome = atualizado.nome;
        sessao.email = atualizado.email;
        salvarSessao(sessao);
        form.elements.novaSenha.value = '';
        form.elements.senhaAtual.value = '';
        alert(emailAlterado
          ? 'Perfil atualizado. Faça login novamente com o novo e-mail.'
          : 'Perfil atualizado com sucesso.');
        if (emailAlterado) {
          limparSessao();
          window.location.href = 'login.html';
        }
      } catch (erro) {
        alert(erro.message);
      }
    });
    var excluir = form.querySelector('[data-excluir-conta]');
    excluir.addEventListener('click', async function () {
      if (!window.confirm('Tem certeza que deseja excluir sua conta? Esta ação não pode ser desfeita.')) return;
      excluir.disabled = true;
      try {
        await api('/usuarios/me', { method: 'DELETE' });
        limparSessao();
        window.location.href = 'login.html';
      } catch (erro) {
        excluir.disabled = false;
        alert(erro.message);
      }
    });
  }

  function iniciarLogout() {
    document.querySelectorAll('[data-logout]').forEach(function (botao) {
      botao.addEventListener('click', async function () {
        var sessao = lerSessao();
        try {
          if (sessao && sessao.refreshToken) {
            await api('/auth/logout', { method: 'POST', body: { refreshToken: sessao.refreshToken } });
          }
        } catch (erro) {
          console.warn('Não foi possível invalidar o refresh token.', erro);
        }
        limparSessao();
        window.location.href = 'login.html';
      });
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    aplicarDadosDaSessao();
    iniciarDrawer();
    iniciarModalExclusao();
    iniciarBotoesDeSenha();
    iniciarFormularioLogin();
    iniciarDataDeNascimento();
    iniciarSeletores();
    iniciarFormularioCadastro();
    iniciarMinhasVagas();
    iniciarDashboard();
    iniciarDetalheVaga();
    iniciarDetalheVagaPublica();
    iniciarEdicaoVaga();
    iniciarEdicaoMidia();
    iniciarPublicacaoVaga();
    iniciarPerfil();
    iniciarLogout();
  });
})();

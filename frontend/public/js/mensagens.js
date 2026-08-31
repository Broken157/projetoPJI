(function () {
  'use strict';

  var API_BASE = (window.PALCO_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');
  var CHAVE_SESSAO = 'palco.sessao';
  var sessao = lerSessao();
  var salaAtual = null;
  var salas = [];
  var websocket = null;
  var stompConectado = false;

  function lerSessao() {
    var bruto = sessionStorage.getItem(CHAVE_SESSAO) || localStorage.getItem(CHAVE_SESSAO);
    if (!bruto) return null;
    try { return JSON.parse(bruto); } catch (erro) { return null; }
  }

  function salvarSessao(dados) {
    sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(dados));
    sessao = dados;
  }

  async function renovarToken() {
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
    if (config.body && typeof config.body !== 'string') {
      headers['Content-Type'] = 'application/json';
      config.body = JSON.stringify(config.body);
    }
    headers.Authorization = 'Bearer ' + sessao.token;
    config.headers = headers;
    var resposta = await fetch(API_BASE + caminho, config);
    if (resposta.status === 401 && !config._renovada) {
      var token = await renovarToken();
      if (token) {
        config._renovada = true;
        config.headers.Authorization = 'Bearer ' + token;
        resposta = await fetch(API_BASE + caminho, config);
      }
    }
    var tipo = resposta.headers.get('content-type') || '';
    var corpo = tipo.indexOf('application/json') >= 0 ? await resposta.json() : null;
    if (!resposta.ok) throw new Error(corpo && corpo.mensagem ? corpo.mensagem : 'Falha HTTP ' + resposta.status + '.');
    return corpo;
  }

  function elemento(tag, classe, texto) {
    var node = document.createElement(tag);
    if (classe) node.className = classe;
    if (texto != null) node.textContent = texto;
    return node;
  }

  function limpar(node) {
    while (node && node.firstChild) node.removeChild(node.firstChild);
  }

  function avatarSeguro(valor, id) {
    if (valor && typeof valor === 'string' && !/^javascript:/i.test(valor)) return valor;
    return 'assets/avatar-perfil.png';
  }

  function dataHora(valor) {
    if (!valor) return '';
    var data = new Date(valor);
    return Number.isNaN(data.getTime()) ? '' : data.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
  }

  function podeEditar(mensagem) {
    if (!mensagem || mensagem.excluida || Number(mensagem.remetenteId) !== Number(sessao.id)) return false;
    var envio = new Date(mensagem.dataEnvio).getTime();
    return Number.isFinite(envio) && Date.now() - envio <= 15 * 60 * 1000;
  }

  function mostrarErro(erro) {
    var caixa = document.querySelector('[data-chat-erro]');
    caixa.textContent = erro.message || String(erro);
    caixa.hidden = false;
  }

  function renderSalas() {
    var lista = document.querySelector('[data-chat-salas]');
    limpar(lista);
    var total = salas.reduce(function (soma, sala) { return soma + Number(sala.naoLidas || 0); }, 0);
    document.querySelector('[data-chat-total-nao-lidas]').textContent = String(total);
    if (!salas.length) {
      lista.appendChild(elemento('p', 'conversas__vazia', 'Nenhuma conversa iniciada.'));
      return;
    }
    salas.forEach(function (sala) {
      var botao = elemento('button', 'conversa-item' + (Number(sala.salaId) === Number(salaAtual) ? ' conversa-item--ativa' : ''));
      botao.type = 'button';
      botao.dataset.salaId = String(sala.salaId);
      var avatar = elemento('img', 'conversa-item__avatar');
      avatar.src = avatarSeguro(sala.participanteAvatar, sala.participanteId);
      avatar.alt = '';
      var texto = elemento('span');
      texto.appendChild(elemento('strong', 'conversa-item__nome', sala.participanteNome));
      texto.appendChild(elemento('small', 'conversa-item__preview', sala.ultimaMensagem || 'Conversa iniciada'));
      botao.appendChild(avatar);
      botao.appendChild(texto);
      if (sala.naoLidas) botao.appendChild(elemento('span', 'conversa-item__badge', String(sala.naoLidas)));
      botao.addEventListener('click', function () { abrirSala(sala.salaId); });
      lista.appendChild(botao);
    });
  }

  function iniciarEdicao(cartao, mensagem) {
    limpar(cartao);
    var form = elemento('form', 'mensagem__edicao');
    var campo = elemento('textarea');
    campo.maxLength = 4000;
    campo.required = true;
    campo.value = mensagem.texto;
    var acoes = elemento('div', 'mensagem__edicao-acoes');
    var salvar = elemento('button', '', 'Salvar');
    salvar.type = 'submit';
    var cancelar = elemento('button', '', 'Cancelar');
    cancelar.type = 'button';
    cancelar.addEventListener('click', carregarHistorico);
    acoes.appendChild(salvar);
    acoes.appendChild(cancelar);
    form.appendChild(campo);
    form.appendChild(acoes);
    form.addEventListener('submit', async function (evento) {
      evento.preventDefault();
      try {
        await api('/chat/mensagens/' + encodeURIComponent(mensagem.id), {
          method: 'PATCH', body: { texto: campo.value }
        });
        await carregarHistorico();
      } catch (erro) { mostrarErro(erro); }
    });
    cartao.appendChild(form);
    campo.focus();
  }

  function renderMensagens(pagina) {
    var historico = document.querySelector('[data-chat-mensagens]');
    limpar(historico);
    var mensagens = ((pagina && pagina.content) || []).slice().reverse();
    if (!mensagens.length) {
      historico.appendChild(elemento('p', 'conversas__vazia', 'Ainda não há mensagens nesta conversa.'));
    }
    mensagens.forEach(function (mensagem) {
      var propria = Number(mensagem.remetenteId) === Number(sessao.id);
      var cartao = elemento('article', 'mensagem' + (propria ? ' mensagem--propria' : '') + (mensagem.excluida ? ' mensagem--excluida' : ''));
      cartao.dataset.mensagemId = String(mensagem.id);
      cartao.appendChild(elemento('p', 'mensagem__texto', mensagem.texto));
      var meta = elemento('footer', 'mensagem__meta');
      meta.appendChild(elemento('time', '', dataHora(mensagem.dataEnvio)));
      if (propria) meta.appendChild(elemento('span', '', mensagem.lida ? 'Lida' : 'Enviada'));
      if (propria && !mensagem.excluida) {
        var acoes = elemento('span', 'mensagem__acoes');
        if (podeEditar(mensagem)) {
          var editar = elemento('button', 'mensagem__acao', 'Editar');
          editar.type = 'button';
          editar.addEventListener('click', function () { iniciarEdicao(cartao, mensagem); });
          acoes.appendChild(editar);
        }
        var excluir = elemento('button', 'mensagem__acao', 'Excluir');
        excluir.type = 'button';
        excluir.addEventListener('click', async function () {
          if (!window.confirm('Excluir esta mensagem? O histórico mostrará o aviso de exclusão.')) return;
          try {
            await api('/chat/mensagens/' + encodeURIComponent(mensagem.id), { method: 'DELETE' });
            await carregarHistorico();
          } catch (erro) { mostrarErro(erro); }
        });
        acoes.appendChild(excluir);
        meta.appendChild(acoes);
      }
      cartao.appendChild(meta);
      historico.appendChild(cartao);
    });
    historico.hidden = false;
    historico.scrollTop = historico.scrollHeight;
  }

  async function carregarSalas() {
    var pagina = await api('/chat/salas?page=0&size=50');
    salas = pagina.content || [];
    renderSalas();
  }

  async function carregarHistorico() {
    if (!salaAtual) return;
    var pagina = await api('/chat/salas/' + encodeURIComponent(salaAtual) + '/mensagens?page=0&size=50');
    renderMensagens(pagina);
  }

  async function abrirSala(salaId) {
    salaAtual = Number(salaId);
    var sala = salas.find(function (item) { return Number(item.salaId) === salaAtual; });
    document.querySelector('[data-chat-participante]').textContent = sala ? sala.participanteNome : 'Conversa';
    document.querySelector('[data-chat-vazia]').hidden = true;
    document.querySelector('[data-chat-form]').hidden = false;
    renderSalas();
    await carregarHistorico();
    await api('/chat/salas/' + encodeURIComponent(salaAtual) + '/lidas', { method: 'PATCH' });
    await carregarSalas();
    await carregarHistorico();
  }

  function frameStomp(destino, corpo) {
    var json = JSON.stringify(corpo);
    return 'SEND\ndestination:' + destino + '\ncontent-type:application/json\ncontent-length:'
      + new TextEncoder().encode(json).length + '\n\n' + json + '\0';
  }

  function tratarEventoChat(evento) {
    if (!evento || !evento.salaId) return;
    carregarSalas().catch(mostrarErro);
    if (Number(evento.salaId) === Number(salaAtual)) {
      carregarHistorico().then(function () {
        if (evento.tipo === 'NOVA_MENSAGEM' && evento.mensagem
            && Number(evento.mensagem.remetenteId) !== Number(sessao.id)) {
          return api('/chat/salas/' + encodeURIComponent(salaAtual) + '/lidas', { method: 'PATCH' });
        }
      }).then(function () {
        return Promise.all([carregarSalas(), carregarHistorico()]);
      }).catch(mostrarErro);
    }
  }

  function tratarFrame(frame) {
    var separador = frame.indexOf('\n\n');
    var cabecalho = separador >= 0 ? frame.substring(0, separador) : frame;
    var corpo = separador >= 0 ? frame.substring(separador + 2) : '';
    var comando = cabecalho.split('\n')[0];
    if (comando === 'CONNECTED') {
      stompConectado = true;
      document.querySelector('[data-chat-conexao]').textContent = 'Tempo real ativo';
      document.querySelector('[data-chat-conexao]').classList.add('mensagens__conexao--ativa');
      websocket.send('SUBSCRIBE\nid:rf24-chat\ndestination:/user/queue/chat\nack:auto\n\n\0');
    } else if (comando === 'MESSAGE') {
      try { tratarEventoChat(JSON.parse(corpo)); } catch (erro) {}
    }
  }

  function iniciarWebSocket() {
    if (!window.WebSocket) {
      document.querySelector('[data-chat-conexao]').textContent = 'Atualização por recarga';
      return;
    }
    websocket = new WebSocket(API_BASE.replace(/^http/, 'ws').replace(/\/api$/, '') + '/ws');
    websocket.onopen = function () {
      websocket.send('CONNECT\naccept-version:1.2\nhost:' + window.location.host
        + '\nAuthorization:Bearer ' + sessao.token + '\nheart-beat:10000,10000\n\n\0');
    };
    websocket.onmessage = function (evento) {
      String(evento.data).split('\0').filter(Boolean).forEach(tratarFrame);
    };
    websocket.onerror = function () { if (websocket) websocket.close(); };
    websocket.onclose = function () {
      stompConectado = false;
      document.querySelector('[data-chat-conexao]').textContent = 'Atualização por recarga';
      document.querySelector('[data-chat-conexao]').classList.remove('mensagens__conexao--ativa');
    };
  }

  async function iniciar() {
    if (!sessao || !sessao.token) {
      window.location.href = '/login';
      return;
    }
    document.querySelector('[data-chat-avatar]').src = avatarSeguro(sessao.avatarUrl, sessao.id);
    document.querySelector('[data-chat-sair]').addEventListener('click', function () {
      sessionStorage.removeItem(CHAVE_SESSAO);
      localStorage.removeItem(CHAVE_SESSAO);
      window.location.href = '/login';
    });
    document.querySelector('[data-chat-form]').addEventListener('submit', async function (evento) {
      evento.preventDefault();
      var campo = document.querySelector('[data-chat-texto]');
      var texto = campo.value.trim();
      if (!texto || !salaAtual) return;
      campo.value = '';
      try {
        if (stompConectado && websocket && websocket.readyState === WebSocket.OPEN) {
          websocket.send(frameStomp('/app/chat/salas/' + salaAtual + '/mensagens', { texto: texto }));
        } else {
          await api('/chat/salas/' + encodeURIComponent(salaAtual) + '/mensagens', {
            method: 'POST', body: { texto: texto }
          });
          await carregarHistorico();
          await carregarSalas();
        }
      } catch (erro) {
        campo.value = texto;
        mostrarErro(erro);
      }
    });

    try {
      await carregarSalas();
      document.querySelector('[data-chat-conteudo]').hidden = false;
      document.querySelector('[data-chat]').setAttribute('aria-busy', 'false');
      iniciarWebSocket();
      var solicitada = Number(new URLSearchParams(window.location.search).get('sala'));
      if (solicitada && salas.some(function (item) { return Number(item.salaId) === solicitada; })) {
        await abrirSala(solicitada);
      } else if (salas.length) {
        await abrirSala(salas[0].salaId);
      }
    } catch (erro) {
      mostrarErro(erro);
      document.querySelector('[data-chat-conteudo]').hidden = false;
      document.querySelector('[data-chat]').setAttribute('aria-busy', 'false');
    }
  }

  iniciar();
})();

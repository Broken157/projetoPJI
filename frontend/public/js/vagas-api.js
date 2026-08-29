/* Cliente mínimo compartilhado pelo RF03 público e pela landing.
   Reutiliza a sessão atual sem renovar token nem criar outro fluxo de login. */
(function (global) {
  'use strict';

  var API_BASE = (global.PALCO_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');
  var CHAVE_SESSAO = 'palco.sessao';
  var CAMPOS_FILTRO = [
    'titulo',
    'empresa',
    'cidade',
    'estado',
    'modeloTrabalho',
    'tipoContrato',
    'faixaSalarialMin',
    'faixaSalarialMax',
    'areaAtuacao'
  ];

  function lerSessao() {
    var bruto = global.sessionStorage.getItem(CHAVE_SESSAO)
      || global.localStorage.getItem(CHAVE_SESSAO);
    if (!bruto) return null;
    try {
      return JSON.parse(bruto);
    } catch (erro) {
      return null;
    }
  }

  function mensagemDaApi(corpo, status) {
    if (corpo && typeof corpo.mensagem === 'string' && corpo.mensagem.trim()) {
      return corpo.mensagem.trim();
    }
    return 'Não foi possível carregar as vagas agora. Tente novamente.';
  }

  async function lerCorpo(resposta) {
    var tipo = resposta.headers && resposta.headers.get
      ? resposta.headers.get('content-type') || ''
      : '';
    if (tipo.indexOf('application/json') < 0) return null;
    return resposta.json();
  }

  async function requisitar(caminho, opcoes) {
    var config = Object.assign({}, opcoes || {});
    var headers = Object.assign({ Accept: 'application/json' }, config.headers || {});
    var sessao = lerSessao();
    var enviouToken = Boolean(sessao && sessao.token);

    if (enviouToken) headers.Authorization = 'Bearer ' + sessao.token;
    config.headers = headers;

    var resposta = await global.fetch(API_BASE + caminho, config);

    /* O feed é público. Uma sessão expirada não deve criar loop de refresh nem
       impedir a busca anônima: repete uma única vez sem Authorization. */
    if (resposta.status === 401 && enviouToken) {
      var headersPublicos = Object.assign({}, headers);
      delete headersPublicos.Authorization;
      config.headers = headersPublicos;
      resposta = await global.fetch(API_BASE + caminho, config);
    }

    var corpo = await lerCorpo(resposta);
    if (!resposta.ok) {
      var erro = new Error(mensagemDaApi(corpo, resposta.status));
      erro.status = resposta.status;
      throw erro;
    }
    return corpo || {};
  }

  function valorPreenchido(valor) {
    return valor !== null && valor !== undefined && String(valor).trim() !== '';
  }

  function parametrosListagem(filtros, paginacao) {
    var parametros = new URLSearchParams();
    var valores = filtros || {};
    CAMPOS_FILTRO.forEach(function (campo) {
      if (valorPreenchido(valores[campo])) {
        parametros.set(campo, String(valores[campo]).trim());
      }
    });

    var pagina = paginacao || {};
    if (valorPreenchido(pagina.cursor)) parametros.set('cursor', String(pagina.cursor));
    if (valorPreenchido(pagina.cursorCanceladas)) {
      parametros.set('cursorCanceladas', String(pagina.cursorCanceladas));
    }
    parametros.set('size', String(pagina.size || 20));
    return parametros;
  }

  function primeiraFotoValida(fotos) {
    if (!Array.isArray(fotos)) return null;
    for (var i = 0; i < fotos.length; i += 1) {
      if (typeof fotos[i] !== 'string' || !fotos[i].trim()) continue;
      try {
        var url = new URL(fotos[i].trim(), global.location.href);
        if (url.protocol === 'http:' || url.protocol === 'https:') return url.href;
      } catch (erro) {
        // URL inválida: tenta a próxima e preserva o fallback local.
      }
    }
    return null;
  }

  function urlDetalhe(id) {
    return 'detalhe-vaga.html?id=' + encodeURIComponent(String(id));
  }

  global.PalcoVagas = Object.freeze({
    requisitar: requisitar,
    parametrosListagem: parametrosListagem,
    primeiraFotoValida: primeiraFotoValida,
    urlDetalhe: urlDetalhe
  });
})(window);

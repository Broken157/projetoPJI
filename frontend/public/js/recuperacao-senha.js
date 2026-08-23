/* Integração mínima das telas do RF09 com a API Palco. */
(function () {
  'use strict';

  var API_BASE = (window.PALCO_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');
  var MENSAGEM_TOKEN_INVALIDO =
    'O link de recuperação é inválido ou expirou. Solicite uma nova recuperação de senha.';

  async function post(caminho, corpo) {
    var resposta = await fetch(API_BASE + caminho, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(corpo)
    });
    var dados = await resposta.json().catch(function () { return null; });
    if (!resposta.ok) throw new Error('REQUISICAO_REJEITADA');
    return dados;
  }

  function iniciarRecuperacao() {
    var form = document.getElementById('form-recuperar-senha');
    if (!form) return;
    var mensagem = document.getElementById('mensagem-recuperacao');
    var botao = form.querySelector('button[type="submit"]');

    form.addEventListener('submit', async function (evento) {
      evento.preventDefault();
      if (!form.checkValidity()) {
        form.reportValidity();
        return;
      }

      botao.disabled = true;
      mensagem.textContent = '';
      try {
        var resposta = await post('/auth/forgot-password', {
          email: form.elements.email.value.trim()
        });
        mensagem.textContent = resposta && resposta.mensagem
          ? resposta.mensagem
          : 'Se o e-mail estiver cadastrado, você receberá as instruções em breve.';
      } catch (erro) {
        mensagem.textContent = 'Não foi possível enviar a solicitação. Tente novamente.';
      } finally {
        botao.disabled = false;
      }
    });
  }

  function tokenDoFragmento() {
    var parametros = new URLSearchParams(window.location.hash.replace(/^#/, ''));
    var token = parametros.get('token');
    if (token) {
      history.replaceState(null, '', window.location.pathname + window.location.search);
    }
    return token;
  }

  function iniciarRedefinicao() {
    var form = document.getElementById('form-redefinir-senha');
    if (!form) return;
    var token = tokenDoFragmento();
    var mensagem = document.getElementById('mensagem-redefinicao');
    var botao = form.querySelector('button[type="submit"]');

    if (!token) {
      botao.disabled = true;
      mensagem.textContent = 'Link de recuperação inválido ou incompleto.';
    }

    form.addEventListener('submit', async function (evento) {
      evento.preventDefault();
      if (!token) return;
      if (!form.checkValidity()) {
        form.reportValidity();
        return;
      }

      var novaSenha = form.elements.novaSenha.value;
      if (novaSenha !== form.elements.confirmarSenha.value) {
        mensagem.textContent = 'As senhas informadas não são iguais.';
        return;
      }

      botao.disabled = true;
      mensagem.textContent = '';
      try {
        var resposta = await post('/auth/reset-password', {
          token: token,
          novaSenha: novaSenha
        });
        mensagem.textContent = resposta && resposta.mensagem
          ? resposta.mensagem
          : 'Senha redefinida com sucesso.';
        form.reset();
        token = null;
      } catch (erro) {
        mensagem.textContent = MENSAGEM_TOKEN_INVALIDO;
        botao.disabled = false;
      }
    });
  }

  iniciarRecuperacao();
  iniciarRedefinicao();
}());

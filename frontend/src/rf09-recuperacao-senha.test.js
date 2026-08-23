import fs from 'fs';
import path from 'path';
import { waitFor } from '@testing-library/dom';

const script = fs.readFileSync(
  path.join(process.cwd(), 'public', 'js', 'recuperacao-senha.js'),
  'utf8'
);

function montarRecuperacao() {
  document.body.innerHTML = `
    <form id="form-recuperar-senha" novalidate>
      <input type="email" name="email" required>
      <button type="submit">Enviar</button>
      <p id="mensagem-recuperacao"></p>
    </form>`;
}

function montarRedefinicao() {
  document.body.innerHTML = `
    <form id="form-redefinir-senha" novalidate>
      <input type="password" name="novaSenha" required>
      <input type="password" name="confirmarSenha" required>
      <button type="submit">Redefinir</button>
      <p id="mensagem-redefinicao"></p>
    </form>`;
}

function enviar(form) {
  form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
}

beforeEach(() => {
  window.fetch = jest.fn();
  window.history.replaceState(null, '', '/');
});

test('recuperação exige e-mail e mostra a resposta genérica da API', async () => {
  montarRecuperacao();
  window.eval(script);
  const form = document.getElementById('form-recuperar-senha');

  enviar(form);
  expect(window.fetch).not.toHaveBeenCalled();

  form.elements.email.value = 'artista@palco.test';
  window.fetch.mockResolvedValue({
    ok: true,
    json: async () => ({
      mensagem: 'Se o e-mail estiver cadastrado, você receberá as instruções em breve.'
    })
  });
  enviar(form);

  await waitFor(() => expect(window.fetch).toHaveBeenCalledWith(
    'http://localhost:8080/api/auth/forgot-password',
    expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ email: 'artista@palco.test' })
    })
  ));
  await waitFor(() => expect(document.getElementById('mensagem-recuperacao'))
    .toHaveTextContent('Se o e-mail estiver cadastrado'));
});

test('redefinição lê e remove o fragmento, bloqueia divergência e envia token no body', async () => {
  montarRedefinicao();
  window.history.replaceState(null, '', '/redefinir-senha.html#token=TOKEN_SEGURO');
  window.fetch.mockResolvedValue({
    ok: true,
    json: async () => ({ mensagem: 'Senha redefinida com sucesso.' })
  });
  window.eval(script);

  expect(window.location.hash).toBe('');
  const form = document.getElementById('form-redefinir-senha');
  form.elements.novaSenha.value = 'senha-nova';
  form.elements.confirmarSenha.value = 'diferente';
  enviar(form);
  expect(window.fetch).not.toHaveBeenCalled();
  expect(document.getElementById('mensagem-redefinicao'))
    .toHaveTextContent('não são iguais');

  form.elements.confirmarSenha.value = 'senha-nova';
  enviar(form);

  await waitFor(() => expect(window.fetch).toHaveBeenCalledTimes(1));
  const [url, opcoes] = window.fetch.mock.calls[0];
  expect(url).toBe('http://localhost:8080/api/auth/reset-password');
  expect(url).not.toContain('token=');
  expect(JSON.parse(opcoes.body)).toEqual({
    token: 'TOKEN_SEGURO',
    novaSenha: 'senha-nova'
  });
  await waitFor(() => expect(document.getElementById('mensagem-redefinicao'))
    .toHaveTextContent('Senha redefinida com sucesso'));
});

test('redefinição sem token bloqueia envio', () => {
  montarRedefinicao();
  window.history.replaceState(null, '', '/redefinir-senha.html');
  window.eval(script);

  const form = document.getElementById('form-redefinir-senha');
  expect(form.querySelector('button')).toBeDisabled();
  expect(document.getElementById('mensagem-redefinicao'))
    .toHaveTextContent('inválido ou incompleto');
  enviar(form);
  expect(window.fetch).not.toHaveBeenCalled();
});

test('erro de token exibe mensagem simples de inválido ou expirado', async () => {
  montarRedefinicao();
  window.history.replaceState(null, '', '/redefinir-senha.html#token=EXPIRADO');
  window.fetch.mockResolvedValue({ ok: false, json: async () => ({}) });
  window.eval(script);

  const form = document.getElementById('form-redefinir-senha');
  form.elements.novaSenha.value = 'senha-nova';
  form.elements.confirmarSenha.value = 'senha-nova';
  enviar(form);

  await waitFor(() => expect(document.getElementById('mensagem-redefinicao'))
    .toHaveTextContent('é inválido ou expirou'));
});

test('script não persiste o token no navegador', () => {
  expect(script).not.toMatch(/localStorage|sessionStorage|indexedDB|document\.cookie/);
});

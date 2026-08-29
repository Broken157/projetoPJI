import sessionService from '../../auth/sessionService';
import { DEFAULT_API_BASE_URL, resolveApiBaseUrl } from '../../config/apiConfig';
import ApiError from './ApiError';
import apiClient from './apiClient';

function response({ body = null, status = 200, contentType = 'application/json' } = {}) {
  return {
    status,
    ok: status >= 200 && status < 300,
    headers: { get: jest.fn().mockReturnValue(contentType) },
    json: jest.fn().mockResolvedValue(body),
    text: jest.fn().mockResolvedValue(typeof body === 'string' ? body : ''),
  };
}

beforeEach(() => {
  global.fetch = jest.fn();
  jest.spyOn(sessionService, 'getAccessToken').mockReturnValue(null);
});

afterEach(() => {
  jest.restoreAllMocks();
});

test('resolve a URL base padrão, configuração global e barra final', () => {
  expect(resolveApiBaseUrl({})).toBe(DEFAULT_API_BASE_URL);
  expect(resolveApiBaseUrl({ PALCO_API_BASE_URL: 'https://api.palco.test/api/' })).toBe(
    'https://api.palco.test/api'
  );
});

test('executa GET na URL base sem Authorization para sessão anônima', async () => {
  global.fetch.mockResolvedValue(response({ body: [{ id: 1 }] }));

  await expect(apiClient.get('/vagas')).resolves.toEqual([{ id: 1 }]);

  expect(global.fetch).toHaveBeenCalledWith(`${DEFAULT_API_BASE_URL}/vagas`, {
    method: 'GET',
    headers: expect.any(Headers),
    body: undefined,
  });
  const [, options] = global.fetch.mock.calls[0];
  expect(options.headers.get('Authorization')).toBeNull();
});

test('serializa corpo JSON e configura Content-Type no POST', async () => {
  global.fetch.mockResolvedValue(response({ body: { id: 10 }, status: 201 }));

  await apiClient.post('/vagas', { titulo: 'Vaga' });

  const [, options] = global.fetch.mock.calls[0];
  expect(options.method).toBe('POST');
  expect(options.body).toBe(JSON.stringify({ titulo: 'Vaga' }));
  expect(options.headers.get('Content-Type')).toBe('application/json');
});

test('envia Bearer token quando a sessão possui access token', async () => {
  sessionService.getAccessToken.mockReturnValue('jwt-token');
  global.fetch.mockResolvedValue(response({ body: {} }));

  await apiClient.get('/dashboard');

  const [, options] = global.fetch.mock.calls[0];
  expect(options.headers.get('Authorization')).toBe('Bearer jwt-token');
});

test('retorna null para resposta sem conteúdo', async () => {
  global.fetch.mockResolvedValue(response({ status: 204, contentType: '' }));

  await expect(apiClient.delete('/notificacoes/1')).resolves.toBeNull();
});

test('preserva status, mensagem e body de erro HTTP', async () => {
  const body = { mensagem: 'Conflito de estado.', codigo: 'VAGA_INVALIDA' };
  global.fetch.mockResolvedValue(response({ body, status: 409 }));

  await expect(apiClient.patch('/vagas/1', { acao: 'SUSPENDER' })).rejects.toMatchObject({
    name: 'ApiError',
    status: 409,
    message: 'Conflito de estado.',
    body,
  });
});

test('mantém erro 422 identificável sem convertê-lo em 500', async () => {
  global.fetch.mockResolvedValue(response({ body: { mensagem: 'Dados inválidos.' }, status: 422 }));

  await expect(apiClient.post('/vagas', {})).rejects.toMatchObject({
    status: 422,
  });
});

test('mantém erro 401 identificável como ApiError', async () => {
  global.fetch.mockResolvedValue(response({ body: { mensagem: 'Não autenticado.' }, status: 401 }));

  await expect(apiClient.get('/dashboard')).rejects.toBeInstanceOf(ApiError);
  await expect(apiClient.get('/dashboard')).rejects.toMatchObject({ status: 401 });
});

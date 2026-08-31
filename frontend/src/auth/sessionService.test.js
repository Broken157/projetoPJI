import {
  SESSION_STORAGE_KEY,
  VACANCY_DRAFT_STORAGE_KEY,
  clearLocalSession,
  getAccessToken,
  getSession,
  isAuthenticated,
  saveSession,
} from './sessionService';

beforeEach(() => {
  window.sessionStorage.clear();
  window.localStorage.clear();
});

test('lê uma sessão existente válida do sessionStorage', () => {
  const session = { token: 'access-token', tipoUsuario: 'CONTRATANTE' };
  window.sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));

  expect(getSession()).toEqual(session);
});

test('mantém o fallback histórico para localStorage', () => {
  const session = { token: 'legacy-token', tipoUsuario: 'ARTISTA' };
  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));

  expect(getSession()).toEqual(session);
});

test('retorna null quando não existe sessão', () => {
  expect(getSession()).toBeNull();
  expect(isAuthenticated()).toBe(false);
});

test('remove os storages e retorna null quando o JSON é inválido', () => {
  window.sessionStorage.setItem(SESSION_STORAGE_KEY, '{inválido');
  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify({ token: 'fallback' }));

  expect(getSession()).toBeNull();
  expect(window.sessionStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  expect(window.localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
});

test('lê o access token sem expor o storage aos consumidores', () => {
  window.sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify({ token: 'access-token' }));

  expect(getAccessToken()).toBe('access-token');
  expect(isAuthenticated()).toBe(true);
});

test('limpa sessão local e rascunho de vaga', () => {
  window.sessionStorage.setItem(SESSION_STORAGE_KEY, '{}');
  window.localStorage.setItem(SESSION_STORAGE_KEY, '{}');
  window.sessionStorage.setItem(VACANCY_DRAFT_STORAGE_KEY, '{}');

  clearLocalSession();

  expect(window.sessionStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  expect(window.localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  expect(window.sessionStorage.getItem(VACANCY_DRAFT_STORAGE_KEY)).toBeNull();
});

test('salva somente os campos permitidos no sessionStorage e nunca persiste senha', () => {
  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify({ token: 'antigo' }));

  const saved = saveSession({
    token: 'jwt-atual',
    id: 7,
    email: 'artista@palco.test',
    tipoUsuario: 'ARTISTA',
    perfilCompleto: false,
    senha: 'nunca-persistir',
    campoInesperado: 'ignorar',
  });

  expect(saved).toEqual({
    token: 'jwt-atual',
    id: 7,
    email: 'artista@palco.test',
    tipoUsuario: 'ARTISTA',
    perfilCompleto: false,
  });
  expect(window.sessionStorage.getItem(SESSION_STORAGE_KEY)).not.toContain('senha');
  expect(window.sessionStorage.getItem(SESSION_STORAGE_KEY)).not.toContain('nunca-persistir');
  expect(window.localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
});

test('recusa resposta de autenticação sem JWT', () => {
  expect(() => saveSession({ email: 'sem-token@palco.test' })).toThrow('token válido');
  expect(window.sessionStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
});

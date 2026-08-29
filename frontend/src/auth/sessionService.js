export const SESSION_STORAGE_KEY = 'palco.sessao';
export const VACANCY_DRAFT_STORAGE_KEY = 'palco.vaga.rascunho';

function storages() {
  return {
    session: window.sessionStorage,
    local: window.localStorage,
  };
}

export function clearLocalSession() {
  const { session, local } = storages();
  session.removeItem(SESSION_STORAGE_KEY);
  local.removeItem(SESSION_STORAGE_KEY);
  session.removeItem(VACANCY_DRAFT_STORAGE_KEY);
}

export function getSession() {
  const { session, local } = storages();
  const serializedSession =
    session.getItem(SESSION_STORAGE_KEY) || local.getItem(SESSION_STORAGE_KEY);

  if (!serializedSession) return null;

  try {
    return JSON.parse(serializedSession);
  } catch {
    clearLocalSession();
    return null;
  }
}

export function getAccessToken() {
  return getSession()?.token || null;
}

export function isAuthenticated() {
  return Boolean(getAccessToken());
}

const sessionService = {
  getSession,
  getAccessToken,
  isAuthenticated,
  clearLocalSession,
};

export default sessionService;

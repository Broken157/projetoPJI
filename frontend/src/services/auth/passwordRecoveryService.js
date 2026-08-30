import apiClient from '../api/apiClient';

export const GENERIC_RECOVERY_MESSAGE =
  'Se o e-mail estiver cadastrado, você receberá as instruções em breve.';

export const INVALID_RESET_LINK_MESSAGE =
  'O link de recuperação é inválido ou expirou. Solicite uma nova recuperação de senha.';

export function requestPasswordRecovery(email) {
  return apiClient.post('/auth/forgot-password', { email }, { token: null });
}

export function resetPassword(token, novaSenha) {
  return apiClient.post('/auth/reset-password', { token, novaSenha }, { token: null });
}

export function readResetToken(runtime = window) {
  const parameters = new URLSearchParams(runtime.location.hash.replace(/^#/, ''));
  return parameters.get('token') || null;
}

export function sanitizeResetUrl(runtime = window) {
  const url = new URL(runtime.location.href);
  const fragmentParameters = new URLSearchParams(url.hash.replace(/^#/, ''));
  const fragmentContainsToken = fragmentParameters.has('token');
  const queryContainsToken = url.searchParams.has('token');

  if (!fragmentContainsToken && !queryContainsToken) return;

  if (fragmentContainsToken) url.hash = '';
  if (queryContainsToken) url.searchParams.delete('token');
  runtime.history.replaceState(null, '', `${url.pathname}${url.search}${url.hash}`);
}

export function redirectToLegacyLogin(runtime = window) {
  runtime.location.assign('/login.html');
}

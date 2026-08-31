import apiClient from '../api/apiClient';

const requestsInFlight = new Map();

export function getPublicProfile(type, id) {
  const normalizedType = String(type).toUpperCase();
  const normalizedId = String(id);
  const key = `${normalizedType}/${normalizedId}`;

  if (!requestsInFlight.has(key)) {
    const request = apiClient
      .get(
        `/perfis/publicos/${encodeURIComponent(normalizedType)}/${encodeURIComponent(normalizedId)}`,
        { token: null }
      )
      .finally(() => requestsInFlight.delete(key));
    requestsInFlight.set(key, request);
  }

  return requestsInFlight.get(key);
}

export function createConversation(usuarioDestinoId) {
  return apiClient.post('/chat/salas', { usuarioDestinoId });
}

export function messagesUrl(salaId) {
  return `/mensagens.html?sala=${encodeURIComponent(salaId)}`;
}

export function navigateToMessages(salaId, location = window.location) {
  location.assign(messagesUrl(salaId));
}

import apiClient from '../api/apiClient';

export const CHAT_PAGE_SIZE = 20;

function positiveId(value, label) {
  const normalized = Number(value);
  if (!Number.isSafeInteger(normalized) || normalized < 1) {
    throw new TypeError(`${label} inválido.`);
  }
  return normalized;
}

export function listRooms(page = 0, size = CHAT_PAGE_SIZE) {
  return apiClient.get(`/chat/salas?page=${page}&size=${size}`);
}

export function createRoom(usuarioDestinoId) {
  return apiClient.post('/chat/salas', {
    usuarioDestinoId: positiveId(usuarioDestinoId, 'Usuário de destino'),
  });
}

export function listMessages(roomId, page = 0, size = CHAT_PAGE_SIZE) {
  return apiClient.get(
    `/chat/salas/${positiveId(roomId, 'Sala')}/mensagens?page=${page}&size=${size}`
  );
}

export function sendMessage(roomId, texto) {
  return apiClient.post(`/chat/salas/${positiveId(roomId, 'Sala')}/mensagens`, { texto });
}

export function markRoomRead(roomId) {
  return apiClient.patch(`/chat/salas/${positiveId(roomId, 'Sala')}/lidas`);
}

export function editMessage(messageId, texto) {
  return apiClient.patch(`/chat/mensagens/${positiveId(messageId, 'Mensagem')}`, { texto });
}

export function deleteMessage(messageId) {
  return apiClient.delete(`/chat/mensagens/${positiveId(messageId, 'Mensagem')}`);
}

export function messagesUrl(roomId) {
  return `/mensagens?sala=${encodeURIComponent(positiveId(roomId, 'Sala'))}`;
}

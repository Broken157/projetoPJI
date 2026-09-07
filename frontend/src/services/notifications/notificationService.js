import apiClient from '../api/apiClient';

export const NOTIFICATION_PAGE_SIZE = 20;

export function listNotifications(page = 0, size = NOTIFICATION_PAGE_SIZE, options = {}) {
  if (!Number.isInteger(page) || page < 0 || !Number.isInteger(size) || size < 1) {
    throw new Error('Paginação de notificações inválida.');
  }
  return apiClient.get(`/notificacoes?page=${page}&size=${Math.min(size, 50)}`, options);
}

export function countUnreadNotifications(options = {}) {
  return apiClient.get('/notificacoes/nao-lidas/count', options);
}

export function markNotificationRead(id, options = {}) {
  if (!/^[1-9][0-9]*$/.test(String(id))) throw new Error('Notificação inválida.');
  return apiClient.patch(`/notificacoes/${encodeURIComponent(id)}/lida`, undefined, options);
}

export function markAllNotificationsRead(options = {}) {
  return apiClient.patch('/notificacoes/lidas', undefined, options);
}

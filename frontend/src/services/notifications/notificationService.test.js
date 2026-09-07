import apiClient from '../api/apiClient';
import { listNotifications, countUnreadNotifications, markNotificationRead, markAllNotificationsRead } from './notificationService';

jest.mock('../api/apiClient', () => ({ get: jest.fn(), patch: jest.fn() }));
beforeEach(() => jest.clearAllMocks());

test('lista preserva padrão 20, paginação e máximo 50', () => {
  listNotifications();
  expect(apiClient.get).toHaveBeenLastCalledWith('/notificacoes?page=0&size=20', {});
  listNotifications(2, 100);
  expect(apiClient.get).toHaveBeenLastCalledWith('/notificacoes?page=2&size=50', {});
  expect(() => listNotifications(-1)).toThrow();
  expect(() => listNotifications(0, 0)).toThrow();
});

test('contagem e leitura usam endpoints reais sem identidade no payload', () => {
  const options = { token: 'jwt', signal: new AbortController().signal };
  countUnreadNotifications(options);
  markNotificationRead(12, options);
  markAllNotificationsRead(options);
  expect(apiClient.get).toHaveBeenCalledWith('/notificacoes/nao-lidas/count', options);
  expect(apiClient.patch.mock.calls).toEqual([
    ['/notificacoes/12/lida', undefined, options], ['/notificacoes/lidas', undefined, options],
  ]);
  expect(() => markNotificationRead('../12')).toThrow();
});

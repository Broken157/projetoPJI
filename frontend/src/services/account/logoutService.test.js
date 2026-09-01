import sessionService from '../../auth/sessionService';
import apiClient from '../api/apiClient';
import { logoutCurrentSession } from './logoutService';

jest.mock('../../auth/sessionService', () => ({ getSession: jest.fn(), clearLocalSession: jest.fn() }));
jest.mock('../api/apiClient', () => ({ post: jest.fn() }));

beforeEach(() => {
  sessionService.getSession.mockReset();
  sessionService.clearLocalSession.mockReset();
  apiClient.post.mockReset();
});

test('invalida refresh token e limpa a sessão', async () => {
  sessionService.getSession.mockReturnValue({ token: 'jwt', refreshToken: 'refresh' });
  apiClient.post.mockResolvedValue(null);
  await logoutCurrentSession();
  expect(apiClient.post).toHaveBeenCalledWith('/auth/logout', { refreshToken: 'refresh' });
  expect(sessionService.clearLocalSession).toHaveBeenCalledTimes(1);
});

test('sessão sem refresh token é limpa sem inventar chamada backend', async () => {
  sessionService.getSession.mockReturnValue({ token: 'jwt' });
  await logoutCurrentSession();
  expect(apiClient.post).not.toHaveBeenCalled();
  expect(sessionService.clearLocalSession).toHaveBeenCalledTimes(1);
});

test('erro do endpoint não mantém sessão local utilizável', async () => {
  sessionService.getSession.mockReturnValue({ token: 'jwt', refreshToken: 'refresh' });
  apiClient.post.mockRejectedValue(new Error('offline'));
  await expect(logoutCurrentSession()).rejects.toThrow('offline');
  expect(sessionService.clearLocalSession).toHaveBeenCalledTimes(1);
});

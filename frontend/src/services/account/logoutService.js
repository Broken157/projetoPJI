import sessionService from '../../auth/sessionService';
import apiClient from '../api/apiClient';

export async function logoutCurrentSession() {
  const session = sessionService.getSession();
  try {
    if (session?.refreshToken) {
      await apiClient.post('/auth/logout', { refreshToken: session.refreshToken });
    }
  } finally {
    sessionService.clearLocalSession();
  }
}

const logoutService = { logoutCurrentSession };

export default logoutService;

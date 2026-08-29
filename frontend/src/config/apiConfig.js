export const DEFAULT_API_BASE_URL = 'http://localhost:8080/api';

export function resolveApiBaseUrl(runtime = window) {
  const configuredUrl = runtime?.PALCO_API_BASE_URL || DEFAULT_API_BASE_URL;
  return configuredUrl.replace(/\/+$/, '');
}

export const API_BASE_URL = resolveApiBaseUrl();

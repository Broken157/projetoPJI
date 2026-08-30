import apiClient from '../api/apiClient';

const requestsInFlight = new Map();

export function getVagaDetails(id) {
  const key = String(id);

  if (!requestsInFlight.has(key)) {
    const request = apiClient
      .get(`/vagas/${encodeURIComponent(key)}`)
      .finally(() => requestsInFlight.delete(key));
    requestsInFlight.set(key, request);
  }

  return requestsInFlight.get(key);
}

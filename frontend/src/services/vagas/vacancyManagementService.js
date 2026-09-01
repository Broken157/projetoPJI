import apiClient from '../api/apiClient';

export const MY_VACANCIES_PAGE_SIZE = 20;

const EDITABLE_FIELDS = [
  'titulo',
  'descricao',
  'requisitos',
  'remuneraValor',
  'formaPagamento',
  'cidade',
  'estado',
  'enderecoCompleto',
  'beneficios',
  'modeloTrabalho',
  'tipoContrato',
  'tagIds',
  'categoria',
  'experiencia',
  'dataLimiteCandidatura',
  'abrangencia',
  'fotos',
];

export function normalizeVacancyPayload(source) {
  const payload = EDITABLE_FIELDS.reduce((result, field) => {
    if (source?.[field] !== undefined) result[field] = source[field];
    return result;
  }, {});

  payload.titulo = String(payload.titulo || '').trim();
  payload.descricao = String(payload.descricao || '').trim();
  payload.requisitos = String(payload.requisitos || '').trim();
  payload.remuneraValor = Number(payload.remuneraValor);
  payload.formaPagamento = String(payload.formaPagamento || '').trim();
  payload.cidade = String(payload.cidade || '').trim();
  payload.estado = String(payload.estado || '').trim().toUpperCase();
  payload.tipoContrato = String(payload.tipoContrato || '').trim();
  payload.enderecoCompleto = String(payload.enderecoCompleto || '').trim() || null;
  payload.beneficios = String(payload.beneficios || '').trim() || null;
  payload.categoria = String(payload.categoria || '').trim() || null;
  payload.experiencia = String(payload.experiencia || '').trim() || null;
  payload.dataLimiteCandidatura = payload.dataLimiteCandidatura || null;
  payload.abrangencia = String(payload.abrangencia || '').trim() || null;
  payload.tagIds = [...new Set((Array.isArray(payload.tagIds) ? payload.tagIds : [])
    .map(Number)
    .filter((id) => Number.isInteger(id) && id > 0))];
  payload.fotos = (Array.isArray(payload.fotos) ? payload.fotos : [])
    .map((url) => String(url || '').trim())
    .filter(Boolean);
  return payload;
}

export function listMyVacancies({ cursor, size = MY_VACANCIES_PAGE_SIZE } = {}) {
  const params = new URLSearchParams({ size: String(size) });
  if (cursor !== null && cursor !== undefined) params.set('cursor', String(cursor));
  return apiClient.get(`/vagas/minhas?${params}`);
}

export function getManagedVacancy(id) {
  return apiClient.get(`/vagas/${encodeURIComponent(id)}`);
}

export function getRelatedVacancies(id) {
  return apiClient.get(`/vagas/${encodeURIComponent(id)}/similares?size=3`);
}

export function listVacancyTags() {
  return apiClient.get('/tags');
}

export function listReceivedApplications({ page = 0, size = 50 } = {}) {
  return apiClient.get(`/candidaturas/minhas-vagas?page=${page}&size=${size}`);
}

export async function listAllReceivedApplications() {
  const size = 50;
  const applications = [];
  for (let page = 0; ; page += 1) {
    const current = await listReceivedApplications({ page, size });
    applications.push(...current);
    if (current.length < size) return applications;
  }
}

export function createVacancy(source) {
  return apiClient.post('/vagas', normalizeVacancyPayload(source));
}

export function updateVacancy(id, source) {
  return apiClient.put(`/vagas/${encodeURIComponent(id)}`, normalizeVacancyPayload(source));
}

export function changeVacancyStatus(id, acao) {
  return apiClient.patch(`/vagas/${encodeURIComponent(id)}/status`, { acao });
}

export function cancelVacancy(id, motivo) {
  return apiClient.delete(`/vagas/${encodeURIComponent(id)}`, {
    body: { confirmacao: true, motivo: String(motivo || '').trim() },
  });
}

const vacancyManagementService = {
  listMyVacancies,
  getManagedVacancy,
  getRelatedVacancies,
  listVacancyTags,
  listReceivedApplications,
  listAllReceivedApplications,
  createVacancy,
  updateVacancy,
  changeVacancyStatus,
  cancelVacancy,
};

export default vacancyManagementService;

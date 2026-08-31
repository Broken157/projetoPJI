import apiClient from '../api/apiClient';

export function createCandidatura({ vagaId, mensagemApresentacao, linkPortfolioCandidatura }) {
  return apiClient.post('/candidaturas', {
    vagaId: Number(vagaId),
    mensagemApresentacao,
    linkPortfolioCandidatura,
  });
}

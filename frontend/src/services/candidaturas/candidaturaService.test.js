import apiClient from '../api/apiClient';
import { createCandidatura } from './candidaturaService';

jest.mock('../api/apiClient', () => ({
  __esModule: true,
  default: { post: jest.fn() },
}));

beforeEach(() => {
  apiClient.post.mockReset();
});

test('envia somente os campos oficiais e converte vagaId para número', async () => {
  apiClient.post.mockResolvedValue({ id: 9, status: 'PENDENTE' });

  await createCandidatura({
    vagaId: '42',
    mensagemApresentacao: 'Minha apresentação',
    linkPortfolioCandidatura: 'https://portfolio.example',
    artistaId: 777,
    usuarioId: 888,
  });

  expect(apiClient.post).toHaveBeenCalledWith('/candidaturas', {
    vagaId: 42,
    mensagemApresentacao: 'Minha apresentação',
    linkPortfolioCandidatura: 'https://portfolio.example',
  });
  const payload = apiClient.post.mock.calls[0][1];
  expect(payload).not.toHaveProperty('artistaId');
  expect(payload).not.toHaveProperty('usuarioId');
});

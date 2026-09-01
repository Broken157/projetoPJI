import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import {
  listMyVacancies,
  listAllReceivedApplications,
} from '../../services/vagas/vacancyManagementService';
import MyVacanciesPage from './MyVacanciesPage';

jest.mock('../../components/vagas/ContractorVacancyLayout', () => function Layout({ children }) { return children; });
jest.mock('../../services/vagas/vacancyManagementService', () => ({
  MY_VACANCIES_PAGE_SIZE: 20,
  listMyVacancies: jest.fn(),
  listAllReceivedApplications: jest.fn(),
}));

const openVacancy = { id: 1, titulo: 'Vaga com candidatura', status: 'ABERTA', categoria: 'Música', modeloTrabalho: 'PRESENCIAL', cidade: 'Recife', estado: 'PE', tipoContrato: 'Freela', dataLimiteCandidatura: '2030-01-01' };
const pausedVacancy = { ...openVacancy, id: 2, titulo: 'Vaga sem candidatura', status: 'PAUSADA' };

beforeEach(() => {
  listMyVacancies.mockReset();
  listAllReceivedApplications.mockReset();
  listMyVacancies.mockResolvedValue({ content: [openVacancy, pausedVacancy], hasMore: false, nextCursor: null });
  listAllReceivedApplications.mockResolvedValue([{ id: 50, vagaId: 1 }]);
});

test('lista somente vagas do endpoint paginado com status e ações reais', async () => {
  render(<MyVacanciesPage />);
  expect(await screen.findByRole('heading', { name: 'Vaga com candidatura' })).toBeInTheDocument();
  expect(screen.getByText('Aberta')).toBeInTheDocument();
  expect(screen.getByText('Pausada')).toBeInTheDocument();
  expect(screen.getAllByRole('link', { name: 'Gerenciar' })[0]).toHaveAttribute('href', '/vagas/1/gerenciar');
  expect(listMyVacancies).toHaveBeenCalledWith({ size: 20 });
});

test('preserva filtros por candidatura sem renderizar texto não confiável como HTML', async () => {
  listMyVacancies.mockResolvedValue({ content: [{ ...openVacancy, titulo: '<img src=x onerror=alert(1)>' }, pausedVacancy], hasMore: false });
  render(<MyVacanciesPage />);
  expect(await screen.findByRole('heading', { name: '<img src=x onerror=alert(1)>' })).toBeInTheDocument();
  expect(document.querySelector('img[src="x"]')).toBeNull();
  fireEvent.click(screen.getByRole('button', { name: /Sem candidatura/ }));
  expect(screen.getByRole('heading', { name: 'Vaga sem candidatura' })).toBeInTheDocument();
  expect(screen.queryByRole('heading', { name: '<img src=x onerror=alert(1)>' })).not.toBeInTheDocument();
});

test('carrega próxima página somente pelo cursor informado', async () => {
  listMyVacancies
    .mockResolvedValueOnce({ content: [openVacancy], hasMore: true, nextCursor: 1 })
    .mockResolvedValueOnce({ content: [pausedVacancy], hasMore: false, nextCursor: null });
  render(<MyVacanciesPage />);
  const more = await screen.findByRole('button', { name: 'Carregar mais' });
  fireEvent.click(more);
  expect(await screen.findByRole('heading', { name: 'Vaga sem candidatura' })).toBeInTheDocument();
  expect(listMyVacancies).toHaveBeenNthCalledWith(2, { cursor: 1, size: 20 });
  expect(screen.queryByRole('button', { name: 'Carregar mais' })).not.toBeInTheDocument();
});

test('preserva erro 403 da API em mensagem contextual', async () => {
  listMyVacancies.mockRejectedValue({ status: 403, message: 'interno' });
  render(<MyVacanciesPage />);
  expect(await screen.findByRole('alert')).toHaveTextContent('Você não tem permissão');
  await waitFor(() => expect(screen.queryByText('interno')).not.toBeInTheDocument());
});

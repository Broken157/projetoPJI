import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import {
  createVacancy,
  getManagedVacancy,
  listVacancyTags,
  updateVacancy,
} from '../../services/vagas/vacancyManagementService';
import VacancyCreatePage from './VacancyCreatePage';
import VacancyEditPage from './VacancyEditPage';

jest.mock('../../components/vagas/ContractorVacancyLayout', () => function Layout({ children }) { return children; });
jest.mock('../../services/vagas/vacancyManagementService', () => ({
  createVacancy: jest.fn(),
  getManagedVacancy: jest.fn(),
  listVacancyTags: jest.fn(),
  updateVacancy: jest.fn(),
}));

const ownVacancy = {
  id: 8, propriaDoContratante: true, titulo: 'Vaga atual', descricao: 'Descrição',
  requisitos: 'Requisitos', remuneraValor: 500, formaPagamento: 'Pix', cidade: 'Recife',
  estado: 'PE', modeloTrabalho: 'PRESENCIAL', tipoContrato: 'Freela', tagIds: [2], status: 'PAUSADA',
};

function renderRoute(path, element) {
  return render(<MemoryRouter initialEntries={[path]} future={{ v7_relativeSplatPath: true, v7_startTransition: true }}><Routes><Route path={path.includes('editar') ? '/vagas/:id/editar' : '/vagas/nova'} element={element} /><Route path="/vagas/:id/gerenciar" element={<h1>Gestão após salvar</h1>} /></Routes></MemoryRouter>);
}

function fillCreateForm() {
  fireEvent.change(screen.getByLabelText('Título da vaga'), { target: { value: 'Nova vaga' } });
  fireEvent.change(screen.getByLabelText('Descrição'), { target: { value: 'Descrição' } });
  fireEvent.change(screen.getByLabelText('Requisitos'), { target: { value: 'Requisitos' } });
  fireEvent.change(screen.getByLabelText('Remuneração'), { target: { value: '1000' } });
  fireEvent.change(screen.getByLabelText('Forma de pagamento'), { target: { value: 'Pix' } });
  fireEvent.change(screen.getByLabelText('Cidade'), { target: { value: 'Recife' } });
  fireEvent.change(screen.getByLabelText('Estado (UF)'), { target: { value: 'PE' } });
  fireEvent.change(screen.getByLabelText('Tipo de contrato'), { target: { value: 'Freela' } });
}

beforeEach(() => {
  createVacancy.mockReset();
  getManagedVacancy.mockReset();
  listVacancyTags.mockReset();
  updateVacancy.mockReset();
  listVacancyTags.mockResolvedValue([{ id: 2, nome: 'Música' }]);
});

test('publicação válida usa tags da API e segue para gestão após 201', async () => {
  createVacancy.mockResolvedValue({ id: 81 });
  renderRoute('/vagas/nova', <VacancyCreatePage />);
  await screen.findByLabelText('Música');
  fillCreateForm();
  fireEvent.click(screen.getByLabelText('Música'));
  fireEvent.click(screen.getByRole('button', { name: 'Publicar vaga' }));
  await waitFor(() => expect(createVacancy).toHaveBeenCalledWith(expect.objectContaining({ titulo: 'Nova vaga', tagIds: [2] })));
  expect(await screen.findByRole('heading', { name: 'Gestão após salvar' })).toBeInTheDocument();
});

test.each([
  [403, 'Você não tem permissão'],
  [409, 'entrou em conflito'],
  [422, 'Dados de publicação inválidos'],
])('publicação preserva erro contextual HTTP %s', async (status, expected) => {
  createVacancy.mockRejectedValue({ status, message: status === 422 ? 'Dados de publicação inválidos' : 'interno' });
  renderRoute('/vagas/nova', <VacancyCreatePage />);
  await screen.findByLabelText('Música');
  fillCreateForm();
  fireEvent.click(screen.getByRole('button', { name: 'Publicar vaga' }));
  expect(await screen.findByRole('alert')).toHaveTextContent(expected);
});

test('edição própria carrega dados/tags e envia somente alterações do formulário', async () => {
  getManagedVacancy.mockResolvedValue(ownVacancy);
  updateVacancy.mockResolvedValue({ ...ownVacancy, titulo: 'Título atualizado' });
  renderRoute('/vagas/8/editar', <VacancyEditPage />);
  const title = await screen.findByLabelText('Título da vaga');
  expect(title).toHaveValue('Vaga atual');
  expect(screen.getByLabelText('Música')).toBeChecked();
  fireEvent.change(title, { target: { value: 'Título atualizado' } });
  fireEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }));
  await waitFor(() => expect(updateVacancy).toHaveBeenCalledWith('8', expect.objectContaining({ titulo: 'Título atualizado', tagIds: [2] })));
  expect(await screen.findByRole('heading', { name: 'Gestão após salvar' })).toBeInTheDocument();
});

test('contratante terceiro não recebe formulário de edição', async () => {
  getManagedVacancy.mockResolvedValue({ ...ownVacancy, propriaDoContratante: false });
  renderRoute('/vagas/8/editar', <VacancyEditPage />);
  expect(await screen.findByRole('heading', { name: 'Acesso negado' })).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Salvar alterações' })).not.toBeInTheDocument();
});

test('edição preserva 404 sem inventar vaga', async () => {
  getManagedVacancy.mockRejectedValue({ status: 404, message: 'Vaga não encontrada.' });
  renderRoute('/vagas/404/editar', <VacancyEditPage />);
  expect(await screen.findByRole('alert')).toHaveTextContent('A vaga solicitada não foi encontrada');
});

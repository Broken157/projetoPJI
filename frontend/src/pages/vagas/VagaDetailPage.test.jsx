import { act, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ApiError from '../../services/api/ApiError';
import apiClient from '../../services/api/apiClient';
import VagaDetailPage from './VagaDetailPage';

jest.mock('../../services/api/apiClient', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

const vaga = {
  id: 42,
  titulo: 'Guitarrista para festival',
  nomeContratante: 'Produções Aurora',
  cidade: 'São Paulo',
  estado: 'SP',
  modeloTrabalho: 'PRESENCIAL',
  remuneraValor: 1234.56,
  descricao: 'Apresentação no palco principal.',
  requisitos: 'Experiência com repertório autoral.',
  status: 'ABERTA',
  tagIds: [2, 7],
};

function renderPage(path = '/vagas/42', routePath = '/vagas/:id') {
  return render(
    <MemoryRouter
      initialEntries={[path]}
      future={{ v7_relativeSplatPath: true, v7_startTransition: true }}
    >
      <Routes>
        <Route path={routePath} element={<VagaDetailPage />} />
      </Routes>
    </MemoryRouter>
  );
}

beforeEach(() => {
  apiClient.get.mockReset();
  apiClient.post.mockReset();
  window.localStorage.clear();
  window.sessionStorage.clear();
  document.title = 'Palco';
});

test('ID válido realiza o GET correto', async () => {
  apiClient.get.mockResolvedValue(vaga);
  renderPage('/vagas/42');

  await screen.findByRole('heading', { name: vaga.titulo });
  expect(apiClient.get).toHaveBeenCalledTimes(1);
  expect(apiClient.get).toHaveBeenCalledWith('/vagas/42');
});

test('exibe loading enquanto a resposta é aguardada', async () => {
  let resolveRequest;
  apiClient.get.mockReturnValue(
    new Promise((resolve) => {
      resolveRequest = resolve;
    })
  );
  renderPage();

  expect(screen.getByRole('status')).toHaveTextContent('Carregando vaga…');

  await act(async () => resolveRequest(vaga));
  expect(await screen.findByRole('heading', { name: vaga.titulo })).toBeInTheDocument();
});

test('renderiza o título retornado', async () => {
  apiClient.get.mockResolvedValue(vaga);
  renderPage();

  expect(await screen.findByRole('heading', { name: vaga.titulo })).toBeInTheDocument();
});

test('renderiza o nome do contratante', async () => {
  apiClient.get.mockResolvedValue(vaga);
  renderPage();

  expect(await screen.findByText('Produções Aurora')).toBeInTheDocument();
});

test('renderiza cidade e estado', async () => {
  apiClient.get.mockResolvedValue(vaga);
  renderPage();

  expect(await screen.findByText(/São Paulo\/SP/)).toBeInTheDocument();
});

test('formata a remuneração em reais', async () => {
  apiClient.get.mockResolvedValue(vaga);
  renderPage();

  expect(await screen.findByText(/R\$\s*1\.234,56/)).toBeInTheDocument();
});

test('renderiza a descrição', async () => {
  apiClient.get.mockResolvedValue(vaga);
  renderPage();

  expect(await screen.findByText(vaga.descricao)).toBeInTheDocument();
});

test('renderiza os requisitos', async () => {
  apiClient.get.mockResolvedValue(vaga);
  renderPage();

  expect(await screen.findByText(vaga.requisitos)).toBeInTheDocument();
});

test('não quebra quando campos opcionais estão ausentes', async () => {
  apiClient.get.mockResolvedValue({
    titulo: 'Vaga sem opcionais',
    descricao: 'Descrição disponível.',
    requisitos: 'Requisitos disponíveis.',
    status: 'ABERTA',
  });
  renderPage();

  expect(await screen.findByRole('heading', { name: 'Vaga sem opcionais' })).toBeInTheDocument();
  expect(screen.queryByLabelText('Áreas da vaga')).not.toBeInTheDocument();
});

test('renderiza as tags quando presentes', async () => {
  apiClient.get.mockResolvedValue(vaga);
  renderPage();

  expect(await screen.findByText('Área #2')).toBeInTheDocument();
  expect(screen.getByText('Área #7')).toBeInTheDocument();
});

test('404 produz o estado NotFound da vaga', async () => {
  apiClient.get.mockRejectedValue(
    new ApiError({ status: 404, message: 'Vaga não encontrada.', body: null })
  );
  renderPage();

  expect(
    await screen.findByRole('heading', { name: 'Não foi possível abrir esta vaga' })
  ).toBeInTheDocument();
  expect(screen.getByText('Vaga não encontrada.')).toBeInTheDocument();
  expect(
    within(screen.getByRole('alert')).getByRole('link', { name: 'Voltar ao painel' })
  ).toHaveAttribute('href', '/dashboard-contratante.html');
});

test('500 produz o ErrorState preservando a mensagem', async () => {
  apiClient.get.mockRejectedValue(
    new ApiError({ status: 500, message: 'Falha interna temporária.', body: null })
  );
  renderPage();

  expect(
    await screen.findByRole('heading', { name: 'Não foi possível abrir esta vaga' })
  ).toBeInTheDocument();
  expect(screen.getByRole('alert')).toHaveTextContent('Falha interna temporária.');
});

test('ausência de ID não chama a API', () => {
  renderPage('/vagas', '/vagas');

  expect(
    screen.getByRole('heading', { name: 'Não foi possível abrir esta vaga' })
  ).toBeInTheDocument();
  expect(screen.getByRole('alert')).toHaveTextContent('Identificador de vaga inválido.');
  expect(apiClient.get).not.toHaveBeenCalled();
});

test('ID inválido não chama a API', () => {
  renderPage('/vagas/inválido');

  expect(screen.getByRole('alert')).toHaveTextContent('Identificador de vaga inválido.');
  expect(apiClient.get).not.toHaveBeenCalled();
});

test('renderiza caracteres especiais como texto sem criar HTML', async () => {
  const untrustedText = '<script>window.comprometido = true</script> & Música';
  apiClient.get.mockResolvedValue({ ...vaga, descricao: untrustedText });
  const { container } = renderPage();

  expect(await screen.findByText(untrustedText)).toBeInTheDocument();
  expect(container.querySelector('script')).not.toBeInTheDocument();
  expect(window.comprometido).toBeUndefined();
});

test('visitante anônimo consegue carregar a tela', async () => {
  expect(window.localStorage).toHaveLength(0);
  expect(window.sessionStorage).toHaveLength(0);
  apiClient.get.mockResolvedValue(vaga);
  renderPage();

  expect(await screen.findByRole('heading', { name: vaga.titulo })).toBeInTheDocument();
  expect(apiClient.get).toHaveBeenCalledWith('/vagas/42');
});

test.each([
  [401, 'Não autenticado.'],
  [403, 'Acesso negado.'],
  [422, 'Identificador recusado.'],
])('não mascara o erro HTTP %i', async (status, message) => {
  apiClient.get.mockRejectedValue(new ApiError({ status, message, body: null }));
  renderPage(`/vagas/${status}`);

  await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent(message));
});

import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import apiClient from '../../services/api/apiClient';
import HomePage from './HomePage';

jest.mock('../../services/api/apiClient', () => ({
  __esModule: true,
  default: { get: jest.fn() },
}));

const vacancy = {
  id: 31,
  titulo: 'Cantora para festival',
  nomeContratante: 'Aurora Cultural',
  cidade: 'Recife',
  estado: 'PE',
  modeloTrabalho: 'PRESENCIAL',
  tipoContrato: 'EVENTO',
  remuneraValor: 900,
  descricao: 'Apresentação no palco principal.',
};

function LocationProbe() {
  const location = useLocation();
  return <output data-testid="location">{location.pathname}{location.search}</output>;
}

function renderHome() {
  return render(
    <MemoryRouter future={{ v7_relativeSplatPath: true, v7_startTransition: true }}>
      <HomePage />
      <LocationProbe />
    </MemoryRouter>
  );
}

beforeEach(() => {
  apiClient.get.mockReset();
  window.localStorage.clear();
  window.sessionStorage.clear();
  window.matchMedia = jest.fn(() => ({ matches: true }));
  window.scrollTo = jest.fn();
});

test('carrega feed público real e cria destino React com ID real', async () => {
  apiClient.get.mockResolvedValue({ content: [vacancy] });
  renderHome();

  expect(await screen.findByRole('heading', { name: vacancy.titulo })).toBeInTheDocument();
  expect(apiClient.get).toHaveBeenCalledWith('/vagas?size=8');
  expect(screen.getByRole('link', { name: 'Ver vaga' })).toHaveAttribute('href', '/vagas/31');
  expect(screen.getByText('Aurora Cultural', { exact: false })).toBeInTheDocument();
});

test('exibe loading e substitui a demonstração quando a API responde', async () => {
  let resolveRequest;
  apiClient.get.mockReturnValue(new Promise((resolve) => { resolveRequest = resolve; }));
  renderHome();

  expect(screen.getByText('Carregando oportunidades…')).toBeInTheDocument();
  expect(screen.getAllByRole('link', { name: 'Ver vaga' })[0]).toHaveAttribute('aria-disabled', 'true');

  await act(async () => resolveRequest({ content: [vacancy] }));
  expect(await screen.findByRole('heading', { name: vacancy.titulo })).toBeInTheDocument();
});

test.each([
  [{ content: [] }, 'Nenhuma vaga aberta disponível no momento.'],
  [new Error('Serviço temporariamente indisponível'), 'Serviço temporariamente indisponível'],
])('trata feed vazio ou erro sem inventar links reais', async (result, message) => {
  if (result instanceof Error) apiClient.get.mockRejectedValue(result);
  else apiClient.get.mockResolvedValue(result);
  renderHome();

  expect(await screen.findByText(message)).toBeInTheDocument();
  screen.getAllByRole('link', { name: 'Ver vaga' }).forEach((link) => {
    expect(link).toHaveAttribute('aria-disabled', 'true');
    expect(link).not.toHaveAttribute('href');
  });
});

test('envia a busca da Home para /vagas usando query string', async () => {
  apiClient.get.mockResolvedValue({ content: [] });
  renderHome();
  await screen.findByText('Nenhuma vaga aberta disponível no momento.');

  fireEvent.change(screen.getByPlaceholderText('Busque por título da vaga'), {
    target: { value: 'atriz musical' },
  });
  fireEvent.submit(screen.getByRole('search'));

  await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/vagas?titulo=atriz%20musical'));
});

test('renderiza conteúdo malicioso como texto e mantém CTAs pendentes desabilitados', async () => {
  const malicious = '<script>window.__rf03_pwned = true</script>';
  apiClient.get.mockResolvedValue({ content: [{ ...vacancy, titulo: malicious }] });
  renderHome();

  expect(await screen.findByText(malicious)).toBeInTheDocument();
  expect(document.querySelector('.vaga-mini script')).toBeNull();
  expect(screen.getByRole('link', { name: 'Artistas' })).toHaveAttribute('aria-disabled', 'true');
});

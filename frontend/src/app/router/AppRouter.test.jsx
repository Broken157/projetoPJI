import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AppRoutes } from './AppRouter';

jest.mock('../../pages/vagas/VagaDetailPage', () => {
  const { useParams } = jest.requireActual('react-router-dom');

  return function VagaDetailPageMock() {
    const { id } = useParams();
    return <h1>Detalhe da vaga {id}</h1>;
  };
});

test('renderiza a rota técnica inicial', () => {
  render(
    <MemoryRouter
      initialEntries={['/']}
      future={{ v7_relativeSplatPath: true, v7_startTransition: true }}
    >
      <AppRoutes />
    </MemoryRouter>
  );

  expect(screen.getByRole('heading', { name: /fundação react do palco/i })).toBeInTheDocument();
});

test('renderiza o fallback para uma rota React inexistente', () => {
  render(
    <MemoryRouter
      initialEntries={['/rota-inexistente']}
      future={{ v7_relativeSplatPath: true, v7_startTransition: true }}
    >
      <AppRoutes />
    </MemoryRouter>
  );

  expect(screen.getByRole('heading', { name: /página não encontrada/i })).toBeInTheDocument();
});

test('monta a página de detalhe e entrega o parâmetro da rota', () => {
  render(
    <MemoryRouter
      initialEntries={['/vagas/77']}
      future={{ v7_relativeSplatPath: true, v7_startTransition: true }}
    >
      <AppRoutes />
    </MemoryRouter>
  );

  expect(screen.getByRole('heading', { name: 'Detalhe da vaga 77' })).toBeInTheDocument();
});

import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AppRoutes } from './AppRouter';

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

import { render, screen } from '@testing-library/react';
import ErrorState from './ErrorState';
import LoadingState from './LoadingState';
import NotFound from './NotFound';

test('LoadingState expõe um status acessível', () => {
  render(<LoadingState message="Carregando vaga…" />);

  expect(screen.getByRole('status')).toHaveTextContent('Carregando vaga…');
});

test('ErrorState preserva a mensagem de erro recebida', () => {
  render(<ErrorState error={new Error('Falha específica.')} />);

  expect(screen.getByRole('alert')).toHaveTextContent('Falha específica.');
});

test('NotFound informa a ausência da rota', () => {
  render(<NotFound />);

  expect(screen.getByRole('heading', { name: /página não encontrada/i })).toBeInTheDocument();
});

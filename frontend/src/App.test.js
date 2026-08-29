import { render, screen } from '@testing-library/react';
import App from './app/App';

test('monta a fundação React na rota inicial', () => {
  window.history.pushState({}, '', '/');
  render(<App />);
  expect(screen.getByRole('heading', { name: /fundação react do palco/i })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /abrir home legada/i })).toHaveAttribute(
    'href',
    '/home.html'
  );
});

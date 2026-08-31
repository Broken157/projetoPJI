import { render, screen } from '@testing-library/react';
import App from './app/App';

jest.mock('./pages/home/HomePage', () => function HomePageMock() {
  return <h1>Home Palco React</h1>;
});

test('monta a Home React na rota inicial', () => {
  window.history.pushState({}, '', '/');
  render(<App />);
  expect(screen.getByRole('heading', { name: 'Home Palco React' })).toBeInTheDocument();
});

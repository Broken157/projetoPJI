import { fireEvent, render, screen } from '@testing-library/react';
import BackToTopButton from './BackToTopButton';

beforeEach(() => {
  Object.defineProperty(window, 'scrollY', { configurable: true, writable: true, value: 0 });
  window.requestAnimationFrame = (callback) => { callback(); return 1; };
  window.cancelAnimationFrame = jest.fn();
  window.matchMedia = jest.fn(() => ({ matches: true }));
  window.scrollTo = jest.fn();
});

test('surge somente após 300 px e volta ao topo respeitando movimento reduzido', () => {
  render(<BackToTopButton />);
  expect(screen.queryByRole('button', { name: /voltar ao topo/i })).not.toBeInTheDocument();

  window.scrollY = 301;
  fireEvent.scroll(window);

  fireEvent.click(screen.getByRole('button', { name: /voltar ao topo/i }));
  expect(window.scrollTo).toHaveBeenCalledWith({ top: 0, left: 0, behavior: 'auto' });
});

import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { logoutCurrentSession } from '../../services/account/logoutService';
import LogoutButton from './LogoutButton';

jest.mock('../../services/account/logoutService', () => ({ logoutCurrentSession: jest.fn() }));

beforeEach(() => logoutCurrentSession.mockReset());

function renderButton() {
  return render(<MemoryRouter initialEntries={['/dashboard']} future={{ v7_relativeSplatPath: true, v7_startTransition: true }}><Routes><Route path="/dashboard" element={<LogoutButton />} /><Route path="/login" element={<h1>Login</h1>} /></Routes></MemoryRouter>);
}

test('bloqueia duplo clique durante logout e redireciona', async () => {
  let resolve;
  logoutCurrentSession.mockReturnValue(new Promise((done) => { resolve = done; }));
  renderButton();
  const button = screen.getByRole('button', { name: 'Sair' });
  fireEvent.click(button);
  fireEvent.click(button);
  expect(logoutCurrentSession).toHaveBeenCalledTimes(1);
  expect(screen.getByRole('button', { name: 'Saindo…' })).toBeDisabled();
  resolve();
  expect(await screen.findByRole('heading', { name: 'Login' })).toBeInTheDocument();
});

test('erro remoto também redireciona após a limpeza feita pelo serviço', async () => {
  logoutCurrentSession.mockRejectedValue(new Error('offline'));
  renderButton();
  fireEvent.click(screen.getByRole('button', { name: 'Sair' }));
  await waitFor(() => expect(screen.getByRole('heading', { name: 'Login' })).toBeInTheDocument());
});

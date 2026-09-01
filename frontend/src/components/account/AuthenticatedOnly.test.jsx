import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import sessionService from '../../auth/sessionService';
import AuthenticatedOnly from './AuthenticatedOnly';

jest.mock('../../auth/sessionService', () => ({ getAccessToken: jest.fn() }));

test('redireciona anônimo para login', () => {
  sessionService.getAccessToken.mockReturnValue(null);
  render(<MemoryRouter initialEntries={['/perfil']} future={{ v7_relativeSplatPath: true, v7_startTransition: true }}><Routes><Route path="/perfil" element={<AuthenticatedOnly><h1>Privado</h1></AuthenticatedOnly>} /><Route path="/login" element={<h1>Login</h1>} /></Routes></MemoryRouter>);
  expect(screen.getByRole('heading', { name: 'Login' })).toBeInTheDocument();
  expect(screen.queryByText('Privado')).not.toBeInTheDocument();
});

test('permite usuário autenticado de qualquer papel', () => {
  sessionService.getAccessToken.mockReturnValue('jwt');
  render(<MemoryRouter future={{ v7_relativeSplatPath: true, v7_startTransition: true }}><AuthenticatedOnly><h1>Privado</h1></AuthenticatedOnly></MemoryRouter>);
  expect(screen.getByRole('heading', { name: 'Privado' })).toBeInTheDocument();
});

import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { SESSION_STORAGE_KEY } from '../../auth/sessionService';
import ContractorOnly from './ContractorOnly';

function renderProtected() {
  return render(
    <MemoryRouter initialEntries={['/minhas-vagas']} future={{ v7_relativeSplatPath: true, v7_startTransition: true }}>
      <Routes>
        <Route path="/login" element={<h1>Login</h1>} />
        <Route path="/minhas-vagas" element={<ContractorOnly><h1>Gestão liberada</h1></ContractorOnly>} />
      </Routes>
    </MemoryRouter>
  );
}

beforeEach(() => {
  window.sessionStorage.clear();
  window.localStorage.clear();
});

test('anônimo é encaminhado ao login sem renderizar conteúdo protegido', () => {
  renderProtected();
  expect(screen.getByRole('heading', { name: 'Login' })).toBeInTheDocument();
  expect(screen.queryByText('Gestão liberada')).not.toBeInTheDocument();
});

test('ARTISTA autenticado recebe bloqueio de papel', () => {
  window.sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify({ token: 'jwt', tipoUsuario: 'ARTISTA' }));
  renderProtected();
  expect(screen.getByRole('heading', { name: 'Área exclusiva para contratantes' })).toBeInTheDocument();
  expect(screen.queryByText('Gestão liberada')).not.toBeInTheDocument();
});

test('CONTRATANTE autenticado acessa a gestão', () => {
  window.sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify({ token: 'jwt', tipoUsuario: 'CONTRATANTE' }));
  renderProtected();
  expect(screen.getByRole('heading', { name: 'Gestão liberada' })).toBeInTheDocument();
});

import { BrowserRouter, Route, Routes } from 'react-router-dom';
import NotFound from '../../components/common/NotFound';
import ForgotPasswordPage from '../../pages/auth/ForgotPasswordPage';
import ResetPasswordPage from '../../pages/auth/ResetPasswordPage';
import PublicProfilePage from '../../pages/perfis/PublicProfilePage';
import VagaDetailPage from '../../pages/vagas/VagaDetailPage';
import ReactFoundationPage from '../ReactFoundationPage';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<ReactFoundationPage />} />
      <Route path="/recuperar-senha" element={<ForgotPasswordPage />} />
      <Route path="/redefinir-senha" element={<ResetPasswordPage />} />
      <Route path="/vagas/:id" element={<VagaDetailPage />} />
      <Route path="/perfis/:tipo/:id" element={<PublicProfilePage />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}

export default function AppRouter() {
  return (
    <BrowserRouter future={{ v7_relativeSplatPath: true, v7_startTransition: true }}>
      <AppRoutes />
    </BrowserRouter>
  );
}

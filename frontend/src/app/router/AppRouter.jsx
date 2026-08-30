import { BrowserRouter, Route, Routes } from 'react-router-dom';
import NotFound from '../../components/common/NotFound';
import VagaDetailPage from '../../pages/vagas/VagaDetailPage';
import ReactFoundationPage from '../ReactFoundationPage';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<ReactFoundationPage />} />
      <Route path="/vagas/:id" element={<VagaDetailPage />} />
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

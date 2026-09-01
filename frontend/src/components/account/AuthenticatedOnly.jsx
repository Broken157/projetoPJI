import { Navigate, useLocation } from 'react-router-dom';
import sessionService from '../../auth/sessionService';

export default function AuthenticatedOnly({ children }) {
  const location = useLocation();
  if (!sessionService.getAccessToken()) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return children;
}

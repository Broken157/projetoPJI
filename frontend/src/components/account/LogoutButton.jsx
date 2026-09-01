import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { logoutCurrentSession } from '../../services/account/logoutService';

export default function LogoutButton({ className = 'account-logout' }) {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  async function handleLogout() {
    if (loading) return;
    setLoading(true);
    try {
      await logoutCurrentSession();
    } catch {
      // A limpeza local ocorre no finally do serviço e nunca deixa a sessão utilizável.
    } finally {
      navigate('/login', { replace: true });
    }
  }

  return (
    <button className={className} type="button" onClick={handleLogout} disabled={loading}>
      {loading ? 'Saindo…' : 'Sair'}
    </button>
  );
}

import sessionService from '../../auth/sessionService';

export default function ContractorVacancyLayout({ children }) {
  function logout() {
    sessionService.clearLocalSession();
    window.location.assign('/login');
  }

  return (
    <div className="pagina-app management-shell">
      <header>
        <nav className="app-navbar management-navbar" aria-label="Navegação da gestão de vagas">
          <a href="/dashboard-contratante.html" aria-label="Palco — painel do contratante">
            <img className="navbar__logo" src="/assets/logo-palco.png" alt="Palco" />
          </a>
          <ul className="app-navbar__menu">
            <li><a className="navbar__link navbar__link--destaque" href="/minhas-vagas">Minhas vagas</a></li>
            <li><a className="navbar__link" href="/vagas/nova">Publicar vaga</a></li>
          </ul>
          <div className="app-navbar__acoes management-navbar__actions">
            <a className="navbar__link" href="/dashboard-contratante.html">Painel</a>
            <button className="management-logout" type="button" onClick={logout}>Sair</button>
          </div>
        </nav>
      </header>
      {children}
    </div>
  );
}

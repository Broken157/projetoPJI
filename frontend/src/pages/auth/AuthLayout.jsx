export default function AuthLayout({ children, currentPage }) {
  return (
    <div className="auth-page">
      <header>
        <nav className="navbar" aria-label="Navegação principal">
          <a href="/">
            <img className="navbar__logo" src="/assets/logo-palco.png" alt="Palco" />
          </a>
          <ul className="navbar__menu">
            <li><a className="navbar__link navbar__link--destaque" href="/vagas">Explorar</a></li>
            <li><a className="navbar__link" href="/comunidade.html">Comunidade</a></li>
            <li><a className="navbar__link" href="/empresa.html">Empresa</a></li>
          </ul>
          <div className="navbar__acoes">
            <a className="navbar__link" href="/login" aria-current={currentPage === 'login' ? 'page' : undefined}>Login</a>
            <a className="navbar__link navbar__link--destaque" href="/cadastro" aria-current={currentPage === 'cadastro' ? 'page' : undefined}>Cadastrar</a>
          </div>
        </nav>
      </header>
      {children}
    </div>
  );
}

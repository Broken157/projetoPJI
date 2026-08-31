export default function PasswordRecoveryLayout({ children }) {
  return (
    <div className="pagina-login auth-recovery">
      <header>
        <nav className="navbar" aria-label="Navegação principal">
          <a href="/index.html">
            <img className="navbar__logo" src="/assets/logo-palco.png" alt="Palco" />
          </a>
          <ul className="navbar__menu">
            <li>
              <a className="navbar__link navbar__link--destaque" href="/explorar.html">
                Explorar
              </a>
            </li>
            <li>
              <a className="navbar__link" href="/comunidade.html">
                Comunidade
              </a>
            </li>
            <li>
              <a className="navbar__link" href="/empresa.html">
                Empresa
              </a>
            </li>
          </ul>
          <div className="navbar__acoes">
            <a className="navbar__link" href="/login">
              Login
            </a>
            <a
              className="navbar__link navbar__link--destaque"
              href="/cadastro"
            >
              Cadastrar
            </a>
          </div>
        </nav>
      </header>
      {children}
    </div>
  );
}

import React, { useState } from 'react';

// Substitua estes callbacks pelas rotas e serviços da sua aplicação.
export default function App({ onLogin, onGoogleLogin, onNavigate, hideHeader = false }) {
  const [showPassword, setShowPassword] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [connectionsOpen, setConnectionsOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  function navigate(event, destination) {
    if (onNavigate) { event.preventDefault(); onNavigate(destination); return; }
    event.preventDefault();
    setMessage(`A página “${destination}” ainda não está conectada a este front-end.`);
    setMenuOpen(false);
  }
  function link(label, path, className = '') {
    return <a className={className} href={path} onClick={event => navigate(event, label)}>{label}</a>;
  }
  async function submit(event) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    if (!onLogin) { setMessage('Campos válidos. Conecte o serviço de autenticação para entrar.'); return; }
    setBusy(true); setMessage('');
    try { await onLogin({ email: data.get('email'), password: data.get('password') }); }
    catch { setMessage('Não foi possível entrar. Confira seus dados e tente novamente.'); }
    finally { setBusy(false); }
  }
  async function googleLogin() {
    if (!onGoogleLogin) { setMessage('O login com Google precisa ser conectado ao serviço de autenticação.'); return; }
    setBusy(true); setMessage('');
    try { await onGoogleLogin(); }
    catch { setMessage('Não foi possível entrar com Google. Tente novamente.'); }
    finally { setBusy(false); }
  }

  return <div className="page">
    <a className="skip-link" href="#login">Ir para o login</a>
    {!hideHeader && <header className="header">
      <a className="logo" href="/" aria-label="Palco — início" onClick={e => navigate(e, 'Home')}><img src="/assets/logo.png" alt="" /></a>
      <button className="menu-toggle" aria-label={menuOpen ? 'Fechar menu' : 'Abrir menu'} aria-expanded={menuOpen} aria-controls="navigation" onClick={() => setMenuOpen(!menuOpen)}>{menuOpen ? '✕' : '☰'}</button>
      <nav id="navigation" className={menuOpen ? 'navigation open' : 'navigation'} aria-label="Navegação principal">
        {link('Home', '/')}{link('Artistas', '/artistas')}{link('Contratantes', '/contratantes')}{link('Vagas', '/vagas')}
        <div className="connections"><button aria-expanded={connectionsOpen} aria-controls="connections-menu" onClick={() => setConnectionsOpen(!connectionsOpen)} onKeyDown={e => { if(e.key === 'Escape') setConnectionsOpen(false); }}>Conexões <span className="chevron" /></button>
          {connectionsOpen && <div id="connections-menu" className="dropdown">{link('Minhas conexões', '/conexoes')}{link('Mensagens', '/mensagens')}</div>}
        </div>
        {link('Holofotes', '/holofotes')}{link('Sobre nós', '/sobre')}
      </nav>
      <div className="account"><a className="current" href="#login" aria-current="page">Login</a>{link('Cadastro', '/cadastro', 'register')}</div>
    </header>}
    <main id="login" className="main">
      <section className="login" aria-labelledby="title">
        <div className="brand"><h1 id="title">Palco</h1><p>Onde talentos encontram oportunidades.</p></div>
        <form onSubmit={submit}>
          <div className="field"><label htmlFor="email">E-mail</label><input id="email" name="email" type="email" placeholder="rayzaablo@gmail.com" autoComplete="email" required /></div>
          <div className="field password-field"><label htmlFor="password">Senha</label><div className="password-input"><input id="password" name="password" type={showPassword ? 'text' : 'password'} placeholder="Senha" autoComplete="current-password" required /><button className="eye" type="button" aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'} aria-pressed={showPassword} onClick={() => setShowPassword(!showPassword)}><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true"><path d="M2 12s3-8 10-8 10 8 10 8-3 8-10 8S2 12 2 12Z"/><circle cx="12" cy="12" r="3.5"/>{showPassword && <path d="m3 3 18 18"/>}</svg></button></div></div>
          {link('Esqueceu a senha?', '/recuperar-senha', 'forgot')}
          <button className="submit" type="submit" disabled={busy}>{busy ? 'Entrando…' : 'Entrar'}</button>
          <button className="google" type="button" disabled={busy} onClick={googleLogin}><img src="/assets/google.png" alt="" />Continuar com google</button>
        </form>
        <p className="signup">Ainda não está na Palco? {link('Crie uma conta.', '/cadastro')}</p>
        <p className="terms">Ao continuar, você concorda com os {link('Termos de Serviço', '/termos')} do Palco e confirma que leu nossa {link('Política de Privacidade', '/privacidade')}. Aviso na coleta de informações.</p>
        <p className="feedback" role="status" aria-live="polite">{message}</p>
      </section>
    </main>
  </div>;
}

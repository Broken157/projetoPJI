import React,{useEffect,useRef,useState} from 'react';
import Icon from './Icon';
import './Navbar.css';

export default function Navbar({navigate,notify=()=>{},loggedIn=true,user,onLogout}){
  const [open,setOpen]=useState(null);
  const [mobile,setMobile]=useState(false);
  const root=useRef();
  const trigger=useRef();
  useEffect(()=>{
    function outside(event){if(!root.current?.contains(event.target))setOpen(null)}
    function escape(event){if(event.key==='Escape'){setOpen(null);setMobile(false);trigger.current?.focus()}}
    document.addEventListener('pointerdown',outside);document.addEventListener('keydown',escape);
    return()=>{document.removeEventListener('pointerdown',outside);document.removeEventListener('keydown',escape)};
  },[]);
  function toggle(name,event){trigger.current=event.currentTarget;setOpen(open===name?null:name)}
  function go(path){setOpen(null);setMobile(false);navigate(path)}
  function unavailable(label){setOpen(null);setMobile(false);notify(`“${label}” está indisponível nesta versão.`)}
  const link=(label,path)=> <a href={path} onClick={e=>{e.preventDefault();go(path)}}>{label}</a>;
  return <header className="palco-nav" ref={root}>
    <a className="palco-nav-logo" href={loggedIn?'/dashboard':'/login'} onClick={e=>{e.preventDefault();go(loggedIn?'/dashboard':'/login')}} aria-label="Palco — início"><img src="/assets/logo.png" alt="Palco"/></a>
    <button className="palco-nav-mobile" aria-label={mobile?'Fechar navegação':'Abrir navegação'} aria-expanded={mobile} aria-controls="palco-navigation" onClick={()=>setMobile(!mobile)}>{mobile?'×':'☰'}</button>
    <nav id="palco-navigation" className={mobile?'expanded':''} aria-label="Navegação principal">
      {link('Início',loggedIn?'/dashboard':'/login')}{link('Artistas',user?.role==='contratante'?'/talentos':'/perfil')}
      {['Contratantes','Vagas','Editais'].map(label=><button key={label} onClick={()=>label==='Vagas'?go('/vagas'):label==='Contratantes'&&user?.role==='contratante'?go('/contratante/perfil'):unavailable(label)}>{label}</button>)}
      <div className="nav-connections"><button className={open==='connections'?'active':''} aria-expanded={open==='connections'} aria-controls="connections-panel" onClick={e=>toggle('connections',e)}>Conexões <span className="nav-chevron"/></button>
        {open==='connections'&&<div id="connections-panel" className="connections-panel"><button onClick={()=>go('/comunidades')}><strong>Comunidades</strong><span>Participe de comunidades de acordo com seus interesses</span></button><button onClick={()=>go('/galeria')}><strong>Galeria Virtual</strong><span>Espaço para exposição e interação com obras.</span></button></div>}
      </div>
      <button onClick={()=>unavailable('Holofotes')}>Holofotes</button>
    </nav>
    {loggedIn?<div className="palco-nav-tools"><button aria-label="Meus Salvos" onClick={()=>go('/salvos')}><Icon name="star"/></button><button className="nav-bell" aria-label="Notificações" onClick={()=>go('/notificacoes')}><Icon name="bell"/></button><button className="nav-account" aria-label="Menu da conta" aria-expanded={open==='account'} aria-controls="account-panel" onClick={e=>toggle('account',e)}><Icon name="user"/></button>
      {open==='account'&&<div id="account-panel" className="account-panel"><div className="account-identity"><strong>{user?.name||'Minha conta'}</strong><span>{user?.role==='contratante'?'Contratante':'Artista independente'}</span></div>{[['user','Meu perfil',user?.role==='contratante'?'/contratante/perfil':'/perfil'],['message','Mensagens','/mensagens'],['shield','Denúncia'],['requests','Solicitações'],['support','Suporte'],['calendar','Minha agenda','/agenda'],['gear','Excluir conta','/conta/excluir']].filter(([,label])=>label!=='Minha agenda'||user?.role==='artista').map(([icon,label,path])=><button key={label} onClick={()=>path?go(path):unavailable(label)}><Icon name={icon}/>{label}</button>)}<button className="account-exit" onClick={()=>{setOpen(null);onLogout?onLogout():go('/login')}}><Icon name="logout"/><span>Sair</span></button></div>}
    </div>:<div className="nav-guest"><a href="/login" onClick={e=>{e.preventDefault();go('/login')}}>Login</a><button onClick={()=>go('/cadastro')}>Cadastro</button></div>}
  </header>;
}

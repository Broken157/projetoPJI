import React,{useState,useEffect,useRef} from 'react';
import AuthScreens from './pages/AuthScreens';
import Dashboard from './pages/Dashboard';
import HirerDashboard from './pages/HirerDashboard';
import TalentBank from './pages/TalentBank';
import Inbox from './pages/Inbox';
import Terms from './pages/Terms';
import Community from './pages/Community';
import Agenda from './pages/Agenda';
import DeleteAccount from './pages/DeleteAccount';
import Profile from './pages/Profile';
import Portfolio from './pages/Portfolio';
import Saved from './pages/Saved';
import Notifications from './pages/Notifications';
import Registration from './pages/Registration';
import Vacancies,{Applications} from './pages/Vacancies';
import {getSession,logout,isPublicRoute,canAccessRoute,canonicalUser} from './lib/auth';
import useResource from './lib/useResource';
import Navbar from './components/Navbar';
import './portfolio.css';
import './portfolio-v2.css';
import './auth-dashboard.css';
import './hirer.css';
import './community.css';
import './integration.css';
export default function App(){
  const [path,setPath]=useState(location.pathname==='/'?'/dashboard':location.pathname),[revision,setRevision]=useState(0);
  const [user,setUser]=useState(null),[ready,setReady]=useState(false),[sessionError,setSessionError]=useState(''),[toast,setToast]=useState('');
  const timer=useRef();
  const capabilities=useResource('/capacidades');
  function notify(message){setToast(message);clearTimeout(timer.current);timer.current=setTimeout(()=>setToast(''),7000);}
  function navigate(url){
    const next=new URL(url,location.origin);if(next.origin!==location.origin)return;
    history.pushState({},'',next.pathname+next.search);setPath(next.pathname==='/'?'/dashboard':next.pathname);setRevision(r=>r+1);window.scrollTo(0,0);
  }
  useEffect(()=>{const pop=()=>{setPath(location.pathname==='/'?'/dashboard':location.pathname);setRevision(n=>n+1);};window.addEventListener('popstate',pop);return()=>{window.removeEventListener('popstate',pop);clearTimeout(timer.current);};},[]);
  useEffect(()=>{let active=true;async function refresh(){try{const account=await getSession();if(active){setUser(account);setSessionError('');}}catch(e){if(active)setSessionError(e.message);}finally{if(active)setReady(true);}}
    const ended=()=>{setUser(null);setReady(true);};
    refresh();window.addEventListener('focus',refresh);window.addEventListener('palco:session-ended',ended);
    return()=>{active=false;window.removeEventListener('focus',refresh);window.removeEventListener('palco:session-ended',ended);};
  },[]);
  useEffect(()=>{if(!ready)return;if(!user&&!isPublicRoute(path)){history.replaceState({},'','/login');setPath('/login');}
    else if(user&&!canAccessRoute(user,path)){history.replaceState({},'','/dashboard');setPath('/dashboard');notify('Esta função não está disponível para o seu tipo de conta.');}
  },[ready,user,path]);
  async function signOut(){try{await logout();setUser(null);navigate('/login');}catch(e){notify(e.message);}}
  function authenticated(account){setUser(account);navigate('/dashboard');}
  if(!ready)return <div className="pf-app session-loading" role="status"><img src="/assets/logo.png" alt="Palco"/><p>Carregando…</p></div>;
  if((!user&&!isPublicRoute(path))||(user&&!canAccessRoute(user,path)))return null;
  const shared={user,navigate,notify,capabilities:capabilities.data};
  let content;
  if(['/login','/recuperar-senha','/redefinir-senha'].includes(path))content=<AuthScreens key={path} path={path} navigate={navigate} onAuthenticated={authenticated} onSessionCleared={()=>setUser(null)} notify={notify}/>;
  else if(path==='/cadastro')content=<Registration {...shared}/>;
  else if(path==='/termos'||path==='/privacidade')content=<Terms privacy={path==='/privacidade'}/>;
  else if(path==='/dashboard')content=user.role==='artista'?<Dashboard {...shared}/>:<HirerDashboard {...shared}/>;
  else if(path==='/agenda')content=<Agenda {...shared}/>;
  else if(path==='/conta/excluir')content=<DeleteAccount {...shared}/>;
  else if(path==='/comunidades'||path==='/comunidade2')content=<Community key={path+location.search} {...shared} discussion={path==='/comunidade2'}/>;
  else if(path==='/mensagens')content=<Inbox key={user.id+location.search} {...shared}/>;
  else if(path==='/talentos')content=capabilities.data?.taxonomia===false?<main className="talent-bank"><header><h1>BANCO DE<br/>TALENTOS</h1></header><section className="talent-filter-panel"><p role="status">A busca de talentos está indisponível nesta versão.</p><p>Área, função e especialização ainda não estão disponíveis no cadastro de artistas.</p><button onClick={()=>navigate('/salvos')}>Ver perfis salvos</button></section></main>:<TalentBank {...shared}/>;
  else if(path==='/salvos')content=<Saved {...shared}/>;
  else if(path==='/notificacoes')content=<Notifications {...shared}/>;
  else if(path==='/candidaturas')content=<main className="dashboard"><Applications {...shared}/></main>;
  else if(path.startsWith('/portfolio/'))content=<Portfolio {...shared} artistId={user.id} own editor/>;
  else if(['/perfil','/perfil/editar','/galeria','/contratante/perfil'].includes(path)||/^\/perfis\/(artista|contratante)\/\d+$/i.test(path))content=<Profile key={path+revision} {...shared} path={path} onUserChange={u=>setUser(canonicalUser(u))}/>;
  else if(path==='/minhas-vagas'||/^\/vagas(?:\/(nova|\d+)(?:\/(editar|gerenciar))?)?$/.test(path))content=<Vacancies key={path} {...shared} path={path}/>;
  else content=<main className="dashboard"><h1>Página não encontrada</h1><button onClick={()=>navigate(user?'/dashboard':'/login')}>Voltar ao início</button></main>;
  return <div className="pf-app"><Navbar {...shared} loggedIn={!!user} onLogout={signOut}/>{sessionError&&<p role="alert" className="auth-form-error">{sessionError}</p>}{content}{toast&&<div className="pf-toast" role="status">{toast}<button aria-label="Fechar aviso" onClick={()=>setToast('')}>×</button></div>}</div>;
}

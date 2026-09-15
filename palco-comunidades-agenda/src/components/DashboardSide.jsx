import React from 'react';
import Icon from './Icon';
import useResource from '../lib/useResource';
import {ApiState} from './ApiState';
import {patch} from '../lib/api';
export function notificationPath(link){
  if(!link)return '/notificacoes';
  try{const url=new URL(link,location.origin);if(url.origin!==location.origin)return '/notificacoes';
    if(/^\/(vagas\/\d+|mensagens|candidaturas|perfil|dashboard)(\/|$)/.test(url.pathname))return url.pathname+url.search;
    const id=url.searchParams.get('id');if(url.pathname.endsWith('detalhe-vaga.html')&&/^\d+$/.test(id))return '/vagas/'+id;
    const profile=url.pathname.match(/^\/perfis\/(ARTISTA|CONTRATANTE)\/(\d+)$/);if(profile)return '/perfis/'+profile[1].toLowerCase()+'/'+profile[2];
  }catch{}return '/notificacoes';
}
export default function DashboardSide({navigate,notify}){
  const rooms=useResource('/chat/salas?size=3',true,30000);
  const notices=useResource('/notificacoes?size=5',true,30000);
  return <aside className="dashboard-side">
    <section className="dashboard-card dashboard-messages"><header><h2><Icon name="message"/>Mensagens</h2><button onClick={()=>navigate('/mensagens')}>Ver todas →</button></header>
      <ApiState {...rooms} retry={rooms.reload} empty={!rooms.data?.content?.length}/>
      {rooms.data?.content?.map(m=><button className="message-row" key={m.salaId} onClick={()=>navigate('/mensagens?sala='+m.salaId)}>
        {m.participanteAvatar&&<img src={m.participanteAvatar} alt=""/>}<span><strong>{m.participanteNome}</strong><small>{m.ultimaMensagem}</small></span>{m.naoLidas>0&&<b>{m.naoLidas}</b>}
      </button>)}
    </section>
    <section className="dashboard-card dashboard-notifications" id="dashboard-notifications"><header><h2><Icon name="bell"/>Notificações</h2><button onClick={()=>navigate('/notificacoes')}>Ver todas →</button></header>
      <ApiState {...notices} retry={notices.reload} empty={!notices.data?.content?.length}/>
      {notices.data?.content?.map(n=><button className="notice-row" key={n.id} onClick={async()=>{try{await patch('/notificacoes/'+n.id+'/lida');navigate(notificationPath(n.link));}catch(e){notify(e.message);}}}>
        <span className="notice-icon"><Icon name="bell"/></span><span><strong>{n.mensagem}</strong><small>{new Date(n.data).toLocaleString('pt-BR')}{n.lida?'':' · Nova'}</small></span>
      </button>)}
    </section>
    <section className="dashboard-community"><Icon name="user"/><h2>Explore a comunidade</h2><p>Conheça as comunidades disponíveis no Palco.</p><button className="yellow" onClick={()=>navigate('/comunidades')}>Explorar</button></section>
  </aside>;
}

import React,{useState,useEffect} from 'react';
import useResource from '../lib/useResource';
import {patch,getAccessToken} from '../lib/api';
import {connectChatRealtime} from '../lib/chatRealtime';
import {notificationPath} from '../components/DashboardSide';
import {ApiState,Pagination} from '../components/ApiState';
export default function Notifications({navigate,notify}){
  const [page,setPage]=useState(0),[busy,setBusy]=useState(false);
  const result=useResource('/notificacoes?page='+page+'&size=20',true,30000);
  useEffect(()=>{const c=connectChatRealtime({token:getAccessToken(),queue:'/user/queue/notificacoes',onEvent:result.reload});return()=>c.disconnect();},[]);
  return <main className="dashboard"><h1>NOTIFICAÇÕES</h1><button className="yellow" disabled={busy} onClick={async()=>{setBusy(true);try{await patch('/notificacoes/lidas');result.reload();}catch(e){notify(e.message);}finally{setBusy(false);}}}>Marcar todas como lidas</button><ApiState {...result} retry={result.reload} empty={!result.data?.content?.length}/>
  <section className="dashboard-card">{result.data?.content?.map(n=><button className="notice-row" key={n.id} onClick={async()=>{try{await patch('/notificacoes/'+n.id+'/lida');result.reload();const path=notificationPath(n.link);if(path!=='/notificacoes')navigate(path);}catch(e){notify(e.message);}}}><span><strong>{n.mensagem}</strong><small>{new Date(n.data).toLocaleString('pt-BR')}{n.lida?'':' · Nova'}</small></span></button>)}</section><Pagination page={page} hasNext={result.data?.hasNext} onChange={setPage}/></main>;
}

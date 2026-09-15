import React,{useState} from 'react';
import Icon from '../components/Icon';
import Modal from '../components/Modal';
import useResource from '../lib/useResource';
import {post,put,remove,query} from '../lib/api';
import {ApiState,Pagination} from '../components/ApiState';
const isoLocal=d=>new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,19);
export default function Agenda({notify}){
  const [date,setDate]=useState(()=>new Date()),[mode,setMode]=useState('month'),[page,setPage]=useState(0),[modal,setModal]=useState(null),[busy,setBusy]=useState(false),[error,setError]=useState('');
  const start=new Date(date.getFullYear(),date.getMonth(),mode==='month'?1:date.getDate()-((date.getDay()+6)%7));
  const end=new Date(start);if(mode==='month')end.setMonth(end.getMonth()+1);else end.setDate(end.getDate()+7);
  const result=useResource('/agenda'+query({inicio:isoLocal(start),fim:isoLocal(end),page,size:30}));
  function move(step){const d=new Date(date);if(mode==='month')d.setMonth(d.getMonth()+step,1);else d.setDate(d.getDate()+step*7);setDate(d);setPage(0);}
  async function save(e){e.preventDefault();const f=new FormData(e.currentTarget);const data=Object.fromEntries(f);setError('');
    if(data.fim<=data.inicio){setError('O fim deve ser posterior ao início.');return;}setBusy(true);
    try{if(modal.id)await put('/agenda/'+modal.id,data);else await post('/agenda',data);setModal(null);result.reload();notify('Compromisso salvo.');}
    catch(e){setError(e.message);}finally{setBusy(false);}
  }
  return <main className="agenda-page"><h1>AGENDA</h1><div className="agenda-toolbar"><b>Agenda / <span>{date.toLocaleDateString('pt-BR',{month:'long',year:'numeric'})}</span></b><button onClick={()=>{setModal({});setError('');}}><Icon name="plus"/>Novo Evento</button></div>
    <div className="integration-pagination"><button onClick={()=>move(-1)}>Anterior</button><select aria-label="Visualização da agenda" value={mode} onChange={e=>{setMode(e.target.value);setPage(0);}}><option value="month">Mensal</option><option value="week">Semanal</option></select><button onClick={()=>move(1)}>Próximo</button></div>
    <ApiState {...result} retry={result.reload} empty={!result.data?.content?.length}/>
    <div className="agenda-grid">{result.data?.content?.map(item=><article key={item.id}><button className="agenda-delete" aria-label={'Excluir '+item.titulo} onClick={()=>{setModal({remove:item});setError('');}}><Icon name="trash"/></button><div><h2>{item.titulo}</h2><p>{new Date(item.inicio).toLocaleString('pt-BR')} — {new Date(item.fim).toLocaleString('pt-BR')}</p><p>{item.localizacao}</p><button className="community-yellow" onClick={()=>{setModal(item);setError('');}}>Ver / editar</button></div></article>)}</div>
    <Pagination page={page} hasNext={result.data?.hasMore} onChange={setPage}/>
    {modal&&<Modal title={modal.remove?'Excluir compromisso?':modal.id?'Editar compromisso':'Novo compromisso'} onClose={()=>setModal(null)}>{modal.remove?<><p>{modal.remove.titulo}</p><button className="community-yellow" disabled={busy} onClick={async()=>{setBusy(true);try{await remove('/agenda/'+modal.remove.id);setModal(null);result.reload();}catch(e){setError(e.message);}finally{setBusy(false);}}}>Confirmar exclusão</button></>:<form className="community-form" onSubmit={save}>
      <label>Título<input name="titulo" defaultValue={modal.titulo||''} maxLength={150} required/></label><label>Tipo<input name="tipo" defaultValue={modal.tipo||'Compromisso'} maxLength={50} required/></label>
      <label>Início<input name="inicio" type="datetime-local" defaultValue={modal.inicio?.slice(0,16)} required/></label><label>Fim<input name="fim" type="datetime-local" defaultValue={modal.fim?.slice(0,16)} required/></label>
      <label>Local<input name="localizacao" defaultValue={modal.localizacao||''} maxLength={255} required/></label><label>Descrição<textarea name="descricao" defaultValue={modal.descricao||''} maxLength={5000}/></label><button className="community-yellow" disabled={busy}>Salvar compromisso</button>
    </form>}{error&&<p role="alert" className="auth-form-error">{error}</p>}</Modal>}
  </main>;
}

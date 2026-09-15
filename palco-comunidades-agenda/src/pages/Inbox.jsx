import React,{useState,useEffect} from 'react';
import Icon from '../components/Icon';
import Modal from '../components/Modal';
import useResource from '../lib/useResource';
import {post,patch,remove,getAccessToken} from '../lib/api';
import {connectChatRealtime} from '../lib/chatRealtime';
import {ApiState,Pagination} from '../components/ApiState';
export default function Inbox({user,navigate,notify,capabilities}){
  const params=new URLSearchParams(location.search);
  const [room,setRoom]=useState(params.get('sala')||''),[extra,setExtra]=useState(null),[roomPage,setRoomPage]=useState(0),[page,setPage]=useState(0),[text,setText]=useState(''),[search,setSearch]=useState(''),[busy,setBusy]=useState(false),[showList,setShowList]=useState(false),[status,setStatus]=useState('offline'),[editing,setEditing]=useState(null),[deletion,setDeletion]=useState(null);
  const rooms=useResource('/chat/salas?page='+roomPage+'&size=20',true,30000);
  const messages=useResource('/chat/salas/'+room+'/mensagens?page='+page+'&size=30',!!room,15000);
  const contact=params.get('contato');
  useEffect(()=>{let active=true;if(!/^\d+$/.test(contact||''))return;post('/chat/salas',{usuarioDestinoId:Number(contact)}).then(r=>{if(active){setExtra(r);setRoom(String(r.salaId));rooms.reload();}}).catch(e=>active&&notify(e.message));return()=>{active=false;};},[contact]);
  useEffect(()=>{if(!room&&rooms.data?.content?.length)setRoom(String(rooms.data.content[0].salaId));},[rooms.data,room]);
  useEffect(()=>{const connection=connectChatRealtime({token:getAccessToken(),onEvent:()=>{rooms.reload();messages.reload();},onState:setStatus});return()=>connection.disconnect();},[room]);
  useEffect(()=>{if(room&&messages.data?.content?.some(m=>m.remetenteId!==user.id&&!m.lida))patch('/chat/salas/'+room+'/lidas').then(rooms.reload).catch(e=>notify(e.message));},[room,messages.data,user.id]);
  const people=extra&&!rooms.data?.content?.some(r=>r.salaId===extra.salaId)?[extra,...(rooms.data?.content||[])]:rooms.data?.content||[];
  const person=people.find(p=>String(p.salaId)===String(room));
  async function send(e){e.preventDefault();if(!text.trim()||busy)return;setBusy(true);try{
    if(editing){await patch('/chat/mensagens/'+editing.id,{texto:text.trim()});setEditing(null);}
    else await post('/chat/salas/'+room+'/mensagens',{texto:text.trim()});
    setText('');setPage(0);messages.reload();rooms.reload();
  }catch(e){notify(e.message);}finally{setBusy(false);}}
  return <main className={'inbox '+(showList?'inbox-show-list':'')}><aside className="inbox-sidebar"><button className="inbox-back" onClick={()=>navigate('/dashboard')}><Icon name="arrow"/>Voltar</button><h1>Seu chat</h1><label className="inbox-search"><input placeholder="Pesquisar nesta página" aria-label="Pesquisar uma conversa" value={search} onChange={e=>setSearch(e.target.value)}/><Icon name="search"/></label>
    <ApiState {...rooms} retry={rooms.reload} empty={!people.length}/><div className="inbox-contacts">{people.filter(p=>p.participanteNome.toLowerCase().includes(search.toLowerCase())).map(p=><button className={String(p.salaId)===String(room)?'selected':''} key={p.salaId} onClick={()=>{setRoom(String(p.salaId));setPage(0);setShowList(false);setText('');setEditing(null);}}>
      {p.participanteAvatar&&<img src={p.participanteAvatar} alt=""/>}<span><strong>{p.participanteNome}</strong><small>{p.ultimaMensagem}</small></span><span className="contact-meta">{p.naoLidas>0&&<b>{p.naoLidas}</b>}</span></button>)}</div><Pagination page={roomPage} hasNext={rooms.data?.hasMore} onChange={setRoomPage}/>
  </aside><section className="inbox-main"><header><button className="inbox-mobile-back" aria-label="Ver conversas" onClick={()=>setShowList(true)}><Icon name="arrow"/></button>{person?.participanteAvatar&&<img src={person.participanteAvatar} alt=""/>}<div><h2>{person?.participanteNome||'Mensagens'}</h2><small>{status==='connected'?'Atualização em tempo real':'Atualização periódica'}</small></div></header>
    <div className="message-history" aria-label="Histórico da conversa"><ApiState {...messages} retry={messages.reload} empty={!!room&&!messages.data?.content?.length}/>{!room&&<p>Selecione uma conversa ou abra o chat em um perfil.</p>}
      {messages.data?.content?.slice().reverse().map(m=><div key={m.id} className={'chat-message '+(m.remetenteId===user.id?'outgoing':'incoming')}><div className="chat-bubble"><p>{m.excluida?'Mensagem removida':m.texto}</p><time>{new Date(m.dataEnvio).toLocaleString('pt-BR')}</time>{m.remetenteId===user.id&&!m.excluida&&capabilities?.editarMensagens===true&&<div><button onClick={()=>{setEditing(m);setText(m.texto);}}>Editar</button><button onClick={()=>setDeletion(m)}>Remover</button>{m.lida&&<small>Lida</small>}</div>}</div></div>)}
    </div>{room&&<Pagination page={page} hasNext={messages.data?.hasMore} onChange={setPage}/>}
    <div className="chat-composer">{editing&&<p>Editando mensagem <button onClick={()=>{setEditing(null);setText('');}}>Cancelar</button></p>}<form onSubmit={send}><button type="button" aria-label="Anexos indisponíveis" disabled title="Anexos não estão disponíveis nesta versão"><Icon name="paperclip"/></button><input aria-label="Digite uma mensagem" placeholder="Digite uma mensagem" maxLength={4000} value={text} onChange={e=>setText(e.target.value)} disabled={!room||busy}/><button className="chat-send" aria-label={editing?'Salvar edição':'Enviar mensagem'} disabled={!room||busy||!text.trim()}><Icon name="send"/></button></form></div>
  </section>{deletion&&<Modal title="Remover esta mensagem?" onClose={()=>setDeletion(null)}><button className="yellow" disabled={busy} onClick={async()=>{setBusy(true);try{await remove('/chat/mensagens/'+deletion.id);setDeletion(null);messages.reload();}catch(e){notify(e.message);}finally{setBusy(false);}}}>Confirmar</button></Modal>}</main>;
}

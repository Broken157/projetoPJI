import React,{useState} from 'react';
import useResource from '../lib/useResource';
import {post,remove,query} from '../lib/api';
import Modal from './Modal';
export default function SaveButton({type='PERFIL_ARTISTA',id,user,notify=()=>{}}){
  const state=useResource('/salvos/estado'+query({tipoAlvo:type,alvoId:id}),!!user&&!!id);
  const [confirm,setConfirm]=useState(false),[busy,setBusy]=useState(false);
  async function change(){
    setBusy(true);
    try{if(state.data?.salvo)await remove('/salvos/'+type+'/'+id);else await post('/salvos',{tipoAlvo:type,alvoId:id});
      setConfirm(false);state.reload();
    }catch(e){notify(e.message);}finally{setBusy(false);}
  }
  if(!user)return null;
  return <><button disabled={busy||state.loading||!!state.error} aria-pressed={!!state.data?.salvo}
    title={state.error?.message} onClick={()=>state.data?.salvo?change():setConfirm(true)}>
    {state.data?.salvo?'Salvo':type==='VAGA'?'Salvar vaga':'Salvar perfil'}{state.data?.quantidadeSalvos!=null?' · '+state.data.quantidadeSalvos:''}
  </button>{state.error&&<p role="alert">{state.error.message} <button onClick={state.reload}>Tentar novamente</button></p>}{confirm&&<Modal title={type==='VAGA'?'Salvar esta vaga?':'Salvar este perfil?'} onClose={()=>setConfirm(false)}>
    <p>O item ficará disponível em Meus Salvos.</p><button className="yellow" disabled={busy} onClick={change}>Confirmar</button>
  </Modal>}</>;
}

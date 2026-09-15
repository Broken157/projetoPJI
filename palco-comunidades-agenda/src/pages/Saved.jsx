import React,{useState} from 'react';
import useResource from '../lib/useResource';
import {remove,query} from '../lib/api';
import {ApiState,Pagination} from '../components/ApiState';
export default function Saved({navigate,notify}){
  const [page,setPage]=useState(0),[type,setType]=useState(''),[busy,setBusy]=useState(null);
  const result=useResource('/salvos'+query({page,size:12,tipoAlvo:type}));
  return <main className="dashboard"><h1>MEUS SALVOS</h1><label>Mostrar<select value={type} onChange={e=>{setType(e.target.value);setPage(0);}}><option value="">Todos</option><option value="PERFIL_ARTISTA">Perfis</option><option value="VAGA">Vagas</option></select></label>
    <ApiState {...result} retry={result.reload} empty={!result.data?.content?.length}/><div className="jobs-list">{result.data?.content?.map(x=><article className="job-card" key={x.tipoAlvo+':'+x.alvoId}>{x.avatarUrl&&<img src={x.avatarUrl} alt=""/>}<div className="job-info"><h2>{x.disponivel?x.nome:'Item indisponível'}</h2><p>{x.localizacao}</p><p>{x.status}</p></div><button className="yellow" disabled={!x.disponivel} onClick={()=>navigate(x.tipoAlvo==='VAGA'?'/vagas/'+x.alvoId:'/perfis/artista/'+x.alvoId)}>Abrir</button><button disabled={busy!==null} onClick={async()=>{setBusy(x.alvoId);try{await remove('/salvos/'+x.tipoAlvo+'/'+x.alvoId);result.reload();}catch(e){notify(e.message);}finally{setBusy(null);}}}>Remover</button></article>)}</div><Pagination page={page} hasNext={result.data?.hasNext} onChange={setPage}/>
  </main>;
}

import React,{useState} from 'react';
import Icon from '../components/Icon';
import useResource from '../lib/useResource';
import {ApiState,Pagination} from '../components/ApiState';
export default function Community({navigate,discussion}){
  const [page,setPage]=useState(0),id=new URLSearchParams(location.search).get('id');
  const result=useResource(discussion&&/^\d+$/.test(id||'')?'/comunidades/'+id:'/comunidades?page='+page+'&size=12');
  const detail=discussion&&result.data?.id;
  return <main className="community-page"><h1>COMUNIDADES</h1><button className="community-back" onClick={()=>navigate(discussion?'/comunidades':'/dashboard')}><Icon name="arrow"/>Voltar</button>
    <ApiState {...result} retry={result.reload} empty={!detail&&!result.data?.content?.length}/>
    <div className="community-layout"><article className="community-main">{detail?<section className="festival-intro"><div><h2>{result.data.nome}</h2><h3>Descrição</h3><p>{result.data.descricao}</p><div className="festival-facts"><div><b>Categoria</b><span>{result.data.categoria}</span></div></div></div></section>:result.data?.content?.map(c=><section key={c.id} className="festival-intro"><div><h2>{c.nome}</h2><p>{c.descricao}</p><p>{c.categoria}</p><button className="community-yellow" onClick={()=>navigate('/comunidade2?id='+c.id)}>Ver comunidade</button></div></section>)}</article>
    <aside className="side-card"><h2>Comunidades</h2><p>Publicações, comentários e eventos de comunidades ainda não estão disponíveis nesta versão.</p><button className="community-yellow" disabled>Nova publicação</button></aside></div>
    {!detail&&<Pagination page={page} hasNext={result.data?.hasMore} onChange={setPage}/>}
  </main>;
}

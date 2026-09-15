import React from 'react';
export function ApiState({loading,error,empty,retry}){
  if(loading)return <p role="status">Carregando…</p>;
  if(error)return <div role="alert" className="auth-form-error">{error.message} {retry&&<button onClick={retry}>Tentar novamente</button>}</div>;
  if(empty)return <p role="status">Nenhum item encontrado.</p>;
  return null;
}
export function Pagination({page,hasNext,onChange}){
  return <nav className="integration-pagination" aria-label="Paginação"><button disabled={page===0} onClick={()=>onChange(page-1)}>Anterior</button><span>Página {page+1}</span><button disabled={!hasNext} onClick={()=>onChange(page+1)}>Próxima</button></nav>;
}

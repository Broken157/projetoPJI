import React from 'react';
import Icon from '../components/Icon';
export default function DeleteAccount({navigate}){
  return <main className="delete-account-page"><h1>SOLICITAR EXCLUSÃO DE CONTA</h1><section className="deletion-banner"><Icon name="trash"/><div><h2>Excluir sua conta é uma ação permanente</h2><p>Esta operação ainda não está disponível.</p></div></section>
    <section className="deletion-card"><h2>Exclusão indisponível</h2><p>A exclusão segura da conta e a revogação completa das sessões ainda não são suportadas nesta versão. Nenhuma solicitação foi registrada.</p><div className="deletion-buttons"><button className="community-yellow" disabled>Solicitar exclusão</button><button onClick={()=>navigate('/dashboard')}>Voltar</button></div></section>
  </main>;
}

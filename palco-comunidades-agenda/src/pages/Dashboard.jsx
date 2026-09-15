import React from 'react';
import Icon from '../components/Icon';
import useResource from '../lib/useResource';
import {ApiState} from '../components/ApiState';
import DashboardSide from '../components/DashboardSide';
export default function Dashboard({navigate,notify,user,capabilities}){
  const result=useResource('/dashboard?size=5');
  const data=result.data;
  return <main className="dashboard">
    <section className="dashboard-welcome"><div><h1>OLÁ, {user.name.split(' ')[0].toUpperCase()}!</h1><p>Seu talento é único. Aqui você encontra oportunidades que combinam com o seu perfil.</p><button className="yellow" onClick={()=>navigate('/perfil')}>{data?.perfilCompleto?'Ver meu perfil':'Atualizar meu perfil'}<Icon name="plus"/></button></div><img src="/assets/logo.png" alt=""/></section>
    {data&&!data.perfilCompleto&&<section className="dashboard-progress"><span>!</span><div><h2>Perfil incompleto</h2><p>Os dados profissionais necessários para concluir o perfil ainda não estão disponíveis nesta versão.</p><button onClick={()=>navigate('/perfil/editar')}>Editar perfil</button></div></section>}
    <ApiState {...result} retry={result.reload}/>
    <div className="dashboard-columns"><div><section className="jobs-panel"><h2>Vagas recomendadas</h2><p>Analise vagas que combinam com você</p><div className="jobs-list">
      {data?.vagasRecomendadas?.content?.map(v=><article className="job-card" key={v.id}><div className="job-info"><div className="job-title"><h3>{v.titulo}</h3><span>{v.modeloTrabalho}</span></div><p>{v.nomeContratante}</p><p>{[v.cidade,v.estado].filter(Boolean).join(', ')}</p><p className="job-description">{v.descricao}</p></div><button className="yellow" onClick={()=>navigate('/vagas/'+v.id)}>Ver vaga</button></article>)}
      {data&&!data.vagasRecomendadas?.content?.length&&<p>{capabilities?.taxonomia===false?'Recomendações indisponíveis nesta versão.':'Nenhuma vaga recomendada no momento.'}</p>}
      <button onClick={()=>navigate('/vagas')}>Explorar vagas</button>
    </div></section><section className="dashboard-card"><header><h2>Candidaturas</h2><button onClick={()=>navigate('/candidaturas')}>Ver todas →</button></header>
      {data?.candidaturasRecentes?.content?.map(c=><button className="notice-row" key={c.id} onClick={()=>navigate('/vagas/'+c.vagaId)}><span><strong>{c.tituloVaga}</strong><small>{c.status}</small></span></button>)}
      {data&&!data.candidaturasRecentes?.content?.length&&<p>Nenhuma candidatura recente.</p>}
    </section></div><DashboardSide navigate={navigate} notify={notify}/></div>
  </main>;
}

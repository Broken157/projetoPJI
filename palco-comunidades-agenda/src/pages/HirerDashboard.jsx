import React from 'react';
import Icon from '../components/Icon';
import useResource from '../lib/useResource';
import {ApiState} from '../components/ApiState';
import DashboardSide from '../components/DashboardSide';
import SaveButton from '../components/SaveButton';
export default function HirerDashboard({user,navigate,notify,capabilities}){
  const result=useResource('/dashboard?size=8'), vacancies=useResource('/vagas/minhas?size=5');
  const data=result.data;
  return <main className="hirer-dashboard"><section className="hirer-hero"><span>OLÁ, CONTRATANTE</span><h1>{user.name.toUpperCase()}!</h1><Icon name="user"/>
    <div className="hirer-stats"><div><small>Candidaturas</small><b>{data?.candidaturasRecentes?.totalElements??'—'}</b></div><div><small>Mensagens não lidas</small><b>{data?.mensagens?.quantidadeNaoLidas??'—'}</b></div></div></section>
    <ApiState {...result} retry={result.reload}/>
    <section className="hirer-profile-summary"><h2>Suas vagas</h2><button className="yellow" onClick={()=>navigate('/vagas/nova')}>Publicar vaga</button><button onClick={()=>navigate('/minhas-vagas')}>Ver todas</button><ApiState {...vacancies} retry={vacancies.reload} empty={!vacancies.data?.content?.length}/>
      {vacancies.data?.content?.map(v=><button className="notice-row" key={v.id} onClick={()=>navigate('/vagas/'+v.id+'/gerenciar')}><span><strong>{v.titulo}</strong><small>{v.status}</small></span></button>)}
    </section><div className="hirer-columns"><section className="recommended-artists"><h2>Artistas recomendados</h2><p>Encontre talentos que combinam com suas vagas</p>
      <div className="recommended-grid">{data?.talentosSugeridos?.content?.map(t=><article key={t.artistaId} className="recommended-artist">
        {t.avatarUrl&&<img src={t.avatarUrl} alt=""/>}<strong>{t.nomeExibicao}</strong><small>{t.localizacao}</small><div className="recommend-tags">{t.funcoes?.map(f=><span key={f.id}>{f.nome}</span>)}</div>
        <div className="recommend-actions"><button onClick={()=>navigate('/perfis/artista/'+t.artistaId)}>Ver perfil</button><SaveButton id={t.artistaId} user={user} notify={notify}/></div>
      </article>)}</div>{data&&!data.talentosSugeridos?.content?.length&&<p>{capabilities?.taxonomia===false?'Recomendações indisponíveis nesta versão.':'Nenhum talento recomendado no momento.'}</p>}
      <button className="yellow" onClick={()=>navigate('/talentos')}>Explorar Banco de Talentos</button>
    </section><DashboardSide navigate={navigate} notify={notify}/></div></main>;
}

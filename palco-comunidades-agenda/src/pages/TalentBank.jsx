import React,{useState} from 'react';
import Icon from '../components/Icon';
import useResource from '../lib/useResource';
import {query} from '../lib/api';
import {ApiState,Pagination} from '../components/ApiState';
import SaveButton from '../components/SaveButton';
export default function TalentBank({user,navigate,notify}){
  const [filters,setFilters]=useState(false),[form,setForm]=useState({}),[active,setActive]=useState({}),[page,setPage]=useState(0);
  const areas=useResource('/talentos/areas?size=50'), functions=useResource('/talentos/funcoes'+query({areaId:form.areaId,size:50}),!!form.areaId);
  const specializations=useResource('/talentos/especializacoes'+query({areaId:form.areaId,funcaoIds:form.funcaoIds,size:50}),!!form.areaId&&!!form.funcaoIds);
  const result=useResource('/talentos'+query({...active,page,size:12}));
  function set(key,value){setForm(f=>({...f,[key]:value,...(key==='areaId'?{funcaoIds:'',especializacaoIds:''}:key==='funcaoIds'?{especializacaoIds:''}:{})}));}
  return <main className="talent-bank"><header><h1>BANCO DE<br/>TALENTOS</h1><form onSubmit={e=>{e.preventDefault();setActive(form);setPage(0);}}><label className="talent-search"><input placeholder="Buscar por localização..." aria-label="Localização dos talentos" value={form.localizacao||''} onChange={e=>set('localizacao',e.target.value)}/><button aria-label="Buscar talentos"><Icon name="search"/></button></label></form><button className="talent-filter" aria-expanded={filters} onClick={()=>setFilters(!filters)}>Filtro <Icon name="filter"/></button></header>
    {filters&&<form className="talent-filter-panel" onSubmit={e=>{e.preventDefault();setActive(form);setPage(0);}}>
      {[['areaId','Área artística',areas],['funcaoIds','Função',functions],['especializacaoIds','Especialização',specializations]].map(([key,label,source])=><label key={key}>{label}<select value={form[key]||''} onChange={e=>set(key,e.target.value)} disabled={source.loading||(key==='funcaoIds'&&!form.areaId)||(key==='especializacaoIds'&&!form.funcaoIds)}><option value="">Todas</option>{source.data?.content?.map(x=><option key={x.id} value={x.id}>{x.nome}</option>)}</select><ApiState error={source.error}/></label>)}
      <label>Experiência<select value={form.experienciaMinima||''} onChange={e=>set('experienciaMinima',e.target.value)}><option value="">Todas</option>{['SEM_EXPERIENCIA','INICIANTE','INTERMEDIARIO','EXPERIENTE','ESPECIALISTA'].map(x=><option key={x}>{x}</option>)}</select></label>
      <label>Disponibilidade<select value={form.disponivel??''} onChange={e=>set('disponivel',e.target.value)}><option value="">Todas</option><option value="true">Disponível</option><option value="false">Indisponível</option></select></label>
      <label>Tipo de perfil<select value={form.tipos||''} onChange={e=>set('tipos',e.target.value)}><option value="">Todos</option>{['ARTISTA_SOLO','DUPLA','BANDA','GRUPO_ARTISTICO','ESTUDIO','PRODUTORA_EMPRESA'].map(x=><option key={x}>{x}</option>)}</select></label>
      <button className="yellow">Aplicar filtros</button><button type="button" onClick={()=>{setForm({});setActive({});setPage(0);}}>Limpar filtros</button>
    </form>}
    <div className="talent-favorite-filter"><button onClick={()=>navigate('/salvos')}>Meus perfis salvos</button></div>
    <ApiState {...result} retry={result.reload} empty={!result.data?.content?.length}/>
    <div className="talent-track">{result.data?.content?.map(t=><article className="talent-card" key={t.artistaId}>
      <div className="talent-person">{t.avatarUrl&&<img src={t.avatarUrl} alt=""/>}<div><h2>{t.nomeExibicao}</h2><p>{t.areas?.map(a=>a.nome).join(', ')}</p><small><Icon name="pin"/>{t.localizacao}</small></div></div>
      <h3>Sobre</h3><p className="talent-about">{t.biografia}</p><h3>Especialidades</h3><div className="talent-skills">{t.areas?.flatMap(a=>a.especializacoes||[]).map(s=><span key={s.id}>{s.nome}</span>)}</div>
      <SaveButton id={t.artistaId} user={user} notify={notify}/><div className="talent-actions"><button onClick={()=>navigate('/perfis/artista/'+t.artistaId)}>Ver perfil</button><button className="yellow" onClick={()=>navigate('/mensagens?contato='+t.artistaId)}>Chat</button></div>
    </article>)}</div>
    <Pagination page={page} hasNext={result.data?.hasMore} onChange={setPage}/><footer><button onClick={()=>navigate('/dashboard')}><Icon name="arrow"/>Voltar para o Início</button></footer>
  </main>;
}

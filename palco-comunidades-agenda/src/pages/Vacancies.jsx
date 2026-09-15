import React,{useState} from 'react';
import Modal from '../components/Modal';
import useResource from '../lib/useResource';
import {api,post,put,patch,remove,query,safeUrl,get} from '../lib/api';
import {ApiState,Pagination} from '../components/ApiState';
import SaveButton from '../components/SaveButton';
export default function Vacancies({path,user,navigate,notify,capabilities}){
  const match=path.match(/^\/vagas\/(\d+)/),id=match?.[1],mine=path==='/minhas-vagas';
  const create=path==='/vagas/nova',editing=path.endsWith('/editar'),manage=path.endsWith('/gerenciar');
  const [filter,setFilter]=useState(''),[search,setSearch]=useState(''),[cursors,setCursors]=useState([null]),[index,setIndex]=useState(0),[modal,setModal]=useState(null),[busy,setBusy]=useState(false),[error,setError]=useState('');
  const result=useResource(id?'/vagas/'+id:(mine?'/vagas/minhas':'/vagas')+query({cursor:cursors[index],titulo:mine?undefined:search,size:12}),!create);
  const vacancy=id?result.data:null;
  const owner=user?.role==='contratante'&&vacancy?.contratanteId===user.id;
  async function action(acao){setBusy(true);try{await patch('/vagas/'+id+'/status',{acao});result.reload();notify('Status atualizado.');}catch(e){notify(e.message);}finally{setBusy(false);}}
  if(create||editing)return <VacancyForm vacancy={vacancy} loading={result.loading&&!create} user={user} notify={notify} navigate={navigate} capabilities={capabilities} error={result.error}/>;
  return <main className="dashboard"><h1>{id?vacancy?.titulo||'VAGA':mine?'MINHAS VAGAS':'VAGAS'}</h1>
    {!id&&<><form className="community-form" onSubmit={e=>{e.preventDefault();setSearch(filter);setIndex(0);setCursors([null]);}}><label>Buscar vagas<input value={filter} onChange={e=>setFilter(e.target.value)} maxLength={150}/></label><button className="yellow">Buscar</button></form>{user?.role==='contratante'&&<button className="yellow" onClick={()=>navigate('/vagas/nova')}>Publicar vaga</button>}</>}
    <ApiState {...result} retry={result.reload} empty={!id&&!result.data?.content?.length}/>
    {!id?<><div className="jobs-list">{result.data?.content?.map(v=><article className="job-card" key={v.id}><div className="job-info"><div className="job-title"><h2>{v.titulo}</h2><span>{v.status}</span></div><p>{v.nomeContratante}</p><p>{v.cidade}, {v.estado}</p><p className="job-description">{v.descricao}</p></div><button className="yellow" onClick={()=>navigate('/vagas/'+v.id+(mine?'/gerenciar':''))}>Ver vaga</button></article>)}</div><Pagination page={index} hasNext={result.data?.hasMore} onChange={n=>{if(n>index)setCursors(c=>[...c.slice(0,n),result.data.nextCursor]);setIndex(n);}}/></>:
      vacancy&&<><section className="dashboard-card vacancy-detail"><h2>{vacancy.nomeContratante}</h2><p>{vacancy.cidade}, {vacancy.estado} · {vacancy.modeloTrabalho} · {vacancy.status}</p><p>{vacancy.tipoContrato}</p><h3>Descrição</h3><p>{vacancy.descricao}</p><h3>Requisitos</h3><p>{vacancy.requisitos}</p><h3>Remuneração</h3><p>{vacancy.remuneraValor!=null?Number(vacancy.remuneraValor).toLocaleString('pt-BR',{style:'currency',currency:'BRL'}):vacancy.formaRemuneracao||'A combinar'} {vacancy.formaPagamento}</p>{vacancy.dataLimiteCandidatura&&<p>Inscrições até {vacancy.dataLimiteCandidatura}</p>}
      <SaveButton type="VAGA" id={Number(id)} user={user} notify={notify}/>
      {owner?<div className="integration-actions">{['ABERTA','PAUSADA','RASCUNHO'].includes(vacancy.status)&&<button onClick={()=>navigate('/vagas/'+id+'/editar')}>Editar vaga</button>}{vacancy.status==='ABERTA'&&<button disabled={busy} onClick={()=>action('SUSPENDER')}>Pausar</button>}{vacancy.status==='PAUSADA'&&<button disabled={busy} onClick={()=>action('REABRIR')}>Reabrir</button>}{['ABERTA','PAUSADA'].includes(vacancy.status)&&<><button disabled={busy} onClick={()=>{setModal('end');setError('');}}>Encerrar</button><button disabled={busy} onClick={()=>{setModal('cancel');setError('');}}>Cancelar vaga</button></>}</div>:user?.role==='artista'?<div className="integration-actions">{vacancy.minhaCandidaturaId?<p>Sua candidatura: {vacancy.statusMinhaCandidatura}</p>:<button className="yellow" disabled={vacancy.status!=='ABERTA'||!user.perfilCompleto} onClick={()=>{setModal('apply');setError('');}}>Candidatar-me</button>}{!user.perfilCompleto&&<p>Seu perfil ainda não está completo.</p>}</div>:!user&&<button className="yellow" onClick={()=>navigate('/login')}>Entre para se candidatar</button>}</section>
      {owner&&(manage||id)&&<Applications user={user} vagaId={id} navigate={navigate} notify={notify}/>}</>}
    {modal&&<Modal title={modal==='apply'?'Candidatar-se à vaga':modal==='end'?'Encerrar vaga?':'Cancelar vaga?'} onClose={()=>setModal(null)}>
      <form className="community-form" onSubmit={async e=>{e.preventDefault();const form=new FormData(e.currentTarget);setBusy(true);setError('');try{
        if(modal==='apply')await post('/candidaturas',{vagaId:Number(id),mensagemApresentacao:form.get('mensagem'),linkPortfolioCandidatura:form.get('link')});
        else if(modal==='end')await patch('/vagas/'+id+'/status',{acao:'ENCERRAR'});
        else await api('/vagas/'+id,{method:'DELETE',body:{confirmacao:true,motivo:form.get('motivo')}});
        setModal(null);result.reload();notify('Operação concluída.');
      }catch(e){setError(e.message);}finally{setBusy(false);}}}>
        {modal==='apply'?<><label>Apresentação<textarea name="mensagem" required maxLength={2000}/></label><label>Link do portfólio<input name="link" type="url" required maxLength={255}/></label></>:modal==='cancel'?<><p>O cancelamento é definitivo. O histórico será preservado.</p><label>Motivo<textarea name="motivo" required maxLength={2000}/></label></>:<p>O encerramento é definitivo.</p>}
        {error&&<p role="alert" className="auth-form-error">{error}</p>}<button className="yellow" disabled={busy}>Confirmar</button>
      </form></Modal>}
  </main>;
}
function VacancyForm({vacancy,loading,error,navigate,notify,capabilities}){
  const [busy,setBusy]=useState(false),[issue,setIssue]=useState(''),[area,setArea]=useState('');
  const taxonomy=capabilities?.taxonomia===true;
  const areas=useResource('/areas',taxonomy),functions=useResource('/funcoes',taxonomy);
  const areaId=area||vacancy?.areaId;
  async function save(e){e.preventDefault();setBusy(true);setIssue('');const form=new FormData(e.currentTarget);
    const body=Object.fromEntries(form);body.valorMinimo=Number(body.valorMinimo);body.valorMaximo=null;
    if(taxonomy){body.areaId=Number(areaId);body.funcaoIds=form.getAll('funcaoIds').map(Number);}
    else{delete body.areaId;delete body.formaRemuneracao;}
    if(!body.dataLimiteCandidatura)body.dataLimiteCandidatura=null;
    try{const v=vacancy?await put('/vagas/'+vacancy.id,body):await post('/vagas',body);notify('Vaga salva.');navigate('/vagas/'+v.id+'/gerenciar');}catch(e){setIssue(e.message);}finally{setBusy(false);}
  }
  if(loading||error)return <main className="dashboard"><ApiState loading={loading} error={error}/></main>;
  return <main className="dashboard"><h1>{vacancy?'EDITAR VAGA':'PUBLICAR VAGA'}</h1><form className="community-form" onSubmit={save} key={vacancy?.id||'new'}>
    {[['titulo','Título',150],['cidade','Cidade',100],['estado','Estado (UF)',2],['tipoContrato','Tipo de contrato',100]].map(([key,label,max])=><label key={key}>{label}<input name={key} defaultValue={vacancy?.[key]||''} maxLength={max} required/></label>)}
    {[['descricao','Descrição'],['requisitos','Requisitos']].map(([key,label])=><label key={key}>{label}<textarea name={key} defaultValue={vacancy?.[key]||''} required maxLength={10000}/></label>)}
    <label>Valor (R$)<input name="valorMinimo" type="number" min="0" step="0.01" defaultValue={vacancy?.valorMinimo??vacancy?.remuneraValor??''} required/></label>
    {taxonomy?<><label>Área<select value={areaId||''} onChange={e=>setArea(e.target.value)} required><option value="">Selecione</option>{areas.data?.map(a=><option value={a.id} key={a.id}>{a.nome}</option>)}</select></label><fieldset><legend>Funções (até 5)</legend>{functions.data?.filter(f=>f.areaId===Number(areaId)).map(f=><label key={f.id}><input type="checkbox" name="funcaoIds" value={f.id} defaultChecked={vacancy?.funcaoIds?.includes(f.id)}/>{f.nome}</label>)}</fieldset><label>Forma de remuneração<select name="formaRemuneracao" defaultValue={vacancy?.formaRemuneracao||'A_COMBINAR'}>{['POR_HORA','DIARIA','POR_EVENTO','POR_PROJETO','MENSAL','A_COMBINAR'].map(x=><option key={x}>{x}</option>)}</select></label></>:<><label>Forma de pagamento<input name="formaPagamento" maxLength={100} defaultValue={vacancy?.formaPagamento||''} required/></label><label>Categoria (texto livre)<input name="categoria" maxLength={100} defaultValue={vacancy?.categoria||''}/></label></>}
    <label>Modelo de trabalho<select name="modeloTrabalho" defaultValue={vacancy?.modeloTrabalho||'PRESENCIAL'}>{['PRESENCIAL','REMOTO','HIBRIDO'].map(x=><option key={x}>{x}</option>)}</select></label>
    <label>Abrangência<select name="abrangencia" defaultValue={vacancy?.abrangencia||'LOCAL'}>{['LOCAL','REGIONAL','NACIONAL','INTERNACIONAL','REMOTO'].map(x=><option key={x}>{x}</option>)}</select></label>
    <label>Data limite<input name="dataLimiteCandidatura" type="date" defaultValue={vacancy?.dataLimiteCandidatura||''}/></label><label>Benefícios<textarea name="beneficios" defaultValue={vacancy?.beneficios||''}/></label>
    {issue&&<p role="alert" className="auth-form-error">{issue}</p>}<button className="yellow" disabled={busy}>{busy?'Salvando…':vacancy?'Salvar alterações':'Publicar vaga'}</button><button type="button" onClick={()=>navigate('/minhas-vagas')}>Voltar</button>
  </form></main>;
}
export function Applications({user,vagaId,navigate,notify}){
  const [page,setPage]=useState(0),[busy,setBusy]=useState(false),[withdraw,setWithdraw]=useState(null);
  const result=useResource(vagaId?'/vagas/'+vagaId+'/candidaturas?page='+page+'&size=12':'/candidaturas?page='+page+'&size=12');
  const rows=Array.isArray(result.data)?result.data:result.data?.content||[];
  async function review(row,status){setBusy(true);try{const source=await get('/candidaturas/'+row.candidaturaId);await put('/candidaturas/'+source.id,{vagaId:source.vagaId,artistaId:source.artistaId,mensagemApresentacao:source.mensagemApresentacao,linkPortfolioCandidatura:source.linkPortfolioCandidatura,status});result.reload();}catch(e){notify(e.message);}finally{setBusy(false);}}
  return <section className="dashboard-card"><h2>Candidaturas</h2><ApiState {...result} retry={result.reload} empty={!rows.length}/>{rows.map(c=><article className="notice-row" key={c.id||c.candidaturaId}><span><strong>{c.nomeArtista||'Vaga '+c.vagaId}</strong><small>{c.status}</small><p>{c.mensagemApresentacao}</p>{safeUrl(c.linkPortfolioCandidatura)&&<a href={safeUrl(c.linkPortfolioCandidatura)} target="_blank" rel="noreferrer">Portfólio</a>}</span>
    {vagaId&&<><button onClick={()=>navigate('/perfis/artista/'+c.artistaId)}>Ver perfil</button>{['PENDENTE','EM_ANALISE'].includes(c.status)&&['EM_ANALISE','ACEITA','REJEITADA'].filter(s=>s!==c.status).map(s=><button disabled={busy} key={s} onClick={()=>review(c,s)}>{s}</button>)}</>}
    {!vagaId&&<button onClick={()=>navigate('/vagas/'+c.vagaId)}>Ver vaga</button>}
    {user.role==='artista'&&['PENDENTE','EM_ANALISE','ACEITA'].includes(c.status)&&<button disabled={busy} onClick={()=>setWithdraw(c)}>Retirar candidatura</button>}
  </article>)}<Pagination page={page} hasNext={Array.isArray(result.data)?rows.length===12:result.data?.hasNext} onChange={setPage}/>
  {withdraw&&<Modal title="Retirar candidatura?" onClose={()=>setWithdraw(null)}><p>A candidatura continuará no histórico.</p><button className="yellow" disabled={busy} onClick={async()=>{setBusy(true);try{await remove('/candidaturas/'+withdraw.id);setWithdraw(null);result.reload();}catch(e){notify(e.message);}finally{setBusy(false);}}}>Confirmar retirada</button></Modal>}</section>;
}

import React,{useState} from 'react';
import Icon from '../components/Icon';
import useResource from '../lib/useResource';
import {get,put,patch,safeUrl} from '../lib/api';
import {ApiState} from '../components/ApiState';
import SaveButton from '../components/SaveButton';
import Portfolio from './Portfolio';
export default function Profile({user,navigate,notify,path,onUserChange,capabilities}){
  const match=path.match(/^\/perfis\/(artista|contratante)\/(\d+)$/i);
  const own=!match||(user&&Number(match[2])===user.id&&match[1].toLowerCase()===user.role);
  const type=match?.[1]?.toLowerCase()||user?.role,id=match?Number(match[2]):user?.id;
  const result=useResource(own?'/perfis-'+(type==='artista'?'artistas':'contratantes')+'/'+id:'/perfis/publicos/'+type.toUpperCase()+'/'+id);
  const areas=useResource('/areas',type==='artista'&&capabilities?.taxonomia===true), functions=useResource('/funcoes',own&&type==='artista'&&capabilities?.taxonomia===true);
  const [editing,setEditing]=useState(path==='/perfil/editar'),[busy,setBusy]=useState(false),[tab,setTab]=useState('Portfólios');
  const p=result.data;
  async function save(event){
    event.preventDefault();setBusy(true);
    const form=new FormData(event.currentTarget);
    const payload={usuarioId:id,biografia:form.get('biografia'),localizacao:form.get('localizacao'),bannerUrl:form.get('bannerUrl')||null};
    if(type==='artista'&&capabilities?.taxonomia===true)Object.assign(payload,{urlPortfolio:form.get('urlPortfolio')||null,areaPrincipalId:p.areaPrincipalId,funcaoIds:form.getAll('funcoes').map(Number),tipoPerfilArtistico:form.get('tipoPerfilArtistico'),raioAtuacao:form.get('raioAtuacao'),disponivelOportunidades:form.get('disponivel')==='on'});
    else if(type!=='artista')Object.assign(payload,{nomeEmpresa:form.get('nomeEmpresa'),tipoPerfil:form.get('tipoPerfil')});
    if(type==='artista')payload.urlPortfolio=form.get('urlPortfolio')||null;
    try{await put('/perfis-'+(type==='artista'?'artistas':'contratantes')+'/'+id,payload);
      if(form.get('avatarUrl')!==p.avatarUrl)await patch('/usuarios/me/avatar',{avatarUrl:form.get('avatarUrl')||null});
      const account=await get('/usuarios/me');onUserChange?.(account);
      result.reload();setEditing(false);notify('Perfil atualizado.');
    }catch(e){notify(e.message);}finally{setBusy(false);}
  }
  const name=own?user?.name:p?.nomeExibicao;
  const area=areas.data?.find(a=>a.id===p?.areaPrincipalId);
  return <main className="profile-layout">
    <aside className="profile-sidebar"><section className="profile-card"><div className="avatar-banner"/>{p?.avatarUrl&&<img className="avatar" src={safeUrl(p.avatarUrl)} alt=""/>}<h2>{name}</h2><strong className="pink">{type==='artista'?area?.nome||'Artista':'Contratante'}</strong><span className="location"><Icon name="pin"/>{p?.localizacao}</span></section>
      {own&&<section className="side-card"><h2>Gestão rápida</h2><button className="quick-action" onClick={()=>setEditing(!editing)}><span className="round yellow"><Icon name="edit"/></span><span><b>Editar perfil</b><small>Atualize suas informações</small></span></button>{type==='artista'&&<button className="quick-action" onClick={()=>navigate('/portfolio/novo')}>Novo portfólio</button>}</section>}
      {type==='artista'&&<><section className="side-card"><h2>Funções</h2><div className="tags">{(p?.funcoes||functions.data?.filter(f=>p?.funcaoIds?.includes(f.id))||[]).map(f=><span key={f.id}>{f.nome}</span>)}</div></section><section className="side-card"><h2>Áreas e experiência</h2>{p?.areas?.map(a=><div key={a.id}><b>{a.nome}</b><p>{a.nivelExperiencia||'Não informada'}</p><div className="tags">{a.especializacoes?.map(s=><span key={s.id}>{s.nome}</span>)}</div></div>)}</section></>}
    </aside>
    <div className="profile-main"><ApiState {...result} retry={result.reload}/>
      {p&&<section className="profile-hero"><div className="hero-banner" style={p.bannerUrl?{backgroundImage:'url('+safeUrl(p.bannerUrl)+')'}:{}}><b>{own?'SEU PERFIL DE '+type.toUpperCase():name}</b><p>Complete, destaque e compartilhe sua presença no Palco.</p></div>
      <div className="hero-details"><div className="bio"><h1>{name}</h1><h2>Biografia</h2><p>{p.biografia||'Biografia não informada.'}</p>{p.nomeEmpresa&&<p>{p.nomeEmpresa}</p>}</div><div className="hero-right">
        {own&&<div className="stats"><div><small>Perfil completo</small><strong>{user.perfilCompleto?'Sim':'Pendente'}</strong></div><div><small>Disponibilidade</small><strong>{p.disponivelOportunidades===true?'Disponível':p.disponivelOportunidades===false?'Indisponível':'—'}</strong></div></div>}
        {area&&<div className="music-card"><div><h2>{area.nome}</h2><p>Área artística principal</p></div></div>}
        {safeUrl(p.urlPortfolio)&&<a href={safeUrl(p.urlPortfolio)} target="_blank" rel="noreferrer">Portfólio externo</a>}
        {!own&&type==='artista'&&<SaveButton id={id} user={user} notify={notify}/>}
        {!own&&user&&<button className="yellow" onClick={()=>navigate('/mensagens?contato='+id)}>Enviar mensagem</button>}
      </div></div></section>}
      {editing&&own&&p&&<section className="portfolio-panel"><form className="community-form" onSubmit={save}>
        <h2>Editar perfil</h2><label>Biografia<textarea name="biografia" defaultValue={p.biografia||''} maxLength={5000} required/></label>
        <label>Localização pública (cidade/estado)<input name="localizacao" defaultValue={p.localizacao||''} maxLength={150} required/></label>
        <label>URL do avatar<input name="avatarUrl" type="url" defaultValue={p.avatarUrl||''} maxLength={255}/></label>
        <label>URL do banner<input name="bannerUrl" type="url" defaultValue={p.bannerUrl||''} maxLength={255}/></label>
        {type==='artista'?<><label>Link do portfólio<input type="url" name="urlPortfolio" defaultValue={p.urlPortfolio||''} maxLength={255}/></label>
          {capabilities?.taxonomia===true&&<><label>Tipo de perfil<select name="tipoPerfilArtistico" defaultValue={p.tipoPerfilArtistico}>{['ARTISTA_SOLO','DUPLA','BANDA','GRUPO_ARTISTICO','ESTUDIO','PRODUTORA_EMPRESA'].map(t=><option key={t}>{t}</option>)}</select></label>
          <label>Raio de atuação<select name="raioAtuacao" defaultValue={p.raioAtuacao||''} required><option value="">Selecione</option>{['LOCAL','REGIONAL','NACIONAL','INTERNACIONAL','REMOTO'].map(t=><option key={t}>{t}</option>)}</select></label>
          <label><input name="disponivel" type="checkbox" defaultChecked={p.disponivelOportunidades}/>Disponível para oportunidades</label>
          <fieldset><legend>Funções da área principal (até 5)</legend>{functions.data?.filter(f=>f.areaId===p.areaPrincipalId).map(f=><label key={f.id}><input type="checkbox" name="funcoes" value={f.id} defaultChecked={p.funcaoIds?.includes(f.id)}/>{f.nome}</label>)}</fieldset></>}
        </>:<><label>Nome da empresa<input name="nomeEmpresa" defaultValue={p.nomeEmpresa||''} maxLength={150}/></label><label>Tipo de perfil<input name="tipoPerfil" defaultValue={p.tipoPerfil||''} maxLength={100}/></label></>}
        <button className="yellow" disabled={busy}>{busy?'Salvando…':'Salvar alterações'}</button>
      </form></section>}
      {type==='artista'&&<><nav className="profile-tabs" aria-label="Seções do perfil">{['Portfólios','Publicações','Agenda','Medalhas'].map(t=><button key={t} className={tab===t?'selected':''} onClick={()=>t==='Agenda'&&own?navigate('/agenda'):setTab(t)}>{t}</button>)}</nav><section className="portfolio-panel">{tab==='Portfólios'?<Portfolio user={user} artistId={id} own={own} gallery={path==='/galeria'} navigate={navigate} notify={notify}/>:<div className="empty-tab"><h2>{tab}</h2><p>Conteúdo indisponível nesta versão.</p></div>}</section></>}
    </div></main>;
}

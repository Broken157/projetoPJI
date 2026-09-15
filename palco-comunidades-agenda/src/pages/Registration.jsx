import React,{useState} from 'react';
import useResource from '../lib/useResource';
import {post} from '../lib/api';
import {validateNewPassword} from '../lib/auth';
export default function Registration({navigate,capabilities}){
  const [role,setRole]=useState('ARTISTA'),[birth,setBirth]=useState(''),[busy,setBusy]=useState(false),[error,setError]=useState('');
  const areas=useResource('/areas',capabilities?.taxonomia===true);
  const now=new Date(),adult=birth&&new Date(Number(birth.slice(0,4))+18,Number(birth.slice(5,7))-1,Number(birth.slice(8,10)))<=now;
  async function submit(e){e.preventDefault();const form=new FormData(e.currentTarget),body=Object.fromEntries(form);setError('');const issue=validateNewPassword(body.senha,body.confirmacao);if(issue){setError(issue);return;}delete body.confirmacao;delete body.termos;
    if(body.areaPrincipalId)body.areaPrincipalId=Number(body.areaPrincipalId);
    setBusy(true);try{await post('/auth/cadastro',body,{auth:false});navigate('/login');}catch(e){setError(e.message);}finally{setBusy(false);}
  }
  return <main className="recovery-page"><section className="recovery-card"><h1>Crie sua conta</h1><form className="community-form" onSubmit={submit}>
    <label>Tipo de conta<select name="tipoUsuario" value={role} onChange={e=>setRole(e.target.value)}><option value="ARTISTA">Artista</option><option value="CONTRATANTE">Contratante</option></select></label>
    <label>Nome<input name="nome" maxLength={150} required/></label><label>E-mail<input name="email" type="email" maxLength={150} required autoComplete="email"/></label>
    <label>Telefone<input name="telefone" type="tel" maxLength={20} required/></label><label>Data de nascimento<input name="dataNascimento" type="date" required value={birth} onChange={e=>setBirth(e.target.value)}/></label>
    {birth&&!adult&&<fieldset><legend>Responsável legal</legend><label>Nome<input name="nomeResponsavel" required maxLength={150}/></label><label>Telefone<input name="telefoneResponsavel" required maxLength={20}/></label><label>E-mail<input type="email" name="emailResponsavel" required maxLength={150}/></label></fieldset>}
    {role==='ARTISTA'&&capabilities?.taxonomia===true&&<><label>Tipo de perfil<select name="tipoPerfilArtistico">{['ARTISTA_SOLO','DUPLA','BANDA','GRUPO_ARTISTICO','ESTUDIO','PRODUTORA_EMPRESA'].map(x=><option key={x}>{x}</option>)}</select></label><label>Área principal<select name="areaPrincipalId" required><option value="">Selecione</option>{areas.data?.map(a=><option key={a.id} value={a.id}>{a.nome}</option>)}</select></label></>}
    {role==='CONTRATANTE'&&<label>Tipo de perfil<input name="tipoPerfilContratante" maxLength={100} required/></label>}
    <label>Senha<input name="senha" type="password" autoComplete="new-password" required minLength={8} maxLength={72}/></label><small>Use maiúscula, minúscula, número e símbolo.</small><label>Confirmar senha<input name="confirmacao" type="password" required maxLength={72}/></label><label><input type="checkbox" name="termos" required/>Li os <a href="/termos">termos</a> e a <a href="/privacidade">política de privacidade</a>.</label>
    {error&&<p role="alert" className="auth-form-error">{error}</p>}<button className="yellow" disabled={busy}>{busy?'Cadastrando…':'Criar conta'}</button><button type="button" onClick={()=>navigate('/login')}>Já tenho uma conta</button>
  </form></section></main>;
}

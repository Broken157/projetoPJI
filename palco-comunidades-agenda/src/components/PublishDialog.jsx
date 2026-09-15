import React,{useRef,useState} from 'react';
import Modal from './Modal';
import Icon from './Icon';
import {Cover} from './Media';
import {validFile} from '../lib/storage';
import {metadataError} from '../lib/portfolio';
export default function PublishDialog({draft,onClose,onPublish,saving}){
 const [meta,setMeta]=useState({title:draft.title==='Meu portfólio'?'':draft.title||'',description:draft.description||'',cover:draft.cover||'',coverFile:draft.coverFile||null});
 const [error,setError]=useState('');const input=useRef();
 return <Modal className="publish-modal" title="Estamos quase lá!" back onClose={()=>onClose(meta)}><p className="publish-subtitle">Adicione as informações finais para a submissão do seu portfólio.</p><form className="publish-form" onSubmit={e=>{e.preventDefault();const issue=metadataError(meta);if(issue){setError(issue);return}onPublish({...meta,title:meta.title.trim()})}} noValidate>
  <div className="publish-cover"><label>Capa do projeto <span>(obrigatório)</span></label><input type="file" accept=".jpg,.jpeg,.png" ref={input} hidden onChange={e=>{const file=e.target.files?.[0];e.target.value='';if(!file)return;if(!validFile(file,'image')){setError('A capa deve ser JPG, JPEG ou PNG, com até 5 MB.');return}setMeta({...meta,coverFile:file,cover:''});setError('')}}/><button type="button" className="cover-upload" onClick={()=>input.current.click()} aria-label={meta.coverFile||meta.cover?'Trocar capa do projeto':'Adicionar capa do projeto'}>{meta.coverFile||meta.cover?<Cover item={meta}/>:<><span><Icon name="plus"/></span><small>Adicionar foto</small></>}</button></div>
  <div className="publish-fields"><label htmlFor="portfolio-title">Título <span>(obrigatório)</span></label><input id="portfolio-title" maxLength={100} required value={meta.title} onChange={e=>setMeta({...meta,title:e.target.value})}/><label htmlFor="portfolio-description">Descrição <span>(opcional)</span></label><textarea id="portfolio-description" rows={6} maxLength={2000} value={meta.description} onChange={e=>setMeta({...meta,description:e.target.value})}/><button className="yellow" disabled={saving}>{saving?'Publicando…':'Publicar'}</button></div>{error&&<p role="alert" className="publish-error">{error}</p>}
 </form></Modal>
}

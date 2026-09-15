import React,{useState,useEffect,useRef} from 'react';
import Icon from '../components/Icon';
import Modal from '../components/Modal';
import useResource from '../lib/useResource';
import {api,post,remove,safeUrl} from '../lib/api';
import {ApiState,Pagination} from '../components/ApiState';
function FileMedia({item,full=false}){
  const [url,setUrl]=useState(''),[error,setError]=useState('');
  useEffect(()=>{let active=true,objectUrl;
    if(!full&&item.tipo!=='IMAGEM')return;
    const path=item.contentUrl?.replace(/^\/api/,'');
    if(!path||!path.startsWith('/portfolio/'))return;
    api(path,{blob:true}).then(blob=>{objectUrl=URL.createObjectURL(blob);if(active)setUrl(objectUrl);}).catch(e=>active&&setError(e.message));
    return()=>{active=false;if(objectUrl)URL.revokeObjectURL(objectUrl);};
  },[item.id,item.contentUrl,item.tipo,full]);
  if(error)return <p role="alert">{error}</p>;
  if(item.tipo==='IMAGEM'&&url)return <img src={url} alt={item.nomeOriginal}/>;
  if(full&&url&&item.tipo==='AUDIO')return <audio src={url} controls/>;
  if(full&&url&&item.tipo==='PDF')return <a href={url} download={item.nomeOriginal}>Baixar PDF</a>;
  return <div className="media-file"><Icon name="upload"/><span>{item.nomeOriginal}</span></div>;
}
export default function Portfolio({user,artistId,own=true,gallery=false,editor=false,navigate,notify}){
  const [page,setPage]=useState(0),[videoPage,setVideoPage]=useState(0),[pending,setPending]=useState([]),[link,setLink]=useState(''),[busy,setBusy]=useState(false),[selected,setSelected]=useState(null),[deletion,setDeletion]=useState(null),[activeMedia,setActiveMedia]=useState(''),[linkModal,setLinkModal]=useState(false);
  const fileInput=useRef();
  function requestUpload(type){setActiveMedia(type);if(type==='video'){setLinkModal(true);return;}fileInput.current.accept={image:'.jpg,.jpeg,.png',audio:'.mp3',pdf:'.pdf'}[type];fileInput.current.click();}
  const root=own?'/portfolio/me':'/portfolio/publico/artistas/'+artistId;
  const files=useResource(root+'/arquivos?page='+page+'&size=12'), videos=useResource(root+'/videos?page='+videoPage+'&size=12');
  function choose(event){const input=event.target;const chosen=Array.from(input.files||[]);input.value='';
    if(chosen.some(f=>{const ext=f.name.split('.').pop().toLowerCase();const max={jpg:5,jpeg:5,png:5,pdf:10,mp3:20}[ext];return !max||!f.size||f.size>max*1024*1024;})){notify('Use JPG/JPEG/PNG até 5 MB, PDF até 10 MB ou MP3 até 20 MB.');return;}
    setPending(p=>[...p,...chosen]);
  }
  async function publish(){
    setBusy(true);let completed=0;
    try{
      for(const file of pending){const body=new FormData();body.append('arquivo',file);await post('/portfolio/arquivos',body);completed++;}
      if(link.trim()){await post('/portfolio/videos',{url:link.trim()});setLink('');}
      notify('Conteúdo publicado.');files.reload();videos.reload();
      if(editor)navigate('/perfil');
    }catch(e){notify((completed?'Alguns arquivos já foram publicados. ':'')+e.message);files.reload();videos.reload();}
    finally{setPending(p=>p.slice(completed));setBusy(false);}
  }
  const content=<><ApiState {...files} retry={files.reload} empty={!files.data?.content?.length}/>
    <div className={gallery?'photo-grid':'portfolio-grid'}>{files.data?.content?.filter(f=>!gallery||f.tipo==='IMAGEM').map(item=><article className="portfolio-card" key={item.id}>
      <button className="portfolio-open" onClick={()=>setSelected(item)}><FileMedia item={item}/><div className="portfolio-caption"><strong>{item.nomeOriginal}</strong><small>{new Date(item.dataUpload).toLocaleDateString('pt-BR')}</small></div></button>
      {own&&<button className="text-button" onClick={()=>setDeletion({id:item.id,type:'arquivos'})}>Excluir</button>}
    </article>)}{own&&<div className="create-card"><button onClick={()=>navigate('/portfolio/novo')}><span className="create-plus"><Icon name="plus"/></span><b>Criar um portfólio</b></button><p>Publique arquivos e links para mostrar seu trabalho.</p></div>}</div>
    <Pagination page={page} hasNext={files.data?.hasMore} onChange={setPage}/>
    {!gallery&&<><h2>Vídeos e áudio externo</h2><ApiState {...videos} retry={videos.reload} empty={!videos.data?.content?.length}/><div className="portfolio-grid">{videos.data?.content?.map(v=><article className="portfolio-card" key={v.id}><a href={safeUrl(v.urlOriginal)} target="_blank" rel="noreferrer">{v.provedor} — abrir conteúdo</a>{own&&<button onClick={()=>setDeletion({id:v.id,type:'videos'})}>Excluir</button>}</article>)}</div><Pagination page={videoPage} hasNext={videos.data?.hasMore} onChange={setVideoPage}/></>}
  </>;
  return <>{editor?<><div className="editor-toolbar"><button className="round back" onClick={()=>navigate('/perfil')} aria-label="Voltar ao perfil"><Icon name="arrow"/></button><span>Seu portfólio</span><div className="editor-actions"><button className="outline-yellow" disabled title="Rascunhos ainda não possuem armazenamento no servidor">Salvar como rascunho</button><button className="yellow" disabled={busy||(!pending.length&&!link.trim())} onClick={publish}>{busy?'Publicando…':'Publicar'}</button></div></div>
    <main className="editor-layout"><section className="editor-canvas" aria-label="Conteúdo do portfólio">
      <input hidden ref={fileInput} type="file" multiple disabled={busy} onChange={choose}/>
      {!pending.length&&!link?<div className="editor-empty"><h1>Comece a criação do seu portfólio:</h1><div className="start-options">{['image','audio','video'].map((type,i)=><button key={type} disabled={busy} onClick={()=>requestUpload(type)}><span className="circle-icon"><Icon name={type}/></span>{['Imagem','Áudio','Vídeo'][i]}</button>)}</div></div>:<div className="editor-content"><div className="editor-media-grid">{pending.map((f,i)=><div key={i}><PendingMedia file={f}/><div className="file-caption"><p>{f.name}</p><small>{(f.size/1024/1024).toLocaleString('pt-BR',{maximumFractionDigits:1})} MB</small></div><button className="text-button" disabled={busy} onClick={()=>setPending(p=>p.filter((_,n)=>n!==i))}>Remover {f.name}</button></div>)}</div>{link&&<div className="file-caption"><p>{link}</p><button className="text-button" disabled={busy} onClick={()=>setLink('')}>Remover link</button></div>}</div>}
    </section><aside className="editor-sidebar"><section className="add-content"><h2>Adicionar conteúdo</h2><div>{['audio','image','video'].map((type,i)=><button key={type} className={activeMedia===type?'media-selected':''} disabled={busy} onClick={()=>requestUpload(type)}><span className="circle-icon"><Icon name={type}/></span><span>{['Áudio','Imagem','Vídeo'][i]}</span></button>)}</div></section><section className="requirements">{[['Imagens','Formato permitido: JPG/JPEG/PNG;','Tamanho máximo: 5 MB;'],['PDF','Tamanho máximo: 10 MB;'],['Áudios','Formato permitido: MP3;','Tamanho máximo: 20 MB;'],['Vídeos e áudio externo','URL válida do YouTube, Vimeo ou Spotify;']].map(([title,...lines])=><div key={title}><h3>{title}</h3><ul>{lines.map(line=><li key={line}>{line}</li>)}</ul></div>)}</section><section className="attach-pdf"><h2>Anexar PDF</h2><button disabled={busy} onClick={()=>requestUpload('pdf')}><Icon name="upload"/>Adicionar PDF</button><button disabled={busy} onClick={()=>setLinkModal(true)}><Icon name="link"/>Adicionar link externo</button></section></aside></main></>:content}
    {linkModal&&<Modal title="Adicionar link externo" onClose={()=>setLinkModal(false)}><form className="community-form" onSubmit={e=>{e.preventDefault();setLinkModal(false);}}><label>URL do YouTube, Vimeo ou Spotify<input type="url" value={link} onChange={e=>setLink(e.target.value)} maxLength={255} required/></label><button className="yellow">Adicionar</button></form></Modal>}
    {selected&&<Modal title={selected.nomeOriginal} onClose={()=>setSelected(null)}><FileMedia item={selected} full/></Modal>}
    {deletion&&<Modal title="Excluir este conteúdo?" onClose={()=>setDeletion(null)}><p>A exclusão remove o conteúdo do seu portfólio.</p><button className="yellow" disabled={busy} onClick={async()=>{setBusy(true);try{await remove('/portfolio/'+deletion.type+'/'+deletion.id);setDeletion(null);files.reload();videos.reload();}catch(e){notify(e.message);}finally{setBusy(false);}}}>Confirmar exclusão</button></Modal>}
  </>;
}

function PendingMedia({file}){
  const [url,setUrl]=useState('');
  useEffect(()=>{const objectUrl=URL.createObjectURL(file);setUrl(objectUrl);return()=>URL.revokeObjectURL(objectUrl);},[file]);
  const ext=file.name.split('.').pop().toLowerCase();
  return ['jpg','jpeg','png'].includes(ext)?<img src={url} alt={file.name}/>:ext==='mp3'?<audio src={url} controls/>:<div className="media-file"><Icon name="upload"/><span>{file.name}</span></div>;
}

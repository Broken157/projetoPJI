import React,{useState,useEffect,useRef} from 'react';
import Icon from './Icon';
import {videoSource} from '../lib/portfolio';
export function useFileUrl(file){const [url,setUrl]=useState('');useEffect(()=>{if(!file){setUrl('');return}const u=URL.createObjectURL(file);setUrl(u);return()=>URL.revokeObjectURL(u)},[file]);return url}
export function Cover({item}){const url=useFileUrl(item?.coverFile);if(url||item?.cover)return <img src={url||item.cover} alt={item.title||'Capa do portfólio'}/>;return <Media item={item?.kind==='collection'?item.media[0]:item} compact/>}
export default function Media({item,compact=false}){
 const url=useFileUrl(item?.file);const audio=useRef();const [playing,setPlaying]=useState(false);const [failed,setFailed]=useState(false);const [active,setActive]=useState(false);
 if(!item)return null;
 if(item.kind==='audio')return <div className="audio-item"><button className="audio-play" aria-label={playing?'Pausar áudio':'Reproduzir áudio'} disabled={compact||!url} onClick={()=>{if(playing)audio.current.pause();else audio.current.play().catch(()=>setFailed(true))}}>{playing?'Ⅱ':<Icon name="video"/>}</button><div><strong>{item.name}</strong>{url&&!compact?<audio ref={audio} src={url} controls onPlay={()=>setPlaying(true)} onPause={()=>setPlaying(false)} onEnded={()=>setPlaying(false)} onError={()=>setFailed(true)}/>:<small>Arquivo de áudio não incluído na referência.</small>}{failed&&<small role="alert">Não foi possível reproduzir este áudio.</small>}</div></div>;
 if(item.kind==='video'){
  const source=videoSource(item.url||'');
  return <figure className="video-item"><div className="video-surface">{item.poster&&!active&&<img src={item.poster} alt="Prévia do vídeo"/>}{compact?<Icon name="video"/>:source?.type==='embed'&&active?<iframe src={source.url} title={item.caption||'Vídeo do portfólio'} allow="fullscreen; picture-in-picture" allowFullScreen referrerPolicy="strict-origin-when-cross-origin"/>:source?.type==='file'?<video src={source.url} controls preload="metadata" onError={()=>setFailed(true)}/>:source?.type==='external'?<a href={source.url} target="_blank" rel="noreferrer"><Icon name="video"/>Abrir vídeo em nova aba</a>:<button className="video-play" aria-label="Reproduzir vídeo" onClick={()=>source?setActive(true):setFailed(true)}><Icon name="video"/></button>}</div>{failed&&<p className="media-error" role="status">{item.sample?'O vídeo original não foi fornecido. Esta é a prévia visual.':'Não foi possível reproduzir o vídeo.'}</p>}{item.caption&&<figcaption><small>Legenda</small><p>{item.caption}</p></figcaption>}</figure>
 }
 if(item.cover)return <img src={item.cover} alt={item.title||item.name}/>;
 if(item.kind==='image')return <img src={url} alt={item.name}/>;
 if(item.kind==='pdf'&&!compact&&url)return <iframe className="pdf-frame" src={url} title={item.name}/>;
 return <div className="media-file"><Icon name="upload"/><span>{item.name}</span></div>;
}

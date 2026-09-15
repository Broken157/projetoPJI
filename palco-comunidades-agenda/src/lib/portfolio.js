import {safeUrl} from './storage';
export function videoSource(value){
 const safe=safeUrl(value);if(!safe)return null;
 const u=new URL(safe);const host=u.hostname.toLowerCase().replace(/^www\./,'');
 if(['youtube.com','m.youtube.com','youtu.be'].includes(host)){
  const id=host==='youtu.be'?u.pathname.split('/')[1]:u.searchParams.get('v')||u.pathname.match(/^\/(?:shorts|embed)\/([^/]+)/)?.[1];
  return /^[a-zA-Z0-9_-]{11}$/.test(id||'')?{type:'embed',url:`https://www.youtube-nocookie.com/embed/${id}`} : null;
 }
 if(host==='vimeo.com'||host==='player.vimeo.com'){
  const id=u.pathname.match(/(?:^\/|\/video\/)(\d+)\/?$/)?.[1];
  return id?{type:'embed',url:`https://player.vimeo.com/video/${id}`}:null;
 }
 if(/\.(mp4|webm|ogg)$/i.test(u.pathname))return {type:'file',url:safe};
 return {type:'external',url:safe};
}
export function canPublish(draft){return !!draft&&(draft.kind!=='collection'||draft.media?.length>0)}
export function metadataError(meta){if(!meta.title?.trim())return 'Adicione um título ao seu portfólio.';if(!meta.coverFile&&!meta.cover)return 'Adicione uma capa para o projeto.';return ''}
export function canDelete(value){return value==='EXCLUIR'}

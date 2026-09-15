// Access token only in memory. The refresh cookie is HttpOnly and managed by the server.
export const API_BASE=(import.meta.env.VITE_API_URL||'/api').replace(/\/$/,'');
let accessToken=null, refreshing=null, generation=0;
export class ApiError extends Error {
  constructor(message,status=0,details=null){super(message);this.status=status;this.details=details;}
}
export const getAccessToken=()=>accessToken;
export function setAccessToken(token){generation++;accessToken=token||null;}
export function clearSession(){setAccessToken(null);window.dispatchEvent(new Event('palco:session-ended'));}
export function query(values={}){
  const q=new URLSearchParams();
  Object.entries(values).forEach(([key,value])=>{
    if(value!==undefined&&value!==null&&value!=='')q.set(key,Array.isArray(value)?value.join(','):String(value));
  });
  return q.size?'?'+q.toString():'';
}
async function decode(response){
  if(response.status===204)return null;
  const type=response.headers.get('content-type')||'';
  return type.includes('json')?response.json():null;
}
export async function refreshSession(){
  if(!refreshing){
    const current=generation;
    refreshing=(async()=>{
      let response;
      try{response=await fetch(API_BASE+'/auth/refresh',{method:'POST',credentials:'include',headers:{'X-Palco-Session':'1'}});}
      catch{throw new ApiError('Não foi possível conectar ao servidor.');}
      const data=await decode(response);
      if(!response.ok||!data?.token)throw new ApiError(data?.message||'Sua sessão expirou. Entre novamente.',response.status);
      if(current!==generation)throw new ApiError('Sessão alterada.',401);
      accessToken=data.token;
      return data;
    })().finally(()=>{refreshing=null;});
  }
  return refreshing;
}
export async function api(path,{method='GET',body,auth=true,retry=true,signal,blob=false}={}){
  const headers={Accept:blob?'*/*':'application/json','X-Palco-Session':'1'};
  if(auth&&accessToken)headers.Authorization='Bearer '+accessToken;
  if(body!==undefined&&!(body instanceof FormData))headers['Content-Type']='application/json';
  let response;
  try{response=await fetch(API_BASE+path,{method,headers,credentials:'include',signal,
    ...(body!==undefined?{body:body instanceof FormData?body:JSON.stringify(body)}:{})});}
  catch(error){if(error.name==='AbortError')throw error;throw new ApiError('Não foi possível conectar ao servidor. Tente novamente.');}
  if(response.status===401&&auth&&retry){
    try{await refreshSession();}
    catch(error){if(error.status===401||error.status===403||error.status===404)clearSession();throw error;}
    return api(path,{method,body,auth,retry:false,signal,blob});
  }
  if(response.ok&&blob)return response.blob();
  const data=await decode(response);
  if(!response.ok){
    if(response.status===401&&auth)clearSession();
    throw new ApiError(data?.message||data?.mensagem||data?.detail||
      (response.status===403?'Você não tem permissão para esta ação.':response.status===404?'Recurso não encontrado.':'Não foi possível concluir a operação.'),response.status,data);
  }
  return data;
}
export const get=(path,options)=>api(path,options);
export const post=(path,body,options)=>api(path,{...options,method:'POST',body});
export const put=(path,body)=>api(path,{method:'PUT',body});
export const patch=(path,body)=>api(path,{method:'PATCH',body});
export const remove=(path)=>api(path,{method:'DELETE'});
export function safeUrl(value){try{const url=new URL(value);return ['https:','http:'].includes(url.protocol)?url.href:'';}catch{return '';}}


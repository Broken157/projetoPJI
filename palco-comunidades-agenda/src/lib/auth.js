import {get,post,setAccessToken,getAccessToken,refreshSession,clearSession,ApiError} from './api';
export {ApiError as AuthError};
export function canonicalUser(data){
  if(!data)return null;
  if(!['ARTISTA','CONTRATANTE'].includes(data.tipoUsuario))throw new ApiError('Tipo de conta não suportado.',403);
  return {...data,name:data.nome,role:data.tipoUsuario.toLowerCase()};
}
export async function getSession(){
  try{if(!getAccessToken())await refreshSession();return canonicalUser(await get('/usuarios/me'));}
  catch(error){if([401,403,404].includes(error.status))return null;throw error;}
}
export async function login({email,password,rememberMe=false}){
  const result=await post('/auth/login',{email:email.trim(),senha:password,rememberMe},{auth:false});
  setAccessToken(result.token);
  return canonicalUser(await get('/usuarios/me'));
}
export async function logout(){await post('/auth/logout',undefined,{retry:false});clearSession();}
export async function recover(email){
  const result=await post('/auth/forgot-password',{email:email.trim()},{auth:false});
  return {message:result?.mensagem||result?.message||'Se houver uma conta com esse e-mail, você receberá as instruções de recuperação.'};
}
export function validateNewPassword(password,confirmation){
  if(password.length<8||new TextEncoder().encode(password).length>72||!/[A-Z]/.test(password)||!/[a-z]/.test(password)||!/[0-9]/.test(password)||!/[\W_]/.test(password))
    return 'Use de 8 a 72 caracteres, com maiúscula, minúscula, número e símbolo.';
  return password!==confirmation?'As senhas não coincidem.':'';
}
export async function resetPassword({token,password,confirmation}){
  const issue=validateNewPassword(password,confirmation);
  if(issue)throw new ApiError(issue);
  if(!token)throw new ApiError('Link inválido. Solicite uma nova recuperação.');
  await post('/auth/reset-password',{token,novaSenha:password},{auth:false});clearSession();
}
export async function googleLogin(){throw new ApiError('Use seu e-mail e senha. O acesso Google depende da configuração do provedor nesta instalação.');}
export function isPublicRoute(path){
  return ['/login','/cadastro','/recuperar-senha','/redefinir-senha','/termos','/privacidade','/vagas'].includes(path)
    ||/^\/vagas\/\d+$/.test(path)||/^\/perfis\/(artista|contratante)\/\d+$/i.test(path);
}
export function canAccessRoute(user,path){
  if(!user)return isPublicRoute(path);
  if(path.startsWith('/portfolio/')||['/galeria','/agenda'].includes(path))return user.role==='artista';
  if(path.startsWith('/contratante')||path==='/talentos'||path==='/minhas-vagas'||path==='/vagas/nova'||/^\/vagas\/\d+\/(editar|gerenciar)$/.test(path))return user.role==='contratante';
  return true;
}

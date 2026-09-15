import {useEffect,useState,useCallback} from 'react';
import {get} from './api';
export default function useResource(path,enabled=true,interval=0){
  const [state,setState]=useState({data:null,loading:enabled,error:null});
  const [revision,setRevision]=useState(0);
  const reload=useCallback(()=>setRevision(n=>n+1),[]);
  useEffect(()=>{
    if(!enabled||!path){setState({data:null,loading:false,error:null});return;}
    let active=true,inFlight=false;
    const controller=new AbortController();
    setState({data:null,loading:true,error:null});
    async function read(){if(inFlight)return;inFlight=true;try{
      const data=await get(path,{signal:controller.signal});
      if(active)setState({data,loading:false,error:null});
    }catch(error){if(active&&error.name!=='AbortError')setState(s=>({...s,loading:false,error}));}finally{inFlight=false;}}
    read();
    const timer=interval?setInterval(()=>{if(!document.hidden)read();},interval):null;
    return()=>{active=false;controller.abort();clearInterval(timer);};
  },[path,enabled,revision,interval]);
  return {...state,reload};
}

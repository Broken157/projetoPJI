const DB='palco-portfolio';
function open(){return new Promise((resolve,reject)=>{const r=indexedDB.open(DB,1);r.onupgradeneeded=()=>r.result.createObjectStore('state');r.onsuccess=()=>resolve(r.result);r.onerror=()=>reject(r.error)})}
export async function readState(){const db=await open();return new Promise((resolve,reject)=>{const t=db.transaction('state');const r=t.objectStore('state').get('portfolio');r.onsuccess=()=>resolve(r.result);r.onerror=()=>reject(r.error);t.oncomplete=()=>db.close()})}
export async function saveState(state){const db=await open();return new Promise((resolve,reject)=>{const t=db.transaction('state','readwrite');t.objectStore('state').put(state,'portfolio');t.oncomplete=()=>{db.close();resolve()};t.onerror=()=>{db.close();reject(t.error)}})}
export function validFile(file,type){const rules={pdf:[['pdf'],10],image:[['jpg','jpeg','png'],5],audio:[['mp3'],20]};const [extensions,mb]=rules[type];return extensions.includes(file.name.split('.').pop().toLowerCase())&&file.size>0&&file.size<=mb*1024*1024}
export function safeUrl(value){try{const u=new URL(value);return ['https:','http:'].includes(u.protocol)?u.href:null}catch{return null}}

export async function readAccountData(userId,key){const db=await open();return new Promise((resolve,reject)=>{const t=db.transaction('state');const r=t.objectStore('state').get('account:'+userId+':'+key);r.onsuccess=()=>resolve(r.result);r.onerror=()=>reject(r.error);t.oncomplete=()=>db.close()})}
export async function saveAccountData(userId,key,data){const db=await open();return new Promise((resolve,reject)=>{const t=db.transaction('state','readwrite');t.objectStore('state').put(data,'account:'+userId+':'+key);t.oncomplete=()=>{db.close();resolve()};t.onerror=()=>{db.close();reject(t.error)}})}

import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'
const state=JSON.parse(await readFile('/tmp/relayflow-private-browser.json','utf8'))
const base='http://localhost:18081'
async function api(path,token,body,method=body?'POST':'GET',code=0){const r=await fetch(base+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`}:{})},body:body?JSON.stringify(body):undefined});const j=await r.json();assert.equal(j.code,code,path);return j.data}
const login=async mobile=>(await api('/admin-api/system/auth/login',null,{username:mobile,password:state.password,tenantId:state.tenant})).accessToken
const a=await login(state.mobile),b=await login(state.mobile.replace(/^1381/,'1382'))
const bId=JSON.parse(Buffer.from(b.split('.')[1],'base64url')).sub
const body=text=>({type:'doc',content:[{type:'paragraph',content:[{type:'text',text}]}]})
for(const order of ['open-first','save-first']){
 const doc=await api('/app-api/docs/library/nodes',a,{title:'old title'})
 const metadata=async()=>{await api(`/app-api/docs/documents/${doc.objectId}`,a);await api(`/app-api/docs/library/nodes/${doc.nodeId}`,a,{title:'new title'},'PUT')}
 const save=()=>api(`/app-api/docs/documents/${doc.objectId}/body`,a,{body:body('new body'),contentVersion:0},'PUT')
 await Promise.all(order==='open-first'?[metadata(),save()]:[save(),metadata()])
 const current=await api(`/app-api/docs/documents/${doc.objectId}`,a);assert.equal(current.title,'new title');assert.equal(current.body.content[0].content[0].text,'new body');assert.equal(current.contentVersion,1)
 await api(`/app-api/docs/documents/${doc.objectId}/body`,a,{body:body('stale'),contentVersion:0},'PUT',1006001003)
 const shared=await api(`/app-api/docs/documents/${doc.objectId}/shares`,a,{granteeUserId:bId,permission:'VIEW'})
 await api(`/app-api/docs/documents/${doc.objectId}/body`,b,{body:body('forbidden'),contentVersion:1},'PUT',1006001002)
 const shares=await api(`/app-api/docs/documents/${doc.objectId}/shares`,a)
 await api(`/app-api/docs/documents/${doc.objectId}/shares/${shares.shares[0].shareId}`,a,undefined,'DELETE')
 await api(`/app-api/docs/documents/${doc.objectId}`,b,undefined,'GET',1006001002)
 await api(`/app-api/docs/library/nodes/${doc.nodeId}`,a,undefined,'DELETE')
 await api(`/app-api/docs/documents/${doc.objectId}/body`,a,{body:body('deleted'),contentVersion:1},'PUT',1006001001)
}
console.log('ok - real concurrent open/rename/save retain new title and body; CAS stale version, VIEW, revoked access and deletion all reject')

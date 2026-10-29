import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
import {randomUUID} from 'node:crypto'
import {writeFile} from 'node:fs/promises'
const base='http://localhost:18081'
const sql=q=>execFileSync('docker',['exec','relayflow-member-pg','psql','-U','relayflow','-d','member_auth_test','-At','-c',q],{encoding:'utf8'}).trim()
const id=v=>{assert.match(String(v),/^\d+$/);return String(v)}
async function api(path,token,body,method=body?'POST':'GET',ok=true){
 const r=await fetch(base+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`}:{})},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(15000)})
 const j=await r.json();if(ok)assert.equal(j.code,0,path);else assert.notEqual(j.code,0,path);return j.data
}
async function download(file,token,allowed){const r=await fetch(`${base}/app-api/infra/file/download?fileId=${file}`,{headers:{Authorization:`Bearer ${token}`},redirect:'manual'});if(allowed){assert.equal(r.status,302);const u=r.headers.get('location');assert.equal(new URL(u).searchParams.get('X-Amz-Expires'),'900');assert.equal((await fetch(u)).status,200);return u}else{assert.notEqual(r.status,302);assert.equal(r.headers.get('location'),null)}}
const suffix=String(Date.now()).slice(-7),password='test-only-123'
const a=await api('/app-api/system/auth/register',null,{mobile:'1381'+suffix,password,nickname:'文件甲',tenantName:'文件授权验收'})
const b=await api('/app-api/system/auth/register',null,{mobile:'1382'+suffix,password,nickname:'文件乙',tenantName:'另一企业'})
const uid=t=>JSON.parse(Buffer.from(t.split('.')[1],'base64url')).sub
const ta=id(a.tenantId),ua=id(uid(a.accessToken)),ub=id(uid(b.accessToken))
sql(`INSERT INTO sys_tenant_user(id,tenant_id,user_id,status) VALUES(${id(BigInt(ub)+500n)},${ta},${ub},'ACTIVE')`)
const bt=(await api('/admin-api/system/auth/login',null,{username:'1382'+suffix,password,tenantId:ta})).accessToken
const at=a.accessToken
const bytes=Buffer.from('private file access '+randomUUID())
async function upload(access='private'){
 const session=await api('/app-api/infra/file/upload-session',at,{filename:'test.txt',size:bytes.length,mimeType:'text/plain',accessLevel:access})
 const put=await fetch(session.uploadUrl,{method:'PUT',headers:session.headers,body:bytes});assert.equal(put.status,200)
 return (await api('/app-api/infra/file/upload-confirm',at,{uploadId:session.uploadId,size:bytes.length,etag:put.headers.get('etag')})).fileId
}
const file=id(await upload());await download(file,at,true);await download(file,bt,false);await download(file,b.accessToken,false)
const payload={version:1,blocks:[{type:'file',fileId:file,filename:'test.txt'}]}
const before=sql(`SELECT count(*) FROM im_message WHERE tenant_id=${ta}`)
await api('/app-api/im/message/send',bt,{peerUserId:ub,clientMsgId:randomUUID(),type:'file',content:payload},'POST',false)
assert.equal(sql(`SELECT count(*) FROM im_message WHERE tenant_id=${ta}`),before)
const message=await api('/app-api/im/message/send',at,{peerUserId:ub,clientMsgId:randomUUID(),type:'file',content:payload})
await download(file,bt,true)
sql(`UPDATE im_conversation_member SET deleted=1 WHERE tenant_id=${ta} AND conversation_id=${id(message.conversationId)} AND subject_id=${ub} AND subject_type='user'`)
await download(file,bt,false)
sql(`UPDATE im_conversation_member SET deleted=0 WHERE tenant_id=${ta} AND conversation_id=${id(message.conversationId)} AND subject_id=${ub} AND subject_type='user'`)
const forward=await api('/app-api/im/message/forward',bt,{messageId:message.messageId??message.id??message.msgId,targetConversationId:message.conversationId,clientMsgId:randomUUID()})
await api('/app-api/im/message/recall',at,{messageId:message.messageId??message.id??message.msgId});await download(file,bt,true)
await api('/app-api/im/message/recall',bt,{messageId:forward.messageId??forward.id});await download(file,bt,false);await download(file,at,false)
console.log('ok - creator preview, same/cross tenant denial, forged reference rejection, legitimate recipient/forward and recall union')
const driveFile=id(await upload());const item=await api('/app-api/docs/drive/files',at,{fileId:driveFile})
sql(`UPDATE doc_object SET owner_user_id=${ub} WHERE id=${id(item.objectId)} AND tenant_id=${ta}`)
await download(driveFile,at,false);await download(driveFile,bt,true)
const admin=await fetch(`${base}/admin-api/infra/file/${driveFile}/download`,{headers:{Authorization:`Bearer ${at}`},redirect:'manual'});assert.equal(admin.status,302)
sql(`UPDATE doc_object SET lifecycle='DELETED' WHERE id=${id(item.objectId)}`);await download(driveFile,bt,false)
const publicFile=id(await upload('public'));const pub=await fetch(`${base}/app-api/infra/file/public/${publicFile}`,{redirect:'manual'});assert.equal(pub.status,302)
await writeFile('/tmp/relayflow-private-browser.json',JSON.stringify({mobile:'1381'+suffix,password,tenant:ta,uid:ua,file,driveFile,publicFile}),{mode:0o600})
console.log('ok - current FILE ownership, lifecycle, separate administrator permission and public files; 15 minute signing TTL')
// Isolated-database fault injection: a binding failure must roll back the message and emit no ACK.
const faultFile=id(await upload());const faultId=randomUUID();const count=sql(`SELECT count(*) FROM im_message WHERE tenant_id=${ta}`)
sql(`CREATE OR REPLACE FUNCTION test_private_binding_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.file_id=${faultFile} THEN RAISE EXCEPTION 'test binding failure'; END IF; RETURN NEW; END $$; CREATE TRIGGER test_private_binding_failure BEFORE INSERT ON infra_file_binding FOR EACH ROW EXECUTE FUNCTION test_private_binding_failure()`)
const ws=new WebSocket(`ws://localhost:18081/infra/ws?token=${encodeURIComponent(at)}`);const frames=[];ws.onmessage=e=>frames.push(JSON.parse(e.data));ws.onerror=()=>{}
try{
 await new Promise((resolve,reject)=>{ws.onopen=resolve;setTimeout(()=>reject(new Error('WS timeout')),5000).unref()})
 ws.send(JSON.stringify({domain:'im',type:'message.send',requestId:faultId,payload:{peerUserId:ub,clientMsgId:faultId,type:'file',content:{version:1,blocks:[{type:'file',fileId:faultFile}]}}}))
 await new Promise(resolve=>setTimeout(resolve,600))
 assert.equal(sql(`SELECT count(*) FROM im_message WHERE tenant_id=${ta}`),count)
 assert.ok(!frames.some(f=>f.requestId===faultId&&f.type==='message.ack'))
}finally{ws.close();sql('DROP TRIGGER test_private_binding_failure ON infra_file_binding; DROP FUNCTION test_private_binding_failure()')}
console.log('ok - binding failure rolls back message in PostgreSQL and sends no WS ACK')

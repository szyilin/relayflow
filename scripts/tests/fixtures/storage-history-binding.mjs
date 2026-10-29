// Isolated PostgreSQL + MinIO acceptance. Start the server with the test environment first.
import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
import {randomUUID} from 'node:crypto'
import {readFile,writeFile} from 'node:fs/promises'
const base='http://localhost:18081',statePath='/tmp/relayflow-storage-browser.json'
const sql=q=>execFileSync('docker',['exec','relayflow-member-pg','psql','-U','relayflow','-d','member_auth_test','-At','-c',q],{encoding:'utf8'}).trim()
let token
async function api(path,body,method=body?'POST':'GET',expected=0){
 const r=await fetch(base+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`}:{})},body:body?JSON.stringify(body):undefined})
 const result=await r.json();assert.equal(result.code,expected,`${path}: ${result.msg}`);return result.data
}
async function read(file,bucket,bytes,access){
 const r=await fetch(`${base}/app-api/infra/file/download?fileId=${file}`,{headers:{Authorization:`Bearer ${token}`},redirect:'manual'})
 assert.equal(r.status,302);const url=new URL(r.headers.get('location'));assert.ok(url.pathname.startsWith(`/${bucket}/`));if(access)assert.ok(url.searchParams.get('X-Amz-Credential').startsWith(access+'/'))
 const object=await fetch(url);assert.equal(object.status,200);assert.equal(await object.text(),bytes)
}
if(process.argv.includes('--verify-restart')){
 const state=JSON.parse(await readFile(statePath,'utf8'));token=(await api('/admin-api/system/auth/login',{username:state.mobile,password:state.password,tenantId:state.tenant})).accessToken
 for(const f of state.files)await read(f.id,f.bucket,f.bytes,f.access)
 console.log('ok - after restart, A/B/C objects retain their bucket, prefix, credentials and bytes');process.exit(0)
}
if(process.argv.includes('--verify-access')){
 const state=JSON.parse(await readFile(statePath,'utf8'))
 const mobile='1372'+String(Date.now()).slice(-7),password='test-only-123'
 const other=await api('/app-api/system/auth/register',{mobile,password,nickname:'存储权限验收',tenantName:'隔离企业'})
 token=other.accessToken
 const denied=await fetch(`${base}/app-api/infra/file/download?fileId=${state.files[0].id}`,{headers:{Authorization:`Bearer ${token}`},redirect:'manual'});assert.notEqual(denied.status,302)
 const uid=String(JSON.parse(Buffer.from(token.split('.')[1],'base64url')).sub);assert.match(uid,/^\d+$/);assert.match(state.tenant,/^\d+$/)
 sql(`INSERT INTO sys_tenant_user(id,tenant_id,user_id,status) VALUES(${BigInt(uid)+500n},${state.tenant},${uid},'ACTIVE')`)
 token=(await api('/admin-api/system/auth/login',{username:mobile,password,tenantId:state.tenant})).accessToken
 for(const [path,method,body] of [
  ['/config','GET'],['/config','PUT',{provider:'minio',operation:'rotate_credentials',revisionId:state.bRevision,accessKey:'missing',secretKey:'invalid-secret'}],
  ['/test-connection','POST',{source:'bootstrap'}],['/config?provider=minio','DELETE'],['/effective-source','PUT',{source:'bootstrap'}]
 ]){
  const r=await fetch(base+'/admin-api/infra/storage'+path,{method,headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});assert.equal(r.status,403)
 }
 console.log('ok - cross-tenant file denial; member without storage permissions receives HTTP 403 for query/save/rotation/test/delete/activation');process.exit(0)
}
const mobile='1371'+String(Date.now()).slice(-7),password='test-only-123'
const user=await api('/app-api/system/auth/register',{mobile,password,nickname:'存储验收',tenantName:'历史存储绑定验收'});token=user.accessToken
const tenant=String(user.tenantId);assert.match(tenant,/^\d+$/)
const config=()=>api('/admin-api/infra/storage/config')
const save=(body,code=0)=>api('/admin-api/infra/storage/config',{provider:'minio',...body},'PUT',code)
const source=value=>api('/admin-api/infra/storage/effective-source',{source:value},'PUT')
const input={endpoint:'http://localhost:19000',bucket:'storage-b',accessKey:'relayflow_test',secretKey:'relayflow_test_only',pathPrefix:'b/'}
const files=[]
async function pending(){
 const bytes='storage-history '+randomUUID();const s=await api('/app-api/infra/file/upload-session',{filename:'storage.txt',size:Buffer.byteLength(bytes),mimeType:'text/plain',accessLevel:'private'})
 const put=await fetch(s.uploadUrl,{method:'PUT',headers:s.headers,body:bytes});assert.equal(put.status,200)
 return {...s,bytes,etag:put.headers.get('etag'),bucket:new URL(s.uploadUrl).pathname.split('/')[1]}
}
async function confirm(s){const f=await api('/app-api/infra/file/upload-confirm',{uploadId:s.uploadId,size:Buffer.byteLength(s.bytes),etag:s.etag});files.push({id:String(f.fileId),bytes:s.bytes,bucket:s.bucket});return String(f.fileId)}
const a=await pending();assert.equal(a.bucket,'relayflow');await save(input)
let c=await config();assert.equal(c.effectiveSource,'bootstrap');assert.ok(c.providers[0].revisionId);assert.equal(c.providers[0].activeRevisionId,null)
const af=await confirm(a);await read(af,'relayflow',a.bytes)
await source('tenant');const b=await pending();assert.equal(b.bucket,'storage-b');assert.match(b.objectKey,/^b\/tenant\//)
await source('bootstrap');const bf=await confirm(b);await read(bf,'storage-b',b.bytes)
await source('tenant');const pendingB=await pending();const bRevision=(await config()).providers[0].revisionId
await save({...input,bucket:'storage-c',pathPrefix:'c/'},2002001002)
await save({...input,bucket:'storage-c',pathPrefix:'c/',createNewLocation:true})
c=await config();assert.equal(c.providers[0].activeRevisionId,bRevision);assert.notEqual(c.providers[0].revisionId,bRevision)
const stillB=await pending();assert.equal(stillB.bucket,'storage-b');await confirm(stillB)
await source('tenant');const cs=await pending();assert.equal(cs.bucket,'storage-c');assert.match(cs.objectKey,/^c\/tenant\//);const cf=await confirm(cs)
await confirm(pendingB);await read(af,'relayflow',a.bytes);await read(bf,'storage-b',b.bytes);await read(cf,'storage-c',cs.bytes)
console.log('ok - saved draft does not activate; PUT/confirm and old downloads retain A/B/C location and full prefix')
await save({operation:'rotate_credentials',revisionId:bRevision,accessKey:'missing-access',secretKey:'invalid-secret'},2002001003);await read(bf,'storage-b',b.bytes)
await save({operation:'rotate_credentials',revisionId:bRevision,accessKey:'relayflow_rotated',secretKey:'rotated_test_only'})
await read(bf,'storage-b',b.bytes,'relayflow_rotated');for(const f of files)if(f.bucket==='storage-b')f.access='relayflow_rotated'
await save({...input,bucket:'storage-c',pathPrefix:'c/',secretKey:'different-secret'},2002001007)
await api('/admin-api/infra/storage/config?provider=minio',undefined,'DELETE',2002001002)
c=await config();const response=JSON.stringify(c);for(const secret of ['relayflow_test_only','rotated_test_only','secret_key_enc'])assert.ok(!response.includes(secret))
console.log('ok - failed rotation preserves credentials; explicit successful rotation preserves location; referenced deletion and implicit rotation rejected')
// Clone only this fixture's rows. NULL binding must never resolve through the current default.
sql(`INSERT INTO infra_file(id,tenant_id,provider,storage_uri,object_key,original_name,size,creator,access_level) SELECT id+1,tenant_id,provider,storage_uri,object_key,original_name,size,creator,access_level FROM infra_file WHERE id=${af}`)
const unknown=String(BigInt(af)+1n);await api(`/app-api/infra/file/download?fileId=${unknown}`,undefined,'GET',2002001006)
const oldSession=await pending();sql(`UPDATE infra_file_upload_session SET storage_config_revision_id=NULL WHERE id=${oldSession.uploadId} AND tenant_id=${tenant}`)
await api('/app-api/infra/file/upload-confirm',{uploadId:oldSession.uploadId,size:Buffer.byteLength(oldSession.bytes),etag:oldSession.etag},'POST',2002001006)
c=await config();assert.equal(Number(c.unresolvedFiles),1);assert.equal(Number(c.unresolvedSessions),1)
await writeFile(statePath,JSON.stringify({mobile,password,tenant,files,bRevision,unknown}),{mode:0o600})
console.log('ok - unresolved historic file/session return stable errors without fallback; secret-free summaries and unresolved counts')

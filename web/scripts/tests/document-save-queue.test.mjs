import {test} from 'node:test'
import assert from 'node:assert/strict'
import {DocumentSaveQueue} from '../../src/utils/documentSaveQueue.ts'
const deferred=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b});return {promise,resolve,reject}}
test('R1 response keeps R2 and serializes next save with confirmed version',async()=>{
 const calls=[],first=deferred(),second=deferred()
 const queue=new DocumentSaveQueue((id,body,version)=>{calls.push({id,body,version});return calls.length===1?first.promise:second.promise},()=>{},()=>false)
 queue.scope('u1:t1');queue.open('a',{text:'base'},0);queue.edit('a',{text:'R1'},60000)
 const saving=queue.flush('a');queue.edit('a',{text:'R2'},60000)
 assert.equal(calls.length,1);first.resolve(1);await new Promise(r=>setImmediate(r))
 assert.equal(queue.get('a').body.text,'R2');assert.deepEqual(calls[1],{id:'a',body:{text:'R2'},version:1})
 second.resolve(2);await saving;assert.equal(queue.get('a').status,'saved');assert.equal(queue.hasUnsaved(),false)
 queue.cancel()
})
test('debounce captures original document and flush drains latest queued edit',async()=>{
 const calls=[];const queue=new DocumentSaveQueue(async(id,body,version)=>{calls.push({id,body,version});return version+1},()=>{},()=>false)
 queue.open('a',{text:'a'},0);queue.open('b',{text:'b'},3);queue.edit('a',{text:'A'},10)
 await new Promise(r=>setTimeout(r,30));assert.equal(calls[0].id,'a');assert.equal(queue.get('b').version,3);queue.cancel()
})
test('conflict preserves draft and original CAS baseline; opening stale data cannot reset dirty draft',async()=>{
 const conflict=new Error('conflict');const queue=new DocumentSaveQueue(async()=>{throw conflict},()=>{},e=>e===conflict)
 queue.open('a',{text:'base'},4);queue.edit('a',{text:'local'},60000);await assert.rejects(queue.flush('a'))
 queue.open('a',{text:'server'},9);assert.equal(queue.get('a').body.text,'local');assert.equal(queue.get('a').version,4)
 assert.equal(queue.get('a').status,'conflict');assert.equal(queue.hasUnsaved(),true);queue.cancel()
})
test('failure retains snapshot, retries original version and merges latest revision',async()=>{
 let fail=true;const calls=[];const queue=new DocumentSaveQueue(async(id,body,version)=>{calls.push({id,body,version});if(fail)throw new Error('offline');return version+1},()=>{},()=>false)
 queue.open('a',{text:'base'},2);queue.edit('a',{text:'local'},60000);await assert.rejects(queue.flush('a'))
 queue.edit('a',{text:'latest'},60000);fail=false;await queue.retry('a');assert.equal(calls[1].version,2);assert.equal(calls[1].body.text,'latest');queue.cancel()
})
test('identity switch aborts flight and never dispatches queued old body under new identity',async()=>{
 const first=deferred(),calls=[];let aborted=false
 const queue=new DocumentSaveQueue((id,body,version,signal)=>{signal.addEventListener('abort',()=>{aborted=true});calls.push({id,body,version});return first.promise},()=>{},()=>false)
 queue.scope('u1:t1');queue.open('a',{text:'base'},0);queue.edit('a',{text:'old'},60000);const flight=queue.flush('a');queue.edit('a',{text:'later'},60000)
 queue.scope('u2:t2');first.resolve(1);await flight;assert.equal(aborted,true);assert.equal(calls.length,1);assert.equal(queue.get('a'),undefined);assert.equal(queue.hasUnsaved(),false)
})

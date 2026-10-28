import assert from 'node:assert/strict'

const base = process.env.TEST_BASE_URL || 'http://web'
for (const path of ['/app/login', '/app/messages/deep-link', '/admin/users']) {
  const response = await fetch(base + path)
  assert.equal(response.status, 200)
  assert.match(await response.text(), /<html/i)
}
console.log('ok - SPA deep links')

for (const path of ['/app-api/missing?value=kept', '/admin-api/missing']) {
  const response = await fetch(base + path)
  assert.equal(response.status, 404)
  assert.match(response.headers.get('content-type'), /json/)
  assert.equal((await response.json()).path, path)
}
assert.equal((await fetch(base + '/assets/nonexistent.js')).status, 404)
console.log('ok - API path/query/errors and missing assets do not fall back to HTML')

await new Promise((resolve, reject) => {
  const ws = new WebSocket(base.replace(/^http/, 'ws') + '/infra/ws?token=test-log-redaction-token')
  const timeout = setTimeout(() => { ws.close(); reject(new Error('WebSocket proxy timeout')) }, 5000)
  ws.onopen = () => ws.send('proxy-round-trip')
  ws.onerror = () => { clearTimeout(timeout); reject(new Error('WebSocket proxy failed')) }
  ws.onmessage = event => {
    try { assert.equal(event.data, 'proxy-round-trip'); clearTimeout(timeout); ws.close(); resolve() }
    catch (error) { reject(error) }
  }
})
console.log('ok - WebSocket upgrade and bidirectional frames')

const start = performance.now()
const stream = await fetch(base + '/app-api/agent/session/send/stream', { method: 'POST' })
assert.equal(stream.status, 200)
const reader = stream.body.getReader()
const first = await reader.read()
assert.ok(performance.now() - start < 1000, 'SSE first chunk must arrive before the delayed final chunk')
assert.match(new TextDecoder().decode(first.value), /first/)
let remaining = ''
while (true) {
  const part = await reader.read()
  if (part.done) break
  remaining += new TextDecoder().decode(part.value)
}
assert.match(remaining, /second/)
console.log('ok - SSE is streamed without buffering')

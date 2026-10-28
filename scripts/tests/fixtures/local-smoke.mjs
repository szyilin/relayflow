import assert from 'node:assert/strict'
import http from 'node:http'
import { readFile, writeFile } from 'node:fs/promises'
import { randomUUID } from 'node:crypto'

const base = 'http://web'
const mode = process.argv[2]
const stateFile = '/state/smoke.json'
let token
async function api(path, body) {
  const response = await fetch(base + path, {
    method: body === undefined ? 'GET' : 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(15000)
  })
  assert.equal(response.status, 200, `HTTP failure: ${path}`)
  const result = await response.json()
  assert.equal(result.code, 0, `API failure: ${path}, code=${result.code}`)
  return result.data
}
// Connect internally, preserving the original public Host and signed path/query.
// The browser check separately uses the original localhost URL without this transport.
function storage(url, method, headers = {}, body) {
  const signed = new URL(url)
  assert.equal(signed.hostname, 'localhost')
  return new Promise((resolve, reject) => {
    const req = http.request({ hostname: 'minio', port: 9000, path: signed.pathname + signed.search,
      method, headers: { ...headers, Host: signed.host }, timeout: 15000 }, response => {
      const chunks = []
      response.on('data', chunk => chunks.push(chunk))
      response.on('end', () => resolve({ status: response.statusCode, headers: response.headers, body: Buffer.concat(chunks) }))
    })
    req.on('timeout', () => req.destroy(new Error('Storage timeout')))
    req.on('error', () => reject(new Error('Storage transport failed')))
    req.end(body)
  })
}
async function download(fileId, expected) {
  const response = await fetch(`${base}/app-api/infra/file/download?fileId=${fileId}`, {
    headers: { Authorization: `Bearer ${token}` }, redirect: 'manual', signal: AbortSignal.timeout(15000)
  })
  assert.equal(response.status, 302)
  const result = await storage(response.headers.get('location'), 'GET')
  assert.equal(result.status, 200)
  assert.equal(result.body.toString(), expected)
}
try {
  const login = await api('/admin-api/system/auth/login', { username: '19988888888', password: '123456' })
  token = login.accessToken
  const user = await api('/admin-api/system/auth/get-permission-info')
  assert.ok(user.isAdmin)
  console.log('ok - seed login and permissions through web proxy')
  if (mode === 'create') {
    const bytes = Buffer.from(`relayflow-local-smoke-${randomUUID()}`)
    const upload = await api('/app-api/infra/file/upload-session', {
      filename: 'local-smoke.txt', size: bytes.length, mimeType: 'text/plain', accessLevel: 'private'
    })
    const put = await storage(upload.uploadUrl, 'PUT', { ...upload.headers, 'Content-Length': bytes.length }, bytes)
    assert.equal(put.status, 200)
    assert.ok(put.headers.etag)
    const confirmed = await api('/app-api/infra/file/upload-confirm', { uploadId: upload.uploadId, size: bytes.length, etag: put.headers.etag })
    await download(confirmed.fileId, bytes.toString())
    console.log('ok - signed PUT, ETag confirm and private GET bytes')

    const ws = new WebSocket(base.replace('http:', 'ws:') + '/infra/ws?token=' + encodeURIComponent(token))
    const envelopes = []
    ws.onmessage = event => envelopes.push(JSON.parse(event.data))
    const wait = async predicate => {
      const deadline = Date.now() + 10000
      while (!predicate()) {
        if (Date.now() > deadline) throw new Error('WebSocket delivery timeout')
        await new Promise(resolve => setTimeout(resolve, 25))
      }
    }
    try {
      await wait(() => ws.readyState === WebSocket.OPEN)
      const requestId = randomUUID()
      ws.send(JSON.stringify({ domain: 'system', type: 'ping', requestId, ts: Date.now(), payload: {} }))
      await wait(() => envelopes.some(item => item.type === 'pong' && item.requestId === requestId))
      console.log('ok - authenticated WebSocket heartbeat')
      const clientMsgId = randomUUID()
      ws.send(JSON.stringify({ domain: 'im', type: 'message.send', requestId: clientMsgId, payload: {
        peerUserId: user.userId, clientMsgId, type: 'text',
        content: { version: 1, blocks: [{ type: 'text', text: bytes.toString() }] }
      } }))
      await wait(() => envelopes.some(item => item.type === 'message.ack' && item.payload?.clientMsgId === clientMsgId))
      const message = envelopes.find(item => item.type === 'message.ack' && item.requestId === clientMsgId).payload
      await writeFile(stateFile, JSON.stringify({ fileId: confirmed.fileId, bytes: bytes.toString(), conversationId: message.conversationId, clientMsgId }))
      console.log('ok - real WebSocket message.send/ack with persisted message')
    } finally { ws.close() }
  } else if (mode === 'verify') {
    const state = JSON.parse(await readFile(stateFile, 'utf8'))
    await download(state.fileId, state.bytes)
    const messages = await api(`/app-api/im/message/list?conversationId=${state.conversationId}&limit=100`)
    assert.ok(messages.some(item => item.clientMsgId === state.clientMsgId))
    console.log('ok - original file and message survive restart')
  } else throw new Error('Expected create or verify')
} catch (error) {
  // Never include token-bearing request objects or signed URLs in diagnostics.
  console.error(`Smoke failed: ${error.message.replace(/https?:\/\/\S+/g, '[URL]')}`)
  process.exitCode = 1
}

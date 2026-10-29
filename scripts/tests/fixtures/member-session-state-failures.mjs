// Fault injection is restricted to the disposable member-session test containers.
import assert from 'node:assert/strict'
import http from 'node:http'
import { createHmac, randomBytes, randomUUID } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { readFile } from 'node:fs/promises'
const state = JSON.parse(await readFile(process.env.RELAYFLOW_AUTH_TEST_BROWSER_STATE, 'utf8'))
const base = 'http://localhost:18081'
const secret = process.env.RELAYFLOW_AUTH_TEST_JWT_SECRET
assert.ok(secret)
const sockets = []
const docker = (...args) => execFileSync('docker', args, { stdio: 'ignore' })
const wait = async predicate => {
  const end = Date.now() + 12000
  while (!predicate()) {
    if (Date.now() > end) throw new Error('Test condition timed out')
    await new Promise(resolve => setTimeout(resolve, 50))
  }
}
async function request(port, path, token, expected, body) {
  const response = await fetch(`http://localhost:${port}${path}`, { method: body ? 'POST' : 'GET',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined, signal: AbortSignal.timeout(10000) })
  assert.equal(response.status, expected, `${port} ${path}`)
  const data = await response.json()
  if (expected === 200) assert.equal(data.code, 0)
  if (expected === 503) assert.equal(data.code, 2000001005)
  return data.data
}
async function handshake(token, expected) {
  await new Promise((resolve, reject) => {
    const req = http.get(base + '/infra/ws?token=' + encodeURIComponent(token), { headers: {
      Connection: 'Upgrade', Upgrade: 'websocket', 'Sec-WebSocket-Version': '13', 'Sec-WebSocket-Key': randomBytes(16).toString('base64')
    } }, response => {
      response.resume()
      try { assert.equal(response.statusCode, expected); resolve() } catch (error) { reject(error) }
    })
    req.on('upgrade', (_r, socket) => { socket.destroy(); reject(new Error('Unexpected upgrade')) })
    req.on('error', () => reject(new Error('Handshake transport error')))
  })
}
async function connect(token) {
  const ws = new WebSocket(base.replace('http:', 'ws:') + '/infra/ws?token=' + encodeURIComponent(token))
  ws.onerror = () => {}
  sockets.push(ws)
  await wait(() => ws.readyState === WebSocket.OPEN)
  return ws
}
try {
  const login = await request(18081, '/admin-api/system/auth/login', null, 200, { username: state.mobile, password: state.password, tenantId: state.tenant })
  const token = login.accessToken
  await request(18082, '/app-api/system/user/profile', token, 200)
  const payload = { sub: state.uid, tenant_id: 999999, user_type: 'admin', member_ver: 0, jti: randomUUID(), exp: Math.floor(Date.now() / 1000) + 120 }
  const header = Buffer.from(JSON.stringify({ alg: 'HS256' })).toString('base64url')
  const body = Buffer.from(JSON.stringify(payload)).toString('base64url')
  const missingMember = `${header}.${body}.${createHmac('sha256', secret).update(`${header}.${body}`).digest('base64url')}`
  await request(18082, '/app-api/system/user/profile', missingMember, 403)
  console.log('ok - single tenant mode validates real membership and does not default a missing membership')

  for (const container of ['relayflow-member-redis', 'relayflow-member-pg']) {
    const ws = await connect(token)
    docker('stop', '-t', '1', container)
    try {
      await request(18081, '/app-api/system/user/profile', token, 503)
      await handshake(token, 503)
      if (container.endsWith('redis')) {
        await request(18081, '/admin-api/system/auth/logout', token, 503, {})
        await request(18081, '/admin-api/system/auth/logout', 'invalid', 200, {})
      }
      await wait(() => ws.readyState === WebSocket.CLOSED)
      console.log(`ok - ${container.endsWith('redis') ? 'Redis' : 'PostgreSQL'} failure rejects HTTP/WS with 503 and closes existing WS`)
    } finally {
      docker('start', container)
      for (let retry = 0; retry < 40; retry++) {
        try {
          await request(18081, '/app-api/system/user/profile', token, 200)
          break
        } catch (error) {
          if (retry === 39) throw error
          await new Promise(resolve => setTimeout(resolve, 250))
        }
      }
    }
  }
  console.log('passed - authority failure and single tenant acceptance')
} finally {
  for (const ws of sockets) if (ws.readyState === WebSocket.OPEN) ws.close()
}

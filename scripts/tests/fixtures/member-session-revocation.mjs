// Run only against an isolated server/database: see this change's verification.md.
import assert from 'node:assert/strict'
import http from 'node:http'
import { createHmac, randomBytes, randomUUID } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { writeFile } from 'node:fs/promises'

const base = process.env.RELAYFLOW_AUTH_TEST_BASE_URL
const pg = process.env.RELAYFLOW_AUTH_TEST_PG_CONTAINER
const secret = process.env.RELAYFLOW_AUTH_TEST_JWT_SECRET
assert.ok(base?.startsWith('http://localhost:'), 'Explicit localhost test server required')
assert.equal(pg, 'relayflow-member-pg', 'Dedicated PostgreSQL test container required')
assert.ok(secret, 'Explicit test signing secret required')
const sql = query => execFileSync('docker', ['exec', pg, 'psql', '-U', 'relayflow', '-d', 'member_auth_test', '-At', '-c', query], { encoding: 'utf8' }).trim()
const id = value => { assert.match(String(value), /^\d+$/); return String(value) }
const sockets = []
const wait = async predicate => {
  const end = Date.now() + 12000
  while (!predicate()) {
    if (Date.now() >= end) throw new Error('WebSocket condition timed out')
    await new Promise(resolve => setTimeout(resolve, 25))
  }
}
async function api(path, token, body, method = body === undefined ? 'GET' : 'POST', status = 200) {
  const response = await fetch(base + path, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(15000) })
  assert.equal(response.status, status, `HTTP ${path}`)
  const result = await response.json()
  if (status === 200) assert.equal(result.code, 0, `API ${path}: ${result.code}`)
  return result.data
}
const claims = token => JSON.parse(Buffer.from(token.split('.')[1], 'base64url'))
function signed(payload) {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256' })).toString('base64url')
  const body = Buffer.from(JSON.stringify(payload).replace(/\"tenant_id\":\"(\d+)\"/, '\"tenant_id\":$1')).toString('base64url')
  return `${header}.${body}.${createHmac('sha256', secret).update(`${header}.${body}`).digest('base64url')}`
}
async function connect(token) {
  const ws = new WebSocket(base.replace('http:', 'ws:') + '/infra/ws?token=' + encodeURIComponent(token))
  ws.messages = []
  ws.onmessage = event => ws.messages.push(JSON.parse(event.data))
  ws.onerror = () => {}
  sockets.push(ws)
  await wait(() => ws.readyState === WebSocket.OPEN)
  return ws
}
async function ping(ws) {
  const requestId = randomUUID()
  ws.send(JSON.stringify({ domain: 'system', type: 'ping', requestId, payload: {} }))
  await wait(() => ws.messages.some(message => message.type === 'pong' && message.requestId === requestId))
}
async function rejectedHandshake(token, expected) {
  await new Promise((resolve, reject) => {
    const request = http.get(base + '/infra/ws?token=' + encodeURIComponent(token), { headers: {
      Connection: 'Upgrade', Upgrade: 'websocket', 'Sec-WebSocket-Version': '13', 'Sec-WebSocket-Key': randomBytes(16).toString('base64')
    } }, response => {
      try { assert.equal(response.statusCode, expected); response.resume(); resolve() } catch (error) { reject(error) }
    })
    request.on('upgrade', (_response, socket) => { socket.destroy(); reject(new Error('Invalid handshake unexpectedly succeeded')) })
    request.on('error', () => reject(new Error('Handshake transport failed')))
  })
}
try {
  const mobile = '139' + String(Date.now()).slice(-8)
  const password = 'auth-test-only-123'
  const registered = await api('/app-api/system/auth/register', null, { mobile, password, nickname: '会话验收', tenantName: '会话隔离验收' })
  const tenant = id(registered.tenantId)
  const first = registered.accessToken
  const uid = id(claims(first).sub)
  assert.equal(claims(first).member_ver, 0)
  const login = async () => (await api('/admin-api/system/auth/login', null, { username: mobile, password, tenantId: tenant })).accessToken
  const second = await login()
  sql(`INSERT INTO sys_tenant_user (id,tenant_id,user_id,status,auth_version) VALUES (${id(BigInt(uid) + 100n)},1,${uid},'ACTIVE',5)`)
  const otherTenant = (await api('/app-api/system/tenant/switch', second, { tenantId: '1' })).accessToken
  assert.equal(claims(otherTenant).member_ver, 5)
  await api('/app-api/system/user/profile', otherTenant)
  const one = await connect(first)
  const twin = await connect(first)
  const good = await connect(second)
  const other = await connect(otherTenant)
  await Promise.all([ping(one), ping(twin), ping(good), ping(other)])
  console.log('ok - register, login, target tenant version and real WS heartbeats')

  await api('/admin-api/system/auth/logout', first, {})
  await wait(() => one.readyState === WebSocket.CLOSED && twin.readyState === WebSocket.CLOSED)
  await api('/app-api/system/user/profile', first, undefined, 'GET', 401)
  await rejectedHandshake(first, 401)
  await api('/admin-api/system/auth/logout', first, {})
  await api('/admin-api/system/auth/logout', 'invalid', {})
  await Promise.all([ping(good), ping(other)])
  const online = execFileSync('docker', ['exec', 'relayflow-member-redis', 'redis-cli', 'EXISTS', `t:${tenant}:online:${uid}`], { encoding: 'utf8' }).trim()
  assert.equal(online, '1')
  console.log('ok - logout revokes every connection sharing jti; other jti/tenant remain usable; retry is idempotent')

  const legacyClaims = { ...claims(second), tenant_id: tenant, jti: randomUUID() }
  delete legacyClaims.member_ver
  const legacy = signed(legacyClaims)
  await api('/app-api/system/user/profile', legacy)
  const expiring = signed({ ...claims(second), tenant_id: tenant, jti: randomUUID(), exp: Math.floor(Date.now() / 1000) + 2 })
  const expiringWs = await connect(expiring)
  await wait(() => expiringWs.readyState === WebSocket.CLOSED)
  await rejectedHandshake(expiring, 401)
  console.log('ok - legacy version zero transition; expired idle WS is reclaimed')

  const before = sql(`SELECT count(*) FROM im_message WHERE tenant_id=${tenant}`)
  await api('/admin-api/system/user/update-status', second, { id: uid, status: 1 }, 'PUT')
  const forbiddenMessageId = randomUUID()
  if (good.readyState === WebSocket.OPEN) good.send(JSON.stringify({ domain: 'im', type: 'message.send', requestId: forbiddenMessageId,
    payload: { peerUserId: uid, clientMsgId: forbiddenMessageId, type: 'text', content: { version: 1, blocks: [{ type: 'text', text: 'must not persist' }] } } }))
  await api('/app-api/system/user/profile', second, { nickname: 'must-not-save' }, 'PUT', 403)
  await rejectedHandshake(second, 403)
  await wait(() => good.readyState === WebSocket.CLOSED)
  assert.equal(sql(`SELECT count(*) FROM im_message WHERE tenant_id=${tenant}`), before)
  assert.notEqual(sql(`SELECT nickname FROM sys_tenant_user WHERE tenant_id=${tenant} AND user_id=${uid}`), 'must-not-save')
  assert.equal(sql(`SELECT auth_version FROM sys_tenant_user WHERE tenant_id=${tenant} AND user_id=${uid}`), '1')
  assert.equal(execFileSync('docker', ['exec', 'relayflow-member-redis', 'redis-cli', 'EXISTS', `t:${tenant}:online:${uid}`], { encoding: 'utf8' }).trim(), '0')
  assert.equal(execFileSync('docker', ['exec', 'relayflow-member-redis', 'redis-cli', 'EXISTS', `t:1:online:${uid}`], { encoding: 'utf8' }).trim(), '1')
  await ping(other)
  sql(`UPDATE sys_tenant_user SET status='ACTIVE' WHERE tenant_id=${tenant} AND user_id=${uid}`)
  await api('/app-api/system/user/profile', legacy, undefined, 'GET', 401)
  await api('/app-api/system/user/profile', second, undefined, 'GET', 401)
  const fresh = await login()
  assert.equal(claims(fresh).member_ver, 1)
  await api('/app-api/system/user/profile', fresh)
  await ping(await connect(fresh))
  console.log('ok - disable rejects HTTP/WS with no business writes; reenable requires fresh version; other tenant stays valid')

  const state = { mobile, password, tenant, uid }
  // Optional browser fixture has no tokens. It belongs only to the disposable test setup.
  if (process.env.RELAYFLOW_AUTH_TEST_BROWSER_STATE) await writeFile(process.env.RELAYFLOW_AUTH_TEST_BROWSER_STATE, JSON.stringify(state), { mode: 0o600 })
  console.log('passed - real HTTP/WS member session revocation acceptance')
} finally {
  for (const ws of sockets) if (ws.readyState === WebSocket.OPEN) ws.close()
}

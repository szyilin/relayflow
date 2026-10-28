import http from 'node:http'
import { createHash } from 'node:crypto'

const server = http.createServer((req, res) => {
  if (req.url === '/health') return res.end('ready')
  if (req.url === '/app-api/agent/session/send/stream') {
    res.writeHead(200, { 'content-type': 'text/event-stream', 'cache-control': 'no-cache' })
    res.write('data: {"part":"first"}\n\n')
    setTimeout(() => res.end('data: {"part":"second"}\n\n'), 1500)
    return
  }
  res.writeHead(404, { 'content-type': 'application/json' })
  res.end(JSON.stringify({ code: 404, path: req.url }))
})

server.on('upgrade', (req, socket) => {
  if (!req.url.startsWith('/infra/ws')) return socket.destroy()
  const accept = createHash('sha1').update(req.headers['sec-websocket-key'] + '258EAFA5-E914-47DA-95CA-C5AB0DC85B11').digest('base64')
  socket.write('HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: ' + accept + '\r\n\r\n')
  let buffered = Buffer.alloc(0)
  socket.on('data', chunk => {
    buffered = Buffer.concat([buffered, chunk])
    while (buffered.length >= 6) {
      const opcode = buffered[0] & 15
      const length = buffered[1] & 127
      if (length > 125) return socket.destroy()
      if (buffered.length < length + 6) return
      const mask = buffered.subarray(2, 6)
      const payload = Buffer.from(buffered.subarray(6, 6 + length))
      for (let i = 0; i < length; i++) payload[i] ^= mask[i % 4]
      buffered = buffered.subarray(6 + length)
      socket.write(Buffer.concat([Buffer.from([0x80 | opcode, length]), payload]))
      if (opcode === 8) return socket.end()
    }
  })
})

server.listen(8080, '0.0.0.0')

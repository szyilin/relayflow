import { onUnmounted, ref, watch } from 'vue'
import type { MessageItem, RealtimeEnvelope } from '../api/app/im'
import { useAuthStore } from '../stores/auth'

const PING_INTERVAL_MS = 30_000

export interface ImWebSocketHandlers {
  onMessageNew: (message: MessageItem) => void
}

function buildWebSocketUrl(token: string): string {
  const base = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? ''
  if (base) {
    const url = new URL('/infra/ws', base.replace(/^http/i, 'ws'))
    url.searchParams.set('token', token)
    return url.toString()
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/infra/ws?token=${encodeURIComponent(token)}`
}

function isMessageItem(payload: unknown): payload is MessageItem {
  if (!payload || typeof payload !== 'object') {
    return false
  }
  const item = payload as Record<string, unknown>
  return typeof item.id !== 'undefined'
    && typeof item.conversationId !== 'undefined'
    && typeof item.seq === 'number'
}

export function useImWebSocket(handlers: ImWebSocketHandlers) {
  const auth = useAuthStore()
  const connected = ref(false)

  let ws: WebSocket | null = null
  let pingTimer: ReturnType<typeof setInterval> | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let disposed = false

  function clearPingTimer() {
    if (pingTimer) {
      clearInterval(pingTimer)
      pingTimer = null
    }
  }

  function clearReconnectTimer() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  function sendEnvelope(envelope: RealtimeEnvelope) {
    if (ws?.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(envelope))
    }
  }

  function sendPing() {
    sendEnvelope({
      domain: 'system',
      type: 'ping',
      requestId: crypto.randomUUID(),
      ts: Date.now(),
      payload: {}
    })
  }

  function scheduleReconnect() {
    if (disposed || !auth.token) {
      return
    }
    clearReconnectTimer()
    reconnectTimer = setTimeout(() => {
      connect()
    }, 3_000)
  }

  function handleEnvelope(envelope: RealtimeEnvelope) {
    if (envelope.domain === 'im' && envelope.type === 'message.new' && isMessageItem(envelope.payload)) {
      handlers.onMessageNew(envelope.payload)
    }
  }

  function disconnect() {
    disposed = true
    clearPingTimer()
    clearReconnectTimer()
    connected.value = false
    if (ws) {
      ws.onopen = null
      ws.onclose = null
      ws.onerror = null
      ws.onmessage = null
      ws.close()
      ws = null
    }
  }

  function connect() {
    clearReconnectTimer()
    const token = auth.token
    if (!token || disposed) {
      connected.value = false
      return
    }

    if (ws) {
      ws.onopen = null
      ws.onclose = null
      ws.onerror = null
      ws.onmessage = null
      ws.close()
      ws = null
    }

    disposed = false
    const url = buildWebSocketUrl(token)
    ws = new WebSocket(url)

    ws.onopen = () => {
      connected.value = true
      clearPingTimer()
      sendPing()
      pingTimer = setInterval(sendPing, PING_INTERVAL_MS)
    }

    ws.onmessage = (event) => {
      try {
        const envelope = JSON.parse(String(event.data)) as RealtimeEnvelope
        handleEnvelope(envelope)
      } catch {
        // ignore malformed frames
      }
    }

    ws.onclose = () => {
      connected.value = false
      clearPingTimer()
      ws = null
      scheduleReconnect()
    }

    ws.onerror = () => {
      connected.value = false
    }
  }

  watch(() => auth.token, (token) => {
    if (token) {
      disposed = false
      connect()
    } else {
      disconnect()
    }
  }, { immediate: true })

  onUnmounted(disconnect)

  return {
    connected,
    connect,
    disconnect
  }
}

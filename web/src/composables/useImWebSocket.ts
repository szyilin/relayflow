import { onUnmounted, ref } from 'vue'

/**
 * WebSocket 客户端占位：`-web` 阶段不建立真实连接；`-integrate` 接入 `/infra/ws`。
 */
export function useImWebSocket() {
  const connected = ref(false)

  function connect() {
    connected.value = false
  }

  function disconnect() {
    connected.value = false
  }

  onUnmounted(disconnect)

  return {
    connected,
    connect,
    disconnect
  }
}

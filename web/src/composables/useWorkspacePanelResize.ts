import { onBeforeUnmount, ref } from 'vue'

const STORAGE_KEY = 'relayflow:ws:panel-width'

const DEFAULT_WIDTH = 320
const MIN_WIDTH = 240
const MAX_WIDTH = 480

function readStoredWidth(): number {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return DEFAULT_WIDTH
  }

  const parsed = Number.parseInt(raw, 10)
  if (Number.isNaN(parsed)) {
    return DEFAULT_WIDTH
  }

  return Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, parsed))
}

export function useWorkspacePanelResize() {
  const panelWidth = ref(readStoredWidth())
  const isResizing = ref(false)

  let startX = 0
  let startWidth = 0

  function persistWidth(width: number) {
    localStorage.setItem(STORAGE_KEY, String(width))
  }

  function onPointerMove(event: PointerEvent) {
    const delta = event.clientX - startX
    const next = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, startWidth + delta))
    panelWidth.value = next
  }

  function stopResize() {
    isResizing.value = false
    document.body.classList.remove('workspace-resizing')
    window.removeEventListener('pointermove', onPointerMove)
    window.removeEventListener('pointerup', stopResize)
    persistWidth(panelWidth.value)
  }

  function startResize(event: PointerEvent) {
    event.preventDefault()
    isResizing.value = true
    startX = event.clientX
    startWidth = panelWidth.value
    document.body.classList.add('workspace-resizing')
    window.addEventListener('pointermove', onPointerMove)
    window.addEventListener('pointerup', stopResize)
  }

  onBeforeUnmount(stopResize)

  return {
    panelWidth,
    isResizing,
    startResize
  }
}

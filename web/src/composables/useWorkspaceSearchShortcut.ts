import { onMounted, onUnmounted, type Ref } from 'vue'

function isEditableTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) {
    return false
  }
  const tag = target.tagName
  return tag === 'INPUT' || tag === 'TEXTAREA' || target.isContentEditable
}

/**
 * 工作台全局搜索快捷键：⌘K / Ctrl+K
 */
export function useWorkspaceSearchShortcut(open: Ref<boolean>) {
  function onKeydown(event: KeyboardEvent) {
    if (event.key.toLowerCase() !== 'k') {
      return
    }
    if (!(event.metaKey || event.ctrlKey)) {
      return
    }
    if (isEditableTarget(event.target)) {
      return
    }
    event.preventDefault()
    open.value = true
  }

  onMounted(() => {
    window.addEventListener('keydown', onKeydown)
  })

  onUnmounted(() => {
    window.removeEventListener('keydown', onKeydown)
  })
}

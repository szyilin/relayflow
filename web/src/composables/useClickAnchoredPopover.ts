import { ref, shallowRef } from 'vue'

const CARD_WIDTH = 256
const CARD_HEIGHT = 280
const OFFSET = 12
const VIEWPORT_PADDING = 16

type PopoverSide = 'top' | 'bottom' | 'left' | 'right'
type PopoverAlign = 'start' | 'center' | 'end'

export interface ClickAnchoredPopoverContent {
  side: PopoverSide
  align: PopoverAlign
  sideOffset: number
  collisionPadding: number
}

export function createClickReference(clientX: number, clientY: number) {
  return {
    getBoundingClientRect: () => ({
      width: 0,
      height: 0,
      top: clientY,
      right: clientX,
      bottom: clientY,
      left: clientX,
      x: clientX,
      y: clientY
    })
  }
}

export function resolvePopoverPlacement(clientX: number, clientY: number): Pick<ClickAnchoredPopoverContent, 'side' | 'align'> {
  const vw = window.innerWidth
  const vh = window.innerHeight

  const spaceBelow = vh - clientY - VIEWPORT_PADDING
  const spaceAbove = clientY - VIEWPORT_PADDING
  const spaceRight = vw - clientX - VIEWPORT_PADDING
  const spaceLeft = clientX - VIEWPORT_PADDING

  if (spaceBelow >= CARD_HEIGHT && spaceRight >= CARD_WIDTH * 0.55) {
    return { side: 'bottom', align: 'start' }
  }
  if (spaceAbove >= CARD_HEIGHT && spaceRight >= CARD_WIDTH * 0.55) {
    return { side: 'top', align: 'start' }
  }
  if (spaceRight >= CARD_WIDTH) {
    return { side: 'right', align: 'start' }
  }
  if (spaceLeft >= CARD_WIDTH) {
    return { side: 'left', align: 'end' }
  }
  if (spaceBelow >= CARD_HEIGHT) {
    return { side: 'bottom', align: 'center' }
  }
  if (spaceAbove >= CARD_HEIGHT) {
    return { side: 'top', align: 'center' }
  }
  return { side: 'bottom', align: 'center' }
}

export function useClickAnchoredPopover<T>() {
  const open = ref(false)
  const payload = shallowRef<T>()
  const reference = shallowRef<ReturnType<typeof createClickReference>>()
  const content = ref<ClickAnchoredPopoverContent>({
    side: 'bottom',
    align: 'start',
    sideOffset: OFFSET,
    collisionPadding: VIEWPORT_PADDING
  })

  function show(item: T, event: MouseEvent) {
    const { clientX, clientY } = event
    reference.value = createClickReference(clientX, clientY)
    content.value = {
      ...resolvePopoverPlacement(clientX, clientY),
      sideOffset: OFFSET,
      collisionPadding: VIEWPORT_PADDING
    }
    payload.value = item
    open.value = true
  }

  function close() {
    open.value = false
  }

  return {
    open,
    payload,
    reference,
    content,
    show,
    close
  }
}

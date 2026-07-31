<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import type { MessageReactionAgg } from '../../api/app/im'

const QUICK_EMOJIS = ['👍', '❤️', '😄', '🎉', '👀', '🙏'] as const
const VIEWPORT_PAD = 8
const GAP = 4

const props = defineProps<{
  reactions?: MessageReactionAgg[]
  disabled?: boolean
  forceOpen?: boolean
  /** Align add-button / chips to the end (own messages) */
  alignEnd?: boolean
  /** When force-open from context menu, position near this point */
  anchor?: { x: number, y: number } | null
}>()

const emit = defineEmits<{
  toggle: [emoji: string]
  'update:forceOpen': [value: boolean]
}>()

const moreOpen = ref(false)
const panelReady = ref(false)
const triggerRef = ref<HTMLElement | null>(null)
const panelRef = ref<HTMLElement | null>(null)
const rootRef = ref<HTMLElement | null>(null)
const panelStyle = ref<Record<string, string>>({
  position: 'fixed',
  top: '0px',
  left: '0px',
  zIndex: '85'
})
/** Ignore outside pointerdown briefly after opening from a menu click. */
let suppressOutsideCloseUntil = 0

const chips = computed(() => props.reactions ?? [])
const hasChips = computed(() => chips.value.length > 0)
const showTrigger = computed(() => !props.disabled)

watch(() => props.forceOpen, (open) => {
  if (open) {
    suppressOutsideCloseUntil = Date.now() + 400
    moreOpen.value = true
  }
})

watch(moreOpen, async (open) => {
  if (!open) {
    panelReady.value = false
    return
  }
  panelReady.value = false
  await nextTick()
  // Teleport + layout: wait a frame so panelRef and measurements are ready
  await new Promise<void>((resolve) => {
    requestAnimationFrame(() => resolve())
  })
  if (!moreOpen.value) {
    return
  }
  const ok = positionPanel()
  if (!ok) {
    // One more try after another frame (slow Teleport / font layout)
    await new Promise<void>((resolve) => {
      requestAnimationFrame(() => resolve())
    })
    if (!moreOpen.value) {
      return
    }
    positionPanel()
  }
  if (moreOpen.value) {
    panelReady.value = true
  }
})

watch(() => props.anchor, () => {
  if (moreOpen.value) {
    positionPanel()
  }
})

function resolveAnchorPoint(): { x: number, y: number } {
  const vw = window.innerWidth
  const vh = window.innerHeight

  if (props.anchor && Number.isFinite(props.anchor.x) && Number.isFinite(props.anchor.y)) {
    return { x: props.anchor.x, y: props.anchor.y }
  }

  const trigger = triggerRef.value
  if (trigger) {
    const tr = trigger.getBoundingClientRect()
    // Reject collapsed / escaped containing-block rects (e.g. pinned to shell corner)
    if (tr.width > 0 && tr.height > 0 && tr.bottom > 0 && tr.top < vh && tr.right > 0 && tr.left < vw) {
      return { x: tr.left + tr.width / 2, y: tr.top }
    }
  }

  const root = rootRef.value
  if (root) {
    const rr = root.getBoundingClientRect()
    if (rr.width > 0 || rr.height > 0) {
      return { x: rr.left + Math.max(rr.width, 1) / 2, y: rr.top }
    }
    const parent = root.parentElement
    if (parent) {
      const pr = parent.getBoundingClientRect()
      if (pr.width > 0 || pr.height > 0) {
        return { x: pr.left + pr.width / 2, y: pr.top }
      }
    }
  }

  return { x: vw / 2, y: vh / 2 }
}

function positionPanel(): boolean {
  const panel = panelRef.value
  if (!panel) {
    return false
  }

  const pw = panel.offsetWidth || 220
  const ph = panel.offsetHeight || 40
  const vw = window.innerWidth
  const vh = window.innerHeight
  const { x: anchorX, y: anchorY } = resolveAnchorPoint()

  let top = anchorY - ph - GAP
  if (top < VIEWPORT_PAD) {
    top = anchorY + GAP
  }
  if (top + ph > vh - VIEWPORT_PAD) {
    top = Math.max(VIEWPORT_PAD, vh - ph - VIEWPORT_PAD)
  }

  let left = anchorX - pw / 2
  if (left < VIEWPORT_PAD) {
    left = VIEWPORT_PAD
  }
  if (left + pw > vw - VIEWPORT_PAD) {
    left = Math.max(VIEWPORT_PAD, vw - pw - VIEWPORT_PAD)
  }

  panelStyle.value = {
    position: 'fixed',
    top: `${Math.round(top)}px`,
    left: `${Math.round(left)}px`,
    zIndex: '85'
  }
  return true
}

function onChip(emoji: string) {
  if (props.disabled) {
    return
  }
  emit('toggle', emoji)
}

function onQuick(emoji: string) {
  if (props.disabled) {
    return
  }
  moreOpen.value = false
  emit('update:forceOpen', false)
  emit('toggle', emoji)
}

function toggleMore() {
  moreOpen.value = !moreOpen.value
  if (!moreOpen.value) {
    emit('update:forceOpen', false)
  } else {
    suppressOutsideCloseUntil = Date.now() + 400
  }
}

function closeMore() {
  if (!moreOpen.value) {
    return
  }
  moreOpen.value = false
  emit('update:forceOpen', false)
}

function onPointerDown(event: PointerEvent) {
  if (!moreOpen.value) {
    return
  }
  if (Date.now() < suppressOutsideCloseUntil) {
    return
  }
  const target = event.target as Node | null
  if (triggerRef.value?.contains(target) || panelRef.value?.contains(target)) {
    return
  }
  closeMore()
}

function onKey(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeMore()
  }
}

function onReposition() {
  if (moreOpen.value && panelReady.value) {
    positionPanel()
  }
}

onMounted(() => {
  window.addEventListener('pointerdown', onPointerDown, true)
  window.addEventListener('keydown', onKey)
  window.addEventListener('resize', onReposition)
  window.addEventListener('scroll', onReposition, true)
})

onUnmounted(() => {
  window.removeEventListener('pointerdown', onPointerDown, true)
  window.removeEventListener('keydown', onKey)
  window.removeEventListener('resize', onReposition)
  window.removeEventListener('scroll', onReposition, true)
})
</script>

<template>
  <!--
    Single relative root so the absolute trigger is always positioned
    against the message column, not a higher shell ancestor.
  -->
  <div
    ref="rootRef"
    class="relative"
  >
    <!-- Existing reaction chips: only take layout space when present -->
    <div
      v-if="hasChips"
      class="mt-1 flex flex-wrap items-center gap-1 px-1"
      :class="alignEnd ? 'justify-end' : 'justify-start'"
    >
      <button
        v-for="item in chips"
        :key="item.emoji"
        type="button"
        class="inline-flex items-center gap-0.5 rounded-full border px-1.5 py-0.5 text-xs transition-colors"
        :class="item.me
          ? 'border-primary/40 bg-primary/10 text-[var(--ws-text)]'
          : 'border-[var(--ws-border-subtle)] bg-[var(--ws-input-bar-bg)] text-[var(--ws-text)] hover:bg-black/5'"
        :disabled="disabled"
        @click.stop="onChip(item.emoji)"
      >
        <span>{{ item.emoji }}</span>
        <span class="tabular-nums text-[10px] opacity-70">{{ item.count }}</span>
      </button>
    </div>

    <!--
      Add trigger: out of flow, pinned beside the message meta/time row.
      Hidden until hover / open so it never reserves a blank row.
    -->
    <button
      v-if="showTrigger"
      ref="triggerRef"
      type="button"
      class="absolute bottom-0 z-[5] inline-flex size-5 items-center justify-center rounded-full border border-dashed border-[var(--ws-border-subtle)] bg-[var(--ui-bg)] text-[var(--ws-text-muted)] shadow-sm transition-opacity"
      :class="[
        alignEnd ? 'right-0 translate-x-full pl-1' : 'left-0 -translate-x-full pr-1',
        moreOpen || forceOpen
          ? 'opacity-100'
          : 'pointer-events-none opacity-0 group-hover:pointer-events-auto group-hover:opacity-100'
      ]"
      title="表情回应"
      aria-label="表情回应"
      @click.stop="toggleMore"
    >
      <UIcon
        name="i-lucide-smile-plus"
        class="size-3"
      />
    </button>

    <Teleport to="body">
      <div
        v-if="moreOpen"
        ref="panelRef"
        class="flex gap-0.5 rounded-md border border-[var(--ws-border-subtle)] bg-[var(--ui-bg)] p-1 shadow-md"
        :style="{ ...panelStyle, visibility: panelReady ? 'visible' : 'hidden' }"
        @click.stop
      >
        <button
          v-for="emoji in QUICK_EMOJIS"
          :key="emoji"
          type="button"
          class="size-7 rounded text-base hover:bg-[var(--ws-input-bar-bg)]"
          @click="onQuick(emoji)"
        >
          {{ emoji }}
        </button>
      </div>
    </Teleport>
  </div>
</template>

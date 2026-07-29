<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import type { MessageReactionAgg } from '../../api/app/im'

const QUICK_EMOJIS = ['👍', '❤️', '😄', '🎉', '👀', '🙏'] as const
const VIEWPORT_PAD = 8
const GAP = 4

const props = defineProps<{
  reactions?: MessageReactionAgg[]
  disabled?: boolean
  forceOpen?: boolean
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
const panelStyle = ref<Record<string, string>>({})

watch(() => props.forceOpen, (open) => {
  if (open) {
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
  await nextTick()
  positionPanel()
  panelReady.value = true
})

function positionPanel() {
  const panel = panelRef.value
  if (!panel) {
    return
  }

  const pw = panel.offsetWidth || 220
  const ph = panel.offsetHeight || 40
  const vw = window.innerWidth
  const vh = window.innerHeight

  let anchorX: number
  let anchorY: number
  let preferAbove = true

  if (props.forceOpen && props.anchor) {
    anchorX = props.anchor.x
    anchorY = props.anchor.y
  } else if (triggerRef.value) {
    const tr = triggerRef.value.getBoundingClientRect()
    anchorX = tr.left + tr.width / 2
    anchorY = tr.top
    preferAbove = true
  } else {
    anchorX = vw / 2
    anchorY = vh / 2
  }

  let top = preferAbove ? anchorY - ph - GAP : anchorY + GAP
  if (top < VIEWPORT_PAD) {
    top = anchorY + GAP
  }
  if (top + ph > vh - VIEWPORT_PAD) {
    top = Math.max(VIEWPORT_PAD, vh - ph - VIEWPORT_PAD)
  }

  // Center horizontally on anchor, then clamp
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
  if (moreOpen.value) {
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
  <div class="mt-1 flex flex-wrap items-center gap-1">
    <button
      v-for="item in (reactions ?? [])"
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

    <div
      v-if="!disabled"
      class="relative"
    >
      <button
        ref="triggerRef"
        type="button"
        class="inline-flex size-6 items-center justify-center rounded-full border border-dashed border-[var(--ws-border-subtle)] text-[var(--ws-text-muted)] transition-opacity"
        :class="moreOpen || forceOpen ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'"
        title="表情回应"
        aria-label="表情回应"
        @click.stop="toggleMore"
      >
        <UIcon
          name="i-lucide-smile-plus"
          class="size-3.5"
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
  </div>
</template>

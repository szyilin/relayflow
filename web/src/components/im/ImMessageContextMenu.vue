<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'

export type ImMessageMenuAction = {
  label: string
  icon?: string
  onSelect: (point?: { x: number, y: number }) => void
}

const props = defineProps<{
  open: boolean
  x: number
  y: number
  items: ImMessageMenuAction[]
}>()

const emit = defineEmits<{
  close: []
}>()

const menuRef = ref<HTMLElement | null>(null)
const pos = ref({ left: 0, top: 0 })

const visible = computed(() => props.open && props.items.length > 0)

function clampPosition() {
  const el = menuRef.value
  const pad = 8
  const width = el?.offsetWidth ?? 180
  const height = el?.offsetHeight ?? 160
  const maxLeft = window.innerWidth - width - pad
  const maxTop = window.innerHeight - height - pad
  pos.value = {
    left: Math.max(pad, Math.min(props.x, maxLeft)),
    top: Math.max(pad, Math.min(props.y, maxTop))
  }
}

watch(() => [props.open, props.x, props.y, props.items] as const, async () => {
  if (!props.open) {
    return
  }
  pos.value = { left: props.x, top: props.y }
  await nextTick()
  clampPosition()
})

function onKey(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    emit('close')
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKey)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKey)
})

function run(action: ImMessageMenuAction, event: MouseEvent) {
  action.onSelect({ x: event.clientX, y: event.clientY })
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="fixed inset-0 z-[80]"
      @click="emit('close')"
      @contextmenu.prevent="emit('close')"
    >
      <div
        ref="menuRef"
        class="fixed min-w-40 select-none overflow-hidden rounded-md border border-[var(--ws-border-subtle)] bg-[var(--ui-bg)] py-1 shadow-lg"
        :style="{ left: `${pos.left}px`, top: `${pos.top}px` }"
        role="menu"
        @click.stop
      >
        <button
          v-for="(action, index) in items"
          :key="`${action.label}-${index}`"
          type="button"
          class="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm text-[var(--ws-text)] hover:bg-[var(--ws-input-bar-bg)]"
          role="menuitem"
          @click="run(action, $event)"
        >
          <UIcon
            v-if="action.icon"
            :name="action.icon"
            class="size-4 shrink-0 text-[var(--ws-text-muted)]"
          />
          <span>{{ action.label }}</span>
        </button>
      </div>
    </div>
  </Teleport>
</template>

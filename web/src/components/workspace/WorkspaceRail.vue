<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkspaceNav } from '../../composables/useWorkspaceNav'
import { moveRailIdBefore } from '../../composables/workspaceRailOrder'
import { useUserPreferenceStore } from '../../stores/userPreference'
import WorkspaceRailHeader from './WorkspaceRailHeader.vue'

const router = useRouter()
const toast = useToast()
const preferenceStore = useUserPreferenceStore()
const { railItems, activeRailId } = useWorkspaceNav()

const draggingId = ref<string | null>(null)
/** Insert before this id when dropMode === 'before'. */
const overBeforeId = ref<string | null>(null)
/** 'before' = line above overBeforeId; 'end' = line after last item. */
const dropMode = ref<'before' | 'end' | null>(null)
const suppressNextClick = ref(false)

function onDragStart(event: DragEvent, id: string) {
  draggingId.value = id
  overBeforeId.value = null
  dropMode.value = null
  suppressNextClick.value = false
  event.dataTransfer?.setData('text/plain', id)
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

function onDragEnd() {
  draggingId.value = null
  overBeforeId.value = null
  dropMode.value = null
}

function onItemDragOver(event: DragEvent, itemId: string, index: number) {
  if (!draggingId.value || draggingId.value === itemId) {
    return
  }
  event.preventDefault()
  event.stopPropagation()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  const el = event.currentTarget as HTMLElement | null
  const rect = el?.getBoundingClientRect()
  const before = !rect || event.clientY < rect.top + rect.height / 2
  if (before) {
    dropMode.value = 'before'
    overBeforeId.value = itemId
    return
  }
  const next = railItems.value[index + 1]
  if (next) {
    dropMode.value = 'before'
    overBeforeId.value = next.id
  } else {
    dropMode.value = 'end'
    overBeforeId.value = null
  }
}

function onNavDragOver(event: DragEvent) {
  if (!draggingId.value) {
    return
  }
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  dropMode.value = 'end'
  overBeforeId.value = null
}

async function onDrop(event: DragEvent) {
  event.preventDefault()
  const fromId = event.dataTransfer?.getData('text/plain') || draggingId.value
  const mode = dropMode.value
  const beforeId = mode === 'end' ? null : overBeforeId.value
  onDragEnd()
  if (!fromId || mode == null) {
    return
  }

  const current = railItems.value.map(item => item.id)
  const next = moveRailIdBefore(current, fromId, beforeId)
  if (next.join(',') === current.join(',')) {
    return
  }

  suppressNextClick.value = true
  const ok = await preferenceStore.setRailOrder(next)
  if (!ok) {
    toast.add({
      title: '侧栏排序保存失败',
      description: preferenceStore.lastSyncError ?? '请稍后重试',
      color: 'error'
    })
  }
}

function onItemClick(event: MouseEvent, to: string) {
  if (suppressNextClick.value) {
    event.preventDefault()
    suppressNextClick.value = false
    return
  }
  event.preventDefault()
  void router.push(to)
}
</script>

<template>
  <aside class="workspace-rail workspace-card flex h-full shrink-0 flex-col overflow-hidden">
    <WorkspaceRailHeader />

    <nav
      class="flex-1 space-y-0.5 overflow-y-auto p-2"
      @dragover="onNavDragOver"
      @drop="onDrop"
    >
      <a
        v-for="(item, index) in railItems"
        :key="item.id"
        :href="item.to"
        class="workspace-rail-btn relative flex cursor-grab items-center gap-3 rounded-xl px-3 py-2.5 active:cursor-grabbing"
        :class="[
          draggingId === item.id ? 'opacity-50' : '',
          dropMode === 'before' && overBeforeId === item.id ? 'workspace-rail-drop-before' : ''
        ]"
        :style="item.tintVar ? { '--ws-module-tint': `var(${item.tintVar})` } : undefined"
        :data-active="activeRailId === item.id"
        :draggable="true"
        @dragstart="onDragStart($event, item.id)"
        @dragend="onDragEnd"
        @dragover="onItemDragOver($event, item.id, index)"
        @drop="onDrop"
        @click="onItemClick($event, item.to)"
      >
        <UIcon :name="item.icon" class="workspace-rail-icon size-5 shrink-0" />
        <span class="truncate text-sm font-medium">{{ item.label }}</span>
        <span
          v-if="item.badge"
          class="ml-auto flex size-5 shrink-0 items-center justify-center rounded-full bg-error text-[10px] font-medium text-white"
        >
          {{ item.badge }}
        </span>
      </a>
      <div
        v-if="draggingId && dropMode === 'end'"
        class="workspace-rail-drop-end pointer-events-none"
        aria-hidden="true"
      />
    </nav>
  </aside>
</template>

<style scoped>
.workspace-rail-drop-before::before {
  content: '';
  position: absolute;
  left: 0.5rem;
  right: 0.5rem;
  top: -1px;
  height: 2px;
  border-radius: 1px;
  background: var(--ui-primary);
  pointer-events: none;
}

.workspace-rail-drop-end {
  height: 2px;
  margin: 2px 0.5rem 0;
  border-radius: 1px;
  background: var(--ui-primary);
}
</style>

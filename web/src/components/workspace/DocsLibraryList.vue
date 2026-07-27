<script setup lang="ts">
import { computed, ref } from 'vue'
import type { DocsLibraryNode } from '../../api/app/docs'
import WorkspaceOwnerChip from './WorkspaceOwnerChip.vue'
import { useAuthStore } from '../../stores/auth'

const props = defineProps<{
  nodes: DocsLibraryNode[]
  activeObjectId?: string | null
  actionsFor: (node: DocsLibraryNode) => unknown
  /** visit lookup: objectId → lastOpenedAt ISO */
  lastOpenedByObjectId?: Record<string, string | undefined>
}>()

const emit = defineEmits<{
  open: [objectId: string]
}>()

const auth = useAuthStore()
const collapsed = ref<Set<string>>(new Set())

const MAX_INDENT_LEVEL = 6
const INDENT_PX = 16

type FlatRow = {
  node: DocsLibraryNode
  depth: number
}

const flatRows = computed(() => {
  const rows: FlatRow[] = []
  function walk(nodes: DocsLibraryNode[], depth: number) {
    for (const node of nodes) {
      rows.push({ node, depth })
      if (node.children.length && !collapsed.value.has(node.nodeId)) {
        walk(node.children, depth + 1)
      }
    }
  }
  walk(props.nodes, 0)
  return rows
})

function toggle(nodeId: string) {
  const next = new Set(collapsed.value)
  if (next.has(nodeId)) {
    next.delete(nodeId)
  } else {
    next.add(nodeId)
  }
  collapsed.value = next
}

function formatTime(value?: string | null) {
  if (!value) {
    return '—'
  }
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) {
    return '—'
  }
  return d.toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function indentStyle(depth: number) {
  const capped = Math.min(depth, MAX_INDENT_LEVEL)
  return { paddingLeft: `${capped * INDENT_PX}px` }
}
</script>

<template>
  <div class="flex h-full min-h-0 flex-col">
    <div class="flex items-center gap-2 border-b border-[var(--ws-border-subtle)] px-5 py-3">
      <UIcon
        name="i-lucide-book-open"
        class="size-4 shrink-0 text-[var(--ws-text-muted)]"
      />
      <h2 class="text-base font-semibold">
        我的文档库
      </h2>
    </div>

    <div class="min-h-0 flex-1 overflow-auto">
      <p
        v-if="!flatRows.length"
        class="px-5 py-12 text-center text-sm text-[var(--ws-text-muted)]"
      >
        还没有文档，点击上方「新建」开始。
      </p>

      <template v-else>
        <div
          class="sticky top-0 z-[1] hidden grid-cols-[minmax(0,1.6fr)_minmax(7rem,0.7fr)_minmax(5.5rem,0.55fr)_minmax(5.5rem,0.55fr)_2.5rem] gap-2 border-b border-[var(--ws-border-subtle)] bg-[var(--ws-bg)] px-5 py-2 text-[11px] font-medium text-[var(--ws-text-muted)] md:grid"
        >
          <span>标题</span>
          <span>所有者</span>
          <span>创建时间</span>
          <span>最近访问</span>
          <span />
        </div>

        <div
          v-for="{ node, depth } in flatRows"
          :key="node.nodeId"
          class="group grid grid-cols-[minmax(0,1fr)_auto] items-center gap-2 border-b border-[var(--ws-border-subtle)] px-3 py-2 hover:bg-[var(--ws-list-hover)] md:grid-cols-[minmax(0,1.6fr)_minmax(7rem,0.7fr)_minmax(5.5rem,0.55fr)_minmax(5.5rem,0.55fr)_2.5rem] md:px-5"
          :data-active="activeObjectId === node.objectId ? 'true' : undefined"
        >
          <button
            type="button"
            class="flex min-w-0 items-center gap-1 text-left"
            :style="indentStyle(depth)"
            @click="emit('open', node.objectId)"
          >
            <button
              v-if="node.children.length"
              type="button"
              class="flex size-6 shrink-0 items-center justify-center rounded text-[var(--ws-text-muted)] hover:bg-[var(--ws-bg-muted)]"
              @click.stop="toggle(node.nodeId)"
            >
              <UIcon
                :name="collapsed.has(node.nodeId) ? 'i-lucide-chevron-right' : 'i-lucide-chevron-down'"
                class="size-3.5"
              />
            </button>
            <span
              v-else
              class="size-6 shrink-0"
            />
            <UIcon
              name="i-lucide-file-text"
              class="size-4 shrink-0 text-[var(--ws-text-muted)]"
            />
            <span class="min-w-0 truncate text-sm font-medium">{{ node.title || '未命名文档' }}</span>
            <span
              v-if="depth > MAX_INDENT_LEVEL"
              class="shrink-0 text-[10px] text-[var(--ws-text-muted)]"
            >…</span>
          </button>

          <div
            class="hidden min-w-0 md:block"
            @click.stop
          >
            <WorkspaceOwnerChip
              v-if="auth.userId"
              :user-id="auth.userId"
            />
            <span
              v-else
              class="text-xs text-[var(--ws-text-muted)]"
            >—</span>
          </div>

          <span class="hidden truncate text-xs text-[var(--ws-text-muted)] md:block">
            {{ formatTime(node.createTime) }}
          </span>
          <span class="hidden truncate text-xs text-[var(--ws-text-muted)] md:block">
            {{ formatTime(node.lastOpenedAt ?? lastOpenedByObjectId?.[node.objectId]) }}
          </span>

          <div
            class="justify-self-end"
            @click.stop
          >
            <UDropdownMenu :items="actionsFor(node) as never">
              <UButton
                color="neutral"
                variant="ghost"
                size="xs"
                icon="i-lucide-ellipsis"
                square
                class="opacity-100 md:opacity-0 md:group-hover:opacity-100"
              />
            </UDropdownMenu>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

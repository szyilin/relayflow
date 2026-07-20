<script setup lang="ts">
import { ref } from 'vue'
import type { DocsLibraryNode } from '../../api/app/docs'

defineProps<{
  nodes: DocsLibraryNode[]
  activeObjectId: string | null
  actionsFor: (node: DocsLibraryNode) => unknown
}>()

const emit = defineEmits<{
  open: [objectId: string]
}>()

const expanded = ref<Set<string>>(new Set())

function toggle(nodeId: string) {
  const next = new Set(expanded.value)
  if (next.has(nodeId)) {
    next.delete(nodeId)
  } else {
    next.add(nodeId)
  }
  expanded.value = next
}
</script>

<template>
  <ul class="space-y-0.5">
    <li
      v-for="node in nodes"
      :key="node.nodeId"
    >
      <div class="group flex items-center gap-0.5">
        <button
          v-if="node.children.length"
          type="button"
          class="flex size-6 shrink-0 items-center justify-center rounded text-[var(--ws-text-muted)] hover:bg-[var(--ws-list-hover)]"
          @click="toggle(node.nodeId)"
        >
          <UIcon
            :name="expanded.has(node.nodeId) ? 'i-lucide-chevron-down' : 'i-lucide-chevron-right'"
            class="size-3.5"
          />
        </button>
        <span
          v-else
          class="size-6 shrink-0"
        />
        <button
          type="button"
          class="workspace-list-item min-w-0 flex-1 truncate px-2 py-1.5 text-left text-sm"
          :data-active="activeObjectId === node.objectId"
          @click="emit('open', node.objectId)"
        >
          {{ node.title }}
        </button>
        <UDropdownMenu :items="actionsFor(node) as never">
          <UButton
            size="xs"
            color="neutral"
            variant="ghost"
            icon="i-lucide-ellipsis"
            class="opacity-0 group-hover:opacity-100"
          />
        </UDropdownMenu>
      </div>
      <div
        v-if="node.children.length && expanded.has(node.nodeId)"
        class="ml-3 border-l border-[var(--ws-border-subtle)] pl-1"
      >
        <DocsTreeNodes
          :nodes="node.children"
          :active-object-id="activeObjectId"
          :actions-for="actionsFor"
          @open="emit('open', $event)"
        />
      </div>
    </li>
  </ul>
</template>

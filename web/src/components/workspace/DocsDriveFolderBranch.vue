<script setup lang="ts">
import type { DriveFolderTreeNode } from './docsDriveFolderTreeTypes'

defineProps<{
  nodes: DriveFolderTreeNode[]
  expanded: Set<string>
  selectedFolderId: string | null
  depth: number
}>()

const emit = defineEmits<{
  toggle: [node: DriveFolderTreeNode]
  select: [folderId: string, name: string]
}>()
</script>

<template>
  <ul class="space-y-0.5">
    <li
      v-for="node in nodes"
      :key="node.folderId"
    >
      <div
        class="group flex items-center gap-0.5"
        :style="{ paddingLeft: `${Math.min(depth, 6) * 12}px` }"
      >
        <button
          type="button"
          class="flex size-6 shrink-0 items-center justify-center rounded text-[var(--ws-text-muted)] hover:bg-[var(--ws-list-hover)]"
          @click.stop="emit('toggle', node)"
        >
          <UIcon
            :name="expanded.has(node.folderId) ? 'i-lucide-chevron-down' : 'i-lucide-chevron-right'"
            class="size-3.5"
          />
        </button>
        <button
          type="button"
          class="workspace-list-item flex min-w-0 flex-1 items-center gap-2 px-2 py-1.5 text-left"
          :data-active="selectedFolderId === node.folderId ? 'true' : undefined"
          @click="emit('select', node.folderId, node.name)"
        >
          <UIcon
            name="i-lucide-folder"
            class="size-3.5 shrink-0 text-amber-500"
          />
          <span class="min-w-0 truncate">{{ node.name }}</span>
        </button>
      </div>
      <DocsDriveFolderBranch
        v-if="expanded.has(node.folderId) && node.children.length"
        :nodes="node.children"
        :expanded="expanded"
        :selected-folder-id="selectedFolderId"
        :depth="depth + 1"
        @toggle="emit('toggle', $event)"
        @select="(id, name) => emit('select', id, name)"
      />
    </li>
  </ul>
</template>

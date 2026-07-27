<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { listDriveFolders, type DocsDriveFolder } from '../../api/app/docs-drive'
import DocsDriveFolderBranch from './DocsDriveFolderBranch.vue'
import type { DriveFolderTreeNode } from './docsDriveFolderTreeTypes'

const props = defineProps<{
  selectedFolderId: string | null
}>()

const emit = defineEmits<{
  select: [folderId: string | null, name?: string]
  createFolder: []
  back: []
}>()

const rootChildren = ref<DriveFolderTreeNode[]>([])
const expanded = ref<Set<string>>(new Set())
const loading = ref(false)

function toNode(folder: DocsDriveFolder): DriveFolderTreeNode {
  return { ...folder, children: [], loaded: false }
}

async function loadChildren(parentId: string | null): Promise<DriveFolderTreeNode[]> {
  const resp = await listDriveFolders(parentId)
  return (resp.folders ?? []).map(toNode)
}

async function ensureRoot() {
  loading.value = true
  try {
    rootChildren.value = await loadChildren(null)
  } finally {
    loading.value = false
  }
}

async function onToggle(node: DriveFolderTreeNode) {
  const next = new Set(expanded.value)
  if (next.has(node.folderId)) {
    next.delete(node.folderId)
    expanded.value = next
    return
  }
  if (!node.loaded) {
    node.children = await loadChildren(node.folderId)
    node.loaded = true
  }
  next.add(node.folderId)
  expanded.value = next
}

onMounted(() => {
  void ensureRoot()
})

watch(
  () => props.selectedFolderId,
  async (id) => {
    if (id == null) {
      return
    }
    if (!rootChildren.value.some(n => n.folderId === id)) {
      await ensureRoot()
    }
  }
)

defineExpose({
  refresh: ensureRoot
})
</script>

<template>
  <div class="flex h-full min-h-0 flex-col">
    <div class="flex items-center gap-1 border-b border-[var(--ws-border-subtle)] px-2 py-2">
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        icon="i-lucide-arrow-left"
        aria-label="返回云文档"
        @click="emit('back')"
      />
      <span class="min-w-0 flex-1 truncate text-sm font-semibold">云盘</span>
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        icon="i-lucide-folder-plus"
        aria-label="新建文件夹"
        @click="emit('createFolder')"
      />
    </div>

    <div class="min-h-0 flex-1 overflow-y-auto p-1.5 text-sm">
      <button
        type="button"
        class="workspace-list-item mb-0.5 flex w-full items-center gap-2 px-2 py-1.5 text-left"
        :data-active="selectedFolderId === null ? 'true' : undefined"
        @click="emit('select', null, '我的文件夹')"
      >
        <UIcon
          name="i-lucide-hard-drive"
          class="size-4 shrink-0 text-[var(--ws-text-muted)]"
        />
        <span class="min-w-0 truncate">我的文件夹</span>
      </button>

      <p
        v-if="loading && !rootChildren.length"
        class="px-2 py-3 text-xs text-[var(--ws-text-muted)]"
      >
        加载中…
      </p>

      <DocsDriveFolderBranch
        :nodes="rootChildren"
        :expanded="expanded"
        :selected-folder-id="selectedFolderId"
        :depth="0"
        @toggle="onToggle"
        @select="(id, name) => emit('select', id, name)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { listDriveFolders, type DocsDriveFolder } from '../../api/app/docs-drive'
import { useDocsDriveStore } from '../../stores/docsDrive'
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

const docsDrive = useDocsDriveStore()

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

function findNode(
  nodes: DriveFolderTreeNode[],
  folderId: string
): DriveFolderTreeNode | null {
  for (const node of nodes) {
    if (node.folderId === folderId) {
      return node
    }
    const nested = findNode(node.children, folderId)
    if (nested) {
      return nested
    }
  }
  return null
}

/** Merge server folder list into local nodes; keep expanded subtrees of surviving folders. */
function mergePreserve(
  previous: DriveFolderTreeNode[],
  fresh: DocsDriveFolder[]
): DriveFolderTreeNode[] {
  const prevById = new Map(previous.map(n => [n.folderId, n]))
  return fresh.map((folder) => {
    const prev = prevById.get(folder.folderId)
    if (!prev) {
      return toNode(folder)
    }
    return {
      ...folder,
      children: prev.children,
      loaded: prev.loaded
    }
  })
}

function setChildrenAt(parentId: string | null, folders: DocsDriveFolder[]) {
  if (parentId == null) {
    rootChildren.value = mergePreserve(rootChildren.value, folders)
    return true
  }
  const parent = findNode(rootChildren.value, parentId)
  if (!parent) {
    return false
  }
  parent.children = mergePreserve(parent.children, folders)
  parent.loaded = true
  return true
}

/**
 * Sync tree from the drive listing (create / rename / delete in main pane)
 * without a full-tree reload — preserves expanded branches (no flash).
 */
function syncFromListing() {
  const parentId = docsDrive.currentFolderId
  const folders = docsDrive.listing.folders ?? []
  const applied = setChildrenAt(parentId, folders)
  if (!applied && parentId != null) {
    // Parent not in tree yet — expand path, then apply once nodes exist.
    void expandToSelected(parentId).then(() => {
      setChildrenAt(parentId, folders)
      if (folders.length) {
        const next = new Set(expanded.value)
        next.add(parentId)
        expanded.value = next
      }
    })
    return
  }
  // Creating under a folder: ensure parent is expanded so the new row is visible.
  if (parentId != null && folders.length) {
    const next = new Set(expanded.value)
    if (!next.has(parentId)) {
      next.add(parentId)
      expanded.value = next
    }
  }
}

/** Expand ancestors along store breadcrumbs so the selected folder stays visible. */
async function expandToSelected(folderId: string | null) {
  if (folderId == null || !rootChildren.value.length) {
    return
  }

  const pathIds = docsDrive.breadcrumbs
    .map(c => c.folderId)
    .filter((id): id is string => id != null)

  const ancestors = pathIds.length && pathIds[pathIds.length - 1] === folderId
    ? pathIds.slice(0, -1)
    : pathIds.filter(id => id !== folderId)

  const nextExpanded = new Set(expanded.value)
  let level = rootChildren.value

  for (const ancestorId of ancestors) {
    const node = level.find(n => n.folderId === ancestorId)
    if (!node) {
      break
    }
    if (!node.loaded) {
      node.children = await loadChildren(node.folderId)
      node.loaded = true
    }
    nextExpanded.add(node.folderId)
    level = node.children
  }

  expanded.value = nextExpanded
}

async function ensureRoot() {
  loading.value = true
  try {
    const prevExpanded = new Set(expanded.value)
    rootChildren.value = await loadChildren(null)
    const nextExpanded = new Set<string>()
    async function restore(nodes: DriveFolderTreeNode[]) {
      for (const node of nodes) {
        if (!prevExpanded.has(node.folderId)) {
          continue
        }
        if (!node.loaded) {
          node.children = await loadChildren(node.folderId)
          node.loaded = true
        }
        nextExpanded.add(node.folderId)
        await restore(node.children)
      }
    }
    await restore(rootChildren.value)
    expanded.value = nextExpanded
    await expandToSelected(props.selectedFolderId)
    // Prefer listing for the open folder when available (already fetched by browser/store).
    if (docsDrive.listing) {
      syncFromListing()
    }
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
  (id) => {
    void expandToSelected(id)
  }
)

// Main-pane create / rename / delete updates listing → patch tree locally (no full remount).
watch(
  () => docsDrive.listingVersion,
  (version) => {
    if (version < 1) {
      return
    }
    syncFromListing()
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

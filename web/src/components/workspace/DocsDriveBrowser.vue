<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { DocsDriveFolder, DocsDriveItem } from '../../api/app/docs-drive'
import { infraFileDownloadPath } from '../../api/app/docs-drive'
import { downloadAuthenticatedFile } from '../../api/app/file'
import { useDocsDriveStore } from '../../stores/docsDrive'
import { useDocsStore } from '../../stores/docs'
import type { DocsLibraryNode } from '../../api/app/docs'

const emit = defineEmits<{
  openRichDoc: [objectId: string]
}>()

const drive = useDocsDriveStore()
const docs = useDocsStore()
const toast = useToast()

const fileInputRef = ref<HTMLInputElement | null>(null)
const folderNameOpen = ref(false)
const folderName = ref('新建文件夹')

const renameOpen = ref(false)
const renameTarget = ref<{ kind: 'folder' | 'item', id: string } | null>(null)
const renameValue = ref('')

const moveToLibraryOpen = ref(false)
const moveToLibraryObjectId = ref<string | null>(null)
const moveToLibraryParentId = ref<string | null>(null)

const libraryParentTargets = computed(() => {
  const out: { parentId: string | null, label: string }[] = [{ parentId: null, label: '文档库根目录' }]
  function walk(nodes: DocsLibraryNode[], depth: number) {
    for (const n of nodes) {
      out.push({ parentId: n.nodeId, label: `${'—'.repeat(depth)} ${n.title}` })
      walk(n.children, depth + 1)
    }
  }
  walk(docs.treeRoots, 0)
  return out
})

onMounted(async () => {
  try {
    await drive.loadListing(null)
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '加载云盘失败', color: 'error' })
  }
})

function openCreateFolder() {
  folderName.value = '新建文件夹'
  folderNameOpen.value = true
}

async function confirmCreateFolder() {
  try {
    await drive.createFolder(folderName.value)
    folderNameOpen.value = false
    toast.add({ title: '已新建文件夹', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '创建失败', color: 'error' })
  }
}

function triggerUpload() {
  fileInputRef.value?.click()
}

async function onFilePicked(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  try {
    await drive.registerUploadedFile(file)
    toast.add({ title: '已上传', description: file.name, color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '上传失败', color: 'error' })
  }
}

function openRenameFolder(folder: DocsDriveFolder) {
  renameTarget.value = { kind: 'folder', id: folder.folderId }
  renameValue.value = folder.name
  renameOpen.value = true
}

function openRenameItem(item: DocsDriveItem) {
  renameTarget.value = { kind: 'item', id: item.itemId }
  renameValue.value = item.title
  renameOpen.value = true
}

async function confirmRename() {
  if (!renameTarget.value) {
    return
  }
  try {
    if (renameTarget.value.kind === 'folder') {
      await drive.renameFolder(renameTarget.value.id, renameValue.value)
    } else {
      await drive.renameItem(renameTarget.value.id, renameValue.value)
    }
    renameOpen.value = false
    toast.add({ title: '已重命名', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '重命名失败', color: 'error' })
  }
}

async function onDeleteFolder(folder: DocsDriveFolder) {
  const ok = window.confirm(`确定删除文件夹「${folder.name}」？`)
  if (!ok) {
    return
  }
  try {
    await drive.deleteFolder(folder.folderId)
    toast.add({ title: '已删除', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '删除失败', color: 'error' })
  }
}

async function onDeleteItem(item: DocsDriveItem) {
  const ok = window.confirm(`确定删除「${item.title}」？`)
  if (!ok) {
    return
  }
  try {
    await drive.deleteItem(item.itemId)
    toast.add({ title: '已删除', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '删除失败', color: 'error' })
  }
}

async function onDownload(item: DocsDriveItem) {
  if (item.type !== 'FILE' || !item.storageFileId) {
    toast.add({ title: '无法下载', color: 'error' })
    return
  }
  try {
    await downloadAuthenticatedFile(infraFileDownloadPath(item.storageFileId), item.title)
    toast.add({ title: '已开始下载', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '下载失败', color: 'error' })
  }
}

function onOpenItem(item: DocsDriveItem) {
  if (item.type === 'RICH_DOC') {
    emit('openRichDoc', item.objectId)
    return
  }
  void onDownload(item)
}

function openMoveToLibrary(item: DocsDriveItem) {
  if (item.type !== 'RICH_DOC') {
    toast.add({ title: '文件不能移入文档库', color: 'error' })
    return
  }
  moveToLibraryObjectId.value = item.objectId
  moveToLibraryParentId.value = null
  moveToLibraryOpen.value = true
}

async function confirmMoveToLibrary() {
  if (!moveToLibraryObjectId.value) {
    return
  }
  try {
    await drive.moveToLibrary(moveToLibraryObjectId.value, moveToLibraryParentId.value)
    await docs.loadTree()
    await docs.loadRecent()
    if (docs.activeObjectId === moveToLibraryObjectId.value) {
      docs.clearActive()
    }
    moveToLibraryOpen.value = false
    toast.add({ title: '已移回文档库', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '移动失败', color: 'error' })
  }
}

function folderActions(folder: DocsDriveFolder) {
  return [
    [
      { label: '重命名', icon: 'i-lucide-pencil', onSelect: () => openRenameFolder(folder) },
      {
        label: '删除',
        icon: 'i-lucide-trash-2',
        color: 'error' as const,
        onSelect: () => void onDeleteFolder(folder)
      }
    ]
  ]
}

function itemActions(item: DocsDriveItem) {
  const primary = [
    {
      label: item.type === 'FILE' ? '下载' : '打开',
      icon: item.type === 'FILE' ? 'i-lucide-download' : 'i-lucide-file-text',
      onSelect: () => void onOpenItem(item)
    },
    { label: '重命名', icon: 'i-lucide-pencil', onSelect: () => openRenameItem(item) }
  ]
  if (item.type === 'RICH_DOC') {
    primary.push({
      label: '移回文档库',
      icon: 'i-lucide-folder-input',
      onSelect: () => openMoveToLibrary(item)
    })
  }
  return [
    primary,
    [
      {
        label: '删除',
        icon: 'i-lucide-trash-2',
        color: 'error' as const,
        onSelect: () => void onDeleteItem(item)
      }
    ]
  ]
}
</script>

<template>
  <div class="flex h-full min-h-0 flex-col">
    <div class="flex flex-wrap items-center gap-2 border-b border-[var(--ws-border-subtle)] px-4 py-3">
      <nav class="flex min-w-0 flex-1 flex-wrap items-center gap-1 text-sm">
        <template
          v-for="(crumb, index) in drive.breadcrumbs"
          :key="`${crumb.folderId ?? 'root'}-${index}`"
        >
          <span
            v-if="index > 0"
            class="text-[var(--ws-text-muted)]"
          >/</span>
          <button
            type="button"
            class="truncate rounded px-1.5 py-0.5 hover:bg-[var(--ws-list-hover)]"
            :class="index === drive.breadcrumbs.length - 1 ? 'font-semibold' : 'text-[var(--ws-text-muted)]'"
            @click="drive.enterFolder(crumb.folderId)"
          >
            {{ crumb.name }}
          </button>
        </template>
      </nav>
      <UButton
        size="sm"
        color="neutral"
        variant="soft"
        icon="i-lucide-folder-plus"
        label="新建文件夹"
        :loading="drive.loading"
        @click="openCreateFolder"
      />
      <UButton
        size="sm"
        color="primary"
        icon="i-lucide-upload"
        label="上传"
        :loading="drive.loading"
        @click="triggerUpload"
      />
      <input
        ref="fileInputRef"
        type="file"
        class="hidden"
        @change="onFilePicked"
      >
    </div>

    <div class="min-h-0 flex-1 overflow-y-auto p-4">
      <p
        v-if="drive.loading && !drive.listing.folders.length && !drive.listing.items.length"
        class="py-12 text-center text-sm text-[var(--ws-text-muted)]"
      >
        加载中…
      </p>
      <p
        v-else-if="!drive.listing.folders.length && !drive.listing.items.length"
        class="py-12 text-center text-sm text-[var(--ws-text-muted)]"
      >
        此文件夹为空。可新建文件夹或上传文件。
      </p>

      <ul
        v-else
        class="divide-y divide-[var(--ws-border-subtle)] rounded-lg border border-[var(--ws-border-subtle)]"
      >
        <li
          v-for="folder in drive.listing.folders"
          :key="folder.folderId"
          class="flex items-center gap-3 px-3 py-2.5 hover:bg-[var(--ws-list-hover)]"
        >
          <button
            type="button"
            class="flex min-w-0 flex-1 items-center gap-3 text-left"
            @click="drive.enterFolder(folder.folderId, folder.name)"
          >
            <UIcon
              name="i-lucide-folder"
              class="size-5 shrink-0 text-amber-500"
            />
            <span class="min-w-0 flex-1 truncate font-medium">{{ folder.name }}</span>
            <span class="shrink-0 text-xs text-[var(--ws-text-muted)]">文件夹</span>
          </button>
          <UDropdownMenu :items="folderActions(folder)">
            <UButton
              size="xs"
              color="neutral"
              variant="ghost"
              icon="i-lucide-ellipsis"
              aria-label="更多"
            />
          </UDropdownMenu>
        </li>

        <li
          v-for="item in drive.listing.items"
          :key="item.itemId"
          class="flex items-center gap-3 px-3 py-2.5 hover:bg-[var(--ws-list-hover)]"
        >
          <button
            type="button"
            class="flex min-w-0 flex-1 items-center gap-3 text-left"
            @click="onOpenItem(item)"
          >
            <UIcon
              :name="item.type === 'FILE' ? 'i-lucide-file' : 'i-lucide-file-text'"
              class="size-5 shrink-0 text-[var(--ws-text-muted)]"
            />
            <span class="min-w-0 flex-1 truncate font-medium">{{ item.title }}</span>
            <span class="shrink-0 text-xs text-[var(--ws-text-muted)]">
              {{ item.type === 'FILE' ? drive.formatSize(item.sizeBytes) : '文档' }}
            </span>
          </button>
          <UButton
            v-if="item.type === 'FILE'"
            size="xs"
            color="neutral"
            variant="ghost"
            icon="i-lucide-download"
            aria-label="下载"
            @click="onDownload(item)"
          />
          <UDropdownMenu :items="itemActions(item)">
            <UButton
              size="xs"
              color="neutral"
              variant="ghost"
              icon="i-lucide-ellipsis"
              aria-label="更多"
            />
          </UDropdownMenu>
        </li>
      </ul>
    </div>

    <UModal
      v-model:open="folderNameOpen"
      title="新建文件夹"
    >
      <template #body>
        <UFormField label="名称">
          <UInput
            v-model="folderName"
            class="w-full"
            @keyup.enter="confirmCreateFolder"
          />
        </UFormField>
      </template>
      <template #footer>
        <div class="flex justify-end gap-2">
          <UButton
            color="neutral"
            variant="ghost"
            label="取消"
            @click="folderNameOpen = false"
          />
          <UButton
            color="primary"
            label="创建"
            @click="confirmCreateFolder"
          />
        </div>
      </template>
    </UModal>

    <UModal
      v-model:open="renameOpen"
      title="重命名"
    >
      <template #body>
        <UFormField label="名称">
          <UInput
            v-model="renameValue"
            class="w-full"
            @keyup.enter="confirmRename"
          />
        </UFormField>
      </template>
      <template #footer>
        <div class="flex justify-end gap-2">
          <UButton
            color="neutral"
            variant="ghost"
            label="取消"
            @click="renameOpen = false"
          />
          <UButton
            color="primary"
            label="确定"
            @click="confirmRename"
          />
        </div>
      </template>
    </UModal>

    <UModal
      v-model:open="moveToLibraryOpen"
      title="移回文档库"
    >
      <template #body>
        <UFormField label="目标父页面">
          <USelect
            v-model="moveToLibraryParentId"
            :items="libraryParentTargets.map(t => ({ label: t.label, value: t.parentId }))"
            value-key="value"
            class="w-full"
          />
        </UFormField>
      </template>
      <template #footer>
        <div class="flex justify-end gap-2">
          <UButton
            color="neutral"
            variant="ghost"
            label="取消"
            @click="moveToLibraryOpen = false"
          />
          <UButton
            color="primary"
            label="移动"
            @click="confirmMoveToLibrary"
          />
        </div>
      </template>
    </UModal>
  </div>
</template>

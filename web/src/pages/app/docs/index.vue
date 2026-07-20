<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDebounceFn } from '@vueuse/core'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import DocsTreeNodes from '../../../components/workspace/DocsTreeNodes.vue'
import DocsDriveBrowser from '../../../components/workspace/DocsDriveBrowser.vue'
import type { DocsLibraryNode, TipTapDocJson } from '../../../api/app/docs'
import { emptyTipTapDoc } from '../../../api/app/docs'
import { useDocsStore } from '../../../stores/docs'
import { useDocsDriveStore } from '../../../stores/docsDrive'
import { listDriveFolders } from '../../../api/app/docs-drive'

const DocsRichEditor = defineAsyncComponent(() =>
  import('../../../components/workspace/DocsRichEditor.vue')
)

const route = useRoute()
const router = useRouter()
const docs = useDocsStore()
const docsDrive = useDocsDriveStore()
const toast = useToast()

const renameOpen = ref(false)
const renameTitle = ref('')
const renameNodeId = ref<string | null>(null)

const moveOpen = ref(false)
const moveNodeId = ref<string | null>(null)
const moveParentId = ref<string | null>(null)

const moveToDriveOpen = ref(false)
const moveToDriveObjectId = ref<string | null>(null)
const moveToDriveFolderId = ref<string | null>(null)
const driveFolderTargets = ref<{ folderId: string | null, label: string }[]>([
  { folderId: null, label: '我的文件夹（根）' }
])

const draftBody = ref<TipTapDocJson>(emptyTipTapDoc())
const draftTitle = ref('')
const saveHint = ref('')

const panelItems = [
  { key: 'library' as const, label: '我的文档库', enabled: true },
  { key: 'recent' as const, label: '最近', enabled: true },
  { key: 'shared' as const, label: '与我共享', enabled: false },
  { key: 'starred' as const, label: '星标', enabled: false },
  { key: 'drive' as const, label: '云盘', enabled: true },
  { key: 'wiki' as const, label: '知识库', enabled: false }
]

const flatMoveTargets = computed(() => {
  const out: { nodeId: string | null, label: string }[] = [{ nodeId: null, label: '根目录' }]
  function walk(nodes: DocsLibraryNode[], depth: number) {
    for (const n of nodes) {
      if (n.nodeId === moveNodeId.value) {
        continue
      }
      out.push({ nodeId: n.nodeId, label: `${'—'.repeat(depth)} ${n.title}` })
      walk(n.children, depth + 1)
    }
  }
  walk(docs.treeRoots, 0)
  return out
})

onMounted(async () => {
  await docs.ensureHydrated()
  const docId = typeof route.query.docId === 'string' ? route.query.docId : null
  if (docId) {
    try {
      await docs.openDocument(docId)
    } catch {
      toast.add({ title: '无法打开文档', color: 'error' })
    }
  }
})

watch(
  () => docs.activeDocument,
  (doc) => {
    if (!doc) {
      draftBody.value = emptyTipTapDoc()
      draftTitle.value = ''
      return
    }
    draftBody.value = doc.body
    draftTitle.value = doc.title
  },
  { immediate: true }
)

watch(
  () => docs.activeObjectId,
  (id) => {
    const current = typeof route.query.docId === 'string' ? route.query.docId : null
    if (id === current) {
      return
    }
    void router.replace({
      path: '/app/docs',
      query: id ? { docId: id } : {}
    })
  }
)

const persistBody = useDebounceFn(async () => {
  const doc = docs.activeDocument
  if (!doc) {
    return
  }
  try {
    await docs.saveBody(doc.objectId, draftBody.value, doc.contentVersion)
    saveHint.value = '已保存'
    window.setTimeout(() => {
      if (saveHint.value === '已保存') {
        saveHint.value = ''
      }
    }, 1500)
  } catch (e) {
    const msg = e instanceof Error ? e.message : '保存失败'
    saveHint.value = msg
    toast.add({ title: msg, color: 'error' })
  }
}, 1200)

async function onBodyUpdate(value: TipTapDocJson) {
  draftBody.value = value
  saveHint.value = '保存中…'
  await persistBody()
}

async function onTitleBlur() {
  const doc = docs.activeDocument
  if (!doc) {
    return
  }
  try {
    await docs.saveTitle(doc.objectId, draftTitle.value)
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '标题保存失败', color: 'error' })
  }
}

async function selectPanel(key: typeof panelItems[number]['key']) {
  const item = panelItems.find(p => p.key === key)
  if (!item?.enabled) {
    toast.add({ title: '即将推出', description: item?.label, color: 'neutral' })
    return
  }
  if (key === 'drive') {
    docs.panel = 'drive'
    docs.clearActive()
    try {
      await docsDrive.loadListing(docsDrive.currentFolderId)
    } catch (e) {
      toast.add({ title: e instanceof Error ? e.message : '加载云盘失败', color: 'error' })
    }
    return
  }
  docs.panel = key === 'recent' ? 'recent' : 'library'
  if (key === 'library' || key === 'recent') {
    docs.clearActive()
  }
}

async function onOpenDriveRichDoc(objectId: string) {
  try {
    await docs.openDocument(objectId)
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '打开失败', color: 'error' })
  }
}

async function onCreate(parentId: string | null = null) {
  try {
    await docs.createPage(parentId)
    docs.panel = 'library'
    toast.add({ title: '已新建文档', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '新建失败', color: 'error' })
  }
}

async function onOpen(objectId: string) {
  try {
    await docs.openDocument(objectId)
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '打开失败', color: 'error' })
  }
}

function openRename(node: DocsLibraryNode) {
  renameNodeId.value = node.nodeId
  renameTitle.value = node.title
  renameOpen.value = true
}

async function confirmRename() {
  if (!renameNodeId.value) {
    return
  }
  try {
    await docs.renameNode(renameNodeId.value, renameTitle.value)
    renameOpen.value = false
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '重命名失败', color: 'error' })
  }
}

function openMove(node: DocsLibraryNode) {
  moveNodeId.value = node.nodeId
  moveParentId.value = node.parentId
  moveOpen.value = true
}

async function confirmMove() {
  if (!moveNodeId.value) {
    return
  }
  try {
    await docs.moveNode(moveNodeId.value, moveParentId.value)
    moveOpen.value = false
    toast.add({ title: '已移动', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '移动失败', color: 'error' })
  }
}

async function onDelete(node: DocsLibraryNode) {
  const ok = window.confirm(`确定删除「${node.title}」及其子页面？`)
  if (!ok) {
    return
  }
  try {
    await docs.removeNode(node.nodeId)
    toast.add({ title: '已删除', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '删除失败', color: 'error' })
  }
}

async function onExportMd() {
  const doc = docs.activeDocument
  if (!doc) {
    return
  }
  try {
    const markdown = await docs.exportMarkdown(doc.objectId)
    const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${doc.title || 'document'}.md`
    a.click()
    URL.revokeObjectURL(url)
    toast.add({ title: '已导出 Markdown', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '导出失败', color: 'error' })
  }
}

async function openMoveToDrive(node: DocsLibraryNode) {
  moveToDriveObjectId.value = node.objectId
  moveToDriveFolderId.value = null
  try {
    const root = await listDriveFolders(null)
    driveFolderTargets.value = [
      { folderId: null, label: '我的文件夹（根）' },
      ...root.folders.map(f => ({ folderId: f.folderId, label: f.name }))
    ]
  } catch {
    driveFolderTargets.value = [{ folderId: null, label: '我的文件夹（根）' }]
  }
  moveToDriveOpen.value = true
}

async function confirmMoveToDrive() {
  if (!moveToDriveObjectId.value) {
    return
  }
  try {
    await docsDrive.moveToDrive(moveToDriveObjectId.value, moveToDriveFolderId.value)
    await docs.loadTree()
    await docs.loadRecent()
    if (docs.activeObjectId === moveToDriveObjectId.value) {
      docs.clearActive()
    }
    moveToDriveOpen.value = false
    docs.panel = 'drive'
    toast.add({ title: '已移动到云盘', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '移动失败', color: 'error' })
  }
}

function nodeActions(node: DocsLibraryNode) {
  return [
    [
      { label: '新建子文档', icon: 'i-lucide-file-plus', onSelect: () => void onCreate(node.nodeId) },
      { label: '重命名', icon: 'i-lucide-pencil', onSelect: () => openRename(node) },
      { label: '移动到…', icon: 'i-lucide-folder-input', onSelect: () => openMove(node) },
      { label: '移动到云盘', icon: 'i-lucide-hard-drive', onSelect: () => void openMoveToDrive(node) }
    ],
    [
      { label: '删除', icon: 'i-lucide-trash-2', color: 'error' as const, onSelect: () => void onDelete(node) }
    ]
  ]
}
</script>

<route lang="yaml">
meta:
  layout: workspace
</route>

<template>
  <WorkspaceShell>
    <template #panel>
      <div class="flex items-center justify-between border-b border-[var(--ws-border-subtle)] px-4 py-3">
        <div class="font-semibold">
          云文档
        </div>
        <UButton
          size="xs"
          color="primary"
          icon="i-lucide-plus"
          label="新建"
          @click="onCreate(null)"
        />
      </div>
      <div class="space-y-1 p-2 text-sm">
        <button
          v-for="item in panelItems"
          :key="item.key"
          type="button"
          class="workspace-list-item w-full px-3 py-2 text-left"
          :data-active="item.enabled && ((item.key === 'library' && docs.panel === 'library') || (item.key === 'recent' && docs.panel === 'recent') || (item.key === 'drive' && docs.panel === 'drive'))"
          :class="{ 'opacity-50': !item.enabled }"
          @click="selectPanel(item.key)"
        >
          {{ item.label }}
          <span
            v-if="!item.enabled"
            class="ml-1 text-xs text-[var(--ws-text-muted)]"
          >即将推出</span>
        </button>
      </div>

      <div
        v-if="docs.panel === 'library'"
        class="min-h-0 flex-1 overflow-y-auto border-t border-[var(--ws-border-subtle)] p-2"
      >
        <p
          v-if="!docs.treeRoots.length"
          class="px-2 py-3 text-xs text-[var(--ws-text-muted)]"
        >
          还没有文档，点击上方「新建」开始。
        </p>
        <DocsTreeNodes
          :nodes="docs.treeRoots"
          :active-object-id="docs.activeObjectId"
          :actions-for="nodeActions"
          @open="onOpen"
        />
      </div>

      <div
        v-else-if="docs.panel === 'recent'"
        class="min-h-0 flex-1 overflow-y-auto border-t border-[var(--ws-border-subtle)] p-2"
      >
        <p
          v-if="!docs.recentItems.length"
          class="px-2 py-3 text-xs text-[var(--ws-text-muted)]"
        >
          打开过的文档会显示在这里。
        </p>
        <button
          v-for="item in docs.recentItems"
          :key="item.objectId"
          type="button"
          class="workspace-list-item mb-0.5 w-full truncate px-3 py-2 text-left text-sm"
          :data-active="docs.activeObjectId === item.objectId"
          @click="onOpen(item.objectId)"
        >
          {{ item.title }}
        </button>
      </div>

      <div
        v-else-if="docs.panel === 'drive'"
        class="min-h-0 flex-1 overflow-y-auto border-t border-[var(--ws-border-subtle)] p-2"
      >
        <p class="px-2 py-3 text-xs text-[var(--ws-text-muted)]">
          我的文件夹 · 在右侧浏览与上传
        </p>
      </div>
    </template>

    <DocsDriveBrowser
      v-if="docs.panel === 'drive' && !docs.activeDocument"
      @open-rich-doc="onOpenDriveRichDoc"
    />

    <div
      v-else-if="!docs.activeDocument"
      class="flex h-full items-center justify-center p-8"
    >
      <UEmpty
        icon="i-lucide-file-text"
        title="选择或新建文档"
        description="我的文档库支持页面树与富文本编辑；云盘可浏览「我的文件夹」并上传文件"
      >
        <template #actions>
          <UButton
            color="primary"
            label="新建文档"
            @click="onCreate(null)"
          />
          <UButton
            color="neutral"
            variant="soft"
            label="打开云盘"
            @click="selectPanel('drive')"
          />
        </template>
      </UEmpty>
    </div>

    <div
      v-else
      class="flex h-full min-h-0 flex-col"
    >
      <div class="flex items-center gap-2 border-b border-[var(--ws-border-subtle)] px-4 py-2">
        <UInput
          v-model="draftTitle"
          class="max-w-xl flex-1"
          size="lg"
          variant="ghost"
          placeholder="未命名文档"
          @blur="onTitleBlur"
        />
        <span class="text-xs text-[var(--ws-text-muted)]">{{ saveHint }}</span>
        <UButton
          size="sm"
          color="neutral"
          variant="soft"
          icon="i-lucide-download"
          label="导出 Markdown"
          @click="onExportMd"
        />
      </div>
      <Suspense>
        <DocsRichEditor
          :model-value="draftBody"
          class="min-h-0 flex-1"
          @update:model-value="onBodyUpdate"
        />
        <template #fallback>
          <div class="flex flex-1 items-center justify-center text-sm text-[var(--ws-text-muted)]">
            加载编辑器…
          </div>
        </template>
      </Suspense>
    </div>
  </WorkspaceShell>

  <UModal v-model:open="renameOpen" title="重命名">
    <template #body>
      <UFormField label="标题">
        <UInput
          v-model="renameTitle"
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

  <UModal v-model:open="moveOpen" title="移动到">
    <template #body>
      <UFormField label="目标父页面">
        <USelect
          v-model="moveParentId"
          :items="flatMoveTargets.map(t => ({ label: t.label, value: t.nodeId }))"
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
          @click="moveOpen = false"
        />
        <UButton
          color="primary"
          label="移动"
          @click="confirmMove"
        />
      </div>
    </template>
  </UModal>

  <UModal v-model:open="moveToDriveOpen" title="移动到云盘">
    <template #body>
      <UFormField label="目标文件夹">
        <USelect
          v-model="moveToDriveFolderId"
          :items="driveFolderTargets.map(t => ({ label: t.label, value: t.folderId }))"
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
          @click="moveToDriveOpen = false"
        />
        <UButton
          color="primary"
          label="移动"
          @click="confirmMoveToDrive"
        />
      </div>
    </template>
  </UModal>
</template>

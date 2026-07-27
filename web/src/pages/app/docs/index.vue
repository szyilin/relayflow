<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDebounceFn } from '@vueuse/core'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import DocsDriveBrowser from '../../../components/workspace/DocsDriveBrowser.vue'
import DocsDriveFolderTree from '../../../components/workspace/DocsDriveFolderTree.vue'
import DocsLibraryList from '../../../components/workspace/DocsLibraryList.vue'
import DocsRichEditor from '../../../components/workspace/DocsRichEditor.vue'
import DocsRecentList from '../../../components/workspace/DocsRecentList.vue'
import DocsSharedList from '../../../components/workspace/DocsSharedList.vue'
import DocsStarredList from '../../../components/workspace/DocsStarredList.vue'
import DocsShareDialog from '../../../components/workspace/DocsShareDialog.vue'
import AppTreeSelect from '../../../components/AppTreeSelect.vue'
import type { DocsLibraryNode, DocsSharePermission, TipTapDocJson } from '../../../api/app/docs'
import { emptyTipTapDoc } from '../../../api/app/docs'
import { useDocsStore, type DocsHomeTab, type DocsPanel } from '../../../stores/docs'
import { useDocsDriveStore } from '../../../stores/docsDrive'
import {
  libraryNodesToTreeSelectItems,
  loadDriveFolderTreeSelectItems,
  type AppTreeSelectItem
} from '../../../utils/treeSelect'

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
const driveFolderTreeItems = ref<AppTreeSelectItem[]>([
  { id: '__drive_root__', label: '我的文件夹（根）', value: null, icon: 'i-lucide-hard-drive' }
])

const draftBody = ref<TipTapDocJson>(emptyTipTapDoc())
const draftTitle = ref('')
const saveHint = ref('')
/** Where the open document was launched from (for back / path bar). */
const docOpenSource = ref<'library' | 'recent' | 'drive' | 'shared' | 'starred' | 'home' | null>(null)

/** root = 云文档导航；driveExplorer = 滑入文件夹树 */
const navMode = ref<'root' | 'driveExplorer'>('root')
const driveHover = ref(false)
const driveBrowserRef = ref<{ triggerUpload: () => void } | null>(null)
const pendingDriveUpload = ref(false)

const shareOpen = ref(false)
const shareObjectId = ref<string | null>(null)
const shareTitle = ref('')

const shareList = computed(() =>
  shareObjectId.value ? docs.listShares(shareObjectId.value) : []
)

const docEditable = computed(() => docs.activeDocument?.permission !== 'VIEW')

const docMoreItems = computed(() => {
  const doc = docs.activeDocument
  const objectId = doc?.objectId
  const owned = doc?.ownedByMe === true
  const groups: Array<Array<{ label: string, icon?: string, color?: 'error', onSelect: () => void }>> = []
  const primary: Array<{ label: string, icon?: string, onSelect: () => void }> = [
    {
      label: docs.isStarred(objectId ?? '') ? '取消星标' : '星标',
      icon: 'i-lucide-star',
      onSelect: () => {
        if (objectId) {
          void onToggleStar(objectId)
        }
      }
    },
    {
      label: '复制链接',
      icon: 'i-lucide-link',
      onSelect: () => {
        if (objectId) {
          void copyDocLink(objectId)
        }
      }
    },
    {
      label: '导出 Markdown',
      icon: 'i-lucide-download',
      onSelect: () => {
        void onExportMd()
      }
    }
  ]
  if (owned && objectId) {
    primary.unshift({
      label: '分享',
      icon: 'i-lucide-share-2',
      onSelect: () => {
        void openShare(objectId)
      }
    })
  }
  groups.push(primary)
  if (docOpenSource.value === 'recent' && objectId) {
    groups.push([
      {
        label: '从「最近」中移除',
        icon: 'i-lucide-eye-off',
        onSelect: () => {
          void onRemoveRecent(objectId)
        }
      }
    ])
  }
  if (!owned && objectId) {
    groups.push([
      {
        label: '离开分享',
        icon: 'i-lucide-log-out',
        color: 'error',
        onSelect: () => {
          void onLeaveShare(objectId)
        }
      }
    ])
  }
  return groups
})

type PanelKey = DocsPanel | 'wiki'

const panelNav: Array<
  | { type: 'section', label: string }
  | { type: 'item', key: PanelKey, label: string, icon: string, enabled: boolean, enterable?: boolean }
> = [
  { type: 'section', label: '浏览' },
  { type: 'item', key: 'home', label: '主页', icon: 'i-lucide-home', enabled: true },
  { type: 'item', key: 'library', label: '我的文档库', icon: 'i-lucide-book-open', enabled: true },
  { type: 'item', key: 'drive', label: '云盘', icon: 'i-lucide-hard-drive', enabled: true, enterable: true },
  { type: 'item', key: 'wiki', label: '知识库', icon: 'i-lucide-library', enabled: false }
]

const homeTabs: Array<{ key: DocsHomeTab, label: string }> = [
  { key: 'recent', label: '最近访问' },
  { key: 'shared', label: '与我共享' },
  { key: 'starred', label: '星标' }
]

const showDrivePathBar = computed(() =>
  !!docs.activeDocument && (docOpenSource.value === 'drive' || docs.panel === 'drive')
)

const driveDocCrumbs = computed(() => {
  if (!showDrivePathBar.value) {
    return []
  }
  return docsDrive.breadcrumbs
})

const lastOpenedByObjectId = computed(() => {
  const map: Record<string, string | undefined> = {}
  for (const item of docs.recentItems) {
    map[item.objectId] = item.lastOpenedAt ?? undefined
  }
  return map
})

function isPanelActive(key: PanelKey): boolean {
  if (!key || key === 'wiki') {
    return false
  }
  return docs.panel === key
}

const libraryMoveTreeItems = computed(() =>
  libraryNodesToTreeSelectItems(docs.treeRoots, { excludeNodeId: moveNodeId.value })
)

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
  if (!doc || doc.permission === 'VIEW') {
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
  if (!docEditable.value) {
    return
  }
  draftBody.value = value
  saveHint.value = '保存中…'
  await persistBody()
}

async function onTitleBlur() {
  const doc = docs.activeDocument
  if (!doc || !docEditable.value) {
    return
  }
  try {
    await docs.saveTitle(doc.objectId, draftTitle.value)
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '标题保存失败', color: 'error' })
  }
}

async function enterDriveExplorer(options?: { upload?: boolean }) {
  navMode.value = 'driveExplorer'
  docs.panel = 'drive'
  docs.clearActive()
  docOpenSource.value = null
  pendingDriveUpload.value = options?.upload === true
  try {
    await docsDrive.enterFolder(null)
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '加载云盘失败', color: 'error' })
  }
  if (pendingDriveUpload.value) {
    await nextTick()
    driveBrowserRef.value?.triggerUpload()
    pendingDriveUpload.value = false
  }
}

function leaveDriveExplorer() {
  navMode.value = 'root'
  docs.panel = 'home'
  docs.homeTab = 'recent'
  docs.clearActive()
  docOpenSource.value = null
}

async function selectPanel(key: PanelKey) {
  const item = panelNav.find(p => p.type === 'item' && p.key === key)
  if (!item || item.type !== 'item') {
    return
  }
  if (!item.enabled) {
    toast.add({ title: '即将推出', description: item.label, color: 'neutral' })
    return
  }
  if (key === 'drive') {
    await enterDriveExplorer()
    return
  }
  navMode.value = 'root'
  docs.panel = key as DocsPanel
  docs.clearActive()
  docOpenSource.value = null
  if (key === 'home') {
    docs.homeTab = 'recent'
    void docs.loadRecent().catch(() => {})
  }
}

async function selectHomeTab(tab: DocsHomeTab) {
  docs.panel = 'home'
  docs.homeTab = tab
  docs.clearActive()
  docOpenSource.value = null
  if (tab === 'recent') {
    void docs.loadRecent().catch(() => {})
  } else if (tab === 'shared') {
    void docs.loadShared().catch(() => {})
  } else {
    void docs.loadStarred().catch(() => {})
  }
}

async function onHomeUpload() {
  await enterDriveExplorer({ upload: true })
}

function onHomeTemplate() {
  toast.add({ title: '即将推出', description: '模板库', color: 'neutral' })
}

async function onOpenDriveRichDoc(objectId: string) {
  try {
    docOpenSource.value = 'drive'
    docs.panel = 'drive'
    navMode.value = 'driveExplorer'
    await docs.openDocument(objectId)
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '打开失败', color: 'error' })
  }
}

async function onCreate(parentId: string | null = null) {
  try {
    await docs.createPage(parentId)
    navMode.value = 'root'
    docs.panel = 'library'
    docOpenSource.value = 'library'
    toast.add({ title: '已新建文档', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '新建失败', color: 'error' })
  }
}

async function onOpen(objectId: string) {
  try {
    if (docs.panel === 'home') {
      docOpenSource.value = docs.homeTab
    } else if (docs.panel === 'library' || docs.panel === 'drive') {
      docOpenSource.value = docs.panel
    } else {
      docOpenSource.value = 'library'
    }
    await docs.openDocument(objectId)
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '打开失败', color: 'error' })
  }
}

function backFromDocument() {
  const source = docOpenSource.value
  docs.clearActive()
  if (source === 'drive') {
    docs.panel = 'drive'
    navMode.value = 'driveExplorer'
    void docsDrive.loadListing(docsDrive.currentFolderId).catch((e) => {
      toast.add({ title: e instanceof Error ? e.message : '加载云盘失败', color: 'error' })
    })
  } else if (source === 'recent' || source === 'shared' || source === 'starred') {
    navMode.value = 'root'
    docs.panel = 'home'
    docs.homeTab = source
  } else if (source === 'library') {
    navMode.value = 'root'
    docs.panel = 'library'
  } else {
    navMode.value = 'root'
    docs.panel = 'home'
    docs.homeTab = 'recent'
  }
}

async function onDriveTreeSelect(folderId: string | null, name?: string) {
  try {
    await docsDrive.enterFolder(folderId, name)
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '打开文件夹失败', color: 'error' })
  }
}

async function onDriveTreeCreateFolder() {
  const name = window.prompt('文件夹名称', '新建文件夹')
  if (!name?.trim()) {
    return
  }
  try {
    await docsDrive.createFolder(name.trim())
    // Tree syncs from listing (no full refresh / flicker)
    toast.add({ title: '已新建文件夹', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '创建失败', color: 'error' })
  }
}

async function copyDocLink(objectId: string) {
  const url = `${window.location.origin}/app/docs?docId=${objectId}`
  try {
    await navigator.clipboard.writeText(url)
    toast.add({ title: '已复制链接', color: 'success' })
  } catch {
    toast.add({ title: '复制失败', color: 'error' })
  }
}

async function openShare(objectId: string) {
  shareObjectId.value = objectId
  const node = docs.findNodeByObjectId(objectId)
  const recent = docs.recentItems.find(i => i.objectId === objectId)
  shareTitle.value = node?.title || recent?.title || docs.activeDocument?.title || '未命名文档'
  shareOpen.value = true
  try {
    await docs.loadShares(objectId)
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '加载协作者失败', color: 'error' })
  }
}

async function onGrantShare(payload: { granteeUserId: string, granteeDisplayName: string, permission: DocsSharePermission }) {
  if (!shareObjectId.value) {
    return
  }
  try {
    await docs.grantShare(shareObjectId.value, payload.granteeUserId, payload.granteeDisplayName, payload.permission)
    toast.add({ title: `已分享给 ${payload.granteeDisplayName}`, color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '分享失败', color: 'error' })
  }
}

async function onRevokeShare(shareId: string) {
  if (!shareObjectId.value) {
    return
  }
  try {
    await docs.revokeShare(shareObjectId.value, shareId)
    toast.add({ title: '已撤销', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '撤销失败', color: 'error' })
  }
}

async function onUpdateSharePerm(payload: { shareId: string, permission: DocsSharePermission }) {
  if (!shareObjectId.value) {
    return
  }
  try {
    await docs.updateSharePermission(shareObjectId.value, payload.shareId, payload.permission)
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '更新权限失败', color: 'error' })
  }
}

async function onToggleStar(objectId: string) {
  const wasStarred = docs.isStarred(objectId)
  try {
    await docs.toggleStar(objectId)
    toast.add({ title: wasStarred ? '已取消星标' : '已加星标', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '操作失败', color: 'error' })
  }
}

async function onRemoveRecent(objectId: string) {
  try {
    await docs.removeFromRecent(objectId)
    if (docs.activeObjectId === objectId) {
      docs.clearActive()
    }
    toast.add({ title: '已从最近移除', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '移除失败', color: 'error' })
  }
}

async function onLeaveShare(objectId: string) {
  const ok = window.confirm('确定离开该分享？离开后将无法再打开此文档。')
  if (!ok) {
    return
  }
  try {
    await docs.leaveShare(objectId)
    toast.add({ title: '已离开分享', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '离开失败', color: 'error' })
  }
}

async function onDeleteOwnedFromRecent(objectId: string) {
  const node = docs.findNodeByObjectId(objectId)
  if (!node) {
    toast.add({ title: '仅支持删除文档库中的文档', color: 'neutral' })
    return
  }
  await onDelete(node)
}

async function onExportMdFor(objectId: string) {
  try {
    const markdown = await docs.exportMarkdown(objectId)
    const title = docs.recentItems.find(i => i.objectId === objectId)?.title
      || docs.sharedItems.find(i => i.objectId === objectId)?.title
      || 'document'
    const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${title || 'document'}.md`
    a.click()
    URL.revokeObjectURL(url)
    toast.add({ title: '已导出 Markdown', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '导出失败', color: 'error' })
  }
}

async function onDriveCrumbClick(folderId: string | null) {
  docs.clearActive()
  docs.panel = 'drive'
  navMode.value = 'driveExplorer'
  docOpenSource.value = null
  try {
    await docsDrive.enterFolder(folderId)
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '打开文件夹失败', color: 'error' })
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
    driveFolderTreeItems.value = await loadDriveFolderTreeSelectItems()
  } catch {
    driveFolderTreeItems.value = [
      { id: '__drive_root__', label: '我的文件夹（根）', value: null, icon: 'i-lucide-hard-drive' }
    ]
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
    navMode.value = 'driveExplorer'
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
      { label: '移动到云盘', icon: 'i-lucide-hard-drive', onSelect: () => void openMoveToDrive(node) },
      { label: '分享', icon: 'i-lucide-share-2', onSelect: () => void openShare(node.objectId) },
      {
        label: docs.isStarred(node.objectId) ? '取消星标' : '星标',
        icon: 'i-lucide-star',
        onSelect: () => void onToggleStar(node.objectId)
      },
      {
        label: '导出 Markdown',
        icon: 'i-lucide-download',
        onSelect: () => void onExportMdFor(node.objectId)
      }
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
      <div
        class="relative flex min-h-0 flex-1 flex-col overflow-hidden"
      >
        <!-- 云文档根导航 -->
        <div
          class="flex min-h-0 flex-1 flex-col transition-transform duration-300 ease-out"
          :class="navMode === 'driveExplorer' ? '-translate-x-full absolute inset-0 pointer-events-none' : 'translate-x-0 relative'"
        >
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
          <div class="min-h-0 flex-1 space-y-3 overflow-y-auto p-2 text-sm">
            <template
              v-for="(entry, idx) in panelNav"
              :key="entry.type === 'section' ? `s-${entry.label}-${idx}` : entry.key"
            >
              <div
                v-if="entry.type === 'section'"
                class="px-3 pt-1 text-[11px] font-semibold tracking-wide text-[var(--ws-text-muted)]"
                :class="idx === 0 ? '' : 'mt-1 border-t border-[var(--ws-border-subtle)] pt-3'"
              >
                {{ entry.label }}
              </div>
              <button
                v-else
                type="button"
                class="workspace-list-item group flex w-full items-center gap-2.5 px-3 py-2 text-left"
                :data-active="entry.enabled && isPanelActive(entry.key) ? 'true' : undefined"
                :class="{ 'opacity-50': !entry.enabled }"
                @click="selectPanel(entry.key)"
                @mouseenter="entry.key === 'drive' ? (driveHover = true) : undefined"
                @mouseleave="entry.key === 'drive' ? (driveHover = false) : undefined"
              >
                <UIcon
                  :name="entry.icon"
                  class="size-4 shrink-0"
                  :class="entry.enabled && isPanelActive(entry.key) ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
                />
                <span class="min-w-0 flex-1 truncate">{{ entry.label }}</span>
                <span
                  v-if="!entry.enabled"
                  class="shrink-0 text-[10px] text-[var(--ws-text-muted)]"
                >即将推出</span>
                <UIcon
                  v-else-if="entry.enterable && (driveHover || isPanelActive('drive'))"
                  name="i-lucide-chevron-right"
                  class="size-4 shrink-0 text-[var(--ws-text-muted)] opacity-80"
                />
              </button>
            </template>
          </div>
        </div>

        <!-- 云盘滑入文件夹树 -->
        <div
          class="absolute inset-0 flex min-h-0 flex-col bg-[var(--ws-bg)] transition-transform duration-300 ease-out"
          :class="navMode === 'driveExplorer' ? 'translate-x-0' : 'translate-x-full pointer-events-none'"
        >
          <DocsDriveFolderTree
            v-if="navMode === 'driveExplorer'"
            :selected-folder-id="docsDrive.currentFolderId"
            @back="leaveDriveExplorer"
            @select="onDriveTreeSelect"
            @create-folder="onDriveTreeCreateFolder"
          />
        </div>
      </div>
    </template>

    <!-- 主区：主页 -->
    <div
      v-if="docs.panel === 'home' && !docs.activeDocument"
      class="flex h-full min-h-0 flex-col"
    >
      <div class="grid grid-cols-1 gap-3 border-b border-[var(--ws-border-subtle)] px-5 py-4 sm:grid-cols-3">
        <button
          type="button"
          class="flex items-center gap-3 rounded-xl border border-[var(--ws-border-subtle)] px-4 py-3 text-left hover:bg-[var(--ws-list-hover)]"
          @click="onCreate(null)"
        >
          <span class="flex size-10 items-center justify-center rounded-lg bg-[var(--ui-primary)]/10 text-[var(--ui-primary)]">
            <UIcon
              name="i-lucide-file-plus"
              class="size-5"
            />
          </span>
          <span>
            <span class="block text-sm font-semibold">新建</span>
            <span class="text-xs text-[var(--ws-text-muted)]">创建文档</span>
          </span>
        </button>
        <button
          type="button"
          class="flex items-center gap-3 rounded-xl border border-[var(--ws-border-subtle)] px-4 py-3 text-left hover:bg-[var(--ws-list-hover)]"
          @click="onHomeUpload"
        >
          <span class="flex size-10 items-center justify-center rounded-lg bg-[var(--ws-bg-muted)]">
            <UIcon
              name="i-lucide-upload"
              class="size-5 text-[var(--ws-text-secondary)]"
            />
          </span>
          <span>
            <span class="block text-sm font-semibold">上传</span>
            <span class="text-xs text-[var(--ws-text-muted)]">进入云盘上传</span>
          </span>
        </button>
        <button
          type="button"
          class="flex items-center gap-3 rounded-xl border border-[var(--ws-border-subtle)] px-4 py-3 text-left hover:bg-[var(--ws-list-hover)]"
          @click="onHomeTemplate"
        >
          <span class="flex size-10 items-center justify-center rounded-lg bg-[var(--ws-bg-muted)]">
            <UIcon
              name="i-lucide-layout-template"
              class="size-5 text-[var(--ws-text-secondary)]"
            />
          </span>
          <span>
            <span class="block text-sm font-semibold">模板库</span>
            <span class="text-xs text-[var(--ws-text-muted)]">即将推出</span>
          </span>
        </button>
      </div>

      <div class="flex items-center gap-1 border-b border-[var(--ws-border-subtle)] px-3 pt-2">
        <button
          v-for="tab in homeTabs"
          :key="tab.key"
          type="button"
          class="relative px-3 py-2 text-sm transition-colors"
          :class="docs.homeTab === tab.key
            ? 'font-semibold text-[var(--ws-text)]'
            : 'text-[var(--ws-text-muted)] hover:text-[var(--ws-text)]'"
          @click="selectHomeTab(tab.key)"
        >
          {{ tab.label }}
          <span
            v-if="docs.homeTab === tab.key"
            class="absolute inset-x-2 -bottom-px h-0.5 rounded-full bg-[var(--ui-primary)]"
          />
        </button>
      </div>

      <div class="min-h-0 flex-1">
        <DocsRecentList
          v-if="docs.homeTab === 'recent'"
          embedded
          :items="docs.recentItems"
          :active-object-id="docs.activeObjectId"
          @open="onOpen"
          @remove="(id) => void onRemoveRecent(id)"
          @delete-owned="onDeleteOwnedFromRecent"
          @share="(id) => void openShare(id)"
          @star="(id) => void onToggleStar(id)"
          @copy-link="(id) => void copyDocLink(id)"
          @export-md="(id) => void onExportMdFor(id)"
        />
        <DocsSharedList
          v-else-if="docs.homeTab === 'shared'"
          embedded
          :items="docs.sharedItems"
          :active-object-id="docs.activeObjectId"
          @open="onOpen"
          @leave="(id) => void onLeaveShare(id)"
          @star="(id) => void onToggleStar(id)"
          @copy-link="(id) => void copyDocLink(id)"
          @export-md="(id) => void onExportMdFor(id)"
        />
        <DocsStarredList
          v-else
          embedded
          :items="docs.starredItems"
          :active-object-id="docs.activeObjectId"
          @open="onOpen"
          @unstar="(id) => void onToggleStar(id)"
          @share="(id) => void openShare(id)"
          @copy-link="(id) => void copyDocLink(id)"
          @export-md="(id) => void onExportMdFor(id)"
        />
      </div>
    </div>

    <DocsLibraryList
      v-else-if="docs.panel === 'library' && !docs.activeDocument"
      :nodes="docs.treeRoots"
      :active-object-id="docs.activeObjectId"
      :actions-for="nodeActions"
      :last-opened-by-object-id="lastOpenedByObjectId"
      @open="onOpen"
    />

    <DocsDriveBrowser
      v-else-if="docs.panel === 'drive' && !docs.activeDocument"
      ref="driveBrowserRef"
      hide-breadcrumbs
      @open-rich-doc="onOpenDriveRichDoc"
    />

    <div
      v-else-if="!docs.activeDocument"
      class="flex h-full items-center justify-center p-8"
    >
      <UEmpty
        icon="i-lucide-file-text"
        title="选择或新建文档"
        description="从主页浏览最近与共享，或打开我的文档库与云盘"
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
            label="打开主页"
            @click="selectPanel('home')"
          />
        </template>
      </UEmpty>
    </div>

    <div
      v-else
      class="flex h-full min-h-0 flex-col"
    >
      <div
        v-if="showDrivePathBar"
        class="flex items-center gap-1 border-b border-[var(--ws-border-subtle)] px-3 py-1.5"
      >
        <UButton
          size="xs"
          color="neutral"
          variant="ghost"
          icon="i-lucide-arrow-left"
          aria-label="返回云盘"
          @click="backFromDocument"
        />
        <nav class="flex min-w-0 flex-1 items-center gap-1 overflow-x-auto text-xs">
          <template
            v-for="(crumb, index) in driveDocCrumbs"
            :key="`${crumb.folderId ?? 'root'}-${index}`"
          >
            <span
              v-if="index > 0"
              class="shrink-0 text-[var(--ws-text-muted)]"
            >/</span>
            <button
              type="button"
              class="max-w-36 shrink-0 truncate rounded px-1 py-0.5 text-[var(--ws-text-muted)] hover:bg-[var(--ws-rail-hover)] hover:text-[var(--ws-text)]"
              @click="onDriveCrumbClick(crumb.folderId)"
            >
              {{ crumb.name }}
            </button>
          </template>
          <span class="shrink-0 text-[var(--ws-text-muted)]">/</span>
          <span class="min-w-0 truncate font-medium text-[var(--ws-text)]">
            {{ draftTitle || '未命名文档' }}
          </span>
        </nav>
      </div>
      <div class="flex items-center gap-2 border-b border-[var(--ws-border-subtle)] px-4 py-2">
        <UButton
          v-if="!showDrivePathBar"
          size="sm"
          color="neutral"
          variant="ghost"
          icon="i-lucide-arrow-left"
          aria-label="返回"
          @click="backFromDocument"
        />
        <UInput
          v-model="draftTitle"
          class="min-w-0 flex-1"
          size="lg"
          variant="ghost"
          placeholder="未命名文档"
          :disabled="!docEditable"
          @blur="onTitleBlur"
        />
        <span class="shrink-0 text-xs text-[var(--ws-text-muted)]">{{ saveHint }}</span>
        <UDropdownMenu :items="docMoreItems">
          <UButton
            size="sm"
            color="neutral"
            variant="ghost"
            icon="i-lucide-ellipsis"
            aria-label="更多"
          />
        </UDropdownMenu>
      </div>
      <DocsRichEditor
        :model-value="draftBody"
        :editable="docEditable"
        class="min-h-0 flex-1"
        @update:model-value="onBodyUpdate"
      />
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
        <AppTreeSelect
          v-model="moveParentId"
          :items="libraryMoveTreeItems"
          placeholder="选择目标父页面"
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
        <AppTreeSelect
          v-model="moveToDriveFolderId"
          :items="driveFolderTreeItems"
          placeholder="选择目标文件夹"
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

  <DocsShareDialog
    v-model:open="shareOpen"
    :object-id="shareObjectId"
    :title="shareTitle"
    :shares="shareList"
    @grant="onGrantShare"
    @revoke="onRevokeShare"
    @update-permission="onUpdateSharePerm"
  />
</template>

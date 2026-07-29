import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  createDocumentShare,
  createLibraryNode,
  deleteDocumentShare,
  deleteLibraryNode,
  exportDocumentMarkdown,
  getDocsEffectiveSettings,
  getDocument,
  leaveSharedDocument,
  listDocumentShares,
  listLibraryTree,
  listRecentDocuments,
  listSharedWithMe,
  listStarredDocuments,
  removeRecentDocument,
  saveDocumentBody,
  starDocument,
  unstarDocument,
  updateDocumentShare,
  updateLibraryNode,
  type DocsDocument,
  type DocsEffectiveSettings,
  type DocsLibraryNode,
  type DocsRecentItem,
  type DocsShareItem,
  type DocsSharePermission,
  type DocsSharedWithMeItem,
  type DocsStarredItem,
  type TipTapDocJson
} from '../api/app/docs'
import { ApiError } from '../api/request'
import { useAuthStore } from './auth'
import { useDocsDriveStore } from './docsDrive'
import { useProfileStore } from './profile'

/** Business error codes from relayflow-module-docs ErrorCodeConstants */
const DOC_NOT_FOUND = 1_006_001_001
const DOC_FORBIDDEN = 1_006_001_002
const DOC_VERSION_CONFLICT = 1_006_001_003
const DOC_MEMBER_SHARE_DISABLED = 1_006_001_014
const DOC_MARKDOWN_EXPORT_DISABLED = 1_006_001_015
const DOC_DRIVE_UPLOAD_DISABLED = 1_006_001_016

function cloneDoc(body: TipTapDocJson): TipTapDocJson {
  return JSON.parse(JSON.stringify(body)) as TipTapDocJson
}

function toStoreError(error: unknown, fallback: string): Error {
  if (error instanceof ApiError) {
    if (error.code === DOC_NOT_FOUND) {
      return new Error('文档不存在或已被删除')
    }
    if (error.code === DOC_FORBIDDEN) {
      return new Error('无权访问该文档（分享已取消或已离开）')
    }
    if (error.code === DOC_MEMBER_SHARE_DISABLED) {
      return new Error('企业已禁止成员分享文档')
    }
    if (error.code === DOC_MARKDOWN_EXPORT_DISABLED) {
      return new Error('企业已禁止导出 Markdown')
    }
    if (error.code === DOC_DRIVE_UPLOAD_DISABLED) {
      return new Error('企业已禁止上传云盘文件')
    }
    return new Error(error.message || fallback)
  }
  if (error instanceof Error) {
    return error
  }
  return new Error(fallback)
}

function findNodeInTree(
  nodes: DocsLibraryNode[],
  predicate: (node: DocsLibraryNode) => boolean
): DocsLibraryNode | undefined {
  for (const node of nodes) {
    if (predicate(node)) {
      return node
    }
    const child = findNodeInTree(node.children, predicate)
    if (child) {
      return child
    }
  }
  return undefined
}

function patchTreeTitle(nodes: DocsLibraryNode[], objectId: string, title: string): DocsLibraryNode[] {
  return nodes.map((node) => {
    const children = patchTreeTitle(node.children, objectId, title)
    if (node.objectId === objectId) {
      return { ...node, title, children }
    }
    if (children !== node.children) {
      return { ...node, children }
    }
    return node
  })
}

export type DocsPanel = 'home' | 'library' | 'drive'
export type DocsHomeTab = 'recent' | 'shared' | 'starred'

export const useDocsStore = defineStore('docs', () => {
  const panel = ref<DocsPanel>('home')
  const homeTab = ref<DocsHomeTab>('recent')
  const loading = ref(false)
  const saving = ref(false)
  const error = ref<string | null>(null)
  const hydrated = ref(false)

  const treeRoots = ref<DocsLibraryNode[]>([])
  const recentItems = ref<DocsRecentItem[]>([])
  const sharedItems = ref<DocsSharedWithMeItem[]>([])
  const starredItems = ref<DocsStarredItem[]>([])
  const sharesByObject = ref<Record<string, DocsShareItem[]>>({})

  const activeObjectId = ref<string | null>(null)
  const activeDocument = ref<DocsDocument | null>(null)
  const effectiveSettings = ref<DocsEffectiveSettings | null>(null)

  const memberShareEnabled = computed(() => effectiveSettings.value?.memberShareEnabled ?? true)
  const markdownExportEnabled = computed(() => effectiveSettings.value?.markdownExportEnabled ?? true)
  const driveUploadEnabled = computed(() => effectiveSettings.value?.driveUploadEnabled ?? true)

  function myUserId() {
    return useAuthStore().userId ?? useProfileStore().profile?.userId ?? ''
  }

  function findNodeByObjectId(objectId: string): DocsLibraryNode | undefined {
    return findNodeInTree(treeRoots.value, n => n.objectId === objectId)
  }

  function isStarred(objectId: string) {
    if (!objectId) {
      return false
    }
    if (starredItems.value.some(i => i.objectId === objectId)) {
      return true
    }
    if (sharedItems.value.some(i => i.objectId === objectId && i.starred)) {
      return true
    }
    return recentItems.value.some(i => i.objectId === objectId && i.starred)
  }

  function listShares(objectId: string): DocsShareItem[] {
    return sharesByObject.value[objectId] ?? []
  }

  async function loadTree() {
    const resp = await listLibraryTree()
    treeRoots.value = resp.nodes ?? []
  }

  async function loadRecent(limit = 20) {
    try {
      recentItems.value = await listRecentDocuments(limit)
    } catch {
      recentItems.value = []
    }
  }

  async function loadShared(limit = 50) {
    try {
      const resp = await listSharedWithMe(limit)
      sharedItems.value = resp.items ?? []
    } catch {
      sharedItems.value = []
    }
  }

  async function loadStarred(limit = 50) {
    try {
      const resp = await listStarredDocuments(limit)
      starredItems.value = resp.items ?? []
    } catch {
      starredItems.value = []
    }
  }

  async function loadShares(objectId: string) {
    const resp = await listDocumentShares(objectId)
    sharesByObject.value = {
      ...sharesByObject.value,
      [objectId]: resp.shares ?? []
    }
    return sharesByObject.value[objectId]
  }

  async function ensureHydrated() {
    if (hydrated.value) {
      return
    }
    loading.value = true
    error.value = null
    try {
      if (!useProfileStore().profile) {
        await useProfileStore().fetchProfile().catch(() => null)
      }
      await Promise.all([
        loadTree(),
        loadRecent(),
        loadShared(),
        loadStarred(),
        ensureEffectiveSettings()
      ])
      hydrated.value = true
    } catch (e) {
      error.value = toStoreError(e, '加载文档库失败').message
      throw e
    } finally {
      loading.value = false
    }
  }

  async function ensureEffectiveSettings() {
    if (effectiveSettings.value) {
      return effectiveSettings.value
    }
    try {
      effectiveSettings.value = await getDocsEffectiveSettings()
    } catch {
      effectiveSettings.value = {
        memberShareEnabled: true,
        markdownExportEnabled: true,
        driveUploadEnabled: true
      }
    }
    return effectiveSettings.value
  }

  async function refreshCollabLists() {
    await Promise.all([loadRecent(), loadShared(), loadStarred()])
  }

  async function createPage(parentId: string | null = null, title = '未命名文档') {
    await ensureHydrated()
    const created = await createLibraryNode({ parentId, title })
    await loadTree()
    await openDocument(created.objectId)
    return { nodeId: created.nodeId, objectId: created.objectId }
  }

  async function renameNode(nodeId: string, title: string) {
    const next = title.trim() || '未命名文档'
    const updated = await updateLibraryNode(nodeId, { title: next })
    treeRoots.value = patchTreeTitle(treeRoots.value, updated.objectId, updated.title)
    if (activeDocument.value?.objectId === updated.objectId) {
      activeDocument.value = { ...activeDocument.value, title: updated.title }
    }
    recentItems.value = recentItems.value.map(item =>
      item.objectId === updated.objectId
        ? { ...item, title: updated.title, titleSnapshot: updated.title }
        : item
    )
    sharedItems.value = sharedItems.value.map(item =>
      item.objectId === updated.objectId ? { ...item, title: updated.title } : item
    )
    starredItems.value = starredItems.value.map(item =>
      item.objectId === updated.objectId ? { ...item, title: updated.title } : item
    )
  }

  async function moveNode(nodeId: string, parentId: string | null) {
    await updateLibraryNode(nodeId, { parentId })
    await loadTree()
  }

  async function removeNode(nodeId: string) {
    const node = findNodeInTree(treeRoots.value, n => n.nodeId === nodeId)
    const removedObjectId = node?.objectId
    await deleteLibraryNode(nodeId)
    await loadTree()
    await refreshCollabLists()
    if (removedObjectId && activeObjectId.value === removedObjectId) {
      clearActive()
    }
  }

  async function openDocument(objectId: string) {
    const tombstone = recentItems.value.find(i => i.objectId === objectId)
    if (tombstone?.accessState === 'DELETED') {
      throw new Error('文档已被删除，无法打开')
    }
    if (tombstone?.accessState === 'NO_ACCESS') {
      throw new Error('无权访问该文档（分享已取消或已离开）')
    }

    try {
      const doc = await getDocument(objectId)
      const enriched: DocsDocument = {
        ...doc,
        body: cloneDoc(doc.body),
        ownedByMe: doc.ownedByMe ?? findNodeByObjectId(objectId) != null,
        permission: doc.permission ?? 'EDIT',
        ownerUserId: doc.ownerUserId ?? myUserId()
      }
      activeObjectId.value = objectId
      activeDocument.value = enriched
      await loadRecent()
    } catch (e) {
      throw toStoreError(e, '打开文档失败')
    }
  }

  async function saveTitle(objectId: string, title: string) {
    if (activeDocument.value?.objectId === objectId && activeDocument.value.permission === 'VIEW') {
      throw new Error('无权重命名')
    }
    const node = findNodeByObjectId(objectId)
    if (node) {
      await renameNode(node.nodeId, title)
      return
    }
    const drive = useDocsDriveStore()
    const item = drive.listing.items.find(i => i.objectId === objectId)
    if (item) {
      const next = title.trim() || '未命名文档'
      await drive.renameItem(item.itemId, next)
      if (activeDocument.value?.objectId === objectId) {
        activeDocument.value = { ...activeDocument.value, title: next }
      }
      return
    }
    throw new Error('文档不存在或无权重命名')
  }

  async function saveBody(objectId: string, body: TipTapDocJson, contentVersion: number) {
    saving.value = true
    try {
      if (activeDocument.value?.objectId === objectId && activeDocument.value.permission === 'VIEW') {
        throw new Error('无权编辑')
      }
      const resp = await saveDocumentBody(objectId, { body, contentVersion })
      if (activeDocument.value?.objectId === objectId) {
        activeDocument.value = {
          ...activeDocument.value,
          body: cloneDoc(body),
          contentVersion: resp.contentVersion
        }
      }
      return resp.contentVersion
    } catch (e) {
      if (e instanceof ApiError && e.code === DOC_VERSION_CONFLICT) {
        throw toStoreError(e, '文档已被更新，请刷新后重试')
      }
      throw toStoreError(e, '保存失败')
    } finally {
      saving.value = false
    }
  }

  async function exportMarkdown(objectId: string): Promise<string> {
    await ensureEffectiveSettings()
    if (!markdownExportEnabled.value) {
      throw new Error('企业已禁止导出 Markdown')
    }
    try {
      const resp = await exportDocumentMarkdown(objectId)
      return resp.markdown
    } catch (e) {
      throw toStoreError(e, '导出失败')
    }
  }

  async function grantShare(
    objectId: string,
    granteeUserId: string,
    _granteeDisplayName: string,
    permission: DocsSharePermission
  ) {
    await ensureEffectiveSettings()
    if (!memberShareEnabled.value) {
      throw new Error('企业已禁止成员分享文档')
    }
    if (granteeUserId === myUserId()) {
      throw new Error('不能分享给自己')
    }
    try {
      await createDocumentShare(objectId, { granteeUserId, permission })
      await loadShares(objectId)
    } catch (e) {
      throw toStoreError(e, '分享失败')
    }
  }

  async function updateSharePermission(
    objectId: string,
    shareId: string,
    permission: DocsSharePermission
  ) {
    await ensureEffectiveSettings()
    if (!memberShareEnabled.value) {
      throw new Error('企业已禁止成员分享文档')
    }
    try {
      await updateDocumentShare(objectId, shareId, { permission })
      await loadShares(objectId)
    } catch (e) {
      throw toStoreError(e, '更新权限失败')
    }
  }

  async function revokeShare(objectId: string, shareId: string) {
    await ensureEffectiveSettings()
    if (!memberShareEnabled.value) {
      throw new Error('企业已禁止成员分享文档')
    }
    try {
      await deleteDocumentShare(objectId, shareId)
      await loadShares(objectId)
    } catch (e) {
      throw toStoreError(e, '撤销失败')
    }
  }

  async function leaveShare(objectId: string) {
    try {
      await leaveSharedDocument(objectId)
      if (activeObjectId.value === objectId) {
        clearActive()
      }
      await refreshCollabLists()
    } catch (e) {
      throw toStoreError(e, '离开分享失败')
    }
  }

  async function toggleStar(objectId: string) {
    const starred = isStarred(objectId)
    try {
      if (starred) {
        await unstarDocument(objectId)
      } else {
        const visit = recentItems.value.find(r => r.objectId === objectId)
        if (visit?.accessState && visit.accessState !== 'OK') {
          throw new Error('无法星标不可访问的文档')
        }
        await starDocument(objectId)
      }
      await refreshCollabLists()
    } catch (e) {
      throw toStoreError(e, '星标操作失败')
    }
  }

  async function removeFromRecent(objectId: string) {
    try {
      await removeRecentDocument(objectId)
      recentItems.value = recentItems.value.filter(i => i.objectId !== objectId)
    } catch (e) {
      throw toStoreError(e, '从最近移除失败')
    }
  }

  function clearActive() {
    activeObjectId.value = null
    activeDocument.value = null
  }

  function resetLocal() {
    hydrated.value = false
    effectiveSettings.value = null
    treeRoots.value = []
    recentItems.value = []
    sharedItems.value = []
    starredItems.value = []
    sharesByObject.value = {}
    clearActive()
    error.value = null
    loading.value = false
    saving.value = false
    panel.value = 'home'
    homeTab.value = 'recent'
  }

  return {
    panel,
    homeTab,
    loading,
    saving,
    error,
    treeRoots,
    recentItems,
    sharedItems,
    starredItems,
    activeObjectId,
    activeDocument,
    effectiveSettings,
    memberShareEnabled,
    markdownExportEnabled,
    driveUploadEnabled,
    ensureHydrated,
    ensureEffectiveSettings,
    loadTree,
    loadRecent,
    loadShared,
    loadStarred,
    loadShares,
    refreshCollabLists,
    createPage,
    renameNode,
    moveNode,
    removeNode,
    openDocument,
    saveTitle,
    saveBody,
    exportMarkdown,
    clearActive,
    resetLocal,
    findNodeByObjectId,
    listShares,
    grantShare,
    updateSharePermission,
    revokeShare,
    leaveShare,
    toggleStar,
    isStarred,
    removeFromRecent
  }
})

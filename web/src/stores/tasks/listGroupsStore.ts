import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { TaskItem } from '../../api/app/task'
import {
  createListGroup as createListGroupApi,
  deleteListGroup as deleteListGroupApi,
  listListGroups,
  moveListGroupTask,
  type ListGroup
} from '../../api/app/taskListGroup'
import type { TaskGroupBucket } from './groupByLocal'

export type { ListGroup }

interface ListGroupCache {
  groups: ListGroup[]
  membership: Record<string, string>
  loaded: boolean
}

function emptyCache(): ListGroupCache {
  return { groups: [], membership: {}, loaded: false }
}

export const useListGroupsStore = defineStore('listGroups', () => {
  const byListId = ref<Record<string, ListGroupCache>>({})
  const activeListId = ref<string | null>(null)
  const loading = ref(false)

  function ensureCache(listId: string): ListGroupCache {
    if (!byListId.value[listId]) {
      byListId.value = {
        ...byListId.value,
        [listId]: emptyCache()
      }
    }
    return byListId.value[listId]!
  }

  function setActiveList(listId: string | null) {
    activeListId.value = listId
  }

  const activeCache = computed(() => {
    const id = activeListId.value
    if (!id) {
      return emptyCache()
    }
    return ensureCache(id)
  })

  const groups = computed(() => activeCache.value.groups)

  const sortedGroups = computed(() =>
    [...activeCache.value.groups].sort((a, b) => a.rank - b.rank || a.id.localeCompare(b.id))
  )

  const defaultGroupId = computed(
    () => activeCache.value.groups.find(g => g.isDefault)?.id ?? ''
  )

  function applyList(listId: string, result: Awaited<ReturnType<typeof listListGroups>>) {
    const membership: Record<string, string> = {}
    for (const m of result.memberships) {
      membership[m.taskId] = m.groupId
    }
    byListId.value = {
      ...byListId.value,
      [listId]: {
        groups: result.groups,
        membership,
        loaded: true
      }
    }
  }

  async function fetchList(listId: string | null | undefined, force = false) {
    if (!listId) {
      return
    }
    if (loading.value) {
      return
    }
    const cache = ensureCache(listId)
    if (cache.loaded && !force) {
      return
    }
    loading.value = true
    try {
      applyList(listId, await listListGroups(listId))
    } finally {
      loading.value = false
    }
  }

  function mutateActive(updater: (cache: ListGroupCache) => ListGroupCache) {
    const id = activeListId.value
    if (!id) {
      return
    }
    byListId.value = {
      ...byListId.value,
      [id]: updater(ensureCache(id))
    }
  }

  function ensureMembership(taskId: string) {
    const cache = activeCache.value
    if (cache.membership[taskId]) {
      return
    }
    const fallback = defaultGroupId.value
    if (!fallback) {
      return
    }
    mutateActive(c => ({
      ...c,
      membership: { ...c.membership, [taskId]: fallback }
    }))
  }

  function partition(items: TaskItem[]): TaskGroupBucket[] {
    for (const item of items) {
      ensureMembership(item.id)
    }
    const cache = activeCache.value
    const buckets = sortedGroups.value.map(g => ({
      key: g.id,
      label: g.name,
      items: items.filter(item => cache.membership[item.id] === g.id)
    }))
    if (!buckets.length) {
      return [{ key: '__all__', label: '清单分组', items: [...items] }]
    }
    return buckets
  }

  async function createGroup(name: string): Promise<ListGroup> {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_GROUP_FORBIDDEN')
    }
    const trimmed = name.trim()
    if (!trimmed) {
      throw new Error('TASK_LIST_GROUP_NAME_EMPTY')
    }
    const group = await createListGroupApi(listId, trimmed)
    mutateActive(c => ({
      ...c,
      groups: [...c.groups, group]
    }))
    return group
  }

  async function deleteGroup(groupId: string) {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_GROUP_FORBIDDEN')
    }
    const cache = ensureCache(listId)
    const target = cache.groups.find(g => g.id === groupId)
    if (!target) {
      throw new Error('TASK_LIST_GROUP_NOT_FOUND')
    }
    if (target.isDefault) {
      throw new Error('TASK_LIST_GROUP_FORBIDDEN')
    }
    const prev = { ...cache, membership: { ...cache.membership }, groups: [...cache.groups] }
    const fallback = defaultGroupId.value
    const nextMembership = { ...cache.membership }
    for (const [taskId, gid] of Object.entries(nextMembership)) {
      if (gid === groupId) {
        nextMembership[taskId] = fallback
      }
    }
    byListId.value = {
      ...byListId.value,
      [listId]: {
        ...cache,
        membership: nextMembership,
        groups: cache.groups.filter(g => g.id !== groupId)
      }
    }
    try {
      await deleteListGroupApi(groupId)
    } catch (e) {
      byListId.value = { ...byListId.value, [listId]: prev }
      throw e
    }
  }

  async function moveTask(taskId: string, groupId: string, beforeId?: string | null) {
    const listId = activeListId.value
    if (!listId) {
      throw new Error('TASK_LIST_GROUP_FORBIDDEN')
    }
    const cache = ensureCache(listId)
    if (!cache.groups.some(g => g.id === groupId)) {
      throw new Error('TASK_LIST_GROUP_NOT_FOUND')
    }
    const prev = cache.membership[taskId]
    byListId.value = {
      ...byListId.value,
      [listId]: {
        ...cache,
        membership: { ...cache.membership, [taskId]: groupId }
      }
    }
    try {
      await moveListGroupTask({ listId, taskId, groupId, beforeId })
    } catch (e) {
      const rolled = { ...ensureCache(listId).membership }
      if (prev) {
        rolled[taskId] = prev
      } else {
        delete rolled[taskId]
      }
      byListId.value = {
        ...byListId.value,
        [listId]: { ...ensureCache(listId), membership: rolled }
      }
      throw e
    }
  }

  function ensureTaskInDefault(taskId: string) {
    const fallback = defaultGroupId.value
    if (!fallback) {
      return
    }
    mutateActive(c => ({
      ...c,
      membership: { ...c.membership, [taskId]: fallback }
    }))
  }

  function resetForTenantSwitch() {
    byListId.value = {}
    activeListId.value = null
  }

  return {
    activeListId,
    groups,
    sortedGroups,
    defaultGroupId,
    loading,
    setActiveList,
    fetchList,
    partition,
    createGroup,
    deleteGroup,
    moveTask,
    ensureTaskInDefault,
    resetForTenantSwitch
  }
})

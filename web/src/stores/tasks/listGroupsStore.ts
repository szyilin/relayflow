import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { TaskItem } from '../../api/app/task'
import type { TaskGroupBucket } from './groupByLocal'

/** Temporary until list-groups-api / integrate. */
export const USE_LOCAL_LIST_GROUPS = true

export interface ListGroup {
  id: string
  name: string
  isDefault: boolean
  rank: number
}

interface ListGroupState {
  groups: ListGroup[]
  /** taskId → groupId */
  membership: Record<string, string>
  nextCustomId: number
}

function emptyState(): ListGroupState {
  return {
    groups: [{ id: 'list-default', name: '默认', isDefault: true, rank: 0 }],
    membership: {},
    nextCustomId: 1
  }
}

export const useListGroupsStore = defineStore('listGroups', () => {
  const byListId = ref<Record<string, ListGroupState>>({})
  const activeListId = ref<string | null>(null)

  function ensureState(listId: string): ListGroupState {
    if (!byListId.value[listId]) {
      byListId.value = {
        ...byListId.value,
        [listId]: emptyState()
      }
    }
    return byListId.value[listId]!
  }

  function setActiveList(listId: string | null) {
    activeListId.value = listId
    if (listId) {
      ensureState(listId)
    }
  }

  const activeState = computed(() => {
    const id = activeListId.value
    if (!id) {
      return emptyState()
    }
    return ensureState(id)
  })

  const groups = computed(() => activeState.value.groups)

  const sortedGroups = computed(() =>
    [...activeState.value.groups].sort((a, b) => a.rank - b.rank || a.id.localeCompare(b.id))
  )

  const defaultGroupId = computed(
    () => activeState.value.groups.find(g => g.isDefault)?.id ?? 'list-default'
  )

  function mutateActive(updater: (state: ListGroupState) => ListGroupState) {
    const id = activeListId.value
    if (!id) {
      return
    }
    const next = updater(ensureState(id))
    byListId.value = { ...byListId.value, [id]: next }
  }

  function ensureMembership(taskId: string) {
    const id = activeListId.value
    if (!id) {
      return
    }
    const state = ensureState(id)
    if (state.membership[taskId]) {
      return
    }
    mutateActive(s => ({
      ...s,
      membership: { ...s.membership, [taskId]: defaultGroupId.value }
    }))
  }

  function partition(items: TaskItem[]): TaskGroupBucket[] {
    for (const item of items) {
      ensureMembership(item.id)
    }
    const state = activeState.value
    return sortedGroups.value.map(g => ({
      key: g.id,
      label: g.name,
      items: items.filter(item => state.membership[item.id] === g.id)
    }))
  }

  function createGroup(name: string): ListGroup {
    const trimmed = name.trim()
    if (!trimmed) {
      throw new Error('TASK_LIST_GROUP_NAME_EMPTY')
    }
    const state = activeState.value
    const maxRank = state.groups.reduce((m, g) => Math.max(m, g.rank), 0)
    const group: ListGroup = {
      id: `list-custom-${state.nextCustomId}`,
      name: trimmed,
      isDefault: false,
      rank: maxRank + 1
    }
    mutateActive(s => ({
      ...s,
      groups: [...s.groups, group],
      nextCustomId: s.nextCustomId + 1
    }))
    return group
  }

  function deleteGroup(groupId: string) {
    const state = activeState.value
    const target = state.groups.find(g => g.id === groupId)
    if (!target) {
      throw new Error('TASK_LIST_GROUP_NOT_FOUND')
    }
    if (target.isDefault) {
      throw new Error('TASK_LIST_GROUP_FORBIDDEN')
    }
    const fallback = defaultGroupId.value
    mutateActive((s) => {
      const membership = { ...s.membership }
      for (const [taskId, gid] of Object.entries(membership)) {
        if (gid === groupId) {
          membership[taskId] = fallback
        }
      }
      return {
        ...s,
        membership,
        groups: s.groups.filter(g => g.id !== groupId)
      }
    })
  }

  function moveTask(taskId: string, groupId: string) {
    if (!activeState.value.groups.some(g => g.id === groupId)) {
      throw new Error('TASK_LIST_GROUP_NOT_FOUND')
    }
    mutateActive(s => ({
      ...s,
      membership: { ...s.membership, [taskId]: groupId }
    }))
  }

  function ensureTaskInDefault(taskId: string) {
    mutateActive(s => ({
      ...s,
      membership: { ...s.membership, [taskId]: defaultGroupId.value }
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
    setActiveList,
    partition,
    createGroup,
    deleteGroup,
    moveTask,
    ensureTaskInDefault,
    resetForTenantSwitch
  }
})

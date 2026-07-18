import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { TaskItem } from '../../api/app/task'
import type { TaskGroupBucket } from './groupByLocal'

/** Temporary until mine-groups-api / integrate. */
export const USE_LOCAL_MINE_GROUPS = true

export interface MineGroup {
  id: string
  name: string
  isDefault: boolean
  rank: number
}

const DEFAULT_GROUP_ID = 'mine-default'
let nextCustomId = 1

export const useMineGroupsStore = defineStore('mineGroups', () => {
  const groups = ref<MineGroup[]>([
    { id: DEFAULT_GROUP_ID, name: '默认', isDefault: true, rank: 0 }
  ])
  /** taskId → groupId */
  const membership = ref<Record<string, string>>({})

  const sortedGroups = computed(() =>
    [...groups.value].sort((a, b) => a.rank - b.rank || a.id.localeCompare(b.id))
  )

  const defaultGroupId = computed(
    () => groups.value.find(g => g.isDefault)?.id ?? DEFAULT_GROUP_ID
  )

  function ensureMembership(taskId: string) {
    if (!membership.value[taskId]) {
      membership.value = {
        ...membership.value,
        [taskId]: defaultGroupId.value
      }
    }
  }

  function partition(items: TaskItem[]): TaskGroupBucket[] {
    for (const item of items) {
      ensureMembership(item.id)
    }
    return sortedGroups.value.map(g => ({
      key: g.id,
      label: g.isDefault ? `${g.name}` : g.name,
      items: items.filter(item => membership.value[item.id] === g.id)
    }))
  }

  function createGroup(name: string): MineGroup {
    const trimmed = name.trim()
    if (!trimmed) {
      throw new Error('TASK_MINE_GROUP_NAME_EMPTY')
    }
    const maxRank = groups.value.reduce((m, g) => Math.max(m, g.rank), 0)
    const group: MineGroup = {
      id: `mine-custom-${nextCustomId++}`,
      name: trimmed,
      isDefault: false,
      rank: maxRank + 1
    }
    groups.value = [...groups.value, group]
    return group
  }

  function deleteGroup(groupId: string) {
    const target = groups.value.find(g => g.id === groupId)
    if (!target) {
      throw new Error('TASK_MINE_GROUP_NOT_FOUND')
    }
    if (target.isDefault) {
      throw new Error('TASK_MINE_GROUP_FORBIDDEN')
    }
    const fallback = defaultGroupId.value
    const nextMembership = { ...membership.value }
    for (const [taskId, gid] of Object.entries(nextMembership)) {
      if (gid === groupId) {
        nextMembership[taskId] = fallback
      }
    }
    membership.value = nextMembership
    groups.value = groups.value.filter(g => g.id !== groupId)
  }

  function moveTask(taskId: string, groupId: string) {
    if (!groups.value.some(g => g.id === groupId)) {
      throw new Error('TASK_MINE_GROUP_NOT_FOUND')
    }
    membership.value = {
      ...membership.value,
      [taskId]: groupId
    }
  }

  function ensureTaskInDefault(taskId: string) {
    membership.value = {
      ...membership.value,
      [taskId]: defaultGroupId.value
    }
  }

  function resetLocal() {
    groups.value = [
      { id: DEFAULT_GROUP_ID, name: '默认', isDefault: true, rank: 0 }
    ]
    membership.value = {}
    nextCustomId = 1
  }

  return {
    groups,
    sortedGroups,
    defaultGroupId,
    membership,
    partition,
    createGroup,
    deleteGroup,
    moveTask,
    ensureTaskInDefault,
    resetLocal
  }
})

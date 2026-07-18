import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { TaskItem } from '../../api/app/task'
import {
  createMineGroup as createMineGroupApi,
  deleteMineGroup as deleteMineGroupApi,
  listMineGroups,
  moveMineGroupTask,
  type MineGroup
} from '../../api/app/taskMineGroup'
import type { TaskGroupBucket } from './groupByLocal'

export type { MineGroup }

export const useMineGroupsStore = defineStore('mineGroups', () => {
  const groups = ref<MineGroup[]>([])
  /** taskId → groupId */
  const membership = ref<Record<string, string>>({})
  const loading = ref(false)
  const loaded = ref(false)

  const sortedGroups = computed(() =>
    [...groups.value].sort((a, b) => a.rank - b.rank || a.id.localeCompare(b.id))
  )

  const defaultGroupId = computed(
    () => groups.value.find(g => g.isDefault)?.id ?? ''
  )

  function applyList(result: Awaited<ReturnType<typeof listMineGroups>>) {
    groups.value = result.groups
    const next: Record<string, string> = {}
    for (const m of result.memberships) {
      next[m.taskId] = m.groupId
    }
    membership.value = next
    loaded.value = true
  }

  async function fetchList(force = false) {
    if (loading.value) {
      return
    }
    if (loaded.value && !force) {
      return
    }
    loading.value = true
    try {
      applyList(await listMineGroups())
    } finally {
      loading.value = false
    }
  }

  function ensureMembership(taskId: string) {
    if (membership.value[taskId]) {
      return
    }
    const fallback = defaultGroupId.value
    if (!fallback) {
      return
    }
    membership.value = {
      ...membership.value,
      [taskId]: fallback
    }
  }

  function partition(items: TaskItem[]): TaskGroupBucket[] {
    for (const item of items) {
      ensureMembership(item.id)
    }
    const buckets = sortedGroups.value.map(g => ({
      key: g.id,
      label: g.name,
      items: items.filter(item => membership.value[item.id] === g.id)
    }))
    if (!buckets.length) {
      return [{ key: '__all__', label: '自定义分组', items: [...items] }]
    }
    return buckets
  }

  async function createGroup(name: string): Promise<MineGroup> {
    const trimmed = name.trim()
    if (!trimmed) {
      throw new Error('TASK_MINE_GROUP_NAME_EMPTY')
    }
    const group = await createMineGroupApi(trimmed)
    groups.value = [...groups.value, group]
    return group
  }

  async function deleteGroup(groupId: string) {
    const target = groups.value.find(g => g.id === groupId)
    if (!target) {
      throw new Error('TASK_MINE_GROUP_NOT_FOUND')
    }
    if (target.isDefault) {
      throw new Error('TASK_MINE_GROUP_FORBIDDEN')
    }
    const prevGroups = groups.value
    const prevMembership = membership.value
    const fallback = defaultGroupId.value
    const nextMembership = { ...membership.value }
    for (const [taskId, gid] of Object.entries(nextMembership)) {
      if (gid === groupId) {
        nextMembership[taskId] = fallback
      }
    }
    membership.value = nextMembership
    groups.value = groups.value.filter(g => g.id !== groupId)
    try {
      await deleteMineGroupApi(groupId)
    } catch (e) {
      groups.value = prevGroups
      membership.value = prevMembership
      throw e
    }
  }

  async function moveTask(taskId: string, groupId: string, beforeId?: string | null) {
    if (!groups.value.some(g => g.id === groupId)) {
      throw new Error('TASK_MINE_GROUP_NOT_FOUND')
    }
    const prev = membership.value[taskId]
    membership.value = {
      ...membership.value,
      [taskId]: groupId
    }
    try {
      await moveMineGroupTask({ taskId, groupId, beforeId })
    } catch (e) {
      if (prev) {
        membership.value = { ...membership.value, [taskId]: prev }
      } else {
        const next = { ...membership.value }
        delete next[taskId]
        membership.value = next
      }
      throw e
    }
  }

  function ensureTaskInDefault(taskId: string) {
    const fallback = defaultGroupId.value
    if (!fallback) {
      return
    }
    membership.value = {
      ...membership.value,
      [taskId]: fallback
    }
  }

  function resetForTenantSwitch() {
    groups.value = []
    membership.value = {}
    loaded.value = false
  }

  return {
    groups,
    sortedGroups,
    defaultGroupId,
    membership,
    loading,
    loaded,
    fetchList,
    partition,
    createGroup,
    deleteGroup,
    moveTask,
    ensureTaskInDefault,
    resetForTenantSwitch
  }
})

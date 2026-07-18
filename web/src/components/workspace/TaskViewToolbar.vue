<script setup lang="ts">
import { computed } from 'vue'
import type { DropdownMenuItem } from '@nuxt/ui'
import type { TaskItemStatus } from '../../api/app/task'
import {
  useTaskViewConfigStore,
  type TaskViewGroupBy,
  type TaskViewSort,
  type TaskViewSortKey,
  type TaskViewDisplayMode
} from '../../stores/tasks/viewConfigStore'

const props = defineProps<{
  canUsePersonalCustomGroup?: boolean
  canUseListGroup?: boolean
  showBoardMode?: boolean
}>()

const viewConfig = useTaskViewConfigStore()

const sortLabel = computed(() => {
  const sort = viewConfig.activeConfig.sort
  if (sort === 'MANUAL') {
    return '拖拽自定义'
  }
  const names: Record<TaskViewSortKey, string> = {
    createTime: '创建时间',
    dueTime: '截止时间',
    title: '标题',
    status: '状态',
    updateTime: '更新时间'
  }
  const arrow = sort.order === 'ASC' ? '↑' : '↓'
  return `${names[sort.key]}${arrow}`
})

const groupLabel = computed(() => {
  const g = viewConfig.activeConfig.groupBy
  if (!g) {
    return '无分组'
  }
  if (g.mode === 'PERSONAL_CUSTOM') {
    return '自定义分组'
  }
  if (g.mode === 'LIST_GROUP') {
    return '清单分组'
  }
  const names = {
    status: '状态',
    dueTime: '截止时间',
    assigneeId: '负责人'
  } as const
  return names[g.fieldKey]
})

const filterCount = computed(() => viewConfig.activeConfig.filters.length)

const statusFilterValues = computed(() => {
  const clause = viewConfig.activeConfig.filters.find(f => f.field === 'status')
  return new Set(clause?.values ?? [])
})

const sortItems = computed<DropdownMenuItem[][]>(() => {
  const keys: { key: TaskViewSortKey | 'MANUAL', label: string }[] = [
    { key: 'MANUAL', label: '拖拽自定义' },
    { key: 'dueTime', label: '截止时间' },
    { key: 'createTime', label: '创建时间' },
    { key: 'title', label: '标题' },
    { key: 'status', label: '状态' }
  ]
  return [keys.map(item => ({
    label: item.label,
    onSelect: () => {
      if (item.key === 'MANUAL') {
        viewConfig.patchActiveConfig({ sort: 'MANUAL' })
        return
      }
      const current = viewConfig.activeConfig.sort
      let order: 'ASC' | 'DESC' = 'DESC'
      if (current !== 'MANUAL' && current.key === item.key) {
        order = current.order === 'DESC' ? 'ASC' : 'DESC'
      } else if (item.key === 'title') {
        order = 'ASC'
      }
      viewConfig.patchActiveConfig({
        sort: { key: item.key, order } satisfies TaskViewSort
      })
    }
  }))]
})

const groupItems = computed<DropdownMenuItem[][]>(() => {
  const items: DropdownMenuItem[] = [
    {
      label: '无分组',
      onSelect: () => viewConfig.patchActiveConfig({ groupBy: null })
    },
    {
      label: '状态',
      onSelect: () =>
        viewConfig.patchActiveConfig({
          groupBy: { mode: 'FIELD', fieldKey: 'status' }
        })
    },
    {
      label: '截止时间',
      onSelect: () =>
        viewConfig.patchActiveConfig({
          groupBy: { mode: 'FIELD', fieldKey: 'dueTime' }
        })
    },
    {
      label: '负责人',
      onSelect: () =>
        viewConfig.patchActiveConfig({
          groupBy: { mode: 'FIELD', fieldKey: 'assigneeId' }
        })
    }
  ]
  if (props.canUsePersonalCustomGroup) {
    items.push({
      label: '自定义分组',
      onSelect: () =>
        viewConfig.patchActiveConfig({
          groupBy: { mode: 'PERSONAL_CUSTOM' } satisfies TaskViewGroupBy
        })
    })
  }
  if (props.canUseListGroup) {
    items.push({
      label: '清单分组',
      onSelect: () =>
        viewConfig.patchActiveConfig({
          groupBy: { mode: 'LIST_GROUP' } satisfies TaskViewGroupBy
        })
    })
  }
  return [items]
})

const fieldItems = computed<DropdownMenuItem[][]>(() => {
  const fields = [
    { key: 'dueTime', label: '截止时间' },
    { key: 'assignee', label: '负责人' },
    { key: 'status', label: '状态' }
  ]
  return [fields.map(f => ({
    label: `${viewConfig.isFieldVisible(f.key) ? '✓ ' : ''}${f.label}`,
    onSelect: () => {
      const current = new Set(viewConfig.activeConfig.visibleFieldKeys)
      if (current.has(f.key)) {
        current.delete(f.key)
      } else {
        current.add(f.key)
      }
      viewConfig.patchActiveConfig({ visibleFieldKeys: Array.from(current) })
    }
  }))]
})

function setDisplayMode(mode: TaskViewDisplayMode) {
  viewConfig.patchActiveConfig({ displayMode: mode })
}

function setStatusFilter(status: TaskItemStatus, enabled: boolean) {
  const current = new Set(statusFilterValues.value)
  if (enabled) {
    current.add(status)
  } else {
    current.delete(status)
  }
  const values = Array.from(current) as TaskItemStatus[]
  const filters = viewConfig.activeConfig.filters.filter(f => f.field !== 'status')
  if (values.length) {
    filters.push({ field: 'status', op: 'IN', values })
  }
  viewConfig.patchActiveConfig({ filters })
}

function clearFilters() {
  viewConfig.patchActiveConfig({ filters: [] })
}
</script>

<template>
  <div class="flex flex-wrap items-center gap-2 border-b border-[var(--ws-border-subtle)] px-5 py-2">
    <div
      v-if="showBoardMode"
      class="mr-1 flex rounded-md border border-[var(--ws-border-subtle)] p-0.5"
    >
      <button
        type="button"
        class="rounded px-2 py-1 text-xs"
        :class="viewConfig.activeConfig.displayMode === 'LIST'
          ? 'bg-primary/10 font-medium text-primary'
          : 'text-[var(--ws-text-muted)]'"
        @click="setDisplayMode('LIST')"
      >
        列表
      </button>
      <button
        type="button"
        class="rounded px-2 py-1 text-xs"
        :class="viewConfig.activeConfig.displayMode === 'BOARD'
          ? 'bg-primary/10 font-medium text-primary'
          : 'text-[var(--ws-text-muted)]'"
        @click="setDisplayMode('BOARD')"
      >
        看板
      </button>
    </div>

    <UPopover>
      <UButton
        color="neutral"
        variant="ghost"
        size="xs"
        icon="i-lucide-filter"
        :label="filterCount ? `筛选 ${filterCount}` : '筛选'"
        :class="filterCount ? 'text-primary' : ''"
      />
      <template #content>
        <div class="w-56 space-y-2 p-3">
          <div class="flex items-center justify-between">
            <span class="text-xs font-medium text-[var(--ws-text-muted)]">状态</span>
            <UButton
              color="neutral"
              variant="link"
              size="xs"
              label="清空"
              @click="clearFilters"
            />
          </div>
          <label
            v-for="s in (['TODO', 'IN_PROGRESS', 'DONE'] as TaskItemStatus[])"
            :key="s"
            class="flex cursor-pointer items-center gap-2 text-sm"
          >
            <UCheckbox
              :model-value="statusFilterValues.has(s)"
              @update:model-value="(v: boolean | 'indeterminate') => setStatusFilter(s, v === true)"
            />
            <span>{{ s === 'TODO' ? '未开始' : s === 'IN_PROGRESS' ? '进行中' : '已完成' }}</span>
          </label>
          <p class="text-[11px] text-[var(--ws-text-muted)]">
            叠在当前入口默认条件之上（本地暂存）
          </p>
        </div>
      </template>
    </UPopover>

    <UDropdownMenu :items="sortItems">
      <UButton
        color="neutral"
        variant="ghost"
        size="xs"
        icon="i-lucide-arrow-up-down"
        :label="`排序：${sortLabel}`"
      />
    </UDropdownMenu>

    <UDropdownMenu :items="groupItems">
      <UButton
        color="neutral"
        variant="ghost"
        size="xs"
        icon="i-lucide-layers"
        :label="`分组：${groupLabel}`"
      />
    </UDropdownMenu>

    <UDropdownMenu :items="fieldItems">
      <UButton
        color="neutral"
        variant="ghost"
        size="xs"
        icon="i-lucide-columns-3"
        label="字段配置"
      />
    </UDropdownMenu>

    <UButton
      color="neutral"
      variant="ghost"
      size="xs"
      icon="i-lucide-rotate-ccw"
      label="重置"
      class="ml-auto"
      @click="viewConfig.resetActiveToDefault()"
    />
  </div>
</template>

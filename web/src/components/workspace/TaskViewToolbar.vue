<script setup lang="ts">
import { computed, onMounted } from 'vue'
import type { DropdownMenuItem } from '@nuxt/ui'
import type { TaskItemStatus } from '../../api/app/task'
import { useAuthStore } from '../../stores/auth'
import { useContactsStore } from '../../stores/contacts'
import {
  useTaskViewConfigStore,
  type TaskViewGroupBy,
  type TaskViewSort,
  type TaskViewSortKey,
  type TaskViewDisplayMode,
  type TaskViewFieldKey,
  type TaskViewFilterClause,
  type TaskViewFilterField,
  type TaskViewFilterOp
} from '../../stores/tasks/viewConfigStore'
import { isCustomFieldKey } from '../../stores/tasks/customFieldsLocal'
import {
  FILTER_FIELDS,
  FILTER_ME,
  defaultOpForField,
  opNeedsValue,
  opsForField
} from '../../stores/tasks/viewConfigFilters'
import WsDateTimeInput from './WsDateTimeInput.vue'

const props = defineProps<{
  canUsePersonalCustomGroup?: boolean
  canUseListGroup?: boolean
  showBoardMode?: boolean
  /** List-scoped custom single-select fields for groupBy menu. */
  customFieldGroupItems?: { fieldKey: string, label: string }[]
}>()

const viewConfig = useTaskViewConfigStore()
const auth = useAuthStore()
const contacts = useContactsStore()

onMounted(async () => {
  try {
    await contacts.fetchDepts()
    const rootId = contacts.rootDeptId()
    if (rootId) {
      await contacts.fetchMembers({ deptId: rootId })
    }
  } catch {
    // member picker optional
  }
})

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
  if (isCustomFieldKey(g.fieldKey)) {
    const hit = props.customFieldGroupItems?.find(i => i.fieldKey === g.fieldKey)
    return hit?.label ?? '自定义字段'
  }
  const names = {
    status: '状态',
    dueTime: '截止时间',
    assigneeId: '负责人'
  } as const
  return names[g.fieldKey as keyof typeof names] ?? g.fieldKey
})

const filters = computed(() => viewConfig.activeConfig.filters)
const filterCount = computed(() => filters.value.length)

const personOptions = computed(() => {
  const opts: { value: string, label: string }[] = [
    { value: FILTER_ME, label: '我' }
  ]
  const selfId = auth.userId ? String(auth.userId) : ''
  for (const m of contacts.members) {
    if (selfId && m.id === selfId) {
      continue
    }
    opts.push({ value: m.id, label: m.nickname || m.username || m.id })
  }
  return opts
})

const statusOptions: { value: TaskItemStatus, label: string }[] = [
  { value: 'TODO', label: '未完成' },
  { value: 'DONE', label: '已完成' }
]

const fieldSelectItems = FILTER_FIELDS.map(f => ({
  label: f.label,
  value: f.field
}))

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
  for (const cf of props.customFieldGroupItems ?? []) {
    items.push({
      label: cf.label,
      onSelect: () =>
        viewConfig.patchActiveConfig({
          groupBy: {
            mode: 'FIELD',
            fieldKey: cf.fieldKey as TaskViewFieldKey
          }
        })
    })
  }
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

function commitFilters(next: TaskViewFilterClause[]) {
  viewConfig.patchActiveConfig({ filters: next })
}

function updateClause(index: number, patch: Partial<TaskViewFilterClause>) {
  const next = filters.value.map((c, i) => (i === index ? { ...c, ...patch } : c))
  commitFilters(next)
}

function setClauseField(index: number, field: TaskViewFilterField) {
  updateClause(index, {
    field,
    op: defaultOpForField(field),
    values: field === 'status' ? [] : field === 'title' ? [''] : []
  })
}

function setClauseOp(index: number, op: TaskViewFilterOp) {
  const clause = filters.value[index]
  if (!clause) {
    return
  }
  updateClause(index, {
    op,
    values: opNeedsValue(op) ? clause.values : []
  })
}

function setPersonValue(index: number, value: string | undefined) {
  updateClause(index, { values: value ? [value] : [] })
}

function setTextValue(index: number, value: string) {
  updateClause(index, { values: [value] })
}

function setDateValue(index: number, value: string) {
  updateClause(index, { values: value ? [value] : [] })
}

function toggleStatusValue(index: number, status: TaskItemStatus, enabled: boolean) {
  const clause = filters.value[index]
  if (!clause) {
    return
  }
  const set = new Set(clause.values)
  if (enabled) {
    set.add(status)
  } else {
    set.delete(status)
  }
  updateClause(index, { values: Array.from(set) })
}

function setStatusEq(index: number, status: string | undefined) {
  updateClause(index, { values: status ? [status] : [] })
}

function addFilter() {
  commitFilters([
    ...filters.value,
    { field: 'status', op: 'IN', values: [] }
  ])
}

function removeFilter(index: number) {
  commitFilters(filters.value.filter((_, i) => i !== index))
}

function clearFilters() {
  commitFilters([])
}

function isPersonField(field: TaskViewFilterField) {
  return field === 'assigneeId' || field === 'creatorId' || field === 'assignerId'
}

function isTimeField(field: TaskViewFilterField) {
  return field === 'dueTime' || field === 'createTime'
}

function personLabel(value: string | undefined) {
  if (!value) {
    return '选择成员'
  }
  if (value === FILTER_ME) {
    return '我'
  }
  const hit = personOptions.value.find(o => o.value === value)
  return hit?.label ?? value
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

    <UPopover :ui="{ content: 'p-0' }">
      <UButton
        color="neutral"
        variant="ghost"
        size="xs"
        icon="i-lucide-filter"
        :label="filterCount ? `筛选 ${filterCount}` : '筛选'"
        :class="filterCount ? 'text-primary' : ''"
      />
      <template #content>
        <div class="w-[min(36rem,calc(100vw-2rem))] space-y-3 p-3">
          <div class="flex items-center justify-between gap-2">
            <span class="text-xs font-medium text-[var(--ws-text-muted)]">筛选条件</span>
            <UButton
              color="neutral"
              variant="link"
              size="xs"
              label="清空"
              :disabled="!filterCount"
              @click="clearFilters"
            />
          </div>

          <div
            v-if="!filters.length"
            class="rounded-md border border-dashed border-[var(--ws-border-subtle)] px-3 py-4 text-center text-xs text-[var(--ws-text-muted)]"
          >
            暂无筛选条件，可添加或点工具栏「重置」恢复入口默认
          </div>

          <div
            v-for="(clause, index) in filters"
            :key="index"
            class="flex flex-wrap items-start gap-2"
          >
            <span class="mt-1.5 w-6 shrink-0 text-xs text-[var(--ws-text-muted)]">
              {{ index === 0 ? '当' : '且' }}
            </span>

            <USelect
              :model-value="clause.field"
              :items="fieldSelectItems"
              value-key="value"
              label-key="label"
              size="xs"
              class="w-28"
              @update:model-value="(v: string) => setClauseField(index, v as TaskViewFilterField)"
            />

            <USelect
              :model-value="clause.op"
              :items="opsForField(clause.field).map(o => ({ label: o.label, value: o.op }))"
              value-key="value"
              label-key="label"
              size="xs"
              class="w-28"
              @update:model-value="(v: string) => setClauseOp(index, v as TaskViewFilterOp)"
            />

            <div
              v-if="opNeedsValue(clause.op)"
              class="min-w-0 flex-1"
            >
              <div
                v-if="clause.field === 'status' && clause.op === 'IN'"
                class="flex flex-wrap gap-2"
              >
                <label
                  v-for="s in statusOptions"
                  :key="s.value"
                  class="flex cursor-pointer items-center gap-1.5 text-xs"
                >
                  <UCheckbox
                    :model-value="clause.values.includes(s.value)"
                    @update:model-value="(v: boolean | 'indeterminate') => toggleStatusValue(index, s.value, v === true)"
                  />
                  <span>{{ s.label }}</span>
                </label>
              </div>

              <USelect
                v-else-if="clause.field === 'status'"
                :model-value="(clause.values[0] as TaskItemStatus | undefined)"
                :items="statusOptions.map(s => ({ label: s.label, value: s.value }))"
                value-key="value"
                label-key="label"
                size="xs"
                class="w-full min-w-28"
                placeholder="状态"
                @update:model-value="(v: string) => setStatusEq(index, v)"
              />

              <USelectMenu
                v-else-if="isPersonField(clause.field)"
                :model-value="clause.values[0]"
                :items="personOptions"
                value-key="value"
                label-key="label"
                size="xs"
                class="w-full min-w-36"
                :placeholder="personLabel(clause.values[0])"
                searchable
                @update:model-value="(v: string | undefined) => setPersonValue(index, v)"
              />

              <WsDateTimeInput
                v-else-if="isTimeField(clause.field)"
                type="date"
                size="xs"
                class="w-full min-w-36"
                :model-value="(clause.values[0] || '').slice(0, 10)"
                @update:model-value="(v: string) => setDateValue(index, String(v || ''))"
              />

              <UInput
                v-else
                size="xs"
                class="w-full min-w-36"
                placeholder="输入内容"
                :model-value="clause.values[0] || ''"
                @update:model-value="(v: string | number) => setTextValue(index, String(v ?? ''))"
              />
            </div>

            <UButton
              color="neutral"
              variant="ghost"
              size="xs"
              icon="i-lucide-x"
              class="shrink-0"
              @click="removeFilter(index)"
            />
          </div>

          <UButton
            color="neutral"
            variant="soft"
            size="xs"
            icon="i-lucide-plus"
            label="添加"
            @click="addFilter"
          />
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

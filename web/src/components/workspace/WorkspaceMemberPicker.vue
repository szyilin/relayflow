<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { useContactsStore } from '../../stores/contacts'
import { useMemberPickerStore } from '../../stores/memberPicker'
import WorkspaceAvatar from './WorkspaceAvatar.vue'
import WorkspaceSemanticBadge from './WorkspaceSemanticBadge.vue'

export interface PickerMember {
  userId: string
  displayName: string
  avatarText: string
  subtitle?: string
  external?: boolean
}

type PickerView = 'sources' | 'org' | 'external' | 'managed' | 'managedMembers'

const props = withDefaults(defineProps<{
  modelValue: PickerMember[]
  /** Already-in-group / self etc. — hidden from selectable lists */
  excludeUserIds?: string[]
}>(), {
  excludeUserIds: () => []
})

const emit = defineEmits<{
  'update:modelValue': [value: PickerMember[]]
}>()

const auth = useAuthStore()
const contacts = useContactsStore()
const memberPicker = useMemberPickerStore()

const view = ref<PickerView>('sources')
const keyword = ref('')
const loading = ref(false)
const errorMessage = ref<string | null>(null)
const activeManagedGroupId = ref<string | null>(null)
/** Avoid re-showing blocking skeleton after first successful load of an empty list */
const externalHydrated = ref(false)

const excludeSet = computed(() => {
  const set = new Set(props.excludeUserIds)
  if (auth.userId) {
    set.add(auth.userId)
  }
  return set
})

const selectedMap = computed(() =>
  new Map(props.modelValue.map(item => [item.userId, item])))

const breadcrumbLabel = computed(() => {
  if (view.value === 'sources') {
    return '联系人'
  }
  if (view.value === 'org') {
    const names = contacts.breadcrumb.map(d => d.name)
    return ['联系人', '组织内联系人', ...names.slice(1)].join(' > ')
  }
  if (view.value === 'external') {
    return '联系人 > 外部联系人'
  }
  if (view.value === 'managed') {
    return '联系人 > 我管理的群组'
  }
  const group = memberPicker.managedGroups.find(g => g.id === activeManagedGroupId.value)
  return `联系人 > 我管理的群组 > ${group?.title ?? '群组'}`
})

const sourceEntries = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  const all = [
    { key: 'org' as const, label: '组织内联系人', icon: 'i-lucide-building-2' },
    { key: 'external' as const, label: '外部联系人', icon: 'i-lucide-globe' },
    { key: 'managed' as const, label: '我管理的群组', icon: 'i-lucide-users' }
  ]
  if (!q) {
    return all
  }
  return all.filter(item => item.label.toLowerCase().includes(q))
})

const orgRows = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  const depts = contacts.childDepts
  const members = contacts.members.filter((m) => {
    if (excludeSet.value.has(m.id)) {
      return false
    }
    if (!q) {
      return true
    }
    return m.nickname.toLowerCase().includes(q)
      || m.username.toLowerCase().includes(q)
  })
  return { depts, members }
})

const externalRows = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return contacts.externalContacts.filter((item) => {
    if (excludeSet.value.has(item.userId)) {
      return false
    }
    if (!q) {
      return true
    }
    return item.displayName.toLowerCase().includes(q)
      || item.orgLabel.toLowerCase().includes(q)
  })
})

const managedRows = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return memberPicker.managedGroups.filter(g =>
    !q || g.title.toLowerCase().includes(q))
})

const managedMemberRows = computed(() => {
  const group = memberPicker.managedGroups.find(g => g.id === activeManagedGroupId.value)
  if (!group) {
    return []
  }
  const q = keyword.value.trim().toLowerCase()
  return group.members.filter((m) => {
    if (excludeSet.value.has(m.userId)) {
      return false
    }
    if (!q) {
      return true
    }
    return m.displayName.toLowerCase().includes(q)
  })
})

/** Soft in-view loading — never blank the whole pane with skeletons on tag click */
const showOrgLoading = computed(() =>
  loading.value
  && contacts.depts.length === 0
  && contacts.members.length === 0)

const showExternalLoading = computed(() =>
  loading.value
  && !externalHydrated.value
  && contacts.externalContacts.length === 0)

const showManagedLoading = computed(() =>
  (loading.value || memberPicker.loadingManaged)
  && !memberPicker.loadedOnce
  && memberPicker.managedGroups.length === 0)

watch(view, async (next) => {
  errorMessage.value = null
  if (next === 'org') {
    const cold = contacts.depts.length === 0 && contacts.members.length === 0
    if (cold) {
      loading.value = true
    }
    try {
      await contacts.fetchDepts()
      const rootId = contacts.rootDeptId()
      if (rootId && contacts.selectedDeptId !== rootId) {
        await contacts.fetchMembers({ deptId: rootId, keyword: '' })
      } else if (rootId && contacts.members.length === 0) {
        await contacts.fetchMembers({ deptId: rootId, keyword: '' })
      }
    } catch {
      errorMessage.value = contacts.lastError ?? '加载组织失败'
    } finally {
      loading.value = false
    }
  }
  if (next === 'external') {
    const cold = !externalHydrated.value && contacts.externalContacts.length === 0
    if (cold) {
      loading.value = true
    }
    try {
      await contacts.fetchExternal({ silent: true })
      externalHydrated.value = true
    } catch {
      errorMessage.value = contacts.lastError ?? '加载外部联系人失败'
    } finally {
      loading.value = false
    }
  }
  if (next === 'managed') {
    const cold = !memberPicker.loadedOnce && memberPicker.managedGroups.length === 0
    if (cold) {
      loading.value = true
    }
    try {
      await memberPicker.loadManagedGroups()
    } catch {
      errorMessage.value = memberPicker.lastError ?? '加载我管理的群组失败'
    } finally {
      loading.value = false
    }
  }
})

function isSelected(userId: string) {
  return selectedMap.value.has(userId)
}

function setSelected(next: PickerMember[]) {
  emit('update:modelValue', next)
}

function toggleMember(member: PickerMember, checked: boolean) {
  if (excludeSet.value.has(member.userId)) {
    return
  }
  if (checked) {
    if (isSelected(member.userId)) {
      return
    }
    setSelected([...props.modelValue, member])
    return
  }
  setSelected(props.modelValue.filter(item => item.userId !== member.userId))
}

function selectableMembersOf(groupId: string) {
  const group = memberPicker.managedGroups.find(g => g.id === groupId)
  if (!group) {
    return [] as PickerMember[]
  }
  return group.members
    .filter(m => !excludeSet.value.has(m.userId))
    .map(m => ({
      userId: m.userId,
      displayName: m.displayName,
      avatarText: m.avatarText,
      subtitle: group.title,
      external: false
    }))
}

function toggleManagedGroup(groupId: string, checked: boolean) {
  const mapped = selectableMembersOf(groupId)
  if (checked) {
    const merged = [...props.modelValue]
    for (const m of mapped) {
      if (!merged.some(x => x.userId === m.userId)) {
        merged.push(m)
      }
    }
    setSelected(merged)
    return
  }
  const removeIds = new Set(mapped.map(m => m.userId))
  setSelected(props.modelValue.filter(item => !removeIds.has(item.userId)))
}

function groupFullySelected(groupId: string) {
  const mapped = selectableMembersOf(groupId)
  if (!mapped.length) {
    return false
  }
  return mapped.every(m => isSelected(m.userId))
}

async function enterDept(deptId: string) {
  // Keep current org rows visible while drilling down — avoid pane flash
  try {
    await contacts.enterDept(deptId)
  } catch {
    errorMessage.value = contacts.lastError ?? '进入部门失败'
  }
}

function goSources() {
  view.value = 'sources'
  keyword.value = ''
  activeManagedGroupId.value = null
}

function openManagedMembers(groupId: string) {
  activeManagedGroupId.value = groupId
  view.value = 'managedMembers'
  keyword.value = ''
}

function removeSelected(userId: string) {
  setSelected(props.modelValue.filter(item => item.userId !== userId))
}

function openSource(key: 'org' | 'external' | 'managed') {
  view.value = key
}

defineExpose({
  reset() {
    goSources()
    errorMessage.value = null
    loading.value = false
  }
})
</script>

<template>
  <div class="grid min-h-[280px] grid-cols-1 gap-3 md:grid-cols-2">
    <div class="flex min-h-0 flex-col rounded-lg border border-[var(--ws-border-subtle)]">
      <div class="border-b border-[var(--ws-border-subtle)] px-3 py-2">
        <UInput
          v-model="keyword"
          size="sm"
          icon="i-lucide-search"
          placeholder="搜索联系人、部门和我管理的群组"
        />
        <p class="mt-2 truncate text-xs text-[var(--ws-text-muted)]">
          {{ breadcrumbLabel }}
        </p>
      </div>

      <div class="min-h-0 flex-1 overflow-y-auto p-2">
        <p
          v-if="errorMessage"
          class="mb-2 px-2 text-sm text-error"
        >
          {{ errorMessage }}
        </p>

        <template v-if="view === 'sources'">
          <button
            v-for="item in sourceEntries"
            :key="item.key"
            type="button"
            class="workspace-list-item flex w-full items-center gap-3 px-3 py-2.5 text-left"
            @click="openSource(item.key)"
          >
            <UIcon :name="item.icon" class="size-4 text-[var(--ws-text-muted)]" />
            <span class="text-sm font-medium">{{ item.label }}</span>
            <UIcon name="i-lucide-chevron-right" class="ml-auto size-4 opacity-50" />
          </button>
          <UEmpty
            v-if="!sourceEntries.length"
            icon="i-lucide-search"
            title="无匹配入口"
            class="py-8"
          />
        </template>

        <template v-else-if="view === 'org'">
          <button
            type="button"
            class="mb-1 px-2 text-xs text-primary"
            @click="goSources"
          >
            ← 返回
          </button>
          <div
            v-if="showOrgLoading"
            class="flex items-center justify-center py-10"
          >
            <UIcon
              name="i-lucide-loader-circle"
              class="size-5 animate-spin text-[var(--ws-text-muted)]"
            />
          </div>
          <template v-else>
            <button
              v-for="dept in orgRows.depts"
              :key="dept.id"
              type="button"
              class="workspace-list-item flex w-full items-center gap-3 px-3 py-2 text-left"
              @click="enterDept(dept.id)"
            >
              <UIcon name="i-lucide-folder-tree" class="size-4 text-primary" />
              <span class="min-w-0 flex-1 truncate text-sm">{{ dept.name }}</span>
              <span class="text-xs text-primary">下级</span>
            </button>
            <label
              v-for="member in orgRows.members"
              :key="member.id"
              class="flex cursor-pointer items-center gap-3 rounded-md px-3 py-2 hover:bg-[var(--ws-rail-hover)]"
            >
              <UCheckbox
                :model-value="isSelected(member.id)"
                @update:model-value="toggleMember({
                  userId: member.id,
                  displayName: member.nickname,
                  avatarText: member.avatarText,
                  subtitle: member.deptName
                }, $event === true)"
              />
              <WorkspaceAvatar :text="member.avatarText" :seed="member.id" size="sm" />
              <span class="truncate text-sm">{{ member.nickname }}</span>
            </label>
            <UEmpty
              v-if="!orgRows.depts.length && !orgRows.members.length"
              icon="i-lucide-users"
              title="此部门暂无成员"
              class="py-8"
            />
          </template>
        </template>

        <template v-else-if="view === 'external'">
          <button
            type="button"
            class="mb-1 px-2 text-xs text-primary"
            @click="goSources"
          >
            ← 返回
          </button>
          <div
            v-if="showExternalLoading"
            class="flex items-center justify-center py-10"
          >
            <UIcon
              name="i-lucide-loader-circle"
              class="size-5 animate-spin text-[var(--ws-text-muted)]"
            />
          </div>
          <template v-else>
            <label
              v-for="person in externalRows"
              :key="person.id"
              class="flex cursor-pointer items-center gap-3 rounded-md px-3 py-2 hover:bg-[var(--ws-rail-hover)]"
            >
              <UCheckbox
                :model-value="isSelected(person.userId)"
                @update:model-value="toggleMember({
                  userId: person.userId,
                  displayName: person.displayName,
                  avatarText: person.avatarText,
                  subtitle: person.orgLabel,
                  external: true
                }, $event === true)"
              />
              <WorkspaceAvatar :text="person.avatarText" :seed="person.userId" size="sm" />
              <span class="truncate text-sm">{{ person.displayName }}</span>
              <WorkspaceSemanticBadge kind="external" size="sm">外部</WorkspaceSemanticBadge>
            </label>
            <UEmpty
              v-if="!externalRows.length"
              icon="i-lucide-globe"
              title="暂无外部联系人"
              description="添加外部联系人后可在此选择"
              class="py-8"
            />
          </template>
        </template>

        <template v-else-if="view === 'managed'">
          <button
            type="button"
            class="mb-1 px-2 text-xs text-primary"
            @click="goSources"
          >
            ← 返回
          </button>
          <div
            v-if="showManagedLoading"
            class="flex items-center justify-center py-10"
          >
            <UIcon
              name="i-lucide-loader-circle"
              class="size-5 animate-spin text-[var(--ws-text-muted)]"
            />
          </div>
          <template v-else>
            <div
              v-for="group in managedRows"
              :key="group.id"
              class="flex items-center gap-2 rounded-md px-2 py-2 hover:bg-[var(--ws-rail-hover)]"
            >
              <UCheckbox
                :model-value="groupFullySelected(group.id)"
                :disabled="!selectableMembersOf(group.id).length"
                @update:model-value="toggleManagedGroup(group.id, $event === true)"
              />
              <button
                type="button"
                class="flex min-w-0 flex-1 items-center gap-3 text-left"
                @click="openManagedMembers(group.id)"
              >
                <WorkspaceAvatar
                  :text="group.title.slice(0, 1)"
                  :seed="group.id"
                  icon="i-lucide-users"
                  size="sm"
                />
                <span class="truncate text-sm">
                  {{ group.title }}
                  <span class="text-[var(--ws-text-muted)]">({{ group.memberCount }})</span>
                </span>
                <UIcon name="i-lucide-chevron-right" class="ml-auto size-4 opacity-50" />
              </button>
            </div>
            <UEmpty
              v-if="!managedRows.length"
              icon="i-lucide-users"
              title="暂无我管理的群组"
              description="你担任群主或管理员的群会出现在这里"
              class="py-8"
            />
          </template>
        </template>

        <template v-else>
          <button
            type="button"
            class="mb-1 px-2 text-xs text-primary"
            @click="view = 'managed'"
          >
            ← 返回
          </button>
          <label
            v-for="member in managedMemberRows"
            :key="member.userId"
            class="flex cursor-pointer items-center gap-3 rounded-md px-3 py-2 hover:bg-[var(--ws-rail-hover)]"
          >
            <UCheckbox
              :model-value="isSelected(member.userId)"
              @update:model-value="toggleMember({
                userId: member.userId,
                displayName: member.displayName,
                avatarText: member.avatarText
              }, $event === true)"
            />
            <WorkspaceAvatar :text="member.avatarText" :seed="member.userId" size="sm" />
            <span class="truncate text-sm">{{ member.displayName }}</span>
          </label>
          <UEmpty
            v-if="!managedMemberRows.length"
            icon="i-lucide-users"
            title="无可选成员"
            class="py-8"
          />
        </template>
      </div>
    </div>

    <div class="flex min-h-0 flex-col rounded-lg border border-[var(--ws-border-subtle)]">
      <div class="border-b border-[var(--ws-border-subtle)] px-3 py-2 text-sm font-medium">
        已选：{{ modelValue.length }} 人
      </div>
      <div class="min-h-0 flex-1 overflow-y-auto p-2">
        <div
          v-if="modelValue.length"
          class="space-y-1"
        >
          <div
            v-for="person in modelValue"
            :key="person.userId"
            class="flex items-center gap-2 rounded-md px-2 py-1.5"
          >
            <WorkspaceAvatar :text="person.avatarText" :seed="person.userId" size="sm" />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm">
                {{ person.displayName }}
                <WorkspaceSemanticBadge
                  v-if="person.external"
                  kind="external"
                  size="sm"
                  class="ml-1"
                >
                  外部
                </WorkspaceSemanticBadge>
              </p>
            </div>
            <UButton
              size="xs"
              color="neutral"
              variant="ghost"
              icon="i-lucide-x"
              @click="removeSelected(person.userId)"
            />
          </div>
        </div>
        <p
          v-else
          class="px-2 py-8 text-center text-sm text-[var(--ws-text-muted)]"
        >
          从左侧选择成员
        </p>
      </div>
    </div>
  </div>
</template>

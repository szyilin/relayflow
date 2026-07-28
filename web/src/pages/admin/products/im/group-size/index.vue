<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AdminNavbar from '../../../../../components/admin/AdminNavbar.vue'
import AdminImProductShell from '../../../../../components/admin/products/AdminImProductShell.vue'
import { usePermission } from '../../../../../composables/usePermission'
import {
  GROUP_SIZE_PRESETS,
  cloneImProductSettings,
  searchTenantGroups,
  type GroupSizeGroupRule,
  type GroupSizeUserRule,
  type ImGroupSearchItem
} from '../../../../../api/admin/imProductSettings'
import { getUserPage } from '../../../../../api/admin/user'
import { useDeptStore } from '../../../../../stores/dept'
import { useImProductSettingsStore } from '../../../../../stores/imProductSettings'

interface SubjectOption {
  label: string
  value: string
  kind: 'DEPT' | 'USER'
}

interface LimitOption {
  label: string
  value: number
}

interface GroupOption {
  label: string
  value: string
  name: string
}

const toast = useToast()
const { hasPermission } = usePermission()
const settingsStore = useImProductSettingsStore()
const deptStore = useDeptStore()

const canQuery = computed(() => hasPermission('im:product-settings:query'))
const canUpdate = computed(() => hasPermission('im:product-settings:update'))

const form = reactive(cloneImProductSettings(settingsStore.settings))
const dirty = ref(false)

const presetItems: Array<{ label: string, value: number | 'CUSTOM' }> = [
  ...GROUP_SIZE_PRESETS.map(n => ({ label: `${n} 人`, value: n as number | 'CUSTOM' })),
  { label: '自定义', value: 'CUSTOM' as const }
]

const limitOptions: LimitOption[] = GROUP_SIZE_PRESETS.map(n => ({ label: `${n} 人`, value: n }))

const limitMode = ref<number | 'CUSTOM'>(50)
const customLimit = ref(50)

const deptItems = computed<SubjectOption[]>(() =>
  deptStore.list.map(d => ({ label: d.name, value: d.id, kind: 'DEPT' as const }))
)

const userOptions = ref<SubjectOption[]>([])
const groupOptions = ref<GroupOption[]>([])
const groupSearching = ref(false)

const selectedLimitMode = computed({
  get: () => presetItems.find(i => i.value === limitMode.value) ?? presetItems[3]!,
  set: (item: { label: string, value: number | 'CUSTOM' } | undefined) => {
    if (!item) {
      return
    }
    limitMode.value = item.value
    if (item.value !== 'CUSTOM') {
      form.groupMemberLimitDefault = item.value
      dirty.value = true
    }
  }
})

function applyFromStore() {
  const next = cloneImProductSettings(settingsStore.settings)
  Object.assign(form, next)
  if ((GROUP_SIZE_PRESETS as readonly number[]).includes(form.groupMemberLimitDefault)) {
    limitMode.value = form.groupMemberLimitDefault
    customLimit.value = form.groupMemberLimitDefault
  } else {
    limitMode.value = 'CUSTOM'
    customLimit.value = form.groupMemberLimitDefault
  }
  dirty.value = false
}

watch(customLimit, (n) => {
  if (limitMode.value === 'CUSTOM') {
    form.groupMemberLimitDefault = Math.max(2, Math.floor(Number(n) || 2))
    dirty.value = true
  }
})

function markDirty() {
  dirty.value = true
}

function newKey() {
  return `k-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function addUserRule() {
  form.userRules.push({
    key: newKey(),
    subjectType: 'USER',
    subjectId: '',
    subjectLabel: '',
    memberLimit: form.groupMemberLimitDefault
  })
  markDirty()
}

function removeUserRule(index: number) {
  form.userRules.splice(index, 1)
  markDirty()
}

function addGroupRule() {
  form.groupRules.push({
    key: newKey(),
    conversationId: '',
    groupName: '',
    memberLimit: form.groupMemberLimitDefault
  })
  markDirty()
}

function removeGroupRule(index: number) {
  form.groupRules.splice(index, 1)
  markDirty()
}

async function loadUsers(keyword: string) {
  try {
    const page = await getUserPage({ pageNo: 1, pageSize: 20, keyword: keyword || undefined })
    userOptions.value = page.list.map(u => ({
      label: u.nickname || u.username,
      value: u.id,
      kind: 'USER' as const
    }))
  } catch {
    userOptions.value = []
  }
}

async function loadGroups(keyword: string) {
  groupSearching.value = true
  try {
    const rows: ImGroupSearchItem[] = await searchTenantGroups(keyword)
    groupOptions.value = rows.map(g => ({
      label: `${g.name}（${g.memberCount} 人）`,
      value: g.conversationId,
      name: g.name
    }))
  } catch {
    groupOptions.value = []
  } finally {
    groupSearching.value = false
  }
}

function subjectSelectItems(rule: GroupSizeUserRule): SubjectOption[] {
  const selected = rule.subjectId
    ? [{
        label: rule.subjectLabel || rule.subjectId,
        value: rule.subjectId,
        kind: rule.subjectType
      }]
    : []
  const merged = new Map<string, SubjectOption>()
  for (const item of [...deptItems.value, ...userOptions.value, ...selected]) {
    merged.set(`${item.kind}:${item.value}`, item)
  }
  return [...merged.values()]
}

function selectedSubject(rule: GroupSizeUserRule): SubjectOption | undefined {
  if (!rule.subjectId) {
    return undefined
  }
  return {
    label: rule.subjectLabel || rule.subjectId,
    value: rule.subjectId,
    kind: rule.subjectType
  }
}

function onSubjectPicked(rule: GroupSizeUserRule, item: SubjectOption | undefined | null) {
  if (!item) {
    rule.subjectId = ''
    rule.subjectLabel = ''
    markDirty()
    return
  }
  rule.subjectType = item.kind
  rule.subjectId = item.value
  rule.subjectLabel = item.label
  markDirty()
}

function selectedGroup(rule: GroupSizeGroupRule): GroupOption | undefined {
  if (!rule.conversationId) {
    return undefined
  }
  return {
    label: rule.groupName || rule.conversationId,
    value: rule.conversationId,
    name: rule.groupName
  }
}

function groupSelectItems(rule: GroupSizeGroupRule): GroupOption[] {
  const selected = selectedGroup(rule)
  const merged = new Map<string, GroupOption>()
  for (const item of groupOptions.value) {
    merged.set(item.value, item)
  }
  if (selected) {
    merged.set(selected.value, selected)
  }
  return [...merged.values()]
}

function onGroupPicked(rule: GroupSizeGroupRule, item: GroupOption | undefined | null) {
  if (!item) {
    rule.conversationId = ''
    rule.groupName = ''
    markDirty()
    return
  }
  rule.conversationId = item.value
  rule.groupName = item.name
  markDirty()
}

function selectedLimit(rule: { memberLimit: number }): LimitOption {
  return limitOptions.find(o => o.value === rule.memberLimit)
    ?? { label: `${rule.memberLimit} 人`, value: rule.memberLimit }
}

function onLimitPicked(rule: { memberLimit: number }, item: LimitOption | undefined | null) {
  if (item) {
    rule.memberLimit = item.value
    markDirty()
  }
}

function cancelEdits() {
  applyFromStore()
}

async function save() {
  if (!canUpdate.value) {
    return
  }
  try {
    await settingsStore.save(cloneImProductSettings(form))
    applyFromStore()
    toast.add({
      title: '已保存',
      color: 'success'
    })
  } catch (error) {
    toast.add({
      title: '保存失败',
      description: error instanceof Error ? error.message : '请重试',
      color: 'error'
    })
  }
}

async function loadPage() {
  if (!canQuery.value) {
    return
  }
  try {
    await Promise.all([
      settingsStore.load(),
      deptStore.list.length ? Promise.resolve() : deptStore.fetchList()
    ])
    applyFromStore()
    await loadUsers('')
  } catch {
    applyFromStore()
  }
}

onMounted(() => {
  void loadPage()
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-im-group-size">
    <template #header>
      <AdminNavbar title="即时消息 · 群规模管控" />
    </template>

    <template #body>
      <div class="flex flex-col gap-6 p-6">
        <UAlert
          v-if="!canQuery"
          color="warning"
          variant="subtle"
          title="无权限"
          description="需要 im:product-settings:query 权限。"
        />

        <AdminImProductShell
          v-else
          title="群规模管控"
          description="仅作用于对话群。全员群、部门群等特例不受默认/用户规则上限约束（V1 仅对话群 + 后续部门群）。"
        >
          <template #actions>
            <UButton
              color="neutral"
              variant="ghost"
              :disabled="!dirty || settingsStore.saving"
              @click="cancelEdits"
            >
              取消
            </UButton>
            <UButton
              color="primary"
              :loading="settingsStore.saving"
              :disabled="!canUpdate || !dirty"
              @click="save"
            >
              保存
            </UButton>
          </template>

          <div class="space-y-8">
            <UCard>
              <template #header>
                <h3 class="font-semibold">
                  群成员人数上限
                </h3>
              </template>
              <div class="max-w-xs space-y-3">
                <USelectMenu
                  v-model="selectedLimitMode"
                  :items="presetItems"
                  :disabled="!canUpdate"
                />
                <UInput
                  v-if="limitMode === 'CUSTOM'"
                  v-model.number="customLimit"
                  type="number"
                  :min="2"
                  placeholder="自定义人数"
                  :disabled="!canUpdate"
                />
                <p class="text-xs text-muted">
                  部门群不受以上限制（与飞书说明对齐的 V1 子集）。
                </p>
              </div>
            </UCard>

            <UCard>
              <template #header>
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <h3 class="font-semibold">
                      用户规则集
                    </h3>
                    <p class="mt-1 text-xs text-muted">
                      补充规则内的成员可发起超过企业默认上限的群聊
                    </p>
                  </div>
                  <UButton
                    color="primary"
                    variant="soft"
                    size="sm"
                    icon="i-lucide-plus"
                    :disabled="!canUpdate"
                    @click="addUserRule"
                  >
                    新增规则
                  </UButton>
                </div>
              </template>

              <div
                v-if="form.userRules.length === 0"
                class="py-6 text-center text-sm text-muted"
              >
                暂无用户规则
              </div>
              <div
                v-else
                class="space-y-3"
              >
                <div
                  v-for="(rule, index) in form.userRules"
                  :key="rule.key ?? `${rule.subjectType}-${rule.subjectId}-${index}`"
                  class="grid gap-3 rounded-lg border border-default p-3 md:grid-cols-[1fr_10rem_auto]"
                >
                  <USelectMenu
                    :model-value="selectedSubject(rule)"
                    :items="subjectSelectItems(rule)"
                    placeholder="选择部门或用户"
                    :disabled="!canUpdate"
                    @update:model-value="(item: SubjectOption | undefined | null) => onSubjectPicked(rule, item)"
                    @update:open="(open: boolean) => { if (open) void loadUsers('') }"
                  >
                    <template #item-label="{ item }">
                      <span
                        v-if="item && typeof item === 'object' && 'kind' in item"
                        class="flex items-center gap-2"
                      >
                        <UBadge
                          size="sm"
                          color="neutral"
                          variant="subtle"
                        >
                          {{ (item as SubjectOption).kind === 'DEPT' ? '部门' : '用户' }}
                        </UBadge>
                        {{ (item as SubjectOption).label }}
                      </span>
                    </template>
                  </USelectMenu>
                  <USelectMenu
                    :model-value="selectedLimit(rule)"
                    :items="limitOptions"
                    :disabled="!canUpdate"
                    @update:model-value="(item: LimitOption | undefined | null) => onLimitPicked(rule, item)"
                  />
                  <UButton
                    color="neutral"
                    variant="ghost"
                    icon="i-lucide-trash-2"
                    :disabled="!canUpdate"
                    @click="removeUserRule(index)"
                  />
                </div>
              </div>
            </UCard>

            <UCard>
              <template #header>
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <h3 class="font-semibold">
                      群组规则集
                    </h3>
                    <p class="mt-1 text-xs text-muted">
                      为本企业已创建对话群单独规定人数上限
                    </p>
                  </div>
                  <UButton
                    color="primary"
                    variant="soft"
                    size="sm"
                    icon="i-lucide-plus"
                    :disabled="!canUpdate"
                    @click="addGroupRule"
                  >
                    新增规则
                  </UButton>
                </div>
              </template>

              <div
                v-if="form.groupRules.length === 0"
                class="py-6 text-center text-sm text-muted"
              >
                暂无群组规则
              </div>
              <div
                v-else
                class="space-y-3"
              >
                <div
                  v-for="(rule, index) in form.groupRules"
                  :key="rule.key ?? `${rule.conversationId}-${index}`"
                  class="grid gap-3 rounded-lg border border-default p-3 md:grid-cols-[1fr_10rem_auto]"
                >
                  <USelectMenu
                    :model-value="selectedGroup(rule)"
                    :items="groupSelectItems(rule)"
                    placeholder="选择本企业对话群"
                    :loading="groupSearching"
                    :disabled="!canUpdate"
                    @update:model-value="(item: GroupOption | undefined | null) => onGroupPicked(rule, item)"
                    @update:open="(open: boolean) => { if (open) void loadGroups('') }"
                  />
                  <USelectMenu
                    :model-value="selectedLimit(rule)"
                    :items="limitOptions"
                    :disabled="!canUpdate"
                    @update:model-value="(item: LimitOption | undefined | null) => onLimitPicked(rule, item)"
                  />
                  <UButton
                    color="neutral"
                    variant="ghost"
                    icon="i-lucide-trash-2"
                    :disabled="!canUpdate"
                    @click="removeGroupRule(index)"
                  />
                </div>
              </div>
            </UCard>
          </div>
        </AdminImProductShell>
      </div>
    </template>
  </UDashboardPanel>
</template>

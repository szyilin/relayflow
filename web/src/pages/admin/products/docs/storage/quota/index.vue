<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AdminNavbar from '../../../../../../components/admin/AdminNavbar.vue'
import AdminDocsProductShell from '../../../../../../components/admin/products/AdminDocsProductShell.vue'
import AdminMemberSearchField, {
  type AdminMemberSelection
} from '../../../../../../components/admin/AdminMemberSearchField.vue'
import AppTreeSelect from '../../../../../../components/AppTreeSelect.vue'
import WorkspaceOwnerChip from '../../../../../../components/workspace/WorkspaceOwnerChip.vue'
import { usePermission } from '../../../../../../composables/usePermission'
import {
  bytesToGbInput,
  DOCS_STORAGE_WHITELIST_MAX,
  gbInputToBytes
} from '../../../../../../api/admin/docsStorage'
import { useDeptStore } from '../../../../../../stores/dept'
import { useDocsStorageStore } from '../../../../../../stores/docsStorage'
import type { AppTreeSelectItem } from '../../../../../../utils/treeSelect'

type QuotaMode = 'unlimited' | 'capped'
type QuotaTab = 'member' | 'dept' | 'warning'

interface WhitelistDraft {
  member: AdminMemberSelection | null
  mode: QuotaMode
  quotaGb: string
}

interface DeptRuleDraft {
  deptId: string | null
  deptName: string
  mode: QuotaMode
  quotaGb: string
}

const toast = useToast()
const router = useRouter()
const store = useDocsStorageStore()
const deptStore = useDeptStore()
const { hasPermission } = usePermission()

const canQuery = computed(() =>
  hasPermission('docs:storage-quota:query')
  || hasPermission('docs:product-settings:query')
  || hasPermission('im:product-settings:query'))
const canUpdate = computed(() =>
  hasPermission('docs:storage-quota:update')
  || hasPermission('docs:product-settings:update')
  || hasPermission('im:product-settings:update'))

const activeTab = ref<QuotaTab>('member')
const editing = ref(false)

const defaultMode = ref<QuotaMode>('unlimited')
const defaultQuotaGb = ref('10')
const whitelist = ref<WhitelistDraft[]>([])
const deptRules = ref<DeptRuleDraft[]>([])

const tabs = [
  { label: '成员', value: 'member' as const },
  { label: '部门', value: 'dept' as const },
  { label: '上限提示设置', value: 'warning' as const }
]

const deptTreeItems = computed<AppTreeSelectItem[]>(() => {
  function convert(nodes: typeof deptStore.tree): AppTreeSelectItem[] {
    return nodes.map(n => ({
      id: n.id,
      label: n.label,
      value: n.id,
      icon: 'i-lucide-building-2',
      ...(n.children?.length
        ? { children: convert(n.children), defaultExpanded: true }
        : {})
    }))
  }
  return convert(deptStore.tree)
})

function findDeptName(deptId: string | null): string {
  if (!deptId) {
    return ''
  }
  const hit = deptStore.list.find(d => d.id === deptId)
  return hit?.name || deptId
}

function hydrateFromStore() {
  const bytes = store.quota.defaultQuotaBytes
  if (bytes == null) {
    defaultMode.value = 'unlimited'
    defaultQuotaGb.value = '10'
  } else {
    defaultMode.value = 'capped'
    defaultQuotaGb.value = bytesToGbInput(bytes) || '10'
  }
  whitelist.value = store.quota.userRules.map(r => ({
    member: {
      userId: String(r.userId),
      displayName: r.displayName || String(r.userId)
    },
    mode: r.quotaBytes == null ? 'unlimited' as const : 'capped' as const,
    quotaGb: r.quotaBytes == null ? '10' : (bytesToGbInput(r.quotaBytes) || '10')
  }))
  deptRules.value = (store.quota.deptRules ?? []).map(r => ({
    deptId: String(r.deptId),
    deptName: r.deptName || findDeptName(String(r.deptId)),
    mode: r.quotaBytes == null ? 'unlimited' as const : 'capped' as const,
    quotaGb: r.quotaBytes == null ? '10' : (bytesToGbInput(r.quotaBytes) || '10')
  }))
}

async function loadQuota() {
  if (!canQuery.value) {
    return
  }
  try {
    if (!deptStore.list.length) {
      await deptStore.fetchList()
    }
    await store.loadQuota()
    hydrateFromStore()
    editing.value = false
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '加载配额失败', color: 'error' })
  }
}

function startEdit() {
  if (!canUpdate.value) {
    return
  }
  hydrateFromStore()
  editing.value = true
}

function cancelEdit() {
  hydrateFromStore()
  editing.value = false
}

function addWhitelist() {
  if (whitelist.value.length >= DOCS_STORAGE_WHITELIST_MAX) {
    toast.add({ title: `白名单最多 ${DOCS_STORAGE_WHITELIST_MAX} 条`, color: 'warning' })
    return
  }
  whitelist.value = [
    ...whitelist.value,
    { member: null, mode: 'capped', quotaGb: '10' }
  ]
}

function removeWhitelist(index: number) {
  whitelist.value = whitelist.value.filter((_, i) => i !== index)
}

function addDeptRule() {
  deptRules.value = [
    ...deptRules.value,
    { deptId: null, deptName: '', mode: 'capped', quotaGb: '10' }
  ]
}

function removeDeptRule(index: number) {
  deptRules.value = deptRules.value.filter((_, i) => i !== index)
}

function onDeptPicked(row: DeptRuleDraft, deptId: string | null | undefined) {
  row.deptId = deptId ?? null
  row.deptName = findDeptName(row.deptId)
}

async function save() {
  try {
    const defaultQuotaBytes = defaultMode.value === 'unlimited'
      ? null
      : gbInputToBytes(defaultQuotaGb.value)
    if (defaultMode.value === 'capped' && defaultQuotaBytes == null) {
      toast.add({ title: '请填写每人最多 GB 数', color: 'warning' })
      return
    }

    const userRules = []
    for (const row of whitelist.value) {
      if (!row.member?.userId) {
        toast.add({ title: '请为每条白名单选择成员', color: 'warning' })
        return
      }
      const quotaBytes = row.mode === 'unlimited' ? null : gbInputToBytes(row.quotaGb)
      if (row.mode === 'capped' && quotaBytes == null) {
        toast.add({ title: '请填写白名单上限 GB 数', color: 'warning' })
        return
      }
      userRules.push({
        userId: row.member.userId,
        displayName: row.member.displayName,
        quotaBytes
      })
    }
    if (userRules.length > DOCS_STORAGE_WHITELIST_MAX) {
      toast.add({ title: `白名单最多 ${DOCS_STORAGE_WHITELIST_MAX} 条`, color: 'warning' })
      return
    }

    const nextDeptRules = []
    const seenDept = new Set<string>()
    for (const row of deptRules.value) {
      if (!row.deptId) {
        toast.add({ title: '请为每条规则选择部门', color: 'warning' })
        return
      }
      if (seenDept.has(row.deptId)) {
        toast.add({ title: '同一部门只能设置一条上限', color: 'warning' })
        return
      }
      seenDept.add(row.deptId)
      const quotaBytes = row.mode === 'unlimited' ? null : gbInputToBytes(row.quotaGb)
      if (row.mode === 'capped' && quotaBytes == null) {
        toast.add({ title: '请填写部门上限 GB 数', color: 'warning' })
        return
      }
      nextDeptRules.push({
        deptId: row.deptId,
        deptName: row.deptName || findDeptName(row.deptId),
        quotaBytes
      })
    }

    await store.saveQuota({
      defaultQuotaBytes,
      userRules,
      deptRules: nextDeptRules
    })
    editing.value = false
    toast.add({ title: '已保存存储上限', color: 'success' })
  } catch (e) {
    toast.add({ title: e instanceof Error ? e.message : '保存失败', color: 'error' })
  }
}

function goBack() {
  void router.push('/admin/products/docs/storage')
}

onMounted(() => {
  void loadQuota()
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-docs-storage-quota">
    <template #header>
      <AdminNavbar title="云文档 · 存储上限管理" />
    </template>

    <template #body>
      <div class="flex flex-col gap-6 p-6">
        <UAlert
          v-if="!canQuery"
          color="warning"
          variant="subtle"
          title="无权限"
          description="需要 docs:storage-quota:query 权限。"
        />

        <AdminDocsProductShell
          v-else
          title="存储上限管理"
          description="设置全员默认、成员白名单与部门上限。生效优先级：成员 > 部门（主部门）> 租户默认。"
        >
          <template #actions>
            <UButton
              color="neutral"
              variant="ghost"
              icon="i-lucide-arrow-left"
              @click="goBack"
            >
              返回用量
            </UButton>
            <template v-if="canUpdate && activeTab !== 'warning'">
              <template v-if="!editing">
                <UButton
                  color="primary"
                  :loading="store.quotaLoading"
                  @click="startEdit"
                >
                  编辑
                </UButton>
              </template>
              <template v-else>
                <UButton
                  color="neutral"
                  variant="ghost"
                  @click="cancelEdit"
                >
                  取消
                </UButton>
                <UButton
                  color="primary"
                  :loading="store.saving"
                  @click="save"
                >
                  保存
                </UButton>
              </template>
            </template>
          </template>

          <div class="space-y-6">
            <UTabs
              v-model="activeTab"
              :items="tabs"
              class="w-full max-w-lg"
            />

            <div
              v-if="activeTab === 'member'"
              class="space-y-8"
            >
              <section class="space-y-3">
                <h2 class="text-base font-semibold text-highlighted">
                  成员存储上限设置
                </h2>

                <div class="space-y-3 rounded-lg border border-default p-4">
                  <p class="text-sm font-medium">
                    按租户设置
                  </p>
                  <div class="flex flex-wrap items-center gap-4">
                    <URadioGroup
                      v-model="defaultMode"
                      orientation="horizontal"
                      :disabled="!editing"
                      :items="[
                        { label: '不限制', value: 'unlimited' },
                        { label: '每人最多', value: 'capped' }
                      ]"
                    />
                    <div
                      v-if="defaultMode === 'capped'"
                      class="flex items-center gap-2 text-sm"
                    >
                      <UInput
                        v-model="defaultQuotaGb"
                        class="w-20"
                        :disabled="!editing"
                      />
                      <span>GB</span>
                    </div>
                  </div>
                </div>

                <div class="space-y-3 rounded-lg border border-default p-4">
                  <div class="flex flex-wrap items-center justify-between gap-2">
                    <div>
                      <p class="text-sm font-medium">
                        按白名单设置
                      </p>
                      <p class="mt-1 text-xs text-muted">
                        最多 {{ DOCS_STORAGE_WHITELIST_MAX }} 条。优先级：成员 &gt; 部门（用户组后置）。
                      </p>
                    </div>
                    <UButton
                      v-if="editing"
                      size="sm"
                      variant="soft"
                      icon="i-lucide-plus"
                      :disabled="whitelist.length >= DOCS_STORAGE_WHITELIST_MAX"
                      @click="addWhitelist"
                    >
                      添加白名单
                    </UButton>
                  </div>

                  <div
                    v-if="!whitelist.length"
                    class="rounded-md bg-elevated/50 px-4 py-10 text-center text-sm text-muted"
                  >
                    暂无数据
                  </div>

                  <div
                    v-for="(row, index) in whitelist"
                    :key="index"
                    class="flex flex-wrap items-end gap-3 rounded-md border border-default p-3"
                  >
                    <div class="min-w-[14rem] flex-1">
                      <template v-if="editing">
                        <AdminMemberSearchField
                          v-model="row.member"
                          label="成员"
                          placeholder="请选择成员"
                        />
                      </template>
                      <template v-else-if="row.member">
                        <p class="mb-1 text-sm text-muted">
                          成员
                        </p>
                        <WorkspaceOwnerChip
                          :user-id="row.member.userId"
                          :display-name="row.member.displayName"
                          size="sm"
                        />
                      </template>
                    </div>
                    <div class="flex flex-wrap items-center gap-3 pb-1">
                      <URadioGroup
                        v-model="row.mode"
                        orientation="horizontal"
                        :disabled="!editing"
                        :items="[
                          { label: '不限制', value: 'unlimited' },
                          { label: '最多', value: 'capped' }
                        ]"
                      />
                      <div
                        v-if="row.mode === 'capped'"
                        class="flex items-center gap-2 text-sm"
                      >
                        <UInput
                          v-model="row.quotaGb"
                          class="w-20"
                          :disabled="!editing"
                        />
                        <span>GB</span>
                      </div>
                    </div>
                    <UButton
                      v-if="editing"
                      size="sm"
                      color="error"
                      variant="ghost"
                      icon="i-lucide-trash-2"
                      @click="removeWhitelist(index)"
                    />
                  </div>
                </div>
              </section>
            </div>

            <div
              v-else-if="activeTab === 'dept'"
              class="space-y-4"
            >
              <div class="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h2 class="text-base font-semibold text-highlighted">
                    部门存储上限设置
                  </h2>
                  <p class="mt-1 text-xs text-muted">
                    一个部门只能设置一个上限；作用于该部门主部门成员（无成员白名单时）。
                  </p>
                </div>
                <UButton
                  v-if="editing"
                  size="sm"
                  variant="soft"
                  icon="i-lucide-plus"
                  @click="addDeptRule"
                >
                  添加部门
                </UButton>
              </div>

              <div
                v-if="!deptRules.length"
                class="rounded-md bg-elevated/50 px-4 py-10 text-center text-sm text-muted"
              >
                暂无数据
              </div>

              <div
                v-for="(row, index) in deptRules"
                :key="index"
                class="flex flex-wrap items-end gap-3 rounded-md border border-default p-3"
              >
                <UFormField
                  label="部门"
                  class="min-w-[14rem] flex-1"
                >
                  <template v-if="editing">
                    <AppTreeSelect
                      :model-value="row.deptId"
                      :items="deptTreeItems"
                      placeholder="请选择部门"
                      @update:model-value="(v: string | null | undefined) => onDeptPicked(row, v)"
                    />
                  </template>
                  <p
                    v-else
                    class="text-sm font-medium"
                  >
                    {{ row.deptName || row.deptId || '—' }}
                  </p>
                </UFormField>
                <div class="flex flex-wrap items-center gap-3 pb-1">
                  <URadioGroup
                    v-model="row.mode"
                    orientation="horizontal"
                    :disabled="!editing"
                    :items="[
                      { label: '不限制', value: 'unlimited' },
                      { label: '每个部门最多', value: 'capped' }
                    ]"
                  />
                  <div
                    v-if="row.mode === 'capped'"
                    class="flex items-center gap-2 text-sm"
                  >
                    <UInput
                      v-model="row.quotaGb"
                      class="w-20"
                      :disabled="!editing"
                    />
                    <span>GB</span>
                  </div>
                </div>
                <UButton
                  v-if="editing"
                  size="sm"
                  color="error"
                  variant="ghost"
                  icon="i-lucide-trash-2"
                  @click="removeDeptRule(index)"
                />
              </div>
            </div>

            <div
              v-else
              class="space-y-4"
            >
              <div>
                <h2 class="text-base font-semibold text-highlighted">
                  上限提示设置
                </h2>
                <p class="mt-1 text-sm text-muted">
                  接近或达到存储上限时，向成员发送站内提醒（后续可接 Bot / 邮件）。本版仅占位，不落库、不可保存。
                </p>
              </div>

              <div class="rounded-lg border border-dashed border-default bg-elevated/30 px-6 py-10">
                <div class="mx-auto flex max-w-md flex-col items-center gap-3 text-center">
                  <UIcon
                    name="i-lucide-bell-off"
                    class="size-10 text-muted"
                  />
                  <p class="text-sm font-medium text-highlighted">
                    功能即将推出
                  </p>
                  <p class="text-sm text-muted">
                    计划能力：用量阈值阈值、触顶提醒文案、通知渠道开关。当前触顶仍由上传链路返回
                    <code class="text-xs">DOC_STORAGE_QUOTA_EXCEEDED</code>
                    ，与本页无关。
                  </p>
                </div>
              </div>

              <UAlert
                color="neutral"
                variant="subtle"
                title="用户组白名单后置"
                description="飞书优先级为「成员 > 用户组 > 部门」。本版仅实现成员与部门（主部门）；用户组规则不在本 change 范围，后续另开。"
              />
            </div>
          </div>
        </AdminDocsProductShell>
      </div>
    </template>
  </UDashboardPanel>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, watch } from 'vue'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { usePermission } from '../../../../composables/usePermission'
import {
  type InviteMode,
  type NonAdminInviteScope
} from '../../../../api/admin/inviteSettings'
import { useDeptStore } from '../../../../stores/dept'
import { useInviteSettingsStore } from '../../../../stores/inviteSettings'

const toast = useToast()
const { hasPermission } = usePermission()
const inviteStore = useInviteSettingsStore()
const deptStore = useDeptStore()

const canQuery = computed(() => hasPermission('system:user:list'))
const canUpdate = computed(() => hasPermission('system:user:create'))

const form = reactive({
  inviteMode: 'ADMIN_ONLY' as InviteMode,
  nonAdminScope: 'INVITER_DEPT' as NonAdminInviteScope,
  nonAdminDeptIds: [] as string[],
  nonAdminExpireDays: 3,
  nonAdminRequireApproval: false
})

const showNonAdminRules = computed(() => form.inviteMode === 'ALL_MEMBERS')

const inviteModeItems = [
  {
    value: 'OFF' as const,
    label: '关闭邀请',
    description: '管理员与成员均不可发送邀请；已发出的开放邀请将视为失效'
  },
  {
    value: 'ADMIN_ONLY' as const,
    label: '开启邀请，仅管理员可发送邀请',
    description: '仅管理身份可在工作台「添加企业成员」或管理端邀请成员'
  },
  {
    value: 'ALL_MEMBERS' as const,
    label: '开启邀请，全员（包括管理员、非管理员）均可发送邀请',
    description: '非管理员须遵守下方范围、有效期与审批设置'
  }
]

const scopeItems = [
  { value: 'INVITER_DEPT' as const, label: '邀请人所在部门' },
  { value: 'DESIGNATED_DEPTS' as const, label: '指定部门' }
]

const expireItems = [
  { label: '1 天后过期', value: 1 },
  { label: '3 天后过期', value: 3 },
  { label: '7 天后过期', value: 7 },
  { label: '30 天后过期', value: 30 }
]

const deptSelectItems = computed(() =>
  deptStore.list.map(item => ({
    label: item.name,
    value: item.id
  }))
)

const selectedExpire = computed({
  get: () => expireItems.find(item => item.value === form.nonAdminExpireDays) ?? expireItems[1]!,
  set: (item: { label: string, value: number } | undefined) => {
    if (item) {
      form.nonAdminExpireDays = item.value
    }
  }
})

const selectedDepts = computed({
  get: () => deptSelectItems.value.filter(item => form.nonAdminDeptIds.includes(item.value)),
  set: (items: Array<{ label: string, value: string }> | undefined | null) => {
    form.nonAdminDeptIds = (items ?? []).map(item => item.value)
  }
})

function applyFromStore() {
  const s = inviteStore.settings
  form.inviteMode = s.inviteMode
  form.nonAdminScope = s.nonAdminScope
  form.nonAdminDeptIds = [...s.nonAdminDeptIds]
  form.nonAdminExpireDays = s.nonAdminExpireDays
  form.nonAdminRequireApproval = s.nonAdminRequireApproval
}

watch(() => form.inviteMode, (mode) => {
  if (mode !== 'ALL_MEMBERS') {
    form.nonAdminRequireApproval = false
  }
})

async function loadPage() {
  if (!canQuery.value) {
    return
  }
  try {
    await Promise.all([
      inviteStore.load(),
      deptStore.fetchList().catch(() => {
        // 部门列表失败时指定部门为空，仍可编辑其它项
      })
    ])
    applyFromStore()
  } catch {
    toast.add({
      title: '加载失败',
      description: inviteStore.lastError ?? '无法加载邀请设置',
      color: 'error'
    })
  }
}

async function save() {
  if (!canUpdate.value) {
    toast.add({ title: '无保存权限', color: 'warning' })
    return
  }
  try {
    await inviteStore.save({
      inviteMode: form.inviteMode,
      nonAdminScope: form.nonAdminScope,
      nonAdminDeptIds: [...form.nonAdminDeptIds],
      nonAdminExpireDays: form.nonAdminExpireDays,
      nonAdminRequireApproval: form.nonAdminRequireApproval
    })
    toast.add({
      title: '已保存',
      description: '邀请设置已写入当前租户',
      color: 'success'
    })
  } catch {
    toast.add({
      title: '保存失败',
      description: inviteStore.lastError ?? '请检查表单',
      color: 'error'
    })
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
  <UDashboardPanel>
    <template #header>
      <AdminNavbar title="邀请设置" />
    </template>

    <template #body>
      <div class="flex flex-col gap-6 p-6">
        <AdminPageHeader
          title="邀请设置"
          description="发送加入组织邀请的全局设置：功能开关、可邀请角色、非管理员范围与审批"
        >
          <template #actions>
            <UButton
              v-if="canUpdate"
              color="primary"
              :loading="inviteStore.saving"
              @click="save"
            >
              保存
            </UButton>
          </template>
        </AdminPageHeader>

        <UAlert
          v-if="!canQuery"
          color="warning"
          title="无查看权限"
          description="需要 system:user:list 权限"
        />

        <div
          v-else
          class="mx-auto w-full max-w-2xl space-y-6"
        >
          <UCard>
            <template #header>
              <div>
                <h3 class="text-sm font-semibold">
                  邀请功能
                </h3>
                <p class="mt-1 text-sm text-muted">
                  设置是否开启邀请，以及允许发送邀请的角色
                </p>
              </div>
            </template>

            <div
              v-if="inviteStore.loading"
              class="space-y-3"
            >
              <USkeleton class="h-16 w-full" />
              <USkeleton class="h-16 w-full" />
              <USkeleton class="h-16 w-full" />
            </div>

            <fieldset
              v-else
              class="space-y-3"
              :disabled="!canUpdate"
            >
              <label
                v-for="item in inviteModeItems"
                :key="item.value"
                class="flex cursor-pointer gap-3 rounded-lg border border-default p-3 transition-colors"
                :class="form.inviteMode === item.value
                  ? 'border-primary bg-primary/5'
                  : 'hover:bg-elevated/50'"
              >
                <input
                  v-model="form.inviteMode"
                  type="radio"
                  class="mt-1"
                  :value="item.value"
                  name="inviteMode"
                >
                <div class="min-w-0">
                  <p class="text-sm font-medium">
                    {{ item.label }}
                  </p>
                  <p class="mt-0.5 text-xs text-muted">
                    {{ item.description }}
                  </p>
                </div>
              </label>
            </fieldset>
          </UCard>

          <UCard v-if="showNonAdminRules">
            <template #header>
              <div>
                <h3 class="text-sm font-semibold">
                  非管理员邀请规则
                </h3>
                <p class="mt-1 text-sm text-muted">
                  仅在「全员均可发送邀请」时生效；管理员不受这些限制
                </p>
              </div>
            </template>

            <div class="space-y-5">
              <UFormField label="非管理员可邀请范围">
                <div class="space-y-2">
                  <label
                    v-for="item in scopeItems"
                    :key="item.value"
                    class="flex cursor-pointer items-center gap-2 text-sm"
                  >
                    <input
                      v-model="form.nonAdminScope"
                      type="radio"
                      :value="item.value"
                      name="nonAdminScope"
                      :disabled="!canUpdate"
                    >
                    {{ item.label }}
                  </label>
                </div>
              </UFormField>

              <UFormField
                v-if="form.nonAdminScope === 'DESIGNATED_DEPTS'"
                label="指定部门"
              >
                <USelectMenu
                  v-model="selectedDepts"
                  :items="deptSelectItems"
                  multiple
                  placeholder="选择部门"
                  :disabled="!canUpdate"
                  class="w-full"
                />
              </UFormField>

              <UFormField label="非管理员邀请有效期">
                <USelectMenu
                  v-model="selectedExpire"
                  :items="expireItems"
                  :disabled="!canUpdate"
                  class="w-full max-w-xs"
                />
              </UFormField>

              <UFormField label="非管理员邀请审批设置">
                <UCheckbox
                  v-model="form.nonAdminRequireApproval"
                  label="需要管理员审批"
                  :disabled="!canUpdate"
                />
                <template #hint>
                  开启后，非管理员发出的邀请须管理员批准后，被邀请人才能加入企业
                </template>
              </UFormField>
            </div>
          </UCard>

          <div class="flex justify-end gap-2">
            <UButton
              color="neutral"
              variant="ghost"
              to="/admin/system/user"
            >
              返回
            </UButton>
            <UButton
              v-if="canUpdate"
              color="primary"
              :loading="inviteStore.saving"
              @click="save"
            >
              保存
            </UButton>
          </div>
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>

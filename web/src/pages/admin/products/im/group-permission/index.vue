<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AdminNavbar from '../../../../../components/admin/AdminNavbar.vue'
import AdminImProductShell from '../../../../../components/admin/products/AdminImProductShell.vue'
import { usePermission } from '../../../../../composables/usePermission'
import {
  cloneImProductSettings,
  type DeptGroupLeavePolicy,
  type MemberExportPolicy
} from '../../../../../api/admin/imProductSettings'
import { getUserPage } from '../../../../../api/admin/user'
import { useImProductSettingsStore } from '../../../../../stores/imProductSettings'

const toast = useToast()
const { hasPermission } = usePermission()
const settingsStore = useImProductSettingsStore()

const canQuery = computed(() => hasPermission('im:product-settings:query'))
const canUpdate = computed(() => hasPermission('im:product-settings:update'))

const form = reactive(cloneImProductSettings(settingsStore.settings))
const dirty = ref(false)

const publicOptions = [
  { value: true, label: '允许设置群组为「公开」', description: '群主和管理员可在群管理中开启「允许被搜索」。开启后群显示公开标记，企业成员可通过搜索加入（V1：直接加入）。' },
  { value: false, label: '禁止设置群组为「公开」', description: '隐藏公开开关；保存策略后关闭已公开群。' }
]

const leaveOptions: Array<{ value: DeptGroupLeavePolicy, label: string }> = [
  { value: 'ALLOW', label: '允许部门成员退出部门群' },
  { value: 'DENY', label: '禁止部门成员退出部门群' }
]

const exportOptions: Array<{ value: MemberExportPolicy, label: string, description: string }> = [
  {
    value: 'ALLOW_OWNER_ADMIN',
    label: '允许群主和群管理员导出（请谨慎选择）',
    description: '在名片可见字段范围内导出部门、直属上级等成员信息。'
  },
  {
    value: 'DENY_ALL',
    label: '禁止任何人导出',
    description: '群主与管理员亦不可导出。'
  },
  {
    value: 'ALLOW_DESIGNATED',
    label: '仅允许指定成员为群主或群管理员时可导出',
    description: '指定成员须同时具备群主/管理员身份才可导出。'
  }
]

const designatedUserOptions = ref<Array<{ label: string, value: string }>>([])

const selectedDesignated = computed({
  get: () => designatedUserOptions.value.filter(u => form.memberExportDesignatedUserIds.includes(u.value)),
  set: (items: Array<{ label: string, value: string }> | undefined | null) => {
    form.memberExportDesignatedUserIds = (items ?? []).map(i => i.value)
    dirty.value = true
  }
})

function applyFromStore() {
  Object.assign(form, cloneImProductSettings(settingsStore.settings))
  dirty.value = false
}

function cancelEdits() {
  applyFromStore()
}

async function loadUsers() {
  try {
    const page = await getUserPage({ pageNo: 1, pageSize: 50 })
    designatedUserOptions.value = page.list.map(u => ({
      label: u.nickname || u.username,
      value: u.id
    }))
  } catch {
    designatedUserOptions.value = []
  }
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
  await settingsStore.load()
  applyFromStore()
  await loadUsers()
}

watch(() => form.allowPublicGroup, () => { dirty.value = true })
watch(() => form.deptGroupLeavePolicy, () => { dirty.value = true })
watch(() => form.memberExportPolicy, () => { dirty.value = true })

onMounted(() => {
  void loadPage()
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-im-group-permission">
    <template #header>
      <AdminNavbar title="即时消息 · 群权限管控" />
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
          title="群权限管控"
          description="组织级进群、部门群退出与成员导出策略。对应工作台能力将随联调逐步强制执行。"
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

          <div class="mx-auto w-full max-w-3xl space-y-8">
            <section class="space-y-3 border-b border-default pb-8">
              <h3 class="font-semibold">
                进群管理
              </h3>
              <p class="text-sm text-muted">
                群主和管理员可设置「允许群组被搜索」。开启后群显示「公开」标记，企业成员可通过搜索加入。
              </p>
              <div class="space-y-2">
                <label
                  v-for="opt in publicOptions"
                  :key="String(opt.value)"
                  class="flex cursor-pointer gap-3 rounded-lg border border-default p-3"
                  :class="form.allowPublicGroup === opt.value ? 'border-primary bg-primary/5' : ''"
                >
                  <input
                    v-model="form.allowPublicGroup"
                    type="radio"
                    class="mt-1"
                    :value="opt.value"
                    :disabled="!canUpdate"
                  >
                  <span>
                    <span class="block font-medium">{{ opt.label }}</span>
                    <span class="mt-1 block text-xs text-muted">{{ opt.description }}</span>
                  </span>
                </label>
              </div>
            </section>

            <section class="space-y-3 border-b border-default pb-8">
              <h3 class="font-semibold">
                部门群管理
              </h3>
              <p class="text-sm text-muted">
                设置是否允许部门成员退出部门群（需配套部门群能力后强制执行）。
              </p>
              <div class="space-y-2">
                <label
                  v-for="opt in leaveOptions"
                  :key="opt.value"
                  class="flex cursor-pointer gap-3 rounded-lg border border-default p-3"
                  :class="form.deptGroupLeavePolicy === opt.value ? 'border-primary bg-primary/5' : ''"
                >
                  <input
                    v-model="form.deptGroupLeavePolicy"
                    type="radio"
                    class="mt-1"
                    :value="opt.value"
                    :disabled="!canUpdate"
                  >
                  <span class="font-medium">{{ opt.label }}</span>
                </label>
              </div>
            </section>

            <section class="space-y-3">
              <h3 class="font-semibold">
                群成员信息导出管理
              </h3>
              <p class="text-sm text-muted">
                在「名片可见信息」范围内，支持导出群成员的部门、直属上级等字段。
              </p>
              <div class="space-y-2">
                <label
                  v-for="opt in exportOptions"
                  :key="opt.value"
                  class="flex cursor-pointer gap-3 rounded-lg border border-default p-3"
                  :class="form.memberExportPolicy === opt.value ? 'border-primary bg-primary/5' : ''"
                >
                  <input
                    v-model="form.memberExportPolicy"
                    type="radio"
                    class="mt-1"
                    :value="opt.value"
                    :disabled="!canUpdate"
                  >
                  <span>
                    <span class="block font-medium">{{ opt.label }}</span>
                    <span class="mt-1 block text-xs text-muted">{{ opt.description }}</span>
                  </span>
                </label>
              </div>
              <UFormField
                v-if="form.memberExportPolicy === 'ALLOW_DESIGNATED'"
                label="指定成员"
                class="pt-2"
              >
                <USelectMenu
                  v-model="selectedDesignated"
                  :items="designatedUserOptions"
                  multiple
                  placeholder="选择可导出成员"
                  :disabled="!canUpdate"
                />
              </UFormField>
            </section>
          </div>
        </AdminImProductShell>
      </div>
    </template>
  </UDashboardPanel>
</template>

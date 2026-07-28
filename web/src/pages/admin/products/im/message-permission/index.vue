<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AdminNavbar from '../../../../../components/admin/AdminNavbar.vue'
import AdminImProductShell from '../../../../../components/admin/products/AdminImProductShell.vue'
import { usePermission } from '../../../../../composables/usePermission'
import {
  RECALL_EDIT_WINDOW_OPTIONS,
  cloneImProductSettings,
  type RecallEditWindowHours
} from '../../../../../api/admin/imProductSettings'
import { useImProductSettingsStore } from '../../../../../stores/imProductSettings'

const toast = useToast()
const { hasPermission } = usePermission()
const settingsStore = useImProductSettingsStore()

const canQuery = computed(() => hasPermission('im:product-settings:query'))
const canUpdate = computed(() => hasPermission('im:product-settings:update'))

const form = reactive(cloneImProductSettings(settingsStore.settings))
const dirty = ref(false)

function applyFromStore() {
  Object.assign(form, cloneImProductSettings(settingsStore.settings))
  dirty.value = false
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
  await settingsStore.load()
  applyFromStore()
}

const recallWindow = computed({
  get: () => RECALL_EDIT_WINDOW_OPTIONS.find(o => o.value === form.recallWithinHours)
    ?? RECALL_EDIT_WINDOW_OPTIONS[1]!,
  set: (item: { label: string, value: RecallEditWindowHours } | undefined) => {
    if (item) {
      form.recallWithinHours = item.value
      dirty.value = true
    }
  }
})

const editWindow = computed({
  get: () => RECALL_EDIT_WINDOW_OPTIONS.find(o => o.value === form.editWithinHours)
    ?? RECALL_EDIT_WINDOW_OPTIONS[1]!,
  set: (item: { label: string, value: RecallEditWindowHours } | undefined) => {
    if (item) {
      form.editWithinHours = item.value
      dirty.value = true
    }
  }
})

watch(() => form.recallEnabled, () => { dirty.value = true })
watch(() => form.editEnabled, () => { dirty.value = true })

onMounted(() => {
  void loadPage()
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-im-message-permission">
    <template #header>
      <AdminNavbar title="即时消息 · 消息权限管控" />
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
          title="消息权限管控"
          description="组织级撤回与编辑策略。群主/群管理员撤回群内消息不受「不允许撤回」开关限制。"
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
                撤回消息
              </h3>
              <p class="text-sm text-muted">
                设置是否允许企业成员撤回已发送消息。此设置不影响群主和群管理员在群内撤回消息。
              </p>
              <div class="space-y-2">
                <label
                  class="flex cursor-pointer gap-3 rounded-lg border border-default p-3"
                  :class="!form.recallEnabled ? 'border-primary bg-primary/5' : ''"
                >
                  <input
                    v-model="form.recallEnabled"
                    type="radio"
                    class="mt-1"
                    :value="false"
                    :disabled="!canUpdate"
                  >
                  <span class="font-medium">不允许撤回</span>
                </label>
                <label
                  class="flex cursor-pointer flex-col gap-3 rounded-lg border border-default p-3"
                  :class="form.recallEnabled ? 'border-primary bg-primary/5' : ''"
                >
                  <span class="flex gap-3">
                    <input
                      v-model="form.recallEnabled"
                      type="radio"
                      class="mt-1"
                      :value="true"
                      :disabled="!canUpdate"
                    >
                    <span class="font-medium">允许撤回</span>
                  </span>
                  <div
                    v-if="form.recallEnabled"
                    class="ml-7 max-w-xs"
                  >
                    <UFormField label="时间限制">
                      <USelectMenu
                        v-model="recallWindow"
                        :items="RECALL_EDIT_WINDOW_OPTIONS"
                        :disabled="!canUpdate"
                      />
                    </UFormField>
                    <p class="mt-1 text-xs text-muted">
                      消息发出后 {{ form.recallWithinHours }} 小时内
                    </p>
                  </div>
                </label>
              </div>
            </section>

            <section class="space-y-3">
              <h3 class="font-semibold">
                编辑消息
              </h3>
              <p class="text-sm text-muted">
                设置是否允许企业成员编辑已发送消息（V1：仅发送者本人）。
              </p>
              <div class="space-y-2">
                <label
                  class="flex cursor-pointer gap-3 rounded-lg border border-default p-3"
                  :class="!form.editEnabled ? 'border-primary bg-primary/5' : ''"
                >
                  <input
                    v-model="form.editEnabled"
                    type="radio"
                    class="mt-1"
                    :value="false"
                    :disabled="!canUpdate"
                  >
                  <span class="font-medium">不允许编辑</span>
                </label>
                <label
                  class="flex cursor-pointer flex-col gap-3 rounded-lg border border-default p-3"
                  :class="form.editEnabled ? 'border-primary bg-primary/5' : ''"
                >
                  <span class="flex gap-3">
                    <input
                      v-model="form.editEnabled"
                      type="radio"
                      class="mt-1"
                      :value="true"
                      :disabled="!canUpdate"
                    >
                    <span class="font-medium">允许编辑</span>
                  </span>
                  <div
                    v-if="form.editEnabled"
                    class="ml-7 max-w-xs"
                  >
                    <UFormField label="时间限制">
                      <USelectMenu
                        v-model="editWindow"
                        :items="RECALL_EDIT_WINDOW_OPTIONS"
                        :disabled="!canUpdate"
                      />
                    </UFormField>
                    <p class="mt-1 text-xs text-muted">
                      消息发出后 {{ form.editWithinHours }} 小时内
                    </p>
                  </div>
                </label>
              </div>
            </section>
          </div>
        </AdminImProductShell>
      </div>
    </template>
  </UDashboardPanel>
</template>

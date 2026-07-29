<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminNavbar from '../../../../../components/admin/AdminNavbar.vue'
import AdminCalendarProductShell from '../../../../../components/admin/products/AdminCalendarProductShell.vue'
import { usePermission } from '../../../../../composables/usePermission'
import { useCalProductSettingsStore } from '../../../../../stores/calProductSettings'

const toast = useToast()
const { hasPermission } = usePermission()
const store = useCalProductSettingsStore()

const canQuery = computed(() => hasPermission('calendar:product-settings:query'))
const canUpdate = computed(() => hasPermission('calendar:product-settings:update'))

const allowOwned = ref(true)
const dirty = ref(false)
const saving = ref(false)

function applyFromStore() {
  allowOwned.value = store.settings.allowMemberCreateOwnedCalendar
  dirty.value = false
}

async function load() {
  if (!canQuery.value) {
    return
  }
  try {
    await store.load()
    applyFromStore()
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '加载失败',
      color: 'error'
    })
  }
}

async function save() {
  if (!canUpdate.value) {
    return
  }
  saving.value = true
  try {
    await store.saveOrgPolicy(allowOwned.value)
    applyFromStore()
    toast.add({ title: '已保存', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '保存失败',
      color: 'error'
    })
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-products-calendar-org">
    <template #header>
      <AdminNavbar title="日历 · 组织策略" />
    </template>
    <template #body>
      <div class="flex flex-col gap-6 p-6">
        <UAlert
          v-if="!canQuery"
          color="warning"
          variant="subtle"
          title="无权限"
          description="需要 calendar:product-settings:query。若刚跑完迁移，请重新登录以刷新权限。"
        />
        <AdminCalendarProductShell
          v-else
          title="组织策略"
          description="控制成员是否可新建个人日历；个人偏好（周起始等）仍在工作台设置中配置。"
        >
          <template
            v-if="canUpdate"
            #actions
          >
            <UButton
              color="neutral"
              variant="ghost"
              :disabled="!dirty || saving"
              @click="applyFromStore"
            >
              取消
            </UButton>
            <UButton
              color="primary"
              :loading="saving"
              :disabled="!dirty"
              @click="save"
            >
              保存
            </UButton>
          </template>

          <UCard>
            <UFormField label="允许成员新建个人日历">
              <USwitch
                :model-value="allowOwned"
                :disabled="!canUpdate"
                @update:model-value="(v: boolean) => { allowOwned = v; dirty = true }"
              />
              <p class="mt-2 text-xs text-muted">
                关闭后，工作台「添加日历」将无法创建 OWNED 日历；主日历与全员日历不受影响。
              </p>
            </UFormField>
          </UCard>
        </AdminCalendarProductShell>
      </div>
    </template>
  </UDashboardPanel>
</template>

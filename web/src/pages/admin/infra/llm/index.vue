<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { usePermission } from '../../../../composables/usePermission'
import { useLlmConnectionStore } from '../../../../stores/llmConnection'
import type { LlmConnectionItem } from '../../../../api/admin/llmConnection'

const toast = useToast()
const store = useLlmConnectionStore()
const { hasPermission } = usePermission()

const canQuery = computed(() => hasPermission('agent:llm:query'))
const canUpdate = computed(() => hasPermission('agent:llm:update'))
const canTest = computed(() => hasPermission('agent:llm:test'))

const editing = ref(false)
const saving = ref(false)
const testing = ref(false)
const deletingId = ref<string | number | null>(null)
const deleteOpen = ref(false)
const deleteTarget = ref<LlmConnectionItem | null>(null)

const form = reactive({
  id: undefined as string | number | undefined,
  code: '',
  name: '',
  providerType: 'openai_compatible',
  baseUrl: 'https://api.openai.com/v1',
  apiKey: '',
  modelName: 'gpt-4o-mini',
  enabled: true,
  apiKeyConfigured: false
})

function resetForm() {
  form.id = undefined
  form.code = ''
  form.name = ''
  form.providerType = 'openai_compatible'
  form.baseUrl = 'https://api.openai.com/v1'
  form.apiKey = ''
  form.modelName = 'gpt-4o-mini'
  form.enabled = true
  form.apiKeyConfigured = false
}

function openCreate() {
  resetForm()
  editing.value = true
}

function openEdit(row: LlmConnectionItem) {
  form.id = row.id
  form.code = row.code
  form.name = row.name
  form.providerType = row.providerType || 'openai_compatible'
  form.baseUrl = row.baseUrl
  form.apiKey = ''
  form.modelName = row.modelName
  form.enabled = row.enabled
  form.apiKeyConfigured = row.apiKeyConfigured
  editing.value = true
}

async function loadList() {
  if (!canQuery.value) {
    return
  }
  try {
    await store.fetchList()
  } catch {
    toast.add({
      title: '加载失败',
      description: store.lastError ?? '无法获取模型连接',
      color: 'error'
    })
  }
}

function validateForm(requireKey: boolean): boolean {
  if (!form.code.trim() || !form.name.trim() || !form.baseUrl.trim() || !form.modelName.trim()) {
    toast.add({ title: '请填写完整必填项', color: 'warning' })
    return false
  }
  if (requireKey && !form.apiKey.trim()) {
    toast.add({ title: '请填写 API Key', color: 'warning' })
    return false
  }
  return true
}

async function handleSave() {
  const isCreate = form.id == null
  if (!validateForm(isCreate && !form.apiKeyConfigured)) {
    return
  }
  saving.value = true
  try {
    await store.save({
      id: form.id,
      code: form.code.trim(),
      name: form.name.trim(),
      providerType: form.providerType,
      baseUrl: form.baseUrl.trim(),
      apiKey: form.apiKey.trim() || undefined,
      modelName: form.modelName.trim(),
      enabled: form.enabled
    })
    editing.value = false
    toast.add({ title: isCreate ? '已创建' : '已保存', color: 'success' })
  } catch (error) {
    toast.add({
      title: '保存失败',
      description: error instanceof Error ? error.message : '操作失败',
      color: 'error'
    })
  } finally {
    saving.value = false
  }
}

async function handleTest() {
  if (!form.baseUrl.trim()) {
    toast.add({ title: '请填写 Base URL', color: 'warning' })
    return
  }
  if (!form.apiKey.trim() && !form.apiKeyConfigured && form.id == null) {
    toast.add({ title: '请填写 API Key 后再测试', color: 'warning' })
    return
  }
  testing.value = true
  try {
    await store.test({
      id: form.id,
      baseUrl: form.baseUrl.trim(),
      apiKey: form.apiKey.trim() || undefined,
      providerType: form.providerType
    })
    toast.add({ title: '连通性测试成功', color: 'success' })
  } catch (error) {
    toast.add({
      title: '连通性测试失败',
      description: error instanceof Error ? error.message : '测试失败',
      color: 'error'
    })
  } finally {
    testing.value = false
  }
}

function askDelete(row: LlmConnectionItem) {
  deleteTarget.value = row
  deleteOpen.value = true
}

async function confirmDelete() {
  if (!deleteTarget.value) {
    return
  }
  deletingId.value = deleteTarget.value.id
  try {
    await store.remove(deleteTarget.value.id)
    deleteOpen.value = false
    if (form.id === deleteTarget.value.id) {
      editing.value = false
    }
    toast.add({ title: '已删除', color: 'success' })
  } catch (error) {
    toast.add({
      title: '删除失败',
      description: error instanceof Error ? error.message : '操作失败',
      color: 'error'
    })
  } finally {
    deletingId.value = null
  }
}

onMounted(() => {
  loadList()
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-infra-llm">
    <template #header>
      <AdminNavbar title="模型设置" />
    </template>

    <template #body>
      <div class="space-y-4">
        <AdminPageHeader
          title="大模型连接"
          description="配置组织可用的 OpenAI 兼容模型；智能体仅引用编码（modelRef），密钥不会返回前端"
        >
          <template
            v-if="canUpdate"
            #actions
          >
            <UButton
              icon="i-lucide-plus"
              color="primary"
              @click="openCreate"
            >
              新建连接
            </UButton>
          </template>
        </AdminPageHeader>

        <div
          v-if="!canQuery"
          class="max-w-3xl"
        >
          <UCard>
            <UEmpty
              icon="i-lucide-shield-alert"
              title="无访问权限"
              description="需要 agent:llm:query 权限"
            />
          </UCard>
        </div>

        <template v-else>
          <div
            v-if="store.loading"
            class="max-w-3xl space-y-3"
          >
            <USkeleton class="h-24 w-full" />
            <USkeleton class="h-48 w-full" />
          </div>

          <div
            v-else
            class="grid max-w-5xl gap-4 lg:grid-cols-2"
          >
            <UCard>
              <div class="space-y-3">
                <h3 class="text-sm font-medium">
                  已配置连接
                </h3>
                <UEmpty
                  v-if="store.connections.length === 0"
                  icon="i-lucide-bot"
                  title="暂无模型连接"
                  description="新建后可在智能体配置中选择"
                />
                <ul
                  v-else
                  class="divide-y divide-default"
                >
                  <li
                    v-for="row in store.connections"
                    :key="String(row.id)"
                    class="flex items-start justify-between gap-3 py-3"
                  >
                    <div class="min-w-0">
                      <div class="flex flex-wrap items-center gap-2">
                        <span class="truncate font-medium">{{ row.name }}</span>
                        <UBadge
                          :color="row.enabled ? 'success' : 'neutral'"
                          variant="subtle"
                        >
                          {{ row.enabled ? '已启用' : '已停用' }}
                        </UBadge>
                      </div>
                      <p class="mt-1 truncate text-xs text-muted">
                        {{ row.code }} · {{ row.modelName }}
                      </p>
                      <p class="truncate text-xs text-muted">
                        {{ row.baseUrl }}
                      </p>
                    </div>
                    <div class="flex shrink-0 gap-1">
                      <UButton
                        size="xs"
                        variant="ghost"
                        icon="i-lucide-pencil"
                        :disabled="!canUpdate"
                        @click="openEdit(row)"
                      />
                      <UButton
                        size="xs"
                        variant="ghost"
                        color="error"
                        icon="i-lucide-trash-2"
                        :disabled="!canUpdate"
                        :loading="deletingId === row.id"
                        @click="askDelete(row)"
                      />
                    </div>
                  </li>
                </ul>
              </div>
            </UCard>

            <UCard v-if="editing">
              <div class="space-y-4">
                <div class="flex items-center justify-between gap-2">
                  <h3 class="text-sm font-medium">
                    {{ form.id == null ? '新建连接' : '编辑连接' }}
                  </h3>
                  <UButton
                    size="xs"
                    variant="ghost"
                    icon="i-lucide-x"
                    @click="editing = false"
                  />
                </div>

                <UFormField
                  label="引用编码 (modelRef)"
                  required
                >
                  <UInput
                    v-model="form.code"
                    :disabled="!canUpdate || form.id != null"
                    placeholder="例如 openai-gpt-4o-mini"
                    class="w-full"
                  />
                </UFormField>
                <UFormField
                  label="显示名称"
                  required
                >
                  <UInput
                    v-model="form.name"
                    :disabled="!canUpdate"
                    class="w-full"
                  />
                </UFormField>
                <UFormField
                  label="Base URL"
                  required
                >
                  <UInput
                    v-model="form.baseUrl"
                    :disabled="!canUpdate"
                    placeholder="https://api.openai.com/v1"
                    class="w-full"
                  />
                </UFormField>
                <UFormField
                  label="模型名"
                  required
                >
                  <UInput
                    v-model="form.modelName"
                    :disabled="!canUpdate"
                    placeholder="gpt-4o-mini"
                    class="w-full"
                  />
                </UFormField>
                <UFormField :label="form.apiKeyConfigured ? 'API Key（留空则保留原密钥）' : 'API Key'">
                  <UInput
                    v-model="form.apiKey"
                    type="password"
                    :disabled="!canUpdate"
                    placeholder="sk-..."
                    class="w-full"
                  />
                </UFormField>
                <UFormField label="启用">
                  <USwitch
                    v-model="form.enabled"
                    :disabled="!canUpdate"
                  />
                </UFormField>

                <div class="flex flex-wrap gap-2">
                  <UButton
                    v-if="canUpdate"
                    color="primary"
                    :loading="saving"
                    @click="handleSave"
                  >
                    保存
                  </UButton>
                  <UButton
                    v-if="canTest"
                    variant="outline"
                    :loading="testing"
                    @click="handleTest"
                  >
                    测试连通性
                  </UButton>
                </div>
              </div>
            </UCard>
          </div>
        </template>
      </div>

      <UModal v-model:open="deleteOpen">
        <template #content>
          <div class="space-y-4 p-4">
            <h3 class="font-medium">
              删除模型连接？
            </h3>
            <p class="text-sm text-muted">
              将删除「{{ deleteTarget?.name }}」（{{ deleteTarget?.code }}）。已引用该编码的智能体需另行调整。
            </p>
            <div class="flex justify-end gap-2">
              <UButton
                variant="ghost"
                @click="deleteOpen = false"
              >
                取消
              </UButton>
              <UButton
                color="error"
                :loading="deletingId != null"
                @click="confirmDelete"
              >
                删除
              </UButton>
            </div>
          </div>
        </template>
      </UModal>
    </template>
  </UDashboardPanel>
</template>

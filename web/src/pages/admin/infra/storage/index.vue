<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { usePermission } from '../../../../composables/usePermission'
import { useStorageStore } from '../../../../stores/storage'

const toast = useToast()
const storageStore = useStorageStore()
const { hasPermission } = usePermission()

const testing = ref(false)
const saving = ref(false)
const deleting = ref(false)
const deleteOpen = ref(false)

const form = reactive({
  endpoint: '',
  bucket: '',
  accessKey: '',
  secretKey: '',
  useSsl: false,
  pathPrefix: '',
  isDefault: true
})

const canQuery = computed(() => hasPermission('infra:storage:query'))
const canUpdate = computed(() => hasPermission('infra:storage:update'))
const canTest = computed(() => hasPermission('infra:storage:test'))

const minioProvider = computed(() => storageStore.findMinioProvider())
const hasSavedConfig = computed(() => minioProvider.value != null)
const secretKeyConfigured = computed(() => minioProvider.value?.secretKeyConfigured === true)

const statusLabel = computed(() => {
  if (!minioProvider.value) {
    return '未配置（使用 Bootstrap）'
  }
  const parts = [minioProvider.value.status === 'legacy' ? 'legacy' : 'active']
  if (minioProvider.value.isDefault) {
    parts.push('默认')
  }
  return parts.join(' · ')
})

function applyProviderToForm() {
  const provider = minioProvider.value
  if (!provider) {
    form.endpoint = ''
    form.bucket = 'relayflow'
    form.accessKey = ''
    form.secretKey = ''
    form.useSsl = false
    form.pathPrefix = ''
    form.isDefault = true
    return
  }

  form.endpoint = provider.endpoint ?? ''
  form.bucket = provider.bucket ?? ''
  form.accessKey = provider.accessKey ?? ''
  form.secretKey = ''
  form.useSsl = provider.useSsl ?? false
  form.pathPrefix = provider.pathPrefix ?? ''
  form.isDefault = provider.isDefault ?? false
}

async function loadConfig() {
  if (!canQuery.value) {
    return
  }

  try {
    await storageStore.fetchConfig()
    applyProviderToForm()
  } catch {
    toast.add({
      title: '加载失败',
      description: storageStore.lastError ?? '无法获取存储配置',
      color: 'error'
    })
  }
}

function buildSavePayload() {
  return {
    provider: 'minio',
    endpoint: form.endpoint.trim(),
    bucket: form.bucket.trim(),
    accessKey: form.accessKey.trim(),
    secretKey: form.secretKey.trim() || undefined,
    useSsl: form.useSsl,
    pathPrefix: form.pathPrefix.trim(),
    isDefault: form.isDefault
  }
}

function buildTestPayload() {
  const useInline = form.secretKey.trim().length > 0 || !secretKeyConfigured.value
  if (!useInline) {
    return { provider: 'minio' }
  }

  return {
    provider: 'minio',
    endpoint: form.endpoint.trim(),
    bucket: form.bucket.trim(),
    accessKey: form.accessKey.trim(),
    secretKey: form.secretKey.trim(),
    useSsl: form.useSsl,
    pathPrefix: form.pathPrefix.trim()
  }
}

function validateForm(requireSecret: boolean) {
  if (!form.endpoint.trim() || !form.bucket.trim() || !form.accessKey.trim()) {
    toast.add({ title: '请填写 Endpoint、Bucket 与 Access Key', color: 'warning' })
    return false
  }

  if (requireSecret && !form.secretKey.trim() && !secretKeyConfigured.value) {
    toast.add({ title: '首次保存须填写 Secret Key', color: 'warning' })
    return false
  }

  if (requireSecret && !form.secretKey.trim() && !hasSavedConfig.value) {
    toast.add({ title: '测试连接须填写 Secret Key', color: 'warning' })
    return false
  }

  return true
}

async function handleTestConnection() {
  if (!validateForm(true)) {
    return
  }

  testing.value = true
  try {
    await storageStore.testConnection(buildTestPayload())
    toast.add({ title: '连接成功', color: 'success' })
  } catch (error) {
    toast.add({
      title: '连接失败',
      description: error instanceof Error ? error.message : '无法连接对象存储',
      color: 'error'
    })
  } finally {
    testing.value = false
  }
}

async function handleSave() {
  if (!validateForm(!secretKeyConfigured.value)) {
    return
  }

  saving.value = true
  try {
    await storageStore.save(buildSavePayload())
    applyProviderToForm()
    toast.add({ title: '配置已保存', color: 'success' })
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

async function confirmDelete() {
  deleting.value = true
  try {
    await storageStore.remove('minio')
    applyProviderToForm()
    deleteOpen.value = false
    toast.add({ title: '租户配置已删除', description: '运行时将回退 Bootstrap 配置', color: 'success' })
  } catch (error) {
    toast.add({
      title: '删除失败',
      description: error instanceof Error ? error.message : '操作失败',
      color: 'error'
    })
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-infra-storage">
    <template #header>
      <AdminNavbar title="存储设置" />
    </template>

    <template #body>
      <div class="space-y-4 p-4 sm:p-6">
        <AdminPageHeader
          title="对象存储"
          description="配置租户 MinIO 连接；未配置时使用 application.yml Bootstrap 默认值"
        />

        <div v-if="!canQuery" class="max-w-2xl">
          <UCard>
            <UEmpty
              icon="i-lucide-shield-alert"
              title="无访问权限"
              description="需要 infra:storage:query 权限"
            />
          </UCard>
        </div>

        <template v-else>
          <UCard class="max-w-2xl">
            <div v-if="storageStore.loading" class="space-y-3 py-2">
              <USkeleton class="h-8 w-full" />
              <USkeleton class="h-8 w-full" />
              <USkeleton class="h-8 w-2/3" />
            </div>

            <div v-else class="space-y-5">
              <div class="flex flex-wrap items-center gap-2">
                <UBadge color="neutral" variant="subtle">
                  MinIO
                </UBadge>
                <UBadge :color="hasSavedConfig ? 'success' : 'warning'" variant="subtle">
                  {{ statusLabel }}
                </UBadge>
              </div>

              <p v-if="!hasSavedConfig" class="text-sm text-muted">
                当前租户尚未保存自定义配置，保存后将写入数据库并可用于文件上传。
              </p>

              <div class="space-y-4">
                <UFormField label="Endpoint" required>
                  <UInput
                    v-model="form.endpoint"
                    placeholder="http://127.0.0.1:9000"
                    class="w-full"
                    :disabled="!canUpdate"
                  />
                </UFormField>

                <UFormField label="Bucket" required>
                  <UInput
                    v-model="form.bucket"
                    placeholder="relayflow"
                    class="w-full"
                    :disabled="!canUpdate"
                  />
                </UFormField>

                <UFormField label="Access Key" required>
                  <UInput
                    v-model="form.accessKey"
                    placeholder="minioadmin"
                    class="w-full"
                    :disabled="!canUpdate"
                  />
                </UFormField>

                <UFormField
                  label="Secret Key"
                  :hint="secretKeyConfigured ? '已配置密钥；留空表示不修改' : '首次保存必填'"
                >
                  <UInput
                    v-model="form.secretKey"
                    type="password"
                    placeholder="••••••••"
                    class="w-full"
                    :disabled="!canUpdate"
                  />
                </UFormField>

                <UFormField label="Path Prefix" hint="可选，对象 key 前缀">
                  <UInput
                    v-model="form.pathPrefix"
                    placeholder="tenant-files/"
                    class="w-full"
                    :disabled="!canUpdate"
                  />
                </UFormField>

                <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
                  <UCheckbox
                    v-model="form.useSsl"
                    label="使用 SSL"
                    :disabled="!canUpdate"
                  />
                  <UCheckbox
                    v-model="form.isDefault"
                    label="设为租户默认存储"
                    :disabled="!canUpdate"
                  />
                </div>
              </div>

              <div class="flex flex-wrap gap-2 pt-2">
                <UButton
                  v-if="canTest"
                  icon="i-lucide-plug-zap"
                  color="neutral"
                  variant="soft"
                  :loading="testing"
                  @click="handleTestConnection"
                >
                  测试连接
                </UButton>
                <UButton
                  v-if="canUpdate"
                  icon="i-lucide-save"
                  :loading="saving"
                  @click="handleSave"
                >
                  保存配置
                </UButton>
                <UButton
                  v-if="canUpdate && hasSavedConfig"
                  icon="i-lucide-trash-2"
                  color="error"
                  variant="soft"
                  @click="deleteOpen = true"
                >
                  删除配置
                </UButton>
              </div>
            </div>
          </UCard>
        </template>
      </div>

      <UModal v-model:open="deleteOpen" title="删除存储配置">
        <template #body>
          <p class="text-sm text-muted">
            确定删除租户 MinIO 配置吗？若仍有文件引用该 provider 将无法删除。删除后将回退 Bootstrap 配置。
          </p>
        </template>

        <template #footer>
          <div class="flex justify-end gap-2">
            <UButton color="neutral" variant="ghost" @click="deleteOpen = false">
              取消
            </UButton>
            <UButton color="error" :loading="deleting" @click="confirmDelete">
              删除
            </UButton>
          </div>
        </template>
      </UModal>
    </template>
  </UDashboardPanel>
</template>

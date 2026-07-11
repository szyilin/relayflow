<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { StorageEffectiveSource } from '../../../../api/admin/storage'
import AdminNavbar from '../../../../components/admin/AdminNavbar.vue'
import AdminPageHeader from '../../../../components/admin/AdminPageHeader.vue'
import { usePermission } from '../../../../composables/usePermission'
import { useStorageStore } from '../../../../stores/storage'

const toast = useToast()
const storageStore = useStorageStore()
const { hasPermission } = usePermission()

const testingBootstrap = ref(false)
const testingTenant = ref(false)
const saving = ref(false)
const deleting = ref(false)
const switchingSource = ref(false)
const deleteOpen = ref(false)

const form = reactive({
  endpoint: '',
  bucket: '',
  accessKey: '',
  secretKey: '',
  useSsl: false,
  pathPrefix: ''
})

const canQuery = computed(() => hasPermission('infra:storage:query'))
const canUpdate = computed(() => hasPermission('infra:storage:update'))
const canTest = computed(() => hasPermission('infra:storage:test'))

const minioProvider = computed(() => storageStore.findMinioProvider())
const hasSavedConfig = computed(() => minioProvider.value != null)
const secretKeyConfigured = computed(() => minioProvider.value?.secretKeyConfigured === true)

const effectiveSourceLabel = computed(() => (
  storageStore.isTenantEffective ? '租户自定义' : '系统默认（Bootstrap）'
))

const bootstrapProviderLabel = computed(() => {
  const provider = storageStore.bootstrap.provider
  return provider ? provider.toUpperCase() : '—'
})

function applyProviderToForm() {
  const provider = minioProvider.value
  if (!provider) {
    form.endpoint = ''
    form.bucket = ''
    form.accessKey = ''
    form.secretKey = ''
    form.useSsl = false
    form.pathPrefix = ''
    return
  }

  form.endpoint = provider.endpoint ?? ''
  form.bucket = provider.bucket ?? ''
  form.accessKey = provider.accessKey ?? ''
  form.secretKey = ''
  form.useSsl = provider.useSsl ?? false
  form.pathPrefix = provider.pathPrefix ?? ''
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

async function handleEffectiveSourceChange(source: StorageEffectiveSource) {
  if (source === storageStore.effectiveSource) {
    return
  }

  if (source === 'tenant' && !hasSavedConfig.value) {
    toast.add({
      title: '请先保存租户自定义配置',
      description: '切换到租户自定义前，需要先填写并保存 MinIO 连接信息',
      color: 'warning'
    })
    return
  }

  switchingSource.value = true
  try {
    await storageStore.setEffectiveSource(source)
    toast.add({
      title: '生效配置已更新',
      description: source === 'bootstrap' ? '新上传将使用系统默认配置' : '新上传将使用租户自定义配置',
      color: 'success'
    })
  } catch (error) {
    toast.add({
      title: '切换失败',
      description: error instanceof Error ? error.message : '操作失败',
      color: 'error'
    })
  } finally {
    switchingSource.value = false
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
    pathPrefix: form.pathPrefix.trim()
  }
}

function buildTenantTestPayload() {
  const useInline = form.secretKey.trim().length > 0 || !secretKeyConfigured.value
  if (!useInline) {
    return { source: 'tenant' as const, provider: 'minio' }
  }

  return {
    source: 'tenant' as const,
    provider: 'minio',
    endpoint: form.endpoint.trim(),
    bucket: form.bucket.trim(),
    accessKey: form.accessKey.trim(),
    secretKey: form.secretKey.trim(),
    useSsl: form.useSsl,
    pathPrefix: form.pathPrefix.trim()
  }
}

function validateTenantForm(requireSecret: boolean) {
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

async function handleTestBootstrap() {
  testingBootstrap.value = true
  try {
    await storageStore.testConnection({ source: 'bootstrap' })
    toast.add({ title: '系统默认连接成功', color: 'success' })
  } catch (error) {
    toast.add({
      title: '系统默认连接失败',
      description: error instanceof Error ? error.message : '无法连接对象存储',
      color: 'error'
    })
  } finally {
    testingBootstrap.value = false
  }
}

async function handleTestTenant() {
  if (!validateTenantForm(true)) {
    return
  }

  testingTenant.value = true
  try {
    await storageStore.testConnection(buildTenantTestPayload())
    toast.add({ title: '租户配置连接成功', color: 'success' })
  } catch (error) {
    toast.add({
      title: '租户配置连接失败',
      description: error instanceof Error ? error.message : '无法连接对象存储',
      color: 'error'
    })
  } finally {
    testingTenant.value = false
  }
}

async function handleSave() {
  if (!validateTenantForm(!secretKeyConfigured.value)) {
    return
  }

  saving.value = true
  try {
    await storageStore.save(buildSavePayload())
    applyProviderToForm()
    toast.add({ title: '租户配置已保存', color: 'success' })
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
    toast.add({
      title: '租户配置已删除',
      description: '生效配置已回退为系统默认',
      color: 'success'
    })
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
          description="系统默认由部署环境提供且不展示敏感信息；租户可另存一套 MinIO 并选择生效来源"
        />

        <div v-if="!canQuery" class="max-w-3xl">
          <UCard>
            <UEmpty
              icon="i-lucide-shield-alert"
              title="无访问权限"
              description="需要 infra:storage:query 权限"
            />
          </UCard>
        </div>

        <template v-else>
          <div v-if="storageStore.loading" class="max-w-3xl space-y-3">
            <USkeleton class="h-24 w-full" />
            <USkeleton class="h-48 w-full" />
            <USkeleton class="h-64 w-full" />
          </div>

          <div v-else class="max-w-3xl space-y-4">
            <UCard>
              <div class="space-y-4">
                <div class="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h3 class="text-sm font-medium">
                      当前生效配置
                    </h3>
                    <p class="mt-1 text-sm text-muted">
                      新文件上传将使用此来源的连接信息
                    </p>
                  </div>
                  <UBadge :color="storageStore.isTenantEffective ? 'primary' : 'success'" variant="subtle">
                    {{ effectiveSourceLabel }}
                  </UBadge>
                </div>

                <UFormField label="选择生效来源">
                  <div class="inline-flex flex-wrap gap-2 rounded-lg border border-default p-1">
                    <UButton
                      size="sm"
                      :variant="storageStore.effectiveSource === 'bootstrap' ? 'solid' : 'ghost'"
                      :loading="switchingSource && storageStore.effectiveSource !== 'bootstrap'"
                      :disabled="!canUpdate"
                      @click="handleEffectiveSourceChange('bootstrap')"
                    >
                      系统默认
                    </UButton>
                    <UButton
                      size="sm"
                      :variant="storageStore.effectiveSource === 'tenant' ? 'solid' : 'ghost'"
                      :loading="switchingSource && storageStore.effectiveSource !== 'tenant'"
                      :disabled="!canUpdate || !hasSavedConfig"
                      @click="handleEffectiveSourceChange('tenant')"
                    >
                      租户自定义
                    </UButton>
                  </div>
                </UFormField>
                <p v-if="!hasSavedConfig" class="text-xs text-muted">
                  保存租户自定义配置后，才可切换为「租户自定义」生效。
                </p>
              </div>
            </UCard>

            <UCard>
              <div class="space-y-4">
                <div class="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h3 class="text-sm font-medium">
                      系统默认（Bootstrap）
                    </h3>
                    <p class="mt-1 text-sm text-muted">
                      来自 `application.yml` / 部署环境，不在管理端展示 Endpoint 或密钥
                    </p>
                  </div>
                  <UBadge :color="storageStore.bootstrap.available ? 'success' : 'warning'" variant="subtle">
                    {{ storageStore.bootstrap.available ? '已启用' : '未配置' }}
                  </UBadge>
                </div>

                <dl class="grid gap-3 text-sm sm:grid-cols-2">
                  <div>
                    <dt class="text-muted">
                      Provider
                    </dt>
                    <dd class="mt-1 font-medium">
                      {{ bootstrapProviderLabel }}
                    </dd>
                  </div>
                  <div>
                    <dt class="text-muted">
                      凭据状态
                    </dt>
                    <dd class="mt-1 font-medium">
                      {{ storageStore.bootstrap.credentialsConfigured ? '已由部署环境配置' : '未完成部署配置' }}
                    </dd>
                  </div>
                </dl>

                <UButton
                  v-if="canTest && storageStore.bootstrap.available"
                  icon="i-lucide-plug-zap"
                  color="neutral"
                  variant="soft"
                  :loading="testingBootstrap"
                  @click="handleTestBootstrap"
                >
                  测试系统默认连接
                </UButton>
              </div>
            </UCard>

            <UCard>
              <div class="space-y-5">
                <div>
                  <h3 class="text-sm font-medium">
                    租户自定义 MinIO
                  </h3>
                  <p class="mt-1 text-sm text-muted">
                    仅保存到当前租户数据库；与系统默认相互独立
                  </p>
                </div>

                <div class="space-y-4">
                  <UFormField label="Endpoint" required>
                    <UInput
                      v-model="form.endpoint"
                      placeholder="请输入 MinIO 服务地址"
                      class="w-full"
                      :disabled="!canUpdate"
                    />
                  </UFormField>

                  <UFormField label="Bucket" required>
                    <UInput
                      v-model="form.bucket"
                      placeholder="请输入 Bucket 名称"
                      class="w-full"
                      :disabled="!canUpdate"
                    />
                  </UFormField>

                  <UFormField label="Access Key" required>
                    <UInput
                      v-model="form.accessKey"
                      placeholder="请输入 Access Key"
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
                      placeholder="例如 files/"
                      class="w-full"
                      :disabled="!canUpdate"
                    />
                  </UFormField>

                  <UCheckbox
                    v-model="form.useSsl"
                    label="使用 SSL"
                    :disabled="!canUpdate"
                  />
                </div>

                <div class="flex flex-wrap gap-2">
                  <UButton
                    v-if="canTest"
                    icon="i-lucide-plug-zap"
                    color="neutral"
                    variant="soft"
                    :loading="testingTenant"
                    @click="handleTestTenant"
                  >
                    测试租户配置
                  </UButton>
                  <UButton
                    v-if="canUpdate"
                    icon="i-lucide-save"
                    :loading="saving"
                    @click="handleSave"
                  >
                    保存租户配置
                  </UButton>
                  <UButton
                    v-if="canUpdate && hasSavedConfig"
                    icon="i-lucide-trash-2"
                    color="error"
                    variant="soft"
                    @click="deleteOpen = true"
                  >
                    删除租户配置
                  </UButton>
                </div>
              </div>
            </UCard>
          </div>
        </template>
      </div>

      <UModal v-model:open="deleteOpen" title="删除租户配置">
        <template #body>
          <p class="text-sm text-muted">
            确定删除租户 MinIO 配置吗？若仍有文件引用该 provider 将无法删除。删除后生效来源将回退为系统默认。
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

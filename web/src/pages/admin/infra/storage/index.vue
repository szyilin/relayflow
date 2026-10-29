<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { StorageEffectiveSource, StorageRevision } from '../../../../api/admin/storage'
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
const createNewLocation = ref(false)
const rotating = ref(false)
const rotation = reactive({ open: false, revisionId: '', accessKey: '', secretKey: '' })
function openRotation(revision: StorageRevision) {
  rotation.revisionId = revision.id; rotation.accessKey = ''; rotation.secretKey = ''; rotation.open = true
}
async function rotateCredentials() {
  if (!rotation.accessKey.trim() || !rotation.secretKey.trim()) return
  rotating.value = true
  try {
    await storageStore.save({ provider: 'minio', operation: 'rotate_credentials', revisionId: rotation.revisionId, accessKey: rotation.accessKey.trim(), secretKey: rotation.secretKey.trim() })
    rotation.secretKey = ''; rotation.open = false; applyProviderToForm()
    toast.add({ title: '该位置凭据已轮换', color: 'success' })
  } catch (error) { toast.add({ title: '轮换失败，原凭据已保留', description: error instanceof Error ? error.message : '操作失败', color: 'error' }) }
  finally { rotating.value = false }
}

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
  if (source === storageStore.effectiveSource && (source === 'bootstrap' || minioProvider.value?.revisionId === minioProvider.value?.activeRevisionId)) {
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
    createNewLocation: createNewLocation.value,
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
    createNewLocation.value = false
    toast.add({ title: '草稿已保存，请显式启用该位置', color: 'success' })
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
      <div class="space-y-4">
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
            <UAlert v-if="storageStore.unresolvedFiles || storageStore.unresolvedSessions" color="warning" title="历史位置尚未解析"
              :description="`${storageStore.unresolvedFiles} 个文件、${storageStore.unresolvedSessions} 个上传会话需要确认原位置。管理员完成映射后恢复读取；未解析的上传请重新发起。`" />
            <UCard>
              <template #header><h3 class="text-sm font-medium">历史存储位置</h3></template>
              <p class="mb-3 text-sm text-muted">历史位置保留对象原址。凭据轮换影响该位置全部历史文件及在途上传；验证失败会保留原凭据。</p>
              <div v-if="!storageStore.revisions.length" class="text-sm text-muted">尚无已记录的位置，首次上传时自动记录当前目标。</div>
              <div v-for="revision in storageStore.revisions" :key="revision.id" class="flex flex-wrap items-center justify-between gap-2 border-t border-default py-3 text-sm">
                <div><div class="font-medium">{{ revision.bucket }} · {{ revision.source === 'bootstrap' ? '系统默认' : '租户自定义' }}</div>
                  <div class="text-xs text-muted">位置 {{ revision.id }} · {{ revision.fileReferences }} 个文件 · {{ revision.sessionReferences }} 个在途会话</div>
                </div>
                <UButton v-if="canUpdate" size="xs" color="neutral" variant="soft" @click="openRotation(revision)">轮换此位置凭据</UButton>
              </div>
            </UCard>
            <UCard>
              <div class="space-y-4">
                <div class="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h3 class="text-sm font-medium">
                      当前生效配置
                    </h3>
                    <p class="mt-1 text-sm text-muted">
                      切换只影响新上传；已有文件和在途上传继续使用原位置
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
                      启用已保存的租户位置
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
                    保存草稿与启用分开；更改位置须另存，轮换凭据请在历史位置中操作
                  </p>
                </div>

                <UCheckbox v-model="createNewLocation" label="明确另存为新位置（保留旧文件位置）" :disabled="!canUpdate" />
                <p v-if="minioProvider?.revisionId" class="text-xs text-muted">
                  草稿位置 {{ minioProvider.revisionId }} · 当前上传位置 {{ storageStore.isTenantEffective ? (minioProvider.activeRevisionId ?? '待确认') : '系统默认' }}
                </p>
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
    <UModal v-model:open="rotation.open" title="轮换同一位置的凭据">
      <template #body><div class="space-y-4">
        <p class="text-sm text-muted">仅替换位置 {{ rotation.revisionId }} 的凭据，影响它的全部历史引用。位置、桶和对象路径保持不变，连接验证成功后才保存。</p>
        <UFormField label="新 Access Key"><UInput v-model="rotation.accessKey" /></UFormField>
        <UFormField label="新 Secret Key"><UInput v-model="rotation.secretKey" type="password" autocomplete="new-password" /></UFormField>
      </div></template>
      <template #footer><UButton :loading="rotating" :disabled="!rotation.accessKey || !rotation.secretKey" @click="rotateCredentials">验证并轮换</UButton></template>
    </UModal>
</template>

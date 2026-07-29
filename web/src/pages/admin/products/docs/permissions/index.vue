<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AdminNavbar from '../../../../../components/admin/AdminNavbar.vue'
import AdminDocsProductShell from '../../../../../components/admin/products/AdminDocsProductShell.vue'
import { usePermission } from '../../../../../composables/usePermission'
import {
  cloneDocsProductSettings
} from '../../../../../api/admin/docsProductSettings'
import { useDocsProductSettingsStore } from '../../../../../stores/docsProductSettings'

const toast = useToast()
const { hasPermission } = usePermission()
const settingsStore = useDocsProductSettingsStore()

/** Prefer docs:* ; fall back to im:* until re-login picks up B-api permission seeds. */
const canQuery = computed(() =>
  hasPermission('docs:product-settings:query') || hasPermission('im:product-settings:query'))
const canUpdate = computed(() =>
  hasPermission('docs:product-settings:update') || hasPermission('im:product-settings:update'))

const form = reactive(cloneDocsProductSettings(settingsStore.settings))
const dirty = ref(false)

function applyFromStore() {
  Object.assign(form, cloneDocsProductSettings(settingsStore.settings))
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
    await settingsStore.save(cloneDocsProductSettings(form))
    applyFromStore()
    toast.add({
      title: settingsStore.usingLocalDraft ? '已保存到本地草稿' : '已保存',
      description: settingsStore.usingLocalDraft
        ? 'Admin API 就绪后将写入服务端'
        : undefined,
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

watch(() => form.memberShareEnabled, () => { dirty.value = true })
watch(() => form.markdownExportEnabled, () => { dirty.value = true })
watch(() => form.driveUploadEnabled, () => { dirty.value = true })

onMounted(() => {
  void loadPage()
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-docs-permissions">
    <template #header>
      <AdminNavbar title="云文档 · 权限与分享" />
    </template>

    <template #body>
      <div class="flex flex-col gap-6 p-6">
        <UAlert
          v-if="!canQuery"
          color="warning"
          variant="subtle"
          title="无权限"
          description="需要 docs:product-settings:query 权限。权限种子将在 B-api 波次写入；开发期可用超级管理员全量权限验证。"
        />

        <AdminDocsProductShell
          v-else
          title="权限与分享"
          description="组织级云文档策略天花板。关闭后工作台对应写路径将被拒绝；已有分享授权仍可读，直至撤销。"
        >
          <template #actions>
            <UBadge
              v-if="settingsStore.usingLocalDraft"
              color="warning"
              variant="subtle"
            >
              本地草稿
            </UBadge>
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
                成员分享
              </h3>
              <p class="text-sm text-muted">
                是否允许文档所有者向本企业成员授予或变更分享（VIEW / EDIT）。关闭后仅禁止新建与变更，不影响已有授权的阅读。
              </p>
              <div class="space-y-2">
                <label
                  class="flex cursor-pointer gap-3 rounded-lg border border-default p-3"
                  :class="form.memberShareEnabled ? 'border-primary bg-primary/5' : ''"
                >
                  <input
                    v-model="form.memberShareEnabled"
                    type="radio"
                    class="mt-1"
                    :value="true"
                    :disabled="!canUpdate"
                  >
                  <span class="font-medium">允许分享给本企业成员</span>
                </label>
                <label
                  class="flex cursor-pointer gap-3 rounded-lg border border-default p-3"
                  :class="!form.memberShareEnabled ? 'border-primary bg-primary/5' : ''"
                >
                  <input
                    v-model="form.memberShareEnabled"
                    type="radio"
                    class="mt-1"
                    :value="false"
                    :disabled="!canUpdate"
                  >
                  <span class="font-medium">禁止新建或变更分享</span>
                </label>
              </div>
            </section>

            <section class="space-y-3 border-b border-default pb-8">
              <h3 class="font-semibold">
                Markdown 导出
              </h3>
              <p class="text-sm text-muted">
                是否允许将云文档导出为 Markdown。关闭后在线阅读不受影响。
              </p>
              <div class="space-y-2">
                <label
                  class="flex cursor-pointer gap-3 rounded-lg border border-default p-3"
                  :class="form.markdownExportEnabled ? 'border-primary bg-primary/5' : ''"
                >
                  <input
                    v-model="form.markdownExportEnabled"
                    type="radio"
                    class="mt-1"
                    :value="true"
                    :disabled="!canUpdate"
                  >
                  <span class="font-medium">允许导出 Markdown</span>
                </label>
                <label
                  class="flex cursor-pointer gap-3 rounded-lg border border-default p-3"
                  :class="!form.markdownExportEnabled ? 'border-primary bg-primary/5' : ''"
                >
                  <input
                    v-model="form.markdownExportEnabled"
                    type="radio"
                    class="mt-1"
                    :value="false"
                    :disabled="!canUpdate"
                  >
                  <span class="font-medium">禁止导出</span>
                </label>
              </div>
            </section>

            <section class="space-y-3">
              <h3 class="font-semibold">
                云盘上传
              </h3>
              <p class="text-sm text-muted">
                是否允许成员向云盘注册新的文件对象。关闭后已有文件仍可访问。
              </p>
              <div class="space-y-2">
                <label
                  class="flex cursor-pointer gap-3 rounded-lg border border-default p-3"
                  :class="form.driveUploadEnabled ? 'border-primary bg-primary/5' : ''"
                >
                  <input
                    v-model="form.driveUploadEnabled"
                    type="radio"
                    class="mt-1"
                    :value="true"
                    :disabled="!canUpdate"
                  >
                  <span class="font-medium">允许上传云盘文件</span>
                </label>
                <label
                  class="flex cursor-pointer gap-3 rounded-lg border border-default p-3"
                  :class="!form.driveUploadEnabled ? 'border-primary bg-primary/5' : ''"
                >
                  <input
                    v-model="form.driveUploadEnabled"
                    type="radio"
                    class="mt-1"
                    :value="false"
                    :disabled="!canUpdate"
                  >
                  <span class="font-medium">禁止上传</span>
                </label>
              </div>
            </section>

            <UAlert
              color="neutral"
              variant="subtle"
              title="关于外链与组织外分享"
              description="当前版本不提供外链或组织外访问。组织策略也不会以开关形式开放此类能力。"
            />
          </div>
        </AdminDocsProductShell>
      </div>
    </template>
  </UDashboardPanel>
</template>

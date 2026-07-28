<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AdminNavbar from '../../components/admin/AdminNavbar.vue'
import { usePermission } from '../../composables/usePermission'
import { useEnterpriseStore } from '../../stores/enterprise'
import { resolveAvatarUrl } from '../../utils/avatar'

const toast = useToast()
const enterpriseStore = useEnterpriseStore()
const { hasPermission } = usePermission()

const canQueryEnterprise = computed(() => hasPermission('system:enterprise:query'))

const quickLinks = [{
  label: '用户管理',
  description: '查看与管理系统用户',
  icon: 'i-lucide-users',
  to: '/admin/system/user'
}, {
  label: '文件管理',
  description: '浏览已上传的文件',
  icon: 'i-lucide-folder-open',
  to: '/admin/infra/file'
}, {
  label: '部门管理',
  description: '组织架构与部门',
  icon: 'i-lucide-building-2',
  to: '/admin/system/dept'
}]

const metrics = computed(() => {
  const data = enterpriseStore.overview
  return [{
    label: '组织总人数',
    value: data?.memberCount ?? 0,
    to: '/admin/system/user'
  }, {
    label: '部门数',
    value: data?.deptCount ?? 0,
    to: '/admin/system/dept'
  }, {
    label: '超级管理员',
    value: data?.superAdminCount ?? 0,
    to: '/admin/system/role'
  }, {
    label: '子管理员',
    value: data?.subAdminCount ?? 0,
    to: '/admin/system/role'
  }]
})

const logoUrl = computed(() => resolveAvatarUrl(enterpriseStore.overview?.logoFileId))
const nameInitial = computed(() => {
  const name = enterpriseStore.overview?.name?.trim()
  return name ? name.slice(0, 1) : '企'
})

onMounted(async () => {
  if (!canQueryEnterprise.value) {
    return
  }
  try {
    await enterpriseStore.loadOverview()
  } catch {
    toast.add({
      title: '加载失败',
      description: enterpriseStore.lastError ?? '无法加载概览统计',
      color: 'error'
    })
  }
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <UDashboardPanel id="admin-home">
    <template #header>
      <AdminNavbar title="概览" />
    </template>

    <template #body>
      <div class="space-y-6">
        <UCard>
          <UAlert
            v-if="!canQueryEnterprise"
            color="warning"
            title="无查看权限"
            description="需要 system:enterprise:query 权限以查看概览统计"
          />

          <div
            v-else-if="enterpriseStore.overviewLoading && !enterpriseStore.overview"
            class="space-y-4"
          >
            <USkeleton class="h-16 w-full" />
            <USkeleton class="h-20 w-full" />
          </div>

          <div
            v-else-if="enterpriseStore.overview"
            class="space-y-6"
          >
            <div class="flex items-start gap-4">
              <UAvatar
                v-if="logoUrl"
                :src="logoUrl"
                :alt="enterpriseStore.overview.name"
                size="3xl"
                class="size-14 rounded-lg"
              />
              <div
                v-else
                class="flex size-14 shrink-0 items-center justify-center rounded-lg bg-primary/15 text-xl font-semibold text-primary"
              >
                {{ nameInitial }}
              </div>

              <div class="min-w-0 space-y-1">
                <h2 class="truncate text-lg font-semibold text-highlighted">
                  {{ enterpriseStore.overview.name }}
                </h2>
                <p class="text-sm text-muted">
                  企业编号：
                  <span class="font-mono text-default">{{ enterpriseStore.overview.code }}</span>
                </p>
              </div>
            </div>

            <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <UButton
                v-for="metric in metrics"
                :key="metric.label"
                :to="metric.to"
                color="neutral"
                variant="soft"
                class="h-auto flex-col items-start gap-1 px-4 py-3"
              >
                <span class="text-xs font-normal text-muted">
                  {{ metric.label }}
                </span>
                <span class="text-2xl font-semibold tabular-nums text-highlighted">
                  {{ metric.value }}
                </span>
              </UButton>
            </div>
          </div>

          <UEmpty
            v-else
            icon="i-lucide-layout-dashboard"
            title="概览统计"
            description="暂无统计数据"
          />
        </UCard>

        <UCard>
          <template #header>
            <h3 class="font-semibold">
              快捷入口
            </h3>
          </template>

          <div class="grid gap-3 sm:grid-cols-3">
            <UButton
              v-for="link in quickLinks"
              :key="link.to"
              :to="link.to"
              color="neutral"
              variant="soft"
              class="h-auto flex-col items-start gap-2 p-4"
            >
              <div class="flex items-center gap-2 font-medium">
                <UIcon
                  :name="link.icon"
                  class="size-4 text-primary"
                />
                {{ link.label }}
              </div>
              <span class="text-xs font-normal text-muted">
                {{ link.description }}
              </span>
            </UButton>
          </div>
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>

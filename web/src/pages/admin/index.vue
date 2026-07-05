<script setup lang="ts">
import AdminNavbar from '../../components/admin/AdminNavbar.vue'
import { mockDashboardStats, mockQuickLinks } from '../../mocks/dashboard'
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
      <div class="space-y-6 p-4 sm:p-6">
        <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <UCard v-for="stat in mockDashboardStats" :key="stat.label">
            <div class="flex items-start justify-between gap-3">
              <div class="space-y-1">
                <p class="text-sm text-muted">
                  {{ stat.label }}
                </p>
                <p class="text-2xl font-semibold">
                  {{ stat.value }}
                </p>
                <p v-if="stat.change" class="text-xs text-muted">
                  {{ stat.change }}
                </p>
              </div>
              <div class="rounded-lg bg-primary/10 p-2 text-primary">
                <UIcon :name="stat.icon" class="size-5" />
              </div>
            </div>
          </UCard>
        </div>

        <UCard>
          <template #header>
            <h3 class="font-semibold">
              快捷入口
            </h3>
          </template>

          <div class="grid gap-3 sm:grid-cols-3">
            <UButton
              v-for="link in mockQuickLinks"
              :key="link.to"
              :to="link.to"
              color="neutral"
              variant="soft"
              class="h-auto flex-col items-start gap-2 p-4"
            >
              <div class="flex items-center gap-2 font-medium">
                <UIcon :name="link.icon" class="size-4 text-primary" />
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

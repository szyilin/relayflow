<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { useWorkspaceNav } from '../../composables/useWorkspaceNav'

const router = useRouter()
const authStore = useAuthStore()
const { railItems, activeRailId } = useWorkspaceNav()

const keyword = ref('')

const displayName = () => authStore.user?.nickname?.slice(0, 1) ?? '员'

async function logout() {
  await authStore.logout()
  router.push('/app/login')
}
</script>

<template>
  <aside class="workspace-rail workspace-card flex h-full shrink-0 flex-col overflow-hidden">
    <div class="space-y-3 border-b border-[var(--ws-border-subtle)] p-3">
      <div class="flex items-center gap-2">
        <RouterLink
          to="/app/messages"
          class="flex size-9 shrink-0 items-center justify-center rounded-xl bg-primary text-white shadow-sm"
          title="RelayFlow"
        >
          <UIcon name="i-lucide-workflow" class="size-4.5" />
        </RouterLink>
        <div class="min-w-0 flex-1">
          <p class="truncate text-sm font-semibold">
            RelayFlow
          </p>
          <p class="truncate text-xs text-[var(--ws-text-muted)]">
            工作台
          </p>
        </div>
        <UButton icon="i-lucide-plus" color="neutral" variant="ghost" square size="sm" class="size-8" />
      </div>

      <UInput
        v-model="keyword"
        placeholder="搜索 (⌘K)"
        icon="i-lucide-search"
        size="sm"
        class="workspace-search"
        disabled
      />
    </div>

    <nav class="flex-1 space-y-0.5 overflow-y-auto p-2">
      <RouterLink
        v-for="item in railItems"
        :key="item.id"
        :to="item.to"
        class="workspace-rail-btn relative flex items-center gap-3 rounded-xl px-3 py-2.5"
        :data-active="activeRailId === item.id"
      >
        <UIcon :name="item.icon" class="size-5 shrink-0" />
        <span class="truncate text-sm font-medium">{{ item.label }}</span>
        <span
          v-if="item.badge"
          class="ml-auto flex size-5 shrink-0 items-center justify-center rounded-full bg-error text-[10px] font-medium text-white"
        >
          {{ item.badge }}
        </span>
      </RouterLink>
    </nav>

    <div class="space-y-1 border-t border-[var(--ws-border-subtle)] p-2">
      <RouterLink
        v-if="authStore.isAdmin"
        to="/admin"
        class="workspace-rail-btn flex items-center gap-3 rounded-xl px-3 py-2 text-sm text-[var(--ws-text-muted)]"
      >
        <UIcon name="i-lucide-shield" class="size-4 shrink-0" />
        <span>管理后台</span>
      </RouterLink>

      <UDropdownMenu :items="[[{
        label: authStore.user?.nickname ?? '员工',
        type: 'label'
      }, {
        label: '退出登录',
        icon: 'i-lucide-log-out',
        onSelect: logout
      }]]">
        <button
          type="button"
          class="workspace-rail-btn flex w-full items-center gap-3 rounded-xl px-3 py-2 text-left"
        >
          <UAvatar
            :alt="displayName()"
            :text="displayName()"
            size="xs"
            class="ring-2 ring-primary/25"
          />
          <span class="truncate text-sm">{{ authStore.user?.nickname ?? '员工' }}</span>
          <UIcon name="i-lucide-chevron-up" class="ml-auto size-4 text-[var(--ws-text-muted)]" />
        </button>
      </UDropdownMenu>
    </div>
  </aside>
</template>

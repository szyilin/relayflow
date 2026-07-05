<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { useWorkspaceNav } from '../../composables/useWorkspaceNav'

const router = useRouter()
const authStore = useAuthStore()
const { railItems, activeRailId } = useWorkspaceNav()

const displayName = () => authStore.user?.nickname?.slice(0, 1) ?? '员'

function logout() {
  authStore.logout()
  router.push('/app/login')
}
</script>

<template>
  <aside class="workspace-rail flex h-full shrink-0 flex-col items-center py-3">
    <RouterLink
      to="/app/messages"
      class="mb-4 flex size-10 items-center justify-center rounded-xl bg-primary text-white shadow-sm"
      title="RelayFlow"
    >
      <UIcon name="i-lucide-workflow" class="size-5" />
    </RouterLink>

    <nav class="flex flex-1 flex-col items-center gap-1">
      <RouterLink
        v-for="item in railItems"
        :key="item.id"
        :to="item.to"
        class="workspace-rail-btn relative flex size-12 flex-col items-center justify-center rounded-2xl"
        :data-active="activeRailId === item.id"
        :title="item.label"
      >
        <UIcon :name="item.icon" class="size-5" />
        <span class="mt-0.5 max-w-full truncate px-0.5 text-[10px] leading-none">{{ item.label }}</span>
        <span
          v-if="item.badge"
          class="absolute right-1 top-1 flex size-4 items-center justify-center rounded-full bg-error text-[10px] font-medium text-white"
        >
          {{ item.badge }}
        </span>
      </RouterLink>
    </nav>

    <div class="mt-auto flex flex-col items-center gap-2 pb-1">
      <UButton
        icon="i-lucide-shield"
        color="neutral"
        variant="ghost"
        square
        size="sm"
        to="/admin"
        title="管理后台"
      />
      <UDropdownMenu :items="[[{
        label: authStore.user?.nickname ?? '员工',
        type: 'label'
      }, {
        label: '退出登录',
        icon: 'i-lucide-log-out',
        onSelect: logout
      }]]">
        <UAvatar
          :alt="displayName()"
          :text="displayName()"
          size="sm"
          class="cursor-pointer ring-2 ring-primary/30"
        />
      </UDropdownMenu>
    </div>
  </aside>
</template>

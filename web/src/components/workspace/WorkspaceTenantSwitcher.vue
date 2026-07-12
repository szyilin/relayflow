<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const toast = useToast()
const switching = ref(false)

const showSwitcher = computed(() => authStore.tenants.length > 1)

const menuItems = computed(() => {
  const currentId = authStore.tenantId
  return [authStore.tenants.map((tenant) => {
    const selected = tenant.tenantId === currentId
    return {
      label: tenant.tenantName,
      icon: selected ? 'i-lucide-check' : tenant.owner ? 'i-lucide-building-2' : 'i-lucide-users',
      disabled: selected,
      onSelect: () => selectTenant(tenant.tenantId)
    }
  })]
})

async function selectTenant(targetTenantId: number) {
  if (targetTenantId === authStore.tenantId || switching.value) {
    return
  }

  switching.value = true
  try {
    const result = await authStore.switchTenant(targetTenantId)
    if (!result.ok) {
      toast.add({ title: '切换失败', description: result.message, color: 'error' })
      return
    }
    toast.add({
      title: '已切换企业',
      description: authStore.activeTenantName,
      color: 'success'
    })
  } finally {
    switching.value = false
  }
}

onMounted(() => {
  if (authStore.isAuthenticated) {
    void authStore.fetchMyTenants()
  }
})
</script>

<template>
  <div v-if="authStore.isAuthenticated" class="min-w-0">
    <UDropdownMenu
      v-if="showSwitcher"
      :items="menuItems"
      :content="{ align: 'start' }"
    >
      <button
        type="button"
        class="flex w-full min-w-0 items-center gap-1.5 rounded-lg px-1 py-0.5 text-left transition-colors hover:bg-[var(--ws-hover-bg)]"
        :disabled="switching"
      >
        <UIcon name="i-lucide-building-2" class="size-3.5 shrink-0 text-[var(--ws-text-muted)]" />
        <span class="truncate text-xs font-medium text-[var(--ws-text-muted)]">
          {{ authStore.activeTenantName }}
        </span>
        <UIcon name="i-lucide-chevrons-up-down" class="size-3 shrink-0 text-[var(--ws-text-muted)]" />
      </button>
    </UDropdownMenu>
    <p
      v-else
      class="truncate text-xs text-[var(--ws-text-muted)]"
      :title="authStore.activeTenantName"
    >
      {{ authStore.activeTenantName }}
    </p>
  </div>
</template>

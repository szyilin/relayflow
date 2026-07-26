<script setup lang="ts">
import type { ContactsCategory } from '../../../stores/contacts'

defineProps<{
  category: ContactsCategory
  pendingRequestCount?: number
  tenantLabel?: string
}>()

const emit = defineEmits<{
  select: [ContactsCategory]
}>()

const items: { key: ContactsCategory, label: string, icon: string }[] = [
  { key: 'org', label: '组织内联系人', icon: 'i-lucide-network' },
  { key: 'external', label: '外部联系人', icon: 'i-lucide-globe' },
  { key: 'requests', label: '新的联系人', icon: 'i-lucide-user-plus' },
  { key: 'starred', label: '星标联系人', icon: 'i-lucide-star' },
  { key: 'groups', label: '我的群组', icon: 'i-lucide-users-round' }
]
</script>

<template>
  <div class="flex min-h-0 flex-1 flex-col">
    <div class="border-b border-[var(--ws-border-subtle)] px-4 py-3">
      <div class="font-semibold">
        通讯录
      </div>
      <p
        v-if="tenantLabel"
        class="mt-1 truncate text-xs text-[var(--ws-text-muted)]"
      >
        {{ tenantLabel }}
      </p>
    </div>

    <nav class="min-h-0 flex-1 space-y-0.5 overflow-y-auto p-2">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        class="workspace-list-item flex w-full items-center gap-2.5 px-3 py-2 text-left"
        :data-active="category === item.key ? 'true' : undefined"
        @click="emit('select', item.key)"
      >
        <UIcon
          :name="item.icon"
          class="size-4 shrink-0"
          :class="category === item.key ? 'text-[var(--ui-primary)]' : 'text-[var(--ws-text-muted)]'"
        />
        <span class="min-w-0 flex-1 truncate">{{ item.label }}</span>
        <span
          v-if="item.key === 'requests' && (pendingRequestCount ?? 0) > 0"
          class="rounded-md bg-[var(--ui-primary)]/15 px-1.5 py-0.5 text-[10px] font-medium text-[var(--ui-primary)]"
        >
          {{ pendingRequestCount }}
        </span>
      </button>
    </nav>
  </div>
</template>

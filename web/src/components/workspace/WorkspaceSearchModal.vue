<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useWorkspaceSearchStore } from '../../stores/workspaceSearch'
import type { WorkspaceSearchGroupType } from '../../api/app/workspace-search'

const GROUP_ICONS: Record<WorkspaceSearchGroupType, string> = {
  member: 'i-lucide-user',
  conversation: 'i-lucide-message-circle',
  task: 'i-lucide-check-square'
}

const open = defineModel<boolean>('open', { required: true })

const router = useRouter()
const searchStore = useWorkspaceSearchStore()
const { keyword, groups, loading, searched } = storeToRefs(searchStore)

const visibleGroups = computed(() => groups.value.filter(group => group.items.length > 0))

const inputRef = ref<{ inputRef?: HTMLInputElement } | null>(null)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

function scheduleSearch(value: string) {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(() => {
    void searchStore.search(value)
  }, 300)
}

watch(open, (isOpen) => {
  if (isOpen) {
    scheduleSearch(keyword.value)
    requestAnimationFrame(() => {
      inputRef.value?.inputRef?.focus()
    })
  } else {
    searchStore.clear()
  }
})

watch(keyword, (value) => {
  if (open.value) {
    scheduleSearch(value)
  }
})

async function handleSelect(route: string) {
  open.value = false
  await router.push(route)
}

function groupIcon(type: WorkspaceSearchGroupType) {
  return GROUP_ICONS[type] ?? 'i-lucide-search'
}
</script>

<template>
  <UModal v-model:open="open" title="搜索" :ui="{ content: 'max-w-lg' }">
    <template #body>
      <UInput
        ref="inputRef"
        v-model="keyword"
        placeholder="搜索联系人、消息、任务…"
        icon="i-lucide-search"
        autofocus
        class="mb-4"
      />

      <div v-if="loading" class="flex justify-center py-10">
        <UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-[var(--ws-text-muted)]" />
      </div>
      <UEmpty
        v-else-if="searched && !visibleGroups.length"
        icon="i-lucide-search-x"
        title="无匹配结果"
        description="试试其他关键词"
      />
      <div v-else class="max-h-[min(24rem,60vh)] space-y-4 overflow-y-auto">
        <section v-for="group in visibleGroups" :key="group.type">
          <p class="mb-2 text-xs font-medium uppercase tracking-wide text-[var(--ws-text-muted)]">
            {{ group.label }}
          </p>
          <ul class="space-y-1">
            <li v-for="item in group.items" :key="item.id">
              <button
                type="button"
                class="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left transition-colors hover:bg-[var(--ws-rail-hover)]"
                @click="handleSelect(item.route)"
              >
                <UIcon :name="groupIcon(group.type)" class="size-4 shrink-0 text-[var(--ws-text-muted)]" />
                <span class="min-w-0 flex-1">
                  <span class="block truncate text-sm font-medium">{{ item.title }}</span>
                  <span v-if="item.subtitle" class="block truncate text-xs text-[var(--ws-text-muted)]">
                    {{ item.subtitle }}
                  </span>
                </span>
              </button>
            </li>
          </ul>
        </section>
      </div>
    </template>
  </UModal>
</template>

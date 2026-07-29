<script setup lang="ts">
import { ref, watch } from 'vue'
import { searchMembers, type MemberSearchItem } from '../../api/app/member-search'
import WorkspaceOwnerChip from '../workspace/WorkspaceOwnerChip.vue'

export interface AdminMemberSelection {
  userId: string
  displayName: string
}

const props = withDefaults(defineProps<{
  modelValue: AdminMemberSelection | null
  label?: string
  placeholder?: string
  disabled?: boolean
}>(), {
  label: '成员',
  placeholder: '姓名、邮箱或手机号',
  disabled: false
})

const emit = defineEmits<{
  'update:modelValue': [value: AdminMemberSelection | null]
}>()

const keyword = ref('')
const hits = ref<MemberSearchItem[]>([])
const searching = ref(false)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch(keyword, (value) => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  const q = value.trim()
  if (q.length < 1) {
    hits.value = []
    return
  }
  debounceTimer = setTimeout(() => {
    void runSearch(q)
  }, 250)
})

async function runSearch(q: string) {
  searching.value = true
  try {
    hits.value = await searchMembers(q, 8)
  } catch {
    hits.value = []
  } finally {
    searching.value = false
  }
}

function selectHit(hit: MemberSearchItem) {
  emit('update:modelValue', {
    userId: hit.id,
    displayName: hit.title
  })
  keyword.value = ''
  hits.value = []
}

function clearSelection() {
  emit('update:modelValue', null)
}
</script>

<template>
  <UFormField
    :label="label"
    class="w-full min-w-[14rem]"
  >
    <div
      v-if="modelValue"
      class="flex items-center gap-2"
    >
      <WorkspaceOwnerChip
        :user-id="modelValue.userId"
        :display-name="modelValue.displayName"
      />
      <UButton
        size="xs"
        color="neutral"
        variant="ghost"
        icon="i-lucide-x"
        :disabled="disabled"
        @click="clearSelection"
      />
    </div>
    <div
      v-else
      class="relative space-y-1"
    >
      <UInput
        v-model="keyword"
        :placeholder="placeholder"
        :disabled="disabled"
        :loading="searching"
        icon="i-lucide-search"
      />
      <ul
        v-if="hits.length"
        class="absolute z-20 mt-1 max-h-48 w-full overflow-auto rounded-md border border-default bg-default py-1 shadow-sm"
      >
        <li
          v-for="hit in hits"
          :key="hit.id"
        >
          <button
            type="button"
            class="flex w-full flex-col items-start gap-0.5 px-3 py-2 text-left text-sm hover:bg-elevated"
            @click="selectHit(hit)"
          >
            <span class="font-medium">
              {{ hit.title }}
            </span>
            <span
              v-if="hit.subtitle"
              class="text-xs text-muted"
            >
              {{ hit.subtitle }}
            </span>
          </button>
        </li>
      </ul>
    </div>
  </UFormField>
</template>

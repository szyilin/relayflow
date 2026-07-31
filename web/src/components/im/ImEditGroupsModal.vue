<script setup lang="ts">
import { ref, watch } from 'vue'
import { useUserPreferenceStore } from '../../stores/userPreference'
import {
  IM_SIDEBAR_GROUP_ICONS,
  IM_SIDEBAR_GROUP_IDS,
  IM_SIDEBAR_GROUP_LABELS,
  resolveSidebarGroupPreferences,
  type ImSidebarGroupId
} from '../../composables/imConversationGroups'

const open = defineModel<boolean>('open', { default: false })

const preference = useUserPreferenceStore()
const toast = useToast()

const draftShown = ref<ImSidebarGroupId[]>([])
const draftHidden = ref<ImSidebarGroupId[]>([])
const saving = ref(false)
const dragFrom = ref<number | null>(null)

watch(open, (value) => {
  if (!value) {
    return
  }
  const resolved = resolveSidebarGroupPreferences(
    preference.settings.im.sidebarGroupOrder,
    preference.settings.im.sidebarGroupHidden
  )
  draftShown.value = [...resolved.order]
  draftHidden.value = [...resolved.hidden]
  const present = new Set([...draftShown.value, ...draftHidden.value])
  for (const id of IM_SIDEBAR_GROUP_IDS) {
    if (!present.has(id)) {
      draftShown.value.push(id)
    }
  }
})

function hideAt(index: number) {
  const [item] = draftShown.value.splice(index, 1)
  if (item) {
    draftHidden.value.push(item)
  }
}

function showAt(index: number) {
  const [item] = draftHidden.value.splice(index, 1)
  if (item) {
    draftShown.value.push(item)
  }
}

function onDragStart(index: number) {
  dragFrom.value = index
}

function onDrop(index: number) {
  const from = dragFrom.value
  dragFrom.value = null
  if (from == null || from === index) {
    return
  }
  const next = [...draftShown.value]
  const [item] = next.splice(from, 1)
  if (!item) {
    return
  }
  next.splice(index, 0, item)
  draftShown.value = next
}

async function save() {
  saving.value = true
  try {
    const ok = await preference.patchIm({
      sidebarGroupOrder: [...draftShown.value],
      sidebarGroupHidden: [...draftHidden.value]
    })
    if (!ok) {
      toast.add({
        title: '保存失败',
        description: preference.lastSyncError ?? '请稍后重试',
        color: 'error'
      })
      return
    }
    open.value = false
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <UModal v-model:open="open" title="编辑分组" :ui="{ content: 'sm:max-w-md' }">
    <template #body>
      <div class="space-y-5">
        <section>
          <h4 class="mb-2 text-xs font-semibold text-[var(--ws-text-muted)]">
            展示
          </h4>
          <ul class="space-y-1">
            <li
              v-for="(id, index) in draftShown"
              :key="id"
              class="flex items-center gap-2 rounded-md border border-[var(--ws-border-subtle)] px-2 py-1.5"
              draggable="true"
              @dragstart="onDragStart(index)"
              @dragover.prevent
              @drop="onDrop(index)"
            >
              <UIcon name="i-lucide-grip-vertical" class="size-4 text-[var(--ws-text-muted)]" />
              <UButton
                icon="i-lucide-minus"
                color="error"
                variant="soft"
                size="xs"
                class="!rounded-full"
                aria-label="隐藏"
                @click="hideAt(index)"
              />
              <UIcon :name="IM_SIDEBAR_GROUP_ICONS[id]" class="size-4" />
              <span class="min-w-0 flex-1 truncate text-sm">{{ IM_SIDEBAR_GROUP_LABELS[id] }}</span>
            </li>
          </ul>
        </section>

        <section>
          <h4 class="mb-2 text-xs font-semibold text-[var(--ws-text-muted)]">
            隐藏
          </h4>
          <ul v-if="draftHidden.length" class="space-y-1">
            <li
              v-for="(id, index) in draftHidden"
              :key="id"
              class="flex items-center gap-2 rounded-md border border-dashed border-[var(--ws-border-subtle)] px-2 py-1.5"
            >
              <UButton
                icon="i-lucide-plus"
                color="success"
                variant="soft"
                size="xs"
                class="!rounded-full"
                aria-label="展示"
                @click="showAt(index)"
              />
              <UIcon :name="IM_SIDEBAR_GROUP_ICONS[id]" class="size-4" />
              <span class="min-w-0 flex-1 truncate text-sm">{{ IM_SIDEBAR_GROUP_LABELS[id] }}</span>
            </li>
          </ul>
          <p v-else class="text-xs text-[var(--ws-text-muted)]">
            暂无隐藏分组
          </p>
        </section>
      </div>
    </template>
    <template #footer>
      <div class="flex justify-end gap-2">
        <UButton color="neutral" variant="ghost" @click="open = false">
          取消
        </UButton>
        <UButton color="primary" :loading="saving" @click="save">
          保存
        </UButton>
      </div>
    </template>
  </UModal>
</template>

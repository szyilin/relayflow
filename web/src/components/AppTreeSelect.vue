<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { AppTreeSelectItem } from '../utils/treeSelect'

const props = withDefaults(defineProps<{
  modelValue: string | null | undefined
  items: AppTreeSelectItem[]
  placeholder?: string
  disabled?: boolean
  /** Expand all branches when the popover opens */
  expandAllOnOpen?: boolean
}>(), {
  placeholder: '请选择',
  disabled: false,
  expandAllOnOpen: true
})

const emit = defineEmits<{
  'update:modelValue': [value: string | null | undefined]
}>()

const open = ref(false)
const expanded = ref<string[]>([])
const treeModel = ref<AppTreeSelectItem | undefined>()

function walk(
  nodes: AppTreeSelectItem[],
  visit: (node: AppTreeSelectItem) => void
) {
  for (const node of nodes) {
    visit(node)
    if (node.children?.length) {
      walk(node.children, visit)
    }
  }
}

function findByValue(
  nodes: AppTreeSelectItem[],
  value: string | null | undefined
): AppTreeSelectItem | undefined {
  let found: AppTreeSelectItem | undefined
  walk(nodes, (node) => {
    if (found) {
      return
    }
    if (value == null) {
      if (node.value == null) {
        found = node
      }
      return
    }
    if (node.value === value) {
      found = node
    }
  })
  return found
}

function collectExpandableIds(nodes: AppTreeSelectItem[]): string[] {
  const ids: string[] = []
  walk(nodes, (n) => {
    if (n.children?.length) {
      ids.push(n.id)
    }
  })
  return ids
}

const selectedItem = computed(() => findByValue(props.items, props.modelValue))

const displayLabel = computed(() => selectedItem.value?.label ?? props.placeholder)

function syncTreeModelFromValue() {
  treeModel.value = selectedItem.value
}

watch(
  () => [props.modelValue, props.items] as const,
  () => {
    syncTreeModelFromValue()
  },
  { immediate: true, deep: true }
)

watch(open, (isOpen) => {
  if (!isOpen) {
    return
  }
  syncTreeModelFromValue()
  if (props.expandAllOnOpen) {
    expanded.value = collectExpandableIds(props.items)
  }
})

function onTreeUpdate(item: AppTreeSelectItem | AppTreeSelectItem[] | undefined) {
  const selected = Array.isArray(item) ? item[0] : item
  if (!selected || selected.disabled) {
    return
  }
  treeModel.value = selected
  emit('update:modelValue', selected.value)
  open.value = false
}
</script>

<template>
  <UPopover
    v-model:open="open"
    :content="{ align: 'start', side: 'bottom' }"
    :ui="{ content: 'p-0' }"
  >
    <UButton
      color="neutral"
      variant="outline"
      class="w-full justify-between gap-2 font-normal"
      :disabled="disabled"
      trailing-icon="i-lucide-chevrons-up-down"
    >
      <span
        class="min-w-0 flex-1 truncate text-left"
        :class="selectedItem ? '' : 'text-muted'"
      >
        {{ displayLabel }}
      </span>
    </UButton>

    <template #content>
      <div class="max-h-72 w-72 min-w-[var(--reka-popover-trigger-width,16rem)] overflow-y-auto p-1.5">
        <p
          v-if="!items.length"
          class="px-2 py-3 text-sm text-muted"
        >
          暂无可选项
        </p>
        <UTree
          v-else
          :model-value="treeModel"
          v-model:expanded="expanded"
          :items="items"
          :get-key="(item: AppTreeSelectItem) => item.id"
          @update:model-value="onTreeUpdate"
        />
      </div>
    </template>
  </UPopover>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { GeoRegionNode } from '../../../api/admin/geo'

const props = defineProps<{
  modelValue: string | null
  regionPath?: string
  disabled?: boolean
  loadChildren: (parentId?: string | null) => Promise<GeoRegionNode[]>
  buildRegionPath: (names: string[]) => string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string | null]
  'update:regionPath': [value: string]
}>()

interface CascaderColumn {
  options: GeoRegionNode[]
  selectedId: string | null
}

const open = ref(false)
const loading = ref(false)
const countryRoot = ref<GeoRegionNode | null>(null)
const columns = ref<CascaderColumn[]>([])
const selectedChain = ref<GeoRegionNode[]>([])
/** 避免自身 emit 再次触发 hydrate */
const syncing = ref(false)

const displayLabel = computed(() => {
  if (selectedChain.value.length === 0) {
    return '请选择省 / 市 / 区'
  }
  return selectedChain.value.map(node => node.name).join(' / ')
})

const hasSelection = computed(() => selectedChain.value.length > 0)

async function resolveCountryRoot(): Promise<GeoRegionNode | null> {
  if (countryRoot.value) {
    return countryRoot.value
  }
  const roots = await props.loadChildren(null)
  const china = roots.find(node => node.code === 'CN' || node.name.includes('中国'))
    ?? roots[0]
    ?? null
  countryRoot.value = china
  return china
}

async function loadProvincesColumn(): Promise<CascaderColumn[]> {
  const country = await resolveCountryRoot()
  if (!country) {
    return []
  }
  const provinces = await props.loadChildren(country.id)
  return [{ options: provinces, selectedId: null }]
}

function emitComplete(chain: GeoRegionNode[]) {
  selectedChain.value = chain
  const leaf = chain[chain.length - 1]
  if (!leaf) {
    syncing.value = true
    emit('update:modelValue', null)
    emit('update:regionPath', '')
    syncing.value = false
    return
  }
  const names = [
    ...(countryRoot.value ? [countryRoot.value.name] : []),
    ...chain.map(node => node.name)
  ]
  syncing.value = true
  emit('update:modelValue', leaf.id)
  emit('update:regionPath', props.buildRegionPath(names))
  syncing.value = false
}

function clearIncompleteEmit() {
  if (props.modelValue == null && !props.regionPath) {
    return
  }
  syncing.value = true
  emit('update:modelValue', null)
  emit('update:regionPath', '')
  syncing.value = false
}

async function selectOption(columnIndex: number, option: GeoRegionNode) {
  const nextColumns = columns.value.slice(0, columnIndex + 1).map((column, index) => ({
    options: column.options,
    selectedId: index === columnIndex ? option.id : column.selectedId
  }))

  const nextChain = selectedChain.value.slice(0, columnIndex)
  nextChain[columnIndex] = option

  if (option.hasChildren) {
    loading.value = true
    try {
      const children = await props.loadChildren(option.id)
      columns.value = [
        ...nextColumns,
        { options: children, selectedId: null }
      ]
      selectedChain.value = nextChain
      clearIncompleteEmit()
    } finally {
      loading.value = false
    }
    return
  }

  columns.value = nextColumns
  emitComplete(nextChain)
  open.value = false
}

async function hydrateFromModelValue() {
  loading.value = true
  try {
    const country = await resolveCountryRoot()
    if (!country) {
      columns.value = []
      selectedChain.value = []
      return
    }

    if (!props.modelValue) {
      columns.value = await loadProvincesColumn()
      selectedChain.value = []
      return
    }

    const pathNames = (props.regionPath ?? '')
      .split('/')
      .map(part => part.trim())
      .filter(Boolean)
      .filter(name => name !== country.name)

    const nextColumns: CascaderColumn[] = []
    const nextChain: GeoRegionNode[] = []
    let parentId: string | null = country.id
    let remainingNames = [...pathNames]

    for (let depth = 0; depth < 3; depth += 1) {
      const options = await props.loadChildren(parentId)
      if (options.length === 0) {
        break
      }

      let matched = options.find(item => item.id === props.modelValue)
      if (!matched && remainingNames.length > 0) {
        const expected = remainingNames[0]!
        matched = options.find(item => item.name === expected)
        if (matched) {
          remainingNames = remainingNames.slice(1)
        }
      }

      nextColumns.push({
        options,
        selectedId: matched?.id ?? null
      })

      if (!matched) {
        break
      }

      nextChain.push(matched)
      if (!matched.hasChildren || matched.id === props.modelValue) {
        break
      }
      parentId = matched.id
    }

    const leaf = nextChain[nextChain.length - 1]
    if (leaf?.hasChildren && leaf.id !== props.modelValue) {
      const children = await props.loadChildren(leaf.id)
      nextColumns.push({ options: children, selectedId: null })
    }

    columns.value = nextColumns.length ? nextColumns : await loadProvincesColumn()
    selectedChain.value = nextChain
  } finally {
    loading.value = false
  }
}

async function ensureColumnsReady() {
  if (columns.value.length > 0) {
    return
  }
  loading.value = true
  try {
    columns.value = await loadProvincesColumn()
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.regionPath] as const,
  async () => {
    if (syncing.value) {
      return
    }
    await hydrateFromModelValue()
  },
  { immediate: true }
)

watch(open, async (isOpen) => {
  if (isOpen) {
    await ensureColumnsReady()
  }
})
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
      :loading="loading && !open"
      trailing-icon="i-lucide-chevrons-up-down"
    >
      <span
        class="min-w-0 flex-1 truncate text-left"
        :class="hasSelection ? '' : 'text-muted'"
      >
        {{ displayLabel }}
      </span>
    </UButton>

    <template #content>
      <div class="flex max-h-72 min-w-64 overflow-hidden">
        <div
          v-if="loading && columns.length === 0"
          class="flex min-w-40 items-center justify-center px-4 py-8 text-sm text-muted"
        >
          加载中…
        </div>

        <p
          v-else-if="columns.length === 0"
          class="px-4 py-8 text-sm text-muted"
        >
          暂无行政区划数据
        </p>

        <div
          v-for="(column, columnIndex) in columns"
          :key="columnIndex"
          class="min-w-36 max-w-44 flex-1 overflow-y-auto border-default not-last:border-r"
        >
          <button
            v-for="option in column.options"
            :key="option.id"
            type="button"
            class="flex w-full items-center justify-between gap-2 px-3 py-2 text-left text-sm transition-colors hover:bg-elevated"
            :class="column.selectedId === option.id ? 'bg-elevated font-medium text-highlighted' : 'text-default'"
            @click="selectOption(columnIndex, option)"
          >
            <span class="min-w-0 truncate">
              {{ option.name }}
            </span>
            <UIcon
              v-if="option.hasChildren"
              name="i-lucide-chevron-right"
              class="size-4 shrink-0 text-muted"
            />
          </button>
        </div>
      </div>
    </template>
  </UPopover>
</template>

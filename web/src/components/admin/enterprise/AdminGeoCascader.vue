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

type SelectItem = { label: string, value: string, node: GeoRegionNode }

const levelLabels = ['国家/地区', '省/直辖市', '城市', '区/县'] as const

const selectedIds = ref<Array<string | null>>([null, null, null, null])
const optionsByLevel = ref<SelectItem[][]>([[], [], [], []])
const loadingLevel = ref<number | null>(null)
const initialized = ref(false)

const selectedItems = computed(() =>
  selectedIds.value.map((id, index) =>
    optionsByLevel.value[index]?.find(item => item.value === id) ?? null))

function toSelectItems(nodes: GeoRegionNode[]): SelectItem[] {
  return nodes.map(node => ({
    label: node.name,
    value: node.id,
    node
  }))
}

async function loadLevel(level: number, parentId?: string | null) {
  loadingLevel.value = level
  try {
    const nodes = await props.loadChildren(parentId)
    optionsByLevel.value[level] = toSelectItems(nodes)
  } finally {
    loadingLevel.value = null
  }
}

function clearFromLevel(level: number) {
  for (let i = level; i < selectedIds.value.length; i += 1) {
    selectedIds.value[i] = null
    if (i > level) {
      optionsByLevel.value[i] = []
    }
  }
}

function emitSelection() {
  const names = selectedItems.value
    .filter((item): item is SelectItem => item != null)
    .map(item => item.label)
  const leaf = [...selectedIds.value].reverse().find(id => id != null) ?? null
  emit('update:modelValue', leaf)
  emit('update:regionPath', props.buildRegionPath(names))
}

async function handleLevelChange(level: number, value: string | null) {
  selectedIds.value[level] = value
  clearFromLevel(level + 1)

  const item = optionsByLevel.value[level]?.find(entry => entry.value === value)
  if (item?.node.hasChildren) {
    await loadLevel(level + 1, item.value)
  }

  emitSelection()
}

async function hydrateFromModelValue() {
  if (!props.modelValue) {
    selectedIds.value = [null, null, null, null]
    await loadLevel(0)
    initialized.value = true
    return
  }

  await loadLevel(0)
  let parentId: string | null = null

  for (let level = 0; level < levelLabels.length; level += 1) {
    const options = optionsByLevel.value[level] ?? []
    let matched = options.find(item => item.value === props.modelValue)
    if (!matched && level === levelLabels.length - 1) {
      matched = options.find(item => item.value === props.modelValue)
    }
    if (!matched) {
      const target = options.find(item =>
        props.regionPath?.includes(item.label))
      matched = target
    }
    if (!matched) {
      break
    }
    selectedIds.value[level] = matched.value
    parentId = matched.value
    if (matched.node.hasChildren && level < levelLabels.length - 1) {
      await loadLevel(level + 1, parentId)
    }
  }

  initialized.value = true
  emitSelection()
}

watch(
  () => props.modelValue,
  async (value, prev) => {
    if (!initialized.value || value !== prev) {
      await hydrateFromModelValue()
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="grid gap-3 sm:grid-cols-2">
    <UFormField
      v-for="(label, level) in levelLabels"
      :key="label"
      :label="label"
    >
      <USelectMenu
        :model-value="selectedIds[level] ?? undefined"
        :items="optionsByLevel[level] ?? []"
        value-key="value"
        :placeholder="`请选择${label}`"
        :disabled="disabled || (level > 0 && !selectedIds[level - 1])"
        :loading="loadingLevel === level"
        class="w-full"
        @update:model-value="(value: string | undefined) => handleLevelChange(level, value ?? null)"
      />
    </UFormField>
  </div>
</template>

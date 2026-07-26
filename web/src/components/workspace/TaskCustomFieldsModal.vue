<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  BASIC_FIELD_TYPES,
  DATE_FORMAT_OPTIONS,
  NUMBER_KIND_OPTIONS,
  RECOMMENDED_FIELD_TEMPLATES,
  OPTION_COLOR_PALETTE,
  fieldTypeLabel,
  fieldTypeMeta,
  formatNumberPreview,
  isPersistableFieldType,
  isSelectUiType,
  optionColorAt,
  type CustomFieldUiType,
  type DateFormatId,
  type NumberValueKind,
  type RecommendedFieldTemplate
} from '../../stores/tasks/customFieldTypes'
import {
  useCustomFieldsStore,
  type ListCustomField
} from '../../stores/tasks/customFieldsStore'

const open = defineModel<boolean>('open', { default: false })

const props = defineProps<{
  canEdit: boolean
}>()

const store = useCustomFieldsStore()
const toast = useToast()

type ModalTab = 'create' | 'list'
type OptionDraft = { key: string, label: string, color: string }

const activeTab = ref<ModalTab>('list')
const editingFieldId = ref<string | null>(null)
const renameDraft = ref('')
const submitting = ref(false)

const newName = ref('')
const newType = ref<CustomFieldUiType>('SINGLE_SELECT')
const optionDrafts = ref<OptionDraft[]>([])
const personAllowMultiple = ref(true)
const numberKind = ref<NumberValueKind>('number')
const numberDecimals = ref(2)
const numberThousands = ref(true)
const dateFormat = ref<DateFormatId>('yyyy-MM-dd')

const fields = computed(() => store.fields)

const editingField = computed(() =>
  editingFieldId.value ? store.fieldById(editingFieldId.value) : undefined
)

const modalTitle = computed(() => {
  if (editingField.value) {
    return '编辑自定义字段'
  }
  return activeTab.value === 'create' ? '添加自定义字段' : '自定义字段'
})

const tabItems = computed(() => [
  { label: '新建字段', value: 'create' },
  { label: '已创建字段', value: 'list' }
])

const typeSelectItems = computed(() =>
  BASIC_FIELD_TYPES.map(t => ({
    label: t.label,
    value: t.value,
    icon: t.icon
  }))
)

const selectedTypeMeta = computed(() => fieldTypeMeta(newType.value))

const numberKindItems = computed(() =>
  NUMBER_KIND_OPTIONS.map(k => ({
    label: k.label,
    value: k.value,
    icon: k.icon
  }))
)

const decimalItems = [0, 1, 2, 3, 4].map(n => ({ label: String(n), value: n }))

const numberPreview = computed(() =>
  formatNumberPreview(numberKind.value, numberDecimals.value, numberThousands.value)
)

const canSubmitCreate = computed(() => {
  if (!props.canEdit || submitting.value) {
    return false
  }
  if (!newName.value.trim()) {
    return false
  }
  if (!isPersistableFieldType(newType.value)) {
    return true
  }
  if (isSelectUiType(newType.value)) {
    const labels = optionDrafts.value.map(o => o.label.trim()).filter(Boolean)
    return labels.length >= 2
  }
  return true
})

const addFieldMenuItems = computed(() => {
  if (!props.canEdit) {
    return []
  }
  const basic = BASIC_FIELD_TYPES.map(t => ({
    label: t.label,
    icon: t.icon,
    onSelect: () => startCreate(t.value)
  }))
  const recommended = RECOMMENDED_FIELD_TEMPLATES.map(t => ({
    label: t.label,
    icon: t.icon,
    onSelect: () => startFromTemplate(t)
  }))
  return [
    [{ label: '基础字段', type: 'label' as const }, ...basic],
    [{ label: '推荐字段', type: 'label' as const }, ...recommended]
  ]
})

watch(open, (v) => {
  if (!v) {
    resetAll()
    return
  }
  activeTab.value = fields.value.length ? 'list' : 'create'
  if (!fields.value.length && props.canEdit) {
    resetCreateForm('SINGLE_SELECT')
  }
})

watch(newType, (type) => {
  if (isSelectUiType(type) && optionDrafts.value.length < 2) {
    optionDrafts.value = defaultSelectOptions()
  }
})

function resetAll() {
  activeTab.value = 'list'
  editingFieldId.value = null
  renameDraft.value = ''
  submitting.value = false
  resetCreateForm('SINGLE_SELECT')
}

function defaultSelectOptions(labels = ['选项 1', '选项 2']): OptionDraft[] {
  return labels.map((label, i) => ({
    key: `opt-${i}-${Math.random().toString(36).slice(2, 7)}`,
    label,
    color: optionColorAt(i)
  }))
}

function resetCreateForm(type: CustomFieldUiType, name = '', optionLabels?: string[]) {
  newName.value = name
  newType.value = type
  optionDrafts.value = isSelectUiType(type)
    ? defaultSelectOptions(optionLabels ?? ['选项 1', '选项 2'])
    : []
  personAllowMultiple.value = true
  numberKind.value = 'number'
  numberDecimals.value = 2
  numberThousands.value = true
  dateFormat.value = 'yyyy-MM-dd'
}

function startCreate(type: CustomFieldUiType = 'SINGLE_SELECT') {
  editingFieldId.value = null
  resetCreateForm(type)
  activeTab.value = 'create'
}

function startFromTemplate(tpl: RecommendedFieldTemplate) {
  editingFieldId.value = null
  resetCreateForm(tpl.fieldType, tpl.name, tpl.options)
  activeTab.value = 'create'
}

function addOptionDraft() {
  optionDrafts.value = [
    ...optionDrafts.value,
    {
      key: `opt-${Date.now()}`,
      label: `选项 ${optionDrafts.value.length + 1}`,
      color: optionColorAt(optionDrafts.value.length)
    }
  ]
}

function removeOptionDraft(key: string) {
  if (optionDrafts.value.length <= 2) {
    toast.add({ title: '至少保留两个选项', color: 'warning' })
    return
  }
  optionDrafts.value = optionDrafts.value.filter(o => o.key !== key)
}

function setDraftColor(key: string, color: string) {
  optionDrafts.value = optionDrafts.value.map(o =>
    o.key === key ? { ...o, color } : o
  )
}

async function submitCreate() {
  if (!canSubmitCreate.value) {
    return
  }
  if (!isPersistableFieldType(newType.value)) {
    toast.add({
      title: '该字段类型即将支持',
      description: '当前版本可先创建「单选」字段，并用于看板/列表分组。',
      color: 'warning'
    })
    return
  }
  submitting.value = true
  try {
    await store.createField(
      newName.value,
      optionDrafts.value.map(o => ({ label: o.label, color: o.color }))
    )
    toast.add({ title: '字段已创建', color: 'success' })
    activeTab.value = 'list'
    resetCreateForm('SINGLE_SELECT')
  } catch (e) {
    const msg = e instanceof Error ? e.message : ''
    toast.add({
      title: msg === 'TASK_LIST_FIELD_OPTIONS_MIN' ? '至少两个有效选项' : '创建失败',
      color: 'error'
    })
  } finally {
    submitting.value = false
  }
}

function openEdit(field: ListCustomField) {
  editingFieldId.value = field.id
  renameDraft.value = field.name
  activeTab.value = 'list'
}

async function submitRename() {
  if (!editingFieldId.value) {
    return
  }
  try {
    await store.renameField(editingFieldId.value, renameDraft.value)
    toast.add({ title: '已重命名', color: 'success' })
  } catch {
    toast.add({ title: '重命名失败', color: 'error' })
  }
}

async function submitAddOption() {
  if (!editingFieldId.value) {
    return
  }
  const n = (editingField.value?.options.length ?? 0) + 1
  try {
    const opt = await store.addOption(editingFieldId.value, `选项 ${n}`)
    await store.setOptionColor(editingFieldId.value, opt.id, optionColorAt(n - 1))
  } catch {
    toast.add({ title: '添加选项失败', color: 'error' })
  }
}

async function handleRenameOption(optionId: string, label: string) {
  if (!editingFieldId.value) {
    return
  }
  try {
    await store.renameOption(editingFieldId.value, optionId, label)
  } catch {
    toast.add({ title: '重命名选项失败', color: 'error' })
  }
}

async function handleOptionColor(optionId: string, color: string) {
  if (!editingFieldId.value) {
    return
  }
  try {
    await store.setOptionColor(editingFieldId.value, optionId, color)
  } catch {
    toast.add({ title: '更新颜色失败', color: 'error' })
  }
}

async function handleDeleteOption(optionId: string) {
  if (!editingFieldId.value) {
    return
  }
  try {
    await store.deleteOption(editingFieldId.value, optionId)
  } catch (e) {
    const msg = e instanceof Error ? e.message : ''
    toast.add({
      title: msg === 'TASK_LIST_FIELD_OPTIONS_MIN' ? '至少保留两个选项' : '删除失败',
      color: 'error'
    })
  }
}

async function handleDeleteField(fieldId: string) {
  try {
    await store.deleteField(fieldId)
    if (editingFieldId.value === fieldId) {
      editingFieldId.value = null
    }
    toast.add({ title: '字段已删除', color: 'success' })
  } catch {
    toast.add({ title: '删除失败', color: 'error' })
  }
}

function onTabChange(v: string | number) {
  const next = String(v) as ModalTab
  editingFieldId.value = null
  if (next === 'create' && props.canEdit) {
    resetCreateForm(newType.value || 'SINGLE_SELECT')
  }
  activeTab.value = next
}
</script>

<template>
  <UModal
    v-model:open="open"
    :title="modalTitle"
    :ui="{ content: 'sm:max-w-xl' }"
  >
    <template #body>
      <div class="space-y-4">
        <p
          v-if="!editingField"
          class="text-xs text-[var(--ws-text-muted)]"
        >
          仅当前清单；单选字段可作看板/列表分组源。
        </p>

        <!-- Edit existing field -->
        <div
          v-if="editingField"
          class="space-y-4"
        >
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <UFormField label="字段标题">
              <div class="flex gap-2">
                <UInput
                  v-model="renameDraft"
                  class="flex-1"
                  :disabled="!canEdit"
                />
                <UButton
                  v-if="canEdit"
                  color="primary"
                  variant="soft"
                  @click="submitRename"
                >
                  保存
                </UButton>
              </div>
            </UFormField>
            <UFormField label="字段类型">
              <div
                class="flex h-8 items-center gap-2 rounded-md border border-[var(--ws-border-subtle)] px-2.5 text-sm text-[var(--ws-text-muted)]"
              >
                <UIcon
                  :name="fieldTypeMeta('SINGLE_SELECT').icon"
                  class="size-4 shrink-0"
                />
                <span>{{ fieldTypeLabel(editingField.fieldType) }}</span>
              </div>
            </UFormField>
          </div>

          <div class="space-y-2">
            <p class="text-xs font-medium text-[var(--ws-text-muted)]">
              选项内容
            </p>
            <div
              v-for="opt in editingField.options"
              :key="opt.id"
              class="flex items-center gap-2"
            >
              <UIcon
                name="i-lucide-grip-vertical"
                class="size-4 shrink-0 text-[var(--ws-text-muted)] opacity-50"
              />
              <UPopover>
                <button
                  type="button"
                  class="size-6 shrink-0 rounded-full border border-white/20 shadow-sm"
                  :style="{ backgroundColor: opt.color || optionColorAt(0) }"
                  :disabled="!canEdit"
                  :aria-label="`选项颜色 ${opt.label}`"
                />
                <template #content>
                  <div class="flex flex-wrap gap-2 p-2">
                    <button
                      v-for="c in OPTION_COLOR_PALETTE"
                      :key="c"
                      type="button"
                      class="size-6 rounded-full ring-offset-2"
                      :class="opt.color === c ? 'ring-2 ring-[var(--ui-primary)]' : ''"
                      :style="{ backgroundColor: c }"
                      @click="handleOptionColor(opt.id, c)"
                    />
                  </div>
                </template>
              </UPopover>
              <UInput
                :model-value="opt.label"
                class="flex-1"
                size="sm"
                :disabled="!canEdit"
                @change="(e: Event) => handleRenameOption(opt.id, (e.target as HTMLInputElement).value)"
              />
              <UButton
                v-if="canEdit"
                color="neutral"
                variant="ghost"
                size="xs"
                icon="i-lucide-x"
                @click="handleDeleteOption(opt.id)"
              />
            </div>
            <UButton
              v-if="canEdit"
              color="neutral"
              variant="ghost"
              size="sm"
              icon="i-lucide-plus"
              label="添加选项"
              class="px-0"
              @click="submitAddOption"
            />
          </div>

          <div class="flex justify-end gap-2">
            <UButton
              color="neutral"
              variant="soft"
              @click="editingFieldId = null"
            >
              返回
            </UButton>
            <UButton
              v-if="canEdit"
              color="error"
              variant="soft"
              @click="handleDeleteField(editingField.id)"
            >
              删除字段
            </UButton>
          </div>
        </div>

        <!-- Tabs: create / list -->
        <template v-else>
          <UTabs
            v-if="canEdit"
            :model-value="activeTab"
            :items="tabItems"
            size="sm"
            class="w-full"
            :ui="{ list: 'w-full' }"
            @update:model-value="onTabChange"
          />

          <!-- Create -->
          <form
            v-if="canEdit && activeTab === 'create'"
            class="space-y-4"
            @submit.prevent="submitCreate"
          >
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <UFormField label="字段标题" required>
                <UInput
                  v-model="newName"
                  placeholder="请输入字段标题"
                  autofocus
                />
              </UFormField>
              <UFormField label="字段类型" required>
                <USelectMenu
                  v-model="newType"
                  :items="typeSelectItems"
                  value-key="value"
                  label-key="label"
                  :icon="selectedTypeMeta.icon"
                  class="w-full"
                >
                  <template #item="{ item }">
                    <div class="flex w-full items-center gap-2">
                      <UIcon
                        :name="(item as { icon?: string }).icon || 'i-lucide-circle'"
                        class="size-4 shrink-0 text-[var(--ws-text-muted)]"
                      />
                      <span class="flex-1">{{ (item as { label: string }).label }}</span>
                      <UIcon
                        v-if="newType === (item as { value: string }).value"
                        name="i-lucide-check"
                        class="size-4 text-[var(--ui-primary)]"
                      />
                    </div>
                  </template>
                </USelectMenu>
              </UFormField>
            </div>

            <!-- Select options -->
            <div
              v-if="isSelectUiType(newType)"
              class="space-y-2"
            >
              <p class="text-xs font-medium text-[var(--ws-text-muted)]">
                选项内容
              </p>
              <div
                v-for="opt in optionDrafts"
                :key="opt.key"
                class="flex items-center gap-2"
              >
                <UIcon
                  name="i-lucide-grip-vertical"
                  class="size-4 shrink-0 text-[var(--ws-text-muted)] opacity-50"
                />
                <UPopover>
                  <button
                    type="button"
                    class="size-6 shrink-0 rounded-full border border-white/20 shadow-sm"
                    :style="{ backgroundColor: opt.color }"
                    :aria-label="`选项颜色 ${opt.label}`"
                  />
                  <template #content>
                    <div class="flex flex-wrap gap-2 p-2">
                      <button
                        v-for="c in OPTION_COLOR_PALETTE"
                        :key="c"
                        type="button"
                        class="size-6 rounded-full ring-offset-2"
                        :class="opt.color === c ? 'ring-2 ring-[var(--ui-primary)]' : ''"
                        :style="{ backgroundColor: c }"
                        @click="setDraftColor(opt.key, c)"
                      />
                    </div>
                  </template>
                </UPopover>
                <UInput
                  v-model="opt.label"
                  class="flex-1"
                  size="sm"
                  placeholder="选项名称"
                />
                <UButton
                  color="neutral"
                  variant="ghost"
                  size="xs"
                  icon="i-lucide-x"
                  @click="removeOptionDraft(opt.key)"
                />
              </div>
              <UButton
                type="button"
                color="neutral"
                variant="ghost"
                size="sm"
                icon="i-lucide-plus"
                label="添加选项"
                class="px-0"
                @click="addOptionDraft"
              />
            </div>

            <!-- Person -->
            <div
              v-else-if="newType === 'PERSON'"
              class="space-y-2"
            >
              <UCheckbox
                v-model="personAllowMultiple"
                label="允许添加多个成员"
              />
            </div>

            <!-- Number -->
            <div
              v-else-if="newType === 'NUMBER'"
              class="space-y-3"
            >
              <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
                <UFormField label="数值类型">
                  <USelectMenu
                    v-model="numberKind"
                    :items="numberKindItems"
                    value-key="value"
                    label-key="label"
                    class="w-full"
                  />
                </UFormField>
                <UFormField label="小数位数">
                  <USelectMenu
                    v-model="numberDecimals"
                    :items="decimalItems"
                    value-key="value"
                    label-key="label"
                    class="w-full"
                  />
                </UFormField>
                <UFormField label="预览">
                  <UInput
                    :model-value="numberPreview"
                    disabled
                    class="w-full"
                  />
                </UFormField>
              </div>
              <UCheckbox
                v-model="numberThousands"
                label="使用千分位符"
              />
            </div>

            <!-- Date -->
            <div
              v-else-if="newType === 'DATE'"
              class="space-y-2"
            >
              <UFormField label="日期格式">
                <USelectMenu
                  v-model="dateFormat"
                  :items="DATE_FORMAT_OPTIONS"
                  value-key="value"
                  label-key="label"
                  class="w-full"
                />
              </UFormField>
            </div>

            <p
              v-if="!isPersistableFieldType(newType)"
              class="rounded-md bg-[var(--ws-panel-bg)] px-3 py-2 text-xs text-[var(--ws-text-muted)] ring-1 ring-[var(--ws-border-subtle)]"
            >
              「{{ selectedTypeMeta.label }}」配置预览已就绪，保存能力即将接入；当前可创建单选字段。
            </p>

            <div class="flex justify-end gap-2 pt-1">
              <UButton
                type="button"
                color="neutral"
                variant="soft"
                @click="activeTab = 'list'"
              >
                取消
              </UButton>
              <UButton
                type="submit"
                color="primary"
                :disabled="!canSubmitCreate"
                :loading="submitting"
              >
                创建字段
              </UButton>
            </div>
          </form>

          <!-- List -->
          <div
            v-else
            class="space-y-3"
          >
            <div
              v-if="canEdit"
              class="flex justify-end"
            >
              <UDropdownMenu :items="addFieldMenuItems">
                <UButton
                  color="primary"
                  variant="soft"
                  size="sm"
                  icon="i-lucide-plus"
                  label="添加自定义字段"
                />
              </UDropdownMenu>
            </div>

            <div
              v-for="field in fields"
              :key="field.id"
              class="flex items-center gap-2 rounded-md border border-[var(--ws-border-subtle)] px-3 py-2"
            >
              <UIcon
                name="i-lucide-grip-vertical"
                class="size-4 shrink-0 text-[var(--ws-text-muted)] opacity-40"
              />
              <UIcon
                :name="fieldTypeMeta('SINGLE_SELECT').icon"
                class="size-4 shrink-0 text-[var(--ws-text-muted)]"
              />
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-medium">
                  {{ field.name }}
                </p>
                <p class="truncate text-xs text-[var(--ws-text-muted)]">
                  {{ fieldTypeLabel(field.fieldType) }}
                  · {{ field.options.map(o => o.label).join(' / ') }}
                </p>
              </div>
              <UButton
                v-if="canEdit"
                color="neutral"
                variant="ghost"
                size="xs"
                label="编辑"
                @click="openEdit(field)"
              />
              <UButton
                v-if="canEdit"
                color="neutral"
                variant="ghost"
                size="xs"
                icon="i-lucide-trash-2"
                @click="handleDeleteField(field.id)"
              />
            </div>

            <p
              v-if="!fields.length"
              class="py-6 text-center text-sm text-[var(--ws-text-muted)]"
            >
              暂无自定义字段
            </p>

            <UButton
              v-if="canEdit && !fields.length"
              color="primary"
              variant="soft"
              size="sm"
              icon="i-lucide-plus"
              label="新建单选字段"
              class="mx-auto flex"
              @click="startCreate('SINGLE_SELECT')"
            />
          </div>
        </template>
      </div>
    </template>
  </UModal>
</template>

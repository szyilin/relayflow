<script setup lang="ts">
import { computed, ref, watch } from 'vue'
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

const creating = ref(false)
const newName = ref('')
const newOptionsText = ref('高\n中\n低')
const editingFieldId = ref<string | null>(null)
const renameDraft = ref('')
const optionDraft = ref('')

const fields = computed(() => store.fields)

const editingField = computed(() =>
  editingFieldId.value ? store.fieldById(editingFieldId.value) : undefined
)

watch(open, (v) => {
  if (!v) {
    creating.value = false
    editingFieldId.value = null
  }
})

function startCreate() {
  creating.value = true
  newName.value = ''
  newOptionsText.value = '高\n中\n低'
}

async function submitCreate() {
  try {
    const labels = newOptionsText.value.split(/\n|,/).map(s => s.trim()).filter(Boolean)
    await store.createField(newName.value, labels)
    creating.value = false
    toast.add({ title: '字段已创建', color: 'success' })
  } catch (e) {
    const msg = e instanceof Error ? e.message : ''
    toast.add({
      title: msg === 'TASK_LIST_FIELD_OPTIONS_MIN' ? '至少两个选项' : '创建失败',
      color: 'error'
    })
  }
}

function openEdit(field: ListCustomField) {
  editingFieldId.value = field.id
  renameDraft.value = field.name
  optionDraft.value = ''
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
  try {
    await store.addOption(editingFieldId.value, optionDraft.value)
    optionDraft.value = ''
    toast.add({ title: '选项已添加', color: 'success' })
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
</script>

<template>
  <UModal v-model:open="open" title="自定义字段" :ui="{ content: 'sm:max-w-lg' }">
    <template #body>
      <div class="space-y-4">
        <p class="text-xs text-[var(--ws-text-muted)]">
          仅当前清单；可作看板/列表分组源。
        </p>

        <div v-if="!creating && !editingField" class="space-y-2">
          <div
            v-for="field in fields"
            :key="field.id"
            class="flex items-center gap-2 rounded-md border border-[var(--ws-border-subtle)] px-3 py-2"
          >
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium">
                {{ field.name }}
              </p>
              <p class="truncate text-xs text-[var(--ws-text-muted)]">
                单选 · {{ field.options.map(o => o.label).join(' / ') }}
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
            class="py-4 text-center text-sm text-[var(--ws-text-muted)]"
          >
            暂无自定义字段
          </p>
          <UButton
            v-if="canEdit"
            color="primary"
            variant="soft"
            size="sm"
            icon="i-lucide-plus"
            label="新建单选字段"
            @click="startCreate"
          />
        </div>

        <form
          v-else-if="creating"
          class="space-y-3"
          @submit.prevent="submitCreate"
        >
          <UFormField label="字段名称" required>
            <UInput v-model="newName" placeholder="例如：优先级" autofocus />
          </UFormField>
          <UFormField label="选项（每行一个，至少两个）" required>
            <UTextarea v-model="newOptionsText" :rows="4" />
          </UFormField>
          <div class="flex justify-end gap-2">
            <UButton color="neutral" variant="soft" @click="creating = false">
              取消
            </UButton>
            <UButton type="submit" color="primary">
              创建
            </UButton>
          </div>
        </form>

        <div v-else-if="editingField" class="space-y-3">
          <UFormField label="字段名称">
            <div class="flex gap-2">
              <UInput v-model="renameDraft" class="flex-1" />
              <UButton color="primary" variant="soft" @click="submitRename">
                保存
              </UButton>
            </div>
          </UFormField>
          <div class="space-y-2">
            <p class="text-xs font-medium text-[var(--ws-text-muted)]">
              选项
            </p>
            <div
              v-for="opt in editingField.options"
              :key="opt.id"
              class="flex items-center gap-2 text-sm"
            >
              <UInput
                :model-value="opt.label"
                class="flex-1"
                size="sm"
                @change="(e: Event) => handleRenameOption(opt.id, (e.target as HTMLInputElement).value)"
              />
              <UButton
                color="neutral"
                variant="ghost"
                size="xs"
                icon="i-lucide-trash-2"
                @click="handleDeleteOption(opt.id)"
              />
            </div>
            <div class="flex gap-2">
              <UInput
                v-model="optionDraft"
                placeholder="新选项"
                class="flex-1"
                @keydown.enter.prevent="submitAddOption"
              />
              <UButton color="neutral" variant="soft" @click="submitAddOption">
                添加
              </UButton>
            </div>
          </div>
          <div class="flex justify-end">
            <UButton color="neutral" variant="soft" @click="editingFieldId = null">
              返回
            </UButton>
          </div>
        </div>
      </div>
    </template>
  </UModal>
</template>

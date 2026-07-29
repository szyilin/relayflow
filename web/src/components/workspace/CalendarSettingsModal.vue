<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { CalendarItem } from '../../api/app/calendar'
import { useCalendarStore } from '../../stores/calendar'

const open = defineModel<boolean>('open', { default: false })

const props = defineProps<{
  calendar: CalendarItem | null
}>()

const emit = defineEmits<{
  share: [calendar: CalendarItem]
  deleted: []
}>()

const calendarStore = useCalendarStore()
const toast = useToast()

const form = reactive({
  name: '',
  description: '',
  color: ''
})
const submitting = ref(false)

const isOwner = computed(() => {
  const t = props.calendar?.type
  return t === 'PRIMARY' || t === 'OWNED'
})

const colorLabel = computed(() =>
  isOwner.value ? '默认颜色' : '日历颜色（仅自己可见）')

const colorHint = computed(() =>
  isOwner.value
    ? '修改后将映射到未设置本机色的订阅者'
    : '仅影响你自己的视图；他人仍看默认色')

watch(
  () => [open.value, props.calendar] as const,
  ([isOpen, cal]) => {
    if (!isOpen || !cal) {
      return
    }
    form.name = cal.name
    form.description = cal.description ?? ''
    form.color = isOwner.value
      ? (cal.defaultColor || cal.color)
      : (cal.displayColor || cal.effectiveColor || cal.defaultColor || cal.color)
  }
)

async function save() {
  if (!props.calendar) {
    return
  }
  submitting.value = true
  try {
    if (isOwner.value) {
      const name = form.name.trim()
      if (!name) {
        toast.add({ title: '请输入日历名称', color: 'error' })
        return
      }
      await calendarStore.updateCalendar({
        id: props.calendar.id,
        name,
        color: form.color,
        description: form.description
      })
    } else {
      await calendarStore.updateDisplayColor(props.calendar.id, form.color)
    }
    toast.add({ title: '已保存', color: 'success' })
    open.value = false
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '保存失败',
      color: 'error'
    })
  } finally {
    submitting.value = false
  }
}

async function clearDisplayColor() {
  if (!props.calendar || isOwner.value) {
    return
  }
  submitting.value = true
  try {
    await calendarStore.updateDisplayColor(props.calendar.id, null)
    form.color = props.calendar.defaultColor || props.calendar.color
    toast.add({ title: '已恢复默认颜色', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '操作失败',
      color: 'error'
    })
  } finally {
    submitting.value = false
  }
}

async function deleteCalendar() {
  if (!props.calendar || props.calendar.type !== 'OWNED') {
    return
  }
  submitting.value = true
  try {
    await calendarStore.removeCalendar(props.calendar.id)
    toast.add({ title: '日历已删除', color: 'success' })
    open.value = false
    emit('deleted')
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '删除失败',
      color: 'error'
    })
  } finally {
    submitting.value = false
  }
}

function openShare() {
  if (props.calendar && isOwner.value) {
    emit('share', props.calendar)
  }
}
</script>

<template>
  <UModal
    v-model:open="open"
    :title="calendar ? `日历设置 · ${calendar.name}` : '日历设置'"
  >
    <template #body>
      <div
        v-if="calendar"
        class="space-y-4"
      >
        <UFormField label="名称">
          <UInput
            v-model="form.name"
            :disabled="!isOwner"
            :ui="!isOwner ? { base: 'opacity-70' } : undefined"
          />
          <p
            v-if="!isOwner"
            class="mt-1 text-xs text-[var(--ws-text-muted)]"
          >
            暂无编辑权限
          </p>
        </UFormField>

        <UFormField :label="colorLabel">
          <div class="flex flex-wrap gap-2">
            <button
              v-for="c in calendarStore.DEFAULT_COLORS"
              :key="c"
              type="button"
              class="size-7 rounded-full ring-offset-2"
              :class="form.color === c ? 'ring-2 ring-primary' : 'ring-1 ring-transparent'"
              :style="{ backgroundColor: c }"
              :title="c"
              @click="form.color = c"
            />
          </div>
          <p class="mt-1 text-xs text-[var(--ws-text-muted)]">
            {{ colorHint }}
          </p>
          <UButton
            v-if="!isOwner && calendar.displayColor"
            class="mt-2"
            size="xs"
            color="neutral"
            variant="ghost"
            :loading="submitting"
            @click="clearDisplayColor"
          >
            恢复默认颜色
          </UButton>
        </UFormField>

        <UFormField label="描述">
          <UTextarea
            v-model="form.description"
            :rows="3"
            :disabled="!isOwner"
            placeholder="暂无描述"
            :ui="!isOwner ? { base: 'opacity-70' } : undefined"
          />
        </UFormField>

        <div
          v-if="isOwner"
          class="rounded-lg border border-[var(--ws-border-subtle)] p-3"
        >
          <div class="mb-2 flex items-center justify-between">
            <p class="text-sm font-medium">
              共享人
            </p>
            <UButton
              size="xs"
              color="primary"
              variant="soft"
              icon="i-lucide-share-2"
              @click="openShare"
            >
              管理共享
            </UButton>
          </div>
          <p class="text-xs text-[var(--ws-text-muted)]">
            添加成员只读访问此日历
          </p>
        </div>

        <div
          v-if="calendar.type === 'OWNED'"
          class="border-t border-[var(--ws-border-subtle)] pt-3"
        >
          <UButton
            color="error"
            variant="soft"
            :loading="submitting"
            @click="deleteCalendar"
          >
            删除日历
          </UButton>
        </div>
      </div>
    </template>
    <template #footer>
      <div class="flex justify-end gap-2">
        <UButton
          color="neutral"
          variant="ghost"
          @click="open = false"
        >
          取消
        </UButton>
        <UButton
          color="primary"
          :loading="submitting"
          @click="save"
        >
          保存
        </UButton>
      </div>
    </template>
  </UModal>
</template>

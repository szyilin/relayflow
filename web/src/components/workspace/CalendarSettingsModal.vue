<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { CalendarItem } from '../../api/app/calendar'
import { usePermission } from '../../composables/usePermission'
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
const { hasPermission } = usePermission()
const toast = useToast()

const form = reactive({
  name: '',
  description: '',
  color: ''
})
const submitting = ref(false)

/** Personal / owned calendars the member owns. */
const isPersonalOwner = computed(() => {
  const t = props.calendar?.type
  return t === 'PRIMARY' || t === 'OWNED'
})

/**
 * Edit name/description: personal owner, or all-staff when the user has admin
 * product-settings write permission (not merely because they subscribed).
 */
const canEditMeta = computed(() => {
  if (!props.calendar) {
    return false
  }
  if (isPersonalOwner.value) {
    return true
  }
  return props.calendar.type === 'ALL_STAFF'
    && hasPermission('calendar:product-settings:update')
})

/** Workspace color control is personal override except for owned calendars' default color. */
const usesPersonalColor = computed(() => !isPersonalOwner.value)

const colorLabel = computed(() =>
  usesPersonalColor.value ? '日历颜色（仅自己可见）' : '默认颜色')

const colorHint = computed(() =>
  usesPersonalColor.value
    ? '仅影响你自己的视图；他人仍看默认色。全员默认色请在管理后台修改。'
    : '修改后将映射到未设置本机色的订阅者')

watch(
  () => [open.value, props.calendar] as const,
  ([isOpen, cal]) => {
    if (!isOpen || !cal) {
      return
    }
    form.name = cal.name
    form.description = cal.description ?? ''
    form.color = usesPersonalColor.value
      ? (cal.displayColor || cal.effectiveColor || cal.defaultColor || cal.color)
      : (cal.defaultColor || cal.color)
  }
)

async function save() {
  if (!props.calendar) {
    return
  }
  submitting.value = true
  try {
    if (canEditMeta.value && isPersonalOwner.value) {
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
    } else if (canEditMeta.value && props.calendar.type === 'ALL_STAFF') {
      const name = form.name.trim()
      if (!name) {
        toast.add({ title: '请输入日历名称', color: 'error' })
        return
      }
      // Name/desc via app update (permission-gated); color stays personal override.
      await calendarStore.updateCalendar({
        id: props.calendar.id,
        name,
        color: props.calendar.defaultColor || props.calendar.color,
        description: form.description
      })
      await calendarStore.updateDisplayColor(props.calendar.id, form.color)
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
  if (!props.calendar || !usesPersonalColor.value) {
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
  if (props.calendar && isPersonalOwner.value) {
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
            :disabled="!canEditMeta"
            :ui="!canEditMeta ? { base: 'opacity-70' } : undefined"
          />
          <p
            v-if="!canEditMeta"
            class="mt-1 text-xs text-[var(--ws-text-muted)]"
          >
            暂无编辑权限
          </p>
          <p
            v-else-if="calendar.type === 'ALL_STAFF'"
            class="mt-1 text-xs text-[var(--ws-text-muted)]"
          >
            将更新全员日历基础信息（需管理后台日历产品权限）
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
            v-if="usesPersonalColor && calendar.displayColor"
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
            :disabled="!canEditMeta"
            placeholder="暂无描述"
            :ui="!canEditMeta ? { base: 'opacity-70' } : undefined"
          />
          <p
            v-if="!canEditMeta"
            class="mt-1 text-xs text-[var(--ws-text-muted)]"
          >
            暂无编辑权限
          </p>
        </UFormField>

        <div
          v-if="isPersonalOwner"
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

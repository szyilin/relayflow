<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { formatISO, parseISO } from 'date-fns'
import type { CalendarEvent } from '../../api/app/calendar'
import { useAuthStore } from '../../stores/auth'
import { useCalendarStore } from '../../stores/calendar'
import { useContactsStore } from '../../stores/contacts'
import { useUserPreferenceStore } from '../../stores/userPreference'

const props = defineProps<{
  mode: 'create' | 'edit'
  event?: CalendarEvent | null
  initialStart?: Date
  initialEnd?: Date
  initialAllDay?: boolean
}>()

const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{ saved: [eventId: string], deleted: [] }>()

const calendarStore = useCalendarStore()
const preference = useUserPreferenceStore()
const contacts = useContactsStore()
const auth = useAuthStore()
const toast = useToast()

const submitting = ref(false)
const showMore = ref(false)
const memberKeyword = ref('')

const form = reactive({
  title: '',
  description: '',
  calendarId: '',
  allDay: false,
  startLocal: '',
  endLocal: '',
  remindBeforeMinutes: 5 as number | null,
  allDayRemindTime: '08:00' as string | null,
  attendeeUserIds: [] as string[]
})

const isOrganizer = computed(() =>
  props.mode === 'create' || props.event?.viewerRole === 'ORGANIZER')

const calendarItems = computed(() =>
  calendarStore.calendars.map(c => ({
    label: c.name,
    value: c.id
  })))

const remindItems = [
  { label: '不提醒', value: 'none' },
  { label: '提前 5 分钟', value: '5' },
  { label: '提前 15 分钟', value: '15' },
  { label: '提前 30 分钟', value: '30' },
  { label: '提前 60 分钟', value: '60' }
]

const candidateMembers = computed(() => {
  const selfId = String(auth.userId ?? '')
  const q = memberKeyword.value.trim().toLowerCase()
  return contacts.members.filter((member) => {
    if (member.id === selfId) {
      return false
    }
    if (!q) {
      return true
    }
    return member.nickname.toLowerCase().includes(q)
      || member.username.toLowerCase().includes(q)
  })
})

function toLocalInput(date: Date, allDay: boolean): string {
  if (allDay) {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  }
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d}T${hh}:${mm}`
}

function fromLocalInput(value: string, allDay: boolean): Date {
  if (allDay) {
    const [y, m, d] = value.split('-').map(Number)
    return new Date(y!, m! - 1, d!, 0, 0, 0, 0)
  }
  return new Date(value)
}

function resetForm() {
  const cal = preference.calendar
  const primary = calendarStore.primaryCalendar
  const start = props.initialStart ?? new Date()
  const end = props.initialEnd
    ?? calendarStore.defaultTimedRange(start, cal.defaultEventDurationMinutes).end
  const allDay = props.initialAllDay ?? false

  if (props.mode === 'edit' && props.event) {
    form.title = props.event.title === '(无主题)' ? '' : props.event.title
    form.description = props.event.description ?? ''
    form.calendarId = props.event.calendarId
    form.allDay = props.event.allDay
    form.startLocal = toLocalInput(parseISO(props.event.startTime), props.event.allDay)
    form.endLocal = toLocalInput(
      props.event.allDay
        ? new Date(parseISO(props.event.endTime).getTime() - 1)
        : parseISO(props.event.endTime),
      props.event.allDay
    )
    form.remindBeforeMinutes = props.event.remindBeforeMinutes ?? null
    form.allDayRemindTime = props.event.allDayRemindTime ?? cal.allDayRemindTime
    form.attendeeUserIds = props.event.attendees
      .filter(a => a.role === 'ATTENDEE')
      .map(a => a.userId)
    showMore.value = true
    return
  }

  form.title = ''
  form.description = ''
  form.calendarId = primary?.id ?? calendarStore.calendars[0]?.id ?? ''
  form.allDay = allDay
  if (allDay) {
    const range = calendarStore.defaultAllDayRange(start)
    form.startLocal = toLocalInput(range.start, true)
    form.endLocal = toLocalInput(new Date(range.end.getTime() - 1), true)
  } else {
    form.startLocal = toLocalInput(start, false)
    form.endLocal = toLocalInput(end, false)
  }
  form.remindBeforeMinutes = cal.defaultRemindBeforeMinutes
  form.allDayRemindTime = cal.allDayRemindTime
  form.attendeeUserIds = []
  showMore.value = false
}

watch(open, async (isOpen) => {
  if (!isOpen) {
    return
  }
  resetForm()
  try {
    await contacts.fetchDepts()
    const rootId = contacts.rootDeptId()
    if (rootId) {
      await contacts.fetchMembers({ deptId: rootId })
    }
  } catch {
    // member pick optional for quick create
  }
})

function toggleAttendee(userId: string, checked: boolean) {
  if (checked) {
    if (!form.attendeeUserIds.includes(userId)) {
      form.attendeeUserIds = [...form.attendeeUserIds, userId]
    }
    return
  }
  form.attendeeUserIds = form.attendeeUserIds.filter(id => id !== userId)
}

async function handleSave() {
  if (!form.calendarId) {
    toast.add({ title: '请选择日历', color: 'error' })
    return
  }
  const start = fromLocalInput(form.startLocal, form.allDay)
  let end = fromLocalInput(form.endLocal, form.allDay)
  if (form.allDay) {
    end = new Date(end.getFullYear(), end.getMonth(), end.getDate() + 1, 0, 0, 0, 0)
  }
  if (!(end.getTime() > start.getTime())) {
    toast.add({ title: '结束时间须晚于开始时间', color: 'error' })
    return
  }

  submitting.value = true
  try {
    const payload = {
      calendarId: form.calendarId,
      title: form.title.trim() || '(无主题)',
      description: form.description,
      startTime: formatISO(start),
      endTime: formatISO(end),
      allDay: form.allDay,
      remindBeforeMinutes: form.allDay ? null : form.remindBeforeMinutes,
      allDayRemindTime: form.allDay ? form.allDayRemindTime : null,
      attendeeUserIds: form.attendeeUserIds
    }
    if (props.mode === 'edit' && props.event) {
      await calendarStore.updateEvent({ id: props.event.id, ...payload })
      toast.add({ title: '日程已更新', color: 'success' })
      emit('saved', props.event.id)
    } else {
      const id = await calendarStore.createEvent(payload)
      toast.add({ title: '日程已创建', color: 'success' })
      emit('saved', id)
    }
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

async function handleDelete() {
  if (!props.event || !isOrganizer.value) {
    return
  }
  submitting.value = true
  try {
    await calendarStore.removeEvent(props.event.id)
    toast.add({ title: '日程已删除', color: 'success' })
    emit('deleted')
    open.value = false
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '删除失败',
      color: 'error'
    })
  } finally {
    submitting.value = false
  }
}

async function handleRespond(response: 'ACCEPTED' | 'DECLINED') {
  if (!props.event) {
    return
  }
  submitting.value = true
  try {
    await calendarStore.respondEvent(props.event.id, response)
    toast.add({ title: response === 'ACCEPTED' ? '已接受' : '已拒绝', color: 'success' })
    emit('saved', props.event.id)
    open.value = false
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '操作失败',
      color: 'error'
    })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <UModal
    v-model:open="open"
    :title="mode === 'create' ? '添加主题' : '日程详情'"
    :description="isOrganizer ? '填写日程信息' : '你被邀请参加此日程'"
    :ui="{ content: 'max-w-lg w-[min(520px,94vw)]' }"
  >
    <template #body>
      <div class="space-y-4">
        <UFormField label="主题">
          <UInput
            v-model="form.title"
            placeholder="添加主题"
            :disabled="!isOrganizer"
            autofocus
          />
        </UFormField>

        <div class="flex items-center gap-3">
          <UCheckbox
            :model-value="form.allDay"
            label="全天"
            :disabled="!isOrganizer"
            @update:model-value="(v: boolean | 'indeterminate') => { form.allDay = v === true }"
          />
        </div>

        <div class="grid gap-3 sm:grid-cols-2">
          <UFormField :label="form.allDay ? '开始日期' : '开始'">
            <UInput
              v-model="form.startLocal"
              :type="form.allDay ? 'date' : 'datetime-local'"
              :disabled="!isOrganizer"
            />
          </UFormField>
          <UFormField :label="form.allDay ? '结束日期' : '结束'">
            <UInput
              v-model="form.endLocal"
              :type="form.allDay ? 'date' : 'datetime-local'"
              :disabled="!isOrganizer"
            />
          </UFormField>
        </div>

        <UFormField label="日历">
          <USelect
            v-model="form.calendarId"
            :items="calendarItems"
            :disabled="!isOrganizer"
            class="w-full"
          />
        </UFormField>

        <UFormField label="描述">
          <UTextarea
            v-model="form.description"
            :rows="2"
            placeholder="添加描述"
            :disabled="!isOrganizer"
          />
        </UFormField>

        <div v-if="!showMore && isOrganizer">
          <UButton
            variant="ghost"
            color="neutral"
            size="sm"
            icon="i-lucide-chevron-down"
            @click="showMore = true"
          >
            更多选项
          </UButton>
        </div>

        <template v-if="showMore || !isOrganizer">
          <UFormField v-if="!form.allDay" label="提醒">
            <USelect
              :model-value="form.remindBeforeMinutes == null ? 'none' : String(form.remindBeforeMinutes)"
              :items="remindItems"
              :disabled="!isOrganizer"
              class="w-full"
              @update:model-value="(v: string) => {
                form.remindBeforeMinutes = v === 'none' ? null : Number(v)
              }"
            />
          </UFormField>
          <UFormField v-else label="全天提醒时刻">
            <UInput
              :model-value="form.allDayRemindTime ?? ''"
              placeholder="08:00"
              :disabled="!isOrganizer"
              @update:model-value="(v: string) => { form.allDayRemindTime = v || '08:00' }"
            />
          </UFormField>

          <UFormField label="参与人">
            <div v-if="isOrganizer" class="space-y-2 rounded-lg border border-[var(--ws-border-subtle)] p-2">
              <UInput
                v-model="memberKeyword"
                placeholder="搜索成员"
                icon="i-lucide-search"
                size="sm"
              />
              <div class="max-h-40 space-y-1 overflow-y-auto">
                <label
                  v-for="member in candidateMembers"
                  :key="member.id"
                  class="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-[var(--ws-rail-hover)]"
                >
                  <UCheckbox
                    :model-value="form.attendeeUserIds.includes(member.id)"
                    @update:model-value="(v: boolean | 'indeterminate') => toggleAttendee(member.id, v === true)"
                  />
                  <span class="truncate">{{ member.nickname }}</span>
                  <span class="ml-auto truncate text-xs text-[var(--ws-text-muted)]">{{ member.deptName }}</span>
                </label>
                <p
                  v-if="candidateMembers.length === 0"
                  class="px-2 py-3 text-center text-xs text-[var(--ws-text-muted)]"
                >
                  暂无其他成员
                </p>
              </div>
            </div>
            <ul v-else class="space-y-1 text-sm">
              <li
                v-for="a in event?.attendees ?? []"
                :key="a.userId"
                class="flex items-center justify-between gap-2 rounded-md bg-[var(--ws-input-bar-bg)] px-2 py-1.5"
              >
                <span>{{ a.nickname || a.userId }}</span>
                <span class="text-xs text-[var(--ws-text-muted)]">{{ a.response }}</span>
              </li>
            </ul>
          </UFormField>
        </template>
      </div>
    </template>

    <template #footer>
      <div class="flex w-full flex-wrap items-center justify-end gap-2">
        <UButton
          v-if="mode === 'edit' && isOrganizer"
          color="error"
          variant="ghost"
          :loading="submitting"
          @click="handleDelete"
        >
          删除
        </UButton>
        <template v-if="mode === 'edit' && !isOrganizer">
          <UButton
            color="neutral"
            variant="soft"
            :loading="submitting"
            @click="handleRespond('DECLINED')"
          >
            拒绝
          </UButton>
          <UButton
            color="primary"
            :loading="submitting"
            @click="handleRespond('ACCEPTED')"
          >
            接受
          </UButton>
        </template>
        <UButton
          v-if="isOrganizer"
          color="neutral"
          variant="ghost"
          @click="open = false"
        >
          取消
        </UButton>
        <UButton
          v-if="isOrganizer"
          color="primary"
          :loading="submitting"
          @click="handleSave"
        >
          保存
        </UButton>
      </div>
    </template>
  </UModal>
</template>

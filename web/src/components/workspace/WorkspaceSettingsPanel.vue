<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  THEME_COLOR_OPTIONS,
  useUserPreferenceStore,
  type ChatBubbleLayout,
  type ThemeMode
} from '../../stores/userPreference'

const open = defineModel<boolean>('open', { default: false })

const preference = useUserPreferenceStore()
const toast = useToast()

type SettingsCategory =
  | 'account'
  | 'general'
  | 'calendar'
  | 'task'
  | 'privacy'
  | 'notifications'
  | 'shortcuts'

const categories: { id: SettingsCategory, label: string, icon: string, ready: boolean }[] = [
  { id: 'account', label: '账号与安全', icon: 'i-lucide-user-round-cog', ready: false },
  { id: 'general', label: '通用', icon: 'i-lucide-sliders-horizontal', ready: true },
  { id: 'calendar', label: '日历', icon: 'i-lucide-calendar', ready: true },
  { id: 'task', label: '任务', icon: 'i-lucide-list-todo', ready: true },
  { id: 'privacy', label: '隐私', icon: 'i-lucide-shield', ready: false },
  { id: 'notifications', label: '通知', icon: 'i-lucide-bell', ready: false },
  { id: 'shortcuts', label: '快捷键', icon: 'i-lucide-keyboard', ready: false }
]

const activeCategory = ref<SettingsCategory>('general')

const themeModes: { value: ThemeMode, label: string, hint: string }[] = [
  { value: 'auto', label: '跟随系统', hint: '随系统外观自动切换' },
  { value: 'light', label: '浅色', hint: '始终使用浅色界面' },
  { value: 'dark', label: '深色', hint: '始终使用深色界面' }
]

const bubbleLayouts: { value: ChatBubbleLayout, label: string, hint: string }[] = [
  { value: 'left', label: '消息气泡左对齐', hint: '所有消息靠左排列' },
  { value: 'split', label: '消息气泡左右分布', hint: '己方靠右、对方靠左' }
]

const weekStartOptions = [
  { value: 0, label: '周日' },
  { value: 1, label: '周一' }
]

const durationOptions = [15, 30, 45, 60, 90, 120]
const remindOptions = [0, 5, 10, 15, 30, 60]
const taskRemindOptions = [
  { value: -1, label: '系统默认（不预填）' },
  { value: 0, label: '不提醒' },
  { value: 5, label: '截止前 5 分钟' },
  { value: 15, label: '截止前 15 分钟' },
  { value: 30, label: '截止前 30 分钟' },
  { value: 60, label: '截止前 1 小时' },
  { value: 1440, label: '截止前 1 天' }
]

const activeCategoryMeta = computed(() =>
  categories.find(item => item.id === activeCategory.value) ?? categories[1]!)

watch(open, (next) => {
  if (next) {
    activeCategory.value = 'general'
    void preference.fetchFromServer()
  }
})

function selectCategory(id: SettingsCategory) {
  const item = categories.find(c => c.id === id)
  activeCategory.value = id
  if (item && !item.ready) {
    toast.add({
      title: '功能即将推出',
      description: `${item.label}（占位）`,
      color: 'neutral'
    })
  }
}

function setThemeMode(mode: ThemeMode) {
  preference.setThemeMode(mode)
}

function setThemeColor(color: string) {
  preference.setThemeColor(color)
}

function setBubbleLayout(layout: ChatBubbleLayout) {
  preference.setChatBubbleLayout(layout)
}
</script>

<template>
  <UModal
    v-model:open="open"
    title="设置"
    description="工作台外观与个人偏好"
    :ui="{ content: 'max-w-3xl w-[min(920px,94vw)]' }"
  >
    <template #body>
      <div class="flex min-h-[420px] gap-0 overflow-hidden rounded-lg border border-[var(--ws-border-subtle)]">
        <nav class="flex w-44 shrink-0 flex-col gap-0.5 border-r border-[var(--ws-border-subtle)] bg-[var(--ws-input-bar-bg)]/50 p-2">
          <button
            v-for="item in categories"
            :key="item.id"
            type="button"
            class="flex items-center gap-2 rounded-lg px-2.5 py-2 text-left text-sm transition-colors"
            :class="activeCategory === item.id
              ? 'bg-primary/10 font-medium text-primary'
              : 'text-[var(--ws-text)] hover:bg-[var(--ws-rail-hover)]'"
            @click="selectCategory(item.id)"
          >
            <UIcon :name="item.icon" class="size-4 shrink-0" />
            <span class="truncate">{{ item.label }}</span>
            <span
              v-if="!item.ready"
              class="ml-auto text-[10px] text-[var(--ws-text-muted)]"
            >占位</span>
          </button>
        </nav>

        <div class="min-w-0 flex-1 overflow-y-auto p-5">
          <template v-if="activeCategory === 'general'">
            <section class="space-y-3">
              <div>
                <h3 class="text-sm font-semibold">
                  主题模式
                </h3>
                <p class="mt-0.5 text-xs text-[var(--ws-text-muted)]">
                  选择工作台浅色、深色或跟随系统
                </p>
              </div>

              <div class="grid gap-2 sm:grid-cols-3">
                <button
                  v-for="mode in themeModes"
                  :key="mode.value"
                  type="button"
                  class="rounded-xl border p-3 text-left transition-colors"
                  :class="preference.themeMode === mode.value
                    ? 'border-primary bg-primary/5 ring-1 ring-primary/30'
                    : 'border-[var(--ws-border-subtle)] hover:bg-[var(--ws-rail-hover)]'"
                  @click="setThemeMode(mode.value)"
                >
                  <div
                    class="mb-2 h-14 overflow-hidden rounded-lg border border-[var(--ws-border-subtle)]"
                    :class="mode.value === 'dark'
                      ? 'bg-zinc-800'
                      : mode.value === 'auto'
                        ? 'bg-gradient-to-r from-zinc-100 to-zinc-800'
                        : 'bg-white'"
                  >
                    <div class="m-2 space-y-1">
                      <div
                        class="h-1.5 w-10 rounded"
                        :class="mode.value === 'dark' ? 'bg-zinc-500' : 'bg-zinc-300'"
                      />
                      <div
                        class="h-6 w-full rounded"
                        :class="mode.value === 'dark' ? 'bg-zinc-700' : 'bg-zinc-100'"
                      />
                    </div>
                  </div>
                  <p class="text-sm font-medium">
                    {{ mode.label }}
                  </p>
                  <p class="mt-0.5 text-xs text-[var(--ws-text-muted)]">
                    {{ mode.hint }}
                  </p>
                </button>
              </div>
            </section>

            <section class="mt-8 space-y-3">
              <div>
                <h3 class="text-sm font-semibold">
                  主题色
                </h3>
                <p class="mt-0.5 text-xs text-[var(--ws-text-muted)]">
                  影响按钮与强调色
                </p>
              </div>
              <div class="flex flex-wrap gap-2.5">
                <button
                  v-for="color in THEME_COLOR_OPTIONS"
                  :key="color"
                  type="button"
                  class="size-8 rounded-full ring-offset-2 transition-shadow"
                  :class="preference.themeColor === color ? 'ring-2 ring-primary' : 'ring-1 ring-transparent hover:ring-[var(--ws-border-subtle)]'"
                  :style="{ backgroundColor: `var(--color-${color}-500)` }"
                  :aria-label="`主题色 ${color}`"
                  :title="color"
                  @click="setThemeColor(color)"
                />
              </div>
            </section>

            <section class="mt-8 space-y-3">
              <div>
                <h3 class="text-sm font-semibold">
                  会话显示模式
                </h3>
                <p class="mt-0.5 text-xs text-[var(--ws-text-muted)]">
                  消息气泡在会话中的排列方式
                </p>
              </div>
              <div class="grid gap-2 sm:grid-cols-2">
                <button
                  v-for="layout in bubbleLayouts"
                  :key="layout.value"
                  type="button"
                  class="rounded-xl border p-3 text-left transition-colors"
                  :class="preference.chatBubbleLayout === layout.value
                    ? 'border-primary bg-primary/5 ring-1 ring-primary/30'
                    : 'border-[var(--ws-border-subtle)] hover:bg-[var(--ws-rail-hover)]'"
                  @click="setBubbleLayout(layout.value)"
                >
                  <div class="mb-2 space-y-1.5 rounded-lg bg-[var(--ws-input-bar-bg)] p-2">
                    <div class="flex justify-start">
                      <div class="h-4 w-16 rounded-md bg-[var(--ws-border-subtle)]" />
                    </div>
                    <div
                      class="flex"
                      :class="layout.value === 'split' ? 'justify-end' : 'justify-start'"
                    >
                      <div class="h-4 w-20 rounded-md bg-primary/70" />
                    </div>
                    <div class="flex justify-start">
                      <div class="h-4 w-14 rounded-md bg-[var(--ws-border-subtle)]" />
                    </div>
                  </div>
                  <p class="text-sm font-medium">
                    {{ layout.label }}
                  </p>
                  <p class="mt-0.5 text-xs text-[var(--ws-text-muted)]">
                    {{ layout.hint }}
                  </p>
                </button>
              </div>
            </section>
          </template>

          <template v-else-if="activeCategory === 'calendar'">
            <section class="space-y-3">
              <div>
                <h3 class="text-sm font-semibold">
                  一周起始日
                </h3>
                <p class="mt-0.5 text-xs text-[var(--ws-text-muted)]">
                  影响日/周/月视图的列顺序
                </p>
              </div>
              <div class="flex flex-wrap gap-2">
                <UButton
                  v-for="opt in weekStartOptions"
                  :key="opt.value"
                  size="sm"
                  :variant="preference.calendar.weekStartsOn === opt.value ? 'solid' : 'soft'"
                  :color="preference.calendar.weekStartsOn === opt.value ? 'primary' : 'neutral'"
                  @click="preference.patchCalendar({ weekStartsOn: opt.value })"
                >
                  {{ opt.label }}
                </UButton>
              </div>
            </section>

            <section class="mt-8 space-y-3">
              <div>
                <h3 class="text-sm font-semibold">
                  默认日程时长
                </h3>
                <p class="mt-0.5 text-xs text-[var(--ws-text-muted)]">
                  快捷创建非全天日程时的默认长度
                </p>
              </div>
              <USelect
                :model-value="String(preference.calendar.defaultEventDurationMinutes)"
                :items="durationOptions.map(m => ({ label: `${m} 分钟`, value: String(m) }))"
                class="max-w-xs"
                @update:model-value="(v: string) => preference.patchCalendar({ defaultEventDurationMinutes: Number(v) })"
              />
            </section>

            <section class="mt-8 space-y-3">
              <div>
                <h3 class="text-sm font-semibold">
                  非全天默认提醒
                </h3>
                <p class="mt-0.5 text-xs text-[var(--ws-text-muted)]">
                  创建日程时预填的提醒提前量
                </p>
              </div>
              <USelect
                :model-value="String(preference.calendar.defaultRemindBeforeMinutes)"
                :items="remindOptions.map(m => ({
                  label: m === 0 ? '不提醒' : `提前 ${m} 分钟`,
                  value: String(m)
                }))"
                class="max-w-xs"
                @update:model-value="(v: string) => preference.patchCalendar({ defaultRemindBeforeMinutes: Number(v) })"
              />
            </section>

            <section class="mt-8 space-y-3">
              <div>
                <h3 class="text-sm font-semibold">
                  全天日程提醒时刻
                </h3>
                <p class="mt-0.5 text-xs text-[var(--ws-text-muted)]">
                  如 08:00（当天本地时间）
                </p>
              </div>
              <UInput
                :model-value="preference.calendar.allDayRemindTime"
                class="max-w-xs"
                placeholder="08:00"
                @update:model-value="(v: string) => preference.patchCalendar({ allDayRemindTime: v || '08:00' })"
              />
            </section>

            <section class="mt-8 space-y-3">
              <div class="flex items-center justify-between gap-4">
                <div>
                  <h3 class="text-sm font-semibold">
                    已结束日程降低亮度
                  </h3>
                  <p class="mt-0.5 text-xs text-[var(--ws-text-muted)]">
                    仅影响日历网格展示
                  </p>
                </div>
                <USwitch
                  :model-value="preference.calendar.dimPastEvents"
                  @update:model-value="(v: boolean) => preference.patchCalendar({ dimPastEvents: v })"
                />
              </div>
            </section>

            <section class="mt-8 space-y-3">
              <div class="flex items-center justify-between gap-4">
                <div>
                  <h3 class="text-sm font-semibold">
                    显示「我的任务」图层
                  </h3>
                  <p class="mt-0.5 text-xs text-[var(--ws-text-muted)]">
                    进入日历时是否默认勾选任务投影图层
                  </p>
                </div>
                <USwitch
                  :model-value="preference.calendar.showTaskLayer"
                  @update:model-value="(v: boolean) => preference.patchCalendar({ showTaskLayer: v })"
                />
              </div>
            </section>
          </template>

          <template v-else-if="activeCategory === 'task'">
            <section class="space-y-3">
              <div>
                <h3 class="text-sm font-semibold">
                  默认截止提醒
                </h3>
                <p class="mt-0.5 text-xs text-[var(--ws-text-muted)]">
                  新建带截止时间的任务时预填的提醒提前量
                </p>
              </div>
              <USelect
                :model-value="String(preference.task.defaultRemindBeforeMinutes)"
                :items="taskRemindOptions.map(o => ({
                  label: o.label,
                  value: String(o.value)
                }))"
                class="max-w-xs"
                @update:model-value="(v: string) => preference.patchTask({ defaultRemindBeforeMinutes: Number(v) })"
              />
            </section>
          </template>

          <div
            v-else
            class="flex h-full min-h-[320px] flex-col items-center justify-center gap-2 text-center"
          >
            <UIcon :name="activeCategoryMeta.icon" class="size-10 text-[var(--ws-text-muted)] opacity-50" />
            <p class="font-medium">
              {{ activeCategoryMeta.label }}
            </p>
            <p class="text-sm text-[var(--ws-text-muted)]">
              功能即将推出（占位）
            </p>
          </div>
        </div>
      </div>
    </template>
  </UModal>
</template>

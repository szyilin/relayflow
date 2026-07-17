import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getUserPreference,
  updateUserPreference,
  type CalendarPreferenceSettings,
  type ChatBubbleLayout,
  type TaskPreferenceSettings,
  type ThemeMode,
  type UserPreferenceSettings
} from '../api/app/userPreference'
import { useAdminColorMode } from '../composables/useAdminColorMode'
import { useAuthStore } from './auth'

export type { ThemeMode, ChatBubbleLayout, UserPreferenceSettings }
export type CalendarPreference = CalendarPreferenceSettings
export type TaskPreference = TaskPreferenceSettings

export const USER_PREFERENCE_SCHEMA_VERSION = 1

export const DEFAULT_USER_PREFERENCE: UserPreferenceSettings = {
  general: {
    themeMode: 'light',
    themeColor: 'teal'
  },
  im: {
    chatBubbleLayout: 'split'
  },
  calendar: {
    weekStartsOn: 0,
    defaultEventDurationMinutes: 30,
    defaultRemindBeforeMinutes: 5,
    allDayRemindTime: '08:00',
    dimPastEvents: true,
    showTaskLayer: true
  },
  task: {
    defaultRemindBeforeMinutes: 30
  }
}

export const THEME_COLOR_OPTIONS = ['teal', 'green', 'cyan', 'blue', 'emerald', 'sky'] as const

function cacheKey(tenantId: string, userId: string) {
  return `relayflow-user-preference-v2:${tenantId}:${userId}`
}

function deepMergeSettings(
  base: UserPreferenceSettings,
  patch: {
    general?: Partial<UserPreferenceSettings['general']>
    im?: Partial<UserPreferenceSettings['im']>
    calendar?: Partial<UserPreferenceSettings['calendar']>
    task?: Partial<UserPreferenceSettings['task']>
  } | null | undefined
): UserPreferenceSettings {
  return {
    general: {
      ...base.general,
      ...(patch?.general ?? {})
    },
    im: {
      ...base.im,
      ...(patch?.im ?? {})
    },
    calendar: {
      ...base.calendar,
      ...(patch?.calendar ?? {})
    },
    task: {
      ...base.task,
      ...(patch?.task ?? {})
    }
  }
}

function readCache(tenantId: string, userId: string): UserPreferenceSettings | null {
  try {
    const raw = localStorage.getItem(cacheKey(tenantId, userId))
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw) as Partial<UserPreferenceSettings>
    return deepMergeSettings(DEFAULT_USER_PREFERENCE, parsed)
  } catch {
    return null
  }
}

function writeCache(tenantId: string, userId: string, settings: UserPreferenceSettings) {
  try {
    localStorage.setItem(cacheKey(tenantId, userId), JSON.stringify(settings))
  } catch {
    // ignore quota / private mode
  }
}

export const useUserPreferenceStore = defineStore('userPreference', () => {
  const settings = ref<UserPreferenceSettings>(structuredClone(DEFAULT_USER_PREFERENCE))
  const schemaVersion = ref(USER_PREFERENCE_SCHEMA_VERSION)
  const hydrated = ref(false)
  const syncing = ref(false)
  const lastSyncError = ref<string | null>(null)

  const themeMode = computed(() => settings.value.general.themeMode)
  const themeColor = computed(() => settings.value.general.themeColor)
  const chatBubbleLayout = computed(() => settings.value.im.chatBubbleLayout)
  const calendar = computed(() => settings.value.calendar)
  const task = computed(() => settings.value.task)

  function authScope(): { tenantId: string, userId: string } | null {
    const auth = useAuthStore()
    if (!auth.isAuthenticated || auth.tenantId == null || auth.userId == null) {
      return null
    }
    return { tenantId: String(auth.tenantId), userId: String(auth.userId) }
  }

  function persistCache() {
    const scope = authScope()
    if (!scope) {
      return
    }
    writeCache(scope.tenantId, scope.userId, settings.value)
  }

  function applyAppearance() {
    const colorMode = useAdminColorMode()
    colorMode.value = settings.value.general.themeMode
    const appConfig = useAppConfig()
    appConfig.ui.colors.primary = settings.value.general.themeColor
  }

  function resetForTenantSwitch() {
    settings.value = structuredClone(DEFAULT_USER_PREFERENCE)
    schemaVersion.value = USER_PREFERENCE_SCHEMA_VERSION
    hydrated.value = false
    lastSyncError.value = null
  }

  async function syncToServer(): Promise<boolean> {
    const auth = useAuthStore()
    if (!auth.isAuthenticated || syncing.value) {
      return false
    }
    syncing.value = true
    lastSyncError.value = null
    try {
      const data = await updateUserPreference({
        schemaVersion: schemaVersion.value,
        settings: settings.value
      })
      settings.value = deepMergeSettings(DEFAULT_USER_PREFERENCE, data.settings)
      schemaVersion.value = data.schemaVersion ?? USER_PREFERENCE_SCHEMA_VERSION
      persistCache()
      applyAppearance()
      return true
    } catch (error) {
      lastSyncError.value = error instanceof Error ? error.message : '同步偏好失败'
      return false
    } finally {
      syncing.value = false
    }
  }

  async function fetchFromServer() {
    const scope = authScope()
    if (!scope) {
      settings.value = structuredClone(DEFAULT_USER_PREFERENCE)
      applyAppearance()
      hydrated.value = true
      return
    }

    const cached = readCache(scope.tenantId, scope.userId)
    if (cached) {
      settings.value = cached
      applyAppearance()
    }

    try {
      const data = await getUserPreference()
      settings.value = deepMergeSettings(DEFAULT_USER_PREFERENCE, data.settings)
      schemaVersion.value = data.schemaVersion ?? USER_PREFERENCE_SCHEMA_VERSION
      persistCache()
      applyAppearance()
      hydrated.value = true
      lastSyncError.value = null
    } catch (error) {
      if (!cached) {
        settings.value = structuredClone(DEFAULT_USER_PREFERENCE)
        applyAppearance()
      }
      hydrated.value = true
      lastSyncError.value = error instanceof Error ? error.message : '加载偏好失败'
    }
  }

  async function setThemeMode(mode: ThemeMode): Promise<boolean> {
    settings.value = {
      ...settings.value,
      general: { ...settings.value.general, themeMode: mode }
    }
    persistCache()
    applyAppearance()
    return syncToServer()
  }

  async function setThemeColor(color: string): Promise<boolean> {
    settings.value = {
      ...settings.value,
      general: { ...settings.value.general, themeColor: color }
    }
    persistCache()
    applyAppearance()
    return syncToServer()
  }

  async function setChatBubbleLayout(layout: ChatBubbleLayout): Promise<boolean> {
    settings.value = {
      ...settings.value,
      im: { ...settings.value.im, chatBubbleLayout: layout }
    }
    persistCache()
    return syncToServer()
  }

  async function patchCalendar(patch: Partial<CalendarPreferenceSettings>): Promise<boolean> {
    settings.value = {
      ...settings.value,
      calendar: { ...settings.value.calendar, ...patch }
    }
    persistCache()
    return syncToServer()
  }

  async function patchTask(patch: Partial<TaskPreferenceSettings>): Promise<boolean> {
    settings.value = {
      ...settings.value,
      task: { ...settings.value.task, ...patch }
    }
    persistCache()
    return syncToServer()
  }

  function replaceSettings(next: UserPreferenceSettings, version = USER_PREFERENCE_SCHEMA_VERSION) {
    settings.value = deepMergeSettings(DEFAULT_USER_PREFERENCE, next)
    schemaVersion.value = version
    persistCache()
    applyAppearance()
  }

  return {
    settings,
    schemaVersion,
    hydrated,
    syncing,
    lastSyncError,
    themeMode,
    themeColor,
    chatBubbleLayout,
    calendar,
    task,
    resetForTenantSwitch,
    fetchFromServer,
    applyAppearance,
    setThemeMode,
    setThemeColor,
    setChatBubbleLayout,
    patchCalendar,
    patchTask,
    replaceSettings
  }
})

import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getUserPreference, updateUserPreference } from '../api/app/userPreference'
import { useAdminColorMode } from '../composables/useAdminColorMode'
import { useAuthStore } from './auth'

export type ThemeMode = 'light' | 'dark' | 'auto'
export type ChatBubbleLayout = 'left' | 'split'

export interface CalendarPreference {
  weekStartsOn: number
  defaultEventDurationMinutes: number
  defaultRemindBeforeMinutes: number
  allDayRemindTime: string
  dimPastEvents: boolean
}

export interface UserPreferenceSettings {
  general: {
    themeMode: ThemeMode
    themeColor: string
  }
  im: {
    chatBubbleLayout: ChatBubbleLayout
  }
  calendar: CalendarPreference
}

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
    dimPastEvents: true
  }
}

export const THEME_COLOR_OPTIONS = ['teal', 'green', 'cyan', 'blue', 'emerald', 'sky'] as const

const STORAGE_KEY = 'relayflow-user-preference-local-v1'

function deepMergeSettings(
  base: UserPreferenceSettings,
  patch: {
    general?: Partial<UserPreferenceSettings['general']>
    im?: Partial<UserPreferenceSettings['im']>
    calendar?: Partial<UserPreferenceSettings['calendar']>
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
    }
  }
}

function readLocal(): UserPreferenceSettings {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return structuredClone(DEFAULT_USER_PREFERENCE)
    }
    const parsed = JSON.parse(raw) as Partial<UserPreferenceSettings>
    return deepMergeSettings(DEFAULT_USER_PREFERENCE, parsed)
  } catch {
    return structuredClone(DEFAULT_USER_PREFERENCE)
  }
}

export const useUserPreferenceStore = defineStore('userPreference', () => {
  const settings = ref<UserPreferenceSettings>(readLocal())
  const schemaVersion = ref(USER_PREFERENCE_SCHEMA_VERSION)
  const hydrated = ref(false)
  const syncing = ref(false)

  const themeMode = computed(() => settings.value.general.themeMode)
  const themeColor = computed(() => settings.value.general.themeColor)
  const chatBubbleLayout = computed(() => settings.value.im.chatBubbleLayout)
  const calendar = computed(() => settings.value.calendar)

  function persistLocal() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings.value))
  }

  function applyAppearance() {
    const colorMode = useAdminColorMode()
    colorMode.value = settings.value.general.themeMode
    const appConfig = useAppConfig()
    appConfig.ui.colors.primary = settings.value.general.themeColor
  }

  async function syncToServer() {
    const auth = useAuthStore()
    if (!auth.isAuthenticated || syncing.value) {
      return
    }
    syncing.value = true
    try {
      const data = await updateUserPreference({
        schemaVersion: schemaVersion.value,
        settings: settings.value
      })
      settings.value = deepMergeSettings(DEFAULT_USER_PREFERENCE, data.settings)
      schemaVersion.value = data.schemaVersion ?? USER_PREFERENCE_SCHEMA_VERSION
      persistLocal()
      applyAppearance()
    } catch {
      // keep local optimistic state; API may be down during -web only
    } finally {
      syncing.value = false
    }
  }

  function hydrateFromLocal() {
    settings.value = readLocal()
    applyAppearance()
    hydrated.value = true
  }

  async function fetchFromServer() {
    const auth = useAuthStore()
    if (!auth.isAuthenticated) {
      hydrateFromLocal()
      return
    }
    try {
      const data = await getUserPreference()
      settings.value = deepMergeSettings(DEFAULT_USER_PREFERENCE, data.settings)
      schemaVersion.value = data.schemaVersion ?? USER_PREFERENCE_SCHEMA_VERSION
      persistLocal()
      applyAppearance()
      hydrated.value = true
    } catch {
      hydrateFromLocal()
    }
  }

  function setThemeMode(mode: ThemeMode) {
    settings.value = {
      ...settings.value,
      general: { ...settings.value.general, themeMode: mode }
    }
    persistLocal()
    applyAppearance()
    void syncToServer()
  }

  function setThemeColor(color: string) {
    settings.value = {
      ...settings.value,
      general: { ...settings.value.general, themeColor: color }
    }
    persistLocal()
    applyAppearance()
    void syncToServer()
  }

  function setChatBubbleLayout(layout: ChatBubbleLayout) {
    settings.value = {
      ...settings.value,
      im: { ...settings.value.im, chatBubbleLayout: layout }
    }
    persistLocal()
    void syncToServer()
  }

  function patchCalendar(patch: Partial<CalendarPreference>) {
    settings.value = {
      ...settings.value,
      calendar: { ...settings.value.calendar, ...patch }
    }
    persistLocal()
    void syncToServer()
  }

  function replaceSettings(next: UserPreferenceSettings, version = USER_PREFERENCE_SCHEMA_VERSION) {
    settings.value = deepMergeSettings(DEFAULT_USER_PREFERENCE, next)
    schemaVersion.value = version
    persistLocal()
    applyAppearance()
  }

  return {
    settings,
    schemaVersion,
    hydrated,
    syncing,
    themeMode,
    themeColor,
    chatBubbleLayout,
    calendar,
    hydrateFromLocal,
    fetchFromServer,
    applyAppearance,
    setThemeMode,
    setThemeColor,
    setChatBubbleLayout,
    patchCalendar,
    replaceSettings
  }
})

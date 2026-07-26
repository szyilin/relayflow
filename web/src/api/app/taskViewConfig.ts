import { get, put } from '../request'
import type {
  TaskViewConfig,
  TaskViewContextType
} from '../../stores/tasks/viewConfigLocal'
import { defaultViewConfig } from '../../stores/tasks/viewConfigLocal'
import { normalizeFilters } from '../../stores/tasks/viewConfigFilters'

export async function getTaskViewConfig(params: {
  contextType: TaskViewContextType
  contextId?: string | null
}): Promise<TaskViewConfig> {
  const data = await get<TaskViewConfig>('/app-api/task/view-config/get', {
    params: {
      contextType: params.contextType,
      contextId: params.contextType === 'LIST' ? params.contextId : undefined
    }
  })
  return normalizeViewConfig(data, params.contextType)
}

export async function saveTaskViewConfig(body: {
  contextType: TaskViewContextType
  contextId?: string | null
  config: TaskViewConfig
}): Promise<void> {
  await put<boolean>('/app-api/task/view-config/save', {
    contextType: body.contextType,
    contextId: body.contextType === 'LIST' ? body.contextId : undefined,
    config: body.config
  })
}

function normalizeViewConfig(
  raw: TaskViewConfig | null | undefined,
  contextType: TaskViewContextType
): TaskViewConfig {
  const fallback = defaultViewConfig(contextType)
  if (!raw) {
    return fallback
  }
  return {
    displayMode: raw.displayMode === 'BOARD' ? 'BOARD' : 'LIST',
    groupBy: raw.groupBy ?? null,
    sort: raw.sort ?? fallback.sort,
    // Respect explicit empty list (user cleared); only fall back when missing
    filters: Array.isArray(raw.filters) ? normalizeFilters(raw.filters) : fallback.filters,
    visibleFieldKeys: raw.visibleFieldKeys?.length
      ? raw.visibleFieldKeys
      : fallback.visibleFieldKeys
  }
}

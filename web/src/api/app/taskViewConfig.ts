import { get, put } from '../request'
import type {
  TaskViewConfig,
  TaskViewContextType
} from '../../stores/tasks/viewConfigLocal'

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
  return normalizeViewConfig(data)
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

function normalizeViewConfig(raw: TaskViewConfig | null | undefined): TaskViewConfig {
  if (!raw) {
    return {
      displayMode: 'LIST',
      groupBy: null,
      sort: { key: 'createTime', order: 'DESC' },
      filters: [],
      visibleFieldKeys: ['dueTime', 'assignee', 'status']
    }
  }
  return {
    displayMode: raw.displayMode === 'BOARD' ? 'BOARD' : 'LIST',
    groupBy: raw.groupBy ?? null,
    sort: raw.sort ?? { key: 'createTime', order: 'DESC' },
    filters: Array.isArray(raw.filters) ? raw.filters : [],
    visibleFieldKeys: raw.visibleFieldKeys?.length
      ? raw.visibleFieldKeys
      : ['dueTime', 'assignee', 'status']
  }
}

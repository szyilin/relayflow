import { get, put } from '../request'

/** Org-level docs product settings (Wave B). */
export interface DocsProductSettings {
  memberShareEnabled: boolean
  markdownExportEnabled: boolean
  driveUploadEnabled: boolean
}

export const DEFAULT_DOCS_PRODUCT_SETTINGS: DocsProductSettings = {
  memberShareEnabled: true,
  markdownExportEnabled: true,
  driveUploadEnabled: true
}

export function cloneDocsProductSettings(source: DocsProductSettings): DocsProductSettings {
  return {
    memberShareEnabled: source.memberShareEnabled,
    markdownExportEnabled: source.markdownExportEnabled,
    driveUploadEnabled: source.driveUploadEnabled
  }
}

export function getDocsProductSettings(): Promise<DocsProductSettings> {
  return get<DocsProductSettings>('/admin-api/docs/product-settings')
}

export function updateDocsProductSettings(payload: DocsProductSettings): Promise<DocsProductSettings> {
  return put<DocsProductSettings>('/admin-api/docs/product-settings', payload)
}

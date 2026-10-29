export type SaveStatus = 'saved' | 'pending' | 'saving' | 'failed' | 'conflict'
export interface Draft<T> {
  id: string
  body: T
  savedBody: T
  version: number
  revision: number
  acknowledged: number
  status: SaveStatus
  error: unknown
  timer?: ReturnType<typeof setTimeout>
  flight?: Promise<void>
  controller?: AbortController
}
const copy = <T>(value: T): T => JSON.parse(JSON.stringify(value)) as T

/** No UI or transport globals: all work is scoped to the identity that created it. */
export class DocumentSaveQueue<T> {
  private drafts = new Map<string, Draft<T>>()
  private identity = ''
  private generation = 0
  private save: (id: string, body: T, version: number, signal: AbortSignal) => Promise<number>
  private changed: () => void
  private conflict: (error: unknown) => boolean
  constructor(save: (id: string, body: T, version: number, signal: AbortSignal) => Promise<number>,
    changed: () => void, conflict: (error: unknown) => boolean) {
    this.save = save; this.changed = changed; this.conflict = conflict
  }
  scope(identity: string) {
    if (identity === this.identity) return
    this.cancel(); this.identity = identity
  }
  cancel() {
    this.generation++
    for (const draft of this.drafts.values()) {
      clearTimeout(draft.timer); draft.controller?.abort()
    }
    this.drafts.clear(); this.changed()
  }
  get(id: string) { return this.drafts.get(id) }
  open(id: string, body: T, version: number) {
    let draft = this.get(id)
    if (!draft || (draft.acknowledged === draft.revision && !draft.flight)) {
      draft = { id, body: copy(body), savedBody: copy(body), version, revision: 0, acknowledged: 0, status: 'saved', error: null }
      this.drafts.set(id, draft)
    }
    this.changed(); return draft
  }
  edit(id: string, body: T, delay = 1200) {
    const draft = this.get(id)
    if (!draft) throw new Error('文档草稿尚未加载')
    draft.body = copy(body); draft.revision++
    clearTimeout(draft.timer)
    if (draft.status !== 'failed' && draft.status !== 'conflict') {
      draft.status = draft.flight ? 'saving' : 'pending'
      const generation = this.generation
      draft.timer = setTimeout(() => {
        if (generation === this.generation) void this.flush(id).catch(() => {})
      }, delay)
    }
    this.changed()
  }
  hasUnsaved() { return [...this.drafts.values()].some(d => d.revision !== d.acknowledged || d.flight) }
  discard(id: string) {
    const draft = this.get(id)
    if (draft?.flight) throw new Error('请等待当前保存结束')
    if (draft) clearTimeout(draft.timer)
    this.drafts.delete(id); this.changed()
  }
  async retry(id: string) {
    const draft = this.get(id)
    if (draft) { draft.status = 'pending'; draft.error = null }
    await this.flush(id)
  }
  async flush(id: string): Promise<void> {
    const draft = this.get(id)
    if (!draft) return
    const generation = this.generation
    clearTimeout(draft.timer)
    while (this.get(id) === draft && generation === this.generation) {
      if (draft.flight) { await draft.flight; continue }
      if (draft.status === 'failed' || draft.status === 'conflict') throw draft.error
      if (draft.revision === draft.acknowledged) return
      const revision = draft.revision, body = copy(draft.body), version = draft.version
      const controller = new AbortController(); draft.controller = controller; draft.status = 'saving'
      const flight = (async () => {
        try {
          const next = await this.save(id, body, version, controller.signal)
          if (generation !== this.generation || this.get(id) !== draft) return
          draft.version = next; draft.acknowledged = revision; draft.savedBody = body
          draft.status = draft.revision === revision ? 'saved' : 'pending'
        } catch (error) {
          if (generation !== this.generation || this.get(id) !== draft) return
          draft.error = error; draft.status = this.conflict(error) ? 'conflict' : 'failed'
          throw error
        } finally {
          if (generation === this.generation && this.get(id) === draft) {
            draft.flight = undefined; draft.controller = undefined; this.changed()
          }
        }
      })()
      draft.flight = flight; this.changed()
      await flight
    }
  }
}

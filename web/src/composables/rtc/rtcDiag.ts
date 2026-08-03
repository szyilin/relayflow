/**
 * Client-side RTC join diagnostics — ring buffer + timings for post-hoc analysis.
 * Export is sanitized (no full LiveKit JWT).
 */

export type RtcDiagEvent = {
  t: number
  name: string
  detail?: Record<string, unknown>
}

const MAX_EVENTS = 120

const startedAt = Date.now()
const events: RtcDiagEvent[] = []
const timings: Record<string, number | null> = {}
const marks: Record<string, number> = {}
let meta: Record<string, unknown> = {}

function now() {
  return Date.now()
}

export function rtcDiagReset(nextMeta?: Record<string, unknown>) {
  events.length = 0
  for (const k of Object.keys(timings)) {
    delete timings[k]
  }
  for (const k of Object.keys(marks)) {
    delete marks[k]
  }
  meta = { ...(nextMeta || {}) }
  rtcDiagEvent('diag.reset')
}

export function rtcDiagSetMeta(patch: Record<string, unknown>) {
  meta = { ...meta, ...patch }
}

export function rtcDiagEvent(name: string, detail?: Record<string, unknown>) {
  events.push({ t: now(), name, detail })
  if (events.length > MAX_EVENTS) {
    events.splice(0, events.length - MAX_EVENTS)
  }
  if (import.meta.env.DEV) {
    // Compact line for terminal / AI paste
    // eslint-disable-next-line no-console
    console.info(`[rtc-diag] ${name}`, detail ?? '')
  }
}

export function rtcDiagMark(label: string) {
  marks[label] = now()
  rtcDiagEvent(`mark:${label}`)
}

export function rtcDiagEnd(label: string, timingKey?: string) {
  const start = marks[label]
  const key = timingKey || label
  if (start != null) {
    timings[key] = now() - start
    delete marks[label]
    rtcDiagEvent(`end:${label}`, { ms: timings[key] })
  } else {
    rtcDiagEvent(`end:${label}`, { ms: null, missingStart: true })
  }
}

export function rtcDiagSetTiming(key: string, ms: number | null) {
  timings[key] = ms
}

export function rtcDiagHasMark(label: string) {
  return marks[label] != null
}

export function rtcDiagHasTiming(key: string) {
  return timings[key] != null
}

/** Record first decoded/local video frame once. */
export function rtcDiagNoteFirstFrame(via: string) {
  if (rtcDiagHasTiming('firstFrameMs')) {
    return
  }
  if (rtcDiagHasMark('firstFrame')) {
    rtcDiagEnd('firstFrame', 'firstFrameMs')
    return
  }
  rtcDiagSetTiming('firstFrameMs', 0)
  rtcDiagEvent('firstFrame', { via })
}

function sanitizeToken(token: string | undefined | null) {
  if (!token) {
    return null
  }
  return {
    length: token.length,
    prefix: token.slice(0, 8)
  }
}

function hostOf(url: string | undefined | null) {
  if (!url) {
    return null
  }
  try {
    return new URL(url.replace(/^ws/i, 'http')).host
  } catch {
    return url.slice(0, 64)
  }
}

export function rtcDiagExportSnapshot(extra?: Record<string, unknown>) {
  const rawToken = typeof meta.token === 'string' ? meta.token : null
  const { token: _drop, ...restMeta } = meta
  return {
    version: 1,
    exportedAt: new Date().toISOString(),
    sincePageMs: now() - startedAt,
    meta: {
      ...restMeta,
      livekitHost: hostOf(typeof meta.livekitUrl === 'string' ? meta.livekitUrl : null),
      token: sanitizeToken(rawToken)
    },
    timings: { ...timings },
    events: events.map((e) => ({
      ...e,
      offsetMs: e.t - startedAt
    })),
    ...extra
  }
}

export function rtcDiagExportJson(extra?: Record<string, unknown>) {
  return JSON.stringify(rtcDiagExportSnapshot(extra), null, 2)
}

export async function rtcDiagCopyToClipboard(extra?: Record<string, unknown>) {
  const text = rtcDiagExportJson(extra)
  await navigator.clipboard.writeText(text)
  return text
}

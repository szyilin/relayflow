import type { LocalTrack } from 'livekit-client'

/**
 * Pre-join → room LocalTrack handoff (session-scoped).
 * Survives modal unmount: only `take` or explicit clear(stop) releases tracks.
 */
let pendingTracks: LocalTrack[] | null = null
/** Generation bumped on set/clear/take — room can detect stale takes. */
let handoffGen = 0
let handoffAt = 0

export function setMeetingTrackHandoff(tracks: LocalTrack[]) {
  // Replace without stopping the new tracks; stop only previous pending.
  if (pendingTracks?.length) {
    for (const t of pendingTracks) {
      if (!tracks.includes(t)) {
        try {
          t.stop()
        } catch {
          // ignore
        }
      }
    }
  }
  pendingTracks = tracks.length ? [...tracks] : null
  handoffGen += 1
  handoffAt = Date.now()
}

/** Take ownership once; caller must publish or stop. */
export function takeMeetingTrackHandoff(): LocalTrack[] | null {
  const tracks = pendingTracks
  pendingTracks = null
  if (tracks?.length) {
    handoffGen += 1
  }
  return tracks
}

export function peekMeetingTrackHandoff(): LocalTrack[] | null {
  return pendingTracks
}

export function meetingTrackHandoffMeta() {
  return {
    count: pendingTracks?.length ?? 0,
    gen: handoffGen,
    ageMs: pendingTracks ? Date.now() - handoffAt : null,
    kinds: pendingTracks?.map(t => t.kind) ?? []
  }
}

/**
 * Clear handoff. Use stop:true only when abandoning (user cancel).
 * Never call stop from modal unmount after a successful confirm — room owns tracks via take().
 */
export function clearMeetingTrackHandoff(opts?: { stop?: boolean }) {
  if (opts?.stop && pendingTracks) {
    for (const t of pendingTracks) {
      try {
        t.stop()
      } catch {
        // ignore
      }
    }
  }
  pendingTracks = null
  handoffGen += 1
}

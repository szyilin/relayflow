/**
 * Shared LiveKit room helpers — reusable by meeting UI and future IM calls.
 * Media connects browser ↔ LiveKit directly; RelayFlow only supplies tokens.
 */
import { computed, ref, shallowRef, type ShallowRef } from 'vue'
import {
  Room,
  RoomEvent,
  Track,
  VideoPresets,
  type LocalTrack,
  type Participant,
  type RemoteTrack,
  type TrackPublication
} from 'livekit-client'

export interface RtcConnectOptions {
  url: string
  token: string
  audio?: boolean
  video?: boolean
  audioDeviceId?: string
  videoDeviceId?: string
}

export type RtcConnectPhase = 'idle' | 'connecting' | 'publishing' | 'connected' | 'error'

export interface RtcTileParticipant {
  identity: string
  name: string
  isLocal: boolean
  isSpeaking: boolean
  isMuted: boolean
  hasVideo: boolean
  videoTrack?: RemoteTrack | LocalTrack | null
  audioTrack?: RemoteTrack | LocalTrack | null
}

const DEVICE_ENABLE_TIMEOUT_MS = 12_000
const SIGNAL_CONNECT_TIMEOUT_MS = 12_000

function withTimeout<T>(promise: Promise<T>, ms: number, label: string): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const timer = window.setTimeout(() => {
      reject(new Error(`${label}超时（${Math.round(ms / 1000)}s）`))
    }, ms)
    promise.then(
      (v) => {
        window.clearTimeout(timer)
        resolve(v)
      },
      (e) => {
        window.clearTimeout(timer)
        reject(e)
      }
    )
  })
}

export function useRtcRoom() {
  const room: ShallowRef<Room | null> = shallowRef(null)
  const connected = ref(false)
  const connecting = ref(false)
  const phase = ref<RtcConnectPhase>('idle')
  const error = ref<string | null>(null)
  const activeSpeakerIds = ref<string[]>([])
  const revision = ref(0)
  const micEnabled = ref(false)
  const camEnabled = ref(false)
  let speakerBumpTimer: ReturnType<typeof setTimeout> | null = null
  /** Monotonic id so a remount / rapid re-connect cannot finish on a stale attempt. */
  let connectGen = 0
  let lastConnectOpts: RtcConnectOptions | null = null

  const participants = computed(() => {
    revision.value
    return collectTiles(room.value, activeSpeakerIds.value)
  })

  function bump() {
    revision.value += 1
    syncDeviceFlags()
  }

  function syncDeviceFlags() {
    const local = room.value?.localParticipant
    if (!local) {
      micEnabled.value = false
      camEnabled.value = false
      return
    }
    micEnabled.value = local.isMicrophoneEnabled
    camEnabled.value = local.isCameraEnabled
  }

  /** Speaking updates are high-frequency; coalesce to reduce tile churn. */
  function bumpSpeakersSoon() {
    if (speakerBumpTimer != null) {
      return
    }
    speakerBumpTimer = setTimeout(() => {
      speakerBumpTimer = null
      bump()
    }, 100)
  }

  function bindRoomEvents(r: Room) {
    r
      .on(RoomEvent.ActiveSpeakersChanged, (speakers: Participant[]) => {
        activeSpeakerIds.value = speakers.map(s => s.identity)
        bumpSpeakersSoon()
      })
      .on(RoomEvent.TrackSubscribed, () => bump())
      .on(RoomEvent.TrackUnsubscribed, () => bump())
      .on(RoomEvent.TrackMuted, () => bump())
      .on(RoomEvent.TrackUnmuted, () => bump())
      .on(RoomEvent.TrackPublished, () => bump())
      .on(RoomEvent.LocalTrackPublished, () => bump())
      .on(RoomEvent.LocalTrackUnpublished, () => bump())
      .on(RoomEvent.ParticipantConnected, () => bump())
      .on(RoomEvent.ParticipantDisconnected, () => bump())
      .on(RoomEvent.Disconnected, () => {
        connected.value = false
        micEnabled.value = false
        camEnabled.value = false
        if (phase.value !== 'error') {
          phase.value = 'idle'
        }
        bump()
      })
  }

  function micCaptureOptions(deviceId?: string) {
    return deviceId ? { deviceId } : undefined
  }

  function camCaptureOptions(deviceId?: string) {
    return {
      resolution: VideoPresets.h360.resolution,
      ...(deviceId ? { deviceId } : {})
    }
  }

  async function connect(opts: RtcConnectOptions) {
    error.value = null
    if (!opts.url || !opts.token) {
      connected.value = false
      connecting.value = false
      phase.value = 'error'
      throw new Error('入会凭证不完整')
    }
    const gen = ++connectGen
    lastConnectOpts = { ...opts }
    connecting.value = true
    phase.value = 'connecting'
    // Optimistic UI: reflect join intent immediately (footer), tiles catch up via tracks.
    micEnabled.value = opts.audio !== false
    camEnabled.value = opts.video === true
    try {
      await disconnect({ preservePhase: true, skipGenBump: true })
      if (gen !== connectGen) {
        return
      }

      const r = new Room({
        // adaptiveStream pauses remote video by element visibility; leave off for
        // predictable local preview when alone. dynacast similarly hurts solo publish.
        adaptiveStream: false,
        dynacast: false,
        // Avoid freeze/pagehide tearing down media during Mac Chrome backgrounding.
        disconnectOnPageLeave: false,
        stopLocalTrackOnUnpublish: true
      })
      bindRoomEvents(r)

      await withTimeout(
        r.connect(opts.url, opts.token),
        SIGNAL_CONNECT_TIMEOUT_MS,
        '信令连接'
      )
      if (gen !== connectGen) {
        await r.disconnect(true)
        return
      }

      room.value = r
      connected.value = true
      phase.value = 'publishing'
      bump()

      // Publish devices with a hard timeout so we never stick on「正在开启设备…」.
      // Failures are soft: room stays connected; user can retry via toolbar.
      const pubTasks: Promise<unknown>[] = []
      if (opts.audio !== false) {
        pubTasks.push(
          withTimeout(
            r.localParticipant.setMicrophoneEnabled(true, micCaptureOptions(opts.audioDeviceId)),
            DEVICE_ENABLE_TIMEOUT_MS,
            '开启麦克风'
          ).catch((e) => {
            console.warn('enable microphone failed', e)
            micEnabled.value = false
          })
        )
      } else {
        micEnabled.value = false
      }
      if (opts.video === true) {
        pubTasks.push(
          withTimeout(
            r.localParticipant.setCameraEnabled(true, camCaptureOptions(opts.videoDeviceId)),
            DEVICE_ENABLE_TIMEOUT_MS,
            '开启摄像头'
          ).catch((e) => {
            console.warn('enable camera failed', e)
            camEnabled.value = false
          })
        )
      } else {
        camEnabled.value = false
      }
      await Promise.all(pubTasks)
      if (gen !== connectGen) {
        return
      }
      phase.value = 'connected'
      bump()
    } catch (e) {
      if (gen !== connectGen) {
        return
      }
      error.value = e instanceof Error ? e.message : '连接会议失败'
      connected.value = false
      phase.value = 'error'
      await disconnect({ skipGenBump: true })
      throw e
    } finally {
      if (gen === connectGen) {
        connecting.value = false
      }
    }
  }

  /** Soft reconnect with a fresh token while preserving mic/cam intent. */
  async function refreshToken(token: string, url?: string) {
    const prev = lastConnectOpts
    if (!prev) {
      throw new Error('尚未入会，无法刷新凭证')
    }
    const audio = room.value?.localParticipant.isMicrophoneEnabled ?? prev.audio !== false
    const video = room.value?.localParticipant.isCameraEnabled ?? prev.video === true
    await connect({
      ...prev,
      url: url || prev.url,
      token,
      audio,
      video
    })
  }

  async function disconnect(opts?: { preservePhase?: boolean, skipGenBump?: boolean }) {
    if (!opts?.skipGenBump) {
      connectGen += 1
    }
    if (speakerBumpTimer != null) {
      clearTimeout(speakerBumpTimer)
      speakerBumpTimer = null
    }
    const r = room.value
    room.value = null
    connected.value = false
    micEnabled.value = false
    camEnabled.value = false
    activeSpeakerIds.value = []
    if (!opts?.preservePhase && phase.value !== 'error') {
      phase.value = 'idle'
    }
    if (r) {
      // Do not await setMicrophone/CameraEnabled(false) — that can hang on device locks
      // and delayed the previous reconnect storm. Room.disconnect(stopTracks=true) stops them.
      try {
        await r.disconnect(true)
      } catch {
        // ignore
      }
    }
    bump()
  }

  async function setMicEnabled(enabled: boolean) {
    const r = room.value
    if (!r) {
      return
    }
    micEnabled.value = enabled
    try {
      await withTimeout(
        r.localParticipant.setMicrophoneEnabled(
          enabled,
          enabled ? micCaptureOptions(lastConnectOpts?.audioDeviceId) : undefined
        ),
        DEVICE_ENABLE_TIMEOUT_MS,
        enabled ? '开启麦克风' : '关闭麦克风'
      )
    } catch (e) {
      micEnabled.value = r.localParticipant.isMicrophoneEnabled
      throw e
    } finally {
      bump()
    }
  }

  async function setCameraEnabled(enabled: boolean) {
    const r = room.value
    if (!r) {
      return
    }
    camEnabled.value = enabled
    try {
      await withTimeout(
        r.localParticipant.setCameraEnabled(
          enabled,
          enabled ? camCaptureOptions(lastConnectOpts?.videoDeviceId) : undefined
        ),
        DEVICE_ENABLE_TIMEOUT_MS,
        enabled ? '开启摄像头' : '关闭摄像头'
      )
    } catch (e) {
      camEnabled.value = r.localParticipant.isCameraEnabled
      throw e
    } finally {
      bump()
    }
  }

  async function sendChat(text: string) {
    const payload = new TextEncoder().encode(JSON.stringify({
      type: 'meeting.chat',
      text,
      ts: Date.now()
    }))
    await room.value?.localParticipant.publishData(payload, { reliable: true })
  }

  function onChat(handler: (msg: { text: string, from: string }) => void) {
    const r = room.value
    if (!r) {
      return () => {}
    }
    const listener = (
      payload: Uint8Array,
      participant?: Participant
    ) => {
      try {
        const raw = JSON.parse(new TextDecoder().decode(payload)) as { type?: string, text?: string }
        if (raw?.type === 'meeting.chat' && raw.text) {
          handler({
            text: raw.text,
            from: participant?.name || participant?.identity || '未知'
          })
        }
      } catch {
        // ignore non-json
      }
    }
    r.on(RoomEvent.DataReceived, listener)
    return () => {
      r.off(RoomEvent.DataReceived, listener)
    }
  }

  return {
    room,
    connected,
    connecting,
    phase,
    error,
    participants,
    activeSpeakerIds,
    micEnabled,
    camEnabled,
    connect,
    refreshToken,
    disconnect,
    setMicEnabled,
    setCameraEnabled,
    sendChat,
    onChat
  }
}

function trackIsLive(track: LocalTrack | RemoteTrack | null | undefined): boolean {
  if (!track) {
    return false
  }
  const media = track.mediaStreamTrack
  if (!media) {
    return !!track
  }
  return media.readyState === 'live'
}

function collectTiles(room: Room | null, speakingIds: string[]): RtcTileParticipant[] {
  if (!room) {
    return []
  }
  const speaking = new Set(speakingIds)
  const tiles: RtcTileParticipant[] = []

  const push = (p: Participant, isLocal: boolean) => {
    let videoTrack: RemoteTrack | LocalTrack | null = null
    let audioTrack: RemoteTrack | LocalTrack | null = null
    let isMuted = true
    let hasVideo = false

    p.trackPublications.forEach((pub: TrackPublication) => {
      // Prefer camera over screenshare for the main tile.
      if (pub.kind === Track.Kind.Video && pub.source === Track.Source.Camera && pub.track) {
        // Keep track reference even while muted so the <video> attach is not torn down.
        videoTrack = pub.track as RemoteTrack | LocalTrack
        // Local: show as long as the MediaStreamTrack is live (browser may briefly
        // flip publication muted during device startup). Remote: respect mute.
        if (isLocal) {
          hasVideo = trackIsLive(videoTrack) && !pub.isMuted
          // If track is live but pub still reports muted during startup, still show.
          if (trackIsLive(videoTrack) && p.isCameraEnabled) {
            hasVideo = true
          }
        } else {
          hasVideo = !pub.isMuted && trackIsLive(videoTrack)
        }
      } else if (pub.kind === Track.Kind.Video && pub.track && !videoTrack) {
        videoTrack = pub.track as RemoteTrack | LocalTrack
        hasVideo = !pub.isMuted && trackIsLive(videoTrack)
      }
      if (pub.kind === Track.Kind.Audio && pub.source === Track.Source.Microphone) {
        if (pub.track) {
          audioTrack = pub.track as RemoteTrack | LocalTrack
        }
        isMuted = pub.isMuted || !pub.track
      }
    })

    // Prefer LiveKit's aggregate flags when publications are mid-flight.
    if (isLocal) {
      isMuted = !p.isMicrophoneEnabled
      if (p.isCameraEnabled && videoTrack && trackIsLive(videoTrack)) {
        hasVideo = true
      }
    }

    tiles.push({
      identity: p.identity,
      name: p.name || p.identity,
      isLocal,
      isSpeaking: speaking.has(p.identity),
      isMuted,
      hasVideo,
      videoTrack,
      audioTrack
    })
  }

  push(room.localParticipant, true)
  room.remoteParticipants.forEach((p) => push(p, false))

  tiles.sort((a, b) => {
    if (a.isSpeaking !== b.isSpeaking) {
      return a.isSpeaking ? -1 : 1
    }
    if (a.isLocal !== b.isLocal) {
      return a.isLocal ? 1 : -1
    }
    return a.name.localeCompare(b.name)
  })
  return tiles
}

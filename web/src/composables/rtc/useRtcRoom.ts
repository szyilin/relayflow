/**
 * Shared LiveKit room helpers — reusable by meeting UI and future IM calls.
 * Media connects browser ↔ LiveKit directly; RelayFlow only supplies tokens.
 */
import { computed, ref, shallowRef, type ShallowRef } from 'vue'
import {
  Room,
  RoomEvent,
  Track,
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

export function useRtcRoom() {
  const room: ShallowRef<Room | null> = shallowRef(null)
  const connected = ref(false)
  const connecting = ref(false)
  const phase = ref<RtcConnectPhase>('idle')
  const error = ref<string | null>(null)
  const activeSpeakerIds = ref<string[]>([])
  const revision = ref(0)

  const participants = computed(() => {
    revision.value
    return collectTiles(room.value, activeSpeakerIds.value)
  })

  function bump() {
    revision.value += 1
  }

  async function connect(opts: RtcConnectOptions) {
    error.value = null
    if (!opts.url || !opts.token) {
      connected.value = false
      connecting.value = false
      phase.value = 'error'
      throw new Error('入会凭证不完整')
    }
    connecting.value = true
    phase.value = 'connecting'
    try {
      await disconnect()
      const r = new Room({
        adaptiveStream: true,
        dynacast: true,
        // Fail faster than default when ICE/signaling hangs (common with wrong node-ip).
        disconnectOnPageLeave: true
      })
      r
        .on(RoomEvent.ActiveSpeakersChanged, (speakers: Participant[]) => {
          activeSpeakerIds.value = speakers.map(s => s.identity)
          bump()
        })
        .on(RoomEvent.TrackSubscribed, () => bump())
        .on(RoomEvent.TrackUnsubscribed, () => bump())
        .on(RoomEvent.TrackMuted, () => bump())
        .on(RoomEvent.TrackUnmuted, () => bump())
        .on(RoomEvent.ParticipantConnected, () => bump())
        .on(RoomEvent.ParticipantDisconnected, () => bump())
        .on(RoomEvent.LocalTrackPublished, () => bump())
        .on(RoomEvent.LocalTrackUnpublished, () => bump())
        .on(RoomEvent.Disconnected, () => {
          connected.value = false
          phase.value = 'idle'
          bump()
        })

      // Signal first so UI can leave "connecting" as soon as room is up.
      await Promise.race([
        r.connect(opts.url, opts.token),
        new Promise<never>((_, reject) => {
          window.setTimeout(() => reject(new Error('连接超时，请检查 LiveKit 是否启动或网络/防火墙')), 20_000)
        })
      ])
      room.value = r
      connected.value = true
      phase.value = 'publishing'
      bump()

      // Acquire devices after signaling — do not block "connected" forever on getUserMedia.
      const pubTasks: Promise<unknown>[] = []
      if (opts.audio !== false) {
        pubTasks.push(r.localParticipant.setMicrophoneEnabled(true).catch((e) => {
          console.warn('enable microphone failed', e)
        }))
      }
      if (opts.video === true) {
        pubTasks.push(r.localParticipant.setCameraEnabled(true).catch((e) => {
          console.warn('enable camera failed', e)
        }))
      }
      await Promise.all(pubTasks)
      phase.value = 'connected'
      bump()
    } catch (e) {
      error.value = e instanceof Error ? e.message : '连接会议失败'
      connected.value = false
      phase.value = 'error'
      await disconnect()
      throw e
    } finally {
      connecting.value = false
    }
  }

  async function disconnect() {
    const r = room.value
    room.value = null
    connected.value = false
    activeSpeakerIds.value = []
    if (phase.value !== 'error') {
      phase.value = 'idle'
    }
    if (r) {
      try {
        // Ensure local mic/cam tracks are stopped (browser indicator).
        await r.localParticipant.setMicrophoneEnabled(false).catch(() => {})
        await r.localParticipant.setCameraEnabled(false).catch(() => {})
      } catch {
        // ignore
      }
      await r.disconnect(true)
    }
    bump()
  }

  async function setMicEnabled(enabled: boolean) {
    await room.value?.localParticipant.setMicrophoneEnabled(enabled)
    bump()
  }

  async function setCameraEnabled(enabled: boolean) {
    await room.value?.localParticipant.setCameraEnabled(enabled)
    bump()
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
    connect,
    disconnect,
    setMicEnabled,
    setCameraEnabled,
    sendChat,
    onChat
  }
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
      if (pub.kind === Track.Kind.Video && pub.track && !pub.isMuted) {
        videoTrack = pub.track as RemoteTrack | LocalTrack
        hasVideo = true
      }
      if (pub.kind === Track.Kind.Audio) {
        isMuted = pub.isMuted || !pub.track
        if (pub.track) {
          audioTrack = pub.track as RemoteTrack | LocalTrack
        }
      }
    })
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

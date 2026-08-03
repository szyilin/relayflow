/**
 * Shared LiveKit room helpers — reusable by meeting UI and future IM calls.
 * Media connects browser ↔ LiveKit directly; RelayFlow only supplies tokens.
 */
import { computed, ref, shallowRef, type ShallowRef } from 'vue'
import {
  ConnectionState,
  createLocalTracks,
  Room,
  RoomEvent,
  Track,
  VideoPresets,
  type LocalTrack,
  type Participant,
  type RemoteTrack,
  type TrackPublication
} from 'livekit-client'
import { meetingTrackHandoffMeta, takeMeetingTrackHandoff } from './meetingTrackHandoff'
import {
  rtcDiagEnd,
  rtcDiagEvent,
  rtcDiagMark,
  rtcDiagNoteFirstFrame,
  rtcDiagSetMeta
} from './rtcDiag'

export interface RtcConnectOptions {
  url: string
  token: string
  audio?: boolean
  video?: boolean
  audioDeviceId?: string
  videoDeviceId?: string
  /** Optional pre-acquired tracks; defaults to pre-join handoff. */
  localTracks?: LocalTrack[]
}

export type RtcConnectPhase = 'idle' | 'connecting' | 'publishing' | 'connected' | 'error'

export interface RtcTileParticipant {
  identity: string
  name: string
  isLocal: boolean
  isSpeaking: boolean
  isMuted: boolean
  hasVideo: boolean
  /** 0–1 speaking level (LiveKit participant.audioLevel). */
  audioLevel: number
  videoTrack?: RemoteTrack | LocalTrack | null
  audioTrack?: RemoteTrack | LocalTrack | null
}

const DEVICE_ENABLE_TIMEOUT_MS = 20_000
const SIGNAL_CONNECT_TIMEOUT_MS = 12_000
const PREPARE_TIMEOUT_MS = 8_000
const TOKEN_REFRESH_MIN_INTERVAL_MS = 30_000
/** Background publish should fail soft; do not block toolbar. */
const PUBLISH_SOFT_TIMEOUT_MS = 12_000
const ENSURE_PUBLISH_DELAY_MS = 1_500
const MAX_ENSURE_ATTEMPTS = 2
const AUDIO_LEVEL_POLL_MS = 120

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

function stopTracksQuietly(tracks: LocalTrack[] | null | undefined) {
  if (!tracks?.length) {
    return
  }
  for (const t of tracks) {
    try {
      t.stop()
    } catch {
      // ignore
    }
  }
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
  /** Keep pre-join LocalTracks for local tile even while SFU publication flaps. */
  const localPreviewVideo = shallowRef<LocalTrack | null>(null)
  const localPreviewAudio = shallowRef<LocalTrack | null>(null)
  let speakerBumpTimer: ReturnType<typeof setTimeout> | null = null
  /** Monotonic id so a remount / rapid re-connect cannot finish on a stale attempt. */
  let connectGen = 0
  let lastConnectOpts: RtcConnectOptions | null = null
  let lastTokenRefreshAt = 0
  let tokenRefreshInFlight = false
  let ensurePublishTimer: ReturnType<typeof setTimeout> | null = null
  let ensureAttempts = 0
  let publishInFlight = false
  let deviceOpInFlight = false
  let audioLevelTimer: ReturnType<typeof setInterval> | null = null

  const participants = computed(() => {
    revision.value
    return collectTiles(
      room.value,
      activeSpeakerIds.value,
      localPreviewVideo.value,
      localPreviewAudio.value,
      micEnabled.value,
      camEnabled.value
    )
  })

  /** Only lock while establishing signal — never during SFU publish / reconnect flaps. */
  const mediaControlsLocked = computed(
    () => phase.value === 'connecting' || (connecting.value && phase.value !== 'connected')
  )

  function bump(opts?: { syncDevices?: boolean }) {
    revision.value += 1
    if (opts?.syncDevices === true) {
      syncDeviceFlagsFromLiveKit()
    }
  }

  function startAudioLevelPolling() {
    stopAudioLevelPolling()
    audioLevelTimer = setInterval(() => {
      if (!room.value || room.value.state === ConnectionState.Disconnected) {
        return
      }
      bump({ syncDevices: false })
    }, AUDIO_LEVEL_POLL_MS)
  }

  function stopAudioLevelPolling() {
    if (audioLevelTimer != null) {
      clearInterval(audioLevelTimer)
      audioLevelTimer = null
    }
  }

  /**
   * Toolbar mic/cam are optimistic (desired). LiveKit flaps during reconnect must not
   * overwrite them — that caused "buttons stuck off" after a brief unpublish.
   * We only log divergence for diagnostics.
   */
  function syncDeviceFlagsFromLiveKit() {
    const local = room.value?.localParticipant
    if (!local || deviceOpInFlight) {
      return
    }
    if (room.value?.state !== ConnectionState.Connected) {
      return
    }
    const nextMic = local.isMicrophoneEnabled
    const nextCam = local.isCameraEnabled
    if (micEnabled.value !== nextMic || camEnabled.value !== nextCam) {
      rtcDiagEvent('deviceFlags.livekit', {
        mic: nextMic,
        cam: nextCam,
        uiMic: micEnabled.value,
        uiCam: camEnabled.value
      })
    }
  }

  function rememberPreviewTracks(tracks: LocalTrack[] | null | undefined) {
    localPreviewVideo.value = null
    localPreviewAudio.value = null
    if (!tracks?.length) {
      return
    }
    for (const t of tracks) {
      if (t.kind === Track.Kind.Video) {
        localPreviewVideo.value = t
      } else if (t.kind === Track.Kind.Audio) {
        localPreviewAudio.value = t
      }
    }
  }

  async function ensureMediaPublished(r: Room) {
    if (r.state !== ConnectionState.Connected) {
      return
    }
    if (deviceOpInFlight || publishInFlight) {
      rtcDiagEvent('ensureMediaPublished.skipped', {
        reason: deviceOpInFlight ? 'deviceOp' : 'publishInFlight'
      })
      return
    }
    if (ensureAttempts >= MAX_ENSURE_ATTEMPTS) {
      rtcDiagEvent('ensureMediaPublished.skipped', { reason: 'maxAttempts', ensureAttempts })
      return
    }
    const wantAudio = micEnabled.value
    const wantVideo = camEnabled.value
    const pubs = snapshotLocalPubs(r)
    const hasMic = r.localParticipant.isMicrophoneEnabled
    const hasCam = r.localParticipant.isCameraEnabled
    if ((!wantAudio || hasMic) && (!wantVideo || hasCam)) {
      rtcDiagEvent('ensureMediaPublished.ok', { wantAudio, wantVideo, localPubs: pubs })
      return
    }
    ensureAttempts += 1
    rtcDiagEvent('ensureMediaPublished', {
      wantAudio,
      wantVideo,
      localPubs: pubs,
      camEnabled: hasCam,
      micEnabled: hasMic,
      attempt: ensureAttempts
    })
    publishInFlight = true
    try {
      if (wantVideo && !hasCam) {
        const preview = localPreviewVideo.value
        if (preview?.mediaStreamTrack?.readyState === 'live') {
          await r.localParticipant.publishTrack(preview, {
            source: Track.Source.Camera,
            simulcast: false,
            videoCodec: 'vp8'
          })
        } else {
          await r.localParticipant.setCameraEnabled(
            true,
            camCaptureOptions(lastConnectOpts?.videoDeviceId)
          )
        }
      }
      if (wantAudio && !r.localParticipant.isMicrophoneEnabled) {
        const preview = localPreviewAudio.value
        if (preview?.mediaStreamTrack?.readyState === 'live') {
          await r.localParticipant.publishTrack(preview, {
            source: Track.Source.Microphone,
            red: false
          })
        } else {
          await r.localParticipant.setMicrophoneEnabled(
            true,
            micCaptureOptions(lastConnectOpts?.audioDeviceId)
          )
        }
      }
    } catch (e) {
      rtcDiagEvent('ensureMediaPublished.failed', {
        message: e instanceof Error ? e.message : String(e)
      })
    } finally {
      publishInFlight = false
      bump({ syncDevices: false })
    }
  }

  function scheduleEnsurePublished(r: Room) {
    if (ensurePublishTimer != null) {
      clearTimeout(ensurePublishTimer)
    }
    ensurePublishTimer = setTimeout(() => {
      ensurePublishTimer = null
      void ensureMediaPublished(r)
    }, ENSURE_PUBLISH_DELAY_MS)
  }

  /** Speaking updates are high-frequency; coalesce and skip device sync. */
  function bumpSpeakersSoon() {
    if (speakerBumpTimer != null) {
      return
    }
    speakerBumpTimer = setTimeout(() => {
      speakerBumpTimer = null
      bump({ syncDevices: false })
    }, 100)
  }

  function bindRoomEvents(r: Room) {
    r
      .on(RoomEvent.ActiveSpeakersChanged, (speakers: Participant[]) => {
        activeSpeakerIds.value = speakers.map(s => s.identity)
        bumpSpeakersSoon()
      })
      .on(RoomEvent.TrackSubscribed, (_track, pub, participant) => {
        rtcDiagEvent('TrackSubscribed', {
          identity: participant.identity,
          source: pub.source,
          kind: pub.kind
        })
        bump()
      })
      .on(RoomEvent.TrackUnsubscribed, () => bump())
      .on(RoomEvent.TrackMuted, (_pub, participant) => {
        rtcDiagEvent('TrackMuted', { identity: participant.identity })
        bump()
      })
      .on(RoomEvent.TrackUnmuted, (_pub, participant) => {
        rtcDiagEvent('TrackUnmuted', { identity: participant.identity })
        bump()
      })
      .on(RoomEvent.TrackPublished, () => bump())
      .on(RoomEvent.LocalTrackPublished, (pub) => {
        rtcDiagEvent('LocalTrackPublished', { source: pub.source, kind: pub.kind })
        if (pub.source === Track.Source.Camera) {
          rtcDiagMark('firstFrame')
        }
        bump({ syncDevices: false })
      })
      .on(RoomEvent.LocalTrackUnpublished, (pub) => {
        rtcDiagEvent('LocalTrackUnpublished', { source: pub.source })
        bump({ syncDevices: false })
      })
      .on(RoomEvent.ParticipantConnected, (p) => {
        rtcDiagEvent('ParticipantConnected', { identity: p.identity })
        bump({ syncDevices: false })
      })
      .on(RoomEvent.ParticipantDisconnected, (p) => {
        rtcDiagEvent('ParticipantDisconnected', { identity: p.identity })
        bump({ syncDevices: false })
      })
      .on(RoomEvent.Reconnecting, () => {
        rtcDiagEvent('Reconnecting', {
          connectionState: r.state,
          localPubs: snapshotLocalPubs(r)
        })
      })
      .on(RoomEvent.SignalReconnecting, () => {
        rtcDiagEvent('SignalReconnecting', { connectionState: r.state })
      })
      .on(RoomEvent.Reconnected, () => {
        rtcDiagEvent('Reconnected', {
          connectionState: r.state,
          localPubs: snapshotLocalPubs(r)
        })
        bump({ syncDevices: false })
        // Soft recover missing pubs after settle — capped attempts, skips if publish in flight.
        scheduleEnsurePublished(r)
      })
      .on(RoomEvent.ConnectionStateChanged, (state: ConnectionState) => {
        rtcDiagEvent('ConnectionStateChanged', {
          state,
          localPubs: snapshotLocalPubs(r)
        })
      })
      .on(RoomEvent.MediaDevicesError, (e: Error) => {
        rtcDiagEvent('MediaDevicesError', { message: e.message })
      })
      .on(RoomEvent.Disconnected, (reason) => {
        rtcDiagEvent('Disconnected', {
          reason: reason != null ? String(reason) : undefined,
          connectionState: r.state,
          localPubs: snapshotLocalPubs(r)
        })
        connected.value = false
        stopAudioLevelPolling()
        if (phase.value !== 'error') {
          phase.value = 'idle'
        }
        bump({ syncDevices: false })
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

  async function publishHandoffTracks(r: Room, tracks: LocalTrack[]) {
    publishInFlight = true
    // Parallel publish — sequential made audio wait behind a slow video renegotiation.
    const tasks = tracks.map(async (track) => {
      const label = track.kind === Track.Kind.Video ? 'publishVideo' : 'publishAudio'
      const timingKey = track.kind === Track.Kind.Video ? 'publishVideoMs' : 'publishAudioMs'
      rtcDiagMark(label)
      try {
        await withTimeout(
          r.localParticipant.publishTrack(track, {
            source: track.source,
            simulcast: false,
            red: false,
            ...(track.kind === Track.Kind.Video ? { videoCodec: 'vp8' as const } : {})
          }),
          PUBLISH_SOFT_TIMEOUT_MS,
          track.kind === Track.Kind.Video ? '发布摄像头' : '发布麦克风'
        )
        rtcDiagEnd(label, timingKey)
        rtcDiagEvent('publishTrack.ok', {
          kind: track.kind,
          source: track.source,
          localPubs: snapshotLocalPubs(r)
        })
      } catch (e) {
        rtcDiagEnd(label, timingKey)
        const alreadyLive = [...r.localParticipant.trackPublications.values()].some(
          (pub) => pub.track === track || (pub.source === track.source && !!pub.track)
        )
        rtcDiagEvent('publishTrack.failed', {
          kind: track.kind,
          message: e instanceof Error ? e.message : String(e),
          alreadyLive,
          localPubs: snapshotLocalPubs(r)
        })
        console.warn('publish handoff track failed', e)
        // Never stop on soft timeout if the publication already exists — stopping
        // tears down a live Mac camera / mic and forces a second getUserMedia.
        if (!alreadyLive) {
          try {
            track.stop()
          } catch {
            // ignore
          }
        }
      }
    })
    try {
      await Promise.all(tasks)
    } finally {
      publishInFlight = false
      bump({ syncDevices: false })
    }
  }

  async function publishViaDeviceApis(r: Room, opts: RtcConnectOptions) {
    // Same path as pre-join handoff: create then publish. Avoid parallel
    // setMicrophoneEnabled + setCameraEnabled (can deadlock device locks).
    rtcDiagMark('createLocalTracks')
    let tracks: LocalTrack[] = []
    try {
      tracks = await withTimeout(
        createLocalTracks({
          audio: opts.audio !== false
            ? (opts.audioDeviceId ? { deviceId: opts.audioDeviceId } : true)
            : false,
          video: opts.video === true
            ? {
                resolution: VideoPresets.h360.resolution,
                ...(opts.videoDeviceId ? { deviceId: opts.videoDeviceId } : {})
              }
            : false
        }),
        DEVICE_ENABLE_TIMEOUT_MS,
        '采集本地音视频'
      )
      rtcDiagEnd('createLocalTracks', 'createLocalTracksMs')
      rtcDiagEvent('createLocalTracks.ok', {
        kinds: tracks.map(t => t.kind)
      })
      await publishHandoffTracks(r, tracks)
      tracks = []
    } catch (e) {
      rtcDiagEnd('createLocalTracks', 'createLocalTracksMs')
      rtcDiagEvent('createLocalTracks.failed', {
        message: e instanceof Error ? e.message : String(e)
      })
      console.warn('createLocalTracks / publish fallback failed', e)
      stopTracksQuietly(tracks)
      // Best-effort cancel any in-flight SDK enable from a prior attempt.
      try {
        await Promise.race([
          Promise.all([
            r.localParticipant.setMicrophoneEnabled(false),
            r.localParticipant.setCameraEnabled(false)
          ]),
          new Promise((resolve) => setTimeout(resolve, 1500))
        ])
      } catch {
        // ignore
      }
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
    micEnabled.value = false
    camEnabled.value = false

    const handoffMetaBefore = meetingTrackHandoffMeta()
    const handoff = opts.localTracks ?? takeMeetingTrackHandoff()
    rememberPreviewTracks(handoff)
    rtcDiagSetMeta({
      livekitUrl: opts.url,
      token: opts.token,
      wantAudio: opts.audio !== false,
      wantVideo: opts.video === true,
      handoffTrackCount: handoff?.length ?? 0,
      handoffBeforeTake: handoffMetaBefore,
      browser: typeof navigator !== 'undefined' ? navigator.userAgent : null
    })
    rtcDiagEvent('connect.start', {
      wantAudio: opts.audio !== false,
      wantVideo: opts.video === true,
      handoffBeforeTake: handoffMetaBefore,
      handoff: handoff?.map(t => ({
        kind: t.kind,
        source: t.source,
        readyState: t.mediaStreamTrack?.readyState
      })) ?? []
    })
    // Show local tile immediately (Mac camera already on from pre-join).
    bump()

    try {
      await disconnect({ preservePhase: true, skipGenBump: true })
      if (gen !== connectGen) {
        stopTracksQuietly(handoff)
        return
      }

      const r = new Room({
        adaptiveStream: false,
        dynacast: false,
        disconnectOnPageLeave: false,
        // Keep MediaStreamTracks across reconnect unpublish/republish (fixes flash-then-gone).
        stopLocalTrackOnUnpublish: false,
        // Safari: single-PC + `_data_track` datachannel often stalls ICE ~15s then full reconnect.
        singlePeerConnection: false,
        publishDefaults: {
          simulcast: false,
          videoCodec: 'vp8',
          // Safari quieter path — audio RED / backup codec renegotiation worsens publish hangs.
          red: false,
          dtx: true,
          backupCodec: false,
          videoEncoding: {
            maxBitrate: 600_000,
            maxFramerate: 24
          }
        }
      })
      bindRoomEvents(r)

      rtcDiagMark('prepareConnection')
      try {
        await withTimeout(
          r.prepareConnection(opts.url, opts.token),
          PREPARE_TIMEOUT_MS,
          '连接预热'
        )
        rtcDiagEnd('prepareConnection', 'prepareMs')
      } catch (e) {
        rtcDiagEnd('prepareConnection', 'prepareMs')
        rtcDiagEvent('prepareConnection.failed', {
          message: e instanceof Error ? e.message : String(e)
        })
      }
      if (gen !== connectGen) {
        stopTracksQuietly(handoff)
        await r.disconnect(true)
        return
      }

      rtcDiagMark('roomConnect')
      await withTimeout(
        r.connect(opts.url, opts.token, {
          autoSubscribe: true,
          peerConnectionTimeout: 45_000
        }),
        SIGNAL_CONNECT_TIMEOUT_MS,
        '信令连接'
      )
      rtcDiagEnd('roomConnect', 'connectMs')
      if (gen !== connectGen) {
        stopTracksQuietly(handoff)
        await r.disconnect(true)
        return
      }

      room.value = r
      connected.value = true
      // Unlock UI immediately after signal — commercial apps show the room before SFU publish settles.
      phase.value = 'connected'
      micEnabled.value = opts.audio !== false
      camEnabled.value = opts.video === true
      connecting.value = false
      ensureAttempts = 0
      startAudioLevelPolling()
      bump({ syncDevices: false })
      rtcDiagEvent('ui.unlocked', {
        mic: micEnabled.value,
        cam: camEnabled.value,
        connectionState: r.state
      })

      // Publish in background so join / toolbar are not blocked by Safari ICE (~15s hangs).
      const publishGen = gen
      void (async () => {
        rtcDiagMark('publishing')
        try {
          if (handoff?.length) {
            await publishHandoffTracks(r, handoff)
          } else {
            await publishViaDeviceApis(r, opts)
          }
        } catch (e) {
          rtcDiagEvent('publish.background.failed', {
            message: e instanceof Error ? e.message : String(e)
          })
        }
        rtcDiagEnd('publishing', 'publishTotalMs')
        if (publishGen !== connectGen || room.value !== r) {
          return
        }
        bump({ syncDevices: false })
        rtcDiagEvent('connect.done', {
          mic: micEnabled.value,
          cam: camEnabled.value,
          connectionState: room.value?.state,
          localPubs: room.value ? snapshotLocalPubs(room.value) : []
        })
        if (
          r.state !== ConnectionState.Connected
          || (micEnabled.value && !r.localParticipant.isMicrophoneEnabled)
          || (camEnabled.value && !r.localParticipant.isCameraEnabled)
        ) {
          scheduleEnsurePublished(r)
        }
      })()
    } catch (e) {
      if (gen !== connectGen) {
        stopTracksQuietly(handoff)
        return
      }
      stopTracksQuietly(handoff)
      error.value = e instanceof Error ? e.message : '连接会议失败'
      connected.value = false
      phase.value = 'error'
      rtcDiagEvent('connect.error', { message: error.value })
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
    if (tokenRefreshInFlight) {
      rtcDiagEvent('tokenRefresh.skipped', { reason: 'inFlight' })
      return
    }
    const elapsed = Date.now() - lastTokenRefreshAt
    if (lastTokenRefreshAt > 0 && elapsed < TOKEN_REFRESH_MIN_INTERVAL_MS) {
      rtcDiagEvent('tokenRefresh.skipped', { reason: 'minInterval', elapsedMs: elapsed })
      return
    }
    const prev = lastConnectOpts
    if (!prev) {
      throw new Error('尚未入会，无法刷新凭证')
    }
    tokenRefreshInFlight = true
    rtcDiagEvent('tokenRefresh.start')
    try {
      const audio = room.value?.localParticipant.isMicrophoneEnabled ?? prev.audio !== false
      const video = room.value?.localParticipant.isCameraEnabled ?? prev.video === true
      await connect({
        ...prev,
        url: url || prev.url,
        token,
        audio,
        video
        // no handoff on refresh — re-acquire via device APIs
      })
      lastTokenRefreshAt = Date.now()
      rtcDiagEvent('tokenRefresh.done')
    } finally {
      tokenRefreshInFlight = false
    }
  }

  async function disconnect(opts?: { preservePhase?: boolean, skipGenBump?: boolean }) {
    if (!opts?.skipGenBump) {
      connectGen += 1
    }
    if (speakerBumpTimer != null) {
      clearTimeout(speakerBumpTimer)
      speakerBumpTimer = null
    }
    if (ensurePublishTimer != null) {
      clearTimeout(ensurePublishTimer)
      ensurePublishTimer = null
    }
    stopAudioLevelPolling()
    publishInFlight = false
    deviceOpInFlight = false
    ensureAttempts = 0
    const r = room.value
    room.value = null
    connected.value = false
    micEnabled.value = false
    camEnabled.value = false
    activeSpeakerIds.value = []
    if (!opts?.preservePhase) {
      stopTracksQuietly(
        [localPreviewVideo.value, localPreviewAudio.value].filter(Boolean) as LocalTrack[]
      )
      localPreviewVideo.value = null
      localPreviewAudio.value = null
    }
    if (!opts?.preservePhase && phase.value !== 'error') {
      phase.value = 'idle'
    }
    if (r) {
      try {
        await r.disconnect(true)
      } catch {
        // ignore
      }
    }
    bump({ syncDevices: false })
  }

  async function setMicEnabled(enabled: boolean) {
    const r = room.value
    if (!r || mediaControlsLocked.value) {
      rtcDiagEvent('toggleMic.blocked', {
        enabled,
        locked: mediaControlsLocked.value,
        hasRoom: !!r
      })
      return
    }
    const prev = micEnabled.value
    micEnabled.value = enabled
    if (lastConnectOpts) {
      lastConnectOpts.audio = enabled
    }
    deviceOpInFlight = true
    rtcDiagEvent('toggleMic', { enabled, prev, connectionState: r.state })
    bump({ syncDevices: false })
    try {
      const pub = r.localParticipant.getTrackPublication(Track.Source.Microphone)
      if (pub?.track) {
        if (enabled) {
          await withTimeout(pub.unmute(), 5_000, '打开麦克风')
        } else {
          await withTimeout(pub.mute(), 5_000, '关闭麦克风')
        }
      } else if (enabled) {
        const preview = localPreviewAudio.value
        if (preview?.mediaStreamTrack?.readyState === 'live') {
          await withTimeout(
            r.localParticipant.publishTrack(preview, {
              source: Track.Source.Microphone,
              red: false
            }),
            PUBLISH_SOFT_TIMEOUT_MS,
            '发布麦克风'
          )
        } else {
          await withTimeout(
            r.localParticipant.setMicrophoneEnabled(
              true,
              micCaptureOptions(lastConnectOpts?.audioDeviceId)
            ),
            DEVICE_ENABLE_TIMEOUT_MS,
            '开启麦克风'
          )
        }
      } else {
        await withTimeout(
          r.localParticipant.setMicrophoneEnabled(false),
          5_000,
          '关闭麦克风'
        )
      }
      rtcDiagEvent('toggleMic.ok', { enabled, localPubs: snapshotLocalPubs(r) })
    } catch (e) {
      micEnabled.value = prev
      if (lastConnectOpts) {
        lastConnectOpts.audio = prev
      }
      rtcDiagEvent('toggleMic.failed', {
        enabled,
        message: e instanceof Error ? e.message : String(e)
      })
      bump({ syncDevices: false })
      throw e
    } finally {
      deviceOpInFlight = false
      bump({ syncDevices: false })
    }
  }

  async function setCameraEnabled(enabled: boolean) {
    const r = room.value
    if (!r || mediaControlsLocked.value) {
      rtcDiagEvent('toggleCam.blocked', {
        enabled,
        locked: mediaControlsLocked.value,
        hasRoom: !!r
      })
      return
    }
    const prev = camEnabled.value
    camEnabled.value = enabled
    if (lastConnectOpts) {
      lastConnectOpts.video = enabled
    }
    deviceOpInFlight = true
    rtcDiagEvent('toggleCam', { enabled, prev, connectionState: r.state })
    bump({ syncDevices: false })
    try {
      const pub = r.localParticipant.getTrackPublication(Track.Source.Camera)
      if (pub?.track) {
        if (enabled) {
          await withTimeout(pub.unmute(), 5_000, '打开摄像头')
        } else {
          await withTimeout(pub.mute(), 5_000, '关闭摄像头')
        }
      } else if (enabled) {
        const preview = localPreviewVideo.value
        if (preview?.mediaStreamTrack?.readyState === 'live') {
          await withTimeout(
            r.localParticipant.publishTrack(preview, {
              source: Track.Source.Camera,
              simulcast: false,
              videoCodec: 'vp8'
            }),
            PUBLISH_SOFT_TIMEOUT_MS,
            '发布摄像头'
          )
        } else {
          await withTimeout(
            r.localParticipant.setCameraEnabled(
              true,
              camCaptureOptions(lastConnectOpts?.videoDeviceId)
            ),
            DEVICE_ENABLE_TIMEOUT_MS,
            '开启摄像头'
          )
        }
      } else {
        await withTimeout(
          r.localParticipant.setCameraEnabled(false),
          5_000,
          '关闭摄像头'
        )
      }
      rtcDiagEvent('toggleCam.ok', { enabled, localPubs: snapshotLocalPubs(r) })
    } catch (e) {
      camEnabled.value = prev
      if (lastConnectOpts) {
        lastConnectOpts.video = prev
      }
      rtcDiagEvent('toggleCam.failed', {
        enabled,
        message: e instanceof Error ? e.message : String(e)
      })
      bump({ syncDevices: false })
      throw e
    } finally {
      deviceOpInFlight = false
      bump({ syncDevices: false })
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

  function noteFirstVideoFrame() {
    rtcDiagNoteFirstFrame('tile')
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
    mediaControlsLocked,
    connect,
    refreshToken,
    disconnect,
    setMicEnabled,
    setCameraEnabled,
    sendChat,
    onChat,
    noteFirstVideoFrame
  }
}

function snapshotLocalPubs(room: Room) {
  const pubs: Array<Record<string, unknown>> = []
  room.localParticipant.trackPublications.forEach((pub) => {
    pubs.push({
      source: pub.source,
      kind: pub.kind,
      muted: pub.isMuted,
      subscribed: pub.isSubscribed,
      hasTrack: !!pub.track,
      readyState: pub.track?.mediaStreamTrack?.readyState
    })
  })
  return pubs
}

function collectTiles(
  room: Room | null,
  speakingIds: string[],
  previewVideo: LocalTrack | null,
  previewAudio: LocalTrack | null,
  uiMicEnabled: boolean,
  uiCamEnabled: boolean
): RtcTileParticipant[] {
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
    let cameraMuted = false

    p.trackPublications.forEach((pub: TrackPublication) => {
      if (pub.kind === Track.Kind.Video && pub.source === Track.Source.Camera && pub.track) {
        videoTrack = pub.track as RemoteTrack | LocalTrack
        cameraMuted = pub.isMuted
      } else if (pub.kind === Track.Kind.Video && pub.track && !videoTrack) {
        videoTrack = pub.track as RemoteTrack | LocalTrack
        cameraMuted = pub.isMuted
      }
      if (pub.kind === Track.Kind.Audio && pub.source === Track.Source.Microphone) {
        if (pub.track) {
          audioTrack = pub.track as RemoteTrack | LocalTrack
        }
        isMuted = pub.isMuted || !pub.track
      }
    })

    if (isLocal) {
      // Prefer live pre-join track so the tile stays lit while publish/reconnect flaps.
      if (uiCamEnabled && (!videoTrack || cameraMuted)
        && previewVideo?.mediaStreamTrack?.readyState === 'live') {
        videoTrack = previewVideo
        cameraMuted = false
      }
      if (!audioTrack && previewAudio?.mediaStreamTrack?.readyState === 'live') {
        audioTrack = previewAudio
      }
      // Toolbar desired state is truth for local mute / camera visibility.
      isMuted = !uiMicEnabled
      if (!uiCamEnabled) {
        hasVideo = false
        videoTrack = null
      } else {
        hasVideo = !!videoTrack && !cameraMuted
        if (previewVideo?.mediaStreamTrack?.readyState === 'live') {
          hasVideo = true
          videoTrack = videoTrack ?? previewVideo
        }
      }
    } else {
      hasVideo = !!videoTrack && !cameraMuted
    }

    const level = isMuted ? 0 : Math.min(1, Math.max(0, p.audioLevel || 0))
    tiles.push({
      identity: p.identity,
      name: p.name || p.identity,
      isLocal,
      isSpeaking: !isMuted && (speaking.has(p.identity) || level > 0.08),
      isMuted,
      hasVideo,
      audioLevel: level,
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

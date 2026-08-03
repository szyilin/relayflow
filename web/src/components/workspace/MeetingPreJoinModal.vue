<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import {
  createLocalTracks,
  Track,
  VideoPresets,
  type LocalTrack,
  type LocalVideoTrack
} from 'livekit-client'
import { setMeetingMediaPrefs } from '../../composables/rtc/meetingMediaPrefs'
import { setMeetingTrackHandoff, clearMeetingTrackHandoff, peekMeetingTrackHandoff } from '../../composables/rtc/meetingTrackHandoff'
import { rtcDiagEvent } from '../../composables/rtc/rtcDiag'

const props = withDefaults(defineProps<{
  open: boolean
  title?: string
  primaryLabel?: string
  /** When true, show editable meeting code at top (join flow). */
  codeEditable?: boolean
  meetingCode?: string
}>(), {
  title: '视频会议',
  primaryLabel: '开始会议',
  codeEditable: false,
  meetingCode: ''
})

const emit = defineEmits<{
  'update:open': [value: boolean]
  'update:meetingCode': [value: string]
  confirm: [opts: {
    audio: boolean
    video: boolean
    meetingCode: string
    audioDeviceId?: string
    videoDeviceId?: string
    audioOutputId?: string
    speakerVolume: number
  }]
}>()

const audioEnabled = ref(true)
const videoEnabled = ref(false)
const speakerVolume = ref(80)
const audioDeviceId = ref('')
const videoDeviceId = ref('')
const audioOutputId = ref('')
const audioInputs = ref<MediaDeviceInfo[]>([])
const videoInputs = ref<MediaDeviceInfo[]>([])
const audioOutputs = ref<MediaDeviceInfo[]>([])
const previewEl = ref<HTMLVideoElement | null>(null)
const confirming = ref(false)
let previewVideo: LocalVideoTrack | null = null
let previewGen = 0

const dialogOpen = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v)
})

const codeModel = computed({
  get: () => props.meetingCode ?? '',
  set: (v: string) => emit('update:meetingCode', v)
})

const audioSelectItems = computed(() =>
  audioInputs.value.map(d => ({
    label: d.label || `麦克风 ${d.deviceId.slice(0, 6)}`,
    value: d.deviceId
  }))
)
const videoSelectItems = computed(() =>
  videoInputs.value.map(d => ({
    label: d.label || `摄像头 ${d.deviceId.slice(0, 6)}`,
    value: d.deviceId
  }))
)
const outputSelectItems = computed(() =>
  audioOutputs.value.map(d => ({
    label: d.label || `扬声器 ${d.deviceId.slice(0, 6)}`,
    value: d.deviceId
  }))
)

async function refreshDevices() {
  if (!navigator.mediaDevices?.enumerateDevices) {
    return
  }
  try {
    // Warm permission so labels populate.
    if (props.open) {
      const warm = await navigator.mediaDevices.getUserMedia({ audio: true, video: false }).catch(() => null)
      warm?.getTracks().forEach(t => t.stop())
    }
    const list = await navigator.mediaDevices.enumerateDevices()
    audioInputs.value = list.filter(d => d.kind === 'audioinput')
    videoInputs.value = list.filter(d => d.kind === 'videoinput')
    audioOutputs.value = list.filter(d => d.kind === 'audiooutput')
    if (!audioDeviceId.value && audioInputs.value[0]) {
      audioDeviceId.value = audioInputs.value[0].deviceId
    }
    if (!videoDeviceId.value && videoInputs.value[0]) {
      videoDeviceId.value = videoInputs.value[0].deviceId
    }
    if (!audioOutputId.value && audioOutputs.value[0]) {
      audioOutputId.value = audioOutputs.value[0].deviceId
    }
  } catch {
    // ignore
  }
}

function detachPreviewVideo() {
  if (previewVideo && previewEl.value) {
    try {
      previewVideo.detach(previewEl.value)
    } catch {
      // ignore
    }
  }
  if (previewEl.value) {
    previewEl.value.srcObject = null
  }
}

function stopPreviewTrack() {
  detachPreviewVideo()
  if (previewVideo) {
    try {
      previewVideo.stop()
    } catch {
      // ignore
    }
  }
  previewVideo = null
}

async function startPreview() {
  const gen = ++previewGen
  stopPreviewTrack()
  if (!videoEnabled.value || !props.open) {
    return
  }
  try {
    const tracks = await createLocalTracks({
      audio: false,
      video: {
        resolution: VideoPresets.h360.resolution,
        ...(videoDeviceId.value ? { deviceId: { exact: videoDeviceId.value } } : {})
      }
    })
    if (gen !== previewGen || !props.open || !videoEnabled.value) {
      tracks.forEach(t => t.stop())
      return
    }
    const video = tracks.find(t => t.kind === Track.Kind.Video) as LocalVideoTrack | undefined
    if (!video) {
      tracks.forEach(t => t.stop())
      return
    }
    previewVideo = video
    if (previewEl.value) {
      video.attach(previewEl.value)
      await previewEl.value.play().catch(() => {})
    }
  } catch {
    if (gen === previewGen) {
      videoEnabled.value = false
    }
  }
}

function stopPreview() {
  previewGen += 1
  stopPreviewTrack()
}

watch([() => props.open, videoEnabled, videoDeviceId], () => {
  if (props.open && videoEnabled.value) {
    void startPreview()
  } else {
    stopPreview()
  }
})

watch(() => props.open, (open) => {
  if (open) {
    // Fresh pre-join: drop any abandoned handoff from a cancelled prior attempt.
    clearMeetingTrackHandoff({ stop: true })
    void refreshDevices()
  } else {
    stopPreview()
    // Do NOT stop handoff tracks here — room page will take() them after navigate.
    // Only abandon when user dismissed without a successful confirm (no pending handoff).
    if (!confirming.value && !peekMeetingTrackHandoff()?.length) {
      clearMeetingTrackHandoff({ stop: true })
    }
    audioEnabled.value = true
    videoEnabled.value = false
    // Keep confirming true until unmount if handoff is pending (avoid unmount stop race).
    if (!peekMeetingTrackHandoff()?.length) {
      confirming.value = false
    }
  }
})

onUnmounted(() => {
  stopPreview()
  // Never stop pending handoff on unmount — navigation unmounts this modal
  // right after confirm; stopping here was wiping tracks (handoffTrackCount: 0).
  if (!peekMeetingTrackHandoff()?.length) {
    clearMeetingTrackHandoff({ stop: false })
  }
})

async function onConfirm() {
  confirming.value = true
  const wantAudio = audioEnabled.value
  const wantVideo = videoEnabled.value
  const tracks: LocalTrack[] = []

  try {
    // Reuse preview video track when possible (avoid second getUserMedia).
    const previewLive = previewVideo?.mediaStreamTrack?.readyState === 'live'
    if (wantVideo && previewVideo && previewLive) {
      detachPreviewVideo()
      tracks.push(previewVideo)
      previewVideo = null
      previewGen += 1
    } else {
      stopPreview()
    }

    const needAudio = wantAudio
    const needVideo = wantVideo && !tracks.some(t => t.kind === Track.Kind.Video)
    if (needAudio || needVideo) {
      const created = await createLocalTracks({
        audio: needAudio
          ? (audioDeviceId.value ? { deviceId: { exact: audioDeviceId.value } } : true)
          : false,
        video: needVideo
          ? {
              resolution: VideoPresets.h360.resolution,
              ...(videoDeviceId.value ? { deviceId: { exact: videoDeviceId.value } } : {})
            }
          : false
      })
      tracks.push(...created)
    }

    // If user wanted video but we only got audio, keep going with audio.
    setMeetingTrackHandoff(tracks)
    rtcDiagEvent('prejoin.handoff', {
      count: tracks.length,
      kinds: tracks.map(t => t.kind),
      wantAudio,
      wantVideo
    })

    const vol = Math.min(1, Math.max(0, speakerVolume.value / 100))
    setMeetingMediaPrefs({
      audioDeviceId: audioDeviceId.value || undefined,
      videoDeviceId: videoDeviceId.value || undefined,
      audioOutputId: audioOutputId.value || undefined,
      speakerVolume: vol
    })
    emit('confirm', {
      audio: wantAudio,
      video: wantVideo,
      meetingCode: codeModel.value.trim(),
      audioDeviceId: audioDeviceId.value || undefined,
      videoDeviceId: videoDeviceId.value || undefined,
      audioOutputId: audioOutputId.value || undefined,
      speakerVolume: vol
    })
  } catch (e) {
    confirming.value = false
    for (const t of tracks) {
      try {
        t.stop()
      } catch {
        // ignore
      }
    }
    clearMeetingTrackHandoff({ stop: true })
    rtcDiagEvent('prejoin.handoff.failed', {
      message: e instanceof Error ? e.message : String(e)
    })
    // Fall back: room will set*Enabled
    const vol = Math.min(1, Math.max(0, speakerVolume.value / 100))
    setMeetingMediaPrefs({
      audioDeviceId: audioDeviceId.value || undefined,
      videoDeviceId: videoDeviceId.value || undefined,
      audioOutputId: audioOutputId.value || undefined,
      speakerVolume: vol
    })
    emit('confirm', {
      audio: wantAudio,
      video: wantVideo,
      meetingCode: codeModel.value.trim(),
      audioDeviceId: audioDeviceId.value || undefined,
      videoDeviceId: videoDeviceId.value || undefined,
      audioOutputId: audioOutputId.value || undefined,
      speakerVolume: vol
    })
  }
}
</script>

<template>
  <UModal
    v-model:open="dialogOpen"
    :title="codeEditable ? undefined : title"
    :ui="{ content: 'sm:max-w-2xl' }"
  >
    <template
      v-if="codeEditable"
      #header
    >
      <div class="w-full px-1">
        <UInput
          v-model="codeModel"
          placeholder="会议号"
          size="xl"
          class="w-full"
          :ui="{ base: 'text-center text-lg tracking-wider' }"
          autofocus
        />
      </div>
    </template>
    <template #body>
      <div class="flex flex-col gap-4">
        <div class="relative aspect-video overflow-hidden rounded-xl bg-neutral-200 dark:bg-neutral-800">
          <video
            v-show="videoEnabled"
            ref="previewEl"
            class="size-full object-cover scale-x-[-1]"
            playsinline
            muted
            autoplay
          />
          <div
            v-if="!videoEnabled"
            class="absolute inset-0 flex flex-col items-center justify-center gap-2 text-muted"
          >
            <UIcon name="i-lucide-user" class="size-16 opacity-40" />
            <span class="text-sm">摄像头已关闭</span>
          </div>
        </div>

        <div class="grid gap-2 sm:grid-cols-3">
          <USelect
            v-if="audioSelectItems.length"
            v-model="audioDeviceId"
            :items="audioSelectItems"
            placeholder="麦克风"
            size="sm"
          />
          <USelect
            v-if="videoSelectItems.length"
            v-model="videoDeviceId"
            :items="videoSelectItems"
            placeholder="摄像头"
            size="sm"
          />
          <USelect
            v-if="outputSelectItems.length"
            v-model="audioOutputId"
            :items="outputSelectItems"
            placeholder="扬声器"
            size="sm"
          />
        </div>

        <div class="flex flex-wrap items-center gap-3">
          <UButton
            :color="audioEnabled ? 'primary' : 'error'"
            :variant="audioEnabled ? 'soft' : 'solid'"
            :icon="audioEnabled ? 'i-lucide-mic' : 'i-lucide-mic-off'"
            :disabled="confirming"
            @click="audioEnabled = !audioEnabled"
          >
            麦克风
          </UButton>
          <UButton
            :color="videoEnabled ? 'primary' : 'error'"
            :variant="videoEnabled ? 'soft' : 'solid'"
            :icon="videoEnabled ? 'i-lucide-video' : 'i-lucide-video-off'"
            :disabled="confirming"
            @click="videoEnabled = !videoEnabled"
          >
            摄像头
          </UButton>
          <div class="flex min-w-40 flex-1 items-center gap-2 px-1">
            <UIcon
              :name="speakerVolume > 0 ? 'i-lucide-volume-2' : 'i-lucide-volume-x'"
              class="size-4 shrink-0 text-muted"
            />
            <input
              v-model.number="speakerVolume"
              type="range"
              min="0"
              max="100"
              class="w-full accent-[var(--ui-primary)]"
              aria-label="扬声器音量"
              @input="setMeetingMediaPrefs({ speakerVolume: speakerVolume / 100 })"
            >
          </div>
          <UButton
            color="primary"
            class="ml-auto"
            :loading="confirming"
            @click="onConfirm"
          >
            {{ primaryLabel || '开始会议' }}
          </UButton>
        </div>
      </div>
    </template>
  </UModal>
</template>

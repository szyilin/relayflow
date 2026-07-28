<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import type { LocalTrack, RemoteTrack } from 'livekit-client'
import type { RtcTileParticipant } from '../../composables/rtc'

const props = defineProps<{
  participant: RtcTileParticipant
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const audioRef = ref<HTMLAudioElement | null>(null)
let attachedVideo: LocalTrack | RemoteTrack | null = null
let attachedAudio: LocalTrack | RemoteTrack | null = null

function attachVideo() {
  const el = videoRef.value
  const track = props.participant.videoTrack ?? null
  if (!el) {
    return
  }
  if (track === attachedVideo && el.srcObject) {
    return
  }
  if (attachedVideo && attachedVideo !== track) {
    try {
      attachedVideo.detach(el)
    } catch {
      // ignore
    }
    attachedVideo = null
  }
  if (track && typeof track.attach === 'function') {
    track.attach(el)
    attachedVideo = track
    void el.play().catch(() => {})
  }
}

function attachAudio() {
  // Local audio must not be played back (echo).
  if (props.participant.isLocal) {
    return
  }
  const el = audioRef.value
  const track = props.participant.audioTrack ?? null
  if (!el) {
    return
  }
  if (track === attachedAudio && el.srcObject) {
    return
  }
  if (attachedAudio && attachedAudio !== track) {
    try {
      attachedAudio.detach(el)
    } catch {
      // ignore
    }
    attachedAudio = null
  }
  if (track && typeof track.attach === 'function') {
    track.attach(el)
    attachedAudio = track
    void el.play().catch(() => {})
  }
}

function detachAll() {
  const videoEl = videoRef.value
  const audioEl = audioRef.value
  if (attachedVideo && videoEl) {
    try {
      attachedVideo.detach(videoEl)
    } catch {
      // ignore
    }
  }
  if (attachedAudio && audioEl) {
    try {
      attachedAudio.detach(audioEl)
    } catch {
      // ignore
    }
  }
  attachedVideo = null
  attachedAudio = null
}

async function syncMedia() {
  await nextTick()
  attachVideo()
  attachAudio()
}

watch(
  () => [props.participant.videoTrack, props.participant.audioTrack, props.participant.hasVideo] as const,
  () => {
    void syncMedia()
  },
  { flush: 'post' }
)

onMounted(() => {
  void syncMedia()
})

onUnmounted(detachAll)
</script>

<template>
  <div
    class="relative aspect-video overflow-hidden rounded-xl bg-neutral-900 text-white shadow-sm ring-1 ring-white/10"
    :class="participant.isSpeaking ? 'ring-2 ring-emerald-400' : ''"
  >
    <!-- Keep <video> mounted; toggling v-show/v-if tears down attach and causes flicker. -->
    <video
      ref="videoRef"
      class="absolute inset-0 size-full object-cover"
      :class="[
        participant.isLocal ? 'scale-x-[-1]' : '',
        participant.hasVideo ? 'opacity-100' : 'opacity-0'
      ]"
      playsinline
      autoplay
      muted
    />
    <audio
      v-if="!participant.isLocal"
      ref="audioRef"
      autoplay
    />
    <div
      v-if="!participant.hasVideo"
      class="absolute inset-0 flex items-center justify-center bg-neutral-800"
    >
      <div class="flex size-16 items-center justify-center rounded-full bg-neutral-700 text-xl font-medium">
        {{ (participant.name || '?').slice(0, 1) }}
      </div>
    </div>
    <div class="absolute bottom-2 left-2 flex max-w-[90%] items-center gap-1.5 rounded-md bg-black/55 px-2 py-1 text-xs backdrop-blur-sm">
      <UIcon
        :name="participant.isMuted ? 'i-lucide-mic-off' : 'i-lucide-mic'"
        class="size-3.5 shrink-0"
        :class="participant.isMuted ? 'text-red-400' : 'text-emerald-300'"
      />
      <span class="truncate">{{ participant.name }}{{ participant.isLocal ? '（我）' : '' }}</span>
    </div>
  </div>
</template>

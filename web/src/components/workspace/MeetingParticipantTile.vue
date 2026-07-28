<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import type { RtcTileParticipant } from '../../composables/rtc'

const props = defineProps<{
  participant: RtcTileParticipant
}>()

const videoRef = ref<HTMLVideoElement | null>(null)

function attach() {
  const el = videoRef.value
  const track = props.participant.videoTrack
  if (!el) {
    return
  }
  if (track && 'attach' in track && typeof track.attach === 'function') {
    track.attach(el)
  } else {
    el.srcObject = null
  }
}

function detach() {
  const el = videoRef.value
  const track = props.participant.videoTrack
  if (track && 'detach' in track && typeof track.detach === 'function') {
    track.detach()
  }
  if (el) {
    el.srcObject = null
  }
}

watch(() => props.participant.videoTrack, () => {
  detach()
  attach()
}, { flush: 'post' })

onMounted(attach)
onUnmounted(detach)
</script>

<template>
  <div
    class="relative aspect-video overflow-hidden rounded-xl bg-neutral-900 text-white shadow-sm ring-1 ring-white/10"
    :class="participant.isSpeaking ? 'ring-2 ring-emerald-400' : ''"
  >
    <video
      v-show="participant.hasVideo"
      ref="videoRef"
      class="size-full object-cover"
      :class="participant.isLocal ? 'scale-x-[-1]' : ''"
      playsinline
      autoplay
      muted
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

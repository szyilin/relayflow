<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'

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
  confirm: [opts: { audio: boolean, video: boolean, meetingCode: string }]
}>()

const audioEnabled = ref(true)
const videoEnabled = ref(false)
const speakerVolume = ref(80)
const previewEl = ref<HTMLVideoElement | null>(null)
let previewStream: MediaStream | null = null
let previewGen = 0

const dialogOpen = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v)
})

const codeModel = computed({
  get: () => props.meetingCode ?? '',
  set: (v: string) => emit('update:meetingCode', v)
})

async function startPreview() {
  const gen = ++previewGen
  stopPreviewTracksOnly()
  if (!videoEnabled.value || !props.open) {
    return
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: true,
      audio: false
    })
    if (gen !== previewGen || !props.open || !videoEnabled.value) {
      stream.getTracks().forEach(t => t.stop())
      return
    }
    previewStream = stream
    if (previewEl.value) {
      previewEl.value.srcObject = previewStream
      await previewEl.value.play().catch(() => {})
    }
  } catch {
    if (gen === previewGen) {
      videoEnabled.value = false
    }
  }
}

function stopPreviewTracksOnly() {
  previewStream?.getTracks().forEach((t) => {
    try {
      t.stop()
    } catch {
      // ignore
    }
  })
  previewStream = null
  if (previewEl.value) {
    previewEl.value.srcObject = null
  }
}

function stopPreview() {
  previewGen += 1
  stopPreviewTracksOnly()
}

watch([() => props.open, videoEnabled], () => {
  if (props.open && videoEnabled.value) {
    void startPreview()
  } else {
    stopPreview()
  }
})

watch(() => props.open, (open) => {
  if (!open) {
    stopPreview()
    audioEnabled.value = true
    videoEnabled.value = false
  }
})

onUnmounted(stopPreview)

async function onConfirm() {
  // Release preview tracks before entering room so mic/cam aren't double-held.
  // Await a microtask so Chrome finishes stopping the device before room getUserMedia.
  stopPreview()
  await Promise.resolve()
  emit('confirm', {
    audio: audioEnabled.value,
    video: videoEnabled.value,
    meetingCode: codeModel.value.trim()
  })
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
            class="size-full object-cover"
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

        <div class="flex flex-wrap items-center gap-3">
          <UButton
            :color="audioEnabled ? 'primary' : 'error'"
            :variant="audioEnabled ? 'soft' : 'solid'"
            :icon="audioEnabled ? 'i-lucide-mic' : 'i-lucide-mic-off'"
            @click="audioEnabled = !audioEnabled"
          >
            麦克风
          </UButton>
          <UButton
            :color="videoEnabled ? 'primary' : 'error'"
            :variant="videoEnabled ? 'soft' : 'solid'"
            :icon="videoEnabled ? 'i-lucide-video' : 'i-lucide-video-off'"
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
            >
          </div>
          <UButton color="primary" class="ml-auto" @click="onConfirm">
            {{ primaryLabel || '开始会议' }}
          </UButton>
        </div>
      </div>
    </template>
  </UModal>
</template>

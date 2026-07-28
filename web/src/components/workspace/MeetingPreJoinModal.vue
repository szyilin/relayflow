<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'

const props = defineProps<{
  open: boolean
  title: string
  primaryLabel?: string
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  confirm: [opts: { audio: boolean, video: boolean }]
}>()

const audioEnabled = ref(true)
const videoEnabled = ref(false)
const speakerVolume = ref(80)
const previewEl = ref<HTMLVideoElement | null>(null)
let previewStream: MediaStream | null = null

const dialogOpen = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v)
})

async function startPreview() {
  stopPreview()
  if (!videoEnabled.value || !props.open) {
    return
  }
  try {
    previewStream = await navigator.mediaDevices.getUserMedia({
      video: true,
      audio: false
    })
    if (previewEl.value) {
      previewEl.value.srcObject = previewStream
      await previewEl.value.play().catch(() => {})
    }
  } catch {
    videoEnabled.value = false
  }
}

function stopPreview() {
  previewStream?.getTracks().forEach(t => t.stop())
  previewStream = null
  if (previewEl.value) {
    previewEl.value.srcObject = null
  }
}

watch([() => props.open, videoEnabled], () => {
  if (props.open && videoEnabled.value) {
    void startPreview()
  } else {
    stopPreview()
  }
})

onMounted(() => {
  if (props.open && videoEnabled.value) {
    void startPreview()
  }
})

onUnmounted(stopPreview)

function onConfirm() {
  emit('confirm', { audio: audioEnabled.value, video: videoEnabled.value })
}
</script>

<template>
  <UModal
    v-model:open="dialogOpen"
    :title="title"
    :ui="{ content: 'sm:max-w-2xl' }"
  >
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
          <UButton
            class="absolute top-3 right-3"
            size="sm"
            color="neutral"
            variant="soft"
            icon="i-lucide-sparkles"
            title="虚拟背景（后续）"
            disabled
          />
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

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { fetchAuthenticatedBlobUrl } from '../../api/app/file'

const props = defineProps<{
  open: boolean
  kind: 'image' | 'pdf' | null
  src?: string
  title?: string
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  download: []
}>()

const resolvedSrc = ref<string>()
let revokeUrl: string | undefined

const titleText = computed(() => props.title?.trim() || (props.kind === 'pdf' ? 'PDF 预览' : '图片预览'))

function cleanup() {
  if (revokeUrl) {
    URL.revokeObjectURL(revokeUrl)
    revokeUrl = undefined
  }
  resolvedSrc.value = undefined
}

watch(() => [props.open, props.src, props.kind] as const, async ([open, src]) => {
  cleanup()
  if (!open || !src) {
    return
  }
  if (src.startsWith('blob:') || src.startsWith('data:')) {
    resolvedSrc.value = src
    return
  }
  if (src.includes('/app-api/infra/file/download')) {
    try {
      const blobUrl = await fetchAuthenticatedBlobUrl(src)
      revokeUrl = blobUrl
      resolvedSrc.value = blobUrl
    } catch {
      resolvedSrc.value = undefined
    }
    return
  }
  resolvedSrc.value = src
})

onUnmounted(cleanup)

function close() {
  emit('update:open', false)
}

function onKey(event: KeyboardEvent) {
  if (props.open && event.key === 'Escape') {
    close()
  }
}

watch(() => props.open, (open) => {
  if (open) {
    window.addEventListener('keydown', onKey)
  } else {
    window.removeEventListener('keydown', onKey)
  }
})

onUnmounted(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-[90] flex flex-col bg-black/80"
      @click.self="close"
    >
      <div class="flex items-center justify-between gap-2 px-4 py-3 text-white">
        <p class="min-w-0 truncate text-sm font-medium">
          {{ titleText }}
        </p>
        <div class="flex shrink-0 items-center gap-1">
          <UButton
            color="neutral"
            variant="ghost"
            size="sm"
            icon="i-lucide-download"
            class="text-white hover:bg-white/10"
            @click.stop="emit('download')"
          >
            下载
          </UButton>
          <UButton
            color="neutral"
            variant="ghost"
            size="sm"
            icon="i-lucide-x"
            square
            class="text-white hover:bg-white/10"
            aria-label="关闭"
            @click.stop="close"
          />
        </div>
      </div>
      <div class="flex min-h-0 flex-1 items-center justify-center p-4" @click.self="close">
        <img
          v-if="kind === 'image' && resolvedSrc"
          :src="resolvedSrc"
          :alt="titleText"
          class="max-h-full max-w-full object-contain"
          @click.stop
        >
        <iframe
          v-else-if="kind === 'pdf' && resolvedSrc"
          :src="resolvedSrc"
          class="h-full w-full max-w-5xl rounded-md bg-white"
          title="PDF 预览"
          @click.stop
        />
        <p
          v-else
          class="text-sm text-white/80"
        >
          无法预览
        </p>
      </div>
    </div>
  </Teleport>
</template>

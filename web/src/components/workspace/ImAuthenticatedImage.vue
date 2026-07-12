<script setup lang="ts">
import { onUnmounted, ref, watch } from 'vue'
import { fetchAuthenticatedBlobUrl } from '../../api/app/file'

const props = defineProps<{
  src?: string
  alt?: string
}>()

const resolvedSrc = ref<string>()
let revokeUrl: string | undefined

function cleanup() {
  if (revokeUrl) {
    URL.revokeObjectURL(revokeUrl)
    revokeUrl = undefined
  }
}

watch(() => props.src, async (src) => {
  cleanup()
  resolvedSrc.value = undefined
  if (!src) {
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
}, { immediate: true })

onUnmounted(cleanup)
</script>

<template>
  <img
    v-if="resolvedSrc"
    :src="resolvedSrc"
    :alt="alt"
    class="max-h-48 max-w-full rounded-lg object-contain"
  >
</template>

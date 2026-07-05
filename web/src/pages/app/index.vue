<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  router.replace('/app/messages')
})
</script>

<route lang="yaml">
meta:
  layout: workspace
</route>

<template>
  <div class="flex min-h-svh items-center justify-center">
    <UIcon name="i-lucide-loader-2" class="size-6 animate-spin text-muted" />
  </div>
</template>

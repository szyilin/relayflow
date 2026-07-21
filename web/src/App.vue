<script setup lang="ts">
import { computed, watchEffect } from 'vue'
import { useAdminColorMode } from './composables/useAdminColorMode'

const colorMode = useAdminColorMode()
const themeColor = computed(() => colorMode.value === 'dark' ? '#141417' : '#ffffff')

/** Top-center soft notice: auto-dismiss, no close / progress. */
const toaster = {
  position: 'top-center' as const,
  duration: 2200,
  progress: false
}

watchEffect(() => {
  let meta = document.querySelector('meta[name="theme-color"]')
  if (!meta) {
    meta = document.createElement('meta')
    meta.setAttribute('name', 'theme-color')
    document.head.appendChild(meta)
  }
  meta.setAttribute('content', themeColor.value)
})
</script>

<template>
  <UApp :toaster="toaster">
    <RouterView />
  </UApp>
</template>

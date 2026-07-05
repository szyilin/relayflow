<script setup lang="ts">
import { computed } from 'vue'
import { useHead } from '@unhead/vue'
import { useAdminColorMode } from './composables/useAdminColorMode'

const colorMode = useAdminColorMode()
const themeColor = computed(() => colorMode.value === 'dark' ? '#141417' : '#ffffff')

useHead({
  meta: [
    { name: 'theme-color', content: themeColor }
  ]
})
</script>

<template>
  <Suspense>
    <UApp>
      <RouterView />
    </UApp>
  </Suspense>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAdminColorMode } from '../../composables/useAdminColorMode'

const colorMode = useAdminColorMode()

const icon = computed(() => colorMode.value === 'dark' ? 'i-lucide-moon' : 'i-lucide-sun')
const label = computed(() => colorMode.value === 'dark' ? '切换浅色' : '切换深色')

function toggle() {
  colorMode.preference = colorMode.value === 'dark' ? 'light' : 'dark'
}
</script>

<template>
  <UButton
    :icon="icon"
    color="neutral"
    variant="ghost"
    square
    :aria-label="label"
    @click="toggle"
  />
</template>

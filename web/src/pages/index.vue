<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  router.replace('/admin/login')
})
</script>

<template>
  <div class="flex min-h-svh items-center justify-center">
    <UIcon name="i-lucide-loader-2" class="size-6 animate-spin text-muted" />
  </div>
</template>

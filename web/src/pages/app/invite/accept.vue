<script setup lang="ts">
import { onBeforeMount } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onBeforeMount(() => {
  const query = typeof route.query.mobile === 'string'
    ? { mobile: route.query.mobile }
    : undefined

  router.replace({
    path: '/app/register',
    query
  })
})
</script>

<route lang="yaml">
meta:
  layout: workspace-auth
</route>

<template>
  <div class="flex min-h-[40vh] items-center justify-center text-sm text-muted">
    正在跳转到注册页…
  </div>
</template>

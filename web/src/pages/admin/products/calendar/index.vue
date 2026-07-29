<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  void router.replace('/admin/products/calendar/org')
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <div class="p-6 text-sm text-muted">
    正在进入日历组织设置…
  </div>
</template>

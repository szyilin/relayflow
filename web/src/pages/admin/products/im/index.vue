<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
onMounted(() => {
  void router.replace('/admin/products/im/group-size')
})
</script>

<route lang="yaml">
meta:
  layout: admin
</route>

<template>
  <div class="flex flex-1 items-center justify-center p-6 text-sm text-muted">
    正在进入即时消息设置…
  </div>
</template>

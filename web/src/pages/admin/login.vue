<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  router.replace({
    path: '/app/login',
    query: route.query
  })
})
</script>

<route lang="yaml">
meta:
  layout: auth
</route>

<template>
  <div />
</template>

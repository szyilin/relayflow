<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const isRegister = computed(() => route.path.startsWith('/app/register'))
const isAddAccount = computed(() => route.query.addAccount === '1')

const brandHeadline = computed(() => {
  if (isAddAccount.value) {
    return '登录更多账号'
  }
  if (isRegister.value) {
    return '创建你的工作台'
  }
  return '进入工作台'
})

const brandSubline = computed(() => {
  if (isAddAccount.value) {
    return '添加另一账号，随时在企业间切换'
  }
  if (isRegister.value) {
    return '注册账号并创建企业，开启团队协作'
  }
  return '消息、任务、文档，一处搞定'
})
</script>

<template>
  <div class="workspace-auth">
    <aside class="workspace-auth-brand">
      <div class="workspace-auth-brand-glow" aria-hidden="true" />
      <div class="workspace-auth-brand-grid" aria-hidden="true" />

      <div class="workspace-auth-brand-inner">
        <div class="workspace-auth-brand-mark">
          <span class="workspace-auth-brand-icon">
            <UIcon name="i-lucide-workflow" class="size-5" />
          </span>
          <span class="workspace-auth-brand-name">RelayFlow</span>
        </div>

        <div class="workspace-auth-brand-hero">
          <h1 class="workspace-auth-brand-title">
            {{ brandHeadline }}
          </h1>
          <p class="workspace-auth-brand-sub">
            {{ brandSubline }}
          </p>
        </div>

        <p class="workspace-auth-brand-foot">
          自托管企业协作
        </p>
      </div>
    </aside>

    <main class="workspace-auth-panel">
      <RouterView />
    </main>
  </div>
</template>

<template>
  <div class="relative min-h-svh grid lg:grid-cols-2">
    <div class="relative hidden lg:flex flex-col justify-between overflow-hidden bg-gradient-to-br from-primary-600 via-primary-600 to-primary-800 p-10 text-white">
      <div class="flex items-center gap-3">
        <UIcon name="i-lucide-workflow" class="size-8 shrink-0" />
        <span class="text-xl font-semibold tracking-tight">RelayFlow</span>
      </div>

      <div class="space-y-4">
        <h1 class="text-3xl font-semibold leading-tight">
          企业协作平台
        </h1>
        <p class="max-w-md text-primary-100/90 text-lg">
          管理控制台 —— 配置组织、用户与基础设施，让团队协作更高效。
        </p>
      </div>

      <p class="text-sm text-primary-100/70">
        RelayFlow · 自托管企业协作
      </p>
    </div>

    <div class="auth-panel flex items-center justify-center p-6 sm:p-10">
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.auth-panel {
  background-color: var(--admin-main-bg, #ffffff);
}

.dark .auth-panel {
  background-color: var(--admin-main-bg, #141417);
}
</style>

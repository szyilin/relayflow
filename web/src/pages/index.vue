<script setup lang="ts">
</script>

<template>
  <div class="relative flex min-h-svh flex-col items-center justify-center overflow-hidden bg-[#0b0c0e] p-6 text-white">
    <div
      class="pointer-events-none absolute inset-0 opacity-40"
      style="background: radial-gradient(circle at 30% 20%, rgb(59 130 246 / 35%), transparent 45%), radial-gradient(circle at 70% 80%, rgb(139 92 246 / 25%), transparent 40%)"
    />

    <div class="relative z-10 w-full max-w-3xl space-y-10">
      <div class="space-y-3 text-center">
        <div class="mx-auto flex size-16 items-center justify-center rounded-2xl bg-primary shadow-lg shadow-primary/30">
          <UIcon name="i-lucide-workflow" class="size-8 text-white" />
        </div>
        <h1 class="text-4xl font-semibold tracking-tight">
          RelayFlow
        </h1>
        <p class="text-lg text-white/60">
          企业协作平台 —— 选择你的入口
        </p>
      </div>

      <div class="grid gap-5 sm:grid-cols-2">
        <RouterLink
          to="/app/login"
          class="group rounded-2xl border border-white/10 bg-white/5 p-6 backdrop-blur transition hover:border-primary/50 hover:bg-white/8"
        >
          <div class="mb-4 flex size-12 items-center justify-center rounded-xl bg-primary/20 text-primary">
            <UIcon name="i-lucide-message-circle" class="size-6" />
          </div>
          <h2 class="text-xl font-semibold">
            员工工作台
          </h2>
          <p class="mt-2 text-sm leading-relaxed text-white/55">
            消息、任务、文档、通讯录 —— 像飞书 / Discord 一样的客户端体验，面向全员日常使用。
          </p>
          <span class="mt-4 inline-flex items-center gap-1 text-sm font-medium text-primary group-hover:gap-2 transition-all">
            进入工作台
            <UIcon name="i-lucide-arrow-right" class="size-4" />
          </span>
        </RouterLink>

        <RouterLink
          to="/admin/login"
          class="group rounded-2xl border border-white/10 bg-white/5 p-6 backdrop-blur transition hover:border-white/25 hover:bg-white/8"
        >
          <div class="mb-4 flex size-12 items-center justify-center rounded-xl bg-white/10 text-white/80">
            <UIcon name="i-lucide-shield" class="size-6" />
          </div>
          <h2 class="text-xl font-semibold">
            管理后台
          </h2>
          <p class="mt-2 text-sm leading-relaxed text-white/55">
            用户、部门、权限、基础设施 —— 传统管理控制台，仅管理员可访问。
          </p>
          <span class="mt-4 inline-flex items-center gap-1 text-sm font-medium text-white/70 group-hover:gap-2 transition-all">
            进入管理端
            <UIcon name="i-lucide-arrow-right" class="size-4" />
          </span>
        </RouterLink>
      </div>

      <p class="text-center text-xs text-white/35">
        两套 UI 独立设计：工作台偏客户端 · 管理端偏企业后台
      </p>
    </div>
  </div>
</template>

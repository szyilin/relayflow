<script setup lang="ts">
import { useRouter } from 'vue-router'
import WorkspaceShell from '../../components/workspace/WorkspaceShell.vue'

const router = useRouter()

function backToWorkspace() {
  router.push('/app/messages')
}
</script>

<template>
  <WorkspaceShell>
    <div class="flex flex-1 flex-col items-center justify-center gap-4 p-8 text-center">
      <div class="flex size-14 items-center justify-center rounded-2xl bg-warning/10 text-warning">
        <UIcon name="i-lucide-shield-off" class="size-7" />
      </div>
      <div class="max-w-md space-y-2">
        <h1 class="text-lg font-semibold text-[var(--ws-text-primary)]">
          没有权限
        </h1>
        <p class="text-sm text-[var(--ws-text-muted)]">
          当前账号无权访问该资源。可返回工作台，或联系管理员开通权限。
        </p>
      </div>
      <UButton color="primary" @click="backToWorkspace">
        返回工作台
      </UButton>
    </div>
  </WorkspaceShell>
</template>

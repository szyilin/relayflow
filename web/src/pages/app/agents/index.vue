<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import WorkspaceShell from '../../../components/workspace/WorkspaceShell.vue'
import { useAgentStore } from '../../../stores/agent'

const agent = useAgentStore()
const toast = useToast()
const messageListRef = ref<HTMLElement | null>(null)

const editorOpen = ref(false)
const editorMode = ref<'create' | 'edit'>('create')
const editorForm = reactive({
  id: '' as string | undefined,
  name: '',
  description: '',
  systemPrompt: '',
  modelRef: ''
})

const autoOpen = ref(false)
const autoForm = reactive({
  id: '' as string | undefined,
  title: '',
  prompt: '',
  agentId: '' as string,
  cronExpr: '0 0 9 * * ?',
  status: 'active' as 'active' | 'disabled'
})

const showChat = computed(() => !!agent.activeSessionId)
const showAutomations = computed(() => agent.panelMode === 'automations' && !agent.activeSessionId)

async function scrollToBottom() {
  await nextTick()
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

watch(
  () => agent.activeMessages.length,
  () => {
    void scrollToBottom()
  }
)

onMounted(async () => {
  try {
    await agent.bootstrap()
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '加载失败',
      color: 'error'
    })
  }
})

function openCreateAgent() {
  if (!agent.models.length) {
    toast.add({
      title: '请先在管理端配置并启用模型连接',
      description: '路径：管理后台 → 基础设施 → 模型连接',
      color: 'warning'
    })
  }
  editorMode.value = 'create'
  editorForm.id = undefined
  editorForm.name = ''
  editorForm.description = ''
  editorForm.systemPrompt = '你是友好的企业办公助手，回答简洁、可执行。'
  editorForm.modelRef = agent.models[0]?.code ?? ''
  editorOpen.value = true
}

function openEditAgent(id: string) {
  const item = agent.agents.find(a => a.id === id)
  if (!item) {
    return
  }
  editorMode.value = 'edit'
  editorForm.id = item.id
  editorForm.name = item.name
  editorForm.description = item.description ?? ''
  editorForm.systemPrompt = item.systemPrompt
  editorForm.modelRef = item.modelRef
  editorOpen.value = true
}

async function submitEditor() {
  if (!editorForm.name.trim()) {
    toast.add({ title: '请填写名称', color: 'warning' })
    return
  }
  if (!editorForm.modelRef) {
    toast.add({ title: '请选择模型', color: 'warning' })
    return
  }
  try {
    await agent.saveAgent({
      id: editorForm.id,
      name: editorForm.name.trim(),
      description: editorForm.description.trim() || undefined,
      systemPrompt: editorForm.systemPrompt,
      modelRef: editorForm.modelRef,
      status: 'active'
    })
    editorOpen.value = false
    toast.add({
      title: editorMode.value === 'create' ? '智能体已创建' : '智能体已更新',
      color: 'success'
    })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '保存失败',
      color: 'error'
    })
  }
}

async function handleDeleteAgent(id: string) {
  try {
    await agent.removeAgent(id)
    toast.add({ title: '已删除', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '删除失败',
      color: 'error'
    })
  }
}

async function handleNewTask() {
  try {
    if (!agent.selectedAgentId) {
      toast.add({ title: '请先选择智能体', color: 'warning' })
      agent.openAgents()
      return
    }
    await agent.startNewTask()
    toast.add({ title: '已开始新任务', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '创建失败',
      color: 'error'
    })
  }
}

async function handleSend() {
  const text = agent.draft
  if (!text.trim() || agent.sending) {
    return
  }
  try {
    await agent.sendMessage(text)
    await scrollToBottom()
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '发送失败',
      color: 'error'
    })
  }
}

async function handleHomeSend() {
  if (!agent.selectedAgentId) {
    toast.add({ title: '请先选择智能体', color: 'warning' })
    return
  }
  await handleSend()
}

async function handleOpenSession(sessionId: string) {
  try {
    await agent.openSession(sessionId)
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '打开会话失败',
      color: 'error'
    })
  }
}

function openCreateAutomation() {
  if (!agent.agents.length) {
    toast.add({ title: '请先创建智能体', color: 'warning' })
    agent.openAgents()
    return
  }
  autoForm.id = undefined
  autoForm.title = ''
  autoForm.prompt = ''
  autoForm.agentId = agent.selectedAgentId ?? agent.agents[0]?.id ?? ''
  autoForm.cronExpr = '0 0 9 * * ?'
  autoForm.status = 'active'
  autoOpen.value = true
}

async function submitAutomation() {
  if (!autoForm.title.trim() || !autoForm.prompt.trim() || !autoForm.agentId || !autoForm.cronExpr.trim()) {
    toast.add({ title: '请填写完整自动化信息', color: 'warning' })
    return
  }
  try {
    await agent.saveAutomation({
      id: autoForm.id,
      title: autoForm.title.trim(),
      prompt: autoForm.prompt.trim(),
      agentId: autoForm.agentId,
      cronExpr: autoForm.cronExpr.trim(),
      status: autoForm.status
    })
    autoOpen.value = false
    toast.add({ title: '自动化已保存', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '保存失败',
      color: 'error'
    })
  }
}

async function handleRunAutomation(id: string) {
  try {
    await agent.runAutomationNow(id)
    toast.add({ title: '已执行，结果将推送到消息', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '执行失败',
      color: 'error'
    })
  }
}

async function handleDeleteAutomation(id: string) {
  try {
    await agent.removeAutomation(id)
    toast.add({ title: '已删除', color: 'success' })
  } catch (error) {
    toast.add({
      title: error instanceof Error ? error.message : '删除失败',
      color: 'error'
    })
  }
}

const modelItems = computed(() =>
  agent.models.map(m => ({
    label: `${m.name}（${m.modelName}）`,
    value: m.code
  }))
)

const agentSelectItems = computed(() =>
  agent.agents
    .filter(a => a.status === 'active')
    .map(a => ({
      label: a.name,
      value: a.id
    }))
)
</script>

<route lang="yaml">
meta:
  layout: workspace
</route>

<template>
  <WorkspaceShell>
    <template #panel>
      <div class="flex items-center justify-between border-b border-[var(--ws-border-subtle)] px-4 py-3">
        <h2 class="font-semibold">
          智能体
        </h2>
        <UButton
          v-if="agent.panelMode === 'automations'"
          icon="i-lucide-plus"
          color="neutral"
          variant="ghost"
          size="sm"
          @click="openCreateAutomation"
        >
          新建
        </UButton>
        <UButton
          v-else
          icon="i-lucide-plus"
          color="neutral"
          variant="ghost"
          size="sm"
          @click="openCreateAgent"
        >
          新建
        </UButton>
      </div>

      <div class="space-y-1 border-b border-[var(--ws-border-subtle)] p-2">
        <button
          type="button"
          class="workspace-list-item flex w-full items-center gap-2 px-3 py-2 text-left"
          :data-active="!agent.activeSessionId && agent.panelMode === 'home'"
          @click="agent.openHome()"
        >
          <UIcon name="i-lucide-sparkles" class="size-4 shrink-0 text-[var(--ui-primary)]" />
          <span class="text-sm font-medium">新任务</span>
        </button>
        <button
          type="button"
          class="workspace-list-item flex w-full items-center gap-2 px-3 py-2 text-left"
          :data-active="agent.panelMode === 'agents'"
          @click="agent.openAgents()"
        >
          <UIcon name="i-lucide-bot" class="size-4 shrink-0" />
          <span class="text-sm font-medium">智能体</span>
        </button>
        <button
          type="button"
          class="workspace-list-item flex w-full items-center gap-2 px-3 py-2 text-left"
          :data-active="agent.panelMode === 'history'"
          @click="agent.openHistory()"
        >
          <UIcon name="i-lucide-history" class="size-4 shrink-0" />
          <span class="text-sm font-medium">历史任务</span>
        </button>
        <button
          type="button"
          class="workspace-list-item flex w-full items-center gap-2 px-3 py-2 text-left"
          :data-active="agent.panelMode === 'automations'"
          @click="agent.openAutomations()"
        >
          <UIcon name="i-lucide-zap" class="size-4 shrink-0" />
          <span class="text-sm font-medium">自动化</span>
        </button>
      </div>

      <div v-if="agent.loading" class="space-y-2 px-3 py-3">
        <USkeleton v-for="i in 4" :key="i" class="h-12 w-full" />
      </div>

      <div v-else-if="agent.panelMode === 'agents'" class="flex-1 overflow-y-auto px-2 py-2">
        <p class="px-2 pb-2 text-xs text-[var(--ws-text-muted)]">
          管理你的专属智能体
        </p>
        <p
          v-if="!agent.agents.length"
          class="px-2 py-6 text-center text-sm text-[var(--ws-text-muted)]"
        >
          还没有智能体，点击上方「新建」
        </p>
        <div v-else class="space-y-0.5">
          <div
            v-for="item in agent.agents"
            :key="item.id"
            class="group workspace-list-item flex w-full items-start gap-2 px-3 py-2.5"
            :data-active="agent.selectedAgentId === item.id"
          >
            <button
              type="button"
              class="min-w-0 flex-1 text-left"
              @click="agent.selectAgent(item.id)"
            >
              <div class="truncate text-sm font-medium">
                {{ item.name }}
              </div>
              <p class="mt-0.5 line-clamp-2 text-xs text-[var(--ws-text-muted)]">
                {{ item.description || '暂无描述' }}
              </p>
            </button>
            <div class="flex shrink-0 opacity-0 transition-opacity group-hover:opacity-100">
              <UButton
                icon="i-lucide-pencil"
                color="neutral"
                variant="ghost"
                size="xs"
                @click="openEditAgent(item.id)"
              />
              <UButton
                icon="i-lucide-trash-2"
                color="error"
                variant="ghost"
                size="xs"
                @click="handleDeleteAgent(item.id)"
              />
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="agent.panelMode === 'automations'" class="flex-1 overflow-y-auto px-2 py-2">
        <p class="px-2 pb-2 text-xs text-[var(--ws-text-muted)]">
          定时跑智能体，结果推送到「消息」（不进 Session）
        </p>
        <p
          v-if="!agent.automations.length"
          class="px-2 py-6 text-center text-sm text-[var(--ws-text-muted)]"
        >
          还没有自动化，点击上方「新建」
        </p>
        <div v-else class="space-y-0.5">
          <div
            v-for="item in agent.automations"
            :key="item.id"
            class="group workspace-list-item flex w-full flex-col gap-1 px-3 py-2.5"
          >
            <div class="flex items-start gap-2">
              <div class="min-w-0 flex-1">
                <div class="truncate text-sm font-medium">
                  {{ item.title }}
                </div>
                <p class="mt-0.5 truncate text-xs text-[var(--ws-text-muted)]">
                  {{ item.agentName || '智能体' }} · {{ item.cronExpr }}
                </p>
                <p v-if="item.lastStatus" class="mt-0.5 text-xs text-[var(--ws-text-muted)]">
                  上次：{{ item.lastStatus }}{{ item.lastError ? ` · ${item.lastError}` : '' }}
                </p>
              </div>
              <div class="flex shrink-0 opacity-0 transition-opacity group-hover:opacity-100">
                <UButton
                  icon="i-lucide-play"
                  color="neutral"
                  variant="ghost"
                  size="xs"
                  :loading="agent.automationBusy"
                  @click="handleRunAutomation(item.id)"
                />
                <UButton
                  icon="i-lucide-trash-2"
                  color="error"
                  variant="ghost"
                  size="xs"
                  @click="handleDeleteAutomation(item.id)"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="flex-1 overflow-y-auto px-2 py-2">
        <p
          v-if="!agent.sessions.length"
          class="px-2 py-6 text-center text-sm text-[var(--ws-text-muted)]"
        >
          还没有历史任务，从「新任务」开始吧
        </p>
        <div v-else class="space-y-0.5">
          <button
            v-for="session in agent.sessions"
            :key="session.id"
            type="button"
            class="workspace-list-item flex w-full flex-col gap-0.5 px-3 py-2.5 text-left"
            :data-active="agent.activeSessionId === session.id"
            @click="handleOpenSession(session.id)"
          >
            <span class="truncate text-sm font-medium">
              {{ session.title || '未命名任务' }}
            </span>
            <span class="truncate text-xs text-[var(--ws-text-muted)]">
              {{ session.agentName || '智能体' }}
            </span>
          </button>
        </div>
      </div>
    </template>

    <!-- 自动化主区 -->
    <div
      v-if="showAutomations"
      class="flex min-h-0 flex-1 flex-col items-center justify-center px-6"
    >
      <UIcon name="i-lucide-zap" class="mb-4 size-10 text-[var(--ui-primary)]" />
      <h1 class="mb-2 text-xl font-semibold">
        自动化
      </h1>
      <p class="mb-6 max-w-md text-center text-sm text-[var(--ws-text-muted)]">
        按 Cron 定时运行智能体，结果经 Carrier Bot 推送到「消息」。不会写入智能体 Session。
      </p>
      <UButton icon="i-lucide-plus" @click="openCreateAutomation">
        新建自动化
      </UButton>
    </div>

    <!-- 新任务落地页 -->
    <div
      v-else-if="!showChat"
      class="flex min-h-0 flex-1 flex-col"
    >
      <div class="flex flex-1 flex-col items-center justify-center px-6 pb-8 pt-12">
        <div class="mb-6 flex size-14 items-center justify-center rounded-2xl bg-[color-mix(in_oklab,var(--ui-primary)_12%,transparent)]">
          <UIcon name="i-lucide-sparkles" class="size-7 text-[var(--ui-primary)]" />
        </div>
        <h1 class="mb-2 text-2xl font-semibold tracking-tight">
          让智能体帮你做点什么
        </h1>
        <p class="mb-8 max-w-md text-center text-sm text-[var(--ws-text-muted)]">
          会话仅在本页，不会出现在「消息」里。回复由 Spring AI Runtime 实时生成。
        </p>
        <UAlert
          v-if="agent.lastError"
          class="mb-4 w-full max-w-xl"
          color="error"
          variant="subtle"
          :title="agent.lastError"
        />
        <UAlert
          v-else-if="!agent.loading && !agent.models.length"
          class="mb-4 w-full max-w-xl"
          color="warning"
          variant="subtle"
          title="尚未配置可用模型"
          description="请先到管理后台「基础设施 → 模型连接」启用至少一个连接，再创建智能体。"
        />

        <div class="mb-4 w-full max-w-xl">
          <label class="mb-1.5 block text-xs text-[var(--ws-text-muted)]">选择智能体</label>
          <USelect
            :model-value="agent.selectedAgentId ?? undefined"
            :items="agentSelectItems"
            placeholder="选择智能体"
            class="w-full"
            @update:model-value="(v: string) => agent.selectAgent(v)"
          />
        </div>

        <div class="w-full max-w-xl rounded-2xl border border-[var(--ws-border)] bg-[var(--ws-input-bar-bg)] p-3">
          <textarea
            v-model="agent.draft"
            rows="3"
            class="w-full resize-none bg-transparent text-sm outline-none placeholder:text-[var(--ws-text-muted)]"
            placeholder="描述你想完成的事…"
            @keydown.meta.enter.prevent="handleHomeSend"
            @keydown.ctrl.enter.prevent="handleHomeSend"
          />
          <div class="mt-2 flex items-center justify-between">
            <UButton
              color="neutral"
              variant="ghost"
              size="sm"
              icon="i-lucide-bot"
              @click="agent.openAgents()"
            >
              管理智能体
            </UButton>
            <UButton
              icon="i-lucide-arrow-up"
              :loading="agent.sending"
              :disabled="!agent.draft.trim()"
              @click="handleHomeSend"
            >
              发送
            </UButton>
          </div>
        </div>
      </div>
    </div>

    <!-- 会话聊天 -->
    <div
      v-else
      class="flex min-h-0 flex-1 flex-col"
    >
      <header class="flex items-center gap-3 border-b border-[var(--ws-border-subtle)] px-5 py-3">
        <div class="min-w-0 flex-1">
          <h2 class="truncate font-semibold">
            {{ agent.activeSession?.title || '新任务' }}
          </h2>
          <p class="truncate text-xs text-[var(--ws-text-muted)]">
            {{ agent.activeSession?.agentName || agent.activeAgent?.name }}
          </p>
        </div>
        <UButton
          color="neutral"
          variant="ghost"
          size="sm"
          icon="i-lucide-plus"
          @click="handleNewTask"
        >
          新任务
        </UButton>
      </header>

      <div
        ref="messageListRef"
        class="flex-1 space-y-4 overflow-y-auto px-5 py-4"
      >
        <div v-if="agent.loadingMessages" class="space-y-3 py-4">
          <USkeleton v-for="i in 3" :key="i" class="h-16 w-2/3" />
        </div>
        <template v-else>
          <div
            v-for="msg in agent.activeMessages"
            :key="msg.id"
            class="flex"
            :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[min(36rem,85%)] rounded-2xl px-4 py-2.5 text-sm leading-relaxed whitespace-pre-wrap"
              :class="msg.role === 'user'
                ? 'bg-[var(--ui-primary)] text-white'
                : 'bg-[var(--ws-input-bar-bg)] text-[var(--ws-text)]'"
            >
              {{ msg.content }}
            </div>
          </div>
          <p
            v-if="!agent.activeMessages.length"
            class="py-12 text-center text-sm text-[var(--ws-text-muted)]"
          >
            发送第一条消息开始对话
          </p>
        </template>
      </div>

      <div class="border-t border-[var(--ws-border-subtle)] p-4">
        <div class="rounded-2xl border border-[var(--ws-border)] bg-[var(--ws-input-bar-bg)] p-3">
          <textarea
            v-model="agent.draft"
            rows="2"
            class="w-full resize-none bg-transparent text-sm outline-none placeholder:text-[var(--ws-text-muted)]"
            placeholder="继续输入…"
            @keydown.meta.enter.prevent="handleSend"
            @keydown.ctrl.enter.prevent="handleSend"
          />
          <div class="mt-2 flex justify-end">
            <UButton
              icon="i-lucide-arrow-up"
              :loading="agent.sending"
              :disabled="!agent.draft.trim()"
              @click="handleSend"
            >
              发送
            </UButton>
          </div>
        </div>
      </div>
    </div>

    <UModal v-model:open="editorOpen" title="智能体" description="定义名称、人设与模型引用">
      <template #body>
        <div class="space-y-4">
          <UFormField label="名称" required>
            <UInput v-model="editorForm.name" placeholder="例如：数据分析师" class="w-full" />
          </UFormField>
          <UFormField label="描述">
            <UInput v-model="editorForm.description" placeholder="一句话说明用途" class="w-full" />
          </UFormField>
          <UFormField label="系统提示词">
            <UTextarea
              v-model="editorForm.systemPrompt"
              :rows="4"
              class="w-full"
              placeholder="你是…"
            />
          </UFormField>
          <UFormField label="模型" required>
            <USelect
              v-model="editorForm.modelRef"
              :items="modelItems"
              placeholder="选择已启用的模型"
              class="w-full"
              :disabled="!modelItems.length"
            />
            <p v-if="!modelItems.length" class="mt-1 text-xs text-[var(--ws-text-muted)]">
              无可用模型：请先在管理端配置并启用模型连接。
            </p>
          </UFormField>
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end gap-2">
          <UButton color="neutral" variant="ghost" @click="editorOpen = false">
            取消
          </UButton>
          <UButton @click="submitEditor">
            保存
          </UButton>
        </div>
      </template>
    </UModal>

    <UModal v-model:open="autoOpen" title="自动化" description="定时运行智能体并通过消息推送结果">
      <template #body>
        <div class="space-y-4">
          <UFormField label="标题" required>
            <UInput v-model="autoForm.title" placeholder="例如：每日任务摘要" class="w-full" />
          </UFormField>
          <UFormField label="智能体" required>
            <USelect
              v-model="autoForm.agentId"
              :items="agentSelectItems"
              placeholder="选择智能体"
              class="w-full"
            />
          </UFormField>
          <UFormField label="任务提示词" required>
            <UTextarea
              v-model="autoForm.prompt"
              :rows="4"
              class="w-full"
              placeholder="告诉智能体要做什么…"
            />
          </UFormField>
          <UFormField label="Cron（6 段）" required>
            <UInput v-model="autoForm.cronExpr" placeholder="0 0 9 * * ?" class="w-full" />
            <p class="mt-1 text-xs text-[var(--ws-text-muted)]">
              示例：每天 9:00 → <code>0 0 9 * * ?</code>；每分钟 → <code>0 * * * * ?</code>
            </p>
          </UFormField>
        </div>
      </template>
      <template #footer>
        <div class="flex justify-end gap-2">
          <UButton color="neutral" variant="ghost" @click="autoOpen = false">
            取消
          </UButton>
          <UButton :loading="agent.automationBusy" @click="submitAutomation">
            保存
          </UButton>
        </div>
      </template>
    </UModal>
  </WorkspaceShell>
</template>

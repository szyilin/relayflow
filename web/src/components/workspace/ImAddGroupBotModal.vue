<script setup lang="ts">
import WorkspaceAvatar from './WorkspaceAvatar.vue'
import { computed, ref, watch } from 'vue'
import WorkspaceModalBody from './WorkspaceModalBody.vue'
import { useImStore } from '../../stores/im'
import type { GroupBotCatalogItem } from '../../api/app/im'

const props = defineProps<{
  conversationId: string
}>()

const open = defineModel<boolean>('open', { required: true })
const emit = defineEmits<{
  added: []
}>()

const im = useImStore()
const catalog = ref<GroupBotCatalogItem[]>([])
const selectedCode = ref<string | null>(null)
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref<string | null>(null)

const availableBots = computed(() => catalog.value.filter(item => !item.alreadyMember))

watch(open, async (isOpen) => {
  if (!isOpen) {
    return
  }
  selectedCode.value = null
  errorMessage.value = null
  loading.value = true
  try {
    catalog.value = await im.fetchGroupBotCatalog(props.conversationId)
  } catch (error) {
    catalog.value = []
    errorMessage.value = error instanceof Error ? error.message : '加载机器人目录失败'
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  if (!selectedCode.value) {
    errorMessage.value = '请选择一个机器人'
    return
  }
  submitting.value = true
  errorMessage.value = null
  try {
    await im.addGroupBot(props.conversationId, selectedCode.value)
    emit('added')
    open.value = false
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '添加机器人失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <UModal
    v-model:open="open"
    title="添加机器人"
  >
    <template #body>
      <WorkspaceModalBody min-height-class="min-h-[14rem]">
        <form
          id="im-add-group-bot-form"
          class="space-y-4"
          @submit.prevent="handleSubmit"
        >
          <div
            v-if="loading"
            class="space-y-2"
          >
            <USkeleton
              v-for="i in 3"
              :key="i"
              class="h-10 w-full"
            />
          </div>

          <div
            v-else-if="availableBots.length"
            class="space-y-1 rounded-lg border border-[var(--ws-border-subtle)] p-2"
          >
            <label
              v-for="bot in availableBots"
              :key="bot.botCode"
              class="flex cursor-pointer items-center gap-3 rounded-md px-2 py-2 hover:bg-[var(--ws-rail-hover)]"
            >
              <UCheckbox
                :model-value="selectedCode === bot.botCode"
                @update:model-value="selectedCode = $event === true ? bot.botCode : null"
              />
              <WorkspaceAvatar
                :src="bot.avatar"
                :text="bot.avatarText"
                :seed="bot.botId || bot.botCode"
                icon="i-lucide-bot"
                size="sm"
              />
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-medium">
                  {{ bot.name }}
                </p>
                <p class="truncate text-xs text-[var(--ws-text-muted)]">
                  {{ bot.botCode }}
                </p>
              </div>
            </label>
          </div>

          <UEmpty
            v-else
            icon="i-lucide-bot"
            title="没有可添加的机器人"
            description="暂无你拥有的、可嵌入群聊的智能体机器人"
            class="flex min-h-[10rem] items-center justify-center py-6"
          />

          <p
            v-if="errorMessage"
            class="text-sm text-error"
          >
            {{ errorMessage }}
          </p>
        </form>
      </WorkspaceModalBody>
    </template>

    <template #footer>
      <div class="flex w-full justify-end gap-2">
        <UButton
          color="neutral"
          variant="ghost"
          @click="open = false"
        >
          取消
        </UButton>
        <UButton
          type="submit"
          form="im-add-group-bot-form"
          :loading="submitting"
          :disabled="!availableBots.length"
        >
          添加
        </UButton>
      </div>
    </template>
  </UModal>
</template>

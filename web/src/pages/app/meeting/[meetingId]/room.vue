<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MeetingParticipantTile from '../../../../components/workspace/MeetingParticipantTile.vue'
import { useRtcRoom } from '../../../../composables/rtc'
import { useMeetingStore } from '../../../../stores/meeting'
import { useAuthStore } from '../../../../stores/auth'
import { useToast } from '../../../../composables/useToast'

const route = useRoute()
const router = useRouter()
const meetingStore = useMeetingStore()
const auth = useAuthStore()
const toast = useToast()

const meetingId = computed(() => String(route.params.meetingId || ''))
const wantAudio = computed(() => route.query.audio !== '0')
const wantVideo = computed(() => route.query.video === '1')

const {
  connecting,
  error,
  participants,
  connect,
  disconnect,
  setMicEnabled,
  setCameraEnabled,
  sendChat,
  onChat
} = useRtcRoom()

const micOn = ref(wantAudio.value)
const camOn = ref(wantVideo.value)
const chatOpen = ref(true)
const chatInput = ref('')
const chatMessages = ref<{ id: string, from: string, text: string }[]>([])
const title = ref('视频会议')
const meetingCode = ref('')
let offChat: (() => void) | undefined

const tiles = computed(() => participants.value)

onMounted(async () => {
  try {
    const cred = await meetingStore.getJoinCredentials(meetingId.value)
    title.value = cred.title
    meetingCode.value = cred.meetingCode
    if (!cred.livekitUrl || !cred.token) {
      throw new Error('入会凭证不完整')
    }
    await connect({
      url: cred.livekitUrl,
      token: cred.token,
      audio: wantAudio.value,
      video: wantVideo.value
    })
    offChat = onChat((msg) => {
      chatMessages.value.push({
        id: `${Date.now()}-${Math.random()}`,
        from: msg.from,
        text: msg.text
      })
    })
  } catch (e) {
    toast.add({
      title: e instanceof Error ? e.message : '进入会议失败',
      color: 'error'
    })
  }
})

onUnmounted(async () => {
  offChat?.()
  await disconnect()
})

async function toggleMic() {
  micOn.value = !micOn.value
  try {
    await setMicEnabled(micOn.value)
  } catch {
    // mock ok
  }
}

async function toggleCam() {
  camOn.value = !camOn.value
  try {
    await setCameraEnabled(camOn.value)
  } catch {
    // mock ok
  }
}

async function onSendChat() {
  const text = chatInput.value.trim()
  if (!text) {
    return
  }
  chatInput.value = ''
  const from = String(auth.user?.nickname || auth.user?.username || auth.userId || '我')
  chatMessages.value.push({
    id: `${Date.now()}`,
    from,
    text
  })
  try {
    await sendChat(text)
  } catch {
    // mock: local only
  }
}

async function hangup() {
  try {
    const { leaveMeeting } = await import('../../../../api/app/meeting')
    await leaveMeeting(meetingId.value)
  } catch {
    // best-effort
  }
  await disconnect()
  await router.push('/app/meeting')
}
</script>

<template>
  <div class="flex h-svh w-full flex-col bg-neutral-950 text-white">
    <header class="flex items-center justify-between gap-3 border-b border-white/10 px-4 py-3">
      <div class="min-w-0">
        <h1 class="truncate text-sm font-semibold">
          {{ title }}
        </h1>
        <p class="text-xs text-white/50">
          会议号 {{ meetingCode || '—' }}
          <span v-if="connecting" class="ml-2">连接中…</span>
          <span v-else-if="error" class="ml-2 text-red-400">{{ error }}</span>
        </p>
      </div>
      <UButton
        color="neutral"
        variant="ghost"
        :icon="chatOpen ? 'i-lucide-panel-right-close' : 'i-lucide-panel-right-open'"
        @click="chatOpen = !chatOpen"
      />
    </header>

    <div class="flex min-h-0 flex-1">
      <div class="min-w-0 flex-1 overflow-y-auto p-4">
        <div
          class="grid gap-3"
          :class="tiles.length <= 1
            ? 'grid-cols-1 max-w-3xl mx-auto'
            : tiles.length <= 4
              ? 'grid-cols-1 sm:grid-cols-2'
              : 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3'"
        >
          <MeetingParticipantTile
            v-for="p in tiles"
            :key="p.identity"
            :participant="p"
          />
        </div>
      </div>

      <aside
        v-if="chatOpen"
        class="flex w-72 shrink-0 flex-col border-l border-white/10 bg-neutral-900"
      >
        <div class="border-b border-white/10 px-3 py-2 text-sm font-medium">
          会议聊天
        </div>
        <div class="flex-1 space-y-2 overflow-y-auto px-3 py-2 text-sm">
          <div
            v-for="m in chatMessages"
            :key="m.id"
            class="rounded-lg bg-white/5 px-2 py-1.5"
          >
            <div class="text-xs text-white/50">
              {{ m.from }}
            </div>
            <div>{{ m.text }}</div>
          </div>
          <p v-if="!chatMessages.length" class="py-8 text-center text-xs text-white/40">
            发送消息与会中成员交流
          </p>
        </div>
        <form class="flex gap-2 border-t border-white/10 p-2" @submit.prevent="onSendChat">
          <UInput
            v-model="chatInput"
            size="sm"
            class="flex-1"
            placeholder="输入消息"
            :ui="{ base: 'bg-white/10 text-white' }"
          />
          <UButton type="submit" size="sm" color="primary">
            发送
          </UButton>
        </form>
      </aside>
    </div>

    <footer class="flex items-center justify-center gap-3 border-t border-white/10 px-4 py-3">
      <UButton
        :color="micOn ? 'neutral' : 'error'"
        :variant="micOn ? 'soft' : 'solid'"
        :icon="micOn ? 'i-lucide-mic' : 'i-lucide-mic-off'"
        @click="toggleMic"
      />
      <UButton
        :color="camOn ? 'neutral' : 'error'"
        :variant="camOn ? 'soft' : 'solid'"
        :icon="camOn ? 'i-lucide-video' : 'i-lucide-video-off'"
        @click="toggleCam"
      />
      <UButton color="error" icon="i-lucide-phone-off" @click="hangup">
        离开
      </UButton>
    </footer>
  </div>
</template>

<route lang="yaml">
meta:
  layout: workspace
</route>

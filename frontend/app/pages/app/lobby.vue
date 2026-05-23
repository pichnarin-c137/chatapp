<script setup lang="ts">
import { LOBBY_ID } from '~/types/conversation'

definePageMeta({ middleware: ['auth'], layout: 'chat' })

const authStore = useAuthStore()
const convStore = useConversationStore()
const { loadHistory, subscribeConversation } = useConversation()
const { send, markSeen } = useChat()

const messages = computed(() => convStore.messagesFor(LOBBY_ID))
const tz = computed(() => authStore.timezone)
const canSend = computed(() => convStore.connection === 'connected')

function markLatestSeen() {
  const last = messages.value[messages.value.length - 1]
  if (last && !last.id.startsWith('temp-')) markSeen(LOBBY_ID, last.id)
}

onMounted(async () => {
  subscribeConversation(LOBBY_ID)
  if (messages.value.length === 0) {
    await loadHistory(LOBBY_ID).catch(() => {})
  }
  markLatestSeen()
})

watch(() => messages.value.length, markLatestSeen)

function onSend(content: string) {
  send(LOBBY_ID, content)
}
</script>

<template>
  <ChatPane
    :conversation-id="LOBBY_ID"
    title="Lobby"
    subtitle="Public channel · everyone is here"
    avatar="#"
    :messages="messages"
    :current-user-id="authStore.user?.id"
    :timezone="tz"
    placeholder="Message #lobby"
    :disabled="!canSend"
    show-back
    back-to="/app"
    @send="onSend"
  />
</template>

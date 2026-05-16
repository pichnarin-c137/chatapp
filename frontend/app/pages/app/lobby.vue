<script setup lang="ts">
import { LOBBY_ID } from '~/composables/useChat'

definePageMeta({ middleware: ['auth'], layout: 'chat' })

const authStore = useAuthStore()
const chatStore = useChatStore()
const { send, loadHistory, subscribeRoom } = useChat()

const messages = computed(() => chatStore.messagesFor(LOBBY_ID))
const tz = computed(() => authStore.timezone)
const canSend = computed(() => chatStore.connection === 'connected')

onMounted(async () => {
  subscribeRoom(LOBBY_ID)
  if (messages.value.length === 0) {
    await loadHistory(LOBBY_ID).catch(() => {})
  }
})

function onSend(content: string) {
  send(LOBBY_ID, content)
}
</script>

<template>
  <ChatPane
    title="Lobby"
    subtitle="Public room · everyone is here"
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

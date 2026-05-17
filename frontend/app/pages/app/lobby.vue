<script setup lang="ts">
import { LOBBY_ID } from '~/types/conversation'

definePageMeta({ middleware: ['auth'], layout: 'chat' })

const authStore = useAuthStore()
const convStore = useConversationStore()
const { send, loadHistory, subscribeConversation } = useConversation()

const messages = computed(() => convStore.messagesFor(LOBBY_ID))
const tz = computed(() => authStore.timezone)
const canSend = computed(() => convStore.connection === 'connected')

onMounted(async () => {
  subscribeConversation(LOBBY_ID)
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

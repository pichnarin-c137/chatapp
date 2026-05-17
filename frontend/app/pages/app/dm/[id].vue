<script setup lang="ts">
definePageMeta({ middleware: ['auth'], layout: 'chat' })

const route = useRoute()
const authStore = useAuthStore()
const convStore = useConversationStore()
const {
  loadConversation,
  loadHistory,
  subscribeConversation,
  unsubscribeConversation,
  send,
} = useConversation()

const conversationId = computed(() => String(route.params.id))
const conversation = computed(() => convStore.conversationById(conversationId.value))
const messages = computed(() => convStore.messagesFor(conversationId.value))
const tz = computed(() => authStore.timezone)
const canSend = computed(() => convStore.connection === 'connected')

async function bootstrap(id: string) {
  try {
    if (!convStore.conversationById(id)) await loadConversation(id)
  } catch (e) {
    console.error('loadConversation failed', e)
    await navigateTo('/app')
    return
  }
  if ((convStore.messagesFor(id) ?? []).length === 0) {
    await loadHistory(id).catch(() => {})
  }
  subscribeConversation(id)
}

onMounted(() => bootstrap(conversationId.value))

watch(conversationId, async (newId, oldId) => {
  if (oldId && oldId !== newId) {
    unsubscribeConversation(oldId)
    await bootstrap(newId)
  }
})

function onSend(content: string) {
  send(conversationId.value, content)
}

const title = computed(() => {
  if (!conversation.value) return 'Loading…'
  if (conversation.value.name) return conversation.value.name
  if (conversation.value.type === 'DIRECT') return 'Direct message'
  return 'Conversation'
})
const subtitle = computed(() => conversation.value?.topic ?? '')
</script>

<template>
  <ChatPane
    :title="title"
    :subtitle="subtitle"
    :messages="messages"
    :current-user-id="authStore.user?.id"
    :timezone="tz"
    placeholder="Type a message"
    :disabled="!canSend"
    show-back
    back-to="/app"
    @send="onSend"
  />
</template>

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
} = useConversation()
const { send, markSeen } = useChat()

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
  markLatestSeen(id)
  convStore.clearMentionUnread(id)
}

function markLatestSeen(id: string) {
  const list = convStore.messagesFor(id)
  const last = list[list.length - 1]
  if (last && !last.id.startsWith('temp-')) markSeen(id, last.id)
}

onMounted(() => bootstrap(conversationId.value))

watch(conversationId, async (newId, oldId) => {
  if (oldId && oldId !== newId) {
    unsubscribeConversation(oldId)
    await bootstrap(newId)
  }
})

// Auto-mark new messages as seen when they arrive while this conversation is open.
watch(
  () => messages.value.length,
  () => {
    markLatestSeen(conversationId.value)
    convStore.clearMentionUnread(conversationId.value)
  },
)

function onSend(content: string) {
  send(conversationId.value, content)
}

const title = computed(() => {
  if (!conversation.value) return 'Loading…'
  if (conversation.value.type === 'DIRECT') {
    return conversation.value.dmOther?.username ?? 'Direct message'
  }
  return conversation.value.name ?? 'Conversation'
})
const subtitle = computed(() => {
  if (conversation.value?.type === 'DIRECT' && conversation.value.dmOther) {
    return `@${conversation.value.dmOther.username}`
  }
  return conversation.value?.topic ?? ''
})
</script>

<template>
  <ChatPane
    :conversation-id="conversationId"
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

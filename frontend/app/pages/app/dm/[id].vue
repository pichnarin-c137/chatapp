<script setup lang="ts">
definePageMeta({ middleware: ['auth'], layout: 'chat' })

const route = useRoute()
const authStore = useAuthStore()
const dmStore = useDmStore()
const {
  loadConversation,
  loadHistory,
  subscribeConversation,
  unsubscribeConversation,
  send,
} = useDm()

const conversationId = computed(() => String(route.params.id))
const conversation = computed(() => dmStore.conversationById(conversationId.value))
const messages = computed(() => dmStore.messagesFor(conversationId.value))
const tz = computed(() => authStore.timezone)

const chatStore = useChatStore()
const canSend = computed(() => chatStore.connection === 'connected')

async function bootstrap(id: string) {
  try {
    if (!dmStore.conversationById(id)) await loadConversation(id)
  } catch (e) {
    console.error('loadConversation failed', e)
    await navigateTo('/app')
    return
  }
  if ((dmStore.messagesFor(id) ?? []).length === 0) {
    await loadHistory(id)
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

const title = computed(() => conversation.value ? `@${conversation.value.peer.username}` : 'Loading…')
const subtitle = computed(() => conversation.value?.peer.email ?? '')
</script>

<template>
  <ChatPane
    :title="title"
    :subtitle="subtitle"
    :messages="messages"
    :current-user-id="authStore.user?.id"
    :timezone="tz"
    :placeholder="conversation ? `Message @${conversation.peer.username}` : 'Message'"
    :disabled="!canSend"
    show-back
    back-to="/app"
    @send="onSend"
  />
</template>

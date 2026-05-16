import type { DirectConversation, DirectMessage } from '~/types/dm'
import { useDmStore } from '~/stores/dm'

export function useDm() {
  const store = useDmStore()
  const { request } = useApi()
  const { connect, disconnect, subscribe, unsubscribe, publish } = useStomp()

  async function loadConversations() {
    const list = await request<DirectConversation[]>('/api/dm')
    store.setConversations(list)
    return list
  }

  async function startWith(userId: string) {
    const conv = await request<DirectConversation>(`/api/dm/with/${userId}`, {
      method: 'POST',
    })
    store.upsertConversation(conv)
    return conv
  }

  async function loadConversation(conversationId: string) {
    const conv = await request<DirectConversation>(`/api/dm/${conversationId}`)
    store.upsertConversation(conv)
    return conv
  }

  async function loadHistory(conversationId: string) {
    const messages = await request<DirectMessage[]>(
      `/api/dm/${conversationId}/messages`,
    )
    store.setMessages(conversationId, messages)
    return messages
  }

  function subscribeConversation(conversationId: string) {
    subscribe(`/topic/dm.${conversationId}`, (body) => {
      try {
        const msg = JSON.parse(body) as DirectMessage
        store.appendMessage(msg)
      } catch (e) {
        console.error('Bad DM frame', e)
      }
    })
  }

  function unsubscribeConversation(conversationId: string) {
    unsubscribe(`/topic/dm.${conversationId}`)
  }

  function send(conversationId: string, content: string, replyTo?: string) {
    const trimmed = content.trim()
    if (!trimmed) return
    publish('/app/dm.send', {
      conversationId,
      content: trimmed,
      replyTo: replyTo ?? null,
    })
  }

  return {
    connect,
    disconnect,
    loadConversations,
    startWith,
    loadConversation,
    loadHistory,
    subscribeConversation,
    unsubscribeConversation,
    send,
  }
}

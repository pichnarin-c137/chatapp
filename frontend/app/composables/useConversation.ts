import type { Conversation, Message } from '~/types/conversation'
import { useConversationStore } from '~/stores/conversation'

export function useConversation() {
  const store = useConversationStore()
  const { request } = useApi()
  const { connect, disconnect, subscribe, unsubscribe, publish } = useStomp()

  async function loadMyConversations() {
    const list = await request<Conversation[]>('/api/conversations')
    store.setConversations(list)
    return list
  }

  async function getOrCreateDirect(userId: string) {
    const conv = await request<Conversation>(
      `/api/conversations/direct/${userId}`,
      { method: 'POST' },
    )
    store.upsertConversation(conv)
    return conv
  }

  async function loadConversation(id: string) {
    const conv = await request<Conversation>(`/api/conversations/${id}`)
    store.upsertConversation(conv)
    return conv
  }

  async function loadHistory(id: string) {
    // Backend returns newest-first; reverse so the UI scrolls naturally.
    const messages = await request<Message[]>(
      `/api/conversations/${id}/messages?page=0&size=50`,
    )
    const ordered = [...messages].reverse()
    store.setMessages(id, ordered)
    return ordered
  }

  function subscribeConversation(id: string) {
    subscribe(`/topic/conversations/${id}`, (body) => {
      try {
        const msg = JSON.parse(body) as Message
        store.appendMessage(msg)
      } catch (e) {
        console.error('Bad message frame', e)
      }
    })
  }

  function unsubscribeConversation(id: string) {
    unsubscribe(`/topic/conversations/${id}`)
  }

  function send(id: string, body: string, replyToId?: string | null) {
    const trimmed = body.trim()
    if (!trimmed) return
    publish(`/app/conversations/${id}/send`, {
      body: trimmed,
      replyToId: replyToId ?? null,
    })
  }

  return {
    connect,
    disconnect,
    loadMyConversations,
    getOrCreateDirect,
    loadConversation,
    loadHistory,
    subscribeConversation,
    unsubscribeConversation,
    send,
  }
}

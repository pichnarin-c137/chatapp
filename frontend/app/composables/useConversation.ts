import type {
  ChatEvent,
  Conversation,
  MessagePage,
} from '~/types/conversation'
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

  /** Initial page of history (newest first from server; reversed for natural scroll). */
  async function loadHistory(id: string) {
    const page = await request<MessagePage>(
      `/api/conversations/${id}/messages?limit=50&direction=BEFORE`,
    )
    const ordered = [...page.items].reverse()
    store.setMessages(id, ordered, page.nextCursor)
    return ordered
  }

  /** Page older messages on scroll-to-top. */
  async function loadOlderHistory(id: string) {
    const cursor = store.$state.oldestCursorByConversation[id]
    if (!cursor) return
    const page = await request<MessagePage>(
      `/api/conversations/${id}/messages?limit=50&direction=BEFORE&cursor=${encodeURIComponent(cursor)}`,
    )
    const ordered = [...page.items].reverse()
    store.prependMessages(id, ordered, page.nextCursor)
  }

  function subscribeConversation(id: string) {
    subscribe(`/topic/conversations/${id}`, (body) => {
      try {
        const event = JSON.parse(body) as ChatEvent
        handleEvent(event)
      } catch (e) {
        console.error('Bad event frame', e)
      }
    })
  }

  function handleEvent(event: ChatEvent) {
    switch (event.event) {
      case 'message.sent':
        store.appendMessage(event.message)
        return
      case 'message.edited':
        store.applyEdit(event.convId, event.messageId, event.body, event.editedAt)
        return
      case 'message.deleted':
        store.applyDelete(event.convId, event.messageId, event.deletedAt)
        return
      case 'message.seen':
        store.applySeen(event.convId, event.userId, event.lastSeenMessageId)
        return
      case 'message.pinned':
        store.applyPin(event.pin)
        return
      case 'message.unpinned':
        store.applyUnpin(event.convId, event.messageId)
        return
    }
  }

  function unsubscribeConversation(id: string) {
    unsubscribe(`/topic/conversations/${id}`)
  }

  return {
    connect,
    disconnect,
    publish,
    loadMyConversations,
    getOrCreateDirect,
    loadConversation,
    loadHistory,
    loadOlderHistory,
    subscribeConversation,
    unsubscribeConversation,
  }
}

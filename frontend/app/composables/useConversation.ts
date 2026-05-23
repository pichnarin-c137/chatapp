import type {
  ChatEvent,
  Conversation,
  MentionNotificationEvent,
  Message,
  MessagePage,
  Reaction,
} from '~/types/conversation'
import { useAuthStore } from '~/stores/auth'
import { useConversationStore } from '~/stores/conversation'
import { usePresenceStore } from '~/stores/presence'
import { useReactionStore } from '~/stores/reactions'
import { useTypingStore } from '~/stores/typing'

export function useConversation() {
  const store = useConversationStore()
  const reactionStore = useReactionStore()
  const typingStore = useTypingStore()
  const presenceStore = usePresenceStore()
  const authStore = useAuthStore()
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
    seedReactionsFromMessages(ordered)
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
    seedReactionsFromMessages(ordered)
    store.prependMessages(id, ordered, page.nextCursor)
  }

  function seedReactionsFromMessages(msgs: Message[]) {
    const map: Record<string, Reaction[]> = {}
    for (const m of msgs) {
      if (m.reactions && m.reactions.length > 0) map[m.id] = m.reactions
    }
    if (Object.keys(map).length > 0) reactionStore.setForMessages(map)
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
    subscribe(`/topic/conversations/${id}/typing`, (body) => {
      try {
        const event = JSON.parse(body) as ChatEvent
        handleEvent(event)
      } catch (e) {
        console.error('Bad typing frame', e)
      }
    })
  }

  function subscribePresence(userId: string) {
    subscribe(`/topic/users/${userId}/presence`, (body) => {
      try {
        const event = JSON.parse(body) as ChatEvent
        handleEvent(event)
      } catch (e) {
        console.error('Bad presence frame', e)
      }
    })
  }

  function unsubscribePresence(userId: string) {
    unsubscribe(`/topic/users/${userId}/presence`)
  }

  /**
   * One-time subscription for the logged-in user's personal queue.
   * Mention notifications land here. Spring rewrites /user/queue/...
   * into the per-session destination keyed by Principal.getName() (the
   * user id, see JwtChannelInterceptor).
   */
  function subscribeMentionNotifications() {
    subscribe('/user/queue/notifications', (body) => {
      try {
        const event = JSON.parse(body) as MentionNotificationEvent
        if (event.event === 'mention') {
          store.bumpMentionUnread(event.convId)
          flashMention(event.messageId)
        }
      } catch (e) {
        console.error('Bad notification frame', e)
      }
    })
  }

  function flashMention(messageId: string) {
    if (!import.meta.client) return
    requestAnimationFrame(() => {
      const el = document.getElementById(`msg-${messageId}`)
      if (!el) return
      el.classList.add('ring-2', 'ring-amber-400')
      window.setTimeout(() => el.classList.remove('ring-2', 'ring-amber-400'), 1500)
    })
  }

  function handleEvent(event: ChatEvent) {
    switch (event.event) {
      case 'message.sent':
        if (event.message.reactions && event.message.reactions.length > 0) {
          reactionStore.setForMessages({ [event.message.id]: event.message.reactions })
        }
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
      case 'reaction.changed':
        reactionStore.applyChange({
          messageId: event.messageId,
          userId: event.userId,
          emoji: event.emoji,
          action: event.action,
          viewerId: authStore.user?.id ?? null,
        })
        return
      case 'typing.start':
        if (event.userId === authStore.user?.id) return
        typingStore.applyStart(event.convId, {
          userId: event.userId,
          username: event.username,
          expiresAt: event.expiresAt,
        })
        return
      case 'typing.stop':
        typingStore.applyStop(event.convId, event.userId)
        return
      case 'presence.update':
        presenceStore.apply(event.userId, {
          status: event.status,
          lastSeenAt: event.lastSeenAt,
        })
        return
    }
  }

  function unsubscribeConversation(id: string) {
    unsubscribe(`/topic/conversations/${id}`)
    unsubscribe(`/topic/conversations/${id}/typing`)
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
    subscribePresence,
    unsubscribePresence,
    subscribeMentionNotifications,
  }
}

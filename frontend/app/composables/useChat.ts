import type { EditHistoryEntry, Message, Pin, Reaction } from '~/types/conversation'
import { useAuthStore } from '~/stores/auth'
import { useConversationStore } from '~/stores/conversation'
import { useReactionStore } from '~/stores/reactions'

/**
 * High-level chat actions. Owns optimistic UI, idempotency keys, and the
 * STOMP/REST split (writes that need real-time fanout go through STOMP;
 * reads + per-user state hits REST).
 */
export function useChat() {
  const store = useConversationStore()
  const reactionStore = useReactionStore()
  const auth = useAuthStore()
  const { request } = useApi()
  const { publish } = useStomp()

  function newId() {
    if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID()
    return 'tmp-' + Math.random().toString(36).slice(2) + Date.now().toString(36)
  }

  function send(conversationId: string, body: string, replyToId: string | null = null) {
    const trimmed = body.trim()
    if (!trimmed) return
    const idempotencyKey = newId()
    const tempId = 'temp-' + idempotencyKey
    const now = new Date().toISOString()
    // Embed a local reply preview so the optimistic bubble shows quoting immediately;
    // the real preview from the server will overwrite it on echo.
    const draft = store.replyDraftFor(conversationId)
    const effectiveReplyToId = replyToId ?? draft?.id ?? null
    const replyTo = effectiveReplyToId && draft && draft.id === effectiveReplyToId ? {
      id: draft.id,
      senderId: draft.senderId,
      senderUsername: draft.senderUsername,
      body: draft.body,
      deleted: !!draft.deletedAt,
    } : null
    const optimistic: Message = {
      id: tempId,
      tempId,
      conversationId,
      senderId: auth.user?.id ?? null,
      senderUsername: auth.user?.username ?? 'me',
      type: 'TEXT',
      body: trimmed,
      replyToId: effectiveReplyToId,
      replyTo,
      editedAt: null,
      sentAt: now,
      deletedAt: null,
      idempotencyKey,
      state: 'sending',
    }
    store.insertOptimistic(optimistic)
    store.setReplyDraft(conversationId, null)
    publish(`/app/conversations/${conversationId}/send`, {
      body: trimmed,
      replyToId: effectiveReplyToId,
      idempotencyKey,
    })
    setTimeout(() => {
      const list = store.messagesFor(conversationId)
      const stillSending = list.find((m) => m.tempId === tempId && m.state === 'sending')
      if (stillSending) {
        store.removeOptimistic(conversationId, tempId)
      }
    }, 10000)
  }

  function editMessage(conversationId: string, messageId: string, body: string) {
    const trimmed = body.trim()
    if (!trimmed) return
    publish(`/app/conversations/${conversationId}/edit`, { messageId, body: trimmed })
  }

  function deleteForEveryone(conversationId: string, messageId: string) {
    publish(`/app/conversations/${conversationId}/delete`, { messageId, scope: 'FOR_EVERYONE' })
  }

  async function deleteForMe(conversationId: string, messageId: string) {
    await request<void>(`/api/messages/${messageId}?scope=FOR_ME`, { method: 'DELETE' })
    store.hideLocally(conversationId, messageId)
  }

  function markSeen(conversationId: string, lastSeenMessageId: string) {
    if (!lastSeenMessageId || lastSeenMessageId.startsWith('temp-')) return
    publish(`/app/conversations/${conversationId}/seen`, { lastSeenMessageId })
  }

  async function loadEditHistory(messageId: string): Promise<EditHistoryEntry[]> {
    return await request<EditHistoryEntry[]>(`/api/messages/${messageId}/edits`)
  }

  // --- Pins ----------------------------------------------------------------

  async function loadPins(conversationId: string) {
    const pins = await request<Pin[]>(`/api/conversations/${conversationId}/pins`)
    store.setPins(conversationId, pins)
    return pins
  }

  async function pin(messageId: string): Promise<Pin> {
    const pinned = await request<Pin>(`/api/messages/${messageId}/pin`, { method: 'POST' })
    store.applyPin(pinned)
    return pinned
  }

  async function unpin(conversationId: string, messageId: string) {
    await request<void>(`/api/messages/${messageId}/pin`, { method: 'DELETE' })
    store.applyUnpin(conversationId, messageId)
  }

  // --- Forward -------------------------------------------------------------

  async function forward(messageId: string, targetConversationIds: string[]): Promise<Message[]> {
    return await request<Message[]>(`/api/messages/${messageId}/forward`, {
      method: 'POST',
      body: { targetConversationIds },
    })
  }

  // --- Reactions -----------------------------------------------------------

  async function addReaction(messageId: string, emoji: string) {
    if (!emoji) return
    const encoded = encodeURIComponent(emoji)
    // Optimistic apply — the broadcast echo will be a no-op via the
    // duplicate check inside applyChange.
    if (auth.user?.id) {
      reactionStore.applyChange({
        messageId,
        userId: auth.user.id,
        emoji,
        action: 'ADD',
        viewerId: auth.user.id,
      })
    }
    try {
      await request<void>(`/api/messages/${messageId}/reactions/${encoded}`, { method: 'PUT' })
    } catch (e) {
      if (auth.user?.id) {
        reactionStore.applyChange({
          messageId,
          userId: auth.user.id,
          emoji,
          action: 'REMOVE',
          viewerId: auth.user.id,
        })
      }
      throw e
    }
  }

  async function removeReaction(messageId: string, emoji: string) {
    if (!emoji) return
    const encoded = encodeURIComponent(emoji)
    if (auth.user?.id) {
      reactionStore.applyChange({
        messageId,
        userId: auth.user.id,
        emoji,
        action: 'REMOVE',
        viewerId: auth.user.id,
      })
    }
    try {
      await request<void>(`/api/messages/${messageId}/reactions/${encoded}`, { method: 'DELETE' })
    } catch (e) {
      if (auth.user?.id) {
        reactionStore.applyChange({
          messageId,
          userId: auth.user.id,
          emoji,
          action: 'ADD',
          viewerId: auth.user.id,
        })
      }
      throw e
    }
  }

  function toggleReaction(messageId: string, emoji: string) {
    const list = reactionStore.reactionsFor(messageId)
    const bucket = list.find((r) => r.emoji === emoji)
    if (bucket?.mine) return removeReaction(messageId, emoji)
    return addReaction(messageId, emoji)
  }

  async function loadReactions(messageId: string): Promise<Reaction[]> {
    return await request<Reaction[]>(`/api/messages/${messageId}/reactions`)
  }

  // --- Reply draft ---------------------------------------------------------

  function startReply(conversationId: string, msg: Message) {
    store.setReplyDraft(conversationId, msg)
  }

  function cancelReply(conversationId: string) {
    store.setReplyDraft(conversationId, null)
  }

  return {
    send,
    editMessage,
    deleteForEveryone,
    deleteForMe,
    markSeen,
    loadEditHistory,
    loadPins,
    pin,
    unpin,
    forward,
    addReaction,
    removeReaction,
    toggleReaction,
    loadReactions,
    startReply,
    cancelReply,
  }
}

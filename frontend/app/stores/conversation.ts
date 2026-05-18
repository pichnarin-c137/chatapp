import { defineStore } from 'pinia'
import type {
  ConnectionStatus,
  Conversation,
  Message,
  Pin,
} from '~/types/conversation'

interface State {
  conversations: Conversation[]
  messagesByConversation: Record<string, Message[]>
  /** Cursor for paging older history per conversation. null = top reached. */
  oldestCursorByConversation: Record<string, string | null>
  /** Per-conversation, per-user last-seen messageId — drives read ticks. */
  seenByConversation: Record<string, Record<string, string>>
  /** Pins per conversation, newest first. Optional — only loaded on demand. */
  pinsByConversation: Record<string, Pin[]>
  /** Per-conversation pending reply selection (message-to-be-quoted in the next send). */
  replyDraftByConversation: Record<string, Message | null>
  connection: ConnectionStatus
  lastError: string | null
}

export const useConversationStore = defineStore('conversation', {
  state: (): State => ({
    conversations: [],
    messagesByConversation: {},
    oldestCursorByConversation: {},
    seenByConversation: {},
    pinsByConversation: {},
    replyDraftByConversation: {},
    connection: 'idle',
    lastError: null,
  }),

  getters: {
    messagesFor: (s) => (conversationId: string) =>
      s.messagesByConversation[conversationId] ?? [],
    conversationById: (s) => (id: string) =>
      s.conversations.find((c) => c.id === id) ?? null,
    lastSeenMessageIdFor: (s) => (conversationId: string, userId: string) =>
      s.seenByConversation[conversationId]?.[userId] ?? null,
    pinsFor: (s) => (conversationId: string) =>
      s.pinsByConversation[conversationId] ?? [],
    replyDraftFor: (s) => (conversationId: string) =>
      s.replyDraftByConversation[conversationId] ?? null,
  },

  actions: {
    setConversations(list: Conversation[]) {
      this.conversations = list
    },

    upsertConversation(conv: Conversation) {
      const idx = this.conversations.findIndex((c) => c.id === conv.id)
      if (idx >= 0) {
        this.conversations = [
          conv,
          ...this.conversations.slice(0, idx),
          ...this.conversations.slice(idx + 1),
        ]
      } else {
        this.conversations = [conv, ...this.conversations]
      }
    },

    /** Replace the entire message list (typically on first load). */
    setMessages(conversationId: string, messages: Message[], nextCursor: string | null) {
      this.messagesByConversation = {
        ...this.messagesByConversation,
        [conversationId]: messages,
      }
      this.oldestCursorByConversation = {
        ...this.oldestCursorByConversation,
        [conversationId]: nextCursor,
      }
    },

    /** Prepend older messages from a paged backfill. */
    prependMessages(conversationId: string, older: Message[], nextCursor: string | null) {
      const existing = this.messagesByConversation[conversationId] ?? []
      const seen = new Set(existing.map((m) => m.id))
      const merged = [...older.filter((m) => !seen.has(m.id)), ...existing]
      this.messagesByConversation = {
        ...this.messagesByConversation,
        [conversationId]: merged,
      }
      this.oldestCursorByConversation = {
        ...this.oldestCursorByConversation,
        [conversationId]: nextCursor,
      }
    },

    /** Optimistic insert for outgoing messages waiting on server ack. */
    insertOptimistic(msg: Message) {
      const list = this.messagesByConversation[msg.conversationId] ?? []
      this.messagesByConversation = {
        ...this.messagesByConversation,
        [msg.conversationId]: [...list, msg],
      }
    },

    /** Drop an optimistic message that failed and won't retry. */
    removeOptimistic(conversationId: string, tempId: string) {
      const list = this.messagesByConversation[conversationId] ?? []
      this.messagesByConversation = {
        ...this.messagesByConversation,
        [conversationId]: list.filter((m) => m.tempId !== tempId),
      }
    },

    /** Append (or swap a matching optimistic placeholder for) a server-confirmed message. */
    appendMessage(msg: Message) {
      const list = this.messagesByConversation[msg.conversationId] ?? []
      // 1) Dedupe by real id.
      if (list.some((m) => m.id === msg.id)) return
      // 2) If an optimistic placeholder is waiting with the same idempotency key, swap it.
      let merged: Message[]
      if (msg.idempotencyKey) {
        const idx = list.findIndex((m) => m.state === 'sending' && m.idempotencyKey === msg.idempotencyKey)
        if (idx >= 0) {
          merged = [...list]
          merged[idx] = { ...msg, state: 'sent' }
        } else {
          merged = [...list, { ...msg, state: 'sent' }]
        }
      } else {
        merged = [...list, { ...msg, state: 'sent' }]
      }
      this.messagesByConversation = {
        ...this.messagesByConversation,
        [msg.conversationId]: merged,
      }
      const idx = this.conversations.findIndex((c) => c.id === msg.conversationId)
      if (idx >= 0) {
        const updated: Conversation = {
          ...this.conversations[idx]!,
          lastMessageAt: msg.sentAt,
          lastMessage: {
            id: msg.id,
            senderId: msg.senderId,
            senderUsername: msg.senderUsername,
            body: msg.body,
            sentAt: msg.sentAt,
            deleted: !!msg.deletedAt,
          },
        }
        this.conversations = [
          updated,
          ...this.conversations.slice(0, idx),
          ...this.conversations.slice(idx + 1),
        ]
      }
    },

    applyEdit(conversationId: string, messageId: string, body: string, editedAt: string) {
      const list = this.messagesByConversation[conversationId] ?? []
      const idx = list.findIndex((m) => m.id === messageId)
      if (idx < 0) return
      const updated = [...list]
      updated[idx] = { ...updated[idx]!, body, editedAt }
      this.messagesByConversation = {
        ...this.messagesByConversation,
        [conversationId]: updated,
      }
      this.patchLastMessageIfMatch(conversationId, messageId, { body })
    },

    applyDelete(conversationId: string, messageId: string, deletedAt: string) {
      const list = this.messagesByConversation[conversationId] ?? []
      const idx = list.findIndex((m) => m.id === messageId)
      if (idx < 0) return
      const updated = [...list]
      updated[idx] = { ...updated[idx]!, deletedAt, body: null }
      this.messagesByConversation = {
        ...this.messagesByConversation,
        [conversationId]: updated,
      }
      this.patchLastMessageIfMatch(conversationId, messageId, { body: null, deleted: true })
    },

    /** Mirror message-level mutations onto the conversation's lastMessage snapshot. */
    patchLastMessageIfMatch(conversationId: string, messageId: string, patch: Partial<{ body: string | null; deleted: boolean }>) {
      const idx = this.conversations.findIndex((c) => c.id === conversationId)
      if (idx < 0) return
      const conv = this.conversations[idx]!
      if (!conv.lastMessage || conv.lastMessage.id !== messageId) return
      const updated: Conversation = {
        ...conv,
        lastMessage: { ...conv.lastMessage, ...patch },
      }
      this.conversations = [
        ...this.conversations.slice(0, idx),
        updated,
        ...this.conversations.slice(idx + 1),
      ]
    },

    /** Locally hide a message after a "delete for me" succeeds. */
    hideLocally(conversationId: string, messageId: string) {
      const list = this.messagesByConversation[conversationId] ?? []
      this.messagesByConversation = {
        ...this.messagesByConversation,
        [conversationId]: list.filter((m) => m.id !== messageId),
      }
    },

    applySeen(conversationId: string, userId: string, lastSeenMessageId: string) {
      const convMap = this.seenByConversation[conversationId] ?? {}
      this.seenByConversation = {
        ...this.seenByConversation,
        [conversationId]: { ...convMap, [userId]: lastSeenMessageId },
      }
    },

    setPins(conversationId: string, pins: Pin[]) {
      this.pinsByConversation = {
        ...this.pinsByConversation,
        [conversationId]: pins,
      }
    },

    applyPin(pin: Pin) {
      const list = this.pinsByConversation[pin.conversationId] ?? []
      if (list.some((p) => p.messageId === pin.messageId)) return
      this.pinsByConversation = {
        ...this.pinsByConversation,
        [pin.conversationId]: [pin, ...list],
      }
    },

    applyUnpin(conversationId: string, messageId: string) {
      const list = this.pinsByConversation[conversationId] ?? []
      this.pinsByConversation = {
        ...this.pinsByConversation,
        [conversationId]: list.filter((p) => p.messageId !== messageId),
      }
    },

    setReplyDraft(conversationId: string, msg: Message | null) {
      this.replyDraftByConversation = {
        ...this.replyDraftByConversation,
        [conversationId]: msg,
      }
    },

    setConnection(status: ConnectionStatus, error: string | null = null) {
      this.connection = status
      this.lastError = error
    },

    reset() {
      this.conversations = []
      this.messagesByConversation = {}
      this.oldestCursorByConversation = {}
      this.seenByConversation = {}
      this.pinsByConversation = {}
      this.replyDraftByConversation = {}
      this.connection = 'idle'
      this.lastError = null
    },
  },
})

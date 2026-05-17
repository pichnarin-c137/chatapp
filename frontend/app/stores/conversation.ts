import { defineStore } from 'pinia'
import type {
  ConnectionStatus,
  Conversation,
  Message,
} from '~/types/conversation'

interface State {
  conversations: Conversation[]
  messagesByConversation: Record<string, Message[]>
  connection: ConnectionStatus
  lastError: string | null
}

export const useConversationStore = defineStore('conversation', {
  state: (): State => ({
    conversations: [],
    messagesByConversation: {},
    connection: 'idle',
    lastError: null,
  }),

  getters: {
    messagesFor: (s) => (conversationId: string) =>
      s.messagesByConversation[conversationId] ?? [],
    conversationById: (s) => (id: string) =>
      s.conversations.find((c) => c.id === id) ?? null,
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

    setMessages(conversationId: string, messages: Message[]) {
      this.messagesByConversation = {
        ...this.messagesByConversation,
        [conversationId]: messages,
      }
    },

    appendMessage(msg: Message) {
      const existing = this.messagesByConversation[msg.conversationId] ?? []
      if (existing.some((m) => m.id === msg.id)) return
      this.messagesByConversation = {
        ...this.messagesByConversation,
        [msg.conversationId]: [...existing, msg],
      }
      const idx = this.conversations.findIndex(
        (c) => c.id === msg.conversationId,
      )
      if (idx >= 0) {
        const updated: Conversation = {
          ...this.conversations[idx]!,
          lastMessageAt: msg.sentAt,
        }
        this.conversations = [
          updated,
          ...this.conversations.slice(0, idx),
          ...this.conversations.slice(idx + 1),
        ]
      }
    },

    setConnection(status: ConnectionStatus, error: string | null = null) {
      this.connection = status
      this.lastError = error
    },

    reset() {
      this.conversations = []
      this.messagesByConversation = {}
      this.connection = 'idle'
      this.lastError = null
    },
  },
})

import { defineStore } from 'pinia'
import type { DirectConversation, DirectMessage } from '~/types/dm'

interface State {
  conversations: DirectConversation[]
  messagesByConversation: Record<string, DirectMessage[]>
}

export const useDmStore = defineStore('dm', {
  state: (): State => ({
    conversations: [],
    messagesByConversation: {},
  }),

  getters: {
    messagesFor: (s) => (conversationId: string) =>
      s.messagesByConversation[conversationId] ?? [],
    conversationById: (s) => (conversationId: string) =>
      s.conversations.find((c) => c.id === conversationId) ?? null,
  },

  actions: {
    setConversations(list: DirectConversation[]) {
      this.conversations = list
    },

    upsertConversation(conv: DirectConversation) {
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

    setMessages(conversationId: string, messages: DirectMessage[]) {
      this.messagesByConversation = {
        ...this.messagesByConversation,
        [conversationId]: messages,
      }
    },

    appendMessage(msg: DirectMessage) {
      const existing = this.messagesByConversation[msg.conversationId] ?? []
      if (existing.some((m) => m.id === msg.id)) return
      this.messagesByConversation = {
        ...this.messagesByConversation,
        [msg.conversationId]: [...existing, msg],
      }
      const convIdx = this.conversations.findIndex((c) => c.id === msg.conversationId)
      if (convIdx >= 0) {
        const updated: DirectConversation = {
          ...this.conversations[convIdx]!,
          lastMessage: msg,
          lastMessageAt: msg.sentAt,
        }
        this.conversations = [
          updated,
          ...this.conversations.slice(0, convIdx),
          ...this.conversations.slice(convIdx + 1),
        ]
      }
    },

    reset() {
      this.conversations = []
      this.messagesByConversation = {}
    },
  },
})

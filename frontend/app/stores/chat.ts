import { defineStore } from 'pinia'
import type { ChatMessage, ConnectionStatus } from '~/types/chat'

interface State {
  messagesByRoom: Record<string, ChatMessage[]>
  connection: ConnectionStatus
  lastError: string | null
}

export const useChatStore = defineStore('chat', {
  state: (): State => ({
    messagesByRoom: {},
    connection: 'idle',
    lastError: null,
  }),

  getters: {
    messagesFor: (s) => (roomId: string) => s.messagesByRoom[roomId] ?? [],
  },

  actions: {
    setMessages(roomId: string, messages: ChatMessage[]) {
      this.messagesByRoom = { ...this.messagesByRoom, [roomId]: messages }
    },

    appendMessage(msg: ChatMessage) {
      const existing = this.messagesByRoom[msg.roomId] ?? []
      if (existing.some((m) => m.id === msg.id)) return
      this.messagesByRoom = {
        ...this.messagesByRoom,
        [msg.roomId]: [...existing, msg],
      }
    },

    setConnection(status: ConnectionStatus, error: string | null = null) {
      this.connection = status
      this.lastError = error
    },

    reset() {
      this.messagesByRoom = {}
      this.connection = 'idle'
      this.lastError = null
    },
  },
})

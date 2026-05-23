import { defineStore } from 'pinia'
import type { Typer } from '~/types/conversation'

interface State {
  typingByConversation: Record<string, Typer[]>
}

export const useTypingStore = defineStore('typing', {
  state: (): State => ({
    typingByConversation: {},
  }),

  getters: {
    /** Active typers in a conversation, excluding the viewer and expired entries. */
    typersFor: (s) => (conversationId: string, viewerId: string | null) => {
      const list = s.typingByConversation[conversationId] ?? []
      const now = Date.now()
      return list.filter((t) => t.userId !== viewerId && Date.parse(t.expiresAt) > now)
    },
  },

  actions: {
    applyStart(conversationId: string, typer: Typer) {
      const list = this.typingByConversation[conversationId] ?? []
      const idx = list.findIndex((t) => t.userId === typer.userId)
      let next: Typer[]
      if (idx >= 0) {
        next = [...list]
        next[idx] = typer
      } else {
        next = [...list, typer]
      }
      this.typingByConversation = {
        ...this.typingByConversation,
        [conversationId]: next,
      }
    },

    applyStop(conversationId: string, userId: string) {
      const list = this.typingByConversation[conversationId] ?? []
      const next = list.filter((t) => t.userId !== userId)
      if (next.length === list.length) return
      this.typingByConversation = {
        ...this.typingByConversation,
        [conversationId]: next,
      }
    },

    /** Drop expired typers across all conversations — called on a 1s tick. */
    pruneExpired() {
      const now = Date.now()
      let changed = false
      const next: Record<string, Typer[]> = {}
      for (const [convId, list] of Object.entries(this.typingByConversation)) {
        const kept = list.filter((t) => Date.parse(t.expiresAt) > now)
        if (kept.length !== list.length) changed = true
        next[convId] = kept
      }
      if (changed) this.typingByConversation = next
    },

    reset() {
      this.typingByConversation = {}
    },
  },
})

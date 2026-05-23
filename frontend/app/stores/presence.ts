import { defineStore } from 'pinia'
import type { PresenceSnapshot } from '~/types/conversation'

interface State {
  presenceByUserId: Record<string, PresenceSnapshot>
}

export const usePresenceStore = defineStore('presence', {
  state: (): State => ({
    presenceByUserId: {},
  }),

  getters: {
    presenceFor: (s) => (userId: string) => s.presenceByUserId[userId] ?? null,
    isOnline: (s) => (userId: string) => s.presenceByUserId[userId]?.status === 'ONLINE',
  },

  actions: {
    apply(userId: string, snapshot: PresenceSnapshot) {
      this.presenceByUserId = {
        ...this.presenceByUserId,
        [userId]: snapshot,
      }
    },

    setMany(map: Record<string, PresenceSnapshot>) {
      this.presenceByUserId = { ...this.presenceByUserId, ...map }
    },

    reset() {
      this.presenceByUserId = {}
    },
  },
})

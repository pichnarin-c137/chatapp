import { defineStore } from 'pinia'
import type { Reaction } from '~/types/conversation'

interface State {
  reactionsByMessage: Record<string, Reaction[]>
}

export const useReactionStore = defineStore('reactions', {
  state: (): State => ({
    reactionsByMessage: {},
  }),

  getters: {
    reactionsFor: (s) => (messageId: string) =>
      s.reactionsByMessage[messageId] ?? [],
  },

  actions: {
    /** Seed buckets for a freshly-loaded message page. Overrides existing. */
    setForMessages(buckets: Record<string, Reaction[]>) {
      this.reactionsByMessage = { ...this.reactionsByMessage, ...buckets }
    },

    /**
     * Apply a single reaction.changed event from STOMP. Recomputes the
     * count, userIds, and mine flag for the affected bucket.
     */
    applyChange(payload: {
      messageId: string
      userId: string
      emoji: string
      action: 'ADD' | 'REMOVE'
      viewerId: string | null
    }) {
      const { messageId, userId, emoji, action, viewerId } = payload
      const list = this.reactionsByMessage[messageId] ?? []
      const idx = list.findIndex((r) => r.emoji === emoji)
      let next: Reaction[]

      if (action === 'ADD') {
        if (idx >= 0) {
          const bucket = list[idx]!
          if (bucket.userIds.includes(userId)) return // already present
          const userIds = [...bucket.userIds, userId]
          next = [...list]
          next[idx] = {
            ...bucket,
            userIds,
            count: userIds.length,
            mine: bucket.mine || userId === viewerId,
          }
        } else {
          next = [
            ...list,
            {
              messageId,
              emoji,
              userIds: [userId],
              count: 1,
              mine: userId === viewerId,
            },
          ]
        }
      } else {
        if (idx < 0) return
        const bucket = list[idx]!
        if (!bucket.userIds.includes(userId)) return
        const userIds = bucket.userIds.filter((u) => u !== userId)
        if (userIds.length === 0) {
          next = list.filter((_, i) => i !== idx)
        } else {
          next = [...list]
          next[idx] = {
            ...bucket,
            userIds,
            count: userIds.length,
            mine: userId === viewerId ? false : bucket.mine,
          }
        }
      }

      this.reactionsByMessage = {
        ...this.reactionsByMessage,
        [messageId]: next,
      }
    },

    /** Drop reactions for messages that have been removed locally. */
    purge(messageIds: string[]) {
      if (messageIds.length === 0) return
      const next = { ...this.reactionsByMessage }
      for (const id of messageIds) delete next[id]
      this.reactionsByMessage = next
    },

    reset() {
      this.reactionsByMessage = {}
    },
  },
})

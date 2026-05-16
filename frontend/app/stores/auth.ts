import { defineStore } from 'pinia'
import type { User } from '~/types/auth'

const TOKEN_KEY = 'chat.token'
const USER_KEY = 'chat.user'

interface State {
  token: string | null
  user: User | null
}

export const useAuthStore = defineStore('auth', {
  state: (): State => ({
    token: null,
    user: null,
  }),

  getters: {
    isAuthenticated: (s) => !!s.token,
    timezone: (s) => s.user?.timezone ?? 'Asia/Phnom_Penh',
  },

  actions: {
    hydrate() {
      if (!import.meta.client) return
      const t = localStorage.getItem(TOKEN_KEY)
      const u = localStorage.getItem(USER_KEY)
      if (t && u) {
        this.token = t
        try { this.user = JSON.parse(u) } catch { this.user = null }
      }
    },

    setAuth(token: string, user: User) {
      this.token = token
      this.user = user
      if (import.meta.client) {
        localStorage.setItem(TOKEN_KEY, token)
        localStorage.setItem(USER_KEY, JSON.stringify(user))
      }
    },

    setUser(user: User) {
      this.user = user
      if (import.meta.client) {
        localStorage.setItem(USER_KEY, JSON.stringify(user))
      }
    },

    logout() {
      this.token = null
      this.user = null
      if (import.meta.client) {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USER_KEY)
      }
    },
  },
})

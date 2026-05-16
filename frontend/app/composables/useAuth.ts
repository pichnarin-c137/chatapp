import { useAuthStore } from '~/stores/auth'
import type { AuthResponse, User } from '~/types/auth'

export function useAuth() {
  const store = useAuthStore()
  const { request } = useApi()

  async function register(payload: {
    username: string
    email: string
    password: string
    timezone?: string
  }) {
    const res = await request<AuthResponse>('/api/auth/register', {
      method: 'POST',
      body: payload,
    })
    store.setAuth(res.token, res.user)
    return res
  }

  async function login(payload: { email: string; password: string }) {
    const res = await request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: payload,
    })
    store.setAuth(res.token, res.user)
    return res
  }

  async function fetchMe() {
    const me = await request<User>('/api/me')
    store.setUser(me)
    return me
  }

  function logout() {
    store.logout()
  }

  return { register, login, logout, fetchMe, store }
}

import { useAuthStore } from '~/stores/auth'

/**
 * On boot: hydrate the auth store from localStorage, then validate the stored
 * token against the backend. If the token is dead (DB reset, user deleted,
 * expired), clear the store so guards stop treating the user as logged in.
 */
export default defineNuxtPlugin(async () => {
  const auth = useAuthStore()
  auth.hydrate()
  if (!auth.token) return

  const { fetchMe } = useAuth()
  try {
    await fetchMe()
  } catch {
    // Token is stale or backend rejected — flush local state so /login is reachable.
    auth.logout()
  }
})

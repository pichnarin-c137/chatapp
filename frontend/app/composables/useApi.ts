import { useAuthStore } from '~/stores/auth'

type ApiOptions = Parameters<typeof $fetch>[1]

export function useApi() {
  const config = useRuntimeConfig()
  const auth = useAuthStore()

  async function request<T>(path: string, opts: ApiOptions = {}): Promise<T> {
    const headers: Record<string, string> = {
      ...((opts?.headers as Record<string, string>) ?? {}),
    }
    if (auth.token) headers.Authorization = `Bearer ${auth.token}`

    try {
      return await $fetch<T>(path, {
        baseURL: config.public.apiBase,
        ...opts,
        headers,
      })
    } catch (err: any) {
      // 401 with a token means the token went stale (e.g. DB reset, user deleted).
      // Flush the auth store so route guards push the user back to /login.
      if (err?.status === 401 && auth.token) {
        auth.logout()
      }
      throw err
    }
  }

  return { request }
}

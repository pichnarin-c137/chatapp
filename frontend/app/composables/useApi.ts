import { useAuthStore } from '~/stores/auth'

type ApiOptions = Parameters<typeof $fetch>[1]

export function useApi() {
  const config = useRuntimeConfig()
  const auth = useAuthStore()

  function request<T>(path: string, opts: ApiOptions = {}): Promise<T> {
    const headers: Record<string, string> = {
      ...((opts?.headers as Record<string, string>) ?? {}),
    }
    if (auth.token) headers.Authorization = `Bearer ${auth.token}`

    return $fetch<T>(path, {
      baseURL: config.public.apiBase,
      ...opts,
      headers,
    })
  }

  return { request }
}

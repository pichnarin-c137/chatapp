import type { User } from '~/types/auth'

export function useUserSearch() {
  const { request } = useApi()

  async function search(query: string): Promise<User[]> {
    const q = query.trim().replace(/^@+/, '')
    if (!q) return []
    return await request<User[]>(`/api/users/search?q=${encodeURIComponent(q)}`)
  }

  return { search }
}

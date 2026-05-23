import type { PresenceSnapshot } from '~/types/conversation'
import { useAuthStore } from '~/stores/auth'
import { usePresenceStore } from '~/stores/presence'

/**
 * Tracks online status for users we care about (DM counterparts for v1).
 * On boot: subscribe to /topic/users/{id}/presence per user and hydrate
 * with a one-shot GET /api/presence. Also pings /app/presence/heartbeat
 * every 30s while connected so our own ONLINE TTL doesn't expire.
 */
export function usePresence() {
  const presenceStore = usePresenceStore()
  const auth = useAuthStore()
  const { request } = useApi()
  const { publish } = useStomp()
  const { subscribePresence, unsubscribePresence } = useConversation()

  const watched = new Set<string>()
  let heartbeatTimer: number | null = null

  function startHeartbeat() {
    if (!import.meta.client) return
    if (heartbeatTimer !== null) return
    publish('/app/presence/heartbeat', {})
    heartbeatTimer = window.setInterval(() => {
      publish('/app/presence/heartbeat', {})
    }, 30000)
  }

  function stopHeartbeat() {
    if (heartbeatTimer !== null) {
      window.clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  async function hydrate(userIds: string[]) {
    if (userIds.length === 0) return
    try {
      const list = await request<Array<{ userId: string; status: 'ONLINE' | 'OFFLINE'; lastSeenAt: string | null }>>(
        `/api/presence?userIds=${userIds.join(',')}`,
      )
      const map: Record<string, PresenceSnapshot> = {}
      for (let i = 0; i < userIds.length; i++) {
        const row = list[i]
        if (!row) continue
        map[row.userId] = { status: row.status, lastSeenAt: row.lastSeenAt }
      }
      presenceStore.setMany(map)
    } catch (e) {
      console.error('presence hydrate failed', e)
    }
  }

  function watch(userIds: string[]) {
    const fresh: string[] = []
    for (const id of userIds) {
      if (!id || id === auth.user?.id) continue
      if (watched.has(id)) continue
      watched.add(id)
      subscribePresence(id)
      fresh.push(id)
    }
    if (fresh.length > 0) hydrate(fresh)
  }

  function unwatch(userIds: string[]) {
    for (const id of userIds) {
      if (!watched.has(id)) continue
      watched.delete(id)
      unsubscribePresence(id)
    }
  }

  function teardown() {
    stopHeartbeat()
    for (const id of [...watched]) unsubscribePresence(id)
    watched.clear()
  }

  return { watch, unwatch, startHeartbeat, stopHeartbeat, teardown }
}

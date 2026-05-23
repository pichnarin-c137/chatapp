import { useTypingStore } from '~/stores/typing'

/**
 * Typing-indicator emitter. The chat input calls `notifyTyping(convId)`
 * on every keystroke; we coalesce that into:
 *   - a STOMP `START` frame at most every 4 seconds (server TTL is 5s)
 *   - an explicit `STOP` frame 5 seconds after the last keystroke
 *   - an explicit `STOP` on conversation switch or send
 *
 * Also runs a 1s setInterval while mounted to prune expired typers from
 * the store (covers the case where the typer's tab dies mid-type).
 */
export function useTyping() {
  const typingStore = useTypingStore()
  const { publish } = useStomp()

  const lastStartByConv = new Map<string, number>()
  const idleTimerByConv = new Map<string, number>()

  let pruneTimer: number | null = null

  function ensurePruneLoop() {
    if (!import.meta.client) return
    if (pruneTimer !== null) return
    pruneTimer = window.setInterval(() => typingStore.pruneExpired(), 1000)
  }

  function stopPruneLoop() {
    if (pruneTimer !== null) {
      window.clearInterval(pruneTimer)
      pruneTimer = null
    }
  }

  function publishStart(conversationId: string) {
    publish(`/app/conversations/${conversationId}/typing`, { action: 'START' })
  }

  function publishStop(conversationId: string) {
    publish(`/app/conversations/${conversationId}/typing`, { action: 'STOP' })
  }

  /**
   * Call on every keystroke. Throttles START to once every ~4s and
   * arms a 5s STOP timer that resets on each call.
   */
  function notifyTyping(conversationId: string) {
    ensurePruneLoop()
    const now = Date.now()
    const last = lastStartByConv.get(conversationId) ?? 0
    if (now - last >= 4000) {
      lastStartByConv.set(conversationId, now)
      publishStart(conversationId)
    }

    const existing = idleTimerByConv.get(conversationId)
    if (existing !== undefined) window.clearTimeout(existing)
    const timer = window.setTimeout(() => {
      lastStartByConv.delete(conversationId)
      idleTimerByConv.delete(conversationId)
      publishStop(conversationId)
    }, 5000)
    idleTimerByConv.set(conversationId, timer)
  }

  /** Force-stop typing for a conversation (e.g. after sending the message). */
  function stopTyping(conversationId: string) {
    const existing = idleTimerByConv.get(conversationId)
    if (existing !== undefined) {
      window.clearTimeout(existing)
      idleTimerByConv.delete(conversationId)
    }
    if (lastStartByConv.has(conversationId)) {
      lastStartByConv.delete(conversationId)
      publishStop(conversationId)
    }
  }

  /** Cleanup everything (called on component unmount). */
  function teardown() {
    for (const id of [...idleTimerByConv.keys()]) stopTyping(id)
    stopPruneLoop()
  }

  return { notifyTyping, stopTyping, teardown }
}

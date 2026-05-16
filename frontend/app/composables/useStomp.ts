import { Client, type IFrame } from '@stomp/stompjs'
import { useAuthStore } from '~/stores/auth'
import { useChatStore } from '~/stores/chat'

let client: Client | null = null
const subscriptions = new Map<string, () => void>()
const pendingSubscriptions: Array<() => void> = []

export function useStomp() {
  const config = useRuntimeConfig()
  const auth = useAuthStore()
  const chat = useChatStore()

  function connect() {
    if (!import.meta.client) return
    if (!auth.token) return
    if (client?.active) return

    chat.setConnection('connecting')

    client = new Client({
      brokerURL: config.public.wsBase,
      connectHeaders: { Authorization: `Bearer ${auth.token}` },
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        chat.setConnection('connected')
        const queued = pendingSubscriptions.splice(0)
        queued.forEach((fn) => fn())
      },
      onStompError: (frame: IFrame) => {
        chat.setConnection('error', frame.headers.message || 'STOMP error')
      },
      onWebSocketError: () => {
        chat.setConnection('reconnecting', 'WebSocket error')
      },
      onWebSocketClose: () => {
        if (chat.connection === 'connected') chat.setConnection('reconnecting')
      },
    })
    client.activate()
  }

  function subscribe(topic: string, handler: (body: string) => void) {
    if (subscriptions.has(topic)) return
    const doSubscribe = () => {
      if (!client) return
      const sub = client.subscribe(topic, (frame) => handler(frame.body))
      subscriptions.set(topic, () => sub.unsubscribe())
    }
    if (client?.connected) doSubscribe()
    else pendingSubscriptions.push(doSubscribe)
  }

  function unsubscribe(topic: string) {
    const off = subscriptions.get(topic)
    if (off) {
      off()
      subscriptions.delete(topic)
    }
  }

  function publish(destination: string, body: unknown) {
    if (!client?.connected) return
    client.publish({ destination, body: JSON.stringify(body) })
  }

  function disconnect() {
    subscriptions.forEach((off) => off())
    subscriptions.clear()
    pendingSubscriptions.length = 0
    client?.deactivate()
    client = null
    chat.setConnection('idle')
  }

  return { connect, disconnect, subscribe, unsubscribe, publish }
}

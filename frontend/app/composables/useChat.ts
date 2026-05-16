import type { ChatMessage } from '~/types/chat'
import { useChatStore } from '~/stores/chat'

export const LOBBY_ID = '00000000-0000-0000-0000-000000000001'

export function useChat() {
  const chat = useChatStore()
  const { request } = useApi()
  const { connect, disconnect, subscribe, unsubscribe, publish } = useStomp()

  function subscribeRoom(roomId: string) {
    subscribe(`/topic/group.${roomId}`, (body) => {
      try {
        const msg = JSON.parse(body) as ChatMessage
        chat.appendMessage(msg)
      } catch (e) {
        console.error('Bad message frame', e)
      }
    })
  }

  function unsubscribeRoom(roomId: string) {
    unsubscribe(`/topic/group.${roomId}`)
  }

  function send(roomId: string, content: string, replyTo?: string) {
    const trimmed = content.trim()
    if (!trimmed) return
    publish('/app/group.send', { roomId, content: trimmed, replyTo: replyTo ?? null })
  }

  async function loadHistory(roomId: string) {
    const messages = await request<ChatMessage[]>(`/api/rooms/${roomId}/messages`)
    chat.setMessages(roomId, messages)
  }

  return { connect, disconnect, subscribeRoom, unsubscribeRoom, send, loadHistory }
}

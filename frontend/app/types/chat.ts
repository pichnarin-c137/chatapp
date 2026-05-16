export interface ChatMessage {
  id: string
  roomId: string
  senderId: string
  senderUsername: string
  content: string
  type: 'TEXT' | 'IMAGE' | 'FILE'
  replyTo: string | null
  sentAt: string
}

export interface Room {
  id: string
  name: string
  type: 'GROUP' | 'PRIVATE'
  createdAt: string
}

export type ConnectionStatus = 'idle' | 'connecting' | 'connected' | 'reconnecting' | 'error'

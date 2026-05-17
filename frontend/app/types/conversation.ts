export type ConversationType = 'DIRECT' | 'GROUP' | 'CHANNEL'

export type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM'

export interface Conversation {
  id: string
  type: ConversationType
  name: string | null
  topic: string | null
  avatarUrl: string | null
  lastMessageAt: string | null
  createdAt: string
}

export interface Message {
  id: string
  conversationId: string
  senderId: string | null
  senderUsername: string
  type: MessageType
  body: string | null
  replyToId: string | null
  editedAt: string | null
  sentAt: string
}

export type ConnectionStatus =
  | 'idle'
  | 'connecting'
  | 'connected'
  | 'reconnecting'
  | 'error'

/** Deterministic id of the default Lobby channel (matches backend constant). */
export const LOBBY_ID = '00000000-0000-0000-0000-000000000001'

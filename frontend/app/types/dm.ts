import type { User } from '~/types/auth'

export interface DirectMessage {
  id: string
  conversationId: string
  senderId: string
  senderUsername: string
  content: string
  replyTo: string | null
  sentAt: string
}

export interface DirectConversation {
  id: string
  peer: User
  createdAt: string
  lastMessageAt: string | null
  lastMessage: DirectMessage | null
}

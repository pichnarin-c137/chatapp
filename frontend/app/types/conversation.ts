export type ConversationType = 'DIRECT' | 'GROUP' | 'CHANNEL'

export type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM'

export interface DmOther {
  id: string
  username: string
  avatarUrl: string | null
}

export interface LastMessageSnapshot {
  id: string
  senderId: string | null
  senderUsername: string
  body: string | null
  sentAt: string
  deleted: boolean
}

export interface Conversation {
  id: string
  type: ConversationType
  name: string | null
  topic: string | null
  avatarUrl: string | null
  lastMessageAt: string | null
  createdAt: string
  /** For DIRECT conversations, the other participant from the viewer's perspective. */
  dmOther?: DmOther | null
  /** Latest visible message — drives the sidebar subtitle. */
  lastMessage?: LastMessageSnapshot | null
}

/** Transient client-side state for optimistic UI. Not persisted server-side. */
export type MessageState = 'sending' | 'sent' | 'failed'

export interface ReplyPreview {
  id: string
  senderId: string | null
  senderUsername: string
  body: string | null
  deleted: boolean
}

export interface ForwardInfo {
  originalMessageId: string
  originalConversationId: string
  originalSenderId: string
  originalSenderUsername: string
}

export interface MentionRange {
  userId: string
  username: string
  startIndex: number
  endIndex: number
}

export interface Reaction {
  messageId: string
  emoji: string
  count: number
  userIds: string[]
  mine: boolean
}

export type PresenceStatus = 'ONLINE' | 'OFFLINE'

export interface PresenceSnapshot {
  status: PresenceStatus
  lastSeenAt: string | null
}

export interface Typer {
  userId: string
  username: string
  expiresAt: string
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
  /** Set when the message was deleted for everyone. Tombstone in UI. */
  deletedAt: string | null
  /** Inline preview of the replied-to message (server-joined; saves a fetch). */
  replyTo?: ReplyPreview | null
  /** Provenance for forwarded messages — original sender + source conversation. */
  forwardOf?: ForwardInfo | null
  /** Parsed @-mentions; rendered as highlight spans. */
  mentions?: MentionRange[]
  /** Grouped emoji buckets (count + mine). */
  reactions?: Reaction[]
  /** Echoed by server on broadcasts so we can swap the optimistic placeholder. */
  idempotencyKey?: string | null
  /** Client-only: marks an optimistic message that hasn't been ack'd yet. */
  state?: MessageState
  /** Client-only: temp id assigned before the server returns the real id. */
  tempId?: string
}

export interface Pin {
  conversationId: string
  messageId: string
  pinnedBy: string
  pinnedAt: string
  message: Message
}

export interface MessagePage {
  items: Message[]
  nextCursor: string | null
  hasMore: boolean
}

export type ConnectionStatus =
  | 'idle'
  | 'connecting'
  | 'connected'
  | 'reconnecting'
  | 'error'

/** Deterministic id of the default Lobby channel (matches backend constant). */
export const LOBBY_ID = '00000000-0000-0000-0000-000000000001'

// ---- STOMP event envelopes broadcast on /topic/conversations/{id} -----------

interface MessageSentEvent {
  event: 'message.sent'
  convId: string
  message: Message
}

interface MessageEditedEvent {
  event: 'message.edited'
  convId: string
  messageId: string
  body: string
  editedAt: string
  editorId: string
}

interface MessageDeletedEvent {
  event: 'message.deleted'
  convId: string
  messageId: string
  deletedBy: string
  deletedAt: string
  scope: 'EVERYONE'
}

interface MessageSeenEvent {
  event: 'message.seen'
  convId: string
  userId: string
  lastSeenMessageId: string
  seenAt: string
}

interface MessagePinnedEvent {
  event: 'message.pinned'
  convId: string
  messageId: string
  pinnedBy: string
  pinnedAt: string
  pin: Pin
}

interface MessageUnpinnedEvent {
  event: 'message.unpinned'
  convId: string
  messageId: string
  unpinnedBy: string
}

interface ReactionChangedEvent {
  event: 'reaction.changed'
  convId: string
  messageId: string
  userId: string
  emoji: string
  action: 'ADD' | 'REMOVE'
}

interface TypingStartEvent {
  event: 'typing.start'
  convId: string
  userId: string
  username: string
  expiresAt: string
}

interface TypingStopEvent {
  event: 'typing.stop'
  convId: string
  userId: string
  username: string
}

interface PresenceUpdateEvent {
  event: 'presence.update'
  userId: string
  status: PresenceStatus
  lastSeenAt: string | null
}

export interface MentionNotificationEvent {
  event: 'mention'
  convId: string
  messageId: string
  fromUserId: string | null
  fromUsername: string
  preview: string
}

export type ChatEvent =
  | MessageSentEvent
  | MessageEditedEvent
  | MessageDeletedEvent
  | MessageSeenEvent
  | MessagePinnedEvent
  | MessageUnpinnedEvent
  | ReactionChangedEvent
  | TypingStartEvent
  | TypingStopEvent
  | PresenceUpdateEvent

export interface EditHistoryEntry {
  id: string
  messageId: string
  editorId: string
  previousBody: string
  editedAt: string
}

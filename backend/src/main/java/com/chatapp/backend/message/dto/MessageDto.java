package com.chatapp.backend.message.dto;
import com.chatapp.backend.message.entity.Message;
import com.chatapp.backend.message.entity.MessageType;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderUsername,
        MessageType type,
        String body,
        UUID replyToId,
        Instant editedAt,
        Instant sentAt,
        Instant deletedAt,
        /** Snapshot of the replied-to message — null when this message is not a reply. */
        ReplyPreview replyTo,
        /** Set when this message is a forward; points back to the original. */
        ForwardInfo forwardOf,
        /** Echoed back on broadcasts so optimistic clients can swap their temp message. */
        String idempotencyKey
) {

    public record ReplyPreview(
            UUID id,
            UUID senderId,
            String senderUsername,
            String body,
            boolean deleted
    ) {}

    public record ForwardInfo(
            UUID originalMessageId,
            UUID originalConversationId,
            UUID originalSenderId,
            String originalSenderUsername
    ) {}

    public static MessageDto from(Message m, String senderUsername) {
        return from(m, senderUsername, null, null, null);
    }

    public static MessageDto from(Message m, String senderUsername, String idempotencyKey) {
        return from(m, senderUsername, null, null, idempotencyKey);
    }

    public static MessageDto from(Message m,
                                  String senderUsername,
                                  ReplyPreview replyTo,
                                  ForwardInfo forwardOf,
                                  String idempotencyKey) {
        return new MessageDto(
                m.getId(),
                m.getConversation().getId(),
                m.getSender() == null ? null : m.getSender().getId(),
                senderUsername,
                m.getType(),
                m.getBody(),
                m.getReplyToId(),
                m.getEditedAt(),
                m.getSentAt(),
                m.getDeletedAt(),
                replyTo,
                forwardOf,
                idempotencyKey
        );
    }
}

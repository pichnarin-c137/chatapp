package com.chatapp.backend.message.dto;
import com.chatapp.backend.message.entity.Message;
import com.chatapp.backend.message.entity.MessageType;

import java.time.Instant;
import java.util.List;
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
        /** @-mentions parsed at send time; client renders highlight spans from these. */
        List<MentionDto> mentions,
        /** Grouped emoji buckets with `mine` flag for the viewer. */
        List<ReactionDto> reactions,
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
        return from(m, senderUsername, null, null, List.of(), List.of(), null);
    }

    public static MessageDto from(Message m, String senderUsername, String idempotencyKey) {
        return from(m, senderUsername, null, null, List.of(), List.of(), idempotencyKey);
    }

    public static MessageDto from(Message m,
                                  String senderUsername,
                                  ReplyPreview replyTo,
                                  ForwardInfo forwardOf,
                                  List<MentionDto> mentions,
                                  List<ReactionDto> reactions,
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
                mentions == null ? List.of() : mentions,
                reactions == null ? List.of() : reactions,
                idempotencyKey
        );
    }
}

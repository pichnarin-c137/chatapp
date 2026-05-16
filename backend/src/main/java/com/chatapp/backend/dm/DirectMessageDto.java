package com.chatapp.backend.dm;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageDto(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderUsername,
        String content,
        UUID replyTo,
        Instant sentAt
) {
    public static DirectMessageDto from(DirectMessage m, String senderUsername) {
        return new DirectMessageDto(
                m.getId(),
                m.getConversationId(),
                m.getSenderId(),
                senderUsername,
                m.getContent(),
                m.getReplyTo(),
                m.getSentAt()
        );
    }
}

package com.chatapp.backend.message;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID roomId,
        UUID senderId,
        String senderUsername,
        String content,
        String type,
        UUID replyTo,
        Instant sentAt
) {
    public static MessageDto from(Message m, String senderUsername) {
        return new MessageDto(
                m.getId(),
                m.getRoomId(),
                m.getSenderId(),
                senderUsername,
                m.getContent(),
                m.getType().name(),
                m.getReplyTo(),
                m.getSentAt()
        );
    }
}

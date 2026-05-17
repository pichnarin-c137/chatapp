package com.chatapp.backend.message.dto;
import com.chatapp.backend.message.entity.MessageType;
import com.chatapp.backend.message.entity.Message;

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
        Instant sentAt
) {
    public static MessageDto from(Message m, String senderUsername) {
        return new MessageDto(
                m.getId(),
                m.getConversation().getId(),
                m.getSender() == null ? null : m.getSender().getId(),
                senderUsername,
                m.getType(),
                m.getBody(),
                m.getReplyToId(),
                m.getEditedAt(),
                m.getSentAt()
        );
    }
}

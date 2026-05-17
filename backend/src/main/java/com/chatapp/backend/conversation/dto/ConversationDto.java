package com.chatapp.backend.conversation.dto;
import com.chatapp.backend.conversation.entity.ConversationType;
import com.chatapp.backend.conversation.entity.Conversation;

import java.time.Instant;
import java.util.UUID;

public record ConversationDto(
        UUID id,
        ConversationType type,
        String name,
        String topic,
        String avatarUrl,
        Instant lastMessageAt,
        Instant createdAt
) {
    public static ConversationDto from(Conversation c) {
        return new ConversationDto(
                c.getId(),
                c.getType(),
                c.getName(),
                c.getTopic(),
                c.getAvatarUrl(),
                c.getLastMessageAt(),
                c.getCreatedAt()
        );
    }
}

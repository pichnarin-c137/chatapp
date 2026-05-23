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
        Instant createdAt,
        /** For DIRECT conversations, the other participant (from the viewer's perspective). Null for GROUP/CHANNEL. */
        DmOther dmOther,
        /** Compact preview of the latest visible message — drives the sidebar subtitle. Null when there are no messages. */
        LastMessage lastMessage
) {
    public record DmOther(UUID id, String username, String avatarUrl) {}

    public record LastMessage(
            UUID id,
            UUID senderId,
            String senderUsername,
            String body,
            Instant sentAt,
            boolean deleted
    ) {}

    public static ConversationDto from(Conversation c) {
        return from(c, null, null);
    }

    public static ConversationDto from(Conversation c, DmOther dmOther) {
        return from(c, dmOther, null);
    }

    public static ConversationDto from(Conversation c, DmOther dmOther, LastMessage lastMessage) {
        return new ConversationDto(
                c.getId(),
                c.getType(),
                c.getName(),
                c.getTopic(),
                c.getAvatarUrl(),
                c.getLastMessageAt(),
                c.getCreatedAt(),
                dmOther,
                lastMessage
        );
    }
}

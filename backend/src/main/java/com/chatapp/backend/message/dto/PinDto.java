package com.chatapp.backend.message.dto;

import com.chatapp.backend.message.entity.PinnedMessage;

import java.time.Instant;
import java.util.UUID;

public record PinDto(
        UUID conversationId,
        UUID messageId,
        UUID pinnedBy,
        Instant pinnedAt,
        /** Snapshot of the pinned message so the panel can render without a second round-trip. */
        MessageDto message
) {
    public static PinDto of(PinnedMessage p, MessageDto message) {
        return new PinDto(
                p.getId().getConversationId(),
                p.getId().getMessageId(),
                p.getPinnedBy(),
                p.getPinnedAt(),
                message);
    }
}

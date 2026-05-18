package com.chatapp.backend.message.event;

import com.chatapp.backend.message.dto.PinDto;

import java.time.Instant;
import java.util.UUID;

public record MessagePinnedEvent(
        UUID conversationId,
        UUID messageId,
        UUID pinnedBy,
        Instant pinnedAt,
        /** Snapshot of the pin (with the full message embedded) so subscribers can update without a refetch. */
        PinDto pin
) {}

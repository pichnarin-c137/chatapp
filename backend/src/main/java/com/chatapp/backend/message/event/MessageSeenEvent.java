package com.chatapp.backend.message.event;

import java.time.Instant;
import java.util.UUID;

public record MessageSeenEvent(
        UUID conversationId,
        UUID userId,
        UUID lastSeenMessageId,
        Instant seenAt
) {}

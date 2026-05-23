package com.chatapp.backend.message.event;

import java.time.Instant;
import java.util.UUID;

public record MessageEditedEvent(
        UUID conversationId,
        UUID messageId,
        UUID editorId,
        String body,
        Instant editedAt
) {}

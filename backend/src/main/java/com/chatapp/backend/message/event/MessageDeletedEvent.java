package com.chatapp.backend.message.event;

import java.time.Instant;
import java.util.UUID;

/** Fired when a message is deleted for everyone (visible tombstone). Delete-for-me is per-user and not broadcast. */
public record MessageDeletedEvent(
        UUID conversationId,
        UUID messageId,
        UUID deletedBy,
        Instant deletedAt
) {}

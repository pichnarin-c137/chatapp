package com.chatapp.backend.typing.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Fired when a user starts/stops typing in a conversation. The broadcaster
 * fans this out on /topic/conversations/{conversationId}/typing.
 *
 * expiresAt is null for STOP events; for START it's the redis-key TTL
 * deadline so the client can self-evict the typer if no further START
 * arrives.
 */
public record TypingChangedEvent(
        UUID conversationId,
        UUID userId,
        String username,
        Action action,
        Instant expiresAt
) {
    public enum Action { START, STOP }
}

package com.chatapp.backend.presence.dto;

import java.time.Instant;
import java.util.UUID;

public record PresenceDto(
        UUID userId,
        Status status,
        Instant lastSeenAt
) {
    public enum Status { ONLINE, OFFLINE }
}

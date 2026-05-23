package com.chatapp.backend.presence.event;

import com.chatapp.backend.presence.dto.PresenceDto;

import java.util.UUID;

/**
 * Fired whenever a user's online status changes (connect / disconnect).
 * The broadcaster fans this out on /topic/users/{userId}/presence.
 */
public record PresenceChangedEvent(UUID userId, PresenceDto presence) {}

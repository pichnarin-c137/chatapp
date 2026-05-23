package com.chatapp.backend.message.event;

import java.util.UUID;

/**
 * Fired after-commit when a user adds or removes a reaction on a message.
 * Broadcaster fans this out on /topic/conversations/{conversationId}.
 */
public record ReactionChangedEvent(
        UUID conversationId,
        UUID messageId,
        UUID userId,
        String emoji,
        Action action
) {
    public enum Action { ADD, REMOVE }
}

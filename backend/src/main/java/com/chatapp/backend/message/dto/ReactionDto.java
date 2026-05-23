package com.chatapp.backend.message.dto;

import java.util.List;
import java.util.UUID;

/**
 * One bucket per (message, emoji), grouped from message_reactions. The
 * `mine` flag is computed per viewer so the client can highlight the
 * user's own reaction without joining against auth.
 */
public record ReactionDto(
        UUID messageId,
        String emoji,
        int count,
        List<UUID> userIds,
        boolean mine
) {}

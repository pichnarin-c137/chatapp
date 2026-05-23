package com.chatapp.backend.message.dto;

import java.util.UUID;

/**
 * One @mention parsed from a message body. startIndex/endIndex are
 * character offsets into messages.body so the client can render
 * highlight spans without re-parsing.
 */
public record MentionDto(
        UUID userId,
        String username,
        int startIndex,
        int endIndex
) {}

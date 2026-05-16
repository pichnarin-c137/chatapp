package com.chatapp.backend.dm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendDirectMessagePayload(
        UUID conversationId,
        @NotBlank @Size(max = 4000) String content,
        UUID replyTo
) {}

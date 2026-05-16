package com.chatapp.backend.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendMessagePayload(
        UUID roomId,
        @NotBlank @Size(max = 4000) String content,
        UUID replyTo
) {}

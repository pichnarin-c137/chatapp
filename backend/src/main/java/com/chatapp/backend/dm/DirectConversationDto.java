package com.chatapp.backend.dm;

import com.chatapp.backend.user.UserDto;

import java.time.Instant;
import java.util.UUID;

public record DirectConversationDto(
        UUID id,
        UserDto peer,
        Instant createdAt,
        Instant lastMessageAt,
        DirectMessageDto lastMessage
) {}

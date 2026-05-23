package com.chatapp.backend.message.event;

import java.util.UUID;

public record MessageUnpinnedEvent(
        UUID conversationId,
        UUID messageId,
        UUID unpinnedBy
) {}

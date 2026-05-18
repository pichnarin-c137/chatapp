package com.chatapp.backend.message.event;

import com.chatapp.backend.message.dto.MessageDto;

import java.util.UUID;

public record MessageSentEvent(UUID conversationId, MessageDto message) {}

package com.chatapp.backend.message.controller;
import com.chatapp.backend.message.dto.MessageDto;
import com.chatapp.backend.message.service.MessageService;

import com.chatapp.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * Unified STOMP endpoint for sending messages into any conversation type
 * (DM, group, channel). Clients send to /app/conversations/{id}/send and
 * subscribe to /topic/conversations/{id} to receive broadcasts.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final MessageService messages;
    private final SimpMessagingTemplate broker;

    public record SendPayload(String body, UUID replyToId) {}

    @MessageMapping("/conversations/{conversationId}/send")
    public void send(@DestinationVariable UUID conversationId,
                     @Payload SendPayload payload,
                     Principal principal) {
        if (!(principal instanceof org.springframework.security.core.Authentication auth)
                || !(auth.getPrincipal() instanceof User user)) {
            log.warn("STOMP send rejected — unauthenticated");
            return;
        }
        MessageDto dto = messages.send(conversationId, user.getId(), payload.body(), payload.replyToId());
        broker.convertAndSend("/topic/conversations/" + conversationId, dto);
    }
}

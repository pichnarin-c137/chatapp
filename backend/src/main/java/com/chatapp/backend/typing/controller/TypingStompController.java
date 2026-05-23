package com.chatapp.backend.typing.controller;

import com.chatapp.backend.typing.service.TypingService;
import com.chatapp.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * STOMP entry for typing indicators. Clients send
 *   /app/conversations/{id}/typing  { "action": "START" | "STOP" }
 * and the service publishes a TypingChangedEvent that the broadcaster
 * fans out on /topic/conversations/{id}/typing.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TypingStompController {

    private final TypingService typing;

    public record TypingPayload(Action action) {
        public enum Action { START, STOP }
    }

    @MessageMapping("/conversations/{conversationId}/typing")
    public void typing(@DestinationVariable UUID conversationId,
                       @Payload TypingPayload payload,
                       Principal principal) {
        User user = userOf(principal);
        if (user == null) return;
        if (payload == null || payload.action() == null) return;
        switch (payload.action()) {
            case START -> typing.start(conversationId, user.getId(), user.getUsername());
            case STOP  -> typing.stop(conversationId, user.getId(), user.getUsername());
        }
    }

    private static User userOf(Principal principal) {
        if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof User user)) {
            log.warn("STOMP typing rejected — unauthenticated");
            return null;
        }
        return user;
    }
}

package com.chatapp.backend.message.controller;
import com.chatapp.backend.message.dto.DeleteScope;
import com.chatapp.backend.message.service.MessageDeletionService;
import com.chatapp.backend.message.service.MessageReadReceiptService;
import com.chatapp.backend.message.service.MessageService;

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
 * STOMP entry points for chat actions. Broadcasts are handled by
 * ChatEventBroadcaster (listens to domain events emitted by services).
 *
 * Clients send to /app/conversations/{id}/{action} and subscribe to
 * /topic/conversations/{id} to receive event envelopes.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private static final String MOD_AUTHORITY = "MESSAGE_DELETE";

    private final MessageService messages;
    private final MessageDeletionService deletions;
    private final MessageReadReceiptService receipts;

    public record SendPayload(String body, UUID replyToId, String idempotencyKey) {}
    public record EditPayload(UUID messageId, String body) {}
    public record DeletePayload(UUID messageId, DeleteScope scope) {}
    public record SeenPayload(UUID lastSeenMessageId) {}

    @MessageMapping("/conversations/{conversationId}/send")
    public void send(@DestinationVariable UUID conversationId,
                     @Payload SendPayload payload,
                     Principal principal) {
        User user = userOrReject(principal, "send");
        if (user == null) return;
        messages.send(conversationId, user.getId(), payload.body(), payload.replyToId(), payload.idempotencyKey());
    }

    @MessageMapping("/conversations/{conversationId}/edit")
    public void edit(@DestinationVariable UUID conversationId,
                     @Payload EditPayload payload,
                     Principal principal) {
        User user = userOrReject(principal, "edit");
        if (user == null) return;
        messages.edit(payload.messageId(), user.getId(), hasAuthority(principal, MOD_AUTHORITY), payload.body());
    }

    @MessageMapping("/conversations/{conversationId}/delete")
    public void delete(@DestinationVariable UUID conversationId,
                       @Payload DeletePayload payload,
                       Principal principal) {
        User user = userOrReject(principal, "delete");
        if (user == null) return;
        if (payload.scope() == DeleteScope.FOR_EVERYONE) {
            messages.deleteForEveryone(payload.messageId(), user.getId(), hasAuthority(principal, MOD_AUTHORITY));
        } else {
            deletions.deleteForMe(payload.messageId(), user.getId());
        }
    }

    @MessageMapping("/conversations/{conversationId}/seen")
    public void seen(@DestinationVariable UUID conversationId,
                     @Payload SeenPayload payload,
                     Principal principal) {
        User user = userOrReject(principal, "seen");
        if (user == null) return;
        receipts.markSeen(conversationId, user.getId(), payload.lastSeenMessageId());
    }

    private static User userOrReject(Principal principal, String action) {
        if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof User user)) {
            log.warn("STOMP {} rejected — unauthenticated", action);
            return null;
        }
        return user;
    }

    private static boolean hasAuthority(Principal principal, String authority) {
        if (!(principal instanceof Authentication auth) || auth.getAuthorities() == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}

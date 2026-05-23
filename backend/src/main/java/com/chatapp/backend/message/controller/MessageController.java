package com.chatapp.backend.message.controller;
import com.chatapp.backend.message.dto.MessageDto;
import com.chatapp.backend.message.dto.MessagePage;
import com.chatapp.backend.message.dto.PinDto;
import com.chatapp.backend.message.service.MessageHistoryService;
import com.chatapp.backend.message.service.MessagePinService;
import com.chatapp.backend.message.service.MessageReadReceiptService;
import com.chatapp.backend.message.service.MessageService;

import com.chatapp.backend.conversation.service.MembershipService;
import com.chatapp.backend.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations/{conversationId}")
public class MessageController {

    private final MessageService messages;
    private final MessageHistoryService history;
    private final MessageReadReceiptService receipts;
    private final MessagePinService pins;
    private final MembershipService memberships;

    @GetMapping("/messages")
    public MessagePage history(@PathVariable UUID conversationId,
                               @RequestParam(required = false) String cursor,
                               @RequestParam(defaultValue = "50") int limit,
                               @RequestParam(defaultValue = "BEFORE") MessageHistoryService.Direction direction,
                               @AuthenticationPrincipal User current) {
        if (!memberships.canRead(conversationId, current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        return history.history(conversationId, current.getId(), cursor, limit, direction);
    }

    @PostMapping("/messages")
    public MessageDto send(@PathVariable UUID conversationId,
                           @RequestBody SendMessageRequest req,
                           @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                           @AuthenticationPrincipal User current) {
        return messages.send(conversationId, current.getId(), req.body(), req.replyToId(), idempotencyKey);
    }

    /** Mark conversation as read up to a specific message. Broadcasts message.seen. */
    @PostMapping("/read")
    public void markRead(@PathVariable UUID conversationId,
                         @RequestBody MarkReadRequest req,
                         @AuthenticationPrincipal User current) {
        receipts.markSeen(conversationId, current.getId(), req.lastReadMessageId());
    }

    @GetMapping("/pins")
    public List<PinDto> listPins(@PathVariable UUID conversationId,
                                 @AuthenticationPrincipal User current) {
        return pins.listFor(conversationId, current.getId());
    }

    public record SendMessageRequest(@NotBlank String body, UUID replyToId) {}
    public record MarkReadRequest(UUID lastReadMessageId) {}
}

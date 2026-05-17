package com.chatapp.backend.message.controller;
import com.chatapp.backend.message.dto.MessageDto;
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
    private final MembershipService memberships;

    @GetMapping("/messages")
    public List<MessageDto> history(@PathVariable UUID conversationId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "50") int size,
                                    @AuthenticationPrincipal User current) {
        if (!memberships.canRead(conversationId, current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        return messages.history(conversationId, page, Math.min(size, 200));
    }

    @PostMapping("/messages")
    public MessageDto send(@PathVariable UUID conversationId,
                           @RequestBody SendMessageRequest req,
                           @AuthenticationPrincipal User current) {
        return messages.send(conversationId, current.getId(), req.body(), req.replyToId());
    }

    @PostMapping("/read")
    public void markRead(@PathVariable UUID conversationId,
                         @RequestBody MarkReadRequest req,
                         @AuthenticationPrincipal User current) {
        memberships.markRead(conversationId, current.getId(), req.lastReadMessageId());
    }

    public record SendMessageRequest(@NotBlank String body, UUID replyToId) {}
    public record MarkReadRequest(UUID lastReadMessageId) {}
}

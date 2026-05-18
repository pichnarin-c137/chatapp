package com.chatapp.backend.message.controller;

import com.chatapp.backend.message.dto.DeleteScope;
import com.chatapp.backend.message.dto.EditHistoryDto;
import com.chatapp.backend.message.dto.MessageDto;
import com.chatapp.backend.message.dto.PinDto;
import com.chatapp.backend.message.dto.ReadReceiptDto;
import com.chatapp.backend.message.repository.MessageEditRepository;
import com.chatapp.backend.message.service.MessageDeletionService;
import com.chatapp.backend.message.service.MessageForwardService;
import com.chatapp.backend.message.service.MessagePinService;
import com.chatapp.backend.message.service.MessageReadReceiptService;
import com.chatapp.backend.message.service.MessageService;
import com.chatapp.backend.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Message-scoped actions that don't carry a conversationId in the URL.
 * Edit / delete / edit-history / read-receipts / pin / forward.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageActionsController {

    private static final String MOD_AUTHORITY = "MESSAGE_DELETE";
    private static final String PIN_AUTHORITY = "MESSAGE_PIN";

    private final MessageService messages;
    private final MessageDeletionService deletions;
    private final MessageReadReceiptService receipts;
    private final MessagePinService pins;
    private final MessageForwardService forwards;
    private final MessageEditRepository edits;

    @PatchMapping("/{id}")
    public MessageDto edit(@PathVariable UUID id,
                           @RequestBody EditMessageRequest req,
                           @AuthenticationPrincipal User current,
                           Authentication auth) {
        return messages.edit(id, current.getId(), hasAuthority(auth, MOD_AUTHORITY), req.body());
    }

    @DeleteMapping("/{id}")
    public MessageDto delete(@PathVariable UUID id,
                             @RequestParam(defaultValue = "FOR_ME") DeleteScope scope,
                             @AuthenticationPrincipal User current,
                             Authentication auth) {
        if (scope == DeleteScope.FOR_EVERYONE) {
            return messages.deleteForEveryone(id, current.getId(), hasAuthority(auth, MOD_AUTHORITY));
        }
        deletions.deleteForMe(id, current.getId());
        return null;
    }

    @GetMapping("/{id}/edits")
    public List<EditHistoryDto> editHistory(@PathVariable UUID id) {
        return edits.findByMessageIdOrderByEditedAtDesc(id).stream()
                .map(EditHistoryDto::from)
                .toList();
    }

    @GetMapping("/{id}/reads")
    public List<ReadReceiptDto> reads(@PathVariable UUID id) {
        return receipts.readsOf(id);
    }

    @PostMapping("/{id}/pin")
    public PinDto pin(@PathVariable UUID id,
                      @AuthenticationPrincipal User current,
                      Authentication auth) {
        return pins.pin(id, current.getId(), hasAuthority(auth, PIN_AUTHORITY));
    }

    @DeleteMapping("/{id}/pin")
    public void unpin(@PathVariable UUID id,
                      @AuthenticationPrincipal User current,
                      Authentication auth) {
        pins.unpin(id, current.getId(), hasAuthority(auth, PIN_AUTHORITY));
    }

    @PostMapping("/{id}/forward")
    public List<MessageDto> forward(@PathVariable UUID id,
                                    @RequestBody ForwardRequest req,
                                    @AuthenticationPrincipal User current) {
        return forwards.forward(id, current.getId(), req.targetConversationIds());
    }

    private static boolean hasAuthority(Authentication auth, String authority) {
        return auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }

    public record EditMessageRequest(@NotBlank String body) {}
    public record ForwardRequest(@NotEmpty List<UUID> targetConversationIds) {}
}

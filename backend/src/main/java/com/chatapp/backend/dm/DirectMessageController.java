package com.chatapp.backend.dm;

import com.chatapp.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dm")
public class DirectMessageController {

    private final DirectMessageService service;

    @GetMapping
    public List<DirectConversationDto> list(@AuthenticationPrincipal User current) {
        return service.listForUser(current.getId());
    }

    @PostMapping("/with/{userId}")
    public DirectConversationDto getOrCreate(@PathVariable UUID userId,
                                              @AuthenticationPrincipal User current) {
        DirectConversation c = service.getOrCreateWith(current, userId);
        return service.loadForUser(c.getId(), current.getId());
    }

    @GetMapping("/{conversationId}")
    public DirectConversationDto get(@PathVariable UUID conversationId,
                                      @AuthenticationPrincipal User current) {
        return service.loadForUser(conversationId, current.getId());
    }

    @GetMapping("/{conversationId}/messages")
    public List<DirectMessageDto> messages(@PathVariable UUID conversationId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "50") int size,
                                            @AuthenticationPrincipal User current) {
        return service.history(conversationId, current.getId(), page, size);
    }
}

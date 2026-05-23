package com.chatapp.backend.message.controller;

import com.chatapp.backend.message.dto.ReactionDto;
import com.chatapp.backend.message.service.MessageReactionService;
import com.chatapp.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Emoji reactions on messages. The {emoji} path variable is URL-encoded
 * by the client so multi-byte unicode (👍 etc.) survives the URL.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageReactionController {

    private final MessageReactionService reactions;

    @PutMapping("/{id}/reactions/{emoji}")
    public void add(@PathVariable UUID id,
                    @PathVariable String emoji,
                    @AuthenticationPrincipal User current) {
        reactions.add(id, current.getId(), decode(emoji));
    }

    @DeleteMapping("/{id}/reactions/{emoji}")
    public void remove(@PathVariable UUID id,
                       @PathVariable String emoji,
                       @AuthenticationPrincipal User current) {
        reactions.remove(id, current.getId(), decode(emoji));
    }

    @GetMapping("/{id}/reactions")
    public List<ReactionDto> list(@PathVariable UUID id,
                                  @AuthenticationPrincipal User current) {
        return reactions.listForMessage(id, current.getId());
    }

    private static String decode(String raw) {
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }
}

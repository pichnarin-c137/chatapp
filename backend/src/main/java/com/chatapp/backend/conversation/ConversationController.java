package com.chatapp.backend.conversation;

import com.chatapp.backend.user.User;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService service;

    @GetMapping
    public List<ConversationDto> listMine(@AuthenticationPrincipal User current) {
        var fromMemberships = service.listForUser(current.getId()).stream()
                .map(m -> ConversationDto.from(m.getConversation()));
        var fromChannels = service.listPublicChannels().stream()
                .map(ConversationDto::from);
        var seen = new java.util.HashSet<UUID>();
        return java.util.stream.Stream.concat(fromMemberships, fromChannels)
                .filter(c -> seen.add(c.id()))
                .toList();
    }

    @GetMapping("/{id}")
    public ConversationDto get(@PathVariable UUID id) {
        return ConversationDto.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<ConversationDto> createGroup(@RequestBody CreateGroupRequest req,
                                                       @AuthenticationPrincipal User current) {
        Conversation c = service.createGroup(req.name(), req.topic(), current.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ConversationDto.from(c));
    }

    @PostMapping("/direct/{userId}")
    public ConversationDto getOrCreateDirect(@PathVariable UUID userId,
                                              @AuthenticationPrincipal User current) {
        return ConversationDto.from(service.getOrCreateDirect(current.getId(), userId));
    }

    public record CreateGroupRequest(@NotBlank String name, String topic) {}
}

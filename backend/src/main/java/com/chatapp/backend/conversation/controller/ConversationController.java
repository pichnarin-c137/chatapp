package com.chatapp.backend.conversation.controller;
import com.chatapp.backend.conversation.dto.ConversationDto;
import com.chatapp.backend.conversation.entity.Conversation;
import com.chatapp.backend.conversation.service.ConversationService;
import com.chatapp.backend.user.entity.User;

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
        return service.listVisibleAsDtos(current.getId());
    }

    @GetMapping("/{id}")
    public ConversationDto get(@PathVariable UUID id, @AuthenticationPrincipal User current) {
        return service.getView(id, current.getId());
    }

    @PostMapping
    public ResponseEntity<ConversationDto> createGroup(@RequestBody CreateGroupRequest req,
                                                       @AuthenticationPrincipal User current) {
        Conversation c = service.createGroup(req.name(), req.topic(), current.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ConversationDto.from(c, service.findDmOther(c.getId(), current.getId())));
    }

    @PostMapping("/direct/{userId}")
    public ConversationDto getOrCreateDirect(@PathVariable UUID userId,
                                              @AuthenticationPrincipal User current) {
        Conversation c = service.getOrCreateDirect(current.getId(), userId);
        return ConversationDto.from(c, service.findDmOther(c.getId(), current.getId()));
    }

    public record CreateGroupRequest(@NotBlank String name, String topic) {}
}

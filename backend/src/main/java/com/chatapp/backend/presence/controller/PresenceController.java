package com.chatapp.backend.presence.controller;

import com.chatapp.backend.presence.dto.PresenceDto;
import com.chatapp.backend.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presence;

    /** Batch lookup. Pass comma-separated UUIDs as `?userIds=a,b,c`. */
    @GetMapping
    public List<PresenceDto> get(@RequestParam("userIds") List<UUID> userIds) {
        var map = presence.statusOf(userIds);
        return userIds.stream().map(map::get).toList();
    }
}

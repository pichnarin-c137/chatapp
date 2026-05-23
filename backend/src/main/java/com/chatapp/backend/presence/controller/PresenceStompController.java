package com.chatapp.backend.presence.controller;

import com.chatapp.backend.presence.service.PresenceService;
import com.chatapp.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PresenceStompController {

    private final PresenceService presence;

    /** Client pings every ~30s while connected to refresh the ONLINE TTL. */
    @MessageMapping("/presence/heartbeat")
    public void heartbeat(Principal principal) {
        if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof User user)) {
            log.warn("STOMP presence heartbeat rejected — unauthenticated");
            return;
        }
        presence.heartbeat(user.getId());
    }
}

package com.chatapp.backend.presence.listener;

import com.chatapp.backend.presence.service.PresenceService;
import com.chatapp.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Bridges Spring WebSocket session lifecycle into the presence service so
 * the user's online state flips automatically on connect/disconnect with
 * no client-side cooperation required.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionListener {

    private final PresenceService presence;

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        User user = userOf(event.getUser());
        if (user == null) return;
        presence.markOnline(user.getId());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        User user = userOf(event.getUser());
        if (user == null) return;
        presence.markOffline(user.getId());
    }

    private static User userOf(Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}

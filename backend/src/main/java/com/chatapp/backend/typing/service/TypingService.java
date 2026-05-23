package com.chatapp.backend.typing.service;

import com.chatapp.backend.conversation.service.MembershipService;
import com.chatapp.backend.typing.event.TypingChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Ephemeral typing state. Backed entirely by Redis — typing keys auto-expire
 * after 5s so a disconnected client never leaves stale indicators. The client
 * is expected to refresh START every 4s while still typing.
 */
@Service
@RequiredArgsConstructor
public class TypingService {

    static final Duration TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redis;
    private final MembershipService memberships;
    private final ApplicationEventPublisher events;

    public void start(UUID conversationId, UUID userId, String username) {
        requireMember(conversationId, userId);
        redis.opsForValue().set(key(conversationId, userId), username, TTL);
        events.publishEvent(new TypingChangedEvent(
                conversationId, userId, username,
                TypingChangedEvent.Action.START,
                Instant.now().plus(TTL)));
    }

    public void stop(UUID conversationId, UUID userId, String username) {
        requireMember(conversationId, userId);
        redis.delete(key(conversationId, userId));
        events.publishEvent(new TypingChangedEvent(
                conversationId, userId, username,
                TypingChangedEvent.Action.STOP,
                null));
    }

    private void requireMember(UUID conversationId, UUID userId) {
        if (!memberships.canRead(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this conversation");
        }
    }

    private static String key(UUID conversationId, UUID userId) {
        return "typing:" + conversationId + ":" + userId;
    }
}

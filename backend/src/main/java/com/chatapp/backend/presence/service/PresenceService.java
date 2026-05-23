package com.chatapp.backend.presence.service;

import com.chatapp.backend.presence.dto.PresenceDto;
import com.chatapp.backend.presence.event.PresenceChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Online/offline presence, backed entirely by Redis. While a user is
 * connected we hold an ONLINE key with a 60s TTL refreshed by client
 * heartbeats every 30s. On disconnect we flip the key to OFFLINE with a
 * 5-minute TTL so "last seen" briefly stays visible.
 *
 * Value format in Redis: "{status}|{epochMillis}" — packed into a single
 * STRING so MGET can hydrate a batch in one round trip.
 */
@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final Duration ONLINE_TTL = Duration.ofSeconds(60);
    private static final Duration OFFLINE_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;
    private final ApplicationEventPublisher events;

    public PresenceDto markOnline(UUID userId) {
        Instant now = Instant.now();
        PresenceDto dto = new PresenceDto(userId, PresenceDto.Status.ONLINE, now);
        redis.opsForValue().set(key(userId), encode(dto), ONLINE_TTL);
        events.publishEvent(new PresenceChangedEvent(userId, dto));
        return dto;
    }

    /**
     * Refresh the ONLINE TTL without re-publishing an event — the user was
     * already online, the dot is already green.
     */
    public void heartbeat(UUID userId) {
        Instant now = Instant.now();
        PresenceDto dto = new PresenceDto(userId, PresenceDto.Status.ONLINE, now);
        redis.opsForValue().set(key(userId), encode(dto), ONLINE_TTL);
    }

    public PresenceDto markOffline(UUID userId) {
        Instant now = Instant.now();
        PresenceDto dto = new PresenceDto(userId, PresenceDto.Status.OFFLINE, now);
        redis.opsForValue().set(key(userId), encode(dto), OFFLINE_TTL);
        events.publishEvent(new PresenceChangedEvent(userId, dto));
        return dto;
    }

    public Map<UUID, PresenceDto> statusOf(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        List<UUID> ordered = new ArrayList<>(userIds);
        List<String> keys = ordered.stream().map(PresenceService::key).toList();
        List<String> raw = redis.opsForValue().multiGet(keys);
        Map<UUID, PresenceDto> out = new HashMap<>(ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            UUID id = ordered.get(i);
            String value = raw == null ? null : raw.get(i);
            out.put(id, decodeOrDefault(id, value));
        }
        return out;
    }

    public PresenceDto statusOf(UUID userId) {
        String value = redis.opsForValue().get(key(userId));
        return decodeOrDefault(userId, value);
    }

    private static String key(UUID userId) {
        return "presence:" + userId;
    }

    private static String encode(PresenceDto dto) {
        return dto.status().name() + "|" + dto.lastSeenAt().toEpochMilli();
    }

    private static PresenceDto decodeOrDefault(UUID userId, String value) {
        if (value == null || value.isBlank()) {
            return new PresenceDto(userId, PresenceDto.Status.OFFLINE, null);
        }
        int sep = value.indexOf('|');
        if (sep <= 0) {
            return new PresenceDto(userId, PresenceDto.Status.OFFLINE, null);
        }
        try {
            PresenceDto.Status status = PresenceDto.Status.valueOf(value.substring(0, sep));
            long millis = Long.parseLong(value.substring(sep + 1));
            return new PresenceDto(userId, status, Instant.ofEpochMilli(millis));
        } catch (Exception e) {
            return new PresenceDto(userId, PresenceDto.Status.OFFLINE, null);
        }
    }
}

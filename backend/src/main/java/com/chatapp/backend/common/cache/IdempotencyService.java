package com.chatapp.backend.common.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Idempotency for write paths. Clients send a UUID Idempotency-Key with each
 * send/edit/etc.; the first successful execution stores (key -> resultId) so
 * a retry of the same key returns the original result instead of duplicating.
 *
 * Keyed by user so two clients can't collide. TTL is short — replays after
 * a long delay are treated as new requests.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redis;

    /** Reserve the key. Returns Optional.empty() on success (caller proceeds);
     *  returns the previously stored result if the key was already claimed. */
    public Optional<String> reserve(UUID userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();
        String k = key(userId, idempotencyKey);
        Boolean acquired = redis.opsForValue().setIfAbsent(k, "PENDING", TTL);
        if (Boolean.TRUE.equals(acquired)) return Optional.empty();
        return Optional.ofNullable(redis.opsForValue().get(k));
    }

    /** Store the result for a reserved key so future replays return it. */
    public void store(UUID userId, String idempotencyKey, String resultId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        redis.opsForValue().set(key(userId, idempotencyKey), resultId, TTL);
    }

    /** Release a key (e.g. when the operation failed and we want to allow retry). */
    public void release(UUID userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        redis.delete(key(userId, idempotencyKey));
    }

    private static String key(UUID userId, String idempotencyKey) {
        return "idempotency:" + userId + ":" + idempotencyKey;
    }
}

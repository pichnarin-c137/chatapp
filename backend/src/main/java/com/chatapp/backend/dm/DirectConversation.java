package com.chatapp.backend.dm;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "direct_conversations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dm_pair", columnNames = {"user_low", "user_high"})
}, indexes = {
        @Index(name = "idx_dm_user_low", columnList = "user_low"),
        @Index(name = "idx_dm_user_high", columnList = "user_high"),
        @Index(name = "idx_dm_last_message", columnList = "last_message_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectConversation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_low", nullable = false)
    private UUID userLow;

    @Column(name = "user_high", nullable = false)
    private UUID userHigh;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public static UUID low(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static UUID high(UUID a, UUID b) {
        return a.compareTo(b) <= 0 ? b : a;
    }

    public boolean includes(UUID userId) {
        return userLow.equals(userId) || userHigh.equals(userId);
    }

    public UUID peerOf(UUID userId) {
        return userLow.equals(userId) ? userHigh : userLow;
    }
}

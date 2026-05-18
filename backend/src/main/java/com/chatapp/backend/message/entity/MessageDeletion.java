package com.chatapp.backend.message.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Per-user "delete for me". Row presence hides the message from this user
 * while leaving it visible to everyone else. Distinct from delete-for-everyone,
 * which sets messages.deleted_at and is broadcast as a tombstone.
 */
@Entity
@Table(name = "message_deletions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDeletion {

    @EmbeddedId
    private MessageDeletionId id;

    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;

    @PrePersist
    void onCreate() {
        if (deletedAt == null) deletedAt = Instant.now();
    }
}

package com.chatapp.backend.message.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pinned_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinnedMessage {

    @EmbeddedId
    private PinnedMessageId id;

    @Column(name = "pinned_by", nullable = false)
    private UUID pinnedBy;

    @Column(name = "pinned_at", nullable = false)
    private Instant pinnedAt;

    @PrePersist
    void onCreate() {
        if (pinnedAt == null) pinnedAt = Instant.now();
    }
}

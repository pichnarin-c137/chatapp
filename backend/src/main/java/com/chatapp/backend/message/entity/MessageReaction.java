package com.chatapp.backend.message.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "message_reactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReaction {

    @EmbeddedId
    private MessageReactionId id;

    @Column(name = "reacted_at", nullable = false)
    private Instant reactedAt;

    @PrePersist
    void onCreate() {
        if (reactedAt == null) reactedAt = Instant.now();
    }
}

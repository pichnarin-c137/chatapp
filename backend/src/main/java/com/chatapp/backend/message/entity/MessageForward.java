package com.chatapp.backend.message.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Provenance for a forwarded message. The forwarded copy is a new row in
 * `messages` with its own id; this table points back to the original so the UI
 * can render "Forwarded from @kira" without ambiguity.
 */
@Entity
@Table(name = "message_forwards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageForward {

    @Id
    @Column(name = "forwarded_message_id")
    private UUID forwardedMessageId;

    @Column(name = "original_message_id", nullable = false)
    private UUID originalMessageId;

    @Column(name = "original_sender_id", nullable = false)
    private UUID originalSenderId;

    @Column(name = "original_conversation_id", nullable = false)
    private UUID originalConversationId;

    @Column(name = "forwarded_at", nullable = false)
    private Instant forwardedAt;

    @PrePersist
    void onCreate() {
        if (forwardedAt == null) forwardedAt = Instant.now();
    }
}

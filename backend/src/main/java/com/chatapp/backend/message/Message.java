package com.chatapp.backend.message;

import com.chatapp.backend.common.audit.BaseSoftDeletableEntity;
import com.chatapp.backend.conversation.Conversation;
import com.chatapp.backend.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends BaseSoftDeletableEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /** Null for SYSTEM messages (e.g. "Alice joined"). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    @Column(length = 4000)
    private String body;

    @Column(name = "reply_to_id")
    private UUID replyToId;

    @Column(name = "thread_root_id")
    private UUID threadRootId;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @PrePersist
    void onCreate() {
        if (sentAt == null) sentAt = Instant.now();
    }
}

package com.chatapp.backend.conversation;

import com.chatapp.backend.common.audit.BaseSoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation extends BaseSoftDeletableEntity {

    /** Deterministic id for the default Lobby channel. */
    public static final UUID LOBBY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String LOBBY_NAME = "Lobby";

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private ConversationType type = ConversationType.GROUP;

    @Column(length = 64)
    private String name;

    @Column(length = 255)
    private String topic;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Denormalised "newest activity" timestamp; updated on every message send. */
    @Column(name = "last_message_at")
    private Instant lastMessageAt;
}

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
@Table(name = "message_edits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEdit {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "editor_id", nullable = false)
    private UUID editorId;

    @Column(name = "previous_body", nullable = false, length = 4000)
    private String previousBody;

    @Column(name = "edited_at", nullable = false)
    private Instant editedAt;

    @PrePersist
    void onCreate() {
        if (editedAt == null) editedAt = Instant.now();
    }
}

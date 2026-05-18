package com.chatapp.backend.message.dto;

import com.chatapp.backend.message.entity.MessageEdit;

import java.time.Instant;
import java.util.UUID;

public record EditHistoryDto(
        UUID id,
        UUID messageId,
        UUID editorId,
        String previousBody,
        Instant editedAt
) {
    public static EditHistoryDto from(MessageEdit e) {
        return new EditHistoryDto(e.getId(), e.getMessageId(), e.getEditorId(), e.getPreviousBody(), e.getEditedAt());
    }
}

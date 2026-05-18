package com.chatapp.backend.message.dto;

import com.chatapp.backend.message.entity.MessageRead;

import java.time.Instant;
import java.util.UUID;

public record ReadReceiptDto(UUID userId, Instant readAt) {
    public static ReadReceiptDto from(MessageRead r) {
        return new ReadReceiptDto(r.getId().getUserId(), r.getReadAt());
    }
}

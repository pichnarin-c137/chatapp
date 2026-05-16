package com.chatapp.backend.user;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String email,
        String avatarUrl,
        String timezone,
        Instant createdAt
) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getUsername(), u.getEmail(),
                u.getAvatarUrl(), u.getTimezone(), u.getCreatedAt());
    }
}

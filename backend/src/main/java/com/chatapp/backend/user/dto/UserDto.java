package com.chatapp.backend.user.dto;
import com.chatapp.backend.user.entity.UserStatus;
import com.chatapp.backend.user.entity.UserProfile;
import com.chatapp.backend.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String email,
        UserStatus status,
        String avatarUrl,
        String timezone,
        Instant createdAt
) {
    public static UserDto from(User u, UserProfile profile) {
        return new UserDto(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getStatus(),
                profile != null ? profile.getAvatarUrl() : null,
                profile != null ? profile.getTimezone() : null,
                u.getCreatedAt()
        );
    }

    public static UserDto from(User u) {
        return from(u, null);
    }
}

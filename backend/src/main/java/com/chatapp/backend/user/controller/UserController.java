package com.chatapp.backend.user.controller;
import com.chatapp.backend.user.dto.UserDto;
import com.chatapp.backend.user.repository.UserProfileRepository;
import com.chatapp.backend.user.repository.UserRepository;
import com.chatapp.backend.user.entity.UserProfile;
import com.chatapp.backend.user.entity.User;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;
    private final UserProfileRepository profiles;

    @GetMapping("/search")
    public List<UserDto> search(@RequestParam("q") String q,
                                @AuthenticationPrincipal User current) {
        String trimmed = q == null ? "" : q.trim();
        if (trimmed.startsWith("@")) trimmed = trimmed.substring(1);
        if (trimmed.isEmpty()) return List.of();
        return users.searchByUsernamePrefix(trimmed, current.getId(), PageRequest.of(0, 10))
                .stream()
                .map(UserDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable UUID id) {
        User u = users.findById(id)
                .filter(usr -> usr.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserProfile profile = profiles.findById(id).orElse(null);
        return UserDto.from(u, profile);
    }
}

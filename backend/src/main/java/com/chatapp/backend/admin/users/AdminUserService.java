package com.chatapp.backend.admin.users;

import com.chatapp.backend.user.Role;
import com.chatapp.backend.user.User;
import com.chatapp.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int PAGE_SIZE = 20;

    private final UserRepository users;

    @Transactional(readOnly = true)
    public Page<User> list(String q, int page) {
        return users.adminSearch(q, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    @Transactional
    public User toggleRole(UUID id, String currentAdminUsername) {
        User u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getUsername().equals(currentAdminUsername) && u.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot demote yourself");
        }
        u.setRole(u.getRole() == Role.ADMIN ? Role.USER : Role.ADMIN);
        return users.save(u);
    }

    @Transactional
    public void delete(UUID id, String currentAdminUsername) {
        User u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getUsername().equals(currentAdminUsername)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete yourself");
        }
        users.delete(u);
    }
}

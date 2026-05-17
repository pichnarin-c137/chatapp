package com.chatapp.backend.admin.users;

import com.chatapp.backend.common.audit.CustomUserDetails;
import com.chatapp.backend.user.User;
import com.chatapp.backend.user.UserRepository;
import com.chatapp.backend.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public User toggleStatus(UUID id, Authentication current) {
        User u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UUID currentId = currentUserId(current);
        if (currentId != null && currentId.equals(u.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change your own status");
        }
        u.setStatus(u.getStatus() == UserStatus.ACTIVE ? UserStatus.DISABLED : UserStatus.ACTIVE);
        return users.save(u);
    }

    @Transactional
    public void softDelete(UUID id, Authentication current) {
        User u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UUID currentId = currentUserId(current);
        if (currentId != null && currentId.equals(u.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete yourself");
        }
        u.markDeleted(currentId);
        users.save(u);
    }

    private UUID currentUserId(Authentication auth) {
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof User u) return u.getId();
        if (p instanceof CustomUserDetails cud) return cud.getId();
        return null;
    }
}

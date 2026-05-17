package com.chatapp.backend.admin.roles;

import com.chatapp.backend.common.audit.CustomUserDetails;
import com.chatapp.backend.rbac.*;
import com.chatapp.backend.user.User;
import com.chatapp.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final RoleRepository roles;
    private final RolePermissionRepository rolePerms;
    private final UserRoleRepository userRoles;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<Role> listRoles() {
        return roles.findAll();
    }

    @Transactional(readOnly = true)
    public List<RolePermission> permissionsOf(UUID roleId) {
        return rolePerms.findByRoleId(roleId);
    }

    @Transactional(readOnly = true)
    public List<UserRole> assignmentsFor(UUID userId) {
        return userRoles.findByUserId(userId);
    }

    @Transactional
    public void assign(UUID userId, UUID roleId, Authentication actor) {
        UserRoleId id = new UserRoleId(userId, roleId);
        if (userRoles.existsById(id)) return;  // already assigned
        User u = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Role r = roles.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        userRoles.save(UserRole.builder()
                .id(id)
                .user(u)
                .role(r)
                .assignedAt(Instant.now())
                .assignedBy(currentUserId(actor))
                .build());
    }

    @Transactional
    public void revoke(UUID userId, UUID roleId, Authentication actor) {
        UserRoleId id = new UserRoleId(userId, roleId);
        userRoles.findById(id).ifPresent(existing -> {
            // Don't let an admin revoke their own SUPER_ADMIN — at least one super-admin must remain.
            UUID currentId = currentUserId(actor);
            if (currentId != null && currentId.equals(userId)
                    && Role.CODE_SUPER_ADMIN.equals(existing.getRole().getCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot revoke your own SUPER_ADMIN role");
            }
            userRoles.delete(existing);
        });
    }

    private UUID currentUserId(Authentication auth) {
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof User u) return u.getId();
        if (p instanceof CustomUserDetails cud) return cud.getId();
        return null;
    }
}

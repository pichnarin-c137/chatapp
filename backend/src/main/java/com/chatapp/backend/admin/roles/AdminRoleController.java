package com.chatapp.backend.admin.roles;

import com.chatapp.backend.rbac.Role;
import com.chatapp.backend.rbac.RolePermission;
import com.chatapp.backend.rbac.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/roles")
public class AdminRoleController {

    private final AdminRoleService service;

    @GetMapping
    public String list(Model model) {
        List<Role> roles = service.listRoles();
        model.addAttribute("pageTitle", "Roles & permissions");
        model.addAttribute("activeNav", "roles");
        model.addAttribute("roles", roles);
        return "admin/roles/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        Role role = service.listRoles().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow();
        List<RolePermission> perms = service.permissionsOf(id);
        model.addAttribute("pageTitle", "Role: " + role.getName());
        model.addAttribute("activeNav", "roles");
        model.addAttribute("role", role);
        model.addAttribute("permissions", perms);
        return "admin/roles/detail";
    }

    /** HTMX: returns the assignments fragment for one user. */
    @GetMapping("/user/{userId}")
    public String userAssignments(@PathVariable UUID userId, Model model) {
        populateAssignments(model, userId);
        return "admin/roles/user_assignments";
    }

    @PostMapping("/user/{userId}/assign")
    public String assign(@PathVariable UUID userId,
                         @RequestParam UUID roleId,
                         Authentication auth,
                         Model model) {
        service.assign(userId, roleId, auth);
        populateAssignments(model, userId);
        return "admin/roles/user_assignments";
    }

    @PostMapping("/user/{userId}/revoke/{roleId}")
    public String revoke(@PathVariable UUID userId,
                         @PathVariable UUID roleId,
                         Authentication auth,
                         Model model) {
        service.revoke(userId, roleId, auth);
        populateAssignments(model, userId);
        return "admin/roles/user_assignments";
    }

    private void populateAssignments(Model model, UUID userId) {
        List<UserRole> assignments = service.assignmentsFor(userId);
        model.addAttribute("userId", userId);
        model.addAttribute("allRoles", service.listRoles());
        model.addAttribute("assignments", assignments);
    }
}

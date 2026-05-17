package com.chatapp.backend.admin.users;

import com.chatapp.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService service;

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {
        populate(model, q, page);
        model.addAttribute("pageTitle", "Users");
        model.addAttribute("activeNav", "users");
        return "admin/users/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        User u = service.findById(id);
        model.addAttribute("pageTitle", "User: " + u.getUsername());
        model.addAttribute("activeNav", "users");
        model.addAttribute("u", u);
        return "admin/users/detail";
    }

    @GetMapping("/rows")
    public String rows(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {
        populate(model, q, page);
        return "admin/users/list :: tableBody";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable UUID id, Authentication auth, Model model) {
        User u = service.toggleStatus(id, auth);
        model.addAttribute("u", u);
        return "admin/users/_row :: userRow(u=${u})";
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public String softDelete(@PathVariable UUID id, Authentication auth) {
        service.softDelete(id, auth);
        return "";
    }

    private void populate(Model model, String q, int page) {
        Page<User> users = service.list(q, page);
        model.addAttribute("users", users.getContent());
        model.addAttribute("page", page);
        model.addAttribute("totalPages", users.getTotalPages());
        model.addAttribute("q", q == null ? "" : q);
    }
}

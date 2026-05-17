package com.chatapp.backend.admin.audit.controller;

import com.chatapp.backend.common.audit.entity.AuditLog;
import com.chatapp.backend.common.audit.repository.AuditLogRepository;
import com.chatapp.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/audit")
public class AdminAuditController {

    private static final int PAGE_SIZE = 50;

    private final AuditLogRepository audit;
    private final UserRepository users;

    @GetMapping
    public String list(@RequestParam(value = "page", defaultValue = "0") int page, Model model) {
        Page<AuditLog> rows = audit.findAll(PageRequest.of(
                Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "at")));

        // Resolve actor usernames in one batch to avoid N+1.
        Map<UUID, String> usernames = users.findAllById(
                rows.stream().map(AuditLog::getActorUserId).filter(java.util.Objects::nonNull).toList()
        ).stream().collect(Collectors.toMap(u -> u.getId(), u -> u.getUsername()));

        model.addAttribute("pageTitle", "Audit log");
        model.addAttribute("activeNav", "audit");
        model.addAttribute("rows", rows.getContent());
        model.addAttribute("usernames", usernames);
        model.addAttribute("page", page);
        model.addAttribute("totalPages", rows.getTotalPages());
        return "admin/audit/list";
    }
}

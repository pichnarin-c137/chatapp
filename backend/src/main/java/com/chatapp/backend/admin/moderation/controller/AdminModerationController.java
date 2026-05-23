package com.chatapp.backend.admin.moderation.controller;
import com.chatapp.backend.admin.moderation.service.AdminModerationService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/moderation")
public class AdminModerationController {

    private final AdminModerationService service;

    @GetMapping
    public String list(@RequestParam(value = "page", defaultValue = "0") int page, Model model) {
        Page<AdminModerationService.ReportRow> rows = service.listOpen(page);
        model.addAttribute("pageTitle", "Moderation");
        model.addAttribute("activeNav", "moderation");
        model.addAttribute("rows", rows.getContent());
        model.addAttribute("page", page);
        model.addAttribute("totalPages", rows.getTotalPages());
        return "admin/moderation/list";
    }

    @DeleteMapping("/messages/{id}")
    @ResponseBody
    public String deleteMessage(@PathVariable UUID id, Authentication auth) {
        service.deleteMessage(id, auth);
        return "";
    }

    @PostMapping("/reports/{id}/resolve")
    @ResponseBody
    public String resolve(@PathVariable UUID id, Authentication auth) {
        service.resolveReport(id, auth);
        return "";
    }
}

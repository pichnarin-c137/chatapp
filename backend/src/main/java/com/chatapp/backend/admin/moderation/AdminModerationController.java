package com.chatapp.backend.admin.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    public String deleteMessage(@PathVariable UUID id) {
        service.deleteMessage(id);
        return "";
    }

    @PostMapping("/reports/{id}/resolve")
    @ResponseBody
    public String resolve(@PathVariable UUID id) {
        service.resolveReport(id);
        return "";
    }
}

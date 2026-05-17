package com.chatapp.backend.admin.conversations.controller;
import com.chatapp.backend.admin.conversations.service.AdminConversationService;

import com.chatapp.backend.conversation.entity.Conversation;
import com.chatapp.backend.conversation.entity.ConversationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/conversations")
public class AdminConversationController {

    private final AdminConversationService service;

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {
        Page<Conversation> page0 = service.list(q, page);
        model.addAttribute("pageTitle", "Conversations");
        model.addAttribute("activeNav", "conversations");
        model.addAttribute("conversations", page0.getContent());
        model.addAttribute("page", page);
        model.addAttribute("totalPages", page0.getTotalPages());
        model.addAttribute("q", q == null ? "" : q);
        return "admin/conversations/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("pageTitle", "New conversation");
        model.addAttribute("activeNav", "conversations");
        model.addAttribute("conv", Conversation.builder().type(ConversationType.GROUP).build());
        model.addAttribute("formAction", "/admin/conversations");
        model.addAttribute("submitLabel", "Create");
        return "admin/conversations/form";
    }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String topic,
                         @RequestParam(defaultValue = "GROUP") ConversationType type) {
        service.create(name, topic, type);
        return "redirect:/admin/conversations";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("pageTitle", "Edit conversation");
        model.addAttribute("activeNav", "conversations");
        model.addAttribute("conv", service.get(id));
        model.addAttribute("formAction", "/admin/conversations/" + id);
        model.addAttribute("submitLabel", "Save changes");
        return "admin/conversations/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id,
                         @RequestParam String name,
                         @RequestParam(required = false) String topic,
                         @RequestParam(required = false) ConversationType type) {
        service.update(id, name, topic, type);
        return "redirect:/admin/conversations";
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public String softDelete(@PathVariable UUID id, Authentication auth) {
        service.softDelete(id, auth);
        return "";
    }
}

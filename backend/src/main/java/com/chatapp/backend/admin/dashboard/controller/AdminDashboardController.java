package com.chatapp.backend.admin.dashboard.controller;
import com.chatapp.backend.admin.dashboard.service.AdminDashboardService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService service;

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("stats", service.stats());
        return "admin/dashboard";
    }
}

package com.chatapp.backend.admin.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminAuthPageController {

    @GetMapping("/admin/login")
    public String login() {
        return "admin/login";
    }
}

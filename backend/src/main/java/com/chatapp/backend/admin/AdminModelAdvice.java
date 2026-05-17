package com.chatapp.backend.admin;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.chatapp.backend.admin")
public class AdminModelAdvice {

    @ModelAttribute("currentAdmin")
    public String currentAdmin(Authentication auth) {
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
    }
}

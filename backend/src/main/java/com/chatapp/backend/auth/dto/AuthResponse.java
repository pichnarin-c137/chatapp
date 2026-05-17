package com.chatapp.backend.auth.dto;

import com.chatapp.backend.user.dto.UserDto;

public record AuthResponse(String token, UserDto user) {}

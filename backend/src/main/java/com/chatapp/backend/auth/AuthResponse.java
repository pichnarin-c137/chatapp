package com.chatapp.backend.auth;

import com.chatapp.backend.user.UserDto;

public record AuthResponse(String token, UserDto user) {}

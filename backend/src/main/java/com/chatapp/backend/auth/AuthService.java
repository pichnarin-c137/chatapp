package com.chatapp.backend.auth;

import com.chatapp.backend.common.security.JwtService;
import com.chatapp.backend.user.User;
import com.chatapp.backend.user.UserDto;
import com.chatapp.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (userRepository.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        User user = User.builder()
                .username(req.username())
                .email(req.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .timezone(req.timezone() != null && !req.timezone().isBlank()
                        ? req.timezone() : "Asia/Phnom_Penh")
                .build();

        userRepository.save(user);
        String token = jwtService.generate(user.getId());
        return new AuthResponse(token, UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtService.generate(user.getId());
        return new AuthResponse(token, UserDto.from(user));
    }
}

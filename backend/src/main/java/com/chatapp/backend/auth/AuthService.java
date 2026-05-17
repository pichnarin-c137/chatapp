package com.chatapp.backend.auth;

import com.chatapp.backend.common.security.JwtService;
import com.chatapp.backend.rbac.Role;
import com.chatapp.backend.rbac.RoleRepository;
import com.chatapp.backend.rbac.UserRole;
import com.chatapp.backend.rbac.UserRoleId;
import com.chatapp.backend.rbac.UserRoleRepository;
import com.chatapp.backend.user.User;
import com.chatapp.backend.user.UserDto;
import com.chatapp.backend.user.UserProfile;
import com.chatapp.backend.user.UserProfileRepository;
import com.chatapp.backend.user.UserRepository;
import com.chatapp.backend.user.UserStatus;
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
    private final UserProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (userRepository.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        User user = User.builder()
                .username(req.username())
                .email(req.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .user(user)
                .timezone(req.timezone() != null && !req.timezone().isBlank()
                        ? req.timezone() : "Asia/Phnom_Penh")
                .build();
        profileRepository.save(profile);

        Role userRole = roleRepository.findByCode(Role.CODE_USER)
                .orElseThrow(() -> new IllegalStateException("USER role missing — V1 migration did not seed roles"));
        userRoleRepository.save(UserRole.builder()
                .id(new UserRoleId(user.getId(), userRole.getId()))
                .user(user)
                .role(userRole)
                .build());

        String token = jwtService.generate(user.getId());
        return new AuthResponse(token, UserDto.from(user, profile));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is " + user.getStatus().name().toLowerCase());
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        UserProfile profile = profileRepository.findById(user.getId()).orElse(null);
        String token = jwtService.generate(user.getId());
        return new AuthResponse(token, UserDto.from(user, profile));
    }
}

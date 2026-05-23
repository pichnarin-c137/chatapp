package com.chatapp.backend.admin.auth.service;

import com.chatapp.backend.common.security.CustomUserDetails;
import com.chatapp.backend.rbac.repository.UserRoleRepository;
import com.chatapp.backend.user.entity.User;
import com.chatapp.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final UserRepository users;
    private final UserRoleRepository userRoles;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User u = users.findByUsername(usernameOrEmail)
                .or(() -> users.findByEmail(usernameOrEmail.toLowerCase()))
                .orElseThrow(() -> new UsernameNotFoundException("No user found for " + usernameOrEmail));

        if (u.getDeletedAt() != null) {
            throw new UsernameNotFoundException("User has been deleted");
        }

        List<SimpleGrantedAuthority> authorities = userRoles
                .findPermissionCodesByUserId(u.getId()).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        if (authorities.isEmpty()) {
            throw new UsernameNotFoundException("User has no admin permissions");
        }

        return new CustomUserDetails(u, authorities);
    }
}

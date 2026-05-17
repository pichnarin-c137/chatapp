package com.chatapp.backend.admin.auth;

import com.chatapp.backend.user.Role;
import com.chatapp.backend.user.User;
import com.chatapp.backend.user.UserRepository;
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

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User u = users.findByUsername(usernameOrEmail)
                .or(() -> users.findByEmail(usernameOrEmail.toLowerCase()))
                .orElseThrow(() -> new UsernameNotFoundException("No admin found for " + usernameOrEmail));

        if (u.getRole() != Role.ADMIN) {
            throw new UsernameNotFoundException("User is not an admin");
        }

        return new org.springframework.security.core.userdetails.User(
                u.getUsername(),
                u.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}

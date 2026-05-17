package com.chatapp.backend.common.security;
import com.chatapp.backend.user.entity.UserStatus;

import com.chatapp.backend.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * UserDetails wrapper that exposes the underlying {@link User} entity, so the
 * admin form-login chain still has access to the user id (needed for auditing).
 */
@RequiredArgsConstructor
@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    public UUID getId() { return user.getId(); }

    @Override public String getPassword() { return user.getPasswordHash(); }
    @Override public String getUsername() { return user.getUsername(); }

    @Override public boolean isAccountNonExpired()     { return user.getDeletedAt() == null; }
    @Override public boolean isAccountNonLocked()       { return user.getStatus() != com.chatapp.backend.user.entity.UserStatus.BANNED; }
    @Override public boolean isCredentialsNonExpired()  { return true; }
    @Override public boolean isEnabled()                { return user.getStatus() == com.chatapp.backend.user.entity.UserStatus.ACTIVE
                                                              && user.getDeletedAt() == null; }
}

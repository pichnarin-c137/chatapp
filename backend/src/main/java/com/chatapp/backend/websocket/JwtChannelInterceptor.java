package com.chatapp.backend.websocket;

import com.chatapp.backend.common.security.JwtService;
import com.chatapp.backend.rbac.UserRoleRepository;
import com.chatapp.backend.user.User;
import com.chatapp.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String auth = accessor.getFirstNativeHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                throw new MessagingException("Missing Authorization header on STOMP CONNECT");
            }
            String token = auth.substring(7);
            UUID userId;
            try {
                userId = jwtService.parseUserId(token);
            } catch (Exception e) {
                throw new MessagingException("Invalid JWT", e);
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new MessagingException("Unknown user"));
            List<SimpleGrantedAuthority> authorities = userRoleRepository
                    .findPermissionCodesByUserId(user.getId()).stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
            accessor.setUser(authToken);
            log.debug("STOMP CONNECT authenticated for user {}", user.getUsername());
        }
        return message;
    }
}

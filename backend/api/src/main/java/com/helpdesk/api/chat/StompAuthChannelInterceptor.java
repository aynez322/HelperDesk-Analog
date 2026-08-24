package com.helpdesk.api.chat;

import com.helpdesk.api.security.AppUserDetailsService;
import com.helpdesk.api.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Authenticates STOMP CONNECT frames carrying {@code Authorization: Bearer <jwt>}.
 * Mirrors JwtAuthenticationFilter: token subject = email, authorities reloaded
 * from the DB via {@link AppUserDetailsService}. Anonymous connections (no header)
 * are allowed — a live chat can be started without an account, matching the
 * anonymous FORM-ticket flow.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public StompAuthChannelInterceptor(JwtService jwtService,
                                        AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String header = accessor.getFirstNativeHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            return message; // anonymous live-chat client
        }

        String token = header.substring(7);
        try {
            if (jwtService.isValid(token)) {
                String email = jwtService.extractUsername(token);
                UserDetails user = userDetailsService.loadUserByUsername(email);
                accessor.setUser(new UsernamePasswordAuthenticationToken(
                        user.getUsername(), null, user.getAuthorities()));
                log.debug("STOMP CONNECT authenticated as {}", email);
            }
        } catch (Exception ex) {
            log.warn("STOMP CONNECT rejected (invalid JWT): {}", ex.getMessage());
        }
        return message;
    }
}

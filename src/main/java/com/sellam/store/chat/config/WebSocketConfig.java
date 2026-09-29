package com.sellam.store.chat.config;

import com.sellam.store.auth.JwtProvider;
import com.sellam.store.common.security.AuthPrincipal;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Pattern CHAT_DESTINATION = Pattern.compile("^/(?:topic|app)/chat/([0-9a-fA-F\\-]{36})$");

    private final JwtProvider jwtProvider;
    private final ShopMembershipRepository shopMembershipRepository;

    public WebSocketConfig(JwtProvider jwtProvider, ShopMembershipRepository shopMembershipRepository) {
        this.jwtProvider = jwtProvider;
        this.shopMembershipRepository = shopMembershipRepository;
    }

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String header = accessor.getFirstNativeHeader("Authorization");
                    if (header == null || !header.startsWith("Bearer ")) {
                        throw new IllegalArgumentException("Authentification WebSocket requise");
                    }

                    String token = header.substring(7);
                    if (!jwtProvider.validateToken(token)) {
                        throw new IllegalArgumentException("Token WebSocket invalide");
                    }

                    AuthPrincipal principal = jwtProvider.getPrincipalFromToken(token);
                    accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
                }
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand())) {
                    requireChatShopAccess(accessor);
                }
                return message;
            }

            private void requireChatShopAccess(StompHeaderAccessor accessor) {
                if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken token)
                        || !(token.getPrincipal() instanceof AuthPrincipal principal)) {
                    throw new AccessDeniedException("Authentification WebSocket requise");
                }
                String destination = accessor.getDestination();
                Matcher matcher = destination == null ? null : CHAT_DESTINATION.matcher(destination);
                if (matcher == null || !matcher.matches()) {
                    throw new AccessDeniedException("Salon de discussion invalide");
                }
                UUID shopId = UUID.fromString(matcher.group(1));
                if (shopMembershipRepository.findActiveMembership(principal.getId(), shopId).isEmpty()) {
                    throw new AccessDeniedException("Accès refusé : vous n'avez pas accès à cette boutique");
                }
            }
        });
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Determine allowed origins based on frontend URL
        String[] allowedOrigins = new String[]{frontendUrl, "http://localhost:5173", "http://localhost:8080"};

        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS()
                .setHeartbeatTime(25000)
                .setSessionCookieNeeded(false);
    }
}

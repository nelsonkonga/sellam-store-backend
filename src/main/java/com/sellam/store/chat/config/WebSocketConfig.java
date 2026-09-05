package com.sellam.store.chat.config;

import com.sellam.store.auth.JwtProvider;
import com.sellam.store.common.security.AuthPrincipal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtProvider jwtProvider;

    public WebSocketConfig(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
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
                return message;
            }
        });
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Determine allowed origins based on frontend URL
        String[] allowedOrigins;
        if (frontendUrl.contains("localhost")) {
            allowedOrigins = new String[]{"http://localhost:5173", "http://localhost:8080", "*"};
        } else {
            // Production: allow frontend domain and localhost for dev
            allowedOrigins = new String[]{frontendUrl, "http://localhost:5173", "http://localhost:8080"};
        }
        
        registry.addEndpoint("/ws-chat")
                .setAllowedOrigins(allowedOrigins)
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(25000)
                .setSessionCookieNeeded(false);
    }
}

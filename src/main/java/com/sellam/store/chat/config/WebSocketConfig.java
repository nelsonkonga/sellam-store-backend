package com.sellam.store.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
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

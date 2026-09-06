package com.sellam.store.common.config;

import com.sellam.store.auth.OAuth2SuccessHandler;
import com.sellam.store.common.security.JwtAuthFilter;
import com.sellam.store.subscriptions.repositories.SubscriptionRepository;
import com.sellam.store.subscriptions.security.SubscriptionAccessFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity

public class SecurityConfig
{
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    // Injectez JwtAuthFilter en tant que ObjectProvider (lazy evaluation) pour éviter la dépendance circulaire :
    // JwtAuthFilter -> PersonRepository -> SecurityConfig -> AuthenticationManager -> [redélégation circulaire]
    // Solution : defer JwtAuthFilter instantiation jusqu'après la configuration de sécurité
    private final ObjectProvider<JwtAuthFilter> jwtAuthFilterProvider;
    private final OAuth2SuccessHandler OAuth2SuccessHandler;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.cors.allowed-origins:http://localhost:5173,https://sellam-store.vercel.app}")
    private String[] allowedOrigins;

    private final SubscriptionRepository subscriptionRepository;

    public SecurityConfig(ObjectProvider<JwtAuthFilter> jwtAuthFilterProvider, @Lazy OAuth2SuccessHandler OAuth2SuccessHandler, SubscriptionRepository subscriptionRepository)
    {
        this.jwtAuthFilterProvider = jwtAuthFilterProvider;
        this.OAuth2SuccessHandler = OAuth2SuccessHandler;
        this.subscriptionRepository = subscriptionRepository;
    }

    private JwtAuthFilter getJwtAuthFilter()
    {
        return jwtAuthFilterProvider.getIfAvailable();
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient()
    {
        RestClientAuthorizationCodeTokenResponseClient delegate = new RestClientAuthorizationCodeTokenResponseClient();
        return request -> {
            OAuth2AccessTokenResponse response = delegate.getTokenResponse(request);
            if (response.getAdditionalParameters() == null) {
                return OAuth2AccessTokenResponse.withResponse(response)
                        .additionalParameters(Collections.emptyMap())
                        .build();
            }
            return response;
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // IMPORTANT : les requêtes OPTIONS (preflight CORS) doivent toujours recevoir
                        // une réponse 200 directe, jamais une redirection. Sans cette règle explicite,
                        // Spring Security peut tenter d'authentifier le preflight lui-même et le
                        // rediriger vers une page de login, ce que le navigateur rejette avec
                        // ERR_INVALID_REDIRECT — bloquant alors la vraie requête (ex: POST /auth/login).
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/api/notifications/push/**").permitAll()
                        .requestMatchers("/api/payments/cinetpay/notify").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().none()
                )
                .exceptionHandling(exceptions -> exceptions
                    .accessDeniedHandler((request, response, exception) -> {
                        response.setStatus(403);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"message\":\"Vous n'êtes pas autorisé à effectuer cette action.\",\"error\":\"Forbidden\",\"status\":403}");
                    })
                )
                .oauth2Login(oauth2 -> oauth2
                        .tokenEndpoint(token -> token.accessTokenResponseClient(accessTokenResponseClient()))
                        .successHandler(OAuth2SuccessHandler)
                        .failureHandler(((request, response, exception) -> {
                            log.warn("OAuth2 authentication failed: {}", exception.getMessage(), exception);
                            response.sendRedirect(frontendUrl + "/login?error=oauth_failed&message=" + exception.getMessage());
                        })))
                .addFilterBefore(getJwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    /**
     * Bean TaskExecutor @Primary pour résoudre l'ambiguïté avec les executors WebSocket.
     * WebSocket (clientInboundChannelExecutor, clientOutboundChannelExecutor, brokerChannelExecutor)
     * crée plusieurs TaskExecutor. Cette configuration crée un executor explicite nommé "taskExecutor"
     * marqué @Primary pour les usages @Async génériques (EmailService, etc.).
     */
    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-task-");
        executor.setAwaitTerminationSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource()
    {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
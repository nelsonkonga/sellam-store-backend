package com.sellam.store.common.security;

import com.sellam.store.auth.JwtProvider;
import com.sellam.store.users.models.PermissionEnum;
import com.sellam.store.users.models.UserEntity;
import com.sellam.store.users.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter
{
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtProvider jwtProvider, UserRepository userRepository)
    {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException
    {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer "))
        {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try
        {
            if (jwtProvider.validateToken(token))
            {
                AuthPrincipal principal = jwtProvider.getPrincipalFromToken(token);

                List<GrantedAuthority> authorities = resolveAuthorities(principal);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities
                );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        catch (Exception e)
        {
            // Un token malformé, une base de données momentanément indisponible,
            // ou toute autre erreur de résolution ne doit jamais casser la chaîne
            // de filtres : on laisse simplement la requête continuer non authentifiée
            // (elle sera rejetée en 401/403 plus loin par Spring Security si la
            // route l'exige). Sans ce catch, une exception ici produirait une 500
            // opaque au lieu d'un refus d'accès normal.
            log.warn("Échec de résolution de l'authentification JWT : {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Résout les autorités effectives du principal :
     * un ACCOUNT (gérant propriétaire) a accès total (bypass)
     * un USER (employé) a les permissions calculées à partir de son rôle + overrides

     * Chargé à chaque requête pour refléter immédiatement tout changement
     * de permissions fait par le gérant, sans que l'employé ait à se reconnecter.
     */
    private List<GrantedAuthority> resolveAuthorities(AuthPrincipal principal)
    {
        if ("ACCOUNT".equals(principal.getUserType()))
        {
            return List.of(new SimpleGrantedAuthority("PERM_ALL"));
        }

        if ("USER".equals(principal.getUserType()))
        {
            Optional<UserEntity> userOpt = userRepository.findById(principal.getId());
            if (userOpt.isEmpty())
            {
                return Collections.emptyList();
            }

            UserEntity user = userOpt.get();
            Set<PermissionEnum> effective = user.getEffectivePermissions();

            return effective.stream()
                    .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p.name()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
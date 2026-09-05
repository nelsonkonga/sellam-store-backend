package com.sellam.store.common.security;

import com.sellam.store.auth.JwtProvider;
import com.sellam.store.identity.models.PersonEntity;
import com.sellam.store.identity.models.ShopMembershipEntity;
import com.sellam.store.identity.repositories.PersonRepository;
import com.sellam.store.identity.repositories.ShopMembershipRepository;
import com.sellam.store.users.models.PermissionEnum;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Filtre JWT adaptÃ© au nouveau modÃ¨le PersonEntity + ShopMembershipEntity.
 * 
 * Changements par rapport Ã  l'ancien JwtAuthFilter :
 * - RÃ©sout les permissions via ShopMembershipEntity au lieu de UserEntity
 * - Supporte le multi-boutique : permissions calculÃ©es par membership active
 * - userType "PERSON" pour le nouveau modÃ¨le
 * - Maintient la compatibilitÃ© avec "ACCOUNT" (PERM_ALL) et "USER" (permissions)
 */
@Component

@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter
{
    private final JwtProvider jwtProvider;
    private final PersonRepository personRepository;
    private final ShopMembershipRepository shopMembershipRepository;

    public JwtAuthFilter(JwtProvider jwtProvider, PersonRepository personRepository, ShopMembershipRepository shopMembershipRepository)
    {
        this.jwtProvider = jwtProvider;
        this.personRepository = personRepository;
        this.shopMembershipRepository = shopMembershipRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException
    {
        // Skip JWT filter for OAuth2 endpoints to avoid interference
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/login/oauth2/"))
        {
            filterChain.doFilter(request, response);
            return;
        }

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
            log.warn("Ã‰chec de rÃ©solution de l'authentification JWT : {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * RÃ©sout les autoritÃ©s effectives du principal :
     *
     * CAS 1 : ACCOUNT (ancien) â†’ accÃ¨s total (PERM_ALL)
     * CAS 2 : PERSON avec systemRole PLATFORM_ADMIN â†’ accÃ¨s total (PERM_ALL)
     * CAS 3 : PERSON avec shopId â†’ permissions scopÃ©es Ã  cette boutique
     * CAS 4 : PERSON sans shopId â†’ permissions neutres uniquement (profil, liste boutiques)
     *
     * RÃ¨gle pour le cas sans shopId :
     * - Juste aprÃ¨s login : accÃ¨s neutre uniquement (pas d'endpoints mÃ©tier)
     * - Ancien gÃ©rant : doit d'abord sÃ©lectionner une boutique
     * - Permet : profil, liste boutiques, changement contact
     * - Refuse : tous les endpoints mÃ©tier scoped
     */
    private List<GrantedAuthority> resolveAuthorities(AuthPrincipal principal)
    {
        // CAS 1 : Ancien type ACCOUNT â†’ accÃ¨s total
        if ("ACCOUNT".equals(principal.getUserType()))
        {
            return List.of(new SimpleGrantedAuthority("PERM_ALL"));
        }

        // CAS 2 : Nouveau type PERSON avec systemRole PLATFORM_ADMIN â†’ accÃ¨s total
        if ("PERSON".equals(principal.getUserType()))
        {
            Optional<PersonEntity> personOpt = personRepository.findById(principal.getId());
            if (personOpt.isPresent())
            {
                PersonEntity person = personOpt.get();
                if (person.getSystemRole() != null
                        && person.getSystemRole().name().equals("PLATFORM_ADMIN"))
                {
                    return List.of(new SimpleGrantedAuthority("PERM_ALL"));
                }
            }
        }

        // CAS 3 : PERSON/USER avec shopId â†’ permissions scopÃ©es Ã  cette boutique
        if (principal.getShopId() != null)
        {
            Optional<PersonEntity> personOpt = personRepository.findById(principal.getId());
            if (personOpt.isEmpty())
            {
                return Collections.emptyList();
            }

            Optional<ShopMembershipEntity> membershipOpt =
                    shopMembershipRepository.findActiveMembership(principal.getId(), principal.getShopId());

            if (membershipOpt.isPresent())
            {
                ShopMembershipEntity membership = membershipOpt.get();
                Set<PermissionEnum> permissions = membership.getEffectivePermissions();

                return permissions.stream()
                        .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p.name()))
                        .collect(Collectors.toList());
            }
        }

        // CAS 4 : Sans shopId â†’ permissions neutres uniquement
        // Permet l'accÃ¨s aux endpoints non scopÃ©s : profil, liste boutiques, changement contact
        // Refuse l'accÃ¨s aux endpoints mÃ©tier scoped (produits, ventes, factures, etc.)
        return List.of(new SimpleGrantedAuthority("NEUTRAL_ACCESS"));
    }
}

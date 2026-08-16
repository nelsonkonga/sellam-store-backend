package com.sellam.store.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Bean exposé aux expressions @PreAuthorize sous le nom "sec".
 * Usage : @PreAuthorize("@sec.can(authentication, 'CREATE_INVOICE')")

 * Un ACCOUNT (gérant propriétaire) a l'autorité spéciale PERM_ALL et passe
 * toujours. Un USER est vérifié selon ses autorités effectives (calculées
 * dans JwtAuthFilter à partir de son rôle + overrides).
 */
@Component("sec")
public class SecurityPermissionEvaluator
{
    public boolean can(Authentication authentication, String permission)
    {
        if ((authentication == null) || (authentication.getAuthorities() == null))
        {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities())
        {
            String name = authority.getAuthority();
            if (name == null)
            {
                continue;
            }
            if (name.equals("PERM_ALL") || name.equals(permission))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Vrai uniquement si le principal est le propriétaire du compte (ACCOUNT),
     * jamais un employé (USER) même avec toutes les permissions.
     * Utilisé pour les actions non-déléguables comme la gestion des employés.
     */
    public boolean isAccountOwner(Authentication authentication)
    {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal))
        {
            return false;
        }
        return "ACCOUNT".equals(principal.getUserType());
    }
}
package com.sellam.store.subscriptions.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bloque les requêtes d'écriture (POST/PUT/PATCH/DELETE) ciblant une
 * boutique dont l'abonnement est EXPIRED.
 *
 * Volontairement permissif sur ce qu'il laisse passer en cas de doute
 * (pas de shopId identifiable dans le path, pas d'abonnement trouvé) :
 * le but est de bloquer les actions métier sur une boutique identifiée,
 * pas de casser des routes qui n'ont pas de notion de boutique (auth,
 * profil, paiement de l'abonnement lui-même...).
 *
 * Le paiement de l'abonnement doit rester accessible même boutique
 * EXPIRED (sinon impossible de la débloquer) : on exclut explicitement
 * les routes /api/shops/{shopId}/subscription/**.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionAccessFilter extends OncePerRequestFilter
{
    private static final Pattern SHOP_ID_PATTERN =
            Pattern.compile("^/api/(?:shops|users/shop|cash/registers|cash/status)/([0-9a-fA-F-]{36})(/.*)?$");

    private final SubscriptionWriteGuard subscriptionWriteGuard;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        if (!isWriteMethod(request.getMethod()))
        {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/") || path.startsWith("/api/payments/"))
        {
            filterChain.doFilter(request, response);
            return;
        }

        UUID shopId = shopIdFromQuery(request);
        Matcher matcher = SHOP_ID_PATTERN.matcher(path);
        if (shopId == null && matcher.matches())
        {
            String subPath = matcher.group(2) == null ? "" : matcher.group(2);
            if (subPath.startsWith("/subscription"))
            {
                filterChain.doFilter(request, response);
                return;
            }
            try
            {
                shopId = UUID.fromString(matcher.group(1));
            }
            catch (IllegalArgumentException ignored)
            {
                shopId = null;
            }
        }

        if (shopId == null)
        {
            filterChain.doFilter(request, response);
            return;
        }

        if (subscriptionWriteGuard.isExpired(shopId))
        {
            writeBlockedResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private UUID shopIdFromQuery(HttpServletRequest request)
    {
        String raw = request.getParameter("shopId");
        if (raw == null || raw.isBlank())
        {
            return null;
        }
        try
        {
            return UUID.fromString(raw);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    private boolean isWriteMethod(String method)
    {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private void writeBlockedResponse(HttpServletResponse response) throws IOException
    {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"SUBSCRIPTION_EXPIRED\",\"message\":\"Votre abonnement a expiré. Renouvelez-le pour continuer à utiliser Sellam.\"}");
    }
}

package com.sellam.store.subscriptions.security;

import com.sellam.store.subscriptions.models.SubscriptionEntity;
import com.sellam.store.subscriptions.models.SubscriptionStatusEnum;
import com.sellam.store.subscriptions.repositories.SubscriptionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
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
@RequiredArgsConstructor
public class SubscriptionAccessFilter extends OncePerRequestFilter
{
    private static final Pattern SHOP_ID_PATTERN =
            Pattern.compile("^/api/shops/([0-9a-fA-F-]{36})(/.*)?$");

    private final SubscriptionRepository subscriptionRepository;

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
        Matcher matcher = SHOP_ID_PATTERN.matcher(path);
        if (!matcher.matches())
        {
            filterChain.doFilter(request, response);
            return;
        }

        String subPath = matcher.group(2) == null ? "" : matcher.group(2);
        if (subPath.startsWith("/subscription"))
        {
            // Le paiement/consultation de l'abonnement doit rester accessible
            // même boutique bloquée, sinon aucun moyen de se débloquer.
            filterChain.doFilter(request, response);
            return;
        }

        UUID shopId;
        try
        {
            shopId = UUID.fromString(matcher.group(1));
        }
        catch (IllegalArgumentException e)
        {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<SubscriptionEntity> subscriptionOpt = subscriptionRepository.findByShopId(shopId);
        if (subscriptionOpt.isEmpty())
        {
            // Pas d'abonnement = ancienne donnée ou boutique de test sans
            // migration ; on ne bloque pas sur une absence de donnée.
            filterChain.doFilter(request, response);
            return;
        }

        if (subscriptionOpt.get().getStatus() == SubscriptionStatusEnum.EXPIRED)
        {
            writeBlockedResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
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

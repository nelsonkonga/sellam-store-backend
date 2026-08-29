package com.sellam.store.auth;

import com.sellam.store.common.security.AuthPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * Provider JWT adapté au nouveau modèle PersonEntity.
 * 
 * Changements par rapport à l'ancien JwtProvider :
 * - userType peut être "PERSON" (nouveau modèle) ou "ACCOUNT"/"USER" (compatibilité)
 * - shopId est nullable et déterminé dynamiquement via ShopMembershipEntity
 * - Le token ne contient plus de shopId fixe pour supporter le multi-boutique
 */
@Component
public class JwtProviderNew
{
    @Value("${jwt.secret}")
    private String secretkey;

    @Value("${jwt.expiration}")
    private long expirationMs;


    private Key getSigningKey()
    {
        return Keys.hmacShaKeyFor(secretkey.getBytes(StandardCharsets.UTF_8));
    }


    /**
     * Génère un token JWT pour une personne.
     * 
     * @param id PersonEntity.id
     * @param userType "PERSON" (nouveau) ou "ACCOUNT"/"USER" (compatibilité)
     * @param shopId UUID de la boutique active (nullable pour multi-boutique)
     * @param phoneNumber Numéro de téléphone
     */
    public String generateToken(UUID id, String userType, UUID shopId, String phoneNumber)
    {
        return Jwts.builder()
                    .setSubject(id.toString())
                    .claim("userType", userType)
                    .claim("shopId", shopId != null ? shopId.toString() : null)
                    .claim("phoneNumber", phoneNumber)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();

    }


    public AuthPrincipal getPrincipalFromToken(String token)
    {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey((getSigningKey()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        String shopIdStr = claims.get("shopId", String.class);

        return AuthPrincipal.builder()
                .id(UUID.fromString(claims.getSubject()))
                .userType(claims.get("userType", String.class))
                .shopId(shopIdStr != null ? UUID.fromString(shopIdStr) : null)
                .phoneNumber(claims.get("phoneNumber", String.class))
                .build();
    }


    public boolean validateToken(String token)
    {
        try
        {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            return true;
        }
        catch (JwtException | IllegalArgumentException e)
        {
            return false;
        }
    }
}

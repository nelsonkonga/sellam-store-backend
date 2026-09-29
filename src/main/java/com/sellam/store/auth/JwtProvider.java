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
 * Provider JWT adaptÃ© au nouveau modÃ¨le PersonEntity.
 * 
 * Changements par rapport Ã  l'ancien JwtProvider :
 * - userType peut Ãªtre "PERSON" (nouveau modÃ¨le) ou "ACCOUNT"/"USER" (compatibilitÃ©)
 * - shopId est nullable et dÃ©terminÃ© dynamiquement via ShopMembershipEntity
 * - Le token ne contient plus de shopId fixe pour supporter le multi-boutique
 */
@Component

public class JwtProvider
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
     * GÃ©nÃ¨re un token JWT pour une personne.
     *
     * @param id PersonEntity.id
     * @param name Nom de la personne
     * @param userType "PERSON" (nouveau) ou "ACCOUNT"/"USER" (compatibilitÃ©)
     * @param shopId UUID de la boutique active (nullable pour multi-boutique)
     * @param phoneNumber NumÃ©ro de tÃ©lÃ©phone
     */
    public String generateToken(UUID id, String name, String userType, UUID shopId, String phoneNumber)
    {
        return generateToken(id, name, userType, shopId, phoneNumber, 0);
    }

    public String generateToken(UUID id, String name, String userType, UUID shopId, String phoneNumber, int tokenVersion)
    {
        return Jwts.builder()
                    .setSubject(id.toString())
                    .claim("name", name)
                    .claim("userType", userType)
                    .claim("shopId", shopId != null ? shopId.toString() : null)
                    .claim("phoneNumber", phoneNumber)
                    .claim("tv", tokenVersion)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();

    }

    public int getTokenVersion(String token)
    {
        Claims claims = parseClaims(token);
        Integer version = claims.get("tv", Integer.class);
        return version == null ? 0 : version;
    }

    private Claims parseClaims(String token)
    {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public AuthPrincipal getPrincipalFromToken(String token)
    {
        Claims claims = parseClaims(token);

        String shopIdStr = claims.get("shopId", String.class);

        return AuthPrincipal.builder()
                .id(UUID.fromString(claims.getSubject()))
                .name(claims.get("name", String.class))
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

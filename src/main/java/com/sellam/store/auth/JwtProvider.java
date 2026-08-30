package com.sellam.store.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import com.sellam.store.common.security.AuthPrincipal;

@Component
@Profile("!phase1")
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

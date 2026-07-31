package com.sellam.store.auth;

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


    public String generateToken(UUID accountId, String phoneNumber)
    {
        return Jwts.builder()
                    .setSubject(accountId.toString())
                    .claim("phoneNumber", phoneNumber)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();

    }


    public UUID getAccountIdFromToken(String token)
    {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey((getSigningKey()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        return UUID.fromString(claims.getSubject());

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

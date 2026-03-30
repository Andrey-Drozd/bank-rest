package com.example.bankcards.security;

import com.example.bankcards.config.JwtProperties;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        List<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .map(Enum::name)
                .toList();

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("username", user.getUsername())
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getLifetime()))
                .signWith(secretKey)
                .compact();
    }

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserPrincipal principal) {
        Claims claims = parseClaims(token);
        return claims.getSubject().equals(principal.getUsername()) && claims.getExpiration().after(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

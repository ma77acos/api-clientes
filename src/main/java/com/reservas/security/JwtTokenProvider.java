// src/main/java/com/reservas/security/JwtTokenProvider.java
package com.reservas.security;

import com.reservas.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genera token desde Authentication (usado en login)
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername());
    }

    /**
     * Genera token desde username/email
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * ✅ NUEVO: Genera token desde User entity (usado en refresh)
     * Incluye claims adicionales como userId, role y complexId
     */
    public String generateTokenFromUser(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        JwtBuilder builder = Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey());

        // Agregar complexId solo si existe
        if (user.getComplex() != null) {
            builder.claim("complexId", user.getComplex().getId());
        }

        return builder.compact();
    }

    /**
     * Genera refresh token (UUID simple)
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * ✅ NUEVO: Valida refresh token
     * Nota: Como el refresh token es un UUID, solo verifica formato
     */
    public boolean validateRefreshToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.warn("Refresh token vacío");
                return false;
            }

            // Validar que sea un UUID válido
            UUID.fromString(token);
            return true;

        } catch (IllegalArgumentException e) {
            log.error("Refresh token con formato inválido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae username/email del token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * ✅ NUEVO: Extrae el userId del token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("userId", Long.class);
    }

    /**
     * ✅ NUEVO: Extrae el role del token
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("role", String.class);
    }

    /**
     * ✅ NUEVO: Extrae el complexId del token (puede ser null)
     */
    public Long getComplexIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("complexId", Long.class);
    }

    /**
     * Valida el access token JWT
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;

        } catch (SecurityException ex) {
            log.error("Firma JWT inválida: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Token JWT malformado: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Token JWT expirado: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Token JWT no soportado: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("Claims JWT vacíos: {}", ex.getMessage());
        }

        return false;
    }

    /**
     * ✅ NUEVO: Verifica si el token está expirado
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().before(new Date());

        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.error("Error verificando expiración del token: {}", e.getMessage());
            return true;
        }
    }

    /**
     * ✅ NUEVO: Obtiene la fecha de expiración del token
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getExpiration();
    }

    /**
     * Retorna el tiempo de expiración en segundos
     */
    public Long getExpirationInSeconds() {
        return jwtExpiration / 1000;
    }

    /**
     * ✅ NUEVO: Retorna el tiempo de expiración del refresh token en segundos
     */
    public Long getRefreshExpirationInSeconds() {
        return refreshExpiration / 1000;
    }
}
package com.aring.security;

import com.aring.model.Admin;
import com.aring.model.SuperUsuario;
import com.aring.model.Tecnico;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Admin admin) {
        return buildToken(admin.getUsername(), Map.of(
                "rol", "ROLE_ADMIN",
                "nombre", admin.getNombre(),
                "apellido", admin.getApellido()));
    }

    public String generateToken(SuperUsuario superUsuario) {
        return buildToken(superUsuario.getUsername(), Map.of(
                "rol", "ROLE_SUPER",
                "nombre", superUsuario.getNombre(),
                "apellido", superUsuario.getApellido()));
    }

    public String generateToken(Tecnico tecnico) {
        return buildToken(tecnico.getRut(), Map.of(
                "rol", "ROLE_TECNICO",
                "tecnicoId", tecnico.getId(),
                "nombre", tecnico.getNombre(),
                "apellido", tecnico.getApellido()));
    }

    private String buildToken(String subject, Map<String, Object> claims) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRol(String token) {
        return parseClaims(token).get("rol", String.class);
    }

    public Long extractTecnicoId(String token) {
        Object value = parseClaims(token).get("tecnicoId");
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

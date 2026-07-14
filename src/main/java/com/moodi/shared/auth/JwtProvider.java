package com.moodi.shared.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String TYPE_CLAIM = "type";

    private final SecretKey key;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
            @Value("${jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    public String issueAccessToken(UUID memberId) {
        return issueAt(memberId, TokenType.ACCESS, System.currentTimeMillis(), accessTokenExpiryMs);
    }

    public IssuedToken issueRefreshToken(UUID memberId) {
        long now = System.currentTimeMillis();
        String token = issueAt(memberId, TokenType.REFRESH, now, refreshTokenExpiryMs);
        return new IssuedToken(token, toLocalDateTime(now + refreshTokenExpiryMs));
    }

    public Optional<UUID> parseAccessToken(String token) {
        return parse(token, TokenType.ACCESS);
    }

    public Optional<UUID> parseRefreshToken(String token) {
        return parse(token, TokenType.REFRESH);
    }

    private String issueAt(UUID memberId, TokenType type, long now, long expiryMs) {
        return Jwts.builder()
                .subject(memberId.toString())
                .claim(TYPE_CLAIM, type.name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryMs))
                .signWith(key)
                .compact();
    }

    private Optional<UUID> parse(String token, TokenType expected) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expected.name().equals(claims.get(TYPE_CLAIM, String.class))) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private LocalDateTime toLocalDateTime(long epochMs) {
        return new Date(epochMs).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}

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
    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;
    private final long adminAccessTokenExpiryMs;
    private final long adminRefreshTokenExpiryMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
            @Value("${jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs,
            @Value("${admin.jwt.access-token-expiry-ms:1800000}") long adminAccessTokenExpiryMs,
            @Value("${admin.jwt.refresh-token-expiry-ms:43200000}") long adminRefreshTokenExpiryMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
        this.adminAccessTokenExpiryMs = adminAccessTokenExpiryMs;
        this.adminRefreshTokenExpiryMs = adminRefreshTokenExpiryMs;
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

    /**
     * 관리자 토큰은 회원 토큰과 같은 키로 서명하되 `type` 클레임으로 구분한다.
     * 권한은 액세스 토큰에만 싣는다 — 재발급 시 DB의 현재 권한을 다시 읽어 넣기 위해서다.
     */
    public String issueAdminAccessToken(UUID adminId, AdminRole role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(adminId.toString())
                .claim(TYPE_CLAIM, TokenType.ADMIN_ACCESS.name())
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + adminAccessTokenExpiryMs))
                .signWith(key)
                .compact();
    }

    public IssuedToken issueAdminRefreshToken(UUID adminId) {
        long now = System.currentTimeMillis();
        String token = issueAt(adminId, TokenType.ADMIN_REFRESH, now, adminRefreshTokenExpiryMs);
        return new IssuedToken(token, toLocalDateTime(now + adminRefreshTokenExpiryMs));
    }

    public Optional<AdminPrincipal> parseAdminAccessToken(String token) {
        return parseClaims(token, TokenType.ADMIN_ACCESS)
                .flatMap(claims -> {
                    try {
                        return Optional.of(new AdminPrincipal(
                                UUID.fromString(claims.getSubject()),
                                AdminRole.valueOf(claims.get(ROLE_CLAIM, String.class))));
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                });
    }

    public Optional<UUID> parseAdminRefreshToken(String token) {
        return parse(token, TokenType.ADMIN_REFRESH);
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
        return parseClaims(token, expected).map(claims -> UUID.fromString(claims.getSubject()));
    }

    private Optional<Claims> parseClaims(String token, TokenType expected) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expected.name().equals(claims.get(TYPE_CLAIM, String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private LocalDateTime toLocalDateTime(long epochMs) {
        return new Date(epochMs).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}

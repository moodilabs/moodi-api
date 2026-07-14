package com.moodi.shared.auth;

import java.time.LocalDateTime;

public record IssuedToken(String token, LocalDateTime expiresAt) {
}

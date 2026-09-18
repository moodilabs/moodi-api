package com.moodi.route.presentation.dto;

import java.util.UUID;

/**
 * 공유 활성화 응답. 앱은 {@code shareUrl} 을 카카오톡 카드 링크·OS 공유 문구에 그대로 넣는다.
 */
public record RouteShareResponse(
        UUID publicId,
        String shortCode,
        String shareUrl
) {
}

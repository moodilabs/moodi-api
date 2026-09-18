package com.moodi.route.presentation;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 요청이 실제로 들어온 공개 origin({@code https://dev-api.moodi.kr}). Firebase Hosting → Cloud Run 프록시를
 * 거치므로 {@code X-Forwarded-Proto}/{@code X-Forwarded-Host} 를 우선하고, 없으면 서블릿 값으로 폴백한다.
 * 공유 링크·og:image 처럼 밖으로 나가는 절대 주소를 만들 때 쓴다.
 */
public final class RequestOrigin {

    private RequestOrigin() {
    }

    public static String of(HttpServletRequest request) {
        String proto = firstHeaderValue(request, "X-Forwarded-Proto", request.getScheme());
        String host = firstHeaderValue(request, "X-Forwarded-Host", hostWithPort(request));
        return proto + "://" + host;
    }

    private static String hostWithPort(HttpServletRequest request) {
        int port = request.getServerPort();
        boolean defaultPort = port <= 0
                || ("http".equalsIgnoreCase(request.getScheme()) && port == 80)
                || ("https".equalsIgnoreCase(request.getScheme()) && port == 443);
        return defaultPort ? request.getServerName() : request.getServerName() + ":" + port;
    }

    private static String firstHeaderValue(HttpServletRequest request, String headerName, String fallback) {
        String value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.split(",")[0].trim();
    }
}

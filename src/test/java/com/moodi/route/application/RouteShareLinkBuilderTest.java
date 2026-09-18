package com.moodi.route.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RouteShareLinkBuilderTest {

    private static final UUID PUBLIC_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    private static final String ORIGIN = "https://dev-api.moodi.kr";

    private RouteShareLinkBuilder builder(String baseUrl) {
        return new RouteShareLinkBuilder(new RouteShareProperties(null, baseUrl, null, null, null));
    }

    @Test
    @DisplayName("단축 코드가 있으면 요청 origin 기준 /s/{code} 링크를 만든다")
    void share_url_prefers_short_code() {
        // when
        String url = builder(null).shareUrl(ORIGIN, PUBLIC_ID, "Ab12Cd34");

        // then
        assertThat(url).isEqualTo("https://dev-api.moodi.kr/s/Ab12Cd34");
    }

    @Test
    @DisplayName("단축 코드가 없으면 긴 링크 /routes/shared/{publicId} 로 폴백한다")
    void share_url_falls_back_to_long_url() {
        // when
        String url = builder(null).shareUrl(ORIGIN, PUBLIC_ID, null);

        // then
        assertThat(url).isEqualTo("https://dev-api.moodi.kr/routes/shared/" + PUBLIC_ID);
    }

    @Test
    @DisplayName("base-url 설정이 있으면 요청 origin 대신 설정값을 쓰고 끝 슬래시는 정리한다")
    void share_url_uses_configured_base_url() {
        // when
        String url = builder("https://moodi.kr/").shortUrl(ORIGIN, "Ab12Cd34");

        // then
        assertThat(url).isEqualTo("https://moodi.kr/s/Ab12Cd34");
    }

    @Test
    @DisplayName("딥링크는 앱이 인식하는 moodi://route/shared/{publicId} 형식이다")
    void deep_link_matches_app_parser() {
        // when
        String deepLink = builder(null).deepLink(PUBLIC_ID);

        // then
        assertThat(deepLink).isEqualTo("moodi://route/shared/" + PUBLIC_ID);
    }
}

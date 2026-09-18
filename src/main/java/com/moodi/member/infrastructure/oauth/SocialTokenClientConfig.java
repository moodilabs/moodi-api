package com.moodi.member.infrastructure.oauth;

import com.moodi.member.application.SocialTokenClient;
import com.moodi.member.domain.OAuthProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/** 제공자별 토큰 어댑터를 enabled 플래그에 따라 조립한다. 둘 다 꺼져 있으면 모든 호출이 경고 후 실패한다. */
@Configuration
@EnableConfigurationProperties({AppleTokenProperties.class, GoogleTokenProperties.class})
public class SocialTokenClientConfig {

    /** 로그인·탈퇴 요청 안에서 동기 호출되므로 제공자가 느려도 요청을 오래 잡지 않게 짧게 둔다. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public SocialTokenClient socialTokenClient(AppleTokenProperties apple, GoogleTokenProperties google, Clock clock) {
        Map<OAuthProvider, SocialTokenClient> clients = new EnumMap<>(OAuthProvider.class);
        if (apple.enabled()) {
            clients.put(OAuthProvider.APPLE, new AppleTokenClient(apple, restClientBuilder(), clock));
        }
        if (google.enabled()) {
            clients.put(OAuthProvider.GOOGLE, new GoogleTokenClient(google, restClientBuilder()));
        }
        return new ProviderRoutingSocialTokenClient(clients);
    }

    private static RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().requestFactory(factory);
    }
}

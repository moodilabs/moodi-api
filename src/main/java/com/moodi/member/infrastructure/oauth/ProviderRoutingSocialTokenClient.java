package com.moodi.member.infrastructure.oauth;

import com.moodi.member.application.SocialTokenClient;
import com.moodi.member.domain.OAuthProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

/**
 * 제공자별 어댑터로 분기한다. 구성되지 않은 제공자는 호출마다 경고를 남기고 실패로 돌려준다 —
 * 운영에서 키가 빠진 채 배포된 것을 로그로 알아챌 수 있게.
 */
@Slf4j
public class ProviderRoutingSocialTokenClient implements SocialTokenClient {

    private final Map<OAuthProvider, SocialTokenClient> clients;

    public ProviderRoutingSocialTokenClient(Map<OAuthProvider, SocialTokenClient> clients) {
        this.clients = Map.copyOf(clients);
    }

    @Override
    public Optional<String> exchangeRefreshToken(OAuthProvider provider, String clientId, String authorizationCode) {
        SocialTokenClient client = clients.get(provider);
        if (client == null) {
            warnUnconfigured(provider);
            return Optional.empty();
        }
        return client.exchangeRefreshToken(provider, clientId, authorizationCode);
    }

    @Override
    public boolean revoke(OAuthProvider provider, String clientId, String refreshToken) {
        SocialTokenClient client = clients.get(provider);
        if (client == null) {
            warnUnconfigured(provider);
            return false;
        }
        return client.revoke(provider, clientId, refreshToken);
    }

    private void warnUnconfigured(OAuthProvider provider) {
        log.warn("{} 토큰 클라이언트 미구성(oauth.{}.token.enabled=false) — 계정 연결 철회가 동작하지 않습니다",
                provider, provider.name().toLowerCase());
    }
}

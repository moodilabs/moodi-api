package com.moodi.member.infrastructure.oauth;

import com.moodi.member.application.SocialTokenClient;
import com.moodi.member.domain.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderRoutingSocialTokenClientTest {

    @Test
    @DisplayName("제공자별 어댑터로 분기하고, 구성되지 않은 제공자는 실패로 돌려준다")
    void routes_by_provider() {
        SocialTokenClient apple = mock(SocialTokenClient.class);
        when(apple.revoke(OAuthProvider.APPLE, "c", "t")).thenReturn(true);
        when(apple.exchangeRefreshToken(OAuthProvider.APPLE, "c", "code")).thenReturn(Optional.of("r"));
        ProviderRoutingSocialTokenClient router = new ProviderRoutingSocialTokenClient(Map.of(OAuthProvider.APPLE, apple));

        assertThat(router.revoke(OAuthProvider.APPLE, "c", "t")).isTrue();
        assertThat(router.exchangeRefreshToken(OAuthProvider.APPLE, "c", "code")).contains("r");
        assertThat(router.revoke(OAuthProvider.GOOGLE, "c", "t")).isFalse();
        assertThat(router.exchangeRefreshToken(OAuthProvider.GOOGLE, "c", "code")).isEmpty();
        verify(apple).revoke(OAuthProvider.APPLE, "c", "t");
    }
}

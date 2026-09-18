package com.moodi.route.application;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteRepository;
import com.moodi.route.domain.RouteShortCode;
import com.moodi.route.support.RouteFixture;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RouteShareServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private RouteShareService routeShareService;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 8, 10);

    private static Route route() {
        return RouteFixture.createRoute(
                MEMBER_ID, "Retro mood trip in Seongsu", START, START,
                List.of(RouteFixture.createDay(1, START, 1))
        );
    }

    @Test
    @DisplayName("루트 공유 활성화 시 단축 코드가 조건부 갱신으로 발급된다")
    void share_route_success() {
        // given
        UUID publicId = UUID.randomUUID();
        Route route = route();
        given(routeRepository.findByPublicId(publicId)).willReturn(Optional.of(route));
        given(routeRepository.existsByShortCode(anyString())).willReturn(false);
        given(routeRepository.assignShortCodeIfAbsent(any(), anyString())).willReturn(1);

        // when
        Route result = routeShareService.share(publicId, MEMBER_ID);

        // then
        assertThat(result).isSameAs(route);
        assertThat(route.isShared()).isTrue();
        assertThat(route.getShortCode()).hasSize(RouteShortCode.LENGTH);
        assertThat(RouteShortCode.isValid(route.getShortCode())).isTrue();
        verify(routeRepository).assignShortCodeIfAbsent(any(), eq(route.getShortCode()));
    }

    @Test
    @DisplayName("동시에 다른 요청이 먼저 코드를 붙였으면 내 코드를 버리고 DB 의 코드를 응답한다")
    void share_route_loses_race_and_reads_stored_code() {
        // given
        UUID publicId = UUID.randomUUID();
        Route stale = route();
        Route stored = route();
        stored.share();
        stored.assignShortCode("Ab12Cd34");
        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(stale), Optional.of(stored));
        given(routeRepository.existsByShortCode(anyString())).willReturn(false);
        given(routeRepository.assignShortCodeIfAbsent(any(), anyString())).willReturn(0);

        // when
        Route result = routeShareService.share(publicId, MEMBER_ID);

        // then
        assertThat(result).isSameAs(stored);
        assertThat(result.getShortCode()).isEqualTo("Ab12Cd34");
        assertThat(stale.hasShortCode()).isFalse();
    }

    @Test
    @DisplayName("이미 단축 코드가 있는 루트를 다시 공유하면 코드를 바꾸지 않는다")
    void share_route_keeps_existing_short_code() {
        // given
        UUID publicId = UUID.randomUUID();
        Route route = route();
        route.share();
        route.assignShortCode("Ab12Cd34");
        given(routeRepository.findByPublicId(publicId)).willReturn(Optional.of(route));

        // when
        routeShareService.share(publicId, MEMBER_ID);

        // then
        assertThat(route.getShortCode()).isEqualTo("Ab12Cd34");
        verify(routeRepository, never()).existsByShortCode(anyString());
    }

    @Test
    @DisplayName("생성한 코드가 이미 쓰이고 있으면 다시 뽑는다")
    void share_route_retries_on_collision() {
        // given
        UUID publicId = UUID.randomUUID();
        Route route = route();
        given(routeRepository.findByPublicId(publicId)).willReturn(Optional.of(route));
        given(routeRepository.existsByShortCode(anyString())).willReturn(true, false);
        given(routeRepository.assignShortCodeIfAbsent(any(), anyString())).willReturn(1);

        // when
        routeShareService.share(publicId, MEMBER_ID);

        // then
        assertThat(route.hasShortCode()).isTrue();
        verify(routeRepository, times(2)).existsByShortCode(anyString());
    }

    @Test
    @DisplayName("코드가 계속 충돌하면 실패한다")
    void share_route_short_code_exhausted() {
        // given
        UUID publicId = UUID.randomUUID();
        Route route = route();
        given(routeRepository.findByPublicId(publicId)).willReturn(Optional.of(route));
        given(routeRepository.existsByShortCode(anyString())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> routeShareService.share(publicId, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_SHORT_CODE_EXHAUSTED);
    }

    @Test
    @DisplayName("존재하지 않는 루트 공유 시 실패")
    void share_route_not_found() {
        // given
        UUID publicId = UUID.randomUUID();
        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> routeShareService.share(publicId, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
    }

    @Test
    @DisplayName("소유자가 아니면 공유 실패")
    void share_route_forbidden() {
        // given
        UUID publicId = UUID.randomUUID();
        Route route = route();
        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(route));

        UUID otherMemberId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> routeShareService.share(publicId, otherMemberId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_FORBIDDEN);
    }
}

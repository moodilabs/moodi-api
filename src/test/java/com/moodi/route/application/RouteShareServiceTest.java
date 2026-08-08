package com.moodi.route.application;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteRepository;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RouteShareServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private RouteShareService routeShareService;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 8, 10);

    @Test
    @DisplayName("루트 공유 활성화 성공")
    void share_route_success() {
        // given
        UUID publicId = UUID.randomUUID();
        Route route = RouteFixture.createRoute(
                MEMBER_ID, "Retro mood trip in Seongsu", START, START,
                List.of(RouteFixture.createDay(1, START, 1))
        );

        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(route));

        // when
        routeShareService.share(publicId, MEMBER_ID);

        // then
        assertThat(route.isShared()).isTrue();
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
        Route route = RouteFixture.createRoute(
                MEMBER_ID, "Retro mood trip in Seongsu", START, START,
                List.of(RouteFixture.createDay(1, START, 1))
        );

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

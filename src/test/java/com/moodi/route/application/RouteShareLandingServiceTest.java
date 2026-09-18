package com.moodi.route.application;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteRepository;
import com.moodi.route.support.RouteFixture;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RouteShareLandingServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private RouteShareLandingService routeShareLandingService;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 8, 10);

    @Test
    @DisplayName("공유 중인 루트는 제목을 담은 랜딩 뷰를 반환한다")
    void get_landing_view_success() {
        // given
        UUID publicId = UUID.randomUUID();
        Route route = RouteFixture.createRoute(
                MEMBER_ID, "Retro mood trip in Seongsu", START, START,
                List.of(RouteFixture.createDay(1, START, 1))
        );
        route.share();

        given(routeRepository.findSharedByPublicId(publicId))
                .willReturn(Optional.of(route));

        // when
        RouteShareLandingView result = routeShareLandingService.getLandingView(publicId);

        // then
        assertThat(result.title()).isEqualTo("Retro mood trip in Seongsu");
        assertThat(result.found()).isTrue();
    }

    @Test
    @DisplayName("공유되지 않았거나 존재하지 않는 루트는 기본 제목으로 폴백한다")
    void get_landing_view_not_found() {
        // given
        UUID publicId = UUID.randomUUID();
        given(routeRepository.findSharedByPublicId(publicId))
                .willReturn(Optional.empty());

        // when
        RouteShareLandingView result = routeShareLandingService.getLandingView(publicId);

        // then
        assertThat(result.found()).isFalse();
        assertThat(result.title()).isEqualTo("Moodi");
    }
}

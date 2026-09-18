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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RouteShareLandingServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private RouteShareLandingService routeShareLandingService;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 9, 23);

    private static Route sharedRoute() {
        Route route = RouteFixture.createRoute(
                MEMBER_ID, "Retro mood trip in Seongsu", START, START.plusDays(1),
                List.of(RouteFixture.createDay(1, START, 2), RouteFixture.createDay(2, START.plusDays(1), 1))
        );
        route.share();
        route.assignShortCode("Ab12Cd34");
        return route;
    }

    @Test
    @DisplayName("공유 중인 루트는 제목·기간·스팟 요약이 담긴 랜딩 뷰를 반환한다")
    void get_landing_view_success() {
        // given
        Route route = sharedRoute();
        given(routeRepository.findSharedByPublicIdWithDays(route.getPublicId()))
                .willReturn(Optional.of(route));

        // when
        RouteShareLandingView result = routeShareLandingService.getLandingView(route.getPublicId());

        // then
        assertThat(result.found()).isTrue();
        assertThat(result.title()).isEqualTo("Retro mood trip in Seongsu");
        assertThat(result.shortCode()).isEqualTo("Ab12Cd34");
        assertThat(result.totalDays()).isEqualTo(2);
        assertThat(result.spotCount()).isEqualTo(3);
        assertThat(result.area()).isEqualTo("서울");
        assertThat(result.imageUrl()).isEqualTo("https://img.example.com/1.jpg");
        assertThat(result.days()).hasSize(2);
        assertThat(result.days().get(0).spots()).extracting(RouteShareLandingView.SpotView::title)
                .containsExactly("스팟 1", "스팟 2");
    }

    @Test
    @DisplayName("단축 코드로도 같은 랜딩 뷰를 반환한다")
    void get_landing_view_by_short_code() {
        // given
        Route route = sharedRoute();
        given(routeRepository.findSharedByShortCodeWithDays("Ab12Cd34"))
                .willReturn(Optional.of(route));

        // when
        RouteShareLandingView result = routeShareLandingService.getLandingViewByShortCode("Ab12Cd34");

        // then
        assertThat(result.found()).isTrue();
        assertThat(result.publicId()).isEqualTo(route.getPublicId());
    }

    @Test
    @DisplayName("단축 코드 형식이 아니면 DB 조회 없이 기본 제목으로 폴백한다")
    void get_landing_view_by_malformed_short_code() {
        // when
        RouteShareLandingView result = routeShareLandingService.getLandingViewByShortCode("not a code!");

        // then
        assertThat(result.found()).isFalse();
        verify(routeRepository, never()).findSharedByShortCodeWithDays(anyString());
    }

    @Test
    @DisplayName("공유되지 않았거나 존재하지 않는 루트는 기본 제목으로 폴백한다")
    void get_landing_view_not_found() {
        // given
        UUID publicId = UUID.randomUUID();
        given(routeRepository.findSharedByPublicIdWithDays(publicId))
                .willReturn(Optional.empty());

        // when
        RouteShareLandingView result = routeShareLandingService.getLandingView(publicId);

        // then
        assertThat(result.found()).isFalse();
        assertThat(result.title()).isEqualTo("Moodi");
        assertThat(result.days()).isEmpty();
    }
}

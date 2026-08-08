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
class RouteDeleteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private RouteDeleteService routeDeleteService;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 8, 10);

    @Test
    @DisplayName("루트 삭제 성공")
    void delete_route_success() {
        // given
        UUID publicId = UUID.randomUUID();
        Route route = RouteFixture.createRoute(
                MEMBER_ID, "Retro mood trip in Seongsu", START, START,
                List.of(RouteFixture.createDay(1, START, 1))
        );

        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(route));

        // when
        routeDeleteService.delete(publicId, MEMBER_ID);

        // then
        assertThat(route.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 루트 삭제 시 실패")
    void delete_route_not_found() {
        // given
        UUID publicId = UUID.randomUUID();
        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> routeDeleteService.delete(publicId, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
    }

    @Test
    @DisplayName("소유자가 아니면 삭제 실패")
    void delete_route_forbidden() {
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
        assertThatThrownBy(() -> routeDeleteService.delete(publicId, otherMemberId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_FORBIDDEN);
    }
}

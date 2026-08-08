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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RouteCopyServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private RouteCopyService routeCopyService;

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID COPIER_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 8, 10);

    @Test
    @DisplayName("공유된 루트 복제 성공")
    void copy_shared_route_success() {
        // given
        UUID publicId = UUID.randomUUID();
        Route original = RouteFixture.createRoute(
                OWNER_ID, "Retro mood trip in Seongsu", START, START.plusDays(1),
                List.of(
                        RouteFixture.createDay(1, START, 2),
                        RouteFixture.createDay(2, START.plusDays(1), 2)
                )
        );
        original.share();

        given(routeRepository.findSharedByPublicId(publicId))
                .willReturn(Optional.of(original));
        given(routeRepository.save(any(Route.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Route copied = routeCopyService.copy(publicId, COPIER_ID);

        // then
        assertThat(copied.getMemberId()).isEqualTo(COPIER_ID);
        assertThat(copied.getPublicId()).isNotEqualTo(original.getPublicId());
        assertThat(copied.getTitle()).isEqualTo(original.getTitle());
        assertThat(copied.getDays()).hasSize(2);
        assertThat(copied.isShared()).isFalse();
    }

    @Test
    @DisplayName("자기 루트도 복제 가능")
    void copy_own_route_success() {
        // given
        UUID publicId = UUID.randomUUID();
        Route original = RouteFixture.createRoute(
                OWNER_ID, "My route", START, START,
                List.of(RouteFixture.createDay(1, START, 2))
        );
        original.share();

        given(routeRepository.findSharedByPublicId(publicId))
                .willReturn(Optional.of(original));
        given(routeRepository.save(any(Route.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        Route copied = routeCopyService.copy(publicId, OWNER_ID);

        // then
        assertThat(copied.getMemberId()).isEqualTo(OWNER_ID);
        assertThat(copied.getPublicId()).isNotEqualTo(original.getPublicId());
    }

    @Test
    @DisplayName("공유되지 않은 루트 복제 시 실패")
    void copy_not_shared_route_fails() {
        // given
        UUID publicId = UUID.randomUUID();
        given(routeRepository.findSharedByPublicId(publicId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> routeCopyService.copy(publicId, COPIER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
    }
}

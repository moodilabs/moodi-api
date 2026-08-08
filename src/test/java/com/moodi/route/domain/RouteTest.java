package com.moodi.route.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.route.support.RouteFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final String TITLE = "성수 1박 2일 코스";
    private static final LocalDate START = LocalDate.of(2026, 8, 10);

    @Test
    @DisplayName("유효한 루트 생성 성공")
    void create_route_success() {
        Route route = RouteFixture.createRoute(2);

        assertThat(route.getTotalDays()).isEqualTo(2);
        assertThat(route.getDays()).hasSize(2);
        assertThat(route.getPublicId()).isNotNull();
    }

    @Test
    @DisplayName("당일치기(1일) 루트 생성 성공")
    void create_single_day_route() {
        Route route = RouteFixture.createRoute(1);

        assertThat(route.getTotalDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("최대 5일 루트 생성 성공")
    void create_max_days_route() {
        Route route = RouteFixture.createRoute(5);

        assertThat(route.getTotalDays()).isEqualTo(5);
    }

    @Test
    @DisplayName("6일 이상 여행 기간이면 실패")
    void create_route_exceeding_max_days() {
        LocalDate endDate = START.plusDays(5);
        List<RouteDay> days = List.of(RouteFixture.createDay(1, START, 1));

        assertThatThrownBy(() -> Route.create(MEMBER_ID, TITLE, START, endDate, days))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("종료일이 시작일보다 이전이면 실패")
    void create_route_end_before_start() {
        LocalDate endDate = START.minusDays(1);
        List<RouteDay> days = List.of(RouteFixture.createDay(1, START, 1));

        assertThatThrownBy(() -> Route.create(MEMBER_ID, TITLE, START, endDate, days))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("dayNumber가 중복되면 실패")
    void create_route_duplicate_day_number() {
        List<RouteDay> days = List.of(
                RouteFixture.createDay(1, START, 1),
                RouteFixture.createDay(1, START.plusDays(1), 1)
        );

        assertThatThrownBy(() -> Route.create(MEMBER_ID, TITLE, START, START.plusDays(1), days))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_DUPLICATE_DAY_NUMBER);
    }

    @Test
    @DisplayName("제목이 비어 있으면 실패")
    void create_route_blank_title() {
        List<RouteDay> days = List.of(RouteFixture.createDay(1, START, 1));

        assertThatThrownBy(() -> Route.create(MEMBER_ID, "   ", START, START, days))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_INVALID_TITLE);
    }

    @Test
    @DisplayName("제목이 40자 초과이면 실패")
    void create_route_title_too_long() {
        String longTitle = "가".repeat(41);
        List<RouteDay> days = List.of(RouteFixture.createDay(1, START, 1));

        assertThatThrownBy(() -> Route.create(MEMBER_ID, longTitle, START, START, days))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_INVALID_TITLE);
    }

    @Test
    @DisplayName("소유 회원 검증 성공")
    void validate_owner_success() {
        Route route = RouteFixture.createRoute(MEMBER_ID, TITLE, START, START,
                List.of(RouteFixture.createDay(1, START, 1)));

        route.validateOwner(MEMBER_ID);
    }

    @Test
    @DisplayName("소유 회원이 아니면 실패")
    void validate_owner_fail() {
        Route route = RouteFixture.createRoute(MEMBER_ID, TITLE, START, START,
                List.of(RouteFixture.createDay(1, START, 1)));

        assertThatThrownBy(() -> route.validateOwner(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_FORBIDDEN);
    }

    @Test
    @DisplayName("제목 수정 성공")
    void update_title_success() {
        Route route = RouteFixture.createRoute(1);

        route.updateTitle("새 제목");

        assertThat(route.getTitle()).isEqualTo("새 제목");
    }

    @Test
    @DisplayName("제목 수정 시 빈 문자열이면 실패")
    void update_title_blank() {
        Route route = RouteFixture.createRoute(1);

        assertThatThrownBy(() -> route.updateTitle(""))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_INVALID_TITLE);
    }

    @Test
    @DisplayName("Soft Delete 성공")
    void soft_delete_success() {
        Route route = RouteFixture.createRoute(1);

        route.softDelete();

        assertThat(route.isDeleted()).isTrue();
        assertThat(route.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("삭제되지 않은 루트는 isDeleted가 false")
    void is_deleted_false_when_not_deleted() {
        Route route = RouteFixture.createRoute(1);

        assertThat(route.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("루트를 다른 회원에게 복제하면 새 publicId와 memberId를 가진 독립 사본이 생성된다")
    void copy_for_creates_independent_copy() {
        // given
        Route original = RouteFixture.createRoute(
                MEMBER_ID, TITLE, START, START.plusDays(1),
                List.of(
                        RouteFixture.createDay(1, START, 2),
                        RouteFixture.createDay(2, START.plusDays(1), 2)
                )
        );
        original.share();
        UUID newMemberId = UUID.randomUUID();

        // when
        Route copy = original.copyFor(newMemberId);

        // then
        assertThat(copy.getPublicId()).isNotEqualTo(original.getPublicId());
        assertThat(copy.getMemberId()).isEqualTo(newMemberId);
        assertThat(copy.getTitle()).isEqualTo(original.getTitle());
        assertThat(copy.getStartDate()).isEqualTo(original.getStartDate());
        assertThat(copy.getEndDate()).isEqualTo(original.getEndDate());
        assertThat(copy.isShared()).isFalse();
        assertThat(copy.getDays()).hasSize(2);
    }

    @Test
    @DisplayName("복제된 루트의 스팟과 이동정보가 원본과 동일하지만 독립 객체이다")
    void copy_for_deep_copies_spots_and_legs() {
        // given
        Route original = RouteFixture.createRoute(
                MEMBER_ID, TITLE, START, START,
                List.of(RouteFixture.createDay(1, START, 3))
        );
        UUID newMemberId = UUID.randomUUID();

        // when
        Route copy = original.copyFor(newMemberId);

        // then
        RouteDay originalDay = original.getDays().get(0);
        RouteDay copiedDay = copy.getDays().get(0);

        assertThat(copiedDay.getSpots()).hasSize(originalDay.getSpots().size());
        assertThat(copiedDay.getLegs()).hasSize(originalDay.getLegs().size());

        assertThat(copiedDay.getSpots().get(0).getSpotTitle())
                .isEqualTo(originalDay.getSpots().get(0).getSpotTitle());
        assertThat(copiedDay.getSpots().get(0))
                .isNotSameAs(originalDay.getSpots().get(0));
    }
}

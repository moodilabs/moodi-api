package com.moodi.spot.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.spot.support.SpotFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpotTest {

    @Test
    @DisplayName("Spot 생성 시 상태는 TAGGING_PENDING이다")
    void create_sets_tagging_pending_status() {
        Spot spot = Spot.create("123", SpotContentType.TOURIST_ATTRACTION, "서울",
                "종로구", "가회동", "kor_service",
                126.98, 37.58, null, "HS", "HS03", null, null);

        assertThat(spot.getStatus()).isEqualTo(SpotStatus.TAGGING_PENDING);
    }

    @Test
    @DisplayName("숙박 유형은 routeExcluded가 true이다")
    void create_accommodation_sets_route_excluded_true() {
        Spot spot = Spot.create("123", SpotContentType.ACCOMMODATION, "부산",
                "동구", "초량동", "kor_service",
                129.05, 35.15, null, null, null, null, null);

        assertThat(spot.isRouteExcluded()).isTrue();
    }

    @Test
    @DisplayName("음식점 유형은 routeExcluded가 true이다")
    void create_restaurant_sets_route_excluded_true() {
        Spot spot = Spot.create("456", SpotContentType.RESTAURANT, "서울",
                "중구", null, "kor_service",
                126.98, 37.58, "02-1234-5678", null, null, null, null);

        assertThat(spot.isRouteExcluded()).isTrue();
    }

    @Test
    @DisplayName("관광지 유형은 routeExcluded가 false이다")
    void create_tourist_attraction_sets_route_excluded_false() {
        Spot spot = Spot.create("789", SpotContentType.TOURIST_ATTRACTION, "서울",
                "종로구", null, "kor_service",
                126.98, 37.58, null, null, null, null, null);

        assertThat(spot.isRouteExcluded()).isFalse();
    }

    @Test
    @DisplayName("Spot 생성 시 모든 필드가 정상 세팅된다")
    void create_sets_all_fields() {
        Spot spot = Spot.create("2733967", SpotContentType.CULTURAL_FACILITY, "서울",
                "종로구", "가회동", "kor_service",
                126.98, 37.58, "02-123-4567", "HS", "HS03", "HS030200", "https://example.com");

        assertThat(spot.getContentId()).isEqualTo("2733967");
        assertThat(spot.getContentType()).isEqualTo(SpotContentType.CULTURAL_FACILITY);
        assertThat(spot.getArea()).isEqualTo("서울");
        assertThat(spot.getSource()).isEqualTo("kor_service");
        assertThat(spot.getLongitude()).isEqualTo(126.98);
        assertThat(spot.getLatitude()).isEqualTo(37.58);
        assertThat(spot.getTel()).isEqualTo("02-123-4567");
        assertThat(spot.getLclsSystm1()).isEqualTo("HS");
    }

    @Test
    @DisplayName("노출 중인 스팟을 숨기면 HIDDEN이 되고 사유·시각이 남는다")
    void hide_marks_hidden_with_reason() {
        Spot spot = SpotFixture.create();
        spot.publish();
        java.time.LocalDateTime now = java.time.LocalDateTime.of(2026, 8, 10, 9, 0);

        spot.hide("폐업 확인 중", now);

        assertThat(spot.getStatus()).isEqualTo(SpotStatus.HIDDEN);
        assertThat(spot.isPublished()).isFalse();
        assertThat(spot.getStatusReason()).isEqualTo("폐업 확인 중");
        assertThat(spot.getStatusChangedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("태깅 전 스팟은 숨길 수 없다")
    void hide_rejects_tagging_pending() {
        Spot spot = SpotFixture.create();

        assertThatThrownBy(() -> spot.hide("reason", java.time.LocalDateTime.of(2026, 8, 10, 9, 0)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("숨김 해제하면 다시 노출되고 사유가 지워진다")
    void unhide_restores_published() {
        Spot spot = SpotFixture.create();
        spot.publish();
        spot.hide("reason", java.time.LocalDateTime.of(2026, 8, 10, 9, 0));

        spot.unhide(java.time.LocalDateTime.of(2026, 8, 11, 9, 0));

        assertThat(spot.isPublished()).isTrue();
        assertThat(spot.getStatusReason()).isNull();
    }

    @Test
    @DisplayName("삭제된 스팟은 다시 노출할 수 없다")
    void unhide_rejects_deleted() {
        Spot spot = SpotFixture.create();
        spot.publish();
        spot.markDeleted("폐업", java.time.LocalDateTime.of(2026, 8, 10, 9, 0));

        assertThat(spot.getStatus()).isEqualTo(SpotStatus.DELETED);
        assertThatThrownBy(() -> spot.unhide(java.time.LocalDateTime.of(2026, 8, 11, 9, 0)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("숨긴 스팟도 삭제할 수 있지만 이미 삭제된 스팟은 다시 삭제할 수 없다")
    void mark_deleted_from_hidden_but_not_twice() {
        Spot spot = SpotFixture.create();
        spot.publish();
        spot.hide("reason", java.time.LocalDateTime.of(2026, 8, 10, 9, 0));

        spot.markDeleted("폐업", java.time.LocalDateTime.of(2026, 8, 11, 9, 0));

        assertThat(spot.getStatus()).isEqualTo(SpotStatus.DELETED);
        assertThatThrownBy(() -> spot.markDeleted("again", java.time.LocalDateTime.of(2026, 8, 12, 9, 0)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("루트 제외 여부는 노출 상태와 별개로 바뀐다")
    void change_route_exclusion() {
        Spot spot = SpotFixture.create();

        spot.changeRouteExclusion(true);

        assertThat(spot.isRouteExcluded()).isTrue();
        assertThat(spot.getStatus()).isEqualTo(SpotStatus.TAGGING_PENDING);
    }
}

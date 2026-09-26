package com.moodi.spot.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.spot.support.SpotFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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

    @Test
    @DisplayName("Spot 생성 시 moodTaggingStatus는 PENDING이다")
    void create_sets_mood_tagging_pending() {
        Spot spot = SpotFixture.create();

        assertThat(spot.getMoodTaggingStatus()).isEqualTo(MoodTaggingStatus.PENDING);
        assertThat(spot.getMoodTaggingAttemptCount()).isZero();
    }

    @Nested
    @DisplayName("무드 태깅 상태 전이")
    class MoodTaggingStatusTransition {

        private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 10, 0);

        @Test
        @DisplayName("PENDING에서 startTagging하면 PROCESSING이 되고 시도 횟수가 증가한다")
        void start_tagging_from_pending() {
            Spot spot = SpotFixture.create();

            spot.startTagging(NOW);

            assertThat(spot.getMoodTaggingStatus()).isEqualTo(MoodTaggingStatus.PROCESSING);
            assertThat(spot.getMoodTaggingAttemptCount()).isEqualTo(1);
            assertThat(spot.getMoodTaggingProcessingStartedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("PROCESSING에서 completeTagging하면 COMPLETED가 된다")
        void complete_tagging() {
            Spot spot = SpotFixture.create();
            spot.startTagging(NOW);

            spot.completeTagging(NOW.plusMinutes(5));

            assertThat(spot.getMoodTaggingStatus()).isEqualTo(MoodTaggingStatus.COMPLETED);
            assertThat(spot.getMoodTaggingProcessingStartedAt()).isNull();
            assertThat(spot.getMoodTaggingLastError()).isNull();
        }

        @Test
        @DisplayName("일시 오류 시 RETRY_WAIT가 되고 재시도 시각이 설정된다")
        void mark_retry_wait_on_transient_error() {
            Spot spot = SpotFixture.create();
            spot.startTagging(NOW);

            spot.markRetryWait("429 Rate Limit", NOW.plusMinutes(1));

            assertThat(spot.getMoodTaggingStatus()).isEqualTo(MoodTaggingStatus.RETRY_WAIT);
            assertThat(spot.getMoodTaggingLastError()).isEqualTo("429 Rate Limit");
            assertThat(spot.getMoodTaggingNextRetryAt()).isEqualTo(NOW.plusMinutes(2));
        }

        @Test
        @DisplayName("재시도 4회 초과 시 FAILED로 전환된다")
        void mark_retry_wait_exceeds_max_becomes_failed() {
            Spot spot = SpotFixture.create();
            LocalDateTime time = NOW;

            for (int i = 0; i < 3; i++) {
                spot.startTagging(time);
                spot.markRetryWait("timeout", time.plusMinutes(1));
                time = spot.getMoodTaggingNextRetryAt().plusMinutes(1);
            }

            // 4번째 시도
            spot.startTagging(time);
            spot.markRetryWait("timeout", time.plusMinutes(1));

            assertThat(spot.getMoodTaggingStatus()).isEqualTo(MoodTaggingStatus.FAILED);
            assertThat(spot.getMoodTaggingAttemptCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("데이터 문제 시 즉시 FAILED가 된다")
        void mark_failed_on_data_error() {
            Spot spot = SpotFixture.create();
            spot.startTagging(NOW);

            spot.markFailed("이미지 없음", NOW.plusMinutes(1));

            assertThat(spot.getMoodTaggingStatus()).isEqualTo(MoodTaggingStatus.FAILED);
            assertThat(spot.getMoodTaggingLastError()).isEqualTo("이미지 없음");
        }

        @Test
        @DisplayName("FAILED에서 resetForRetry하면 PENDING으로 돌아간다")
        void reset_for_retry() {
            Spot spot = SpotFixture.create();
            spot.startTagging(NOW);
            spot.markFailed("파싱 실패", NOW.plusMinutes(1));

            spot.resetForRetry();

            assertThat(spot.getMoodTaggingStatus()).isEqualTo(MoodTaggingStatus.PENDING);
            assertThat(spot.getMoodTaggingAttemptCount()).isZero();
            assertThat(spot.getMoodTaggingLastError()).isNull();
        }

        @Test
        @DisplayName("COMPLETED에서 startTagging하면 예외가 발생한다")
        void start_tagging_rejects_completed() {
            Spot spot = SpotFixture.create();
            spot.startTagging(NOW);
            spot.completeTagging(NOW.plusMinutes(5));

            assertThatThrownBy(() -> spot.startTagging(NOW.plusMinutes(10)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("PROCESSING이 20분 이상 지속되면 stale로 복구된다")
        void recover_stale_processing() {
            Spot spot = SpotFixture.create();
            spot.startTagging(NOW);

            boolean recovered = spot.recoverStaleProcessing(NOW.plusMinutes(25));

            assertThat(recovered).isTrue();
            assertThat(spot.getMoodTaggingStatus()).isEqualTo(MoodTaggingStatus.RETRY_WAIT);
            assertThat(spot.getMoodTaggingLastError()).contains("타임아웃");
        }

        @Test
        @DisplayName("PROCESSING이 20분 미만이면 복구되지 않는다")
        void recover_stale_processing_not_yet() {
            Spot spot = SpotFixture.create();
            spot.startTagging(NOW);

            boolean recovered = spot.recoverStaleProcessing(NOW.plusMinutes(10));

            assertThat(recovered).isFalse();
            assertThat(spot.getMoodTaggingStatus()).isEqualTo(MoodTaggingStatus.PROCESSING);
        }
    }
}

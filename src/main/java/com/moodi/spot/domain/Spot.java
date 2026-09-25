package com.moodi.spot.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Spot extends BaseEntity {

    private static final int MAX_STATUS_REASON_LENGTH = 200;
    private static final int MAX_TAGGING_ERROR_LENGTH = 500;
    private static final int MAX_RETRY_COUNT = 4;
    private static final long PROCESSING_TIMEOUT_MINUTES = 20;

    private Long id;
    private String contentId;
    private SpotContentType contentType;
    private String area;
    private String district;
    private String neighborhood;
    private String source;
    private Double longitude;
    private Double latitude;
    private String tel;
    private boolean routeExcluded;
    private SpotStatus status;
    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;
    private String homepage;
    private String statusReason;
    private LocalDateTime statusChangedAt;

    private MoodTaggingStatus moodTaggingStatus;
    private int moodTaggingAttemptCount;
    private String moodTaggingLastError;
    private LocalDateTime moodTaggingLastAttemptedAt;
    private LocalDateTime moodTaggingNextRetryAt;
    private LocalDateTime moodTaggingProcessingStartedAt;

    private Spot(String contentId, SpotContentType contentType, String area,
                 String district, String neighborhood, String source,
                 Double longitude, Double latitude, String tel,
                 String lclsSystm1, String lclsSystm2, String lclsSystm3, String homepage) {
        this.contentId = contentId;
        this.contentType = contentType;
        this.area = area;
        this.district = district;
        this.neighborhood = neighborhood;
        this.source = source;
        this.longitude = longitude;
        this.latitude = latitude;
        this.tel = tel;
        this.routeExcluded = contentType.isRouteExcluded();
        this.status = SpotStatus.TAGGING_PENDING;
        this.moodTaggingStatus = MoodTaggingStatus.PENDING;
        this.moodTaggingAttemptCount = 0;
        this.lclsSystm1 = lclsSystm1;
        this.lclsSystm2 = lclsSystm2;
        this.lclsSystm3 = lclsSystm3;
        this.homepage = homepage;
    }

    public static Spot create(String contentId, SpotContentType contentType, String area,
                              String district, String neighborhood, String source,
                              Double longitude, Double latitude, String tel,
                              String lclsSystm1, String lclsSystm2, String lclsSystm3, String homepage) {
        return new Spot(contentId, contentType, area, district, neighborhood, source,
                longitude, latitude, tel, lclsSystm1, lclsSystm2, lclsSystm3, homepage);
    }

    public void updateRegion(String district, String neighborhood) {
        this.district = district;
        this.neighborhood = neighborhood;
    }

    public void publish() {
        if (this.status != SpotStatus.TAGGING_PENDING) {
            throw new IllegalStateException("TAGGING_PENDING 상태에서만 PUBLISHED로 변경할 수 있습니다");
        }
        this.status = SpotStatus.PUBLISHED;
    }

    /** 어드민 숨김. 노출 중인 스팟만 숨길 수 있다. */
    public void hide(String reason, LocalDateTime now) {
        if (this.status != SpotStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        validateReason(reason);
        this.status = SpotStatus.HIDDEN;
        this.statusReason = reason;
        this.statusChangedAt = now;
    }

    /** 숨김 해제. 숨긴 스팟만 다시 노출할 수 있다 — DELETED는 되돌리지 않는다. */
    public void unhide(LocalDateTime now) {
        if (this.status != SpotStatus.HIDDEN) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.status = SpotStatus.PUBLISHED;
        this.statusReason = null;
        this.statusChangedAt = now;
    }

    /** 어드민 삭제 처리. 이미 삭제된 스팟은 다시 지울 수 없다. 원본 행은 남긴다(루트·북마크가 ID로 참조). */
    public void markDeleted(String reason, LocalDateTime now) {
        if (this.status == SpotStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        validateReason(reason);
        this.status = SpotStatus.DELETED;
        this.statusReason = reason;
        this.statusChangedAt = now;
    }

    /** 루트 생성 후보에서 제외/포함. 노출 여부와는 별개다. */
    public void changeRouteExclusion(boolean excluded) {
        this.routeExcluded = excluded;
    }

    public boolean isPublished() {
        return this.status == SpotStatus.PUBLISHED;
    }

    /**
     * 배치가 태깅 작업을 선점한다. PENDING 또는 재시도 시각이 지난 RETRY_WAIT에서만 전이 가능.
     * DB에 커밋한 뒤 LLM 호출을 시작해야 한다.
     */
    public void startTagging(LocalDateTime now) {
        if (this.moodTaggingStatus != MoodTaggingStatus.PENDING
                && this.moodTaggingStatus != MoodTaggingStatus.RETRY_WAIT) {
            throw new IllegalStateException(
                    "PENDING 또는 RETRY_WAIT 상태에서만 태깅을 시작할 수 있습니다. 현재: " + this.moodTaggingStatus);
        }
        this.moodTaggingStatus = MoodTaggingStatus.PROCESSING;
        this.moodTaggingAttemptCount++;
        this.moodTaggingProcessingStartedAt = now;
        this.moodTaggingNextRetryAt = null;
    }

    /** 태깅 성공. SpotMood 저장과 함께 호출한다. */
    public void completeTagging(LocalDateTime now) {
        if (this.moodTaggingStatus != MoodTaggingStatus.PROCESSING) {
            throw new IllegalStateException(
                    "PROCESSING 상태에서만 태깅을 완료할 수 있습니다. 현재: " + this.moodTaggingStatus);
        }
        this.moodTaggingStatus = MoodTaggingStatus.COMPLETED;
        this.moodTaggingLastAttemptedAt = now;
        this.moodTaggingLastError = null;
        this.moodTaggingProcessingStartedAt = null;
    }

    /**
     * 일시 오류(timeout, 429, 5xx). 재시도 횟수가 MAX_RETRY_COUNT 이상이면 FAILED로 전환한다.
     * 재시도 간격: 1회→1분, 2회→5분, 3회→30분, 4회 이상→FAILED.
     */
    public void markRetryWait(String error, LocalDateTime now) {
        if (this.moodTaggingStatus != MoodTaggingStatus.PROCESSING) {
            throw new IllegalStateException(
                    "PROCESSING 상태에서만 재시도 대기로 전환할 수 있습니다. 현재: " + this.moodTaggingStatus);
        }
        this.moodTaggingLastAttemptedAt = now;
        this.moodTaggingLastError = truncateError(error);
        this.moodTaggingProcessingStartedAt = null;

        if (this.moodTaggingAttemptCount >= MAX_RETRY_COUNT) {
            this.moodTaggingStatus = MoodTaggingStatus.FAILED;
        } else {
            this.moodTaggingStatus = MoodTaggingStatus.RETRY_WAIT;
            this.moodTaggingNextRetryAt = now.plusMinutes(retryDelayMinutes());
        }
    }

    /** 데이터 자체 문제(이미지 없음, 파싱 실패 등). 재시도해도 해결되지 않으므로 즉시 FAILED. */
    public void markFailed(String error, LocalDateTime now) {
        if (this.moodTaggingStatus != MoodTaggingStatus.PROCESSING) {
            throw new IllegalStateException(
                    "PROCESSING 상태에서만 실패 처리할 수 있습니다. 현재: " + this.moodTaggingStatus);
        }
        this.moodTaggingStatus = MoodTaggingStatus.FAILED;
        this.moodTaggingLastAttemptedAt = now;
        this.moodTaggingLastError = truncateError(error);
        this.moodTaggingProcessingStartedAt = null;
    }

    /** 어드민이 FAILED 스팟을 수동 재시도 대상으로 되돌린다. 시도 횟수를 초기화한다. */
    public void resetForRetry() {
        if (this.moodTaggingStatus != MoodTaggingStatus.FAILED) {
            throw new IllegalStateException(
                    "FAILED 상태에서만 재시도할 수 있습니다. 현재: " + this.moodTaggingStatus);
        }
        this.moodTaggingStatus = MoodTaggingStatus.PENDING;
        this.moodTaggingAttemptCount = 0;
        this.moodTaggingLastError = null;
        this.moodTaggingNextRetryAt = null;
        this.moodTaggingProcessingStartedAt = null;
    }

    /**
     * PROCESSING 상태가 일정 시간 이상 지속되면 서버 중단으로 간주하고 복구한다.
     * 재시도 횟수가 MAX_RETRY_COUNT 이상이면 FAILED로 전환한다.
     * @return 복구 대상이었으면 true
     */
    public boolean recoverStaleProcessing(LocalDateTime now) {
        if (this.moodTaggingStatus != MoodTaggingStatus.PROCESSING) {
            return false;
        }
        if (this.moodTaggingProcessingStartedAt != null
                && this.moodTaggingProcessingStartedAt.plusMinutes(PROCESSING_TIMEOUT_MINUTES).isBefore(now)) {
            this.moodTaggingLastError = "PROCESSING 타임아웃 (" + PROCESSING_TIMEOUT_MINUTES + "분 초과)";
            this.moodTaggingLastAttemptedAt = now;
            this.moodTaggingProcessingStartedAt = null;

            if (this.moodTaggingAttemptCount >= MAX_RETRY_COUNT) {
                this.moodTaggingStatus = MoodTaggingStatus.FAILED;
            } else {
                this.moodTaggingStatus = MoodTaggingStatus.RETRY_WAIT;
                this.moodTaggingNextRetryAt = now.plusMinutes(retryDelayMinutes());
            }
            return true;
        }
        return false;
    }

    private long retryDelayMinutes() {
        return switch (this.moodTaggingAttemptCount) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 30;
            default -> 60;
        };
    }

    private static String truncateError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > MAX_TAGGING_ERROR_LENGTH
                ? error.substring(0, MAX_TAGGING_ERROR_LENGTH) : error;
    }

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank() || reason.length() > MAX_STATUS_REASON_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}

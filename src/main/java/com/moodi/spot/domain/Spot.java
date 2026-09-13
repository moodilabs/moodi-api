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

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank() || reason.length() > MAX_STATUS_REASON_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}

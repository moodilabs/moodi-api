package com.moodi.discovery.domain;

import com.moodi.shared.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 추천 결과 1건. {@code fallback}은 지역 조건을 해제하고 뽑은 대체 추천(DSC-05-02)인지 구분한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PickResultSpot extends BaseEntity {

    private UUID id;
    private UUID pickRequestId;
    private Long spotId;
    private int rank;
    private double similarity;
    private boolean fallback;

    private PickResultSpot(UUID pickRequestId, Long spotId, int rank, double similarity, boolean fallback) {
        this.pickRequestId = pickRequestId;
        this.spotId = spotId;
        this.rank = rank;
        this.similarity = similarity;
        this.fallback = fallback;
    }

    public static PickResultSpot of(UUID pickRequestId, Long spotId, int rank, double similarity, boolean fallback) {
        return new PickResultSpot(pickRequestId, spotId, rank, similarity, fallback);
    }
}

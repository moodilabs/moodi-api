package com.moodi.discovery.domain;

import com.moodi.shared.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 추천 요청 1건 (DSC-04 → DSC-05). 사진 1장과 선택 지역, 그리고 그 결과를 묶는다.
 *
 * <p>사진이 1장이라 이미지용 자식 테이블을 두지 않고 {@code imageKey} 컬럼 하나로 갖는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PickRequest extends BaseEntity {

    private UUID id;
    private UUID memberId;
    private String imageKey;

    private PickRequest(UUID memberId, String imageKey) {
        this.memberId = memberId;
        this.imageKey = imageKey;
    }

    public static PickRequest create(UUID memberId, String imageKey) {
        return new PickRequest(memberId, imageKey);
    }

    public boolean isOwnedBy(UUID candidateMemberId) {
        return memberId.equals(candidateMemberId);
    }
}

package com.moodi.spot.domain;

import com.moodi.shared.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseEntity {

    private Long id;
    private UUID memberId;
    private Long spotId;

    private Bookmark(UUID memberId, Long spotId) {
        this.memberId = memberId;
        this.spotId = spotId;
    }

    public static Bookmark create(UUID memberId, Long spotId) {
        return new Bookmark(memberId, spotId);
    }
}

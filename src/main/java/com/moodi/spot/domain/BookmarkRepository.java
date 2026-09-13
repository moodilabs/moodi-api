package com.moodi.spot.domain;

import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository {

    Bookmark save(Bookmark bookmark);

    Optional<Bookmark> findByMemberIdAndSpotId(UUID memberId, Long spotId);

    boolean existsByMemberIdAndSpotId(UUID memberId, Long spotId);

    void delete(Bookmark bookmark);

    /** 회원 탈퇴 전용. */
    void deleteByMemberId(UUID memberId);
}

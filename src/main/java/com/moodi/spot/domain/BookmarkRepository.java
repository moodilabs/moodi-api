package com.moodi.spot.domain;

import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository {

    Bookmark save(Bookmark bookmark);

    Optional<Bookmark> findByMemberIdAndSpotId(UUID memberId, Long spotId);

    void delete(Bookmark bookmark);
}

package com.moodi.spot.application;

import com.moodi.spot.application.dto.BookmarkSpotRow;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface BookmarkQueryRepository {

    List<BookmarkSpotRow> findByMemberLatest(UUID memberId, String area, List<String> moodTagKeys,
                                              Long cursorId, java.time.LocalDateTime cursorCreatedAt,
                                              int size);

    List<BookmarkSpotRow> findByMemberPopular(UUID memberId, String area, List<String> moodTagKeys,
                                               Long cursorSpotId, Long cursorBookmarkCount,
                                               int size);

    void flush();

    long countBySpotId(Long spotId);

    Map<Long, Long> countBySpotIds(List<Long> spotIds);

    Set<Long> findBookmarkedSpotIds(UUID memberId, List<Long> spotIds);

    int deleteByMemberIdAndSpotIds(UUID memberId, List<Long> spotIds);
}

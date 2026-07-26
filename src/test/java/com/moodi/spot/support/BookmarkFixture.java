package com.moodi.spot.support;

import com.moodi.spot.domain.Bookmark;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

public class BookmarkFixture {

    private static final UUID DEFAULT_MEMBER_ID = UUID.randomUUID();
    private static final Long DEFAULT_SPOT_ID = 1L;

    public static Bookmark create(UUID memberId, Long spotId) {
        return Bookmark.create(memberId, spotId);
    }

    public static Bookmark createWithId(Long id, UUID memberId, Long spotId) {
        Bookmark bookmark = Bookmark.create(memberId, spotId);
        ReflectionTestUtils.setField(bookmark, "id", id);
        return bookmark;
    }

    private BookmarkFixture() {
    }
}

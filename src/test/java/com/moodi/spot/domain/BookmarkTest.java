package com.moodi.spot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkTest {

    @Test
    @DisplayName("북마크를 생성하면 memberId와 spotId가 설정된다")
    void create_success() {
        UUID memberId = UUID.randomUUID();
        Long spotId = 1L;

        Bookmark bookmark = Bookmark.create(memberId, spotId);

        assertThat(bookmark.getMemberId()).isEqualTo(memberId);
        assertThat(bookmark.getSpotId()).isEqualTo(spotId);
        assertThat(bookmark.getId()).isNull();
    }
}

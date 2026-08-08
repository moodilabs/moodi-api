package com.moodi.discovery.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedCursorTest {

    private static final FeedCursor CURSOR = new FeedCursor(
            "seed-a", LocalDateTime.of(2026, 8, 3, 12, 0), 1, 3L, "9e107d9d", 42L);

    @Test
    @DisplayName("인코딩한 커서를 디코딩하면 원래 정렬 키가 복원된다")
    void encode_decode_round_trip() {
        FeedCursor decoded = FeedCursor.decode(CURSOR.encode());

        assertThat(decoded).isEqualTo(CURSOR);
    }

    @Test
    @DisplayName("커서는 base64라 원문이 그대로 노출되지 않는다")
    void encoded_cursor_is_opaque() {
        assertThat(CURSOR.encode()).doesNotContain("seed-a", "|");
    }

    @Test
    @DisplayName("형식이 깨진 커서는 400으로 거른다")
    void decode_rejects_malformed_cursor() {
        assertThatThrownBy(() -> FeedCursor.decode("not-a-cursor"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("필드 수가 모자란 커서는 400으로 거른다")
    void decode_rejects_cursor_with_missing_fields() {
        String truncated = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("seed-a|2026-08-03T12:00".getBytes());

        assertThatThrownBy(() -> FeedCursor.decode(truncated))
                .isInstanceOf(BusinessException.class);
    }
}

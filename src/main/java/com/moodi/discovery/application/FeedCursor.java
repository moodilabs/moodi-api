package com.moodi.discovery.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * B+ 정렬의 keyset 커서. 마지막 행의 정렬 키를 전부 담아 다음 페이지의 시작점을 특정한다.
 *
 * <p>{@code sessionAt}은 설계 문서의 커서 형식에 없던 필드다. 피드 조회가 노출 이력을 쓰기 때문에
 * 페이지를 넘기는 도중 {@code seen} 값이 0에서 1로 바뀌고, 그러면 이미 내보낸 스팟이 정렬 뒤쪽으로
 * 이동해 다음 페이지에 다시 딸려 나온다. 세션 시작 시각을 커서에 실어 "이 시각 이전의 노출만 본 것으로
 * 친다"고 고정해야 스크롤 중 순서가 유지된다.
 *
 * <p>{@code rankScore}는 회원이면 무드 일치 수, 비회원이면 북마크 수다. 둘 다 DESC 정렬이라
 * 커서 형식을 하나로 유지할 수 있다.
 */
public record FeedCursor(
        String seed,
        LocalDateTime sessionAt,
        int seen,
        long rankScore,
        String shuffleKey,
        long spotId
) {

    private static final String DELIMITER = "|";
    private static final int FIELD_COUNT = 6;

    public static FeedCursor decode(String encoded) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\" + DELIMITER, FIELD_COUNT);
            if (parts.length != FIELD_COUNT) {
                throw new IllegalArgumentException("cursor field count mismatch: " + parts.length);
            }
            return new FeedCursor(
                    parts[0],
                    LocalDateTime.parse(parts[1]),
                    Integer.parseInt(parts[2]),
                    Long.parseLong(parts[3]),
                    parts[4],
                    Long.parseLong(parts[5])
            );
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    public String encode() {
        String raw = String.join(DELIMITER,
                seed, sessionAt.toString(), String.valueOf(seen),
                String.valueOf(rankScore), shuffleKey, String.valueOf(spotId));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}

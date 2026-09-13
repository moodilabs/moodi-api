package com.moodi.admin.application;

import com.moodi.admin.application.dto.ApiRequestLogFilter;
import com.moodi.admin.application.dto.ApiRequestLogItem;
import com.moodi.admin.domain.ApiRequestLog;
import com.moodi.admin.domain.ApiRequestLogRepository;
import com.moodi.shared.response.CursorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiRequestLogServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-09-13T10:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);

    @Mock
    private ApiRequestLogRepository apiRequestLogRepository;

    @Mock
    private ApiRequestLogQueryRepository apiRequestLogQueryRepository;

    private ApiRequestLogService service;

    @BeforeEach
    void setUp() {
        service = new ApiRequestLogService(apiRequestLogRepository, apiRequestLogQueryRepository, FIXED_CLOCK, 30);
    }

    @Test
    @DisplayName("요청 로그는 현재 시각으로 저장되고 경로는 300자로 잘린다")
    void record_saves_with_now_and_truncates_path() {
        UUID memberId = UUID.randomUUID();

        service.record(memberId, "GET", "/api/v1/" + "x".repeat(400), 200, 12, "req-1");

        ArgumentCaptor<ApiRequestLog> captor = ArgumentCaptor.forClass(ApiRequestLog.class);
        verify(apiRequestLogRepository).save(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(memberId);
        assertThat(captor.getValue().getPath()).hasSize(ApiRequestLog.MAX_PATH_LENGTH);
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("limit+1건을 읽어 다음 페이지 여부를 판단하고 마지막 ID를 커서로 준다")
    void get_logs_pages_by_cursor() {
        ApiRequestLogFilter filter = new ApiRequestLogFilter(null, null, null, null);
        when(apiRequestLogQueryRepository.findAll(eq(filter), any(), eq(3))).thenReturn(items(10L, 9L, 8L));

        CursorResponse<ApiRequestLogItem> page = service.getLogs(filter, null, 2);

        assertThat(page.items()).extracting(ApiRequestLogItem::id).containsExactly(10L, 9L);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo("9");
    }

    @Test
    @DisplayName("마지막 페이지는 nextCursor 없이 hasNext=false")
    void get_logs_last_page() {
        ApiRequestLogFilter filter = new ApiRequestLogFilter(null, null, null, null);
        when(apiRequestLogQueryRepository.findAll(eq(filter), eq(9L), eq(51))).thenReturn(items(8L));

        CursorResponse<ApiRequestLogItem> page = service.getLogs(filter, 9L, 50);

        assertThat(page.items()).hasSize(1);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    @DisplayName("보관 일수가 지난 로그를 지운다")
    void purge_deletes_before_retention() {
        when(apiRequestLogRepository.deleteByCreatedAtBefore(NOW.minusDays(30))).thenReturn(7L);

        assertThat(service.purgeExpired()).isEqualTo(7L);
    }

    private static List<ApiRequestLogItem> items(Long... ids) {
        return IntStream.range(0, ids.length)
                .mapToObj(i -> new ApiRequestLogItem(ids[i], null, null, null, "GET", "/api/v1/feed", 200, 10, null, NOW))
                .toList();
    }
}

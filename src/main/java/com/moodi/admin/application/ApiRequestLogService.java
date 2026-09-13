package com.moodi.admin.application;

import com.moodi.admin.application.dto.ApiRequestLogFilter;
import com.moodi.admin.application.dto.ApiRequestLogItem;
import com.moodi.admin.domain.ApiRequestLog;
import com.moodi.admin.domain.ApiRequestLogRepository;
import com.moodi.shared.response.CursorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 앱 API 요청 로그 기록·조회·정리. 기록은 인터셉터가 요청 완료 후 호출한다.
 */
@Service
@Transactional
public class ApiRequestLogService {

    public static final int MAX_PAGE_SIZE = 200;

    private final ApiRequestLogRepository apiRequestLogRepository;
    private final ApiRequestLogQueryRepository apiRequestLogQueryRepository;
    private final Clock clock;
    private final int retentionDays;

    public ApiRequestLogService(ApiRequestLogRepository apiRequestLogRepository,
                                ApiRequestLogQueryRepository apiRequestLogQueryRepository,
                                Clock clock,
                                @Value("${admin.api-log.retention-days:30}") int retentionDays) {
        this.apiRequestLogRepository = apiRequestLogRepository;
        this.apiRequestLogQueryRepository = apiRequestLogQueryRepository;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    /** 별도 트랜잭션 — 본 요청은 이미 끝난 뒤라 기록 실패가 요청 결과를 바꾸면 안 된다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID memberId, String method, String path, int statusCode, int durationMs, String requestId) {
        apiRequestLogRepository.save(ApiRequestLog.record(memberId, method, path, statusCode, durationMs, requestId,
                LocalDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    public CursorResponse<ApiRequestLogItem> getLogs(ApiRequestLogFilter filter, Long cursorId, int size) {
        int limit = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        List<ApiRequestLogItem> items = apiRequestLogQueryRepository.findAll(filter, cursorId, limit + 1);
        if (items.isEmpty()) {
            return CursorResponse.empty();
        }
        boolean hasNext = items.size() > limit;
        List<ApiRequestLogItem> page = hasNext ? items.subList(0, limit) : items;
        return CursorResponse.of(page, hasNext ? String.valueOf(page.getLast().id()) : null, hasNext);
    }

    /** 보관 기간(`admin.api-log.retention-days`)이 지난 로그를 지운다. */
    public long purgeExpired() {
        return apiRequestLogRepository.deleteByCreatedAtBefore(LocalDateTime.now(clock).minusDays(retentionDays));
    }
}

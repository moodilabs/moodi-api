package com.moodi.admin.application;

import com.moodi.admin.application.dto.AdminAuditLogItem;
import com.moodi.admin.domain.AdminAuditLog;
import com.moodi.admin.domain.AdminAuditLogRepository;
import com.moodi.shared.response.CursorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 감사 로그 기록·조회. 기록은 인터셉터가 요청 완료 후 호출한다.
 * 조회는 최근 100건씩 ID 커서로 넘긴다 — 감사 로그는 시간순 열람이 전부라 필터는 관리자 ID뿐이다.
 */
@Service
@Transactional
public class AdminAuditService {

    public static final int PAGE_SIZE = 100;

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final Clock clock;

    public AdminAuditService(AdminAuditLogRepository adminAuditLogRepository, Clock clock) {
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.clock = clock;
    }

    /** 별도 트랜잭션으로 남긴다 — 본 요청은 이미 커밋된 뒤라 감사 기록 실패가 요청 결과를 바꾸면 안 된다. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void record(UUID adminId, String method, String path, int statusCode, String requestId) {
        adminAuditLogRepository.save(
                AdminAuditLog.record(adminId, method, path, statusCode, requestId, LocalDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    public CursorResponse<AdminAuditLogItem> getLogs(UUID adminId, Long cursorId) {
        List<AdminAuditLog> logs = find(adminId, cursorId);
        if (logs.isEmpty()) {
            return CursorResponse.empty();
        }
        boolean hasNext = logs.size() == PAGE_SIZE;
        List<AdminAuditLogItem> items = logs.stream().map(AdminAuditLogItem::from).toList();
        return CursorResponse.of(items, hasNext ? String.valueOf(logs.getLast().getId()) : null, hasNext);
    }

    private List<AdminAuditLog> find(UUID adminId, Long cursorId) {
        if (adminId != null && cursorId != null) {
            return adminAuditLogRepository.findTop100ByAdminIdAndIdLessThanOrderByIdDesc(adminId, cursorId);
        }
        if (adminId != null) {
            return adminAuditLogRepository.findTop100ByAdminIdOrderByIdDesc(adminId);
        }
        if (cursorId != null) {
            return adminAuditLogRepository.findTop100ByIdLessThanOrderByIdDesc(cursorId);
        }
        return adminAuditLogRepository.findTop100ByOrderByIdDesc();
    }
}

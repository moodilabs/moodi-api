package com.moodi.admin.application;

import com.moodi.admin.application.dto.AdminAuditLogItem;
import com.moodi.admin.domain.AdminAuditLog;
import com.moodi.admin.domain.AdminAuditLogRepository;
import com.moodi.shared.response.CursorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-10T05:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    private AdminAuditService adminAuditService;

    @BeforeEach
    void setUp() {
        adminAuditService = new AdminAuditService(adminAuditLogRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("기록 시 관리자·메서드·경로·상태·requestId·시각을 저장한다")
    void record_saves_log() {
        adminAuditService.record(ADMIN_ID, "PATCH", "/api/admin/members/x/status", 204, "req-1");

        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(captor.capture());
        AdminAuditLog saved = captor.getValue();
        assertThat(saved.getAdminId()).isEqualTo(ADMIN_ID);
        assertThat(saved.getMethod()).isEqualTo("PATCH");
        assertThat(saved.getPath()).isEqualTo("/api/admin/members/x/status");
        assertThat(saved.getStatusCode()).isEqualTo(204);
        assertThat(saved.getRequestId()).isEqualTo("req-1");
        assertThat(saved.getCreatedAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
    }

    @Test
    @DisplayName("100건이 꽉 차면 마지막 ID를 커서로 준다")
    void get_logs_builds_cursor_when_page_is_full() {
        List<AdminAuditLog> full = LongStream.iterate(200, id -> id - 1).limit(100).mapToObj(this::log).toList();
        when(adminAuditLogRepository.findTop100ByOrderByIdDesc()).thenReturn(full);

        CursorResponse<AdminAuditLogItem> result = adminAuditService.getLogs(null, null);

        assertThat(result.items()).hasSize(100);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("101");
    }

    @Test
    @DisplayName("관리자 ID와 커서를 함께 주면 해당 조회를 쓴다")
    void get_logs_with_admin_and_cursor() {
        when(adminAuditLogRepository.findTop100ByAdminIdAndIdLessThanOrderByIdDesc(ADMIN_ID, 50L))
                .thenReturn(List.of(log(49)));

        CursorResponse<AdminAuditLogItem> result = adminAuditService.getLogs(ADMIN_ID, 50L);

        assertThat(result.items()).extracting(AdminAuditLogItem::id).containsExactly(49L);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    private AdminAuditLog log(long id) {
        AdminAuditLog log = AdminAuditLog.record(ADMIN_ID, "POST", "/api/admin/notices", 201, "r",
                LocalDateTime.now(FIXED_CLOCK));
        ReflectionTestUtils.setField(log, "id", id);
        return log;
    }
}

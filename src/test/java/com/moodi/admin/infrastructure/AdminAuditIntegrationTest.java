package com.moodi.admin.infrastructure;

import com.moodi.admin.domain.AdminAuditLog;
import com.moodi.admin.domain.AdminAuditLogRepository;
import com.moodi.shared.auth.AdminRole;
import com.moodi.shared.auth.JwtProvider;
import com.moodi.support.domain.Notice;
import com.moodi.support.domain.NoticeRepository;
import com.moodi.support.support.NoticeFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 인터셉터 체인(관리자 인증 → 핸들러 → 감사 기록)이 실제 MVC 설정에서 이어지는지 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    @Test
    @DisplayName("관리자 변경 요청이 성공하면 감사 로그가 남고, 조회 요청은 남지 않는다")
    void mutation_is_audited_but_query_is_not() throws Exception {
        UUID adminId = UUID.randomUUID();
        String token = jwtProvider.issueAdminAccessToken(adminId, AdminRole.OPERATOR);
        Notice notice = noticeRepository.save(NoticeFixture.create());

        mockMvc.perform(get("/api/admin/notices/{id}", notice.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/notices/{id}/visibility", notice.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\": false}"))
                .andExpect(status().isNoContent());

        List<AdminAuditLog> logs = adminAuditLogRepository.findTop100ByAdminIdOrderByIdDesc(adminId);
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().getMethod()).isEqualTo("PATCH");
        assertThat(logs.getFirst().getPath()).isEqualTo("/api/admin/notices/" + notice.getId() + "/visibility");
        assertThat(logs.getFirst().getStatusCode()).isEqualTo(204);
    }

    @Test
    @DisplayName("회원 토큰으로는 관리자 변경 요청이 거부되고 감사 로그도 남지 않는다")
    void member_token_is_rejected_without_audit() throws Exception {
        UUID memberId = UUID.randomUUID();
        String memberToken = jwtProvider.issueAccessToken(memberId);

        mockMvc.perform(patch("/api/admin/notices/{id}/visibility", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\": false}"))
                .andExpect(status().isUnauthorized());

        assertThat(adminAuditLogRepository.findTop100ByAdminIdOrderByIdDesc(memberId)).isEmpty();
    }
}

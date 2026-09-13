package com.moodi.admin.infrastructure;

import com.moodi.admin.application.ApiRequestLogQueryRepository;
import com.moodi.admin.application.dto.ApiRequestLogFilter;
import com.moodi.admin.application.dto.ApiRequestLogItem;
import com.moodi.shared.auth.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** 실제 MVC 설정에서 앱 API 요청이 회원 ID와 함께 기록되고, 관리자 API는 기록되지 않는지 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiRequestLogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ApiRequestLogQueryRepository apiRequestLogQueryRepository;

    @Test
    @DisplayName("앱 API 요청은 회원 ID와 함께 기록되고 관리자 API 요청은 기록되지 않는다")
    void app_request_is_logged_with_member_id() throws Exception {
        UUID memberId = UUID.randomUUID();
        String token = jwtProvider.issueAccessToken(memberId);

        mockMvc.perform(get("/api/v1/notices").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
        mockMvc.perform(get("/api/admin/notices"));

        List<ApiRequestLogItem> logs = apiRequestLogQueryRepository.findAll(
                new ApiRequestLogFilter(memberId, null, null, null), null, 10);
        assertThat(logs).hasSize(1);
        assertThat(logs.getFirst().method()).isEqualTo("GET");
        assertThat(logs.getFirst().path()).isEqualTo("/api/v1/notices");
        assertThat(logs.getFirst().memberNickname()).isNull();
        assertThat(apiRequestLogQueryRepository.findAll(new ApiRequestLogFilter(null, null, "/api/admin", null), null, 10))
                .isEmpty();
    }

    @Test
    @DisplayName("인증 실패(401)로 끝난 요청도 회원 ID 없이 기록된다")
    void unauthorized_request_is_logged_without_member() throws Exception {
        mockMvc.perform(get("/api/v1/members/me").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"));

        List<ApiRequestLogItem> logs = apiRequestLogQueryRepository.findAll(
                new ApiRequestLogFilter(null, "GET", "/api/v1/members/me", 4), null, 10);
        assertThat(logs).isNotEmpty();
        assertThat(logs.getFirst().memberId()).isNull();
        assertThat(logs.getFirst().statusCode()).isEqualTo(401);
    }
}

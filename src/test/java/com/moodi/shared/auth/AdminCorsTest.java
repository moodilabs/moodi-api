package com.moodi.shared.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** `admin.cors.allowed-origins`(테스트: https://admin.moodi.kr)에 맞춰 `/api/admin/**`만 CORS가 열리는지 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("관리자 프론트 오리진의 preflight는 허용된다")
    void preflight_from_admin_origin_is_allowed() throws Exception {
        mockMvc.perform(options("/api/admin/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://admin.moodi.kr")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://admin.moodi.kr"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    @DisplayName("허용되지 않은 오리진의 preflight는 거부된다")
    void preflight_from_other_origin_is_rejected() throws Exception {
        mockMvc.perform(options("/api/admin/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("앱 API에는 관리자 오리진이라도 CORS 허용 헤더가 붙지 않는다")
    void app_api_has_no_cors_for_admin_origin() throws Exception {
        mockMvc.perform(options("/api/v1/notices")
                        .header(HttpHeaders.ORIGIN, "https://admin.moodi.kr")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}

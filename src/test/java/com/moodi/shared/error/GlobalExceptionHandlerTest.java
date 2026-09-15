package com.moodi.shared.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 잘못된 형식의 요청이 500 으로 나가던 것을 막는다.
 * <p>
 * 이 어드바이스는 {@code ResponseEntityExceptionHandler} 를 상속하지 않아, 표준 MVC 예외를 명시적으로
 * 받지 않으면 마지막 {@code Exception} 핸들러가 삼켜 전부 500 이 됐다
 * (실측: {@code /api/spots/abc} · {@code ?sort=LATEST} · {@code ?size=-1} · POST 로 조회 등 8종).
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("경로 변수 타입이 맞지 않으면 400")
    void path_variable_type_mismatch_returns_400() throws Exception {
        mockMvc.perform(get("/sample/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("enum 파라미터 값이 잘못되면 400")
    void enum_parameter_mismatch_returns_400() throws Exception {
        mockMvc.perform(get("/sample").param("sort", "LATEST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("필수 파라미터가 빠지면 400")
    void missing_required_parameter_returns_400() throws Exception {
        mockMvc.perform(get("/sample/required"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("지원하지 않는 메서드는 405")
    void unsupported_method_returns_405() throws Exception {
        mockMvc.perform(post("/sample"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("비즈니스 예외는 지정한 상태와 코드로 나간다")
    void business_exception_keeps_status_and_code() throws Exception {
        mockMvc.perform(get("/sample/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.SPOT_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("예상치 못한 예외만 500")
    void unexpected_exception_returns_500() throws Exception {
        mockMvc.perform(get("/sample/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
    }

    enum SampleSort {
        BEST_MATCH, MOST_SAVED
    }

    @RestController
    static class SampleController {

        @GetMapping("/sample")
        String search(@RequestParam(required = false, defaultValue = "BEST_MATCH") SampleSort sort) {
            return sort.name();
        }

        @GetMapping("/sample/required")
        String required(@RequestParam Long spotId) {
            return String.valueOf(spotId);
        }

        @GetMapping("/sample/business")
        String business() {
            throw new BusinessException(ErrorCode.SPOT_NOT_FOUND);
        }

        @GetMapping("/sample/boom")
        String boom() {
            throw new IllegalStateException("unexpected");
        }

        @GetMapping("/sample/{spotId}")
        String detail(@PathVariable Long spotId) {
            return String.valueOf(spotId);
        }
    }
}

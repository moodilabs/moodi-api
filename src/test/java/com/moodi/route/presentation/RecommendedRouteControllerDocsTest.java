package com.moodi.route.presentation;
import com.moodi.route.application.RecommendedRouteService;
import com.moodi.shared.support.RestDocsSupport;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class RecommendedRouteControllerDocsTest extends RestDocsSupport {
    private final RecommendedRouteService service = mock(RecommendedRouteService.class);
    protected Object initController() { return new RecommendedRouteController(service); }
    @Test @DisplayName("앱 운영 콘텐츠 조회 계약")
    void list_published_content() throws Exception {
        when(service.getPublished()).thenReturn(List.of(new RecommendedRouteService.View(1L, "Seoul walk", "https://example.com/seoul.jpg", "Seoul", true, 0, List.of(new RecommendedRouteService.Command.Stop(1L, 1, 1)))));
        mockMvc.perform(get("/api/v1/feed/recommended-routes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1)).andDo(document("curation/recommended-routes"));
    }
}

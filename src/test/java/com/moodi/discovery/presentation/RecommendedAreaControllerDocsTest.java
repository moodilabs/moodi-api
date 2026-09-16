package com.moodi.discovery.presentation;
import com.moodi.discovery.application.RecommendedAreaService;
import com.moodi.shared.support.RestDocsSupport;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class RecommendedAreaControllerDocsTest extends RestDocsSupport {
    private final RecommendedAreaService service = mock(RecommendedAreaService.class);
    protected Object initController() { return new RecommendedAreaController(service); }
    @Test @DisplayName("앱 운영 콘텐츠 조회 계약")
    void list_published_content() throws Exception {
        when(service.getPublished()).thenReturn(List.of(new RecommendedAreaService.View(1L, com.moodi.discovery.domain.PickAreaLevel.REGION, "Seoul", null, null, "Seoul", 0)));
        mockMvc.perform(get("/api/v1/picks/recommended-areas")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1)).andDo(document("curation/recommended-areas"));
    }
}

package com.moodi.member.presentation;
import com.moodi.member.application.SurveyImageService;
import com.moodi.shared.support.RestDocsSupport;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class SurveyImageControllerDocsTest extends RestDocsSupport {
    private final SurveyImageService service = mock(SurveyImageService.class);
    protected Object initController() { return new SurveyImageController(service); }
    @Test @DisplayName("앱 운영 콘텐츠 조회 계약")
    void list_published_content() throws Exception {
        when(service.getPublished()).thenReturn(List.of(new SurveyImageService.View(1L, com.moodi.shared.mood.MoodTag.NATURE, 1L, "https://example.com/forest.jpg", 0)));
        mockMvc.perform(get("/api/v1/members/survey-images")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1)).andDo(document("curation/survey-images"));
    }
}

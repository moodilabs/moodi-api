package com.moodi.member.presentation.admin;
import com.moodi.member.application.SurveyImageService;
import com.moodi.shared.support.AdminRestDocsSupport;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class AdminSurveyImageControllerDocsTest extends AdminRestDocsSupport {
    private final SurveyImageService service = mock(SurveyImageService.class);
    protected Object initController() { return new AdminSurveyImageController(service); }
    @Test @DisplayName("관리 콘텐츠 등록·조회·수정·정렬·삭제 계약")
    void manage_content() throws Exception {
        when(service.create(any())).thenReturn(1L);
        when(service.getAll()).thenReturn(List.of(new SurveyImageService.View(1L, com.moodi.shared.mood.MoodTag.NATURE, 1L, "https://example.com/forest.jpg", 0)));
        when(service.get(1L)).thenReturn(new SurveyImageService.View(1L, com.moodi.shared.mood.MoodTag.NATURE, 1L, "https://example.com/forest.jpg", 0));
        String body = """
                {"mood":"NATURE","spotId":1,"imageUrl":"https://example.com/forest.jpg","sortOrder":0}
                """;
        mockMvc.perform(post("/api/admin/survey-images").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value(1)).andDo(document("admin/survey-images/create"));
        mockMvc.perform(get("/api/admin/survey-images"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(1)).andDo(document("admin/survey-images/list"));
        mockMvc.perform(get("/api/admin/survey-images/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1)).andDo(document("admin/survey-images/detail"));
        mockMvc.perform(put("/api/admin/survey-images/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent()).andDo(document("admin/survey-images/update"));
        mockMvc.perform(put("/api/admin/survey-images/order").contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[1]}"))
                .andExpect(status().isNoContent()).andDo(document("admin/survey-images/order"));
        mockMvc.perform(delete("/api/admin/survey-images/1"))
                .andExpect(status().isNoContent()).andDo(document("admin/survey-images/delete"));
        verify(service).update(eq(1L), any()); verify(service).reorder(List.of(1L)); verify(service).delete(1L);
    }
}

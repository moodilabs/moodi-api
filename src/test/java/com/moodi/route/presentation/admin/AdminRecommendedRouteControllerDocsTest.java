package com.moodi.route.presentation.admin;
import com.moodi.route.application.RecommendedRouteService;
import com.moodi.shared.support.AdminRestDocsSupport;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class AdminRecommendedRouteControllerDocsTest extends AdminRestDocsSupport {
    private final RecommendedRouteService service = mock(RecommendedRouteService.class);
    protected Object initController() { return new AdminRecommendedRouteController(service); }
    @Test @DisplayName("관리 콘텐츠 등록·조회·수정·정렬·삭제 계약")
    void manage_content() throws Exception {
        when(service.create(any())).thenReturn(1L);
        when(service.getAll()).thenReturn(List.of(new RecommendedRouteService.View(1L, "Seoul walk", "https://example.com/seoul.jpg", "Seoul", true, 0, List.of(new RecommendedRouteService.Command.Stop(1L, 1, 1)))));
        when(service.get(1L)).thenReturn(new RecommendedRouteService.View(1L, "Seoul walk", "https://example.com/seoul.jpg", "Seoul", true, 0, List.of(new RecommendedRouteService.Command.Stop(1L, 1, 1))));
        String body = """
                {"title":"Seoul walk","imageUrl":"https://example.com/seoul.jpg","region":"Seoul","visible":true,"sortOrder":0,"stops":[{"spotId":1,"day":1,"sequence":1}]}
                """;
        mockMvc.perform(post("/api/admin/recommended-routes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value(1)).andDo(document("admin/recommended-routes/create"));
        mockMvc.perform(get("/api/admin/recommended-routes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(1)).andDo(document("admin/recommended-routes/list"));
        mockMvc.perform(get("/api/admin/recommended-routes/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1)).andDo(document("admin/recommended-routes/detail"));
        mockMvc.perform(put("/api/admin/recommended-routes/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent()).andDo(document("admin/recommended-routes/update"));
        mockMvc.perform(put("/api/admin/recommended-routes/order").contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[1]}"))
                .andExpect(status().isNoContent()).andDo(document("admin/recommended-routes/order"));
        mockMvc.perform(delete("/api/admin/recommended-routes/1"))
                .andExpect(status().isNoContent()).andDo(document("admin/recommended-routes/delete"));
        verify(service).update(eq(1L), any()); verify(service).reorder(List.of(1L)); verify(service).delete(1L);
    }
    @Test @DisplayName("추천 루트 노출 상태 변경")
    void change_visibility() throws Exception {
        mockMvc.perform(patch("/api/admin/recommended-routes/1/visibility")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"visible\":false}"))
                .andExpect(status().isNoContent()).andDo(document("admin/recommended-routes/visibility"));
        verify(service).changeVisibility(1L, false);
        mockMvc.perform(patch("/api/admin/recommended-routes/1/visibility")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

}

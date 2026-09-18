package com.moodi.discovery.presentation.admin;
import com.moodi.discovery.application.AreaSuggestService;
import com.moodi.discovery.application.AreaSuggestion;
import com.moodi.discovery.application.RecommendedAreaService;
import com.moodi.discovery.domain.PickAreaLevel;
import com.moodi.shared.support.AdminRestDocsSupport;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class AdminRecommendedAreaControllerDocsTest extends AdminRestDocsSupport {
    private final RecommendedAreaService service = mock(RecommendedAreaService.class);
    private final AreaSuggestService areaSuggestService = mock(AreaSuggestService.class);
    protected Object initController() { return new AdminRecommendedAreaController(service, areaSuggestService); }
    @Test @DisplayName("관리 콘텐츠 등록·조회·수정·정렬·삭제 계약")
    void manage_content() throws Exception {
        when(service.create(any())).thenReturn(1L);
        when(service.getAll()).thenReturn(List.of(new RecommendedAreaService.View(1L, com.moodi.discovery.domain.PickAreaLevel.REGION, "Seoul", null, null, "Seoul", 0)));
        when(service.get(1L)).thenReturn(new RecommendedAreaService.View(1L, com.moodi.discovery.domain.PickAreaLevel.REGION, "Seoul", null, null, "Seoul", 0));
        String body = """
                {"level":"REGION","region":"Seoul","district":null,"neighborhood":null,"label":"Seoul","sortOrder":0}
                """;
        mockMvc.perform(post("/api/admin/recommended-areas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value(1)).andDo(document("admin/recommended-areas/create"));
        mockMvc.perform(get("/api/admin/recommended-areas"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(1)).andDo(document("admin/recommended-areas/list"));
        mockMvc.perform(get("/api/admin/recommended-areas/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1)).andDo(document("admin/recommended-areas/detail"));
        mockMvc.perform(put("/api/admin/recommended-areas/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent()).andDo(document("admin/recommended-areas/update"));
        mockMvc.perform(put("/api/admin/recommended-areas/order").contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[1]}"))
                .andExpect(status().isNoContent()).andDo(document("admin/recommended-areas/order"));
        mockMvc.perform(delete("/api/admin/recommended-areas/1"))
                .andExpect(status().isNoContent()).andDo(document("admin/recommended-areas/delete"));
        verify(service).update(eq(1L), any()); verify(service).reorder(List.of(1L)); verify(service).delete(1L);
    }

    @Test @DisplayName("지역 자동완성 — 등록 폼에서 고를 후보를 앱과 같은 원장에서 돌려준다")
    void suggest_areas() throws Exception {
        when(areaSuggestService.search(eq("서울"), anyInt()))
                .thenReturn(List.of(new AreaSuggestion(PickAreaLevel.REGION, "서울", null, null, "서울")));

        mockMvc.perform(get("/api/admin/recommended-areas/suggest").param("keyword", "서울"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].level").value("REGION"))
                .andExpect(jsonPath("$.data[0].region").value("서울"))
                .andExpect(jsonPath("$.data[0].label").value("서울"))
                .andDo(document("admin/recommended-areas/suggest"));
    }
}

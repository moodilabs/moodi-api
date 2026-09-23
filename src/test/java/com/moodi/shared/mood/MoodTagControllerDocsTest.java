package com.moodi.shared.mood;

import com.moodi.shared.support.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MoodTagControllerDocsTest extends RestDocsSupport {

    @Override
    protected Object initController() {
        return new MoodTagController();
    }

    @Test
    @DisplayName("무드 태그 목록 조회")
    void list_mood_tags() throws Exception {
        mockMvc.perform(get("/api/v1/moods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].key").exists())
                .andExpect(jsonPath("$.data[0].label").exists())
                .andExpect(jsonPath("$.data[0].displayTag").exists())
                .andDo(document("mood/mood-tags",
                        responseFields(
                                fieldWithPath("data[].key").description("필터·저장에 사용하는 키 (예: golden_hour)"),
                                fieldWithPath("data[].label").description("한국어 라벨 (예: 노을)"),
                                fieldWithPath("data[].displayTag").description("UI 표시용 해시태그 (예: #GoldenHour)")
                        )
                ));
    }
}

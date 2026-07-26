package com.moodi.spot.presentation;

import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.response.CursorResponse;
import com.moodi.spot.application.BookmarkService;
import com.moodi.spot.application.dto.BookmarkListRequest;
import com.moodi.spot.application.dto.BookmarkSpotResponse;
import com.moodi.spot.application.dto.BookmarkToggleResponse;
import com.moodi.spot.presentation.dto.BookmarkDeleteRequest;
import com.moodi.shared.support.AuthenticatedRestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookmarkControllerDocsTest extends AuthenticatedRestDocsSupport {

    private final BookmarkService bookmarkService = mock(BookmarkService.class);

    @Override
    protected Object initController() {
        return new BookmarkController(bookmarkService);
    }

    @Test
    @DisplayName("북마크 토글 - 저장")
    void toggle_bookmark_save() throws Exception {
        when(bookmarkService.toggle(eq(memberId), eq(1L)))
                .thenReturn(new BookmarkToggleResponse(1L, true, 12L));

        mockMvc.perform(post("/api/spots/{spotId}/bookmark", 1L))
                .andExpect(status().isOk())
                .andDo(document("bookmark/toggle",
                        pathParameters(
                                parameterWithName("spotId").description("스팟 ID")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("토글 결과"),
                                fieldWithPath("data.spotId").type(JsonFieldType.NUMBER).description("스팟 ID"),
                                fieldWithPath("data.bookmarked").type(JsonFieldType.BOOLEAN).description("최종 북마크 상태"),
                                fieldWithPath("data.bookmarkCount").type(JsonFieldType.NUMBER).description("현재 북마크 수")
                        )
                ));
    }

    @Test
    @DisplayName("저장한 스팟 목록 조회")
    void get_bookmarks() throws Exception {
        List<BookmarkSpotResponse> items = List.of(
                new BookmarkSpotResponse(1L, "익선동 한옥 골목", "https://img.moodi.kr/spot1.jpg",
                        "서울", List.of(MoodTag.TRADITIONAL, MoodTag.COZY), 12L,
                        LocalDateTime.of(2026, 7, 25, 10, 0), true),
                new BookmarkSpotResponse(2L, "성수동 카페", "https://img.moodi.kr/spot2.jpg",
                        "서울", List.of(MoodTag.RETRO, MoodTag.INDUSTRIAL), 8L,
                        LocalDateTime.of(2026, 7, 24, 15, 30), true)
        );
        CursorResponse<BookmarkSpotResponse> response = CursorResponse.of(items,
                "2026-07-24T15:30,2", true);

        when(bookmarkService.getBookmarks(eq(memberId), any(BookmarkListRequest.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/bookmarks")
                        .param("area", "서울")
                        .param("moodTags", "traditional", "cozy")
                        .param("sort", "LATEST")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("bookmark/list",
                        queryParameters(
                                parameterWithName("area").optional().description("지역 필터"),
                                parameterWithName("moodTags").optional().description("무드 태그 필터 (OR 조건)"),
                                parameterWithName("sort").optional().description("정렬 (LATEST, POPULAR)"),
                                parameterWithName("cursor").optional().description("커서 (다음 페이지)"),
                                parameterWithName("size").optional().description("페이지 크기 (기본 20)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("페이징 결과"),
                                fieldWithPath("data.items[]").type(JsonFieldType.ARRAY).description("북마크 스팟 목록"),
                                fieldWithPath("data.items[].spotId").type(JsonFieldType.NUMBER).description("스팟 ID"),
                                fieldWithPath("data.items[].title").type(JsonFieldType.STRING).description("스팟 이름"),
                                fieldWithPath("data.items[].imageUrl").type(JsonFieldType.STRING).description("대표 이미지"),
                                fieldWithPath("data.items[].area").type(JsonFieldType.STRING).description("지역"),
                                fieldWithPath("data.items[].moodTags[]").type(JsonFieldType.ARRAY).description("무드 태그 목록"),
                                fieldWithPath("data.items[].bookmarkCount").type(JsonFieldType.NUMBER).description("전체 북마크 수"),
                                fieldWithPath("data.items[].bookmarkedAt").type(JsonFieldType.STRING).description("저장 시각"),
                                fieldWithPath("data.items[].bookmarked").type(JsonFieldType.BOOLEAN).description("현재 사용자 저장 여부"),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).description("다음 페이지 커서").optional(),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    @DisplayName("북마크 일괄 삭제")
    void delete_bookmarks() throws Exception {
        when(bookmarkService.deleteBookmarks(eq(memberId), eq(List.of(1L, 2L, 3L))))
                .thenReturn(2);

        BookmarkDeleteRequest request = new BookmarkDeleteRequest(List.of(1L, 2L, 3L));

        mockMvc.perform(delete("/api/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("bookmark/delete",
                        requestFields(
                                fieldWithPath("spotIds").type(JsonFieldType.ARRAY).description("삭제할 스팟 ID 목록")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("삭제 결과"),
                                fieldWithPath("data.deletedCount").type(JsonFieldType.NUMBER).description("실제 삭제된 개수")
                        )
                ));
    }
}

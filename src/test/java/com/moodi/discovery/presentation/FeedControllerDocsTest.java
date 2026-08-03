package com.moodi.discovery.presentation;

import com.moodi.discovery.application.FeedCursor;
import com.moodi.discovery.application.FeedService;
import com.moodi.discovery.application.FeedSpotItem;
import com.moodi.discovery.application.PopularSpotItem;
import com.moodi.shared.auth.AuthMember;
import com.moodi.shared.auth.OptionalAuthMember;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.support.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedControllerDocsTest extends RestDocsSupport {

    private static final String NEXT_CURSOR = new FeedCursor(
            "3f7a1c9e", LocalDateTime.of(2026, 8, 3, 12, 0), 0, 2L, "9e107d9d", 42L).encode();

    private final FeedService feedService = mock(FeedService.class);

    /**
     * 비회원 케이스를 문서화하려면 null로 바꿀 수 있어야 한다.
     */
    private UUID memberId = UUID.randomUUID();

    @Override
    protected Object initController() {
        return new FeedController(feedService);
    }

    @Override
    protected HandlerMethodArgumentResolver[] argumentResolvers() {
        return new HandlerMethodArgumentResolver[]{new MemberIdArgumentResolverStub()};
    }

    @Test
    @DisplayName("회원 피드 조회 - 선호 무드 기반 개인화")
    void get_feed_for_member() throws Exception {
        List<FeedSpotItem> items = List.of(
                new FeedSpotItem(42L, "익선동 한옥 골목", "https://img.moodi.kr/spot42.jpg", "서울", true),
                new FeedSpotItem(17L, "성수동 카페 거리", "https://img.moodi.kr/spot17.jpg", "서울", false)
        );
        when(feedService.getFeed(eq(memberId), any()))
                .thenReturn(CursorResponse.of(items, NEXT_CURSOR, true));

        mockMvc.perform(get("/api/v1/feed"))
                .andExpect(status().isOk())
                .andDo(document("feed/list",
                        queryParameters(
                                parameterWithName("cursor").optional()
                                        .description("다음 페이지 커서. 없으면 새로고침으로 보고 순서를 새로 구성한다")
                        ),
                        feedResponseFields()
                ));
    }

    @Test
    @DisplayName("비회원 피드 조회 - 개인화 없이 전체 스팟")
    void get_feed_for_guest() throws Exception {
        memberId = null;
        List<FeedSpotItem> items = List.of(
                new FeedSpotItem(8L, "감천문화마을", "https://img.moodi.kr/spot8.jpg", "부산", false)
        );
        when(feedService.getFeed(isNull(), any()))
                .thenReturn(CursorResponse.of(items, NEXT_CURSOR, true));

        mockMvc.perform(get("/api/v1/feed").param("cursor", NEXT_CURSOR))
                .andExpect(status().isOk())
                .andDo(document("feed/list-guest",
                        queryParameters(
                                parameterWithName("cursor").optional().description("다음 페이지 커서")
                        ),
                        feedResponseFields()
                ));
    }

    @Test
    @DisplayName("인기 스팟 Top 5 조회")
    void get_popular_spots() throws Exception {
        List<PopularSpotItem> items = List.of(
                new PopularSpotItem(42L, "익선동 한옥 골목", "https://img.moodi.kr/spot42.jpg", "서울", 128L, true),
                new PopularSpotItem(17L, "성수동 카페 거리", "https://img.moodi.kr/spot17.jpg", "서울", 96L, false)
        );
        when(feedService.getPopularSpots(memberId)).thenReturn(items);

        mockMvc.perform(get("/api/v1/feed/popular-spots"))
                .andExpect(status().isOk())
                .andDo(document("feed/popular-spots",
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("인기 스팟 목록 (최대 5개)"),
                                fieldWithPath("data[].spotId").type(JsonFieldType.NUMBER).description("스팟 ID"),
                                fieldWithPath("data[].title").type(JsonFieldType.STRING).description("스팟 이름"),
                                fieldWithPath("data[].imageUrl").type(JsonFieldType.STRING).description("대표 이미지").optional(),
                                fieldWithPath("data[].area").type(JsonFieldType.STRING).description("지역"),
                                fieldWithPath("data[].bookmarkCount").type(JsonFieldType.NUMBER).description("전체 북마크 수"),
                                fieldWithPath("data[].bookmarked").type(JsonFieldType.BOOLEAN).description("현재 사용자 저장 여부")
                        )
                ));
    }

    private org.springframework.restdocs.snippet.Snippet feedResponseFields() {
        return responseFields(
                fieldWithPath("data").type(JsonFieldType.OBJECT).description("커서 페이징 결과"),
                fieldWithPath("data.items[]").type(JsonFieldType.ARRAY).description("피드 스팟 목록 (한 번에 20개)"),
                fieldWithPath("data.items[].spotId").type(JsonFieldType.NUMBER).description("스팟 ID"),
                fieldWithPath("data.items[].title").type(JsonFieldType.STRING).description("스팟 이름"),
                fieldWithPath("data.items[].imageUrl").type(JsonFieldType.STRING).description("대표 이미지").optional(),
                fieldWithPath("data.items[].area").type(JsonFieldType.STRING).description("지역"),
                fieldWithPath("data.items[].bookmarked").type(JsonFieldType.BOOLEAN)
                        .description("현재 사용자 저장 여부. 비회원은 항상 false"),
                fieldWithPath("data.nextCursor").type(JsonFieldType.STRING)
                        .description("다음 페이지 커서. 마지막 페이지면 null").optional(),
                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
        );
    }

    private class MemberIdArgumentResolverStub implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            boolean isMemberIdParameter = parameter.hasParameterAnnotation(AuthMember.class)
                    || parameter.hasParameterAnnotation(OptionalAuthMember.class);
            return isMemberIdParameter && parameter.getParameterType().equals(UUID.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return memberId;
        }
    }
}

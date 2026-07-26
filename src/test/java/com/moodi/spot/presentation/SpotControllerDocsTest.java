package com.moodi.spot.presentation;

import com.moodi.spot.application.SpotDetailService;
import com.moodi.spot.application.dto.PopularAreaSpotItem;
import com.moodi.spot.application.dto.SimilarMoodSpotItem;
import com.moodi.spot.application.dto.SpotDetailSnapshot;
import com.moodi.spot.application.dto.SpotImageItem;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.shared.support.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpotControllerDocsTest extends RestDocsSupport {

    private final SpotDetailService spotDetailService = mock(SpotDetailService.class);

    @Override
    protected Object initController() {
        return new SpotController(spotDetailService);
    }

    @Test
    @DisplayName("스팟 상세 조회")
    void get_spot_detail() throws Exception {
        SpotDetailSnapshot snapshot = new SpotDetailSnapshot(
                1L,
                "익선동 한옥 골목",
                "서울",
                "종로구",
                "익선동 한옥마을은 서울 종로구에 위치한 전통 한옥 골목입니다.",
                "https://www.ikseon.com",
                "02-1234-5678",
                SpotContentType.TOURIST_ATTRACTION,
                List.of("#Traditional", "#Cozy"),
                List.of("traditional", "cozy"),
                List.of(
                        new SpotImageItem("https://img.moodi.kr/spot1-main.jpg", true, 0),
                        new SpotImageItem("https://img.moodi.kr/spot1-sub.jpg", false, 1)
                ),
                245L,
                false,
                37.5762,
                126.9854,
                "서울특별시 종로구 수표로28길 17",
                "익선동 166",
                List.of(
                        new SimilarMoodSpotItem(2L, "북촌 한옥마을", "https://img.moodi.kr/spot2.jpg", "서울", 189L),
                        new SimilarMoodSpotItem(3L, "전주 한옥마을", "https://img.moodi.kr/spot3.jpg", "전주", 312L)
                ),
                List.of(
                        new PopularAreaSpotItem(4L, "경복궁", "https://img.moodi.kr/spot4.jpg", List.of("#Traditional", "#Expansive")),
                        new PopularAreaSpotItem(5L, "창덕궁", "https://img.moodi.kr/spot5.jpg", List.of("#Traditional", "#Serene"))
                ),
                "익선동 한옥 골목은 전통 한옥과 현대적 감성이 어우러진 공간입니다. 좁은 골목을 따라 걷다 보면 카페, 공방, 레스토랑이 한옥 안에 자리 잡고 있어 독특한 분위기를 자아냅니다."
        );

        when(spotDetailService.getDetail(eq(1L), isNull())).thenReturn(snapshot);

        mockMvc.perform(get("/api/spots/{spotId}", 1L))
                .andExpect(status().isOk())
                .andDo(document("spot/detail",
                        pathParameters(
                                parameterWithName("spotId").description("스팟 ID")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("스팟 상세 정보"),
                                fieldWithPath("data.spotId").type(JsonFieldType.NUMBER).description("스팟 ID"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("스팟 이름"),
                                fieldWithPath("data.area").type(JsonFieldType.STRING).description("지역 (시·도)"),
                                fieldWithPath("data.district").type(JsonFieldType.STRING).description("구·군"),
                                fieldWithPath("data.overview").type(JsonFieldType.STRING).description("스팟 소개"),
                                fieldWithPath("data.homepage").type(JsonFieldType.STRING).description("웹사이트 URL").optional(),
                                fieldWithPath("data.tel").type(JsonFieldType.STRING).description("전화번호").optional(),
                                fieldWithPath("data.moodTags[]").type(JsonFieldType.ARRAY).description("무드 태그 목록"),
                                fieldWithPath("data.images[]").type(JsonFieldType.ARRAY).description("이미지 목록"),
                                fieldWithPath("data.images[].imageUrl").type(JsonFieldType.STRING).description("이미지 URL"),
                                fieldWithPath("data.images[].isPrimary").type(JsonFieldType.BOOLEAN).description("대표 이미지 여부"),
                                fieldWithPath("data.images[].sortOrder").type(JsonFieldType.NUMBER).description("정렬 순서"),
                                fieldWithPath("data.aiDescription").type(JsonFieldType.STRING).description("AI 생성 설명 (최초 조회 시 생성, 실패 시 null)").optional(),
                                fieldWithPath("data.bookmarkCount").type(JsonFieldType.NUMBER).description("북마크 수"),
                                fieldWithPath("data.bookmarked").type(JsonFieldType.BOOLEAN).description("현재 사용자 북마크 여부 (비회원: false)"),
                                fieldWithPath("data.latitude").type(JsonFieldType.NUMBER).description("위도"),
                                fieldWithPath("data.longitude").type(JsonFieldType.NUMBER).description("경도"),
                                fieldWithPath("data.addr1").type(JsonFieldType.STRING).description("주소").optional(),
                                fieldWithPath("data.addr2").type(JsonFieldType.STRING).description("상세주소").optional(),
                                fieldWithPath("data.similarMoodSpots[]").type(JsonFieldType.ARRAY).description("비슷한 무드의 스팟 (최대 5개)"),
                                fieldWithPath("data.similarMoodSpots[].spotId").type(JsonFieldType.NUMBER).description("스팟 ID"),
                                fieldWithPath("data.similarMoodSpots[].title").type(JsonFieldType.STRING).description("스팟 이름"),
                                fieldWithPath("data.similarMoodSpots[].imageUrl").type(JsonFieldType.STRING).description("대표 이미지"),
                                fieldWithPath("data.similarMoodSpots[].area").type(JsonFieldType.STRING).description("지역"),
                                fieldWithPath("data.similarMoodSpots[].bookmarkCount").type(JsonFieldType.NUMBER).description("북마크 수"),
                                fieldWithPath("data.popularAreaSpots[]").type(JsonFieldType.ARRAY).description("같은 지역 인기 스팟 (최대 5개)"),
                                fieldWithPath("data.popularAreaSpots[].spotId").type(JsonFieldType.NUMBER).description("스팟 ID"),
                                fieldWithPath("data.popularAreaSpots[].title").type(JsonFieldType.STRING).description("스팟 이름"),
                                fieldWithPath("data.popularAreaSpots[].imageUrl").type(JsonFieldType.STRING).description("대표 이미지"),
                                fieldWithPath("data.popularAreaSpots[].moodTags[]").type(JsonFieldType.ARRAY).description("무드 태그 목록")
                        )
                ));
    }
}

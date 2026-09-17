package com.moodi.spot.presentation;

import com.moodi.shared.auth.OptionalAuthMember;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.response.SuccessResponse;
import com.moodi.spot.application.SpotDetailService;
import com.moodi.spot.application.SpotSearchService;
import com.moodi.spot.application.dto.SpotDetailSnapshot;
import com.moodi.spot.application.dto.SpotSearchItem;
import com.moodi.spot.application.dto.SpotSearchRequest;
import com.moodi.spot.application.dto.SpotSearchSortType;
import com.moodi.spot.presentation.dto.PopularSpotResponse;
import com.moodi.spot.presentation.dto.SimilarMoodSpotResponse;
import com.moodi.spot.presentation.dto.SpotDetailResponse;
import com.moodi.spot.presentation.dto.SpotDetailResponse.SpotImageResponse;
import com.moodi.spot.presentation.dto.SpotSearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class SpotController {

    /**
     * 공개 엔드포인트라 상한이 없으면 size=5000 한 번에 1.8MB·3초짜리 응답이 나간다(실측).
     * 앱이 쓰는 한 페이지는 20~50 이므로 100 이면 충분하다.
     */
    private static final int MAX_SEARCH_SIZE = 100;

    private final SpotDetailService spotDetailService;
    private final SpotSearchService spotSearchService;

    public SpotController(SpotDetailService spotDetailService, SpotSearchService spotSearchService) {
        this.spotDetailService = spotDetailService;
        this.spotSearchService = spotSearchService;
    }

    @GetMapping("/api/spots/search")
    public SuccessResponse<CursorResponse<SpotSearchResponse>> searchSpots(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) List<String> moodTags,
            @RequestParam(defaultValue = "false") boolean saved,
            @RequestParam(defaultValue = "BEST_MATCH") SpotSearchSortType sort,
            @RequestParam(required = false) UUID routePublicId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @OptionalAuthMember UUID memberId) {
        // saved 는 "내가 저장한 것만"이라 회원이 있어야 뜻이 선다. 예전에는 비로그인
        // 요청이면 조건을 조용히 버려, 저장한 적 없는 스팟까지 그대로 담긴 목록을
        // "저장한 스팟"이라며 돌려줬다 — 약속과 정반대인 응답이라 401 로 끊는다.
        if (saved && memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        SpotSearchRequest request = SpotSearchRequest.of(keyword, area, moodTags, saved, sort,
                routePublicId, cursor, Math.clamp(size, 1, MAX_SEARCH_SIZE));
        CursorResponse<SpotSearchItem> result = spotSearchService.search(memberId, request);
        return SuccessResponse.of(result.map(SpotSearchResponse::from));
    }

    @GetMapping("/api/spots/{spotId}")
    public SuccessResponse<SpotDetailResponse> getSpotDetail(
            @PathVariable Long spotId,
            @OptionalAuthMember UUID memberId) {
        SpotDetailSnapshot snapshot = spotDetailService.getDetail(spotId, memberId);
        return SuccessResponse.of(toResponse(snapshot));
    }

    private SpotDetailResponse toResponse(SpotDetailSnapshot s) {
        return new SpotDetailResponse(
                s.spotId(),
                s.title(),
                s.area(),
                s.district(),
                s.overview(),
                s.homepage(),
                s.tel(),
                s.moodTags(),
                s.images().stream()
                        .map(img -> new SpotImageResponse(img.imageUrl(), img.isPrimary(), img.sortOrder()))
                        .toList(),
                s.aiDescription(),
                s.bookmarkCount(),
                s.bookmarked(),
                s.latitude(),
                s.longitude(),
                s.addr1(),
                s.addr2(),
                s.addr1Ko(),
                s.kakaoMapUrl(),
                s.similarMoodSpots().stream()
                        .map(item -> new SimilarMoodSpotResponse(
                                item.spotId(), item.title(), item.imageUrl(),
                                item.area(), item.bookmarkCount(), item.moodTags()))
                        .toList(),
                s.popularAreaSpots().stream()
                        .map(item -> new PopularSpotResponse(
                                item.spotId(), item.title(), item.imageUrl(), item.moodTags()))
                        .toList()
        );
    }
}

package com.moodi.spot.presentation;

import com.moodi.shared.response.SuccessResponse;
import com.moodi.spot.application.SpotDetailService;
import com.moodi.spot.application.dto.SpotDetailSnapshot;
import com.moodi.spot.presentation.dto.PopularSpotResponse;
import com.moodi.spot.presentation.dto.SimilarMoodSpotResponse;
import com.moodi.spot.presentation.dto.SpotDetailResponse;
import com.moodi.spot.presentation.dto.SpotDetailResponse.SpotImageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpotController {

    private final SpotDetailService spotDetailService;

    public SpotController(SpotDetailService spotDetailService) {
        this.spotDetailService = spotDetailService;
    }

    // TODO: 선택적 인증 구현 후 @Nullable UUID memberId 파라미터 추가
    @GetMapping("/api/spots/{spotId}")
    public SuccessResponse<SpotDetailResponse> getSpotDetail(@PathVariable Long spotId) {
        SpotDetailSnapshot snapshot = spotDetailService.getDetail(spotId, null);
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
                s.similarMoodSpots().stream()
                        .map(item -> new SimilarMoodSpotResponse(
                                item.spotId(), item.title(), item.imageUrl(),
                                item.area(), item.bookmarkCount()))
                        .toList(),
                s.popularAreaSpots().stream()
                        .map(item -> new PopularSpotResponse(
                                item.spotId(), item.title(), item.imageUrl(), item.moodTags()))
                        .toList()
        );
    }
}

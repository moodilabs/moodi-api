package com.moodi.spot.presentation;

import com.moodi.shared.response.SuccessResponse;
import com.moodi.spot.application.SpotDetailService;
import com.moodi.spot.presentation.dto.SpotDetailResponse;
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
        SpotDetailResponse response = spotDetailService.getDetail(spotId, null);
        return SuccessResponse.of(response);
    }
}

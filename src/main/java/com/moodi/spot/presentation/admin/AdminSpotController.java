package com.moodi.spot.presentation.admin;

import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.response.SuccessResponse;
import com.moodi.spot.application.SpotAdminService;
import com.moodi.spot.application.dto.SpotAdminFilter;
import com.moodi.spot.domain.SpotStatus;
import com.moodi.spot.presentation.dto.admin.AdminSpotDescriptionRequest;
import com.moodi.spot.presentation.dto.admin.AdminSpotDetailResponse;
import com.moodi.spot.presentation.dto.admin.AdminSpotMoodRequest;
import com.moodi.spot.presentation.dto.admin.AdminSpotRouteExclusionRequest;
import com.moodi.spot.presentation.dto.admin.AdminSpotStatusRequest;
import com.moodi.spot.presentation.dto.admin.AdminSpotSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 스팟 관리. 노출 상태 변경(삭제 포함)까지 모든 관리자가 할 수 있다. */
@AdminRequired
@RestController
@RequestMapping("/api/admin/spots")
public class AdminSpotController {

    private final SpotAdminService spotAdminService;

    public AdminSpotController(SpotAdminService spotAdminService) {
        this.spotAdminService = spotAdminService;
    }

    @GetMapping
    public SuccessResponse<CursorResponse<AdminSpotSummaryResponse>> getSpots(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SpotStatus status,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        SpotAdminFilter filter = new SpotAdminFilter(keyword, status, area);
        return SuccessResponse.of(spotAdminService.getSpots(filter, parseCursor(cursor), size)
                .map(AdminSpotSummaryResponse::from));
    }

    @GetMapping("/{spotId}")
    public SuccessResponse<AdminSpotDetailResponse> getSpot(@PathVariable Long spotId) {
        return SuccessResponse.of(AdminSpotDetailResponse.from(spotAdminService.getSpot(spotId)));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{spotId}/status")
    public void changeStatus(@PathVariable Long spotId, @Valid @RequestBody AdminSpotStatusRequest request) {
        spotAdminService.changeStatus(spotId, request.status(), request.reason());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{spotId}/route-exclusion")
    public void changeRouteExclusion(@PathVariable Long spotId,
                                     @Valid @RequestBody AdminSpotRouteExclusionRequest request) {
        spotAdminService.changeRouteExclusion(spotId, request.excluded());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/{spotId}/moods")
    public void overrideMoodTags(@PathVariable Long spotId, @Valid @RequestBody AdminSpotMoodRequest request) {
        spotAdminService.overrideMoodTags(spotId, request.moodTags());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/{spotId}/description")
    public void updateDescription(@PathVariable Long spotId,
                                  @Valid @RequestBody AdminSpotDescriptionRequest request) {
        spotAdminService.updateDescription(spotId, request.content());
    }

    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR_FORMAT);
        }
    }
}

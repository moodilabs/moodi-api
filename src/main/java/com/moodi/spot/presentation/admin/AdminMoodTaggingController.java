package com.moodi.spot.presentation.admin;

import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.response.SuccessResponse;
import com.moodi.spot.application.MoodTaggingAdminService;
import com.moodi.spot.presentation.dto.admin.MoodTaggingFailedResponse;
import com.moodi.spot.presentation.dto.admin.MoodTaggingSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@AdminRequired
@RestController
@RequestMapping("/api/admin/mood-tagging")
public class AdminMoodTaggingController {

    private final MoodTaggingAdminService moodTaggingAdminService;

    public AdminMoodTaggingController(MoodTaggingAdminService moodTaggingAdminService) {
        this.moodTaggingAdminService = moodTaggingAdminService;
    }

    @GetMapping("/summary")
    public SuccessResponse<MoodTaggingSummaryResponse> getSummary() {
        Map<String, Long> statusCounts = moodTaggingAdminService.getSummary();
        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        return SuccessResponse.of(new MoodTaggingSummaryResponse(statusCounts, total));
    }

    @GetMapping("/failed")
    public SuccessResponse<List<MoodTaggingFailedResponse>> getFailed() {
        List<MoodTaggingFailedResponse> failed = moodTaggingAdminService.getFailed().stream()
                .map(MoodTaggingFailedResponse::from)
                .toList();
        return SuccessResponse.of(failed);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/spots/{spotId}/retry")
    public void retry(@PathVariable Long spotId) {
        moodTaggingAdminService.retrySpot(spotId);
    }
}

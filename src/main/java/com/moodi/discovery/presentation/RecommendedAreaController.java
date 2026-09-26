package com.moodi.discovery.presentation;

import com.moodi.discovery.application.RecommendedAreaService;
import com.moodi.discovery.presentation.dto.RecommendedAreaResponse;
import com.moodi.shared.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecommendedAreaController {

    private final RecommendedAreaService service;

    public RecommendedAreaController(RecommendedAreaService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/picks/recommended-areas")
    public SuccessResponse<List<RecommendedAreaResponse>> list() {
        List<RecommendedAreaResponse> responses = service.getPublished().stream()
                .map(RecommendedAreaResponse::from)
                .toList();
        return SuccessResponse.of(responses);
    }
}

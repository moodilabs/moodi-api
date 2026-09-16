package com.moodi.route.presentation;

import com.moodi.route.application.RecommendedRouteService;
import com.moodi.shared.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecommendedRouteController {

    private final RecommendedRouteService service;

    public RecommendedRouteController(RecommendedRouteService service) {
        this.service = service;
    }
    @GetMapping("/api/v1/feed/recommended-routes")
    public SuccessResponse<List<RecommendedRouteService.View>> list() {
        return SuccessResponse.of(service.getPublished());
    }
}

package com.moodi.route.presentation;

import com.moodi.route.application.RouteQueryService;
import com.moodi.route.domain.Route;
import com.moodi.route.presentation.dto.SharedRouteDetailResponse;
import com.moodi.shared.auth.OptionalAuthMember;
import com.moodi.shared.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/routes/shared")
public class SharedRouteController {

    private final RouteQueryService routeQueryService;

    public SharedRouteController(RouteQueryService routeQueryService) {
        this.routeQueryService = routeQueryService;
    }

    @GetMapping("/{publicId}")
    public SuccessResponse<SharedRouteDetailResponse> getSharedDetail(
            @OptionalAuthMember UUID memberId,
            @PathVariable UUID publicId) {
        Route route = routeQueryService.getSharedDetail(publicId);
        boolean isOwner = memberId != null && route.getMemberId().equals(memberId);
        return SuccessResponse.of(SharedRouteDetailResponse.from(route, isOwner));
    }
}

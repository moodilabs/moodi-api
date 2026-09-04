package com.moodi.route.presentation;

import com.moodi.route.application.RouteQueryService;
import com.moodi.route.application.SharedRouteDetail;
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
    public SuccessResponse<SharedRouteDetail> getSharedDetail(
            @OptionalAuthMember UUID memberId,
            @PathVariable UUID publicId) {
        SharedRouteDetail detail = routeQueryService.getSharedDetail(publicId, memberId);
        return SuccessResponse.of(detail);
    }
}

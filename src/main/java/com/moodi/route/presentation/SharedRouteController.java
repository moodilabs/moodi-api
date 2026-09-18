package com.moodi.route.presentation;

import com.moodi.route.application.RouteQueryService;
import com.moodi.route.application.RouteShareLinkBuilder;
import com.moodi.route.application.SharedRouteDetail;
import com.moodi.shared.auth.OptionalAuthMember;
import com.moodi.shared.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/routes/shared")
public class SharedRouteController {

    private final RouteQueryService routeQueryService;
    private final RouteShareLinkBuilder shareLinkBuilder;

    public SharedRouteController(RouteQueryService routeQueryService, RouteShareLinkBuilder shareLinkBuilder) {
        this.routeQueryService = routeQueryService;
        this.shareLinkBuilder = shareLinkBuilder;
    }

    @GetMapping("/{publicId}")
    public SuccessResponse<SharedRouteDetail> getSharedDetail(
            @OptionalAuthMember UUID memberId,
            @PathVariable UUID publicId,
            HttpServletRequest request) {
        SharedRouteDetail detail = routeQueryService.getSharedDetail(publicId, memberId);
        String shareUrl = shareLinkBuilder.shareUrl(RequestOrigin.of(request), detail.publicId(), detail.shortCode());
        return SuccessResponse.of(detail.withShareUrl(shareUrl));
    }
}

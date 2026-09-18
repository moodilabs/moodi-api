package com.moodi.route.application;

import com.moodi.route.domain.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteShareLandingService {

    private final RouteRepository routeRepository;

    public RouteShareLandingView getLandingView(UUID publicId) {
        return routeRepository.findSharedByPublicId(publicId)
                .map(route -> RouteShareLandingView.of(route.getTitle()))
                .orElseGet(RouteShareLandingView::notFound);
    }
}

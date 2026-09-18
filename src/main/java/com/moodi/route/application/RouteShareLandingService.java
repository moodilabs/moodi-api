package com.moodi.route.application;

import com.moodi.route.domain.RouteRepository;
import com.moodi.route.domain.RouteShortCode;
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
        return routeRepository.findSharedByPublicIdWithDays(publicId)
                .map(RouteShareLandingView::from)
                .orElseGet(RouteShareLandingView::notFound);
    }

    /** 단축 코드 형식이 아니면 DB를 건드리지 않고 바로 폴백한다. */
    public RouteShareLandingView getLandingViewByShortCode(String shortCode) {
        if (!RouteShortCode.isValid(shortCode)) {
            return RouteShareLandingView.notFound();
        }
        return routeRepository.findSharedByShortCodeWithDays(shortCode)
                .map(RouteShareLandingView::from)
                .orElseGet(RouteShareLandingView::notFound);
    }
}

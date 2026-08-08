package com.moodi.route.application;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteRepository;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteShareService {

    private final RouteRepository routeRepository;

    public void share(UUID publicId, UUID memberId) {
        Route route = routeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_NOT_FOUND));

        route.validateOwner(memberId);
        route.share();
    }
}

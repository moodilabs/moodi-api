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
public class RouteCopyService {

    private final RouteRepository routeRepository;

    public Route copy(UUID publicId, UUID memberId) {
        Route original = routeRepository.findSharedByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_NOT_FOUND));

        Route copy = original.copyFor(memberId);
        return routeRepository.save(copy);
    }
}

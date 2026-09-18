package com.moodi.route.application;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteRepository;
import com.moodi.route.domain.RouteShortCode;
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

    /** 62^8 공간에서 8자리가 연달아 겹칠 확률은 무시해도 되지만, 무한 루프만은 막는다. */
    private static final int MAX_SHORT_CODE_ATTEMPTS = 5;

    private final RouteRepository routeRepository;

    /**
     * 공유를 켜고 단축 코드를 붙인다. 이미 공유 중인 루트에 다시 호출해도 안전하다 — 앱은 공유 버튼을
     * 누를 때마다 이 API를 부르므로, 단축 코드가 생기기 전에 공유된 루트도 이 경로로 코드를 얻는다.
     */
    public Route share(UUID publicId, UUID memberId) {
        Route route = routeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_NOT_FOUND));

        route.validateOwner(memberId);
        route.share();
        if (!route.hasShortCode()) {
            route.assignShortCode(newUniqueShortCode());
        }
        return route;
    }

    private String newUniqueShortCode() {
        for (int i = 0; i < MAX_SHORT_CODE_ATTEMPTS; i++) {
            String candidate = RouteShortCode.generate();
            if (!routeRepository.existsByShortCode(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.ROUTE_SHORT_CODE_EXHAUSTED);
    }
}

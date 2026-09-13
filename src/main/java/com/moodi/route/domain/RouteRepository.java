package com.moodi.route.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RouteRepository {

    Route save(Route route);

    Optional<Route> findById(Long id);

    Optional<Route> findByPublicId(UUID publicId);

    Optional<Route> findByPublicIdWithDays(UUID publicId);

    Optional<Route> findSharedByPublicId(UUID publicId);

    Optional<Route> findSharedByPublicIdWithDays(UUID publicId);

    void delete(Route route);

    /** 회원 탈퇴 전용. 아직 삭제되지 않은 본인 루트를 한 번에 소프트 삭제한다. */
    int softDeleteAllByMemberId(UUID memberId, LocalDateTime deletedAt);
}

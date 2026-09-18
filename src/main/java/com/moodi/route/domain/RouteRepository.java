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

    Optional<Route> findSharedByShortCodeWithDays(String shortCode);

    boolean existsByShortCode(String shortCode);

    void delete(Route route);

    /**
     * 보류 중인 변경을 즉시 DB에 반영한다.
     * <p>
     * 루트 수정은 같은 Day의 스팟·구간을 통째로 갈아끼운다. 삭제와 삽입을 한 트랜잭션에 모아 두면
     * Hibernate가 삽입을 먼저 실행해 {@code uk_route_spot_day_sequence}(route_day_id, sequence)에 걸린다.
     * 비운 뒤 이 메서드로 DELETE를 먼저 내보내고 새 행을 넣는다.
     */
    void flush();

    /** 회원 탈퇴 전용. 아직 삭제되지 않은 본인 루트를 한 번에 소프트 삭제한다. */
    int softDeleteAllByMemberId(UUID memberId, LocalDateTime deletedAt);
}

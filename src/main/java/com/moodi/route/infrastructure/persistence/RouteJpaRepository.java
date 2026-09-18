package com.moodi.route.infrastructure.persistence;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RouteJpaRepository extends RouteRepository, Repository<Route, Long> {

    @Override
    Route save(Route route);

    @Override
    Optional<Route> findById(Long id);

    @Override
    @Query("SELECT r FROM Route r WHERE r.publicId = :publicId AND r.deletedAt IS NULL")
    Optional<Route> findByPublicId(@Param("publicId") UUID publicId);

    @Override
    @Query("SELECT r FROM Route r LEFT JOIN FETCH r.days WHERE r.publicId = :publicId AND r.deletedAt IS NULL")
    Optional<Route> findByPublicIdWithDays(@Param("publicId") UUID publicId);

    @Override
    @Query("SELECT r FROM Route r WHERE r.publicId = :publicId AND r.shared = true AND r.deletedAt IS NULL")
    Optional<Route> findSharedByPublicId(@Param("publicId") UUID publicId);

    @Override
    @Query("SELECT r FROM Route r LEFT JOIN FETCH r.days WHERE r.publicId = :publicId AND r.shared = true AND r.deletedAt IS NULL")
    Optional<Route> findSharedByPublicIdWithDays(@Param("publicId") UUID publicId);

    @Override
    @Query("SELECT r FROM Route r LEFT JOIN FETCH r.days WHERE r.shortCode = :shortCode AND r.shared = true AND r.deletedAt IS NULL")
    Optional<Route> findSharedByShortCodeWithDays(@Param("shortCode") String shortCode);

    /** 삭제된 루트의 코드도 유니크 인덱스에 남아 있으므로 deleted_at 조건 없이 본다. */
    @Override
    @Query("SELECT COUNT(r) > 0 FROM Route r WHERE r.shortCode = :shortCode")
    boolean existsByShortCode(@Param("shortCode") String shortCode);

    /** flushAutomatically: 같은 트랜잭션에서 바꾼 is_shared 를 먼저 내보낸다. clearAutomatically: 갱신 뒤 재조회가 DB 값을 읽게 한다. */
    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Route r SET r.shortCode = :shortCode WHERE r.id = :id AND r.shortCode IS NULL")
    int assignShortCodeIfAbsent(@Param("id") Long id, @Param("shortCode") String shortCode);

    @Override
    void delete(Route route);

    @Override
    void flush();

    @Override
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Route r SET r.deletedAt = :deletedAt WHERE r.memberId = :memberId AND r.deletedAt IS NULL")
    int softDeleteAllByMemberId(@Param("memberId") UUID memberId, @Param("deletedAt") LocalDateTime deletedAt);
}

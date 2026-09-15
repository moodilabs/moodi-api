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
    void delete(Route route);

    @Override
    void flush();

    @Override
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Route r SET r.deletedAt = :deletedAt WHERE r.memberId = :memberId AND r.deletedAt IS NULL")
    int softDeleteAllByMemberId(@Param("memberId") UUID memberId, @Param("deletedAt") LocalDateTime deletedAt);
}

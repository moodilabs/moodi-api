package com.moodi.member.infrastructure.route;

import com.moodi.route.domain.Route;
import com.moodi.route.support.RouteFixture;
import com.moodi.shared.support.RepositoryTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RouteCountReaderAdapterTest extends RepositoryTestSupport {

    private static final LocalDate START = LocalDate.of(2026, 8, 10);

    @Autowired
    private EntityManager em;

    private RouteCountReaderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RouteCountReaderAdapter(em);
    }

    @Test
    @DisplayName("삭제되지 않은 본인 루트만 센다")
    void count_excludes_deleted_and_others() {
        UUID memberId = UUID.randomUUID();
        em.persist(route(memberId));
        em.persist(route(memberId));
        Route deleted = route(memberId);
        deleted.softDelete();
        em.persist(deleted);
        em.persist(route(UUID.randomUUID()));
        em.flush();

        assertThat(adapter.countActiveByMemberId(memberId)).isEqualTo(2L);
    }

    @Test
    @DisplayName("루트가 없으면 0이다")
    void count_returns_zero_when_none() {
        assertThat(adapter.countActiveByMemberId(UUID.randomUUID())).isZero();
    }

    private Route route(UUID memberId) {
        return RouteFixture.createRoute(memberId, "성수 코스", START, START,
                List.of(RouteFixture.createDay(1, START, 1)));
    }
}

package com.moodi.discovery.infrastructure.persistence;

import com.moodi.shared.support.RepositoryTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PickWithdrawalRepositoryImplTest extends RepositoryTestSupport {

    @Autowired
    private EntityManager em;

    private PickWithdrawalRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new PickWithdrawalRepositoryImpl(em);
    }

    @Test
    @DisplayName("회원의 Pick 요청·지역·결과를 지우고 사진 키를 돌려준다")
    void deletes_request_tree_and_returns_image_keys() {
        UUID memberId = UUID.randomUUID();
        UUID request = insertRequest(memberId, "picks/m/a.jpg");
        insertArea(request);
        insertResult(request);
        UUID otherRequest = insertRequest(UUID.randomUUID(), "picks/o/b.jpg");
        insertArea(otherRequest);

        List<String> keys = repository.deleteAllByMemberId(memberId);

        assertThat(keys).containsExactly("picks/m/a.jpg");
        assertThat(count("pick_request")).isEqualTo(1);
        assertThat(count("pick_request_area")).isEqualTo(1);
        assertThat(count("pick_result_spot")).isZero();
    }

    @Test
    @DisplayName("Pick이 없으면 빈 목록이다")
    void returns_empty_when_nothing() {
        assertThat(repository.deleteAllByMemberId(UUID.randomUUID())).isEmpty();
    }

    private UUID insertRequest(UUID memberId, String imageKey) {
        UUID id = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO pick_request (id, member_id, image_key, created_at, updated_at) "
                        + "VALUES (:id, :memberId, :imageKey, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter("id", id).setParameter("memberId", memberId).setParameter("imageKey", imageKey)
                .executeUpdate();
        return id;
    }

    private void insertArea(UUID requestId) {
        em.createNativeQuery("INSERT INTO pick_request_area (id, pick_request_id, level, region, sort_order, created_at, updated_at) "
                        + "VALUES (:id, :requestId, 'REGION', '서울', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter("id", UUID.randomUUID()).setParameter("requestId", requestId).executeUpdate();
    }

    private void insertResult(UUID requestId) {
        em.createNativeQuery("INSERT INTO pick_result_spot (id, pick_request_id, spot_id, rank, similarity, fallback, created_at, updated_at) "
                        + "VALUES (:id, :requestId, 1, 1, 0.9, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter("id", UUID.randomUUID()).setParameter("requestId", requestId).executeUpdate();
    }

    private long count(String table) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM " + table).getSingleResult()).longValue();
    }
}

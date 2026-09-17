package com.moodi.discovery.infrastructure.member;

import com.moodi.discovery.application.PreferredMoodWriter;
import com.moodi.shared.mood.MoodTag;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 회원 선호 무드 태그를 교체하는 어댑터. 온보딩의 delete-then-insert와 동일한 방식이다.
 *
 * <p>온보딩은 "0개 또는 3개 이상" 불변식을 검증하지만, Pick에서 파생된 태그는
 * MoodTagRuleEngine 결과 그대로 저장한다(1~2개가 될 수도 있다).
 */
@Component
public class PreferredMoodWriterAdapter implements PreferredMoodWriter {

    private static final String DELETE_SQL =
            "DELETE FROM member_preferred_mood WHERE member_id = :memberId";

    private static final String INSERT_SQL = """
            INSERT INTO member_preferred_mood (id, member_id, mood, created_at, updated_at)
            VALUES (gen_random_uuid(), :memberId, :mood, now(), now())
            """;

    private final EntityManager em;

    public PreferredMoodWriterAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public void overwrite(UUID memberId, List<MoodTag> tags) {
        em.createNativeQuery(DELETE_SQL)
                .setParameter("memberId", memberId)
                .executeUpdate();

        for (MoodTag tag : tags) {
            em.createNativeQuery(INSERT_SQL)
                    .setParameter("memberId", memberId)
                    .setParameter("mood", tag.name())
                    .executeUpdate();
        }
    }
}

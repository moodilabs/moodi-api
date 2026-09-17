package com.moodi.discovery.infrastructure.member;

import com.moodi.discovery.application.PreferredVectorWriter;
import com.moodi.shared.mood.MoodVector;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PreferredVectorWriterAdapter implements PreferredVectorWriter {

    private static final String UPSERT_SQL = """
            INSERT INTO member_preferred_vector (member_id, mood_vector, created_at, updated_at)
            VALUES (:memberId, CAST(:vector AS jsonb), now(), now())
            ON CONFLICT (member_id)
            DO UPDATE SET mood_vector = CAST(:vector AS jsonb), updated_at = now()
            """;

    private final EntityManager em;
    private final MoodVectorJsonMapper jsonMapper;

    public PreferredVectorWriterAdapter(EntityManager em) {
        this.em = em;
        this.jsonMapper = new MoodVectorJsonMapper();
    }

    @Override
    public void save(UUID memberId, MoodVector vector) {
        em.createNativeQuery(UPSERT_SQL)
                .setParameter("memberId", memberId)
                .setParameter("vector", jsonMapper.toJson(vector))
                .executeUpdate();
    }
}

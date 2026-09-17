package com.moodi.discovery.infrastructure.member;

import com.moodi.discovery.application.PreferredVectorReader;
import com.moodi.shared.mood.MoodVector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PreferredVectorReaderAdapter implements PreferredVectorReader {

    private static final String SQL =
            "SELECT mood_vector::text FROM member_preferred_vector WHERE member_id = :memberId";

    private final EntityManager em;
    private final MoodVectorJsonMapper jsonMapper;

    public PreferredVectorReaderAdapter(EntityManager em) {
        this.em = em;
        this.jsonMapper = new MoodVectorJsonMapper();
    }

    @Override
    public Optional<MoodVector> readByMemberId(UUID memberId) {
        try {
            String json = (String) em.createNativeQuery(SQL)
                    .setParameter("memberId", memberId)
                    .getSingleResult();
            return Optional.of(jsonMapper.fromJson(json));
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}

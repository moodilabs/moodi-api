package com.moodi.discovery.infrastructure.member;

import com.moodi.discovery.application.PreferredMoodReader;
import com.moodi.shared.mood.MoodTag;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 회원 컨텍스트의 선호 무드를 읽는 어댑터.
 *
 * <p>{@code member_preferred_mood.mood}는 enum 이름(예: {@code RETRO})으로 저장되고,
 * {@code spot_mood.mood_tags}는 key(예: {@code retro})로 저장된다. 변환은 공유 커널인 MoodTag가 맡는다.
 */
@Component
public class PreferredMoodReaderAdapter implements PreferredMoodReader {

    private static final String SQL = "SELECT mood FROM member_preferred_mood WHERE member_id = :memberId";

    private final EntityManager em;

    public PreferredMoodReaderAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<MoodTag> readByMemberId(UUID memberId) {
        @SuppressWarnings("unchecked")
        List<String> moodNames = em.createNativeQuery(SQL)
                .setParameter("memberId", memberId)
                .getResultList();

        return moodNames.stream()
                .map(MoodTag::valueOf)
                .toList();
    }
}

package com.moodi.spot.infrastructure.persistence;

import com.moodi.shared.mood.MoodTag;
import com.moodi.spot.application.SpotDetailQueryRepository;
import com.moodi.spot.application.dto.PopularAreaSpotItem;
import com.moodi.spot.application.dto.SimilarMoodSpotItem;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class SpotDetailQueryRepositoryImpl implements SpotDetailQueryRepository {

    private final EntityManager em;

    public SpotDetailQueryRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<SimilarMoodSpotItem> findSimilarMoodSpots(Long spotId, List<String> moodTagKeys, int limit) {
        if (moodTagKeys.isEmpty()) {
            return List.of();
        }

        String sql = """
                SELECT s.id, st.title, si.image_url, s.area,
                       (SELECT COUNT(*) FROM bookmark b WHERE b.spot_id = s.id) AS bookmark_count
                FROM spot s
                JOIN spot_mood sm ON sm.spot_id = s.id
                JOIN spot_translation st ON st.spot_id = s.id AND st.locale = 'ko-KR'
                LEFT JOIN spot_image si ON si.spot_id = s.id AND si.is_primary = true
                WHERE s.id != :spotId
                  AND s.status = 'PUBLISHED'
                  AND sm.mood_tags ?| CAST(:moodTags AS text[])
                ORDER BY RANDOM()
                LIMIT :limit
                """;

        String moodTagsArray = "{" + String.join(",", moodTagKeys) + "}";

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("spotId", spotId)
                .setParameter("moodTags", moodTagsArray)
                .setParameter("limit", limit)
                .getResultList();

        return rows.stream()
                .map(row -> new SimilarMoodSpotItem(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        ((Number) row[4]).longValue()
                ))
                .toList();
    }

    @Override
    public List<PopularAreaSpotItem> findPopularSpotsByArea(Long spotId, String area, String district, int limit) {
        if (area == null || district == null) {
            return List.of();
        }

        String sql = """
                SELECT s.id, st.title, si.image_url, sm.mood_tags
                FROM spot s
                JOIN spot_translation st ON st.spot_id = s.id AND st.locale = 'ko-KR'
                LEFT JOIN spot_image si ON si.spot_id = s.id AND si.is_primary = true
                LEFT JOIN spot_mood sm ON sm.spot_id = s.id
                WHERE s.id != :spotId
                  AND s.status = 'PUBLISHED'
                  AND s.area = :area
                  AND s.district = :district
                ORDER BY (SELECT COUNT(*) FROM bookmark b WHERE b.spot_id = s.id) DESC, s.id ASC
                LIMIT :limit
                """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("spotId", spotId)
                .setParameter("area", area)
                .setParameter("district", district)
                .setParameter("limit", limit)
                .getResultList();

        return rows.stream()
                .map(row -> new PopularAreaSpotItem(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        parseMoodTagDisplayTags((String) row[3])
                ))
                .toList();
    }

    private List<String> parseMoodTagDisplayTags(String moodTagsJson) {
        if (moodTagsJson == null) {
            return List.of();
        }
        String stripped = moodTagsJson.replaceAll("[\\[\\]\"]", "");
        if (stripped.isBlank()) {
            return List.of();
        }
        return Arrays.stream(stripped.split(","))
                .map(String::trim)
                .map(MoodTag::fromKey)
                .map(MoodTag::getDisplayTag)
                .toList();
    }
}

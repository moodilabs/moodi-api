package com.moodi.spot.infrastructure.persistence;

import com.moodi.shared.mood.MoodTag;
import com.moodi.spot.application.SpotDetailQueryRepository;
import com.moodi.spot.application.dto.PopularAreaSpotItem;
import com.moodi.spot.application.dto.SimilarMoodSpotItem;
import com.moodi.spot.application.RegionDictionary;
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

        // "비슷한 무드의 스팟"은 겹치는 무드 태그가 많은 순이다. 예전에는 태그가
        // 하나라도 겹치면 되는 후보를 ORDER BY RANDOM() 으로 뽑아, 같은 스팟을 네 번
        // 열면 네 번 다 다른 목록이 나왔다(제주 해수욕장의 "비슷한 무드"로 인천 시장,
        // 서울역 아울렛 매장이 떴다). 추천이라고 내놓는 자리가 무작위여서는 안 된다.
        String sql = """
                SELECT s.id, st.title,
                       (SELECT si.image_url FROM spot_image si WHERE si.spot_id = s.id AND si.is_primary = true LIMIT 1),
                       s.area,
                       (SELECT COUNT(*) FROM bookmark b WHERE b.spot_id = s.id) AS bookmark_count,
                       sm.mood_tags
                FROM spot s
                JOIN spot_mood sm ON sm.spot_id = s.id
                JOIN spot_translation st ON st.spot_id = s.id AND st.locale = 'en-US'
                WHERE s.id != :spotId
                  AND s.status = 'PUBLISHED'
                  AND (s.route_excluded = false OR (s.content_type = 'SHOPPING' AND s.lcls_systm2 = 'SH06'))
                  AND jsonb_exists_any(sm.mood_tags, CAST(:moodTags AS text[]))
                ORDER BY (
                             SELECT COUNT(*)
                             FROM jsonb_array_elements_text(sm.mood_tags) AS t(tag)
                             WHERE t.tag = ANY(CAST(:moodTags AS text[]))
                         ) DESC,
                         bookmark_count DESC,
                         s.id ASC
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
                        RegionDictionary.translateArea((String) row[3]),
                        ((Number) row[4]).longValue(),
                        parseMoodTagDisplayTags((String) row[5])
                ))
                .toList();
    }

    @Override
    public List<PopularAreaSpotItem> findPopularSpotsByArea(Long spotId, String area, String district, int limit) {
        if (area == null || district == null) {
            return List.of();
        }

        String sql = """
                SELECT s.id, st.title,
                       (SELECT si.image_url FROM spot_image si WHERE si.spot_id = s.id AND si.is_primary = true LIMIT 1),
                       sm.mood_tags
                FROM spot s
                JOIN spot_translation st ON st.spot_id = s.id AND st.locale = 'en-US'
                LEFT JOIN spot_mood sm ON sm.spot_id = s.id
                WHERE s.id != :spotId
                  AND s.status = 'PUBLISHED'
                  AND (s.route_excluded = false OR (s.content_type = 'SHOPPING' AND s.lcls_systm2 = 'SH06'))
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

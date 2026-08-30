package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.application.PopularSpotReader;
import com.moodi.discovery.application.PopularSpotRow;
import com.moodi.discovery.infrastructure.region.RegionNames;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class PopularSpotReaderAdapter implements PopularSpotReader {

    private static final String DEFAULT_LOCALE = "ko-KR";

    private static final String SQL = """
            SELECT s.id, st.title, si.image_url, s.area, sd.content AS description,
                   COALESCE(bc.bookmark_count, 0) AS bookmark_count,
                   (bm.spot_id IS NOT NULL) AS bookmarked
            FROM spot s
            JOIN spot_translation st ON st.spot_id = s.id AND st.locale = :locale
            LEFT JOIN spot_description sd ON sd.spot_id = s.id AND sd.locale = :locale
            LEFT JOIN LATERAL (
                SELECT image_url FROM spot_image
                WHERE spot_id = s.id AND is_primary = true
                ORDER BY sort_order, id
                LIMIT 1
            ) si ON true
            LEFT JOIN (
                SELECT spot_id, COUNT(*) AS bookmark_count FROM bookmark GROUP BY spot_id
            ) bc ON bc.spot_id = s.id
            LEFT JOIN bookmark bm ON bm.spot_id = s.id AND bm.member_id = :memberId
            WHERE s.status = 'PUBLISHED'
            ORDER BY bookmark_count DESC, s.id ASC
            LIMIT :limit
            """;

    private final EntityManager em;

    public PopularSpotReaderAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<PopularSpotRow> readTopByBookmarkCount(UUID memberId, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(SQL)
                .setParameter("locale", DEFAULT_LOCALE)
                .setParameter("memberId", memberId)
                .setParameter("limit", limit)
                .getResultList();

        return rows.stream()
                .map(row -> new PopularSpotRow(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        RegionNames.toEnglishArea((String) row[3]),
                        (String) row[4],
                        ((Number) row[5]).longValue(),
                        (Boolean) row[6]
                ))
                .toList();
    }
}

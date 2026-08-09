package com.moodi.spot.infrastructure.persistence;

import com.moodi.spot.application.SpotBatchWriter;
import com.moodi.spot.application.SpotImportRow;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SpotBatchJdbcWriter implements SpotBatchWriter {

    private static final String UPSERT_SPOT = """
            INSERT INTO spot (content_id, content_type, area, district, neighborhood, source,
                              longitude, latitude, tel, route_excluded, status,
                              lcls_systm1, lcls_systm2, lcls_systm3, homepage, created_at, updated_at)
            VALUES (:contentId, :contentType, :area, :district, :neighborhood, :source,
                    :longitude, :latitude, :tel, :routeExcluded, 'TAGGING_PENDING',
                    :lclsSystm1, :lclsSystm2, :lclsSystm3, :homepage, :now, :now)
            ON CONFLICT (source, content_id) DO UPDATE SET
                content_type = EXCLUDED.content_type,
                area = EXCLUDED.area,
                district = EXCLUDED.district,
                neighborhood = EXCLUDED.neighborhood,
                longitude = EXCLUDED.longitude,
                latitude = EXCLUDED.latitude,
                tel = EXCLUDED.tel,
                route_excluded = EXCLUDED.route_excluded,
                lcls_systm1 = EXCLUDED.lcls_systm1,
                lcls_systm2 = EXCLUDED.lcls_systm2,
                lcls_systm3 = EXCLUDED.lcls_systm3,
                homepage = EXCLUDED.homepage,
                updated_at = EXCLUDED.updated_at
            RETURNING id, content_id, (xmax = 0) AS inserted
            """;

    private static final String UPSERT_TRANSLATION = """
            INSERT INTO spot_translation (spot_id, locale, title, overview, addr1, addr2, created_at, updated_at)
            VALUES (:spotId, 'ko-KR', :title, :overview, :addr1, :addr2, :now, :now)
            ON CONFLICT (spot_id, locale) DO UPDATE SET
                title = EXCLUDED.title,
                overview = EXCLUDED.overview,
                addr1 = EXCLUDED.addr1,
                addr2 = EXCLUDED.addr2,
                updated_at = EXCLUDED.updated_at
            """;

    private static final String DELETE_PRIMARY_IMAGES = """
            DELETE FROM spot_image WHERE spot_id IN (:spotIds) AND is_primary = true
            """;

    private static final String INSERT_IMAGE = """
            INSERT INTO spot_image (spot_id, image_url, is_primary, sort_order, created_at, updated_at)
            VALUES (:spotId, :imageUrl, true, 0, :now, :now)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SpotBatchJdbcWriter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public UpsertResult upsertAll(List<SpotImportRow> rows) {
        if (rows.isEmpty()) {
            return new UpsertResult(0, 0);
        }

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        SpotUpsertDetail detail = upsertSpots(rows, now);
        upsertTranslations(rows, detail.spotKeyToId, now);
        replaceImages(rows, detail.spotKeyToId, now);

        return new UpsertResult(detail.inserted, detail.updated);
    }

    private SpotUpsertDetail upsertSpots(List<SpotImportRow> rows, Timestamp now) {
        Map<SpotKey, Long> spotKeyToId = new HashMap<>();
        int inserted = 0;
        int updated = 0;

        for (SpotImportRow row : rows) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("contentId", row.contentId())
                    .addValue("contentType", row.contentType())
                    .addValue("area", row.area())
                    .addValue("district", row.district())
                    .addValue("neighborhood", row.neighborhood())
                    .addValue("source", row.source())
                    .addValue("longitude", row.longitude())
                    .addValue("latitude", row.latitude())
                    .addValue("tel", row.tel())
                    .addValue("routeExcluded", row.routeExcluded())
                    .addValue("lclsSystm1", row.lclsSystm1())
                    .addValue("lclsSystm2", row.lclsSystm2())
                    .addValue("lclsSystm3", row.lclsSystm3())
                    .addValue("homepage", row.homepage())
                    .addValue("now", now);

            Map<String, Object> returned = jdbcTemplate.queryForMap(UPSERT_SPOT, params);
            spotKeyToId.put(
                    new SpotKey(row.source(), row.contentId()),
                    (Long) returned.get("id")
            );

            if ((Boolean) returned.get("inserted")) {
                inserted++;
            } else {
                updated++;
            }
        }

        return new SpotUpsertDetail(spotKeyToId, inserted, updated);
    }

    private void upsertTranslations(List<SpotImportRow> rows, Map<SpotKey, Long> spotKeyToId, Timestamp now) {
        SqlParameterSource[] batchParams = rows.stream()
                .map(row -> new MapSqlParameterSource()
                        .addValue("spotId", spotKeyToId.get(new SpotKey(row.source(), row.contentId())))
                        .addValue("title", row.title())
                        .addValue("overview", row.overview())
                        .addValue("addr1", row.addr1())
                        .addValue("addr2", row.addr2())
                        .addValue("now", now))
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(UPSERT_TRANSLATION, batchParams);
    }

    private void replaceImages(List<SpotImportRow> rows, Map<SpotKey, Long> spotKeyToId, Timestamp now) {
        List<Long> allSpotIds = new ArrayList<>(spotKeyToId.values());
        jdbcTemplate.update(DELETE_PRIMARY_IMAGES, new MapSqlParameterSource("spotIds", allSpotIds));

        List<SqlParameterSource> imageParams = new ArrayList<>();
        for (SpotImportRow row : rows) {
            if (row.imageUrl() != null) {
                imageParams.add(new MapSqlParameterSource()
                        .addValue("spotId", spotKeyToId.get(new SpotKey(row.source(), row.contentId())))
                        .addValue("imageUrl", row.imageUrl())
                        .addValue("now", now));
            }
        }

        if (!imageParams.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_IMAGE, imageParams.toArray(SqlParameterSource[]::new));
        }
    }

    private record SpotKey(String source, String contentId) {
    }

    private record SpotUpsertDetail(Map<SpotKey, Long> spotKeyToId, int inserted, int updated) {
    }
}

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

    private static final int SPOT_SUB_BATCH_SIZE = 4000;

    private static final String UPSERT_SPOT_PREFIX = """
            INSERT INTO spot (content_id, content_type, area, district, neighborhood, source,
                              longitude, latitude, tel, route_excluded, status,
                              lcls_systm1, lcls_systm2, lcls_systm3, homepage, created_at, updated_at)
            VALUES\s""";

    private static final String UPSERT_SPOT_SUFFIX = """

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
            RETURNING id, source, content_id, (xmax = 0) AS inserted
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

        for (int batchStart = 0; batchStart < rows.size(); batchStart += SPOT_SUB_BATCH_SIZE) {
            List<SpotImportRow> batch = rows.subList(
                    batchStart, Math.min(batchStart + SPOT_SUB_BATCH_SIZE, rows.size()));

            StringBuilder sql = new StringBuilder(UPSERT_SPOT_PREFIX);
            MapSqlParameterSource params = new MapSqlParameterSource().addValue("now", now);

            for (int j = 0; j < batch.size(); j++) {
                if (j > 0) {
                    sql.append(", ");
                }
                sql.append(String.format(
                        "(:cid_%d, :ct_%d, :a_%d, :d_%d, :n_%d, :s_%d, "
                                + ":lon_%d, :lat_%d, :tel_%d, :re_%d, 'TAGGING_PENDING', "
                                + ":l1_%d, :l2_%d, :l3_%d, :hp_%d, :now, :now)",
                        j, j, j, j, j, j, j, j, j, j, j, j, j, j, j));

                SpotImportRow row = batch.get(j);
                params.addValue("cid_" + j, row.contentId())
                        .addValue("ct_" + j, row.contentType())
                        .addValue("a_" + j, row.area())
                        .addValue("d_" + j, row.district())
                        .addValue("n_" + j, row.neighborhood())
                        .addValue("s_" + j, row.source())
                        .addValue("lon_" + j, row.longitude())
                        .addValue("lat_" + j, row.latitude())
                        .addValue("tel_" + j, row.tel())
                        .addValue("re_" + j, row.routeExcluded())
                        .addValue("l1_" + j, row.lclsSystm1())
                        .addValue("l2_" + j, row.lclsSystm2())
                        .addValue("l3_" + j, row.lclsSystm3())
                        .addValue("hp_" + j, row.homepage());
            }

            sql.append(UPSERT_SPOT_SUFFIX);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params);
            for (Map<String, Object> returned : results) {
                spotKeyToId.put(
                        new SpotKey((String) returned.get("source"), (String) returned.get("content_id")),
                        (Long) returned.get("id"));
                if ((Boolean) returned.get("inserted")) {
                    inserted++;
                } else {
                    updated++;
                }
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
        List<Long> spotIdsWithImage = new ArrayList<>();
        List<SqlParameterSource> imageParams = new ArrayList<>();

        for (SpotImportRow row : rows) {
            if (row.imageUrl() != null) {
                Long spotId = spotKeyToId.get(new SpotKey(row.source(), row.contentId()));
                spotIdsWithImage.add(spotId);
                imageParams.add(new MapSqlParameterSource()
                        .addValue("spotId", spotId)
                        .addValue("imageUrl", row.imageUrl())
                        .addValue("now", now));
            }
        }

        if (!spotIdsWithImage.isEmpty()) {
            jdbcTemplate.update(DELETE_PRIMARY_IMAGES, new MapSqlParameterSource("spotIds", spotIdsWithImage));
            jdbcTemplate.batchUpdate(INSERT_IMAGE, imageParams.toArray(SqlParameterSource[]::new));
        }
    }

    private record SpotKey(String source, String contentId) {
    }

    private record SpotUpsertDetail(Map<SpotKey, Long> spotKeyToId, int inserted, int updated) {
    }
}

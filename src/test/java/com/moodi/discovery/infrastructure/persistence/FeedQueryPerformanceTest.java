package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.application.FeedQuery;
import com.moodi.discovery.infrastructure.persistence.FeedSpotReaderAdapter.PreparedSql;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.support.PostgresTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이슈 #21 (JSONB GIN 인덱스 기반 무드 필터링 성능 검증) — 피드 쿼리 기준.
 *
 * <p>기존 인덱스는 {@code idx_spot_mood_tags}(GIN, jsonb_path_ops)이고 이 opclass는 {@code @>}만
 * 지원한다. 설계 문서가 참조한 {@code ?|}는 opclass 문제 이전에 Hibernate 네이티브 쿼리에서 아예
 * 실행되지 않으므로({@code ?}를 JDBC 파라미터로 오인) 피드는 {@code @>} OR 체인으로 짰다.
 */
class FeedQueryPerformanceTest extends PostgresTestSupport {

    private static final int SPOT_COUNT = 50_000;
    private static final int MEMBER_COUNT = 100;
    private static final int IMPRESSIONS_PER_MEMBER = 500;
    private static final int MOOD_TAG_POOL_SIZE = 20;

    /** 전체의 0.1%만 가진 희소 태그. 선택도가 높을 때 인덱스를 타는지 보려고 심는다. */
    private static final String RARE_MOOD_TAG = "neon";
    private static final int RARE_TAG_SPOT_COUNT = 50;

    private static final long SLOW_QUERY_THRESHOLD_MS = 3_000L;
    private static final Pattern EXECUTION_TIME = Pattern.compile("Execution Time: ([0-9.]+) ms");

    @Autowired
    private EntityManager em;

    private FeedSpotReaderAdapter adapter;
    private UUID memberId;

    @BeforeEach
    void seedLargeCatalog() {
        adapter = new FeedSpotReaderAdapter(em);
        insertSpots();
        insertMembersAndImpressions();
        insertBookmarks();
        analyze();
        memberId = firstPerfMemberId();
    }

    @Test
    @DisplayName("선택도가 높은 무드 필터는 GIN 인덱스를 탄다")
    void selective_mood_filter_uses_gin_index() {
        String plan = explain("""
                EXPLAIN (ANALYZE, BUFFERS)
                SELECT sm.spot_id FROM spot_mood sm
                WHERE sm.mood_tags @> CAST(:moodTag AS jsonb)
                """, "moodTag", "[\"" + RARE_MOOD_TAG + "\"]");

        System.out.println("[PLAN] 희소 무드 필터\n" + plan);
        assertThat(plan).contains("idx_spot_mood_tags");
    }

    @Test
    @DisplayName("노출 이력 조회는 (member_id, shown_at) 인덱스를 탄다")
    void impression_lookup_uses_member_index() {
        String plan = explain("""
                EXPLAIN (ANALYZE, BUFFERS)
                SELECT fi.spot_id FROM feed_impression fi
                WHERE fi.member_id = :memberId AND fi.shown_at > :from
                """, "memberId", memberId, "from", LocalDateTime.now().minusDays(30));

        System.out.println("[PLAN] 노출 이력 조회\n" + plan);
        assertThat(plan).contains("idx_feed_impression_member_shown_at");
    }

    @Test
    @DisplayName("5만 건 카탈로그에서 개인화 피드 첫 페이지 실행 계획과 소요 시간")
    void personalized_feed_first_page_at_scale() {
        PreparedSql prepared = adapter.personalizedFeedSql(feedQuery());

        String plan = explainPrepared(prepared);
        double executionMs = parseExecutionTime(plan);

        System.out.println("[PLAN] 개인화 피드 1페이지 (스팟 " + SPOT_COUNT + "건)\n" + plan);
        System.out.println("[TIME] 개인화 피드 1페이지 = " + executionMs + " ms");
        assertThat(executionMs).isLessThan(SLOW_QUERY_THRESHOLD_MS);
    }

    @Test
    @DisplayName("5만 건 카탈로그에서 비회원 피드 첫 페이지 실행 계획과 소요 시간")
    void guest_feed_first_page_at_scale() {
        FeedQuery query = new FeedQuery(null, List.of(), "seed", LocalDateTime.now(),
                LocalDateTime.now().minusDays(30), null, 21);
        PreparedSql prepared = adapter.guestFeedSql(query);

        String plan = explainPrepared(prepared);
        double executionMs = parseExecutionTime(plan);

        System.out.println("[PLAN] 비회원 피드 1페이지 (스팟 " + SPOT_COUNT + "건)\n" + plan);
        System.out.println("[TIME] 비회원 피드 1페이지 = " + executionMs + " ms");
        assertThat(executionMs).isLessThan(SLOW_QUERY_THRESHOLD_MS);
    }

    private FeedQuery feedQuery() {
        LocalDateTime sessionAt = LocalDateTime.now();
        List<String> moodTagKeys = List.of(MoodTag.COZY.getKey(), MoodTag.SERENE.getKey(),
                MoodTag.RETRO.getKey());
        return new FeedQuery(memberId, moodTagKeys, "seed", sessionAt,
                sessionAt.minusDays(30), null, 21);
    }

    private void insertSpots() {
        em.createNativeQuery("""
                INSERT INTO spot (content_id, content_type, area, source, longitude, latitude,
                                  route_excluded, status, created_at, updated_at)
                SELECT 'perf-' || g, 'TOURIST_ATTRACTION', '서울', 'kor_service', 126.0, 37.0,
                       false, 'PUBLISHED', now(), now()
                FROM generate_series(1, :count) g
                """).setParameter("count", SPOT_COUNT).executeUpdate();

        em.createNativeQuery("""
                INSERT INTO spot_translation (spot_id, locale, title, created_at, updated_at)
                SELECT s.id, 'en-US', '스팟-' || s.id, now(), now()
                FROM spot s WHERE s.content_id LIKE 'perf-%'
                """).executeUpdate();

        em.createNativeQuery("""
                INSERT INTO spot_image (spot_id, image_url, is_primary, sort_order, created_at, updated_at)
                SELECT s.id, 'https://img/' || s.id, true, 0, now(), now()
                FROM spot s WHERE s.content_id LIKE 'perf-%'
                """).executeUpdate();

        em.createNativeQuery("""
                INSERT INTO spot_mood (spot_id, mood_vector, mood_tags, confidence, created_at, updated_at)
                SELECT s.id, '{}'::jsonb,
                       to_jsonb(ARRAY[pool[1 + (s.id % :poolSize)], pool[1 + ((s.id * 7) % :poolSize)]]),
                       0.9, now(), now()
                FROM spot s,
                     (SELECT ARRAY['nature','ocean','cityscape','riverside','countryside',
                                   'expansive','traditional','local','retro','industrial',
                                   'modern','cozy','serene','lively','romantic',
                                   'moody','golden_hour','neon','artsy','seasonal'] AS pool) p
                WHERE s.content_id LIKE 'perf-%'
                """).setParameter("poolSize", MOOD_TAG_POOL_SIZE).executeUpdate();

        // 희소 태그를 만들기 위해 pool에서 나온 neon을 전부 걷어내고 소수 스팟에만 다시 심는다
        em.createNativeQuery("""
                UPDATE spot_mood
                SET mood_tags = to_jsonb(ARRAY['modern','local'])
                WHERE mood_tags @> CAST('["neon"]' AS jsonb)
                """).executeUpdate();

        em.createNativeQuery("""
                UPDATE spot_mood
                SET mood_tags = to_jsonb(ARRAY['neon','modern'])
                WHERE spot_id IN (SELECT spot_id FROM spot_mood ORDER BY spot_id LIMIT :count)
                """).setParameter("count", RARE_TAG_SPOT_COUNT).executeUpdate();
    }

    private void insertMembersAndImpressions() {
        em.createNativeQuery("""
                INSERT INTO member (id, provider, provider_id, email, status, created_at, updated_at)
                SELECT gen_random_uuid(), 'GOOGLE', 'perf-sub-' || g, 'perf' || g || '@test.com',
                       'ACTIVE', now(), now()
                FROM generate_series(1, :count) g
                """).setParameter("count", MEMBER_COUNT).executeUpdate();

        em.createNativeQuery("""
                INSERT INTO feed_impression (member_id, spot_id, shown_at)
                SELECT m.id, s.id, now() - (s.id % 40) * interval '1 day'
                FROM (SELECT id FROM member WHERE provider_id LIKE 'perf-sub-%') m
                CROSS JOIN LATERAL (
                    SELECT id FROM spot WHERE content_id LIKE 'perf-%' ORDER BY id LIMIT :perMember
                ) s
                """).setParameter("perMember", IMPRESSIONS_PER_MEMBER).executeUpdate();
    }

    private void insertBookmarks() {
        em.createNativeQuery("""
                INSERT INTO bookmark (member_id, spot_id, created_at, updated_at)
                SELECT m.id, s.id, now(), now()
                FROM (SELECT id FROM member WHERE provider_id LIKE 'perf-sub-%') m
                CROSS JOIN LATERAL (
                    SELECT id FROM spot WHERE content_id LIKE 'perf-%' ORDER BY id LIMIT 100
                ) s
                """).executeUpdate();
    }

    private void analyze() {
        List.of("spot", "spot_mood", "spot_translation", "spot_image", "bookmark", "feed_impression")
                .forEach(table -> em.createNativeQuery("ANALYZE " + table).executeUpdate());
    }

    private UUID firstPerfMemberId() {
        return (UUID) em.createNativeQuery(
                        "SELECT id FROM member WHERE provider_id = 'perf-sub-1'")
                .getSingleResult();
    }

    private String explain(String sql, Object... nameValuePairs) {
        Query query = em.createNativeQuery(sql);
        for (int i = 0; i < nameValuePairs.length; i += 2) {
            query.setParameter((String) nameValuePairs[i], nameValuePairs[i + 1]);
        }
        return toPlanText(query);
    }

    private String explainPrepared(PreparedSql prepared) {
        Query query = em.createNativeQuery("EXPLAIN (ANALYZE, BUFFERS) " + prepared.sql());
        prepared.params().forEach(query::setParameter);
        return toPlanText(query);
    }

    private String toPlanText(Query query) {
        @SuppressWarnings("unchecked")
        List<Object> lines = query.getResultList();
        return lines.stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
    }

    private double parseExecutionTime(String plan) {
        Matcher matcher = EXECUTION_TIME.matcher(plan);
        assertThat(matcher.find()).as("EXPLAIN ANALYZE 결과에 Execution Time이 있어야 한다").isTrue();
        return Double.parseDouble(matcher.group(1));
    }
}

-- 컨텍스트 경계를 넘는 FK 제거.
--
-- 루트 CLAUDE.md 규칙: "컨텍스트 간 참조는 ID(+필요 스냅샷)만 — JPA 연관관계를 경계 너머로 걸지 않는다".
-- 도메인 모델과 orm.xml은 이 규칙을 지켜 왔지만(경계를 넘는 연관관계 매핑 없음, 전부 ID 컬럼),
-- DB 레벨에는 FK가 남아 있어 스키마가 문서와 어긋나 있었다.
--
-- 이미 적용된 마이그레이션은 Flyway checksum 때문에 수정할 수 없으므로 여기서 DROP 한다.
-- 컨텍스트 내부 FK 12개는 그대로 둔다
-- (fk_bookmark_spot · fk_route_day_route · fk_route_spot_route_day · fk_route_leg_route_day ·
--  fk_spot_image_spot · fk_spot_mood_spot · fk_spot_translation_spot · spot_description_spot_id_fkey ·
--  fk_member_agreement_member · fk_member_preferred_mood_member ·
--  pick_request_area_pick_request_id_fkey · pick_result_spot_pick_request_id_fkey).

-- FK DROP은 자식뿐 아니라 **부모 테이블(member·spot)에도** ACCESS EXCLUSIVE 락을 잡는다.
-- 카탈로그만 건드려 실행은 순식간이지만, 스팟 배치나 느린 조회가 락을 쥐고 있으면
-- 그 뒤의 모든 member·spot 쿼리가 대기 큐에 줄서서 사실상 전면 중단이 된다.
-- 3초 안에 못 잡으면 배포만 실패시키고 운영 트래픽은 건드리지 않는다.
-- DROP CONSTRAINT IF EXISTS는 멱등이라 재시도 비용이 없다.
SET LOCAL lock_timeout = '3s';

-- 스팟 → 회원 (V4)
ALTER TABLE bookmark DROP CONSTRAINT IF EXISTS fk_bookmark_member;

-- 루트 → 회원 (V7)
ALTER TABLE route DROP CONSTRAINT IF EXISTS fk_route_member;

-- 추천 → 회원 (V11)
ALTER TABLE feed_impression DROP CONSTRAINT IF EXISTS fk_feed_impression_member;

-- 추천 → 스팟 (V11)
ALTER TABLE feed_impression DROP CONSTRAINT IF EXISTS fk_feed_impression_spot;

-- 추천 → 스팟 (V14). 인라인 REFERENCES라 PostgreSQL이 {table}_{column}_fkey 로 자동 명명한다.
ALTER TABLE pick_result_spot DROP CONSTRAINT IF EXISTS pick_result_spot_spot_id_fkey;

-- IF EXISTS는 이름이 틀려도 NOTICE만 남기고 성공으로 기록된다.
-- "FK가 안 지워졌는데 배포는 성공"을 막기 위해 결과를 직접 검증한다.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint c
                 JOIN pg_class child ON child.oid = c.conrelid
                 JOIN pg_class parent ON parent.oid = c.confrelid
        WHERE c.contype = 'f'
          AND (child.relname, parent.relname) IN (
                                                  ('bookmark', 'member'),
                                                  ('route', 'member'),
                                                  ('feed_impression', 'member'),
                                                  ('feed_impression', 'spot'),
                                                  ('pick_result_spot', 'spot')
            )
    ) THEN
        RAISE EXCEPTION '컨텍스트 경계를 넘는 FK가 남아 있습니다. 제약조건 이름을 확인하세요.';
    END IF;
END
$$;

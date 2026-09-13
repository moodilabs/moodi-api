-- 쇼핑 카테고리의 route_excluded를 true로 변경한다.
-- 시장(SH06)을 포함한 모든 쇼핑 스팟에 적용하며,
-- SH06 예외 처리는 추천 쿼리에서 담당한다.
UPDATE spot
SET route_excluded = true,
    updated_at     = now()
WHERE content_type = 'SHOPPING'
  AND route_excluded = false;

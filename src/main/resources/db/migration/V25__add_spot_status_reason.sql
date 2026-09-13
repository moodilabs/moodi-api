-- 어드민 스팟 상태 관리 (ADM-F06). HIDDEN / DELETED 전환 사유와 시각을 남긴다.
-- 앱은 status = 'PUBLISHED' 만 읽으므로 새 상태값은 마이그레이션 없이 바로 미노출된다.
ALTER TABLE spot
    ADD COLUMN status_reason     VARCHAR(200),
    ADD COLUMN status_changed_at TIMESTAMP;

-- 어드민 목록: 상태 필터 + id 커서
CREATE INDEX idx_spot_status_id ON spot (status, id DESC);

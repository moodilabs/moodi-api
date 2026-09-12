-- 어드민 회원 정지 (ADM-F04). status = 'SUSPENDED' 와 함께 정지 시각·사유를 남긴다.
ALTER TABLE member
    ADD COLUMN suspended_at   TIMESTAMP,
    ADD COLUMN suspend_reason VARCHAR(200);

-- 어드민 목록: ORDER BY created_at DESC, id DESC (커서 페이징)
CREATE INDEX idx_member_created ON member (created_at DESC, id DESC);

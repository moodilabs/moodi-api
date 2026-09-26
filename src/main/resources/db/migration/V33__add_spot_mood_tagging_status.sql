-- 무드 태깅 상태 추적 필드 추가
ALTER TABLE spot
    ADD COLUMN mood_tagging_status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    ADD COLUMN mood_tagging_attempt_count   INT          NOT NULL DEFAULT 0,
    ADD COLUMN mood_tagging_last_error      VARCHAR(500),
    ADD COLUMN mood_tagging_last_attempted_at TIMESTAMP,
    ADD COLUMN mood_tagging_next_retry_at   TIMESTAMP,
    ADD COLUMN mood_tagging_processing_started_at TIMESTAMP;

-- 기존 데이터 마이그레이션: spot_mood가 있는 스팟은 COMPLETED
UPDATE spot s
SET mood_tagging_status = 'COMPLETED',
    mood_tagging_attempt_count = 1
WHERE EXISTS (SELECT 1 FROM spot_mood sm WHERE sm.spot_id = s.id);

-- 배치 대상 조회용 인덱스
CREATE INDEX idx_spot_mood_tagging_status ON spot (mood_tagging_status)
    WHERE mood_tagging_status IN ('PENDING', 'RETRY_WAIT', 'PROCESSING');

-- 탈퇴 사유 (MY-03-02). 회원 행은 개인정보만 비워 남고, 사유는 여기 쌓인다.
-- 재가입 후 다시 탈퇴할 수 있어 member_id 는 unique 가 아니다.
CREATE TABLE member_withdrawal (
    id         UUID         PRIMARY KEY,
    member_id  UUID         NOT NULL,
    detail     VARCHAR(500),
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_member_withdrawal_member_created ON member_withdrawal (member_id, created_at DESC);

CREATE TABLE member_withdrawal_reason (
    withdrawal_id UUID        NOT NULL REFERENCES member_withdrawal (id),
    reason        VARCHAR(40) NOT NULL,
    PRIMARY KEY (withdrawal_id, reason)
);

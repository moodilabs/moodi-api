-- 1:1 문의 (MY-06). 회원이 등록하고 어드민이 답변한다. 답변은 1:1이라 inquiry 컬럼에 접는다.
-- member_id·answered_by는 컨텍스트 경계를 넘는 참조라 FK를 걸지 않는다.
CREATE TABLE inquiry (
    id             UUID          PRIMARY KEY,
    member_id      UUID          NOT NULL,
    topic          VARCHAR(30)   NOT NULL,
    subject        VARCHAR(60)   NOT NULL,
    content        VARCHAR(1000) NOT NULL,
    status         VARCHAR(20)   NOT NULL,   -- RECEIVED | ANSWERED
    answer_content TEXT,
    answered_by    UUID,                     -- admin_account.id
    answered_at    TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL
);

CREATE INDEX idx_inquiry_member_created ON inquiry (member_id, created_at DESC, id DESC);
CREATE INDEX idx_inquiry_status_created ON inquiry (status, created_at DESC, id DESC);

-- 첨부는 비공개 버킷의 객체 키만 저장한다. 읽기 URL은 조회 시점에 서명한다.
CREATE TABLE inquiry_attachment (
    inquiry_id   UUID         NOT NULL REFERENCES inquiry (id),
    object_key   VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    sort_order   INT          NOT NULL,
    PRIMARY KEY (inquiry_id, sort_order)
);

-- 관리자 계정·세션. 회원(member)과 완전히 분리된 신원이며 관리자 프론트(admin.moodi.kr)에서만 쓴다.
-- 최초 계정은 여기서 심지 않고 기동 시 ADMIN_BOOTSTRAP_EMAIL / ADMIN_BOOTSTRAP_PASSWORD 로 만든다.
CREATE TABLE admin_account (
    id                 UUID         PRIMARY KEY,
    email              VARCHAR(255) NOT NULL,
    password_hash      VARCHAR(100) NOT NULL,
    name               VARCHAR(50)  NOT NULL,
    role               VARCHAR(20)  NOT NULL,   -- SUPER | OPERATOR
    status             VARCHAR(20)  NOT NULL,   -- ACTIVE | DISABLED
    failed_login_count INT          NOT NULL DEFAULT 0,
    locked_until       TIMESTAMP,
    last_login_at      TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    CONSTRAINT uk_admin_account_email UNIQUE (email)
);

CREATE TABLE admin_refresh_token (
    id         UUID         PRIMARY KEY,
    admin_id   UUID         NOT NULL REFERENCES admin_account (id),
    token      VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    CONSTRAINT uk_admin_refresh_token_token UNIQUE (token)
);

CREATE INDEX idx_admin_refresh_token_admin_id ON admin_refresh_token (admin_id);

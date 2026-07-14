CREATE TABLE member (
    id          UUID PRIMARY KEY,
    provider    VARCHAR(20)  NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    email       VARCHAR(255),
    nickname    VARCHAR(50),
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT uk_member_provider UNIQUE (provider, provider_id),
    CONSTRAINT uk_member_email UNIQUE (email)
);

CREATE TABLE refresh_token (
    id         UUID PRIMARY KEY,
    member_id  UUID         NOT NULL,
    token      VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    CONSTRAINT uk_refresh_token_token UNIQUE (token)
);

CREATE INDEX idx_refresh_token_member_id ON refresh_token (member_id);

ALTER TABLE member
    ADD COLUMN country    VARCHAR(2),
    ADD COLUMN birth_year INT,
    ADD COLUMN gender     VARCHAR(10);

ALTER TABLE member
    ADD CONSTRAINT uk_member_nickname UNIQUE (nickname);

CREATE TABLE member_agreement (
    id         UUID PRIMARY KEY,
    member_id  UUID        NOT NULL,
    type       VARCHAR(30) NOT NULL,
    agreed     BOOLEAN     NOT NULL,
    agreed_at  TIMESTAMP,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    CONSTRAINT fk_member_agreement_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT uk_member_agreement_member_type UNIQUE (member_id, type)
);

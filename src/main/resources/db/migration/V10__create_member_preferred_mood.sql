CREATE TABLE member_preferred_mood (
    id         UUID PRIMARY KEY,
    member_id  UUID        NOT NULL,
    mood       VARCHAR(30) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    CONSTRAINT fk_member_preferred_mood_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT uk_member_preferred_mood_member_mood UNIQUE (member_id, mood)
);

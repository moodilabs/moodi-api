CREATE TABLE member_preferred_vector (
    member_id   UUID PRIMARY KEY,
    mood_vector JSONB     NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

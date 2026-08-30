CREATE TABLE pick_request (
    id         UUID         NOT NULL PRIMARY KEY,
    member_id  UUID         NOT NULL,
    image_key  VARCHAR(500) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_pick_request_member_created
    ON pick_request (member_id, created_at DESC);

CREATE TABLE pick_request_area (
    id              UUID        NOT NULL PRIMARY KEY,
    pick_request_id UUID        NOT NULL REFERENCES pick_request (id),
    level           VARCHAR(20) NOT NULL,
    region          VARCHAR(50) NOT NULL,
    district        VARCHAR(50),
    neighborhood    VARCHAR(50),
    sort_order      INT         NOT NULL,
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL
);

CREATE INDEX idx_pick_request_area_request
    ON pick_request_area (pick_request_id);

CREATE TABLE pick_result_spot (
    id              UUID             NOT NULL PRIMARY KEY,
    pick_request_id UUID             NOT NULL REFERENCES pick_request (id),
    spot_id         BIGINT           NOT NULL REFERENCES spot (id),
    rank            INT              NOT NULL,
    similarity      DOUBLE PRECISION NOT NULL,
    fallback        BOOLEAN          NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP        NOT NULL,
    updated_at      TIMESTAMP        NOT NULL
);

CREATE INDEX idx_pick_result_spot_request
    ON pick_result_spot (pick_request_id, fallback, rank);

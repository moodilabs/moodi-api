ALTER TABLE spot
    ADD COLUMN district VARCHAR(30),
    ADD COLUMN neighborhood VARCHAR(30);

CREATE INDEX idx_spot_area_district
    ON spot (area, district);

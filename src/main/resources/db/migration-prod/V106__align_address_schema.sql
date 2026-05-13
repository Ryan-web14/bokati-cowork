-- The address table in V3 was created with legacy columns that don't match
-- the Address entity. Add the missing columns, replace the VARCHAR country
-- column with a BIGINT FK to the country table (created in V11), and
-- set created_at / updated_at required by Hibernate validation.

ALTER TABLE address
    ADD COLUMN IF NOT EXISTS street_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS street_name   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS district      VARCHAR(120),
    ADD COLUMN IF NOT EXISTS created_at    TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at    TIMESTAMP;

UPDATE address SET created_at = NOW() WHERE created_at IS NULL;

ALTER TABLE address ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE address ALTER COLUMN created_at SET DEFAULT NOW();

-- Replace the legacy VARCHAR country column with a BIGINT FK.
-- Safe on a fresh database (no data to migrate).
ALTER TABLE address DROP COLUMN IF EXISTS country;
ALTER TABLE address ADD COLUMN country BIGINT;

ALTER TABLE address
    ADD CONSTRAINT address_country_fk
        FOREIGN KEY (country) REFERENCES country(id);
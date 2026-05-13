-- V5 incorrectly declared resource_amenity_link.resource_id as BIGSERIAL.
-- It is a FK column (not a PK) and must not carry an auto-increment default.
-- Drop the spurious sequence and default; the column type is already BIGINT.

ALTER TABLE resource_amenity_link ALTER COLUMN resource_id DROP DEFAULT;
DROP SEQUENCE IF EXISTS resource_amenity_link_resource_id_seq;
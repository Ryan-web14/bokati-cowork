-- BIGSERIAL was incorrectly used on FK/reference columns in V4.
-- BIGSERIAL = BIGINT + implicit sequence + DEFAULT nextval().
-- Drop the spurious defaults and sequences; leave the columns as plain BIGINT.

ALTER TABLE document ALTER COLUMN document_type_id DROP DEFAULT;
DROP SEQUENCE IF EXISTS document_document_type_id_seq;

DROP SEQUENCE IF EXISTS document_review_document_id_seq;

ALTER TABLE document_review ALTER COLUMN reviewed_by DROP DEFAULT;
DROP SEQUENCE IF EXISTS document_review_reviewed_by_seq;

ALTER TABLE kyc_verification ALTER COLUMN kyc_document_id DROP DEFAULT;
DROP SEQUENCE IF EXISTS kyc_verification_kyc_document_id_seq;

ALTER TABLE kyc_verification ALTER COLUMN verified_by DROP DEFAULT;
DROP SEQUENCE IF EXISTS kyc_verification_verified_by_seq;

ALTER TABLE legal_document ALTER COLUMN uploaded_by DROP DEFAULT;
DROP SEQUENCE IF EXISTS legal_document_uploaded_by_seq;
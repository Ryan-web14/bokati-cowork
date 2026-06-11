-- Ensure the NEEDS_CORRECTION value is accepted for the document status column.
-- Hibernate stores enums as VARCHAR so no ALTER TYPE needed for PostgreSQL with string enums.
-- This migration is a documentation marker only.
-- No DDL required.
SELECT 1;

-- Hibernate validation expects integer (int4) for Java Integer fields.
-- These columns were created as smallint (int2); widening to int4 is lossless.
ALTER TABLE booking ALTER COLUMN csat_score TYPE integer;
ALTER TABLE support_ticket ALTER COLUMN csat_score TYPE integer;

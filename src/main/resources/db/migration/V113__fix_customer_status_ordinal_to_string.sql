-- customer.status may be SMALLINT or VARCHAR storing ordinal values.
-- Use a shadow column to avoid type-coercion issues in CASE expressions.

-- 1. Drop any existing check constraints on status
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN
        SELECT conname FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        WHERE t.relname = 'customer' AND c.contype = 'c'
          AND pg_get_constraintdef(c.oid) ILIKE '%status%'
    LOOP
        EXECUTE 'ALTER TABLE customer DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname);
    END LOOP;
END $$;

-- 2. Add a clean VARCHAR column
ALTER TABLE customer ADD COLUMN IF NOT EXISTS status_new VARCHAR(20);

-- 3. Populate it — cast status to text first so it works for both SMALLINT and VARCHAR
-- CustomerStatus ordinal mapping: 0=ACTIVE, 1=PENDING, 2=INACTIVE, 3=SUSPENDED, 4=ARCHIVED
UPDATE customer
SET status_new = CASE status::text
    WHEN '0' THEN 'ACTIVE'
    WHEN '1' THEN 'PENDING'
    WHEN '2' THEN 'INACTIVE'
    WHEN '3' THEN 'SUSPENDED'
    WHEN '4' THEN 'ARCHIVED'
    ELSE status::text
END;

-- 4. Swap columns
ALTER TABLE customer DROP COLUMN status;
ALTER TABLE customer RENAME COLUMN status_new TO status;
ALTER TABLE customer ALTER COLUMN status SET NOT NULL;

-- 5. Add correct constraint
ALTER TABLE customer
    ADD CONSTRAINT ck_customer_status
        CHECK (status IN ('ACTIVE', 'PENDING', 'INACTIVE', 'SUSPENDED', 'ARCHIVED'));
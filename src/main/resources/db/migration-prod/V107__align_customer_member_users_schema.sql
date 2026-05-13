-- customer: add soft-delete support and is_member flag expected by entity
ALTER TABLE customer
    ADD COLUMN IF NOT EXISTS is_member BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted  BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_customer_deleted ON customer(deleted);

-- customer: V3 declared address_id NOT NULL but the entity allows a customer
-- without an address (address is optional at creation time)
ALTER TABLE customer ALTER COLUMN address_id DROP NOT NULL;

-- member: V3 created the status column as "status" but the entity maps
-- to "member_status". Rename only if the old name still exists.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name   = 'member'
          AND column_name  = 'status'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name   = 'member'
          AND column_name  = 'member_status'
    ) THEN
        ALTER TABLE member RENAME COLUMN status TO member_status;
    END IF;
END $$;

-- member: add create_by_admin flag expected by entity
ALTER TABLE member
    ADD COLUMN IF NOT EXISTS create_by_admin BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_member_deleted ON member(deleted);

-- member: V3 added address_id NOT NULL but the Member entity has no address
-- field — the address is accessed through the linked Customer instead.
-- Drop the FK constraint first, then the column.
ALTER TABLE member DROP CONSTRAINT IF EXISTS fk_member_address;
ALTER TABLE member DROP COLUMN IF EXISTS address_id;

-- users: V3 declared the column as "passwordHash"; PostgreSQL normalises
-- unquoted camelCase identifiers to lowercase → "passwordhash".
-- The entity maps to "password_hash" (snake_case) — rename to match.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name   = 'users'
          AND column_name  = 'passwordhash'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name   = 'users'
          AND column_name  = 'password_hash'
    ) THEN
        ALTER TABLE users RENAME COLUMN passwordhash TO password_hash;
    END IF;
END $$;

-- customer: V3 declared address_id NOT NULL but the entity allows a customer
-- without an address (address is optional at creation time)
ALTER TABLE customer ALTER COLUMN address_id DROP NOT NULL;

-- member: V3 added address_id NOT NULL but the Member entity has no address
-- field — the address is accessed through the linked Customer instead.
-- Drop the FK constraint first, then the column.
ALTER TABLE member DROP CONSTRAINT IF EXISTS fk_member_address;
ALTER TABLE member DROP COLUMN IF EXISTS address_id;
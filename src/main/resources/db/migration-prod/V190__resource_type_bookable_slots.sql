-- ============================================================
-- V190: Separate person-capacity from concurrent booking slots
--
-- Resource.capacity = how many people a resource can accommodate (display only)
-- ResourceType.bookable_slots = max concurrent bookings allowed at one time
--
-- For meeting rooms (RTY-001): seating capacity can be > 1
-- but only 1 booking can occupy the room per time slot.
-- ============================================================

ALTER TABLE resource_type
    ADD COLUMN IF NOT EXISTS bookable_slots INTEGER;

-- Meeting rooms are exclusive-use: one booking per time slot regardless of seating capacity.
UPDATE resource_type
SET bookable_slots = 1
WHERE code = 'RTY-001';

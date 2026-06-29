ALTER TABLE member_profile ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(255);
ALTER TABLE member_profile ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(50);

ALTER TABLE member
    ADD COLUMN IF NOT EXISTS portal_activated_at TIMESTAMPTZ;

UPDATE member
SET portal_activated_at = updated_at
WHERE portal_access = true
  AND member_status = 'ACTIVE'
  AND portal_activated_at IS NULL;

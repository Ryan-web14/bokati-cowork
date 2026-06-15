ALTER TABLE member
    ADD COLUMN IF NOT EXISTS kyc_grace_period_end_at TIMESTAMPTZ;

-- Rétro-remplit les membres déjà actifs avec la grace period par défaut (7 jours)
UPDATE member
SET kyc_grace_period_end_at = portal_activated_at + INTERVAL '7 days'
WHERE portal_activated_at IS NOT NULL
  AND kyc_grace_period_end_at IS NULL;

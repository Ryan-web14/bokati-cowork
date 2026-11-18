-- Ressources, lot R1 : moteur de prix unique. Conditions nouvelles des regles tarifaires et trace
-- du prix enregistree avec la reservation.
ALTER TABLE resource_pricing_rule ADD COLUMN IF NOT EXISTS days_of_week VARCHAR(20);
ALTER TABLE resource_pricing_rule ADD COLUMN IF NOT EXISTS customer_segment VARCHAR(30);
ALTER TABLE resource_pricing_rule ADD COLUMN IF NOT EXISTS plan_code VARCHAR(60);
ALTER TABLE resource_pricing_rule ADD COLUMN IF NOT EXISTS min_participants INTEGER;
ALTER TABLE resource_pricing_rule ADD COLUMN IF NOT EXISTS max_participants INTEGER;
ALTER TABLE resource_pricing_rule ADD COLUMN IF NOT EXISTS min_duration_minutes INTEGER;
ALTER TABLE resource_pricing_rule ADD COLUMN IF NOT EXISTS max_duration_minutes INTEGER;
ALTER TABLE resource_pricing_rule ADD COLUMN IF NOT EXISTS min_lead_minutes INTEGER;
ALTER TABLE resource_pricing_rule ADD COLUMN IF NOT EXISTS channel VARCHAR(20);
ALTER TABLE resource_pricing_rule ADD COLUMN IF NOT EXISTS stackable BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE resource_pricing_rule ADD COLUMN IF NOT EXISTS per_participant BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE booking ADD COLUMN IF NOT EXISTS price_trace TEXT;

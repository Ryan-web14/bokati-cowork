-- Centre d'audit, lot A1 : element principal touche par chaque action (type et code lisible),
-- pour retrouver tout ce qui est arrive a une facture, un membre ou une reservation.
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS entity_type VARCHAR(80);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS entity_code VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_audit_log_entity ON audit_log (entity_type, entity_code, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor_created ON audit_log (actor_email, created_at);

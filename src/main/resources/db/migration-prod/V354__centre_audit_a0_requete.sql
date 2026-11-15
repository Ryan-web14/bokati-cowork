-- Centre d'audit, lot A0 : identifiant de requete (en-tete X-Request-Id) porte par chaque ligne
-- d'audit et chaque evenement de la boite de sortie, pour suivre une requete de bout en bout.
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS request_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS ix_audit_log_request_id ON audit_log (request_id) WHERE request_id IS NOT NULL;
ALTER TABLE outbox_event ADD COLUMN IF NOT EXISTS request_id VARCHAR(64);

-- Centre d'audit, lot A7 : journal des actions et evenements de securite chaines par empreinte
-- (SHA-256 du contenu et de l'empreinte precedente), en ajout seul. Une ligne ne peut pas etre
-- modifiee ; elle ne peut etre supprimee que par la purge de conservation (bokati.audit_purge).
-- Le chainage commence a cette migration : les lignes plus anciennes ne sont pas reprises.

ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS chain_seq BIGINT;
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS previous_hash VARCHAR(64);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS current_hash VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS ux_audit_log_chain ON audit_log (chain_seq) WHERE chain_seq IS NOT NULL;

ALTER TABLE security_event ADD COLUMN IF NOT EXISTS chain_seq BIGINT;
ALTER TABLE security_event ADD COLUMN IF NOT EXISTS previous_hash VARCHAR(64);
ALTER TABLE security_event ADD COLUMN IF NOT EXISTS current_hash VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS ux_security_event_chain ON security_event (chain_seq) WHERE chain_seq IS NOT NULL;

CREATE OR REPLACE FUNCTION audit_log_hash(prev TEXT, r audit_log) RETURNS TEXT AS $$
    SELECT encode(sha256(convert_to(jsonb_build_array(
        prev, r.chain_seq, extract(epoch FROM r.created_at), r.actor_id, r.actor_email, r.action, r.ressource,
        r.audit_status, r.module, r.ip_address, r.user_agent, r.session_id, r.request_id, r.error_code,
        r.error_message, r.metadata_json, r.diff_json, r.entity_type, r.entity_code)::TEXT, 'UTF8')), 'hex')
$$ LANGUAGE sql IMMUTABLE;

CREATE OR REPLACE FUNCTION security_event_hash(prev TEXT, r security_event) RETURNS TEXT AS $$
    SELECT encode(sha256(convert_to(jsonb_build_array(
        prev, r.chain_seq, extract(epoch FROM r.created_at), r.event_type, r.user_id, r.user_code, r.login,
        r.ip_address, r.user_agent, r.request_id, r.path, r.details_json)::TEXT, 'UTF8')), 'hex')
$$ LANGUAGE sql IMMUTABLE;

CREATE OR REPLACE FUNCTION audit_log_chain() RETURNS TRIGGER AS $$
DECLARE
    last_seq  BIGINT;
    last_hash TEXT;
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('audit_log_chain'));
    SELECT chain_seq, current_hash INTO last_seq, last_hash
    FROM audit_log WHERE chain_seq IS NOT NULL ORDER BY chain_seq DESC LIMIT 1;
    NEW.chain_seq := coalesce(last_seq, 0) + 1;
    NEW.previous_hash := last_hash;
    NEW.current_hash := audit_log_hash(last_hash, NEW);
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION security_event_chain() RETURNS TRIGGER AS $$
DECLARE
    last_seq  BIGINT;
    last_hash TEXT;
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('security_event_chain'));
    SELECT chain_seq, current_hash INTO last_seq, last_hash
    FROM security_event WHERE chain_seq IS NOT NULL ORDER BY chain_seq DESC LIMIT 1;
    NEW.chain_seq := coalesce(last_seq, 0) + 1;
    NEW.previous_hash := last_hash;
    NEW.current_hash := security_event_hash(last_hash, NEW);
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION audit_append_only() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' AND coalesce(current_setting('bokati.audit_purge', true), '') = 'on' THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'Journal % en ajout seul : % refuse', TG_TABLE_NAME, TG_OP;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_log_chain ON audit_log;
CREATE TRIGGER trg_audit_log_chain BEFORE INSERT ON audit_log FOR EACH ROW EXECUTE FUNCTION audit_log_chain();
DROP TRIGGER IF EXISTS trg_audit_log_append_only ON audit_log;
CREATE TRIGGER trg_audit_log_append_only BEFORE UPDATE OR DELETE ON audit_log FOR EACH ROW EXECUTE FUNCTION audit_append_only();

DROP TRIGGER IF EXISTS trg_security_event_chain ON security_event;
CREATE TRIGGER trg_security_event_chain BEFORE INSERT ON security_event FOR EACH ROW EXECUTE FUNCTION security_event_chain();
DROP TRIGGER IF EXISTS trg_security_event_append_only ON security_event;
CREATE TRIGGER trg_security_event_append_only BEFORE UPDATE OR DELETE ON security_event FOR EACH ROW EXECUTE FUNCTION audit_append_only();

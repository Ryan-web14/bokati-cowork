-- ============================================================
-- V184: Contract amendment workflow + append-only audit trail
-- ============================================================

-- 1. Extend status CHECK constraint on contract_record to include AMENDED
DO $$
DECLARE
    v_constraint text;
BEGIN
    SELECT conname INTO v_constraint
    FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(c.conkey)
    WHERE t.relname = 'contract_record'
      AND a.attname = 'status'
      AND c.contype = 'c'
    LIMIT 1;

    IF v_constraint IS NOT NULL THEN
        EXECUTE 'ALTER TABLE contract_record DROP CONSTRAINT ' || quote_ident(v_constraint);
    END IF;
END $$;

ALTER TABLE contract_record
    ADD CONSTRAINT contract_record_status_check
        CHECK (status IN (
            'DRAFT', 'GENERATED', 'UNDER_REVIEW', 'AWAITING_SIGNATURE',
            'SIGNED', 'ACTIVE', 'SUSPENDED', 'AMENDED',
            'EXPIRED', 'TERMINATED', 'CANCELLED'
        ));

-- 2. Amendment (avenant) table
CREATE TABLE contract_amendment (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(120)  NOT NULL UNIQUE,
    contract_id     BIGINT        REFERENCES contract_record(id),
    original_contract_code VARCHAR(120) NOT NULL,
    status          VARCHAR(40)   NOT NULL
                        CHECK (status IN ('DRAFT','UNDER_REVIEW','PENDING_SIGNATURE','ACTIVE','REJECTED','CANCELLED')),
    description     TEXT,
    proposed_by     BIGINT,
    proposed_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    effective_date  DATE,
    draft_document_code  VARCHAR(120),
    signed_document_code VARCHAR(120),
    signed_at       TIMESTAMPTZ,
    activated_at    TIMESTAMPTZ,
    reviewed_by     BIGINT,
    review_comment  TEXT,
    rejection_reason TEXT,
    cancellation_reason TEXT,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ca_contract_code ON contract_amendment(original_contract_code);
CREATE INDEX idx_ca_status        ON contract_amendment(status);

-- 3. Contract audit event table — append-only, hash-chained
CREATE TABLE contract_audit_event (
    id              BIGSERIAL PRIMARY KEY,
    contract_code   VARCHAR(120)  NOT NULL,
    amendment_code  VARCHAR(120),
    event_type      VARCHAR(80)   NOT NULL,
    actor_id        BIGINT,
    actor_type      VARCHAR(30)   NOT NULL DEFAULT 'USER',
    actor_name      VARCHAR(200),
    occurred_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    previous_hash   VARCHAR(64),
    event_hash      VARCHAR(64)   NOT NULL,
    payload         TEXT,
    justification   TEXT
);

CREATE INDEX idx_cae_contract_code ON contract_audit_event(contract_code);
CREATE INDEX idx_cae_occurred_at   ON contract_audit_event(occurred_at);

-- Append-only enforcement: reject any UPDATE or DELETE on audit rows
CREATE OR REPLACE FUNCTION fn_contract_audit_event_immutable()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'contract_audit_event rows are immutable — modification or deletion is prohibited';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_contract_audit_event_immutable
    BEFORE UPDATE OR DELETE ON contract_audit_event
    FOR EACH ROW EXECUTE FUNCTION fn_contract_audit_event_immutable();

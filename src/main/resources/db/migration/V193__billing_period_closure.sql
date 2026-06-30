-- ============================================================
-- V189: Clôtures périodiques de facturation (append-only)
-- ============================================================
CREATE TABLE billing_period_closure (
    id                    BIGSERIAL     PRIMARY KEY,
    period_type           VARCHAR(10)   NOT NULL CHECK (period_type IN ('DAY','MONTH','YEAR')),
    period_label          VARCHAR(20)   NOT NULL,
    document_type         VARCHAR(40)   NOT NULL,
    total_documents       INTEGER       NOT NULL DEFAULT 0,
    total_invoiced        NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_paid            NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_credit_notes    NUMERIC(19,4) NOT NULL DEFAULT 0,
    cumulative_total      NUMERIC(19,4) NOT NULL DEFAULT 0,
    closure_hash          VARCHAR(64)   NOT NULL,
    previous_closure_hash VARCHAR(64),
    computed_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    computed_by           VARCHAR(120)  NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uq_closure_period UNIQUE (period_type, period_label, document_type)
);

CREATE INDEX idx_bpc_period ON billing_period_closure(period_type, period_label);

-- Append-only : interdire UPDATE et DELETE
CREATE OR REPLACE FUNCTION fn_billing_period_closure_immutable()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'billing_period_closure rows are immutable — clôtures fiscales non modifiables';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_billing_period_closure_immutable
    BEFORE UPDATE OR DELETE ON billing_period_closure
    FOR EACH ROW EXECUTE FUNCTION fn_billing_period_closure_immutable();

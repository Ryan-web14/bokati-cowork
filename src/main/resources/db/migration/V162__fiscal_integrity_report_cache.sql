CREATE TABLE fiscal_integrity_report (
    id              BIGINT       PRIMARY KEY,
    valid           BOOLEAN      NOT NULL,
    checked_invoices INT         NOT NULL DEFAULT 0,
    broken_chains   INT          NOT NULL DEFAULT 0,
    missing_signatures INT       NOT NULL DEFAULT 0,
    numbering_gaps  INT          NOT NULL DEFAULT 0,
    checked_at      TIMESTAMPTZ  NOT NULL,
    triggered_by    VARCHAR(40)  NOT NULL DEFAULT 'SCHEDULER',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fiscal_integrity_report_checked_at ON fiscal_integrity_report (checked_at DESC);

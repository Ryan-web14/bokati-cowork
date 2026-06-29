ALTER TABLE subscription_pass
    ADD COLUMN IF NOT EXISTS pass_plan_version_id  BIGINT,
    ADD COLUMN IF NOT EXISTS currency              VARCHAR(3),
    ADD COLUMN IF NOT EXISTS subtotal_amount       NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tax_amount            NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_amount          NUMERIC(19,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS auto_renew            BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS next_renewal_date     TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS renewal_count         INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cancelled_at          TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS cancellation_reason   TEXT;

ALTER TABLE subscription_pass
    ADD CONSTRAINT fk_subscription_pass_plan_version_new
        FOREIGN KEY (pass_plan_version_id) REFERENCES pass_plan_version(id);

ALTER TABLE subscription_pass
    DROP CONSTRAINT IF EXISTS ck_subscription_pass_status;

ALTER TABLE subscription_pass
    ADD CONSTRAINT ck_subscription_pass_status CHECK (status IN (
        'DRAFT', 'PENDING_ACTIVATION', 'ACTIVE',
        'PARTIALLY_USED', 'CONSUMED', 'PAST_DUE',
        'EXPIRED', 'CANCELLED', 'SUSPENDED'
    ));

CREATE INDEX IF NOT EXISTS idx_pass_plan_version_ref
    ON subscription_pass(pass_plan_version_id);
CREATE INDEX IF NOT EXISTS idx_pass_auto_renew
    ON subscription_pass(auto_renew, next_renewal_date)
    WHERE auto_renew = TRUE AND status = 'ACTIVE';

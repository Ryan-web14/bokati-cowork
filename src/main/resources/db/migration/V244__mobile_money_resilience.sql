-- Mobile money · resilience et securite
-- Un depot garde son calendrier de verification, sa raison d'echec codee et le message qu'on dit
-- au client. Chaque rappel de l'operateur est ecrit avant d'etre traite · une notification perdue
-- ne l'est plus, et une notification forgee se voit.

ALTER TABLE pawapay_deposit ADD COLUMN IF NOT EXISTS next_status_check_at TIMESTAMPTZ;
ALTER TABLE pawapay_deposit ADD COLUMN IF NOT EXISTS unresolved_at TIMESTAMPTZ;
ALTER TABLE pawapay_deposit ADD COLUMN IF NOT EXISTS failure_code VARCHAR(80);
ALTER TABLE pawapay_deposit ADD COLUMN IF NOT EXISTS user_message VARCHAR(300);
ALTER TABLE pawapay_deposit ADD COLUMN IF NOT EXISTS retryable BOOLEAN;
ALTER TABLE pawapay_deposit ADD COLUMN IF NOT EXISTS provider_transaction_id VARCHAR(120);
ALTER TABLE pawapay_deposit ADD COLUMN IF NOT EXISTS initiation_attempts INTEGER NOT NULL DEFAULT 0;

-- Les depots en cours sont verifies des maintenant par le nouveau calendrier
UPDATE pawapay_deposit SET next_status_check_at = NOW()
WHERE next_status_check_at IS NULL AND status IN ('PROCESSING', 'SUBMITTED_UNCONFIRMED', 'CREATED', 'ACCEPTED');

CREATE INDEX IF NOT EXISTS idx_pawapay_deposit_due ON pawapay_deposit(next_status_check_at) WHERE next_status_check_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_pawapay_deposit_customer ON pawapay_deposit(customer_type, customer_code, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_pawapay_deposit_status ON pawapay_deposit(status, created_at DESC);

CREATE TABLE IF NOT EXISTS pawapay_callback (
    id                  BIGINT PRIMARY KEY,
    kind                VARCHAR(20)  NOT NULL,
    reference_id        VARCHAR(80),
    reported_status     VARCHAR(40),
    raw_body            TEXT         NOT NULL,
    signature_present   BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_via        VARCHAR(30)  NOT NULL,
    outcome             VARCHAR(20)  NOT NULL,
    detail              TEXT,
    remote_address      VARCHAR(80),
    received_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMPTZ,
    CONSTRAINT ck_pawapay_callback_kind CHECK (kind IN ('DEPOSIT', 'REFUND')),
    CONSTRAINT ck_pawapay_callback_verified CHECK (verified_via IN ('SIGNATURE', 'PROVIDER_STATUS', 'NONE')),
    CONSTRAINT ck_pawapay_callback_outcome CHECK (outcome IN ('PROCESSED', 'DEFERRED', 'IGNORED', 'FAILED'))
);
CREATE INDEX IF NOT EXISTS idx_pawapay_callback_reference ON pawapay_callback(reference_id, received_at DESC);
CREATE INDEX IF NOT EXISTS idx_pawapay_callback_received ON pawapay_callback(received_at DESC);

-- Tranche 5b du centre de gestion des entreprises · code de depense propre a chaque collaborateur,
-- code a usage unique au-dela du seuil de securite, et reservations prepayees sur le portefeuille de
-- l'organisation.

-- Le code de depense du collaborateur · le code du portefeuille reste celui du gerant.
ALTER TABLE wallet_spending_authorization ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(100);
ALTER TABLE wallet_spending_authorization ADD COLUMN IF NOT EXISTS pin_set_at TIMESTAMPTZ;
ALTER TABLE wallet_spending_authorization ADD COLUMN IF NOT EXISTS pin_failed_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE wallet_spending_authorization ADD COLUMN IF NOT EXISTS pin_locked_until TIMESTAMPTZ;

-- Une reservation prepayee est retenue avant d'avoir sa facture : la depense porte alors la
-- reservation, la facture s'y ajoute au reglement.
ALTER TABLE wallet_company_spend ALTER COLUMN document_number DROP NOT NULL;
ALTER TABLE wallet_company_spend ADD COLUMN IF NOT EXISTS booking_number VARCHAR(120);
ALTER TABLE wallet_company_spend DROP CONSTRAINT IF EXISTS ck_wallet_company_spend_target;
ALTER TABLE wallet_company_spend ADD CONSTRAINT ck_wallet_company_spend_target
    CHECK (document_number IS NOT NULL OR booking_number IS NOT NULL);
CREATE INDEX IF NOT EXISTS idx_wallet_company_spend_booking
    ON wallet_company_spend (booking_number) WHERE booking_number IS NOT NULL;

-- Le code a usage unique envoye par e-mail · seule son empreinte est gardee.
ALTER TABLE wallet_company_spend ADD COLUMN IF NOT EXISTS otp_hash VARCHAR(100);
ALTER TABLE wallet_company_spend ADD COLUMN IF NOT EXISTS otp_expires_at TIMESTAMPTZ;
ALTER TABLE wallet_company_spend ADD COLUMN IF NOT EXISTS otp_attempts INTEGER NOT NULL DEFAULT 0;

-- AWAITING_OTP : code envoye, rien n'est retenu. HELD : fonds retenus pour une reservation prepayee.
ALTER TABLE wallet_company_spend DROP CONSTRAINT IF EXISTS ck_wallet_company_spend_status;
ALTER TABLE wallet_company_spend ADD CONSTRAINT ck_wallet_company_spend_status
    CHECK (status IN ('AWAITING_OTP', 'HELD', 'PENDING_APPROVAL', 'EXECUTED', 'REJECTED', 'EXPIRED', 'CANCELLED'));
CREATE INDEX IF NOT EXISTS idx_wallet_company_spend_otp
    ON wallet_company_spend (status, otp_expires_at) WHERE status = 'AWAITING_OTP';

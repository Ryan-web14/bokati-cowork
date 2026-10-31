-- Tranche 4b du centre de gestion des entreprises · passes d'organisation partages, beneficiaire sur
-- les retenues et le journal des droits, plafond de places vendu, annexe de consommation des factures.

-- Plafond de places vendu, fixe par l'administration · le gerant regle max_seats en dessous.
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS seat_ceiling INTEGER;
ALTER TABLE subscription DROP CONSTRAINT IF EXISTS ck_subscription_seat_ceiling;
ALTER TABLE subscription ADD CONSTRAINT ck_subscription_seat_ceiling CHECK (seat_ceiling IS NULL OR seat_ceiling >= 0);
-- Le nombre de places deja regle par l'administration devient le plafond vendu.
UPDATE subscription SET seat_ceiling = max_seats WHERE seat_ceiling IS NULL AND max_seats IS NOT NULL;

-- Le collaborateur qui profite du droit de son organisation · vide quand c'est le titulaire.
ALTER TABLE entitlement_ledger ADD COLUMN IF NOT EXISTS beneficiary_type VARCHAR(60);
ALTER TABLE entitlement_ledger ADD COLUMN IF NOT EXISTS beneficiary_code VARCHAR(120);
CREATE INDEX IF NOT EXISTS idx_entitlement_ledger_beneficiary
    ON entitlement_ledger (beneficiary_type, beneficiary_code) WHERE beneficiary_code IS NOT NULL;
ALTER TABLE entitlement_reservation ADD COLUMN IF NOT EXISTS beneficiary_type VARCHAR(60);
ALTER TABLE entitlement_reservation ADD COLUMN IF NOT EXISTS beneficiary_code VARCHAR(120);

-- Numero court de l'acces a un pass · l'identifiant genere depasse ce que lit JavaScript.
CREATE SEQUENCE IF NOT EXISTS pass_beneficiary_short_id_seq START WITH 1;
ALTER TABLE pass_beneficiary ADD COLUMN IF NOT EXISTS short_id BIGINT;
ALTER SEQUENCE pass_beneficiary_short_id_seq OWNED BY pass_beneficiary.short_id;
UPDATE pass_beneficiary SET short_id = nextval('pass_beneficiary_short_id_seq') WHERE short_id IS NULL;
ALTER TABLE pass_beneficiary ALTER COLUMN short_id SET DEFAULT nextval('pass_beneficiary_short_id_seq');
ALTER TABLE pass_beneficiary ALTER COLUMN short_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_pass_beneficiary_short_id ON pass_beneficiary (short_id);
-- Un seul acces ouvert par personne et par pass.
CREATE UNIQUE INDEX IF NOT EXISTS uk_pass_beneficiary_open
    ON pass_beneficiary (pass_id, lower(beneficiary_type), lower(beneficiary_code)) WHERE revoked_at IS NULL;

-- Les reservations prises sur un pass d'organisation · le titulaire du droit n'est pas le membre.
CREATE INDEX IF NOT EXISTS idx_booking_pass_number ON booking (pass_number) WHERE pass_number IS NOT NULL;

-- Annexe de consommation par collaborateur, figee a l'emission de la facture d'abonnement.
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS usage_annex_json TEXT;

-- L'annexe imprimee ne change plus une fois la facture scellee, comme le reste du document.
CREATE OR REPLACE FUNCTION fn_billing_document_check_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF (
           NEW.fiscal_number        IS DISTINCT FROM OLD.fiscal_number
        OR NEW.current_hash         IS DISTINCT FROM OLD.current_hash
        OR NEW.previous_hash        IS DISTINCT FROM OLD.previous_hash
        OR NEW.fiscal_signature     IS DISTINCT FROM OLD.fiscal_signature
        OR NEW.total_amount         IS DISTINCT FROM OLD.total_amount
        OR NEW.subtotal_amount      IS DISTINCT FROM OLD.subtotal_amount
        OR NEW.discount_amount      IS DISTINCT FROM OLD.discount_amount
        OR NEW.taxable_amount       IS DISTINCT FROM OLD.taxable_amount
        OR NEW.vat_amount           IS DISTINCT FROM OLD.vat_amount
        OR NEW.tax_amount           IS DISTINCT FROM OLD.tax_amount
        OR NEW.customer_code        IS DISTINCT FROM OLD.customer_code
        OR NEW.customer_name        IS DISTINCT FROM OLD.customer_name
        OR NEW.customer_niu         IS DISTINCT FROM OLD.customer_niu
        OR NEW.customer_identifiers_json   IS DISTINCT FROM OLD.customer_identifiers_json
        OR NEW.customer_vat_exemption_note IS DISTINCT FROM OLD.customer_vat_exemption_note
        OR NEW.company_name               IS DISTINCT FROM OLD.company_name
        OR NEW.company_identifiers_json   IS DISTINCT FROM OLD.company_identifiers_json
        OR NEW.usage_annex_json           IS DISTINCT FROM OLD.usage_annex_json
        OR NEW.document_type        IS DISTINCT FROM OLD.document_type
        OR NEW.document_number      IS DISTINCT FROM OLD.document_number
        OR NEW.issue_date           IS DISTINCT FROM OLD.issue_date
        OR NEW.currency             IS DISTINCT FROM OLD.currency
        OR NEW.exchange_rate        IS DISTINCT FROM OLD.exchange_rate
        OR NEW.billing_address_json IS DISTINCT FROM OLD.billing_address_json
        OR NEW.seller_name          IS DISTINCT FROM OLD.seller_name
        OR NEW.seller_niu           IS DISTINCT FROM OLD.seller_niu
        OR NEW.seller_address_json  IS DISTINCT FROM OLD.seller_address_json
    ) THEN
        RAISE EXCEPTION
            'billing_document % est verrouillé (locked=true) : les champs financiers sont immuables. '
            'Toute correction doit passer par un avoir (AVR) ou une facture rectificative (REC).',
            OLD.document_number;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

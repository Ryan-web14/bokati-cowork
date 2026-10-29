-- Tranche 2 du centre de gestion des entreprises · les factures d'un membre reliees a son organisation.
--
-- La facture reste au nom du membre ; elle porte en plus l'organisation pour le compte de
-- laquelle elle est emise, figee a l'emission (AT_ISSUE), ou reliee apres coup a une ancienne
-- facture (ATTACHED). Une facture reliee compte dans l'encours de l'organisation.

ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS company_customer_id       BIGINT REFERENCES customer (id),
    ADD COLUMN IF NOT EXISTS company_customer_code     VARCHAR(300),
    ADD COLUMN IF NOT EXISTS company_name              VARCHAR(255),
    ADD COLUMN IF NOT EXISTS company_identifiers_json  TEXT,
    ADD COLUMN IF NOT EXISTS company_link_source       VARCHAR(20),
    ADD COLUMN IF NOT EXISTS company_linked_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS company_linked_by         VARCHAR(150);

CREATE INDEX IF NOT EXISTS idx_billing_document_company_code ON billing_document (company_customer_code);

-- Le choix « pour l'organisation » fait a la reservation, porte jusqu'a la facture.
ALTER TABLE booking ADD COLUMN IF NOT EXISTS billing_company_code VARCHAR(300);
ALTER TABLE billable_item ADD COLUMN IF NOT EXISTS billing_company_code VARCHAR(300);

-- Ce qui est imprime pour l'organisation (nom, identifiants) ne change plus une fois la facture
-- scellee. Le lien lui-meme (code, source, date) reste modifiable : c'est ce qui permet de relier
-- une ancienne facture scellee, sans toucher au document emis.
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

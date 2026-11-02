-- Tranche 0 du centre de gestion des entreprises · une entreprise utilisable en facturation.
--
-- Le client COMPANY reste le compte commercial ; business_entity devient son profil legal,
-- relie par customer.business_entity_id. Tous les types d organisation sont pris en charge
-- (societe, etablissement, entrepreneur individuel, profession liberale, association, ONG,
-- institution publique), chacun avec ses identifiants : aucun n est obligatoire, chacun n est
-- imprime sur la facture que s il est saisi.

-- ── Profil legal ─────────────────────────────────────────────────────────────

ALTER TABLE business_entity ALTER COLUMN niu_number DROP NOT NULL;
ALTER TABLE business_entity ALTER COLUMN rccm_number DROP NOT NULL;

ALTER TABLE business_entity
    ADD COLUMN IF NOT EXISTS organization_type          VARCHAR(40),
    ADD COLUMN IF NOT EXISTS legal_representative_name  VARCHAR(200),
    ADD COLUMN IF NOT EXISTS legal_representative_title VARCHAR(120),
    ADD COLUMN IF NOT EXISTS vat_regime                 VARCHAR(20),
    ADD COLUMN IF NOT EXISTS vat_exemption_note         VARCHAR(500);

UPDATE business_entity
SET organization_type = CASE WHEN upper(legal_form) = 'ETABLISSEMENT' THEN 'ETABLISSEMENT' ELSE 'SOCIETE' END
WHERE organization_type IS NULL;

UPDATE business_entity SET vat_regime = 'SUBJECT' WHERE vat_regime IS NULL;

ALTER TABLE business_entity ALTER COLUMN organization_type SET NOT NULL;
ALTER TABLE business_entity ALTER COLUMN vat_regime SET NOT NULL;

-- Les identifiants autres que le NIU et le RCCM : recepisse, agrement, inscription a l ordre,
-- acte de creation, code administratif, ou libre. Le NIU et le RCCM y figurent aussi ; les
-- colonnes niu_number et rccm_number restent alimentees pour leurs lecteurs actuels.
CREATE TABLE IF NOT EXISTS business_entity_identifier (
    id                 BIGINT PRIMARY KEY,
    business_entity_id BIGINT       NOT NULL REFERENCES business_entity (id),
    identifier_type    VARCHAR(40)  NOT NULL,
    label              VARCHAR(120),
    identifier_value   VARCHAR(150) NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_business_entity_identifier_entity ON business_entity_identifier (business_entity_id);

-- Les numeros deja saisis rejoignent la table, dans le meme format.
INSERT INTO business_entity_identifier (id, business_entity_id, identifier_type, label, identifier_value, created_at)
SELECT (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT * 1000 + ROW_NUMBER() OVER (ORDER BY b.id),
       b.id, 'NIU', 'NIU', b.niu_number, NOW()
FROM business_entity b
WHERE b.niu_number IS NOT NULL AND trim(b.niu_number) <> ''
  AND NOT EXISTS (SELECT 1 FROM business_entity_identifier i
                  WHERE i.business_entity_id = b.id AND i.identifier_type = 'NIU');

INSERT INTO business_entity_identifier (id, business_entity_id, identifier_type, label, identifier_value, created_at)
SELECT (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT * 1000 + 500 + ROW_NUMBER() OVER (ORDER BY b.id),
       b.id, 'RCCM', 'RCCM', b.rccm_number, NOW()
FROM business_entity b
WHERE b.rccm_number IS NOT NULL AND trim(b.rccm_number) <> ''
  AND NOT EXISTS (SELECT 1 FROM business_entity_identifier i
                  WHERE i.business_entity_id = b.id AND i.identifier_type = 'RCCM');

-- ── Compte commercial ────────────────────────────────────────────────────────

ALTER TABLE customer
    ADD COLUMN IF NOT EXISTS business_entity_id BIGINT REFERENCES business_entity (id),
    ADD COLUMN IF NOT EXISTS payment_terms_days INTEGER,
    ADD COLUMN IF NOT EXISTS credit_limit       NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS billing_address_id BIGINT REFERENCES address (id),
    ADD COLUMN IF NOT EXISTS website            VARCHAR(255),
    ADD COLUMN IF NOT EXISTS blocked_reason     VARCHAR(500),
    ADD COLUMN IF NOT EXISTS blocked_by         VARCHAR(150),
    ADD COLUMN IF NOT EXISTS blocked_at         TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_customer_business_entity ON customer (business_entity_id);

-- ── Facture : ce que la fiche lui donne, fige a l emission ────────────────────

ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS customer_identifiers_json   TEXT,
    ADD COLUMN IF NOT EXISTS customer_vat_exemption_note VARCHAR(500);

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

-- Rattachement des paiements et des factures aux elements (lot R1) : une facture faite a la main
-- peut regler un abonnement, un pass, une reservation ou une domiciliation. Les factures produites
-- par l'element lui-meme restent retrouvees par billable_item.source_id ; cette table porte les liens
-- poses par l'equipe et le mode d'activation choisi pour chaque lien.

CREATE TABLE IF NOT EXISTS billing_document_element
(
    id              BIGINT PRIMARY KEY,
    document_number VARCHAR(80)  NOT NULL,
    element_type    VARCHAR(20)  NOT NULL,
    element_code    VARCHAR(80)  NOT NULL,
    activation_mode VARCHAR(20)  NOT NULL DEFAULT 'FULL_SETTLEMENT',
    linked_by       VARCHAR(150),
    linked_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    removed_by      VARCHAR(150),
    removed_at      TIMESTAMPTZ,
    CONSTRAINT ck_billing_document_element_type CHECK (element_type IN ('SUBSCRIPTION', 'PASS', 'BOOKING', 'DOMICILIATION')),
    CONSTRAINT ck_billing_document_element_mode CHECK (activation_mode IN ('FULL_SETTLEMENT', 'FIRST_PAYMENT'))
);

-- Un seul lien actif par facture et par element.
CREATE UNIQUE INDEX IF NOT EXISTS ux_billing_document_element_active
    ON billing_document_element (document_number, element_type, element_code) WHERE removed_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_billing_document_element_element ON billing_document_element (element_type, element_code);

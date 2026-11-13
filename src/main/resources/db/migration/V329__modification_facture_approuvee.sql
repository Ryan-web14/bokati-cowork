-- Lot B14 (facturation) : une facture non payee se modifie par versions, apres approbation dans le
-- centre de la facturation (lot B15). Rien n'est supprime : la demande garde les modifications
-- proposees, son motif, sa decision et son resultat (nouvelle version, ou facture rectificative si
-- la facture etait scellee ou deja comptabilisee, decision D8).

CREATE TABLE IF NOT EXISTS billing_document_change_request
(
    id                     BIGINT PRIMARY KEY,
    document_id            BIGINT         NOT NULL REFERENCES billing_document (id),
    document_number        VARCHAR(60)    NOT NULL,
    status                 VARCHAR(20)    NOT NULL,
    reason                 VARCHAR(1000)  NOT NULL,
    changes_json           TEXT           NOT NULL,
    discount_override      VARCHAR(1000),
    requested_by           VARCHAR(180)   NOT NULL,
    requested_by_name      VARCHAR(255),
    requested_at           TIMESTAMPTZ    NOT NULL,
    total_before           NUMERIC(19, 4),
    total_after            NUMERIC(19, 4),
    approval_number        VARCHAR(60),
    decided_at             TIMESTAMPTZ,
    decided_by             VARCHAR(180),
    decision_comment       VARCHAR(1000),
    result                 VARCHAR(30),
    result_document_number VARCHAR(60),
    version_number         INTEGER,
    CONSTRAINT ck_billing_change_status CHECK (status IN ('PENDING', 'APPLIED', 'REJECTED', 'RETURNED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS ix_billing_change_document ON billing_document_change_request (document_id, requested_at);
CREATE INDEX IF NOT EXISTS ix_billing_change_approval ON billing_document_change_request (approval_number);

-- Facture remplacee par une rectificative presentee comme sa nouvelle version (D8).
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS replaced_by_document_number VARCHAR(60);

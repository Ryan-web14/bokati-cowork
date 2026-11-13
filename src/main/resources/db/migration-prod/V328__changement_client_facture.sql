-- Lot B6 (facturation) : changement de client sur une facture. Avant scellement (ni paiement ni
-- ecriture) : nouvelle version au nouveau client. Sinon, en une action : avoirs au client d'origine,
-- nouvelle facture au nouveau client, et paiements gardes en avoir ou transferes (decision D6).

CREATE TABLE IF NOT EXISTS billing_customer_change
(
    id                       BIGINT PRIMARY KEY,
    original_document_number VARCHAR(60)    NOT NULL,
    new_document_number      VARCHAR(60),
    mode                     VARCHAR(20)    NOT NULL,
    unpaid_credit_note       VARCHAR(60),
    paid_credit_note         VARCHAR(60),
    from_type                VARCHAR(30),
    from_code                VARCHAR(80),
    to_type                  VARCHAR(30)    NOT NULL,
    to_code                  VARCHAR(80)    NOT NULL,
    payments_mode            VARCHAR(20),
    transferred_amount       NUMERIC(19, 4),
    reason                   VARCHAR(1000)  NOT NULL,
    created_by               VARCHAR(180)   NOT NULL,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_customer_change_mode CHECK (mode IN ('IN_PLACE', 'REISSUE')),
    CONSTRAINT ck_billing_customer_change_payments CHECK (payments_mode IS NULL OR payments_mode IN ('KEEP_AS_CREDIT', 'TRANSFER'))
);

CREATE INDEX IF NOT EXISTS ix_billing_customer_change_original ON billing_customer_change (original_document_number);

-- Transfert d'un reglement du client d'origine vers la nouvelle facture : 411 de l'un vers l'autre.
INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal,
                              entry_label, lines, no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT 3270203, 'SALES_RECEIVABLE_TRANSFER', 1, 'RECEIVABLE_TRANSFER', 'Reglement transfere a un autre client',
       'D client d''origine (lettre avec son avoir) ; C nouveau client (lettre avec sa facture).', 'SALES', 10, NULL, '{journalCode}',
       'Reglement de {fromCustomer} transfere sur {documentNumber}',
       '[{"side":"DEBIT","account":"{clientAccount}","amount":"amount","thirdParty":"{fromCustomer}","reference":"{creditNoteNumber}"},{"side":"CREDIT","account":"{clientAccount}","amount":"amount","thirdParty":"{toCustomer}","reference":"{documentNumber}"}]',
       FALSE, '{"documentNumber":"FAC-2","creditNoteNumber":"AV-1","fromCustomer":"CUS-A","toCustomer":"CUS-B","journalCode":"VT","clientAccount":"411","amount":50000}',
       DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM acc_posting_rule r WHERE r.code = 'SALES_RECEIVABLE_TRANSFER');

-- Rattachement des paiements, lot R4 : annulation d'une affectation. L'affectation est retiree de
-- payment_allocation (montants toujours positifs, une seule ligne par paiement et facture) et sa
-- trace est gardee ici, avec le sort choisi pour la facture et les elements remis en attente.
-- Quand l'affectation etait deja comptabilisee, la somme revient en attente : D 411 / C 4719.

CREATE TABLE IF NOT EXISTS payment_allocation_reversal
(
    id                  BIGINT PRIMARY KEY,
    allocation_id       BIGINT         NOT NULL,
    transaction_number  VARCHAR(80)    NOT NULL,
    document_number     VARCHAR(80)    NOT NULL,
    amount              NUMERIC(19, 4) NOT NULL,
    allocation_mode     VARCHAR(20)    NOT NULL,
    allocated_at        TIMESTAMPTZ,
    allocated_by        VARCHAR(180),
    reversed_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    reversed_by         VARCHAR(180),
    reason              VARCHAR(500)   NOT NULL,
    invoice_outcome     VARCHAR(20)    NOT NULL,
    elements_json       TEXT,
    accounting_required BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_payment_allocation_reversal_outcome CHECK (invoice_outcome IN ('REOPEN', 'NEUTRALIZE'))
);
CREATE INDEX IF NOT EXISTS ix_payment_allocation_reversal_transaction ON payment_allocation_reversal (transaction_number);
CREATE INDEX IF NOT EXISTS ix_payment_allocation_reversal_document ON payment_allocation_reversal (document_number);

INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal,
                              entry_label, lines, no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT 3580001, 'SALES_ALLOCATION_REVERSAL', 1, 'ALLOCATION_REVERSAL', 'Annulation de l''affectation d''un encaissement',
       'D client (la facture redevient due) ; C compte d''attente des encaissements non imputes.', 'SALES', 10, NULL, '{journalCode}',
       'Annulation de l''affectation de {transactionNumber} a {documentNumber}',
       '[{"side":"DEBIT","account":"{clientAccount}","amount":"amount","thirdParty":"{customerCode}","reference":"{documentNumber}"},{"side":"CREDIT","account":"{unallocatedAccount}","amount":"amount","reference":"{transactionNumber}"}]',
       FALSE, '{"transactionNumber":"TXN-1","documentNumber":"FAC-1","customerCode":"CUS-1","journalCode":"VT","clientAccount":"411","unallocatedAccount":"4719","amount":590000}',
       DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM acc_posting_rule r WHERE r.code = 'SALES_ALLOCATION_REVERSAL');

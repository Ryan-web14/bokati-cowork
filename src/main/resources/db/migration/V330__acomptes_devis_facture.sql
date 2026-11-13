-- Lot B3 (facturation) : acomptes du devis a la facture. Un devis ou une facture en preparation
-- porte un plan d'acomptes (montant ou pourcentage, du total ou de lignes choisies, avec echeances) ;
-- chaque acompte donne une facture d'acompte numerotee, encaissable par tous les moyens ; la facture
-- finale deduit chaque acompte encaisse et ne reclame que le reste.
-- Comptabilite : la facture d'acompte credite le 4191 (clients, avances et acomptes recus) ; la
-- deduction sur la facture finale solde le 4191 contre le 411 (TVA rattachee a la facture finale,
-- liberee au prorata en regime des encaissements, decision D3).

CREATE TABLE IF NOT EXISTS billing_deposit
(
    id                     BIGINT PRIMARY KEY,
    document_id            BIGINT         NOT NULL REFERENCES billing_document (id),
    document_number        VARCHAR(60)    NOT NULL,
    final_document_number  VARCHAR(60),
    rank                   INTEGER        NOT NULL,
    label                  VARCHAR(255),
    mode                   VARCHAR(20)    NOT NULL,
    value                  NUMERIC(19, 4) NOT NULL,
    base_line_orders       VARCHAR(500),
    amount                 NUMERIC(19, 4) NOT NULL,
    due_date               DATE,
    status                 VARCHAR(20)    NOT NULL,
    deposit_invoice_number VARCHAR(60),
    paid_amount            NUMERIC(19, 4) NOT NULL DEFAULT 0,
    deducted_amount        NUMERIC(19, 4),
    deducted_on            VARCHAR(60),
    deducted_at            TIMESTAMPTZ,
    settlement_document    VARCHAR(60),
    cancel_reason          VARCHAR(1000),
    created_by             VARCHAR(180),
    created_at             TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_billing_deposit_mode CHECK (mode IN ('AMOUNT', 'PERCENTAGE')),
    CONSTRAINT ck_billing_deposit_status CHECK (status IN ('PLANNED', 'INVOICED', 'PARTIALLY_PAID', 'PAID', 'DEDUCTED', 'CANCELLED',
                                                         'REFUNDED', 'CREDITED'))
);

CREATE INDEX IF NOT EXISTS ix_billing_deposit_document ON billing_deposit (document_number);
CREATE INDEX IF NOT EXISTS ix_billing_deposit_final ON billing_deposit (final_document_number);
CREATE INDEX IF NOT EXISTS ix_billing_deposit_invoice ON billing_deposit (deposit_invoice_number);

ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS deposit_for_document_number VARCHAR(60);

-- Schemas d'imputation : facture d'acompte au 4191 (avant le schema general des factures), puis
-- deduction sur la facture finale (4191 contre 411, TVA en attente liberee au prorata).
INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal,
                              entry_label, lines, no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT * FROM (VALUES
(3270201, 'SALES_DEPOSIT_INVOICE', 1, 'INVOICE_ISSUED', 'Facture d''acompte emise',
 'D client TTC ; C clients, avances et acomptes recus (4191). La TVA est portee par la facture finale.', 'SALES', 5,
 '{"depositFor":"*"}', '{journalCode}', 'Facture d''acompte {documentNumber} {customerName}',
 '[{"side":"DEBIT","account":"{clientAccount}","amount":"totalAmount","thirdParty":"{customerCode}","reference":"{documentNumber}","dueDate":"dueDate"},{"side":"CREDIT","account":"{depositAccount}","amount":"totalAmount","thirdParty":"{customerCode}","reference":"{documentNumber}","label":"Acompte sur {depositFor}"}]',
 FALSE, '{"documentNumber":"FAC-2","customerName":"Exemple","customerCode":"CUS-1","journalCode":"VT","clientAccount":"411","depositAccount":"4191","depositFor":"DEV-1","totalAmount":300000,"dueDate":"2026-02-01"}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(3270202, 'SALES_DEPOSIT_APPLIED', 1, 'DEPOSIT_APPLIED', 'Acompte deduit de la facture finale',
 'D 4191 (lettre avec la facture d''acompte) ; C client (lettre avec la facture finale) ; TVA en attente devenue exigible au prorata (regime des encaissements).', 'SALES', 10,
 NULL, '{journalCode}', 'Acompte {depositInvoiceNumber} deduit de {documentNumber}',
 '[{"side":"DEBIT","account":"{depositAccount}","amount":"amount","thirdParty":"{customerCode}","reference":"{depositInvoiceNumber}"},{"side":"CREDIT","account":"{clientAccount}","amount":"amount","thirdParty":"{customerCode}","reference":"{documentNumber}"},{"side":"DEBIT","account":"{vatPendingAccount}","amount":"vat","foreach":"vatTransfers","label":"TVA devenue exigible","reference":"{documentNumber}"},{"side":"CREDIT","account":"{account}","amount":"vat","foreach":"vatTransfers","label":"TVA exigible","reference":"{documentNumber}","taxCode":"{?taxCode}","taxBase":"base"}]',
 FALSE, '{"documentNumber":"FAC-3","depositInvoiceNumber":"FAC-2","customerCode":"CUS-1","journalCode":"VT","clientAccount":"411","depositAccount":"4191","vatPendingAccount":"4438","amount":300000,"vatTransfers":[]}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM')
) AS v(id, code, version, event_type, label, description, family, priority, conditions, journal, entry_label, lines, no_entry, sample_payload,
       effective_from, status, activated_by, activated_at, created_by)
WHERE NOT EXISTS (SELECT 1 FROM acc_posting_rule r WHERE r.code = v.code);

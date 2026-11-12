-- Lot F4 du plan finance : ventes, encaissements, remboursements, portefeuille.
--
-- Les faits (factures, avoirs, transactions, ecritures de portefeuille) ne sont pas modifies : le
-- registre des evenements comptables (acc_posting_event) dit ce qui est deja comptabilise.
--
--   * comptes de tresorerie des moyens de paiement et leurs journaux ;
--   * journal ENC des encaissements hors tresorerie (cheques, cartes, avoirs imputes) ;
--   * schemas initiaux (catalogue, sections 1, 3, 4 et 5).

WITH seed (number, label, parent_number, nature, normal_side, reconcilable, treasury_type) AS (
    VALUES ('5211', 'Banque principale', '521', 'BALANCE', 'DEBIT', FALSE, 'BANK'),
           ('5212', 'Mobile money PawaPay', '521', 'BALANCE', 'DEBIT', FALSE, 'MOBILE_MONEY'),
           ('5213', 'Mobile money numeros marchands', '521', 'BALANCE', 'DEBIT', FALSE, 'MOBILE_MONEY'),
           ('5851', 'Paiements par carte a recevoir', '585', 'BALANCE', 'DEBIT', TRUE, NULL)
), numbered AS (
    SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.number) AS rank
    FROM seed s WHERE NOT EXISTS (SELECT 1 FROM acc_account a WHERE a.number = s.number)
)
INSERT INTO acc_account (id, number, label, parent_number, class_code, nature, normal_side, reconcilable, third_party_type,
                         treasury_type, analytic_required, statement_line_code, status, created_by)
SELECT 2964000 + rank, number, label, parent_number, '5', nature, normal_side, reconcilable, NULL, treasury_type, FALSE,
       'CLASSE_5', 'ACTIVE', 'SYSTEM'
FROM numbered;

WITH seed (code, label, type, treasury, auto_validate) AS (
    VALUES ('BQ-5211', 'Banque principale', 'BANK', '5211', TRUE),
           ('MM-5212', 'Mobile money PawaPay', 'MOBILE_MONEY', '5212', TRUE),
           ('MM-5213', 'Mobile money numeros marchands', 'MOBILE_MONEY', '5213', TRUE),
           ('ENC', 'Encaissements divers (cheques, cartes, avoirs)', 'MISC', NULL, TRUE)
), numbered AS (
    SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.code) AS rank
    FROM seed s WHERE NOT EXISTS (SELECT 1 FROM acc_journal j WHERE j.code = s.code)
)
INSERT INTO acc_journal (id, code, label, type, treasury_account_number, auto_validate, created_by)
SELECT 2964100 + rank, code, label, type, treasury, auto_validate, 'SYSTEM' FROM numbered;

CREATE INDEX IF NOT EXISTS idx_acc_posting_event_reference ON acc_posting_event (reference);

INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal,
                              entry_label, lines, no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
VALUES
(2964201, 'SALES_INVOICE', 1, 'INVOICE_ISSUED', 'Facture emise',
 'D client TTC ; D remise ; C produit HT par ligne ; C TVA ; C centime additionnel.', 'SALES', 10, NULL, '{journalCode}',
 'Facture {documentNumber} {customerName}',
 '[{"side":"DEBIT","account":"{clientAccount}","amount":"totalAmount","thirdParty":"{customerCode}","reference":"{documentNumber}","dueDate":"dueDate"},{"side":"DEBIT","account":"{discountAccount}","amount":"reduction","signed":true,"label":"Remises"},{"side":"CREDIT","account":"{account}","amount":"amount","foreach":"lines","label":"{label}"},{"side":"CREDIT","account":"{vatAccount}","amount":"vatAmount","label":"TVA"},{"side":"CREDIT","account":"{extraTaxAccount}","amount":"extraTaxAmount","label":"Centime additionnel"}]',
 FALSE, '{"documentNumber":"FAC-1","customerName":"Exemple","customerCode":"CUS-1","journalCode":"VT","clientAccount":"411","discountAccount":"709","vatAccount":"4431","extraTaxAccount":"4431","totalAmount":12095,"reduction":0,"vatAmount":1800,"extraTaxAmount":295,"dueDate":"2026-02-01","lines":[{"account":"7061","amount":10000,"label":"Bureau"}]}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2964202, 'SALES_CREDIT_NOTE', 1, 'CREDIT_NOTE_ISSUED', 'Avoir emis',
 'Inverse de la facture : D produit, D TVA ; C client (lettre avec la facture d''origine).', 'SALES', 10, NULL, '{journalCode}',
 'Avoir {documentNumber} {customerName}',
 '[{"side":"DEBIT","account":"{account}","amount":"amount","foreach":"lines","label":"{label}"},{"side":"DEBIT","account":"{vatAccount}","amount":"vatAmount","label":"TVA"},{"side":"DEBIT","account":"{extraTaxAccount}","amount":"extraTaxAmount","label":"Centime additionnel"},{"side":"CREDIT","account":"{discountAccount}","amount":"reduction","signed":true,"label":"Remises"},{"side":"CREDIT","account":"{clientAccount}","amount":"totalAmount","thirdParty":"{customerCode}","reference":"{letteringReference}"}]',
 FALSE, '{"documentNumber":"AV-1","customerName":"Exemple","customerCode":"CUS-1","journalCode":"VT","clientAccount":"411","discountAccount":"709","vatAccount":"4431","extraTaxAccount":"4431","totalAmount":1180,"reduction":0,"vatAmount":180,"extraTaxAmount":0,"letteringReference":"FAC-1","lines":[{"account":"7061","amount":1000,"label":"Bureau"}]}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2964203, 'SALES_PAYMENT', 1, 'PAYMENT_RECEIVED', 'Encaissement',
 'D tresorerie du moyen ; C client par facture imputee (lettrage par numero de facture) ; C 4719 pour le reste (rechargement, trop-percu, a affecter).', 'PAYMENT', 10, NULL, '{journalCode}',
 'Encaissement {transactionNumber} ({method})',
 '[{"side":"DEBIT","account":"{treasuryAccount}","amount":"amount","thirdParty":"{?treasuryThirdParty}","reference":"{transactionNumber}"},{"side":"CREDIT","account":"{clientAccount}","amount":"amount","foreach":"allocations","thirdParty":"{customerCode}","reference":"{documentNumber}"},{"side":"CREDIT","account":"{unallocatedAccount}","amount":"unallocated","reference":"{transactionNumber}"}]',
 FALSE, '{"transactionNumber":"TXN-1","method":"CASH","journalCode":"OD","treasuryAccount":"571","clientAccount":"411","unallocatedAccount":"4719","amount":12095,"unallocated":0,"allocations":[{"documentNumber":"FAC-1","customerCode":"CUS-1","amount":12095}]}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2964204, 'SALES_REFUND', 1, 'PAYMENT_REFUNDED', 'Remboursement',
 'D client par facture de la transaction d''origine ; D 4719 pour le reste ; C tresorerie du moyen.', 'PAYMENT', 10, NULL, '{journalCode}',
 'Remboursement {transactionNumber} ({method})',
 '[{"side":"DEBIT","account":"{clientAccount}","amount":"amount","foreach":"allocations","thirdParty":"{customerCode}","reference":"{documentNumber}"},{"side":"DEBIT","account":"{unallocatedAccount}","amount":"unallocated","reference":"{transactionNumber}"},{"side":"CREDIT","account":"{treasuryAccount}","amount":"amount","thirdParty":"{?treasuryThirdParty}","reference":"{transactionNumber}"}]',
 FALSE, '{"transactionNumber":"TXN-2","method":"CASH","journalCode":"OD","treasuryAccount":"571","clientAccount":"411","unallocatedAccount":"4719","amount":1180,"unallocated":0,"allocations":[{"documentNumber":"FAC-1","customerCode":"CUS-1","amount":1180}]}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2964205, 'WALLET_ENTRY', 1, 'WALLET_ENTRY', 'Mouvement de portefeuille',
 'Le 4191 du client suit son portefeuille : credit du portefeuille C 4191, debit D 4191 ; contrepartie selon le type (4719 liaison, 411, 471, charge, produit).', 'WALLET', 10, NULL, '{journalCode}',
 'Portefeuille {walletNumber} : {entryType} {entryNumber}',
 '[{"side":"DEBIT","account":"{counterAccount}","amount":"signedAmount","signed":true,"thirdParty":"{?counterThirdParty}","reference":"{?counterReference}"},{"side":"CREDIT","account":"4191","amount":"signedAmount","signed":true,"thirdParty":"{ownerCode}","reference":"{entryNumber}"}]',
 FALSE, '{"entryNumber":"WLE-1","walletNumber":"WAL-1","entryType":"TOPUP","journalCode":"PF","counterAccount":"4719","ownerCode":"CUS-1","signedAmount":5000,"counterReference":"TXN-3"}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM')
ON CONFLICT (code, version) DO NOTHING;

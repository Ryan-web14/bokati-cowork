-- Lot K0 de la revue comptable (23/11/2026) : ecritures automatiques justes.
-- A2 : lignes de vente sur un compte de tiers (caution 165, debours) avec le client.
-- A3 : debours sur 4711, et non plus sur 4718 (ecarts d'equilibrage a regulariser).
-- A4 : avoir sur facture d'acompte au 4191 ; annulation du seul reste non regle d'une facture d'acompte.
-- A5 : avoir impute sur une autre facture, compensation 411 contre 411.
-- A6 : TVA liberee (ou remise en attente) par l'affectation tardive, son annulation et le transfert de creance.
-- A7 : une ligne de TVA par taux.
-- A11 : decisions sur ecart de caisse validees aussitot (journal OD).
-- A12 : TVA deductible de la caisse et des justifications avec son code et sa base.

-- ------------------------------------------------------------------ facturation (A5)
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS applied_to_document_number VARCHAR(60);
ALTER TABLE billing_document ADD COLUMN IF NOT EXISTS applied_at TIMESTAMPTZ;

-- Avoirs deja imputes : la facture cible est dans le journal fiscal (applied_to=...|amount=...).
UPDATE billing_document d
SET applied_to_document_number = substring(f.new_value from 'applied_to=([^|]+)'),
    applied_at = f.created_at
FROM fiscal_audit_log f
WHERE f.action = 'CREDIT_NOTE_APPLIED' AND f.entity_id = d.document_number AND d.document_type = 'CREDIT_NOTE'
  AND d.applied_to_document_number IS NULL AND f.new_value LIKE 'applied_to=%';

-- ------------------------------------------------------------------ plan de comptes (A3)
INSERT INTO acc_account (id, number, label, parent_number, class_code, nature, normal_side, reconcilable, third_party_type,
                         treasury_type, analytic_required, statement_line_code, status, created_by)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM acc_account), '4711', 'Débours pour le compte des clients', '471', '4', 'BALANCE', 'DEBIT',
       TRUE, 'CUSTOMER', NULL, FALSE, p.statement_line_code, 'ACTIVE', 'SYSTEM'
FROM acc_account p
WHERE p.number = '471' AND NOT EXISTS (SELECT 1 FROM acc_account WHERE number = '4711');

-- ------------------------------------------------------------------ journal OD (A11)
UPDATE acc_journal SET auto_validate = TRUE WHERE code = 'OD' AND auto_validate = FALSE;

-- ------------------------------------------------------------------ nouvelles versions de schemas
CREATE TEMPORARY TABLE k0_rule (code VARCHAR(80) PRIMARY KEY, lines TEXT NOT NULL) ON COMMIT DROP;

INSERT INTO k0_rule (code, lines)
SELECT r.code,
       replace(replace(replace(r.lines,
           '{"side":"CREDIT","account":"{account}","amount":"amount","foreach":"lines","label":"{label}"}',
           '{"side":"CREDIT","account":"{account}","amount":"amount","foreach":"lines","label":"{label}","thirdParty":"{?thirdParty}"}'),
           '{"side":"CREDIT","account":"{vatAccount}","amount":"vatAmount","label":"TVA","reference":"{documentNumber}","taxCode":"{?vatCode}","taxBase":"vatBase"}',
           '{"side":"CREDIT","account":"{account}","amount":"amount","foreach":"vatLines","label":"TVA {?rateLabel}","reference":"{documentNumber}","taxCode":"{?taxCode}","taxBase":"base"}'),
           '{"side":"DEBIT","account":"{vatAccount}","amount":"vatAmount","label":"TVA","reference":"{vatReference}","taxCode":"{?vatCode}","taxBase":"vatBase"}',
           '{"side":"DEBIT","account":"{account}","amount":"amount","foreach":"vatLines","label":"TVA {?rateLabel}","reference":"{vatReference}","taxCode":"{?taxCode}","taxBase":"base"}')
FROM acc_posting_rule r
WHERE r.code = 'SALES_INVOICE' AND r.status = 'ACTIVE'
  AND r.lines LIKE '%"foreach":"lines","label":"{label}"}%' AND r.lines LIKE '%"amount":"vatAmount","label":"TVA","reference":"{documentNumber}"%';

INSERT INTO k0_rule (code, lines)
SELECT r.code,
       replace(replace(r.lines,
           '{"side":"DEBIT","account":"{account}","amount":"amount","foreach":"lines","label":"{label}"}',
           '{"side":"DEBIT","account":"{account}","amount":"amount","foreach":"lines","label":"{label}","thirdParty":"{?thirdParty}"}'),
           '{"side":"DEBIT","account":"{vatAccount}","amount":"vatAmount","label":"TVA","reference":"{vatReference}","taxCode":"{?vatCode}","taxBase":"vatBase"}',
           '{"side":"DEBIT","account":"{account}","amount":"amount","foreach":"vatLines","label":"TVA {?rateLabel}","reference":"{vatReference}","taxCode":"{?taxCode}","taxBase":"base"}')
FROM acc_posting_rule r
WHERE r.code = 'SALES_CREDIT_NOTE' AND r.status = 'ACTIVE'
  AND r.lines LIKE '%"foreach":"lines","label":"{label}"}%' AND r.lines LIKE '%"amount":"vatAmount","label":"TVA","reference":"{vatReference}"%';

-- A6 : TVA liberee par l'affectation tardive et le transfert de creance, remise en attente par l'annulation d'affectation.
INSERT INTO k0_rule (code, lines)
SELECT r.code, left(r.lines, length(r.lines) - 1) || CASE WHEN r.code = 'SALES_ALLOCATION_REVERSAL' THEN
           ',{"side":"DEBIT","account":"{account}","amount":"vat","foreach":"vatTransfers","label":"TVA exigible remise en attente","reference":"{documentNumber}","taxCode":"{?taxCode}","taxBase":"base"},{"side":"CREDIT","account":"{vatPendingAccount}","amount":"vat","foreach":"vatTransfers","label":"TVA remise en attente","reference":"{documentNumber}"}]'
       ELSE
           ',{"side":"DEBIT","account":"{vatPendingAccount}","amount":"vat","foreach":"vatTransfers","label":"TVA devenue exigible","reference":"{documentNumber}"},{"side":"CREDIT","account":"{account}","amount":"vat","foreach":"vatTransfers","label":"TVA exigible","reference":"{documentNumber}","taxCode":"{?taxCode}","taxBase":"base"}]'
       END
FROM acc_posting_rule r
WHERE r.code IN ('SALES_LATE_ALLOCATION', 'SALES_ALLOCATION_REVERSAL', 'SALES_RECEIVABLE_TRANSFER') AND r.status = 'ACTIVE'
  AND r.lines NOT LIKE '%vatTransfers%';

-- A2 : le D 165 d'un mouvement de caution porte le client.
INSERT INTO k0_rule (code, lines)
SELECT r.code, replace(r.lines,
           '{"side":"DEBIT","account":"{securityDepositAccount}","amount":"amount","reference":"{depositNumber}"}',
           '{"side":"DEBIT","account":"{securityDepositAccount}","amount":"amount","thirdParty":"{?depositThirdParty}","reference":"{depositNumber}"}')
FROM acc_posting_rule r
WHERE r.code = 'SALES_SECURITY_DEPOSIT_MOVEMENT' AND r.status = 'ACTIVE'
  AND r.lines LIKE '%"account":"{securityDepositAccount}","amount":"amount","reference":"{depositNumber}"}%';

-- A12 : TVA de la caisse et des justifications avec son code et sa base.
INSERT INTO k0_rule (code, lines)
SELECT r.code, replace(replace(replace(r.lines,
           '{"side":"DEBIT","account":"{vatAccount}","amount":"vatAmount"}',
           '{"side":"DEBIT","account":"{vatAccount}","amount":"vatAmount","taxCode":"{?vatCode}","taxBase":"netAmount"}'),
           '{"side":"CREDIT","account":"{vatAccount}","amount":"vatAmount"}',
           '{"side":"CREDIT","account":"{vatAccount}","amount":"vatAmount","taxCode":"{?vatCode}","taxBase":"netAmount"}'),
           '{"side":"DEBIT","account":"{vatAccount}","amount":"vatAmount","label":"TVA"}',
           '{"side":"DEBIT","account":"{vatAccount}","amount":"vatAmount","label":"TVA","taxCode":"{?vatCode}","taxBase":"netAmount"}')
FROM acc_posting_rule r
WHERE r.code IN ('CASH_OUT', 'CASH_IN', 'EXPENSE_JUSTIFIED') AND r.status = 'ACTIVE'
  AND r.lines LIKE '%"account":"{vatAccount}","amount":"vatAmount"%' AND r.lines NOT LIKE '%"taxBase":"netAmount"%';

WITH numbered AS (
    SELECT r.*, k.lines AS new_lines, ROW_NUMBER() OVER (ORDER BY r.code) AS rank
    FROM acc_posting_rule r JOIN k0_rule k ON k.code = r.code
    WHERE r.status = 'ACTIVE' AND k.lines <> r.lines
      AND NOT EXISTS (SELECT 1 FROM acc_posting_rule o WHERE o.code = r.code AND o.version = r.version + 1)
)
INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal, entry_label, lines,
                              no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT (SELECT COALESCE(MAX(id), 0) FROM acc_posting_rule) + rank, code, version + 1, event_type, label, description, family, priority,
       conditions, journal, entry_label, new_lines, no_entry, sample_payload, effective_from, 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'
FROM numbered;

UPDATE acc_posting_rule r SET status = 'RETIRED'
WHERE r.code IN (SELECT code FROM k0_rule) AND r.status = 'ACTIVE'
  AND EXISTS (SELECT 1 FROM acc_posting_rule o WHERE o.code = r.code AND o.version > r.version AND o.status = 'ACTIVE');

-- ------------------------------------------------------------------ nouveaux schemas (A4, A5)
WITH v (code, event_type, label, description, priority, conditions, entry_label, lines, sample_payload) AS (VALUES
 ('SALES_DEPOSIT_CREDIT_NOTE', 'CREDIT_NOTE_ISSUED', 'Avoir sur facture d''acompte',
  'D clients, avances et acomptes recus (4191) ; C client. L''avoir d''une facture d''acompte ne touche pas les produits.', 5,
  '{"depositFor":"*"}', 'Avoir {documentNumber} sur acompte {letteringReference} {customerName}',
  '[{"side":"DEBIT","account":"{depositAccount}","amount":"totalAmount","thirdParty":"{customerCode}","reference":"{letteringReference}","label":"Acompte annule"},{"side":"CREDIT","account":"{clientAccount}","amount":"totalAmount","thirdParty":"{customerCode}","reference":"{letteringReference}"}]',
  '{"documentNumber":"AV-1","letteringReference":"FAC-2","customerName":"Exemple","customerCode":"CUS-1","journalCode":"VT","clientAccount":"411","depositAccount":"4191","depositFor":"DEV-1","totalAmount":100000}'),
 ('SALES_DEPOSIT_REMAINDER_CANCELLED', 'INVOICE_CANCELLED', 'Reste d''une facture d''acompte annule',
  'Facture d''acompte en partie reglee, reste repris par la facture finale : D 4191 / C client pour le seul reste non regle.', 5,
  '{"depositFor":"*"}', 'Reste de l''acompte {documentNumber} annule',
  '[{"side":"DEBIT","account":"{depositAccount}","amount":"amount","thirdParty":"{customerCode}","reference":"{documentNumber}","label":"Reste d''acompte annule"},{"side":"CREDIT","account":"{clientAccount}","amount":"amount","thirdParty":"{customerCode}","reference":"{documentNumber}"}]',
  '{"documentNumber":"FAC-2","customerCode":"CUS-1","journalCode":"VT","clientAccount":"411","depositAccount":"4191","depositFor":"DEV-1","amount":40000}'),
 ('SALES_CREDIT_NOTE_APPLIED', 'CREDIT_NOTE_APPLIED', 'Avoir impute sur une autre facture',
  'D client titulaire de l''avoir (lettre avec l''avoir) ; C client de la facture (lettre avec la facture) ; TVA en attente liberee au prorata (regime des encaissements).', 10,
  NULL, 'Avoir {creditNoteNumber} impute sur {documentNumber}',
  '[{"side":"DEBIT","account":"{clientAccount}","amount":"amount","thirdParty":"{holderCode}","reference":"{creditNoteReference}","label":"Avoir {creditNoteNumber} impute sur {documentNumber}"},{"side":"CREDIT","account":"{clientAccount}","amount":"amount","thirdParty":"{customerCode}","reference":"{documentNumber}","label":"Reglement par l''avoir {creditNoteNumber}"},{"side":"DEBIT","account":"{vatPendingAccount}","amount":"vat","foreach":"vatTransfers","label":"TVA devenue exigible","reference":"{documentNumber}"},{"side":"CREDIT","account":"{account}","amount":"vat","foreach":"vatTransfers","label":"TVA exigible","reference":"{documentNumber}","taxCode":"{?taxCode}","taxBase":"base"}]',
  '{"creditNoteNumber":"AV-1","creditNoteReference":"FAC-1","documentNumber":"FAC-9","holderCode":"MBR-1","customerCode":"CUS-COM-7","journalCode":"VT","clientAccount":"411","vatPendingAccount":"4438","amount":50000,"vatTransfers":[]}')
), numbered AS (
    SELECT v.*, ROW_NUMBER() OVER (ORDER BY v.code) AS rank FROM v WHERE NOT EXISTS (SELECT 1 FROM acc_posting_rule r WHERE r.code = v.code)
)
INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal, entry_label, lines,
                              no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT (SELECT COALESCE(MAX(id), 0) FROM acc_posting_rule) + rank, code, 1, event_type, label, description, 'SALES', priority, conditions,
       '{journalCode}', entry_label, lines, FALSE, sample_payload, DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'
FROM numbered;

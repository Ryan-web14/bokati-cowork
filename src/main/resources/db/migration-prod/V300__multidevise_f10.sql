-- Gestion multidevise (complement du lot F10, a la demande de l'utilisateur) : taux saisis a la
-- main ou recuperes chaque jour sur internet ; conversion des factures, paiements et portefeuilles
-- en devise ; ecarts de change au reglement.

ALTER TABLE cash_exchange_rate ADD COLUMN IF NOT EXISTS source VARCHAR(10) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE cash_exchange_rate ADD COLUMN IF NOT EXISTS provider VARCHAR(80);
ALTER TABLE cash_exchange_rate DROP CONSTRAINT IF EXISTS ck_cash_exchange_rate_source;
ALTER TABLE cash_exchange_rate ADD CONSTRAINT ck_cash_exchange_rate_source CHECK (source IN ('MANUAL', 'AUTO', 'PARITY'));

-- Schemas de vente et d'encaissement : la ligne du client porte la devise et le montant d'origine
-- (le lettrage solde l'ecart de change quand les montants en devise s'equilibrent).
INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal, entry_label, lines,
                              no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT r.id + 30000, r.code, r.version + 1, r.event_type, r.label, r.description, r.family, r.priority, r.conditions, r.journal, r.entry_label,
       replace(replace(r.lines,
           '"thirdParty":"{customerCode}","reference":"{documentNumber}","dueDate":"dueDate"}',
           '"thirdParty":"{customerCode}","reference":"{documentNumber}","dueDate":"dueDate","currency":"{?currency}","foreignAmount":"foreignTotal"}'),
           '"thirdParty":"{customerCode}","reference":"{letteringReference}"}',
           '"thirdParty":"{customerCode}","reference":"{letteringReference}","currency":"{?currency}","foreignAmount":"foreignTotal"}'),
       r.no_entry, r.sample_payload, r.effective_from, 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'
FROM acc_posting_rule r
WHERE r.code IN ('SALES_INVOICE', 'SALES_CREDIT_NOTE') AND r.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM acc_posting_rule o WHERE o.code = r.code AND o.version = r.version + 1);

INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal, entry_label, lines,
                              no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT r.id + 30000, r.code, r.version + 1, r.event_type, r.label, r.description, r.family, r.priority, r.conditions, r.journal, r.entry_label,
       replace(r.lines, '"foreach":"allocations","thirdParty":"{customerCode}","reference":"{documentNumber}"}',
           '"foreach":"allocations","thirdParty":"{customerCode}","reference":"{documentNumber}","currency":"{?event.currency}","foreignAmount":"foreignAmount"}'),
       r.no_entry, r.sample_payload, r.effective_from, 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'
FROM acc_posting_rule r
WHERE r.code IN ('SALES_PAYMENT', 'SALES_REFUND') AND r.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM acc_posting_rule o WHERE o.code = r.code AND o.version = r.version + 1);

UPDATE acc_posting_rule r SET status = 'RETIRED'
WHERE r.code IN ('SALES_INVOICE', 'SALES_CREDIT_NOTE', 'SALES_PAYMENT', 'SALES_REFUND') AND r.status = 'ACTIVE'
  AND EXISTS (SELECT 1 FROM acc_posting_rule o WHERE o.code = r.code AND o.version > r.version AND o.status = 'ACTIVE');

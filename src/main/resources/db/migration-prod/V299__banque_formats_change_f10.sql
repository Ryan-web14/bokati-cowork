-- Complements du lot F10 : formats OFX et MT940, ecarts de change sur especes en devise, etat de
-- rapprochement fige.

ALTER TABLE bank_import_template DROP CONSTRAINT IF EXISTS ck_bank_template_format;
ALTER TABLE bank_import_template ADD CONSTRAINT ck_bank_template_format CHECK (format IN ('CSV', 'OFX', 'MT940'));
ALTER TABLE bank_import_template DROP CONSTRAINT IF EXISTS ck_bank_template_amount;
ALTER TABLE bank_import_template ADD CONSTRAINT ck_bank_template_amount
    CHECK (format <> 'CSV' OR amount_column IS NOT NULL OR (debit_column IS NOT NULL AND credit_column IS NOT NULL));
ALTER TABLE bank_import_template ALTER COLUMN date_column DROP NOT NULL;
ALTER TABLE bank_import_template ALTER COLUMN label_column DROP NOT NULL;

ALTER TABLE bank_statement DROP CONSTRAINT IF EXISTS ck_bank_statement_source;
ALTER TABLE bank_statement ADD CONSTRAINT ck_bank_statement_source CHECK (source IN ('FILE', 'MANUAL', 'OFX', 'MT940'));

-- Valeur en XAF comptabilisee d'un mouvement de caisse (taux d'entree, ou valeur moyenne a la sortie).
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS accounting_base_amount NUMERIC(19, 4);

-- Detail fige de l'etat de rapprochement valide (suspens des deux cotes).
ALTER TABLE bank_reconciliation ADD COLUMN IF NOT EXISTS detail_json TEXT;

-- Schemas de sortie de caisse : la caisse est creditee a la valeur comptable des especes en devise,
-- la contrepartie au taux du jour ; l'ecart va en 676 (perte) ou 776 (gain). En XAF, rien ne change.
INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal, entry_label, lines,
                              no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT r.id + 20000, r.code, r.version + 1, r.event_type, r.label, r.description, r.family, r.priority, r.conditions, r.journal, r.entry_label,
       left(replace(r.lines, '"account":"{treasuryAccount}","amount":"amount"', '"account":"{treasuryAccount}","amount":"treasuryAmount"'),
            length(replace(r.lines, '"account":"{treasuryAccount}","amount":"amount"', '"account":"{treasuryAccount}","amount":"treasuryAmount"')) - 1)
           || ',{"side":"DEBIT","account":"{exchangeLossAccount}","amount":"exchangeLoss","label":"Perte de change"},'
           || '{"side":"CREDIT","account":"{exchangeGainAccount}","amount":"exchangeGain","label":"Gain de change"}]',
       r.no_entry,
       left(r.sample_payload, length(r.sample_payload) - 1) || ',"treasuryAmount":' || (r.sample_payload::jsonb ->> 'amount') || ',"exchangeLoss":0,"exchangeGain":0,"exchangeLossAccount":"676","exchangeGainAccount":"776"}',
       r.effective_from, 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'
FROM acc_posting_rule r
WHERE r.code IN ('CASH_OUT', 'CASH_SAFE_DEPOSIT', 'CASH_TRANSFER_OUT') AND r.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM acc_posting_rule o WHERE o.code = r.code AND o.version = r.version + 1);

UPDATE acc_posting_rule r SET status = 'RETIRED'
WHERE r.code IN ('CASH_OUT', 'CASH_SAFE_DEPOSIT', 'CASH_TRANSFER_OUT') AND r.status = 'ACTIVE'
  AND EXISTS (SELECT 1 FROM acc_posting_rule o WHERE o.code = r.code AND o.version > r.version AND o.status = 'ACTIVE');

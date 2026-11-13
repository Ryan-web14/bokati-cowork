-- Lot C9 (facturation, complements) : paiements non affectes. Une somme encaissee sans facture (ou
-- au-dela de ses factures) attend au compte 4719 ; son affectation apres coup a des factures
-- ouvertes du client passe D 4719 / C 411. Les imputations faites apres coup sont marquees LATE :
-- l'ecriture du paiement les laisse en attente et l'ecriture d'affectation les solde.

ALTER TABLE payment_allocation ADD COLUMN IF NOT EXISTS allocation_mode VARCHAR(20) NOT NULL DEFAULT 'PAYMENT';
ALTER TABLE payment_allocation ADD COLUMN IF NOT EXISTS allocated_by VARCHAR(180);

INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal,
                              entry_label, lines, no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
SELECT 3450001, 'SALES_LATE_ALLOCATION', 1, 'LATE_ALLOCATION', 'Affectation d''un encaissement en attente a une facture',
       'D compte d''attente des encaissements non imputes ; C client (lettre avec la facture).', 'SALES', 10, NULL, '{journalCode}',
       'Affectation de {transactionNumber} a {documentNumber}',
       '[{"side":"DEBIT","account":"{unallocatedAccount}","amount":"amount","reference":"{transactionNumber}"},{"side":"CREDIT","account":"{clientAccount}","amount":"amount","thirdParty":"{customerCode}","reference":"{documentNumber}"}]',
       FALSE, '{"transactionNumber":"TXN-1","documentNumber":"FAC-1","customerCode":"CUS-1","journalCode":"VT","clientAccount":"411","unallocatedAccount":"4719","amount":590000}',
       DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM acc_posting_rule r WHERE r.code = 'SALES_LATE_ALLOCATION');

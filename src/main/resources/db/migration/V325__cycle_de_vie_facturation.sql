-- Lot B1 (facturation) : cycle de vie des documents.
-- 1. VOIDED est retire (decision D7) : il n'avait aucun sens distinct de CANCELLED.
-- 2. Sur un avoir, ISSUED voulait dire « consomme ». Il veut desormais dire « emis, non impute » ;
--    la consommation a ses propres statuts : APPLIED (impute sur une facture) et REFUNDED
--    (reverse au portefeuille). Un avoir ISSUED etait consomme s'il etait scelle (la validation
--    le passait a VALIDATED, seule l'imputation ou le reversement le remettait a ISSUED) ou s'il
--    n'avait jamais ete emis (imputation immediate a la creation, sans emission).

UPDATE billing_document SET status = 'CANCELLED', updated_at = NOW() WHERE status = 'VOIDED';

UPDATE billing_document d SET status = 'REFUNDED', updated_at = NOW()
WHERE d.document_type = 'CREDIT_NOTE' AND d.status = 'ISSUED'
  AND EXISTS (SELECT 1 FROM wallet_ledger_entry w WHERE w.idempotency_key = 'CREDIT_NOTE_REFUND:' || d.document_number);

UPDATE billing_document d SET status = 'APPLIED', updated_at = NOW()
WHERE d.document_type = 'CREDIT_NOTE' AND d.status = 'ISSUED'
  AND (d.locked = TRUE OR d.issued_at IS NULL);

-- Acompte encore attendu sur un devis refuse, expire ou annule : abandonne.
UPDATE billing_document_advance a SET status = 'CANCELLED', updated_at = NOW()
WHERE a.status = 'PENDING'
  AND EXISTS (SELECT 1 FROM billing_document d WHERE d.id = a.document_id AND d.document_type = 'QUOTE'
              AND d.status IN ('REJECTED', 'EXPIRED', 'CANCELLED'));

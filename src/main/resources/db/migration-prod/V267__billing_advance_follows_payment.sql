-- L'acompte suit ce qui est paye sur sa facture · en attente, partiellement regle, ou regle.
-- Un paiement partiel laissait l'acompte en attente ; les acomptes deja concernes sont remis
-- d'aplomb ici, avec la regle que l'application applique desormais a chaque paiement.
UPDATE billing_document_advance a
SET status = CASE
        WHEN COALESCE(d.paid_amount, 0) <= 0 THEN 'PENDING'
        WHEN COALESCE(d.paid_amount, 0) >= a.computed_amount THEN 'PAID'
        ELSE 'PARTIALLY_PAID'
    END,
    paid_at = CASE
        WHEN COALESCE(d.paid_amount, 0) >= a.computed_amount THEN COALESCE(a.paid_at, d.paid_at, NOW())
        ELSE NULL
    END,
    updated_at = NOW()
FROM billing_document d
WHERE a.document_id = d.id
  AND a.status <> 'CANCELLED'
  AND a.computed_amount IS NOT NULL
  AND a.computed_amount > 0
  AND d.document_type <> 'QUOTE';

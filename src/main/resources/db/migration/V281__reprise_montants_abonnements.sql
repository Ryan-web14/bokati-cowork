-- Reprise des abonnements crees avant la correction du renouvellement.
--
-- Jusqu'ici la creation laissait dans subtotal_amount, tax_amount et total_amount de l'abonnement
-- le total de la premiere facture : prix de la periode, frais d'installation et caution. Le
-- renouvellement relit ces montants, il refacturait donc les frais et la caution a chaque periode.
--
-- Un abonnement est repris seulement si ses montants sont encore ceux de la creation : son total
-- egale la somme des elements facturables de la souscription (SUBSCRIPTION_RECURRING,
-- SUBSCRIPTION_SETUP_FEE, DEPOSIT) et il en a au moins un de frais ou de caution. Un abonnement
-- dont le plan a change depuis porte deja le prix de la periode seul et n'est pas touche.
--
-- Le nouveau total est l'element SUBSCRIPTION_RECURRING : le prix de la periode taxe comprise,
-- remises deja appliquees. Sous-total et taxe gardent la proportion d'origine entre base et taxe.
--
-- Les anciennes valeurs sont gardees dans subscription_amount_reprise, avec les renouvellements
-- deja factures au mauvais montant : les factures emises ne sont pas modifiees ici.

CREATE TABLE IF NOT EXISTS subscription_amount_reprise (
    subscription_id       BIGINT PRIMARY KEY,
    subscription_number   VARCHAR(100) NOT NULL,
    old_subtotal_amount   NUMERIC(19,4) NOT NULL,
    old_tax_amount        NUMERIC(19,4) NOT NULL,
    old_total_amount      NUMERIC(19,4) NOT NULL,
    new_subtotal_amount   NUMERIC(19,4) NOT NULL,
    new_tax_amount        NUMERIC(19,4) NOT NULL,
    new_total_amount      NUMERIC(19,4) NOT NULL,
    setup_fee_amount      NUMERIC(19,4) NOT NULL,
    deposit_amount        NUMERIC(19,4) NOT NULL,
    overbilled_renewals   INTEGER NOT NULL DEFAULT 0,
    overbilled_amount     NUMERIC(19,4) NOT NULL DEFAULT 0,
    repris_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

WITH creation AS (
    SELECT bi.source_id,
           SUM(CASE WHEN bi.source_type = 'SUBSCRIPTION_RECURRING' THEN bi.amount ELSE 0 END) AS recurring,
           SUM(CASE WHEN bi.source_type = 'SUBSCRIPTION_SETUP_FEE' THEN bi.amount ELSE 0 END) AS setup_fee,
           SUM(CASE WHEN bi.source_type = 'DEPOSIT' THEN bi.amount ELSE 0 END) AS deposit,
           COUNT(*) FILTER (WHERE bi.source_type = 'SUBSCRIPTION_RECURRING') AS recurring_count
    FROM billable_item bi
    WHERE bi.source_type IN ('SUBSCRIPTION_RECURRING', 'SUBSCRIPTION_SETUP_FEE', 'DEPOSIT')
    GROUP BY bi.source_id
),
candidates AS (
    SELECT s.id, s.subscription_number, s.subtotal_amount, s.tax_amount, s.total_amount,
           c.recurring, c.setup_fee, c.deposit,
           s.total_amount - c.deposit AS chargeable
    FROM subscription s
    JOIN creation c ON c.source_id = s.subscription_number
    WHERE c.recurring_count = 1
      AND c.recurring > 0
      AND (c.setup_fee > 0 OR c.deposit > 0)
      AND ABS(s.total_amount - (c.recurring + c.setup_fee + c.deposit)) <= 0.01
      AND s.total_amount - c.deposit > 0
),
renewals AS (
    SELECT bi.source_id, COUNT(*) AS n, SUM(bi.amount) AS amount
    FROM billable_item bi
    JOIN candidates k ON k.subscription_number = bi.source_id
    WHERE bi.source_type = 'SUBSCRIPTION_RENEWAL'
      AND ABS(bi.amount - k.total_amount) <= 0.01
    GROUP BY bi.source_id
)
INSERT INTO subscription_amount_reprise (
    subscription_id, subscription_number,
    old_subtotal_amount, old_tax_amount, old_total_amount,
    new_subtotal_amount, new_tax_amount, new_total_amount,
    setup_fee_amount, deposit_amount, overbilled_renewals, overbilled_amount)
SELECT k.id, k.subscription_number,
       k.subtotal_amount, k.tax_amount, k.total_amount,
       ROUND(k.subtotal_amount * k.recurring / k.chargeable, 4),
       k.recurring - ROUND(k.subtotal_amount * k.recurring / k.chargeable, 4),
       k.recurring,
       k.setup_fee, k.deposit,
       COALESCE(r.n, 0),
       COALESCE(r.n, 0) * (k.total_amount - k.recurring)
FROM candidates k
LEFT JOIN renewals r ON r.source_id = k.subscription_number
ON CONFLICT (subscription_id) DO NOTHING;

UPDATE subscription s
SET subtotal_amount = r.new_subtotal_amount,
    tax_amount      = r.new_tax_amount,
    total_amount    = r.new_total_amount,
    updated_at      = now()
FROM subscription_amount_reprise r
WHERE r.subscription_id = s.id
  AND s.total_amount = r.old_total_amount;

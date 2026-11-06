-- Un abonnement ne se renouvelle qu'une fois paye. A l'echeance, la facture est emise mais la periode
-- ne bouge pas : la periode a venir et sa facture attendent ici le reglement.

ALTER TABLE subscription ADD COLUMN IF NOT EXISTS pending_renewal_invoice VARCHAR(120);
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS pending_renewal_start DATE;
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS pending_renewal_end DATE;

CREATE INDEX IF NOT EXISTS idx_subscription_pending_renewal ON subscription (pending_renewal_invoice)
    WHERE pending_renewal_invoice IS NOT NULL;

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2750001, 'SUBSCRIPTION_RENEWAL_PAYMENT_DUE', 'EMAIL', 'Réglez votre abonnement pour le renouveler', 'subscription-renewal-payment-due', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'SUBSCRIPTION_RENEWAL_PAYMENT_DUE');

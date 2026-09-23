-- Trois courriels qui manquaient, tous sur la meme idee : personne ne doit rester sans nouvelles.
--   - l echec mobile money · le client ignorait pourquoi sa facture restait ouverte ;
--   - le depot sans reponse · il repayait, et se retrouvait debite deux fois ;
--   - le reglement depuis l espace client · la caisse ne l apprenait qu en ouvrant la facture.

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2470001, 'MOBILE_MONEY_DEPOSIT_FAILED', 'EMAIL', 'Votre paiement mobile money n''a pas abouti',
       'mobile-money-deposit-failed', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'MOBILE_MONEY_DEPOSIT_FAILED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2470002, 'MOBILE_MONEY_DEPOSIT_PENDING_REVIEW', 'EMAIL', 'Nous vérifions votre paiement mobile money',
       'mobile-money-deposit-pending-review', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'MOBILE_MONEY_DEPOSIT_PENDING_REVIEW');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2470003, 'SELF_SERVICE_PAYMENT_RECEIVED', 'EMAIL', 'Paiement reçu depuis l''espace client',
       'self-service-payment-received', NULL, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'SELF_SERVICE_PAYMENT_RECEIVED');

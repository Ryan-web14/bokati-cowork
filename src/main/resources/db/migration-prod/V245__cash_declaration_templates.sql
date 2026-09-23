-- Les courriels de l encaissement en especes annonce.
--   - a la caisse : quelqu un passera payer, il faudra confirmer ;
--   - au client : l argent a ete compte, voici la trace ;
--   - au client : le delai est passe, sa facture est de nouveau a regler.

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2490001, 'CASH_PAYMENT_DECLARED', 'EMAIL', 'Espèces annoncées · à encaisser',
       'cash-payment-declared', NULL, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'CASH_PAYMENT_DECLARED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2490002, 'CASH_PAYMENT_CONFIRMED', 'EMAIL', 'Votre paiement en espèces a bien été reçu',
       'cash-payment-confirmed', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'CASH_PAYMENT_CONFIRMED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2490003, 'CASH_PAYMENT_DECLARATION_EXPIRED', 'EMAIL', 'Votre paiement en espèces n''a pas été reçu',
       'cash-payment-declaration-expired', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'CASH_PAYMENT_DECLARATION_EXPIRED');

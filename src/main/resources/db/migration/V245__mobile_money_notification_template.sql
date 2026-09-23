-- Le courriel qui reclame un rapprochement quand l operateur n a jamais tranche.
-- Un depot sans reponse definitive n est ni un encaissement ni un echec · il demande quelqu un.

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2450001, 'MOBILE_MONEY_DEPOSIT_UNRESOLVED', 'EMAIL', 'Paiement mobile money sans réponse · à rapprocher',
       'mobile-money-deposit-unresolved', NULL, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'MOBILE_MONEY_DEPOSIT_UNRESOLVED');

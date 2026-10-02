-- Les trois courriels de la fin d abonnement · J-7 et J-3, le jour meme, puis la cloture.
-- Le code de gabarit est celui que le service pose dans la charge utile, en minuscules.

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2510001, 'subscription_ending_soon', 'EMAIL', 'Votre abonnement arrive à son terme',
       'subscription_ending_soon', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'subscription_ending_soon');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2510002, 'subscription_ends_today', 'EMAIL', 'Votre abonnement prend fin ce soir',
       'subscription_ends_today', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'subscription_ends_today');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2510003, 'subscription_ended', 'EMAIL', 'Votre abonnement a pris fin',
       'subscription_ended', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'subscription_ended');

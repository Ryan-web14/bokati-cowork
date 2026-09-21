-- Lot notifications · les courriels des lots portefeuille, domiciliation, abonnements et relances
-- Un code d evenement, un sujet par defaut, un gabarit Thymeleaf. Le sujet porte par la charge utile prime.

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430001, 'WALLET_TRANSFER_SENT', 'EMAIL', 'Transfert envoyé', 'wallet-transfer-sent', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_TRANSFER_SENT');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430002, 'WALLET_TRANSFER_RECEIVED', 'EMAIL', 'Transfert reçu', 'wallet-transfer-received', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_TRANSFER_RECEIVED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430003, 'WALLET_TOPUP_COMPLETED', 'EMAIL', 'Rechargement effectué', 'wallet-topup-completed', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_TOPUP_COMPLETED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430004, 'WALLET_PAYMENT_REQUEST_RECEIVED', 'EMAIL', 'Demande de paiement', 'wallet-payment-request', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_PAYMENT_REQUEST_RECEIVED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430005, 'WALLET_LOW_BALANCE', 'EMAIL', 'Solde bas sur votre portefeuille', 'wallet-low-balance', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_LOW_BALANCE');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430006, 'WALLET_FROZEN', 'EMAIL', 'Votre portefeuille est suspendu', 'wallet-status-changed', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_FROZEN');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430007, 'WALLET_SUSPENDED', 'EMAIL', 'Votre portefeuille a été suspendu', 'wallet-status-changed', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_SUSPENDED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430008, 'WALLET_UNSUSPENDED', 'EMAIL', 'Votre portefeuille est de nouveau actif', 'wallet-status-changed', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_UNSUSPENDED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430009, 'WALLET_CLOSED', 'EMAIL', 'Votre portefeuille a été clôturé', 'wallet-status-changed', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_CLOSED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430010, 'WALLET_NEW_DEVICE', 'EMAIL', 'Nouvel appareil sur votre portefeuille', 'wallet-security-event', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_NEW_DEVICE');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430011, 'WALLET_REVOKED_DEVICE_USED', 'EMAIL', 'Tentative depuis un appareil révoqué', 'wallet-security-event', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_REVOKED_DEVICE_USED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430012, 'WALLET_LOCKED_BY_OWNER', 'EMAIL', 'Votre portefeuille a été verrouillé', 'wallet-security-event', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_LOCKED_BY_OWNER');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430013, 'WALLET_UNLOCKED_BY_OWNER', 'EMAIL', 'Votre portefeuille a été déverrouillé', 'wallet-security-event', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_UNLOCKED_BY_OWNER');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430014, 'WALLET_KYC_DOCUMENTS_REQUESTED', 'EMAIL', 'Pièces à fournir pour relever vos plafonds', 'wallet-kyc-documents-requested', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_KYC_DOCUMENTS_REQUESTED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430015, 'WALLET_TREASURY_COVERAGE_ALERT', 'EMAIL', 'Couverture des portefeuilles insuffisante', 'wallet-treasury-alert', NULL, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'WALLET_TREASURY_COVERAGE_ALERT');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430016, 'DOMICILIATION_ACTIVATED', 'EMAIL', 'Votre domiciliation est active', 'domiciliation-activated', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'DOMICILIATION_ACTIVATED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430017, 'DOMICILIATION_REGISTRATION_REJECTED', 'EMAIL', 'Enregistrement de votre domiciliation à reprendre', 'domiciliation-registration-rejected', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'DOMICILIATION_REGISTRATION_REJECTED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430018, 'DOMICILIATION_CERTIFICATE_ISSUED', 'EMAIL', 'Votre attestation de domiciliation est disponible', 'domiciliation-certificate-issued', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'DOMICILIATION_CERTIFICATE_ISSUED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430019, 'DOMICILIATION_CERTIFICATE_EXPIRING', 'EMAIL', 'Votre attestation de domiciliation arrive à échéance', 'domiciliation-certificate-expiring', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'DOMICILIATION_CERTIFICATE_EXPIRING');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430020, 'DOMICILIATION_FISCAL_LOST', 'EMAIL', 'Votre adresse n est plus fiscale', 'domiciliation-fiscal-lost', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'DOMICILIATION_FISCAL_LOST');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430021, 'DOMICILIATION_TERMINATED', 'EMAIL', 'Fin de votre domiciliation', 'domiciliation-terminated', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'DOMICILIATION_TERMINATED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430022, 'DOMICILIATION_FISCAL_LOST_ADMIN', 'EMAIL', 'Perte de la qualité fiscale d une domiciliation', 'domiciliation-administration-notice', NULL, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'DOMICILIATION_FISCAL_LOST_ADMIN');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430023, 'DOMICILIATION_TERMINATED_ADMIN', 'EMAIL', 'Fin de domiciliation', 'domiciliation-administration-notice', NULL, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'DOMICILIATION_TERMINATED_ADMIN');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430024, 'MAIL_RECEIVED', 'EMAIL', 'Du courrier vous attend', 'mail-item-event', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'MAIL_RECEIVED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430025, 'MAIL_SCANNED', 'EMAIL', 'Votre courrier a été numérisé', 'mail-item-event', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'MAIL_SCANNED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430026, 'MAIL_FORWARDED', 'EMAIL', 'Votre courrier a été réexpédié', 'mail-item-event', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'MAIL_FORWARDED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430027, 'MAIL_STORAGE_OVERDUE', 'EMAIL', 'Courrier en attente de retrait', 'mail-item-event', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'MAIL_STORAGE_OVERDUE');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430028, 'SUBSCRIPTION_GRACE_PERIOD', 'EMAIL', 'Échéance impayée sur votre abonnement', 'subscription-grace-period', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'SUBSCRIPTION_GRACE_PERIOD');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430029, 'SUBSCRIPTION_TERMINATION_REQUESTED', 'EMAIL', 'Votre demande de résiliation', 'subscription-termination', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'SUBSCRIPTION_TERMINATION_REQUESTED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430030, 'SUBSCRIPTION_TERMINATION_ACCEPTED', 'EMAIL', 'Résiliation acceptée', 'subscription-termination', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'SUBSCRIPTION_TERMINATION_ACCEPTED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430031, 'SUBSCRIPTION_TERMINATION_COMPLETED', 'EMAIL', 'Votre abonnement est clos', 'subscription-termination', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'SUBSCRIPTION_TERMINATION_COMPLETED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430032, 'SUBSCRIPTION_DIRECT_DEBIT_FAILED', 'EMAIL', 'Prélèvement impossible', 'subscription-direct-debit-failed', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'SUBSCRIPTION_DIRECT_DEBIT_FAILED');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430033, 'SUBSCRIPTION_QUOTE_SENT', 'EMAIL', 'Votre devis', 'subscription-quote-sent', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'SUBSCRIPTION_QUOTE_SENT');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430034, 'BILLING_DUNNING_REMINDER', 'EMAIL', 'Rappel · facture en attente', 'billing-dunning', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'BILLING_DUNNING_REMINDER');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430035, 'BILLING_DUNNING_FORMAL_NOTICE', 'EMAIL', 'Mise en demeure', 'billing-dunning', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'BILLING_DUNNING_FORMAL_NOTICE');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430036, 'BILLING_DUNNING_GRACE_PERIOD', 'EMAIL', 'Échéance impayée', 'billing-dunning', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'BILLING_DUNNING_GRACE_PERIOD');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430037, 'BILLING_DUNNING_SUSPEND', 'EMAIL', 'Suspension pour impayé', 'billing-dunning', NULL, TRUE, FALSE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'BILLING_DUNNING_SUSPEND');

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2430038, 'BILLING_DUNNING_HANDOVER', 'EMAIL', 'Impayé à traiter', 'billing-dunning-handover', NULL, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'BILLING_DUNNING_HANDOVER');

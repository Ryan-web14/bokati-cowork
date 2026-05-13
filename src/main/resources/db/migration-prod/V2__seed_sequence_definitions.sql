insert into sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
values

-- =========================================================
-- COMMERCIAL / FACTURATION
-- =========================================================
(1, 'quotation', 'Devis client', 'Séquence des devis clients', 'DEV', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, true, now(), now()),
(2, 'invoice', 'Facture client', 'Séquence des factures clients', 'FAC', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, true, now(), now()),
(3, 'credit_note', 'Avoir client', 'Séquence des notes de crédit / avoirs', 'AV', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, true, now(), now()),
(4, 'receipt', 'Reçu', 'Séquence des reçus de paiement', 'REC', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, true, now(), now()),
(5, 'customer_payment', 'Paiement client', 'Séquence des paiements clients', 'PAY', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()),

-- =========================================================
-- MEMBRES / ABONNEMENTS / COWORKING
-- =========================================================
(6, 'member', 'Membre', 'Identifiant des membres', 'MBR', null, '{PREFIX}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()),
(7, 'membership', 'Adhésion', 'Référence des adhésions', 'MSH', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()),
(8, 'subscription', 'Abonnement', 'Référence des abonnements', 'SUB', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()),
(9, 'booking', 'Réservation', 'Référence des réservations', 'RES', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(10, 'day_pass', 'Pass journalier', 'Référence des pass journaliers', 'DAY', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(11, 'week_pass', 'Pass hebdomadaire', 'Référence des pass hebdomadaires', 'WEEK', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(12, 'month_pass', 'Pass mensuel', 'Référence des pass mensuels', 'MON', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),

-- =========================================================
-- CONTRATS / JURIDIQUE
-- =========================================================
(13, 'contract', 'Contrat', 'Séquence générale des contrats', 'CTR', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()),
(14, 'domiciliation_contract', 'Contrat de domiciliation', 'Séquence des contrats de domiciliation', 'DOM', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()),
(15, 'service_contract', 'Contrat de service', 'Séquence des contrats de service', 'SER', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()),
(16, 'workspace_contract', 'Contrat espace de travail', 'Séquence des contrats de location / occupation d’espace', 'WSP', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()),

-- =========================================================
-- WALLET / CAISSE / POS
-- =========================================================
(17, 'wallet_account', 'Compte portefeuille', 'Identifiant des comptes portefeuille', 'WLT', null, '{PREFIX}-{SEQ}', 8, 1, 1, 'NEVER', true, false, now(), now()),
(18, 'wallet_topup', 'Recharge portefeuille', 'Séquence des recharges portefeuille', 'TOP', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(19, 'wallet_payment', 'Paiement portefeuille', 'Séquence des paiements via portefeuille', 'WPA', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(20, 'cash_transaction', 'Transaction de caisse', 'Séquence des transactions de caisse', 'CSH', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(21, 'pos_sale', 'Vente comptoir', 'Séquence des ventes POS snack bar / comptoir', 'POS', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(22, 'pos_refund', 'Remboursement POS', 'Séquence des remboursements POS', 'RPOS', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),

-- =========================================================
-- STOCK / INVENTAIRE / ACTIFS
-- =========================================================
(23, 'stock_movement', 'Mouvement de stock', 'Séquence des mouvements de stock', 'STM', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(24, 'stock_adjustment', 'Ajustement de stock', 'Séquence des ajustements de stock', 'STA', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(25, 'inventory_session', 'Session inventaire', 'Séquence des sessions d’inventaire', 'INV', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(26, 'inventory_item', 'Article inventaire', 'Identifiant des articles / éléments inventoriés', 'ITM', null, '{PREFIX}-{SEQ}', 8, 1, 1, 'NEVER', true, false, now(), now()),
(27, 'asset', 'Immobilisation / actif', 'Identifiant des actifs et immobilisations', 'AST', null, '{PREFIX}-{SEQ}', 8, 1, 1, 'NEVER', true, false, now(), now()),
(28, 'equipment', 'Équipement', 'Identifiant des équipements', 'EQP', null, '{PREFIX}-{SEQ}', 8, 1, 1, 'NEVER', true, false, now(), now()),
(29, 'consumable', 'Consommable', 'Identifiant des consommables', 'CON', null, '{PREFIX}-{SEQ}', 8, 1, 1, 'NEVER', true, false, now(), now()),

-- =========================================================
-- SUPPORT / EXPLOITATION
-- =========================================================
(30, 'support_ticket', 'Ticket support', 'Séquence des tickets de support', 'SUP', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(31, 'incident', 'Incident', 'Séquence des incidents', 'INC', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),

-- =========================================================
-- RESSOURCES / COWORKING
-- =========================================================
(38, 'resource_type', 'Type de ressource', 'Séquence des types de ressources', 'RTY', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()),
(39, 'resource_group', 'Groupe de ressources', 'Séquence des groupes de ressources', 'RGP', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()),
(40, 'resource_policy', 'Politique de ressource', 'Séquence des politiques de réservation des ressources', 'RPL', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()),
(41, 'resource', 'Ressource', 'Séquence des ressources réservables', 'RES', null, '{PREFIX}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()),
(42, 'resource_amenity', 'Amenité de ressource', 'Séquence des équipements et commodités de ressources', 'AMN', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()),
(43, 'resource_closure', 'Fermeture de ressource', 'Séquence des fermetures et indisponibilités de ressources', 'RCL', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(44, 'resource_pricing_rule', 'Règle tarifaire ressource', 'Séquence des règles tarifaires de ressources', 'RPR', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()),

-- =========================================================
-- DOCUMENTS / KYC / JURIDIQUE
-- =========================================================
(45, 'document_type', 'Type de document', 'Séquence des types de documents', 'DTY', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()),
(46, 'document', 'Document', 'Séquence des documents', 'DOC', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(47, 'document_requirement', 'Exigence documentaire', 'Séquence des règles et exigences documentaires', 'DREQ', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()),
(48, 'document_review', 'Revue de document', 'Séquence des revues de documents', 'DRV', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(49, 'document_signature', 'Signature de document', 'Séquence des signatures de documents', 'DSG', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(50, 'kyc_document', 'Document KYC', 'Séquence des documents KYC', 'KYC', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(51, 'kyc_verification', 'Vérification KYC', 'Séquence des vérifications KYC', 'KYV', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),
(52, 'legal_document', 'Document juridique', 'Séquence des documents juridiques', 'LDC', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()),

-- =========================================================
-- COMPTABILITE
-- =========================================================
(32, 'accounting_entry', 'Écriture comptable', 'Séquence des écritures comptables', 'ECR', null, '{PREFIX}-{YYYY}-{SEQ}', 7, 1, 1, 'YEARLY', true, true, now(), now()),
(33, 'accounting_voucher', 'Pièce comptable', 'Séquence des pièces comptables', 'PC', null, '{PREFIX}-{YYYY}-{SEQ}', 7, 1, 1, 'YEARLY', true, true, now(), now()),
(34, 'journal_entry_batch', 'Lot d’écritures', 'Séquence des lots d’écritures comptables', 'LOT', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()),

-- =========================================================
-- ACHATS / FOURNISSEURS
-- =========================================================
(35, 'purchase_order', 'Bon de commande', 'Séquence des bons de commande fournisseurs', 'BC', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()),
(36, 'goods_receipt', 'Bon de réception', 'Séquence des bons de réception fournisseurs', 'BR', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()),
(37, 'supplier_invoice', 'Facture fournisseur', 'Séquence des factures fournisseurs', 'FF', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now());

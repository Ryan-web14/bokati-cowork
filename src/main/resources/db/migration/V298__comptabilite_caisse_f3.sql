-- Lot F3 du plan finance : tresorerie et caisse comptable.
--
--   * cash_movement enrichi : journal choisi, etat et piece comptables, TVA, fournisseur,
--     beneficiaire, demande ;
--   * cash_variance_review : etat comptable du constat et de l'imputation de l'ecart ;
--   * acc_journal_register : caisses autorisees a ecrire dans un journal ;
--   * schemas d'ecriture initiaux de la caisse (catalogue, section 2), actifs.

ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS journal_code VARCHAR(30);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS accounting_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS accounting_entry_number VARCHAR(40);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS accounting_note VARCHAR(1000);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS vat_amount NUMERIC(19, 4);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS vat_code VARCHAR(20);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS supplier_code VARCHAR(80);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS beneficiary_name VARCHAR(255);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS beneficiary_id_number VARCHAR(80);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS beneficiary_phone VARCHAR(40);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS request_number VARCHAR(60);

-- Les paiements et remboursements sont ecrits par l'evenement de paiement (lot F4) ; comptages,
-- echanges et ecarts n'ont pas d'ecriture de mouvement ; un mouvement annule non plus.
UPDATE cash_movement SET accounting_status = 'NOT_APPLICABLE'
WHERE movement_type IN ('PAYMENT', 'REFUND', 'CLOSING_COUNT', 'EXCHANGE', 'VARIANCE') OR status = 'CANCELLED';

ALTER TABLE cash_movement DROP CONSTRAINT IF EXISTS ck_cash_movement_accounting_status;
ALTER TABLE cash_movement ADD CONSTRAINT ck_cash_movement_accounting_status
    CHECK (accounting_status IN ('NOT_APPLICABLE', 'PENDING', 'QUEUED', 'POSTED', 'UNBALANCED', 'FAILED', 'IGNORED'));
CREATE INDEX IF NOT EXISTS idx_cash_movement_accounting_status ON cash_movement (accounting_status);

ALTER TABLE cash_variance_review ADD COLUMN IF NOT EXISTS accounting_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE cash_variance_review ADD COLUMN IF NOT EXISTS decision_accounting_status VARCHAR(20);
ALTER TABLE cash_variance_review ADD COLUMN IF NOT EXISTS decision_entry_number VARCHAR(40);
-- Un ecart remplace par un recomptage n'a jamais existe en comptabilite.
UPDATE cash_variance_review SET accounting_status = 'NOT_APPLICABLE' WHERE status = 'DECIDED' AND decision IS NULL;

CREATE TABLE IF NOT EXISTS acc_journal_register
(
    id                 BIGINT PRIMARY KEY,
    journal_code       VARCHAR(30)  NOT NULL,
    cash_register_code VARCHAR(100) NOT NULL,
    created_by         VARCHAR(180) NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_acc_journal_register UNIQUE (journal_code, cash_register_code)
);

-- ------------------------------------------------------------- schemas d'ecriture de la caisse

INSERT INTO acc_posting_rule (id, code, version, event_type, label, description, family, priority, conditions, journal,
                              entry_label, lines, no_entry, sample_payload, effective_from, status, activated_by, activated_at, created_by)
VALUES
(2963001, 'CASH_OPENING_FIRST', 1, 'CASH_MOVEMENT', 'Fonds de caisse initial',
 'Premiere session d''une caisse : le fonds vient de la caisse principale ou de la banque (virement interne 585).', 'CASH', 10,
 '{"type":"OPENING_FLOAT","firstSession":"true"}', '{journalCode}', 'Fonds de caisse initial {registerName}',
 '[{"side":"DEBIT","account":"{treasuryAccount}","amount":"amount","reference":"{sessionNumber}"},{"side":"CREDIT","account":"585","amount":"amount","reference":"{sessionNumber}"}]',
 FALSE, '{"type":"OPENING_FLOAT","firstSession":"true","amount":50000,"treasuryAccount":"571","journalCode":"OD","registerName":"Exemple","sessionNumber":"SES-1"}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2963002, 'CASH_OPENING_CARRY', 1, 'CASH_MOVEMENT', 'Fonds de caisse reporte',
 'Ouverture d''une session suivante, passation, report de fin de mois : l''argent ne bouge pas.', 'CASH', 20,
 '{"type":"OPENING_FLOAT"}', NULL, NULL, NULL, TRUE, NULL, DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2963003, 'CASH_OUT', 1, 'CASH_MOVEMENT', 'Sortie de caisse',
 'Bon de sortie : compte de la nature (HT), TVA recuperable, credit de la caisse.', 'CASH', 30,
 '{"type":"CASH_OUT"}', '{journalCode}', '{label}',
 '[{"side":"DEBIT","account":"{natureAccount}","amount":"netAmount","thirdParty":"{?natureThirdParty}","reference":"{?requestNumber}"},{"side":"DEBIT","account":"{vatAccount}","amount":"vatAmount"},{"side":"CREDIT","account":"{treasuryAccount}","amount":"amount","reference":"{movementNumber}"}]',
 FALSE, '{"type":"CASH_OUT","amount":11800,"netAmount":10000,"vatAmount":1800,"vatAccount":"4452","natureAccount":"6055","treasuryAccount":"571","journalCode":"OD","label":"Achat de fournitures","movementNumber":"MVT-1"}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2963004, 'CASH_IN', 1, 'CASH_MOVEMENT', 'Entree de caisse',
 'Bon d''entree hors encaissement client : debit de la caisse, compte de la nature, TVA collectee.', 'CASH', 30,
 '{"type":"CASH_IN"}', '{journalCode}', '{label}',
 '[{"side":"DEBIT","account":"{treasuryAccount}","amount":"amount","reference":"{movementNumber}"},{"side":"CREDIT","account":"{natureAccount}","amount":"netAmount","thirdParty":"{?natureThirdParty}","reference":"{?requestNumber}"},{"side":"CREDIT","account":"{vatAccount}","amount":"vatAmount"}]',
 FALSE, '{"type":"CASH_IN","amount":5000,"netAmount":5000,"vatAmount":0,"vatAccount":"4431","natureAccount":"758","treasuryAccount":"571","journalCode":"OD","label":"Produit divers","movementNumber":"MVT-2"}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2963005, 'CASH_SAFE_DEPOSIT', 1, 'CASH_MOVEMENT', 'Remise en banque ou au coffre',
 'Virement interne 585 ; la banque ou le coffre solde le 585.', 'CASH', 30,
 '{"type":"SAFE_DEPOSIT"}', '{journalCode}', '{label}',
 '[{"side":"DEBIT","account":"585","amount":"amount","reference":"{transferReference}"},{"side":"CREDIT","account":"{treasuryAccount}","amount":"amount","reference":"{movementNumber}"}]',
 FALSE, '{"type":"SAFE_DEPOSIT","amount":100000,"treasuryAccount":"571","journalCode":"OD","label":"Remise","movementNumber":"MVT-3","transferReference":"OTI-1"}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2963006, 'CASH_TRANSFER_OUT', 1, 'CASH_MOVEMENT', 'Transfert sortant',
 'Caisse emettrice : debit 585, credit de la caisse.', 'CASH', 30,
 '{"type":"TRANSFER_OUT"}', '{journalCode}', '{label}',
 '[{"side":"DEBIT","account":"585","amount":"amount","reference":"{transferReference}"},{"side":"CREDIT","account":"{treasuryAccount}","amount":"amount","reference":"{movementNumber}"}]',
 FALSE, '{"type":"TRANSFER_OUT","amount":20000,"treasuryAccount":"571","journalCode":"OD","label":"Transfert","movementNumber":"MVT-4","transferReference":"OTI-2"}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2963007, 'CASH_TRANSFER_IN', 1, 'CASH_MOVEMENT', 'Transfert entrant',
 'Caisse receptrice : debit de la caisse, credit 585.', 'CASH', 30,
 '{"type":"TRANSFER_IN"}', '{journalCode}', '{label}',
 '[{"side":"DEBIT","account":"{treasuryAccount}","amount":"amount","reference":"{movementNumber}"},{"side":"CREDIT","account":"585","amount":"amount","reference":"{transferReference}"}]',
 FALSE, '{"type":"TRANSFER_IN","amount":20000,"treasuryAccount":"571","journalCode":"OD","label":"Transfert","movementNumber":"MVT-5","transferReference":"OTI-2"}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2963008, 'CASH_ADJUSTMENT', 1, 'CASH_MOVEMENT', 'Ajustement de caisse',
 'Correction approuvee : la caisse contre le compte de la nature, a defaut 471. Montant signe (entree positive, sortie negative).', 'CASH', 30,
 '{"type":"ADJUSTMENT"}', '{journalCode}', '{label}',
 '[{"side":"DEBIT","account":"{treasuryAccount}","amount":"signedAmount","signed":true,"reference":"{movementNumber}"},{"side":"CREDIT","account":"{counterAccount}","amount":"signedAmount","signed":true,"thirdParty":"{?natureThirdParty}"}]',
 FALSE, '{"type":"ADJUSTMENT","amount":300,"signedAmount":-300,"counterAccount":"471","treasuryAccount":"571","journalCode":"OD","label":"Ajustement","movementNumber":"MVT-6"}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2963009, 'CASH_VARIANCE_RECORDED', 1, 'CASH_VARIANCE_RECORDED', 'Ecart de caisse constate',
 'Ecart (compte moins attendu) passe en 471 en attendant la decision : manquant D 471 C caisse, excedent D caisse C 471.', 'CASH', 10,
 NULL, '{journalCode}', 'Ecart de caisse {reviewNumber} ({kind})',
 '[{"side":"DEBIT","account":"{treasuryAccount}","amount":"amount","signed":true,"reference":"{sessionNumber}"},{"side":"CREDIT","account":"471","amount":"amount","signed":true,"reference":"{reviewNumber}"}]',
 FALSE, '{"amount":-500,"treasuryAccount":"571","journalCode":"OD","reviewNumber":"ECR-1","kind":"CLOSING","sessionNumber":"SES-1"}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2963010, 'CASH_VARIANCE_EXPENSE', 1, 'CASH_VARIANCE_DECIDED', 'Ecart passe en charge',
 'Manquant passe en charge : D 658, C 471.', 'CASH', 10,
 '{"imputation":"EXPENSE"}', 'OD', 'Imputation de l''ecart {reviewNumber} en charge',
 '[{"side":"DEBIT","account":"658","amount":"0 - amount","signed":true},{"side":"CREDIT","account":"471","amount":"0 - amount","signed":true,"reference":"{reviewNumber}"}]',
 FALSE, '{"imputation":"EXPENSE","amount":-500,"journalCode":"OD","reviewNumber":"ECR-1"}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2963011, 'CASH_VARIANCE_INCOME', 1, 'CASH_VARIANCE_DECIDED', 'Ecart passe en produit',
 'Excedent passe en produit : D 471, C 758.', 'CASH', 10,
 '{"imputation":"INCOME"}', 'OD', 'Imputation de l''ecart {reviewNumber} en produit',
 '[{"side":"DEBIT","account":"471","amount":"amount","signed":true,"reference":"{reviewNumber}"},{"side":"CREDIT","account":"758","amount":"amount","signed":true}]',
 FALSE, '{"imputation":"INCOME","amount":500,"journalCode":"OD","reviewNumber":"ECR-2"}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2963012, 'CASH_VARIANCE_CASHIER', 1, 'CASH_VARIANCE_DECIDED', 'Ecart impute au caissier',
 'Manquant impute au caissier : D 421 (tiers : le caissier), C 471.', 'CASH', 10,
 '{"imputation":"CASHIER"}', 'OD', 'Imputation de l''ecart {reviewNumber} au caissier',
 '[{"side":"DEBIT","account":"421","amount":"0 - amount","signed":true,"thirdParty":"{cashier}","reference":"{reviewNumber}"},{"side":"CREDIT","account":"471","amount":"0 - amount","signed":true,"reference":"{reviewNumber}"}]',
 FALSE, '{"imputation":"CASHIER","amount":-500,"journalCode":"OD","reviewNumber":"ECR-3","cashier":"caisse@elleaose.com"}',
 DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM'),
(2963013, 'CASH_VARIANCE_INVESTIGATION', 1, 'CASH_VARIANCE_DECIDED', 'Ecart mis en recherche',
 'L''ecart reste en 471 jusqu''a sa decision definitive.', 'CASH', 10,
 '{"imputation":"INVESTIGATION"}', NULL, NULL, NULL, TRUE, NULL, DATE '2026-01-01', 'ACTIVE', 'SYSTEM', now(), 'SYSTEM')
ON CONFLICT (code, version) DO NOTHING;

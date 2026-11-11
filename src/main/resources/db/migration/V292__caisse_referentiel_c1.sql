-- Plan finance, lot C1 : referentiel des caisses.
--
-- Fiche de caisse complete (type, statut, caisse mere, moyens et devises acceptes, plafonds,
-- fonds standard, destinataires des alertes, dernier comptage), affectations des caissiers
-- (titulaire, remplacants, interimaires), historique des modifications, horaires, postes
-- autorises, transferts de responsabilite et listes preenregistrees (motifs, banques, pieces).

ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS register_type VARCHAR(20);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS status VARCHAR(20);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS parent_register_code VARCHAR(100);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS accepted_methods VARCHAR(255);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS accepted_currencies VARCHAR(100);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS cap_behavior VARCHAR(10);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS max_out_per_operation NUMERIC(19, 4);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS max_out_per_day NUMERIC(19, 4);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS standard_float NUMERIC(19, 4);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS alert_recipients VARCHAR(1000);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS treasury_account_number VARCHAR(30);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS default_journal_code VARCHAR(30);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS last_count_at TIMESTAMPTZ;
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS last_count_amount NUMERIC(19, 4);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS updated_by VARCHAR(180);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS status_reason VARCHAR(500);
ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ;

UPDATE cash_register
SET register_type = CASE WHEN system_managed THEN 'AUTOMATIC' ELSE 'COUNTER' END
WHERE register_type IS NULL;

UPDATE cash_register
SET status = CASE WHEN active THEN 'ACTIVE' ELSE 'SUSPENDED' END
WHERE status IS NULL;

UPDATE cash_register SET accepted_currencies = 'XAF' WHERE accepted_currencies IS NULL;
UPDATE cash_register SET cap_behavior = 'ALERT' WHERE cap_behavior IS NULL;
UPDATE cash_register SET alert_recipients = manager_email
WHERE alert_recipients IS NULL AND manager_email IS NOT NULL AND trim(manager_email) <> '';

-- Dernier comptage : le montant compte a la derniere session close qui en a un.
UPDATE cash_register cr
SET last_count_at = last_count.closed_at,
    last_count_amount = last_count.counted_closing_amount
FROM (
    SELECT DISTINCT ON (cs.cash_register_id) cs.cash_register_id, cs.closed_at, cs.counted_closing_amount
    FROM cash_session cs
    WHERE cs.status = 'CLOSED' AND cs.counted_closing_amount IS NOT NULL AND cs.closed_at IS NOT NULL
    ORDER BY cs.cash_register_id, cs.closed_at DESC
) last_count
WHERE last_count.cash_register_id = cr.id AND cr.last_count_at IS NULL;

ALTER TABLE cash_register ALTER COLUMN register_type SET NOT NULL;
ALTER TABLE cash_register ALTER COLUMN register_type SET DEFAULT 'COUNTER';
ALTER TABLE cash_register ALTER COLUMN status SET NOT NULL;
ALTER TABLE cash_register ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE cash_register DROP CONSTRAINT IF EXISTS ck_cash_register_type;
ALTER TABLE cash_register ADD CONSTRAINT ck_cash_register_type
    CHECK (register_type IN ('COUNTER', 'MAIN', 'PETTY_CASH', 'SAFE', 'AUTOMATIC'));
ALTER TABLE cash_register DROP CONSTRAINT IF EXISTS ck_cash_register_status;
ALTER TABLE cash_register ADD CONSTRAINT ck_cash_register_status
    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'));

CREATE INDEX IF NOT EXISTS idx_cash_register_location_type ON cash_register (location_code, register_type);

ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS opened_from_device VARCHAR(120);
ALTER TABLE cash_session ADD COLUMN IF NOT EXISTS out_of_hours_reason VARCHAR(500);

CREATE TABLE IF NOT EXISTS cash_register_assignment
(
    id             BIGINT PRIMARY KEY,
    register_code  VARCHAR(100) NOT NULL,
    user_email     VARCHAR(180) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    quality        VARCHAR(20)  NOT NULL,
    backup_rank    INTEGER,
    valid_from     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    valid_to       TIMESTAMPTZ,
    emergency      BOOLEAN      NOT NULL DEFAULT FALSE,
    source         VARCHAR(40),
    source_reference VARCHAR(100),
    granted_by     VARCHAR(180),
    note           VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    revoked_at     TIMESTAMPTZ,
    revoked_by     VARCHAR(180),
    CONSTRAINT ck_cash_assignment_role CHECK (role IN ('CASHIER', 'HEAD_CASHIER', 'SUPERVISOR', 'VIEWER')),
    CONSTRAINT ck_cash_assignment_quality CHECK (quality IN ('HOLDER', 'BACKUP', 'INTERIM')),
    CONSTRAINT ck_cash_assignment_period CHECK (valid_to IS NULL OR valid_to > valid_from),
    CONSTRAINT ck_cash_assignment_interim_end CHECK (quality <> 'INTERIM' OR valid_to IS NOT NULL)
);
CREATE INDEX IF NOT EXISTS idx_cash_assignment_register ON cash_register_assignment (register_code);
CREATE INDEX IF NOT EXISTS idx_cash_assignment_user ON cash_register_assignment (lower(user_email));

CREATE TABLE IF NOT EXISTS cash_register_history
(
    id            BIGINT PRIMARY KEY,
    register_code VARCHAR(100) NOT NULL,
    field         VARCHAR(80)  NOT NULL,
    old_value     TEXT,
    new_value     TEXT,
    changed_by    VARCHAR(180),
    changed_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    effective_at  TIMESTAMPTZ,
    reason        VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_cash_register_history_register ON cash_register_history (register_code, changed_at DESC);

CREATE TABLE IF NOT EXISTS cash_register_schedule
(
    id            BIGINT PRIMARY KEY,
    register_code VARCHAR(100) NOT NULL,
    day_of_week   INTEGER      NOT NULL,
    opens_at      TIME         NOT NULL,
    closes_at     TIME         NOT NULL,
    CONSTRAINT ck_cash_schedule_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_cash_schedule_hours CHECK (closes_at > opens_at)
);
CREATE INDEX IF NOT EXISTS idx_cash_schedule_register ON cash_register_schedule (register_code, day_of_week);

CREATE TABLE IF NOT EXISTS cash_register_device
(
    id             BIGINT PRIMARY KEY,
    register_code  VARCHAR(100) NOT NULL,
    device_code    VARCHAR(120) NOT NULL,
    label          VARCHAR(255),
    status         VARCHAR(20)  NOT NULL,
    first_seen_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    first_seen_by  VARCHAR(180),
    decided_at     TIMESTAMPTZ,
    decided_by     VARCHAR(180),
    CONSTRAINT ck_cash_device_status CHECK (status IN ('PROPOSED', 'ALLOWED', 'REFUSED')),
    CONSTRAINT uq_cash_register_device UNIQUE (register_code, device_code)
);

CREATE TABLE IF NOT EXISTS cash_responsibility_transfer
(
    id                 BIGINT PRIMARY KEY,
    transfer_number    VARCHAR(100) NOT NULL UNIQUE,
    register_code      VARCHAR(100) NOT NULL,
    from_user          VARCHAR(180),
    to_user            VARCHAR(180) NOT NULL,
    reason_code        VARCHAR(60)  NOT NULL,
    reason_text        VARCHAR(500),
    permanent          BOOLEAN      NOT NULL,
    effective_at       TIMESTAMPTZ  NOT NULL,
    ends_at            TIMESTAMPTZ,
    counted_amount     NUMERIC(19, 4),
    expected_amount    NUMERIC(19, 4),
    witness            VARCHAR(180),
    pending_items      JSONB,
    status             VARCHAR(30)  NOT NULL,
    outgoing_signed_by VARCHAR(180),
    outgoing_signed_at TIMESTAMPTZ,
    incoming_signed_by VARCHAR(180),
    incoming_signed_at TIMESTAMPTZ,
    created_by         VARCHAR(180) NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_cash_transfer_status CHECK (status IN ('PENDING_SIGNATURES', 'SIGNED', 'CANCELLED')),
    CONSTRAINT ck_cash_transfer_end CHECK (permanent OR ends_at IS NOT NULL)
);
CREATE INDEX IF NOT EXISTS idx_cash_resp_transfer_register ON cash_responsibility_transfer (register_code, effective_at DESC);

CREATE TABLE IF NOT EXISTS cash_reference_value
(
    id            BIGINT PRIMARY KEY,
    list_code     VARCHAR(60)  NOT NULL,
    code          VARCHAR(60)  NOT NULL,
    label         VARCHAR(255) NOT NULL,
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    requires_text BOOLEAN      NOT NULL DEFAULT FALSE,
    system_value  BOOLEAN      NOT NULL DEFAULT FALSE,
    usage_count   BIGINT       NOT NULL DEFAULT 0,
    updated_by    VARCHAR(180),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_cash_reference_value UNIQUE (list_code, code)
);

-- Listes initiales (section 21.3 du plan des caisses). Chaque liste a une valeur OTHER
-- (« Autre, preciser ») qui exige un texte et se desactive liste par liste.
WITH seed (list_code, code, label, sort_order, requires_text) AS (
    VALUES
    ('VARIANCE_REASON', 'CHANGE_ERROR', 'Erreur de rendu de monnaie', 10, FALSE),
    ('VARIANCE_REASON', 'NOTE_NOT_COUNTED', 'Billet non compte', 20, FALSE),
    ('VARIANCE_REASON', 'RECEIPT_NOT_ENTERED', 'Encaissement non saisi', 30, FALSE),
    ('VARIANCE_REASON', 'PAYOUT_NOT_ENTERED', 'Sortie non saisie', 40, FALSE),
    ('VARIANCE_REASON', 'FAKE_OR_DAMAGED_NOTE', 'Billet faux ou abime', 50, FALSE),
    ('VARIANCE_REASON', 'AMOUNT_ENTRY_ERROR', 'Erreur de saisie de montant', 60, FALSE),
    ('VARIANCE_REASON', 'UNEXPLAINED', 'Inexplique', 70, FALSE),
    ('ADJUSTMENT_REASON', 'VOUCHER_AMOUNT_ERROR', 'Erreur de montant sur un bon', 10, FALSE),
    ('ADJUSTMENT_REASON', 'DUPLICATE', 'Doublon', 20, FALSE),
    ('ADJUSTMENT_REASON', 'FORGOTTEN_MOVEMENT', 'Mouvement oublie', 30, FALSE),
    ('ADJUSTMENT_REASON', 'ACCOUNTING_REQUEST', 'Regularisation demandee par la comptabilite', 40, FALSE),
    ('REVERSAL_REASON', 'ENTRY_ERROR', 'Erreur de saisie', 10, FALSE),
    ('REVERSAL_REASON', 'WRONG_NATURE', 'Mauvaise nature', 20, FALSE),
    ('REVERSAL_REASON', 'WRONG_REGISTER', 'Mauvaise caisse', 30, FALSE),
    ('REVERSAL_REASON', 'NOT_PERFORMED', 'Operation non realisee', 40, FALSE),
    ('REVERSAL_REASON', 'DUPLICATE', 'Doublon', 50, FALSE),
    ('SUSPENSION_REASON', 'CONTROL', 'Controle', 10, FALSE),
    ('SUSPENSION_REASON', 'INCIDENT', 'Incident', 20, FALSE),
    ('SUSPENSION_REASON', 'INVESTIGATION', 'Enquete', 30, FALSE),
    ('SUSPENSION_REASON', 'UNPLANNED_ABSENCE', 'Absence imprevue', 40, FALSE),
    ('SUSPENSION_REASON', 'BREAKDOWN', 'Panne', 50, FALSE),
    ('SESSION_CANCEL_REASON', 'OPENED_BY_MISTAKE', 'Ouverte par erreur', 10, FALSE),
    ('SESSION_CANCEL_REASON', 'WRONG_REGISTER', 'Mauvaise caisse', 20, FALSE),
    ('ABSENCE_REASON', 'ANNUAL_LEAVE', 'Conge annuel', 10, FALSE),
    ('ABSENCE_REASON', 'SICKNESS', 'Maladie', 20, FALSE),
    ('ABSENCE_REASON', 'MISSION', 'Mission', 30, FALSE),
    ('ABSENCE_REASON', 'TRAINING', 'Formation', 40, FALSE),
    ('ABSENCE_REASON', 'SPECIAL_LEAVE', 'Conge exceptionnel', 50, FALSE),
    ('ABSENCE_REASON', 'DEPARTURE', 'Depart', 60, FALSE),
    ('RESPONSIBILITY_TRANSFER_REASON', 'LEAVE', 'Conge', 10, FALSE),
    ('RESPONSIBILITY_TRANSFER_REASON', 'TRANSFER', 'Mutation', 20, FALSE),
    ('RESPONSIBILITY_TRANSFER_REASON', 'DEPARTURE', 'Depart', 30, FALSE),
    ('RESPONSIBILITY_TRANSFER_REASON', 'REORGANISATION', 'Reorganisation', 40, FALSE),
    ('OUT_OF_HOURS_REASON', 'EVENT', 'Evenement', 10, FALSE),
    ('OUT_OF_HOURS_REASON', 'INVENTORY', 'Inventaire', 20, FALSE),
    ('OUT_OF_HOURS_REASON', 'CATCH_UP', 'Rattrapage', 30, FALSE),
    ('EXTERNAL_DOCUMENT_TYPE', 'INVOICE', 'Facture', 10, FALSE),
    ('EXTERNAL_DOCUMENT_TYPE', 'TILL_RECEIPT', 'Ticket de caisse', 20, FALSE),
    ('EXTERNAL_DOCUMENT_TYPE', 'RECEIPT', 'Recu', 30, FALSE),
    ('EXTERNAL_DOCUMENT_TYPE', 'QUITTANCE', 'Quittance', 40, FALSE),
    ('EXTERNAL_DOCUMENT_TYPE', 'EXPENSE_NOTE', 'Note de frais', 50, FALSE),
    ('BANK', 'BGFI', 'BGFI Bank Congo', 10, FALSE),
    ('BANK', 'LCB', 'LCB Bank', 20, FALSE),
    ('BANK', 'ECOBANK', 'Ecobank Congo', 30, FALSE),
    ('BANK', 'UBA', 'UBA Congo', 40, FALSE),
    ('BANK', 'SGC', 'Societe Generale Congo', 50, FALSE),
    ('BANK', 'BCI', 'Banque Commerciale Internationale', 60, FALSE),
    ('BANK', 'CDCO', 'Credit du Congo', 70, FALSE)
), lists AS (
    SELECT DISTINCT list_code FROM seed
), all_values AS (
    SELECT list_code, code, label, sort_order, requires_text FROM seed
    UNION ALL
    SELECT list_code, 'OTHER', 'Autre, preciser', 999, TRUE FROM lists
), numbered AS (
    SELECT v.*, ROW_NUMBER() OVER (ORDER BY v.list_code, v.sort_order, v.code) AS rank
    FROM all_values v
    WHERE NOT EXISTS (SELECT 1 FROM cash_reference_value r WHERE r.list_code = v.list_code AND r.code = v.code)
)
INSERT INTO cash_reference_value (id, list_code, code, label, sort_order, active, requires_text, system_value)
SELECT COALESCE((SELECT MAX(id) FROM cash_reference_value), 2920000) + rank, list_code, code, label, sort_order, TRUE, requires_text, TRUE
FROM numbered;

-- Reprise des affectations : chaque personne qui a ouvert une session sur une caisse ordinaire
-- dans les 90 derniers jours y est affectee comme caissier remplacant. La liste est a revoir
-- avant mise en service ; aucun titulaire n'est designe d'office.
WITH openers AS (
    SELECT DISTINCT cr.register_code,
           COALESCE(u.email, cs.opened_by) AS user_email
    FROM cash_session cs
    JOIN cash_register cr ON cr.id = cs.cash_register_id
    LEFT JOIN users u ON u.user_id = cs.opened_by OR lower(u.email) = lower(cs.opened_by)
    WHERE cr.register_type <> 'AUTOMATIC'
      AND cs.opened_at >= now() - INTERVAL '90 days'
      AND cs.opened_by IS NOT NULL
      AND upper(cs.opened_by) NOT LIKE 'SYSTEM%'
), numbered AS (
    SELECT o.*, ROW_NUMBER() OVER (ORDER BY o.register_code, o.user_email) AS rank
    FROM openers o
    WHERE NOT EXISTS (
        SELECT 1 FROM cash_register_assignment a
        WHERE a.register_code = o.register_code AND lower(a.user_email) = lower(o.user_email)
    )
)
INSERT INTO cash_register_assignment (id, register_code, user_email, role, quality, backup_rank, valid_from, source, granted_by, note)
SELECT COALESCE((SELECT MAX(id) FROM cash_register_assignment), 2920000) + rank, register_code, user_email,
       'CASHIER', 'BACKUP', NULL, now(), 'MIGRATION', 'SYSTEM', 'Reprise : sessions des 90 derniers jours'
FROM numbered;

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2920001, 'cash_responsibility_transfer', 'Remise de caisse', 'PV de remise de caisse (transfert de responsabilite)', 'PRC', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE lower(code) = 'cash_responsibility_transfer');

INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + 1, 'CASH_STAFF_MANAGE',
       'Gerer les caissiers : affectations, remplacants, transferts de responsabilite', 'CASH_STAFF', 'MANAGE', TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE name = 'CASH_STAFF_MANAGE');

INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE
FROM role r
JOIN permission p ON p.name = 'CASH_STAFF_MANAGE'
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN', 'MANAGER')
  AND NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

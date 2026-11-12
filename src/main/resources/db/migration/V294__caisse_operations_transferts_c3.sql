-- Plan finance, lot C3 : operations de caisse et transferts.
--
-- Natures d'operation (le langage de la caisse, traduit plus tard en comptabilite), ordres de
-- transfert interne a deux temps (envoi, reception comptee, contestation), mouvements en attente
-- de visa, contre-passation, echange de monnaie, petite caisse a fonds fixe.

CREATE TABLE IF NOT EXISTS cash_operation_nature
(
    id                     BIGINT PRIMARY KEY,
    code                   VARCHAR(60)    NOT NULL UNIQUE,
    label                  VARCHAR(255)   NOT NULL,
    family                 VARCHAR(30)    NOT NULL,
    direction              VARCHAR(3)     NOT NULL,
    movement_type          VARCHAR(40)    NOT NULL,
    account_number         VARCHAR(30),
    vat_code               VARCHAR(30),
    document_required      BOOLEAN        NOT NULL DEFAULT FALSE,
    justification_days     INTEGER,
    default_section        VARCHAR(60),
    max_per_operation      NUMERIC(19, 4),
    prior_request_required BOOLEAN        NOT NULL DEFAULT FALSE,
    double_visa_threshold  NUMERIC(19, 4),
    keywords               VARCHAR(500),
    sort_order             INTEGER        NOT NULL DEFAULT 0,
    usage_count            BIGINT         NOT NULL DEFAULT 0,
    active                 BOOLEAN        NOT NULL DEFAULT TRUE,
    system_value           BOOLEAN        NOT NULL DEFAULT FALSE,
    updated_by             VARCHAR(180),
    updated_at             TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_cash_nature_direction CHECK (direction IN ('IN', 'OUT')),
    CONSTRAINT ck_cash_nature_family CHECK (family IN ('ENCAISSEMENTS', 'ACHATS', 'DEPLACEMENTS', 'AVANCES', 'REMISES',
        'TRANSFERTS', 'REMBOURSEMENTS', 'DIVERS'))
);

-- Natures initiales (plan, section 7.2). Les comptes sont des propositions SYSCOHADA, a valider
-- par l'expert-comptable ; vides, ils seront completes au lot F3.
WITH seed (code, label, family, direction, movement_type, account_number, document_required, justification_days,
           max_per_operation, prior_request_required, double_visa_threshold, keywords, sort_order) AS (
    VALUES
    ('PRODUIT_DIVERS', 'Produit divers', 'ENCAISSEMENTS', 'IN', 'CASH_IN', '758', FALSE, NULL, NULL, FALSE, NULL, 'divers recette', 10),
    ('DEPOT_GARANTIE_RECU', 'Depot de garantie recu', 'ENCAISSEMENTS', 'IN', 'CASH_IN', '165', TRUE, NULL, NULL, FALSE, NULL, 'caution contrat', 20),
    ('RETOUR_RELIQUAT_AVANCE', 'Retour de reliquat d''avance', 'AVANCES', 'IN', 'CASH_IN', '421', FALSE, NULL, NULL, FALSE, NULL, 'avance reliquat', 30),
    ('APPORT_ESPECES', 'Apport en especes (associe, emprunt)', 'ENCAISSEMENTS', 'IN', 'CASH_IN', NULL, TRUE, NULL, NULL, FALSE, 0, 'apport associe emprunt', 40),
    ('ACHAT_FOURNITURES_BUREAU', 'Achat de fournitures de bureau', 'ACHATS', 'OUT', 'CASH_OUT', '6055', TRUE, 7, 50000, FALSE, NULL, 'papier stylo cartouche', 100),
    ('ACHAT_PRODUITS_ENTRETIEN', 'Achat de produits d''entretien', 'ACHATS', 'OUT', 'CASH_OUT', '6054', TRUE, 7, 50000, FALSE, NULL, 'menage nettoyage', 110),
    ('ACHAT_PETIT_MATERIEL', 'Achat de petit materiel', 'ACHATS', 'OUT', 'CASH_OUT', '6056', TRUE, 7, 50000, FALSE, NULL, 'outil ampoule cable', 120),
    ('ACHAT_CONSOMMABLES_ESPACES', 'Consommables des espaces (eau, cafe, the)', 'ACHATS', 'OUT', 'CASH_OUT', '605', TRUE, 7, 50000, FALSE, NULL, 'eau cafe the sucre', 130),
    ('TRANSPORT_COURSE', 'Transport, course', 'DEPLACEMENTS', 'OUT', 'CASH_OUT', '618', FALSE, 7, 50000, FALSE, NULL, 'taxi course coursier', 200),
    ('FRAIS_MISSION', 'Frais de mission', 'DEPLACEMENTS', 'OUT', 'CASH_OUT', '6181', TRUE, 7, 50000, FALSE, NULL, 'mission deplacement', 210),
    ('AFFRANCHISSEMENT', 'Affranchissement, envoi de courrier', 'DIVERS', 'OUT', 'CASH_OUT', '6261', TRUE, 7, 50000, FALSE, NULL, 'poste courrier colis', 300),
    ('TELECOMMUNICATIONS', 'Credit de communication', 'DIVERS', 'OUT', 'CASH_OUT', '628', TRUE, 7, 50000, FALSE, NULL, 'credit telephone internet', 310),
    ('PETITES_REPARATIONS', 'Petites reparations', 'ACHATS', 'OUT', 'CASH_OUT', '624', TRUE, 7, 50000, FALSE, NULL, 'reparation plombier electricien', 320),
    ('PAIEMENT_FOURNISSEUR', 'Paiement d''une facture fournisseur', 'ACHATS', 'OUT', 'CASH_OUT', '401', TRUE, NULL, NULL, FALSE, NULL, 'fournisseur facture', 330),
    ('AVANCE_A_JUSTIFIER', 'Avance a justifier', 'AVANCES', 'OUT', 'CASH_OUT', '421', FALSE, 7, NULL, TRUE, NULL, 'avance mission achat', 400),
    ('RESTITUTION_CAUTION', 'Restitution d''un depot de garantie', 'REMBOURSEMENTS', 'OUT', 'CASH_OUT', '165', TRUE, NULL, NULL, FALSE, NULL, 'caution restitution', 500),
    ('DEPENSE_DIVERSE', 'Depense diverse', 'DIVERS', 'OUT', 'CASH_OUT', '658', TRUE, 7, 50000, FALSE, NULL, 'divers', 900)
), numbered AS (
    SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.sort_order) AS rank
    FROM seed s
    WHERE NOT EXISTS (SELECT 1 FROM cash_operation_nature n WHERE n.code = s.code)
)
INSERT INTO cash_operation_nature (id, code, label, family, direction, movement_type, account_number, document_required,
                                   justification_days, max_per_operation, prior_request_required, double_visa_threshold,
                                   keywords, sort_order, system_value)
SELECT COALESCE((SELECT MAX(id) FROM cash_operation_nature), 2940000) + rank, code, label, family, direction, movement_type,
       account_number, document_required, justification_days, max_per_operation, prior_request_required,
       double_visa_threshold, keywords, sort_order, TRUE
FROM numbered;

CREATE TABLE IF NOT EXISTS cash_transfer_order
(
    id                  BIGINT PRIMARY KEY,
    transfer_number     VARCHAR(100)   NOT NULL UNIQUE,
    kind                VARCHAR(30)    NOT NULL,
    from_register       VARCHAR(100)   NOT NULL,
    to_register         VARCHAR(100)   NOT NULL,
    currency            VARCHAR(3)     NOT NULL,
    sent_amount         NUMERIC(19, 4) NOT NULL,
    received_amount     NUMERIC(19, 4),
    status              VARCHAR(20)    NOT NULL,
    carrier             VARCHAR(180),
    sent_by             VARCHAR(180)   NOT NULL,
    sent_at             TIMESTAMPTZ    NOT NULL DEFAULT now(),
    from_session        VARCHAR(100),
    out_movement        VARCHAR(100),
    received_by         VARCHAR(180),
    received_at         TIMESTAMPTZ,
    to_session          VARCHAR(100),
    in_movement         VARCHAR(100),
    receipt_count       VARCHAR(100),
    review_number       VARCHAR(100),
    cancelled_by        VARCHAR(180),
    cancelled_at        TIMESTAMPTZ,
    cancel_reason       VARCHAR(500),
    transit_alerted_at  TIMESTAMPTZ,
    note                VARCHAR(1000),
    CONSTRAINT ck_cash_transfer_status CHECK (status IN ('IN_TRANSIT', 'RECEIVED', 'DISPUTED', 'CANCELLED')),
    CONSTRAINT ck_cash_transfer_kind CHECK (kind IN ('FEED', 'REMITTANCE', 'SAFE_DEPOSIT', 'SAFE_WITHDRAWAL', 'PETTY_REFILL', 'OTHER')),
    CONSTRAINT ck_cash_transfer_amount CHECK (sent_amount > 0),
    CONSTRAINT ck_cash_transfer_registers CHECK (from_register <> to_register)
);
CREATE INDEX IF NOT EXISTS idx_cash_transfer_status ON cash_transfer_order (status, sent_at);
CREATE INDEX IF NOT EXISTS idx_cash_transfer_to ON cash_transfer_order (to_register, status);
CREATE INDEX IF NOT EXISTS idx_cash_transfer_from ON cash_transfer_order (from_register, status);

ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS nature_code VARCHAR(60);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS transfer_order_number VARCHAR(100);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS approved_by VARCHAR(180);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS reversal_reason VARCHAR(500);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(180);
ALTER TABLE cash_movement ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500);
CREATE INDEX IF NOT EXISTS idx_cash_movement_status_expiry ON cash_movement (status, expires_at);
CREATE INDEX IF NOT EXISTS idx_cash_movement_transfer ON cash_movement (transfer_order_number);

ALTER TABLE cash_movement DROP CONSTRAINT IF EXISTS ck_cash_movement_direction;
ALTER TABLE cash_movement ADD CONSTRAINT ck_cash_movement_direction CHECK (
    (direction IS NULL OR direction IN ('IN', 'OUT'))
    AND (movement_type NOT IN ('ADJUSTMENT', 'VARIANCE') OR direction IS NOT NULL)
);

ALTER TABLE cash_register ADD COLUMN IF NOT EXISTS double_visa_threshold NUMERIC(19, 4);
UPDATE cash_register SET double_visa_threshold = 50000 WHERE double_visa_threshold IS NULL AND register_type <> 'AUTOMATIC';

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2940001, 'cash_transfer_order', 'Ordre de transfert interne', 'Ordres de transfert interne entre caisses', 'OTI', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE lower(code) = 'cash_transfer_order');

WITH new_permission (name, display_name, module, action) AS (
    VALUES ('CASH_TRANSFER_SEND', 'Envoyer un transfert interne de caisse', 'CASH_TRANSFER', 'SEND'),
           ('CASH_TRANSFER_RECEIVE', 'Recevoir un transfert interne de caisse', 'CASH_TRANSFER', 'RECEIVE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank
    FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE
FROM numbered;

-- Reprise : ADJUST donnait aussi l'envoi et la reception des transferts (plan des caisses, section 17).
WITH grants (permission_name) AS (VALUES ('CASH_TRANSFER_SEND'), ('CASH_TRANSFER_RECEIVE')),
holders AS (
    SELECT DISTINCT g.permission_name, rp.role_id
    FROM grants g
    JOIN permission old ON old.name = 'CASH_ADJUST'
    JOIN role_permission rp ON rp.permission_id = old.id AND rp.is_active = TRUE
    UNION
    SELECT g.permission_name, r.id FROM grants g JOIN role r ON r.name IN ('SUPER_ADMIN', 'ADMIN')
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT h.role_id, p.id, 'SYSTEM', TRUE
FROM holders h
JOIN permission p ON p.name = h.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = h.role_id AND rp.permission_id = p.id);

-- Lot F18 : immobilisations et amortissements. Categories et comptes SYSCOHADA, fiches
-- d'immobilisation, lignes de valeur, plans d'amortissement versionnes (comptable et fiscal) et
-- leurs echeanciers mensuels, evenements de la vie du bien, sorties, depreciations, inventaire
-- physique, entretien, releves d'unites d'oeuvre, liens avec l'inventaire ; facture fournisseur
-- d'immobilisation au compte 481 ; droits.

-- ------------------------------------------------------------------ comptes

WITH seed (id, number, label, parent_number, nature, normal_side, reconcilable, analytic_required) AS (
    VALUES (3120001, '2811', 'Amortissements des frais de developpement', '281', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3120002, '2813', 'Amortissements des logiciels et sites internet', '281', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3120003, '2831', 'Amortissements des batiments sur sol propre', '283', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3120004, '2832', 'Amortissements des batiments sur sol d''autrui', '283', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3120005, '2834', 'Amortissements des amenagements, agencements et installations techniques', '283', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3120006, '2835', 'Amortissements des amenagements de bureaux', '283', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3120007, '2841', 'Amortissements du materiel et outillage', '284', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3120008, '28441', 'Amortissements du materiel de bureau', '2844', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3120009, '28442', 'Amortissements du materiel informatique', '2844', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3120010, '28444', 'Amortissements du mobilier de bureau', '2844', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3120011, '291', 'Depreciations des immobilisations incorporelles', '29', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3120012, '293', 'Depreciations des batiments, installations et agencements', '29', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3120013, '294', 'Depreciations du materiel, du mobilier et du materiel de transport', '29', 'BALANCE', 'CREDIT', FALSE, FALSE),
           (3120014, '6913', 'Dotations aux depreciations des immobilisations incorporelles', '691', 'MANAGEMENT', 'DEBIT', FALSE, TRUE),
           (3120015, '6914', 'Dotations aux depreciations des immobilisations corporelles', '691', 'MANAGEMENT', 'DEBIT', FALSE, TRUE),
           (3120016, '7913', 'Reprises de depreciations des immobilisations incorporelles', '791', 'MANAGEMENT', 'CREDIT', FALSE, TRUE),
           (3120017, '7914', 'Reprises de depreciations des immobilisations corporelles', '791', 'MANAGEMENT', 'CREDIT', FALSE, TRUE),
           (3120018, '721', 'Production immobilisee, immobilisations incorporelles', '72', 'MANAGEMENT', 'CREDIT', FALSE, TRUE),
           (3120019, '722', 'Production immobilisee, immobilisations corporelles', '72', 'MANAGEMENT', 'CREDIT', FALSE, TRUE),
           (3120020, '841', 'Produits hors activites ordinaires constates', '84', 'HAO', 'CREDIT', FALSE, FALSE)
)
INSERT INTO acc_account (id, number, label, parent_number, class_code, nature, normal_side, reconcilable, third_party_type,
                         treasury_type, analytic_required, statement_line_code, status, created_by)
SELECT s.id, s.number, s.label, s.parent_number, substr(s.number, 1, 1), s.nature, s.normal_side, s.reconcilable, NULL, NULL,
       s.analytic_required, 'CLASSE_' || substr(s.number, 1, 1), 'ACTIVE', 'SYSTEM'
FROM seed s
WHERE NOT EXISTS (SELECT 1 FROM acc_account a WHERE a.number = s.number);

-- ------------------------------------------------------------------ factures fournisseurs

-- Compte du fournisseur credite par la facture (481 pour une facture d'immobilisation) ; vide : celui du fournisseur.
ALTER TABLE acc_supplier_invoice ADD COLUMN IF NOT EXISTS payable_account VARCHAR(20);
-- Quantite de la ligne et fiche d'immobilisation a laquelle elle se rattache (frais accessoires, avoir, en cours).
ALTER TABLE acc_supplier_invoice_line ADD COLUMN IF NOT EXISTS quantity NUMERIC(19, 4);
ALTER TABLE acc_supplier_invoice_line ADD COLUMN IF NOT EXISTS fixed_asset_code VARCHAR(40);

-- ------------------------------------------------------------------ categories

CREATE TABLE IF NOT EXISTS fa_category
(
    code                        VARCHAR(40) PRIMARY KEY,
    label                       VARCHAR(200)   NOT NULL,
    asset_account               VARCHAR(20)    NOT NULL,
    depreciation_account        VARCHAR(20),
    expense_account             VARCHAR(20),
    impairment_account          VARCHAR(20),
    impairment_expense_account  VARCHAR(20),
    impairment_reversal_account VARCHAR(20),
    disposal_value_account      VARCHAR(20)    NOT NULL DEFAULT '812',
    disposal_income_account     VARCHAR(20)    NOT NULL DEFAULT '822',
    method                      VARCHAR(12)    NOT NULL DEFAULT 'LINEAR',
    duration_months             INTEGER,
    coefficient                 NUMERIC(5, 2),
    threshold                   NUMERIC(19, 4) NOT NULL DEFAULT 100000,
    tracking                    VARCHAR(10)    NOT NULL DEFAULT 'UNIT',
    track_off_books             BOOLEAN        NOT NULL DEFAULT FALSE,
    tax_method                  VARCHAR(12),
    tax_duration_months         INTEGER,
    component_types             VARCHAR(1000),
    vat_recoverable             BOOLEAN        NOT NULL DEFAULT TRUE,
    in_progress                 BOOLEAN        NOT NULL DEFAULT FALSE,
    active                      BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_fa_category_method CHECK (method IN ('LINEAR', 'DECLINING', 'UNITS', 'NONE')),
    CONSTRAINT ck_fa_category_tax_method CHECK (tax_method IS NULL OR tax_method IN ('LINEAR', 'DECLINING')),
    CONSTRAINT ck_fa_category_tracking CHECK (tracking IN ('UNIT', 'LOT')),
    CONSTRAINT ck_fa_category_duration CHECK (method = 'NONE' OR duration_months > 0)
);

INSERT INTO fa_category (code, label, asset_account, depreciation_account, expense_account, impairment_account, impairment_expense_account,
                         impairment_reversal_account, disposal_value_account, disposal_income_account, method, duration_months, tracking,
                         track_off_books, component_types, in_progress)
VALUES ('LOGICIEL', 'Logiciels, licences acquises, site internet', '213', '2813', '6812', '291', '6913', '7913', '811', '821', 'LINEAR', 36, 'UNIT', FALSE, NULL, FALSE),
       ('DEVELOPPEMENT', 'Frais de developpement immobilises (plateforme)', '211', '2811', '6812', '291', '6913', '7913', '811', '821', 'LINEAR', 60, 'UNIT', FALSE, NULL, FALSE),
       ('AMENAGEMENT', 'Amenagements, agencements et installations des espaces', '235', '2835', '6813', '293', '6914', '7914', '812', '822', 'LINEAR', 120, 'UNIT', FALSE,
        'Cloisons,Electricite,Climatisation,Revetements', FALSE),
       ('MATERIEL_BUREAU', 'Materiel de bureau (imprimantes, photocopieurs, destructeurs)', '2441', '28441', '6813', '294', '6914', '7914', '812', '822', 'LINEAR', 60, 'UNIT', FALSE, NULL, FALSE),
       ('MATERIEL_INFORMATIQUE', 'Materiel informatique (ordinateurs, serveurs, bornes, reseau, Wi-Fi)', '2442', '28442', '6813', '294', '6914', '7914', '812', '822', 'LINEAR', 36, 'UNIT', TRUE, NULL, FALSE),
       ('AUDIOVISUEL', 'Materiel audiovisuel des salles (ecrans, videoprojecteurs, visioconference)', '2441', '28441', '6813', '294', '6914', '7914', '812', '822', 'LINEAR', 60, 'UNIT', FALSE, NULL, FALSE),
       ('MOBILIER', 'Mobilier de bureau et des espaces detente', '2444', '28444', '6813', '294', '6914', '7914', '812', '822', 'LINEAR', 120, 'LOT', TRUE, NULL, FALSE),
       ('OUTILLAGE', 'Materiel et outillage (cuisine, electromenager, entretien)', '241', '2841', '6813', '294', '6914', '7914', '812', '822', 'LINEAR', 60, 'UNIT', FALSE, NULL, FALSE),
       ('TRANSPORT', 'Materiel de transport', '245', '2845', '6813', '294', '6914', '7914', '812', '822', 'LINEAR', 48, 'UNIT', FALSE, NULL, FALSE),
       ('BATIMENT', 'Batiments (si l''exploitant devient proprietaire)', '231', '2831', '6813', '293', '6914', '7914', '812', '822', 'LINEAR', 240, 'UNIT', FALSE, NULL, FALSE),
       ('TERRAIN', 'Terrains', '22', NULL, NULL, NULL, NULL, NULL, '812', '822', 'NONE', NULL, 'UNIT', FALSE, NULL, FALSE),
       ('DEPOT', 'Depots et cautionnements verses (caution du bail, compteurs)', '275', NULL, NULL, NULL, NULL, NULL, '812', '822', 'NONE', NULL, 'UNIT', FALSE, NULL, FALSE),
       ('AVANCE', 'Avances et acomptes verses sur immobilisations corporelles', '252', NULL, NULL, NULL, NULL, NULL, '812', '822', 'NONE', NULL, 'UNIT', FALSE, NULL, FALSE),
       ('EN_COURS', 'Immobilisations en cours (amenagement en chantier)', '239', NULL, NULL, NULL, NULL, NULL, '812', '822', 'NONE', NULL, 'UNIT', FALSE, NULL, TRUE)
ON CONFLICT (code) DO NOTHING;

-- ------------------------------------------------------------------ fiches

CREATE TABLE IF NOT EXISTS fixed_asset_counter
(
    year        INTEGER PRIMARY KEY,
    last_number INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS fixed_asset
(
    id                       BIGINT PRIMARY KEY,
    code                     VARCHAR(40)    NOT NULL UNIQUE,
    company_code             VARCHAR(40)    NOT NULL DEFAULT 'MAIN',
    parent_id                BIGINT REFERENCES fixed_asset (id),
    category_code            VARCHAR(40)    NOT NULL REFERENCES fa_category (code),
    target_category_code     VARCHAR(40),
    designation              VARCHAR(300)   NOT NULL,
    brand                    VARCHAR(120),
    model                    VARCHAR(120),
    serial_number            VARCHAR(120),
    quantity                 INTEGER        NOT NULL DEFAULT 1,
    entry_mode               VARCHAR(20)    NOT NULL DEFAULT 'PURCHASE',
    supplier_code            VARCHAR(40),
    supplier_name            VARCHAR(200),
    supplier_invoice_number  VARCHAR(40),
    supplier_invoice_line_no INTEGER,
    order_number             VARCHAR(40),
    fund_request_ref         VARCHAR(60),
    justification_ref        VARCHAR(60),
    entry_number             VARCHAR(60),
    booked                   BOOLEAN        NOT NULL DEFAULT FALSE,
    tracked_off_books        BOOLEAN        NOT NULL DEFAULT FALSE,
    acquisition_date         DATE,
    received_date            DATE,
    in_service_date          DATE,
    entry_value              NUMERIC(19, 4) NOT NULL DEFAULT 0,
    residual_value           NUMERIC(19, 4) NOT NULL DEFAULT 0,
    deducted_vat             NUMERIC(19, 4) NOT NULL DEFAULT 0,
    accounting_status        VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    operational_status       VARCHAR(12)    NOT NULL DEFAULT 'IN_USE',
    operational_since        DATE,
    method                   VARCHAR(12),
    duration_months          INTEGER,
    coefficient              NUMERIC(5, 2),
    total_units              NUMERIC(19, 4),
    tax_method               VARCHAR(12),
    tax_duration_months      INTEGER,
    plan_version             INTEGER        NOT NULL DEFAULT 0,
    accumulated_depreciation NUMERIC(19, 4) NOT NULL DEFAULT 0,
    accumulated_impairment   NUMERIC(19, 4) NOT NULL DEFAULT 0,
    accumulated_derogatory   NUMERIC(19, 4) NOT NULL DEFAULT 0,
    subsidy_amount           NUMERIC(19, 4) NOT NULL DEFAULT 0,
    subsidy_account          VARCHAR(20),
    subsidy_released         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    organisation             VARCHAR(120),
    site                     VARCHAR(120),
    resource_code            VARCHAR(80),
    amenity_link_id          BIGINT,
    location_label           VARCHAR(300),
    holder                   VARCHAR(180),
    holder_since             DATE,
    analytic_sections        VARCHAR(500),
    warranty_end             DATE,
    maintenance_contract     VARCHAR(200),
    maintenance_end          DATE,
    insurer                  VARCHAR(200),
    policy_number            VARCHAR(120),
    insured_value            NUMERIC(19, 4),
    document_refs            VARCHAR(1000),
    photo_ref                VARCHAR(300),
    qr_active                BOOLEAN        NOT NULL DEFAULT TRUE,
    label_print_count        INTEGER        NOT NULL DEFAULT 0,
    notes                    VARCHAR(1000),
    disposed_on              DATE,
    created_by               VARCHAR(180)   NOT NULL,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_fixed_asset_status CHECK (accounting_status IN ('DRAFT', 'IN_PROGRESS', 'IN_SERVICE', 'FULLY_DEPRECIATED', 'DISPOSED', 'CANCELLED')),
    CONSTRAINT ck_fixed_asset_operational CHECK (operational_status IN ('IN_USE', 'BROKEN', 'IN_REPAIR', 'UNUSED')),
    CONSTRAINT ck_fixed_asset_entry_mode CHECK (entry_mode IN ('PURCHASE', 'IN_PROGRESS', 'PRODUCTION', 'CONTRIBUTION', 'LEASE', 'DONATION', 'ADVANCE', 'REPLAY')),
    CONSTRAINT ck_fixed_asset_method CHECK (method IS NULL OR method IN ('LINEAR', 'DECLINING', 'UNITS', 'NONE')),
    CONSTRAINT ck_fixed_asset_quantity CHECK (quantity > 0),
    CONSTRAINT ck_fixed_asset_values CHECK (entry_value >= 0 AND residual_value >= 0)
);
CREATE INDEX IF NOT EXISTS ix_fixed_asset_status ON fixed_asset (accounting_status);
CREATE INDEX IF NOT EXISTS ix_fixed_asset_category ON fixed_asset (category_code);
CREATE INDEX IF NOT EXISTS ix_fixed_asset_invoice ON fixed_asset (supplier_invoice_number);
CREATE INDEX IF NOT EXISTS ix_fixed_asset_parent ON fixed_asset (parent_id);

CREATE TABLE IF NOT EXISTS fixed_asset_value_line
(
    id           BIGINT PRIMARY KEY,
    asset_id     BIGINT         NOT NULL REFERENCES fixed_asset (id),
    line_type    VARCHAR(24)    NOT NULL,
    amount       NUMERIC(19, 4) NOT NULL,
    value_date   DATE           NOT NULL,
    origin       VARCHAR(120),
    entry_number VARCHAR(60),
    created_by   VARCHAR(180)   NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_fixed_asset_value_line_type CHECK (line_type IN ('PURCHASE', 'ACCESSORY_COST', 'NON_RECOVERABLE_VAT', 'CREDIT_NOTE', 'SUBSEQUENT_EXPENSE',
                                                                   'COMPONENT_SPLIT', 'OPENING'))
);
CREATE INDEX IF NOT EXISTS ix_fixed_asset_value_line_asset ON fixed_asset_value_line (asset_id);

CREATE TABLE IF NOT EXISTS fixed_asset_inventory_link
(
    id                   BIGINT PRIMARY KEY,
    fixed_asset_id       BIGINT       NOT NULL REFERENCES fixed_asset (id),
    inventory_asset_code VARCHAR(80)  NOT NULL,
    created_by           VARCHAR(180) NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_fixed_asset_inventory_link UNIQUE (fixed_asset_id, inventory_asset_code)
);

-- ------------------------------------------------------------------ plans et echeanciers

CREATE TABLE IF NOT EXISTS fixed_asset_plan
(
    id              BIGINT PRIMARY KEY,
    asset_id        BIGINT         NOT NULL REFERENCES fixed_asset (id),
    version         INTEGER        NOT NULL,
    kind            VARCHAR(12)    NOT NULL,
    method          VARCHAR(12)    NOT NULL,
    duration_months INTEGER        NOT NULL,
    rate            NUMERIC(9, 6),
    coefficient     NUMERIC(5, 2),
    base            NUMERIC(19, 4) NOT NULL,
    valid_from      DATE           NOT NULL,
    reason          VARCHAR(500),
    created_by      VARCHAR(180)   NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_fixed_asset_plan UNIQUE (asset_id, version, kind),
    CONSTRAINT ck_fixed_asset_plan_kind CHECK (kind IN ('ACCOUNTING', 'TAX'))
);

CREATE TABLE IF NOT EXISTS fixed_asset_schedule
(
    id               BIGINT PRIMARY KEY,
    plan_id          BIGINT         NOT NULL REFERENCES fixed_asset_plan (id),
    asset_id         BIGINT         NOT NULL REFERENCES fixed_asset (id),
    kind             VARCHAR(12)    NOT NULL,
    version          INTEGER        NOT NULL,
    period_code      VARCHAR(7)     NOT NULL,
    planned          NUMERIC(19, 4) NOT NULL,
    posted           NUMERIC(19, 4) NOT NULL DEFAULT 0,
    cumulative       NUMERIC(19, 4) NOT NULL,
    net_value        NUMERIC(19, 4) NOT NULL,
    status           VARCHAR(16)    NOT NULL DEFAULT 'PLANNED',
    entry_number     VARCHAR(60),
    posted_in_period VARCHAR(7),
    note             VARCHAR(300),
    posted_at        TIMESTAMPTZ,
    CONSTRAINT uq_fixed_asset_schedule UNIQUE (asset_id, kind, version, period_code),
    CONSTRAINT ck_fixed_asset_schedule_status CHECK (status IN ('PLANNED', 'POSTED', 'SKIPPED_LOCKED', 'CAUGHT_UP', 'CANCELLED'))
);
CREATE INDEX IF NOT EXISTS ix_fixed_asset_schedule_run ON fixed_asset_schedule (status, period_code);

CREATE TABLE IF NOT EXISTS fixed_asset_meter_reading
(
    id          BIGINT PRIMARY KEY,
    asset_id    BIGINT         NOT NULL REFERENCES fixed_asset (id),
    period_code VARCHAR(7)     NOT NULL,
    units       NUMERIC(19, 4) NOT NULL,
    created_by  VARCHAR(180)   NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_fixed_asset_meter_reading UNIQUE (asset_id, period_code)
);

-- ------------------------------------------------------------------ vie du bien

CREATE TABLE IF NOT EXISTS fixed_asset_event
(
    id             BIGINT PRIMARY KEY,
    asset_id       BIGINT         NOT NULL REFERENCES fixed_asset (id),
    kind           VARCHAR(30)    NOT NULL,
    event_date     DATE           NOT NULL,
    before_value   VARCHAR(500),
    after_value    VARCHAR(500),
    reason         VARCHAR(500),
    amount         NUMERIC(19, 4),
    voucher_number VARCHAR(60),
    entry_number   VARCHAR(60),
    created_by     VARCHAR(180)   NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_fixed_asset_event_asset ON fixed_asset_event (asset_id);

CREATE TABLE IF NOT EXISTS fixed_asset_disposal
(
    id                       BIGINT PRIMARY KEY,
    asset_id                 BIGINT         NOT NULL REFERENCES fixed_asset (id),
    voucher_number           VARCHAR(60),
    reason                   VARCHAR(20)    NOT NULL,
    disposal_date            DATE           NOT NULL,
    gross_value              NUMERIC(19, 4) NOT NULL DEFAULT 0,
    accumulated_depreciation NUMERIC(19, 4) NOT NULL DEFAULT 0,
    accumulated_impairment   NUMERIC(19, 4) NOT NULL DEFAULT 0,
    net_value                NUMERIC(19, 4) NOT NULL DEFAULT 0,
    price                    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    vat_rate                 NUMERIC(9, 4)  NOT NULL DEFAULT 0,
    vat_amount               NUMERIC(19, 4) NOT NULL DEFAULT 0,
    gain_loss                NUMERIC(19, 4) NOT NULL DEFAULT 0,
    vat_regularization       NUMERIC(19, 4) NOT NULL DEFAULT 0,
    buyer_name               VARCHAR(200),
    justification_ref        VARCHAR(300),
    remove_amenity           BOOLEAN        NOT NULL DEFAULT FALSE,
    status                   VARCHAR(12)    NOT NULL DEFAULT 'PREPARED',
    complement_entry         VARCHAR(60),
    exit_entry               VARCHAR(60),
    sale_entry               VARCHAR(60),
    collected_amount         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    indemnity_amount         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    indemnity_entry          VARCHAR(60),
    billing_document_id      BIGINT,
    prepared_by              VARCHAR(180)   NOT NULL,
    validated_by             VARCHAR(180),
    validated_at             TIMESTAMPTZ,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_fixed_asset_disposal_reason CHECK (reason IN ('SALE', 'SCRAP', 'THEFT', 'LOSS', 'DISASTER', 'DONATION', 'EXCHANGE', 'COMPONENT_REPLACED',
                                                                'SUPPLIER_RETURN')),
    CONSTRAINT ck_fixed_asset_disposal_status CHECK (status IN ('PREPARED', 'VALIDATED', 'CANCELLED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_fixed_asset_disposal_live ON fixed_asset_disposal (asset_id) WHERE status <> 'CANCELLED';

CREATE TABLE IF NOT EXISTS fixed_asset_impairment
(
    id                BIGINT PRIMARY KEY,
    asset_id          BIGINT         NOT NULL REFERENCES fixed_asset (id),
    kind              VARCHAR(12)    NOT NULL,
    impairment_date   DATE           NOT NULL,
    indicator         VARCHAR(30),
    current_value     NUMERIC(19, 4) NOT NULL,
    amount            NUMERIC(19, 4) NOT NULL,
    justification_ref VARCHAR(300),
    status            VARCHAR(20)    NOT NULL,
    requested_by      VARCHAR(180)   NOT NULL,
    approved_by       VARCHAR(180),
    entry_number      VARCHAR(60),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_fixed_asset_impairment_kind CHECK (kind IN ('IMPAIRMENT', 'REVERSAL')),
    CONSTRAINT ck_fixed_asset_impairment_status CHECK (status IN ('PENDING_APPROVAL', 'POSTED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS fixed_asset_maintenance
(
    id                      BIGINT PRIMARY KEY,
    asset_id                BIGINT         NOT NULL REFERENCES fixed_asset (id),
    maintenance_date        DATE           NOT NULL,
    kind                    VARCHAR(20)    NOT NULL DEFAULT 'MAINTENANCE',
    provider                VARCHAR(200),
    contract_ref            VARCHAR(200),
    cost                    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    under_warranty          BOOLEAN        NOT NULL DEFAULT FALSE,
    supplier_invoice_number VARCHAR(40),
    description             VARCHAR(1000),
    created_by              VARCHAR(180)   NOT NULL,
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_fixed_asset_maintenance_kind CHECK (kind IN ('MAINTENANCE', 'REPAIR', 'INSURANCE'))
);

-- ------------------------------------------------------------------ inventaire physique

CREATE TABLE IF NOT EXISTS fixed_asset_inventory_campaign
(
    id               BIGINT PRIMARY KEY,
    code             VARCHAR(40)  NOT NULL UNIQUE,
    label            VARCHAR(200) NOT NULL,
    site             VARCHAR(120),
    resource_codes   VARCHAR(1000),
    category_codes   VARCHAR(1000),
    team             VARCHAR(2000) NOT NULL,
    start_date       DATE         NOT NULL,
    end_date         DATE,
    status           VARCHAR(10)  NOT NULL DEFAULT 'OPEN',
    expected_count   INTEGER      NOT NULL DEFAULT 0,
    seen_count       INTEGER      NOT NULL DEFAULT 0,
    missing_count    INTEGER      NOT NULL DEFAULT 0,
    found_count      INTEGER      NOT NULL DEFAULT 0,
    mislocated_count INTEGER      NOT NULL DEFAULT 0,
    missing_codes    TEXT,
    mislocated_codes TEXT,
    decisions        VARCHAR(2000),
    witness          VARCHAR(180),
    voucher_number   VARCHAR(60),
    opened_by        VARCHAR(180) NOT NULL,
    closed_by        VARCHAR(180),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    closed_at        TIMESTAMPTZ,
    CONSTRAINT ck_fixed_asset_inventory_campaign_status CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE TABLE IF NOT EXISTS fixed_asset_inventory_scan
(
    id                 BIGINT PRIMARY KEY,
    campaign_id        BIGINT       NOT NULL REFERENCES fixed_asset_inventory_campaign (id),
    asset_id           BIGINT REFERENCES fixed_asset (id),
    asset_code         VARCHAR(40),
    unregistered_label VARCHAR(300),
    site               VARCHAR(120),
    resource_code      VARCHAR(80),
    location_label     VARCHAR(300),
    photo_ref          VARCHAR(300),
    mislocated         BOOLEAN      NOT NULL DEFAULT FALSE,
    holder_alert       BOOLEAN      NOT NULL DEFAULT FALSE,
    scanned_by         VARCHAR(180) NOT NULL,
    scanned_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_fixed_asset_inventory_scan_campaign ON fixed_asset_inventory_scan (campaign_id);

-- ------------------------------------------------------------------ droits

WITH new_permission (name, display_name, module, action) AS (
    VALUES ('FIXED_ASSET_READ', 'Consulter le fichier des immobilisations, les fiches et les etats', 'FIXED_ASSET', 'READ'),
           ('FIXED_ASSET_MANAGE', 'Completer les fiches, mettre en service, transferer, affecter, entretien', 'FIXED_ASSET', 'MANAGE'),
           ('FIXED_ASSET_REVISE', 'Reviser un plan d''amortissement, reclasser une immobilisation', 'FIXED_ASSET', 'REVISE'),
           ('FIXED_ASSET_IMPAIR', 'Deprecier une immobilisation et reprendre la depreciation', 'FIXED_ASSET', 'IMPAIR'),
           ('FIXED_ASSET_DISPOSE', 'Preparer et valider la sortie d''une immobilisation', 'FIXED_ASSET', 'DISPOSE'),
           ('FIXED_ASSET_DEPRECIATION_RUN', 'Lancer les dotations aux amortissements d''une periode', 'FIXED_ASSET', 'DEPRECIATION_RUN'),
           ('FIXED_ASSET_INVENTORY', 'Ouvrir et clore une campagne d''inventaire physique', 'FIXED_ASSET', 'INVENTORY'),
           ('FIXED_ASSET_SETTINGS_MANAGE', 'Gerer les categories d''immobilisations', 'FIXED_ASSET_SETTINGS', 'MANAGE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE FROM numbered;

WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN'), ('FINANCE')) AS roles(r)
    CROSS JOIN (VALUES ('FIXED_ASSET_READ'), ('FIXED_ASSET_MANAGE'), ('FIXED_ASSET_REVISE'), ('FIXED_ASSET_IMPAIR'), ('FIXED_ASSET_DISPOSE'),
                       ('FIXED_ASSET_DEPRECIATION_RUN'), ('FIXED_ASSET_INVENTORY'), ('FIXED_ASSET_SETTINGS_MANAGE')) AS perms(p)
    UNION ALL
    SELECT 'MANAGER', p FROM (VALUES ('FIXED_ASSET_READ'), ('FIXED_ASSET_MANAGE'), ('FIXED_ASSET_INVENTORY')) AS perms(p)
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE FROM grants g JOIN role r ON r.name = g.role_name JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- Lot F15 : liasse fiscale (DSF) pre-remplie et passage au resultat fiscal. Taux de non-deductibilite
-- par compte, tableau de determination du resultat fiscal, resultat fiscal arrete par exercice avec
-- le report des deficits, modele de liasse versionne (liste des feuillets).

ALTER TABLE acc_account ADD COLUMN IF NOT EXISTS non_deductible_rate NUMERIC(5, 2);

CREATE TABLE IF NOT EXISTS acc_tax_adjustment
(
    id               BIGINT PRIMARY KEY,
    fiscal_year_code VARCHAR(10)    NOT NULL,
    kind             VARCHAR(15)    NOT NULL,
    code             VARCHAR(40),
    label            VARCHAR(300)   NOT NULL,
    account_number   VARCHAR(20),
    amount           NUMERIC(19, 4) NOT NULL,
    comment          VARCHAR(1000),
    created_by       VARCHAR(180)   NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_acc_tax_adjustment_kind CHECK (kind IN ('REINTEGRATION', 'DEDUCTION'))
);
CREATE INDEX IF NOT EXISTS ix_acc_tax_adjustment_year ON acc_tax_adjustment (fiscal_year_code);

CREATE TABLE IF NOT EXISTS acc_tax_result
(
    id                   BIGINT PRIMARY KEY,
    fiscal_year_code     VARCHAR(10)    NOT NULL UNIQUE,
    accounting_result    NUMERIC(19, 4) NOT NULL,
    reintegrations       NUMERIC(19, 4) NOT NULL,
    deductions           NUMERIC(19, 4) NOT NULL,
    losses_used          NUMERIC(19, 4) NOT NULL DEFAULT 0,
    tax_result           NUMERIC(19, 4) NOT NULL,
    loss_carried_forward NUMERIC(19, 4) NOT NULL DEFAULT 0,
    rate                 NUMERIC(7, 4)  NOT NULL,
    tax                  NUMERIC(19, 4) NOT NULL,
    minimum_tax          NUMERIC(19, 4) NOT NULL DEFAULT 0,
    tax_due              NUMERIC(19, 4) NOT NULL,
    entry_number         VARCHAR(40),
    status               VARCHAR(10)    NOT NULL DEFAULT 'DRAFT',
    computed_by          VARCHAR(180),
    computed_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_acc_tax_result_status CHECK (status IN ('DRAFT', 'POSTED'))
);

CREATE TABLE IF NOT EXISTS acc_liasse_sheet
(
    id         BIGINT PRIMARY KEY,
    version    INTEGER      NOT NULL DEFAULT 1,
    code       VARCHAR(20)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    kind       VARCHAR(20)  NOT NULL,
    source     VARCHAR(40),
    position   INTEGER      NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_acc_liasse_sheet UNIQUE (version, code),
    CONSTRAINT ck_acc_liasse_sheet_kind CHECK (kind IN ('IDENTIFICATION', 'MANAGERS', 'WORKFORCE', 'STATEMENT', 'NOTES', 'TAX'))
);

INSERT INTO acc_liasse_sheet (id, version, code, title, kind, source, position)
SELECT id, 1, code, title, kind, source, position
FROM (VALUES (3190001, 'IDENTIFICATION', 'Fiche d''identification et renseignements divers', 'IDENTIFICATION', NULL, 10),
             (3190002, 'DIRIGEANTS', 'Dirigeants et commissaire aux comptes', 'MANAGERS', NULL, 20),
             (3190003, 'EFFECTIFS', 'Effectifs et masse salariale', 'WORKFORCE', NULL, 30),
             (3190004, 'BILAN', 'Bilan', 'STATEMENT', 'BILAN', 40),
             (3190005, 'COMPTE_RESULTAT', 'Compte de resultat', 'STATEMENT', 'COMPTE_RESULTAT', 50),
             (3190006, 'TFT', 'Tableau des flux de tresorerie', 'STATEMENT', 'TFT', 60),
             (3190007, 'NOTES', 'Notes annexes', 'NOTES', NULL, 70),
             (3190008, 'RESULTAT_FISCAL', 'Tableau de passage du resultat comptable au resultat fiscal', 'TAX', NULL, 80))
         AS s(id, code, title, kind, source, position)
WHERE NOT EXISTS (SELECT 1 FROM acc_liasse_sheet l WHERE l.version = 1 AND l.code = s.code);

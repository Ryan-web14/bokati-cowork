-- Lot F12 : cloture d'exercice et etats financiers SYSCOHADA. Modele d'etats parametrable et
-- versionne (postes, correspondance comptes vers postes), liste de cloture, a-nouveaux detailles
-- avec suite du lettrage, editions definitives figees, notes annexes redigees, justificatifs de
-- solde et confirmations de solde ; droits.

-- ------------------------------------------------------------------ exercice et lettrage

ALTER TABLE acc_fiscal_year ADD COLUMN IF NOT EXISTS net_result NUMERIC(19, 4);
ALTER TABLE acc_fiscal_year ADD COLUMN IF NOT EXISTS opening_entry_number VARCHAR(40);
ALTER TABLE acc_fiscal_year ADD COLUMN IF NOT EXISTS closing_derogation TEXT;

ALTER TABLE acc_entry_line ADD COLUMN IF NOT EXISTS carried_from_line_id BIGINT;
CREATE INDEX IF NOT EXISTS ix_acc_entry_line_carried ON acc_entry_line (carried_from_line_id);

ALTER TABLE acc_lettering DROP CONSTRAINT IF EXISTS ck_acc_lettering_kind;
ALTER TABLE acc_lettering ADD CONSTRAINT ck_acc_lettering_kind CHECK (kind IN ('AUTO_REFERENCE', 'AUTO_MATCH', 'MANUAL', 'CARRY_FORWARD'));

-- ------------------------------------------------------------------ modele d'etats

CREATE TABLE IF NOT EXISTS acc_statement_template
(
    id             BIGINT PRIMARY KEY,
    code           VARCHAR(40)  NOT NULL,
    kind           VARCHAR(30)  NOT NULL,
    version        INTEGER      NOT NULL DEFAULT 1,
    label          VARCHAR(200) NOT NULL,
    effective_from DATE         NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by     VARCHAR(180),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_acc_statement_template UNIQUE (code, version),
    CONSTRAINT ck_acc_statement_template_kind CHECK (kind IN ('BALANCE_SHEET', 'INCOME_STATEMENT', 'CASH_FLOW'))
);

CREATE TABLE IF NOT EXISTS acc_statement_line
(
    id          BIGINT PRIMARY KEY,
    template_id BIGINT       NOT NULL REFERENCES acc_statement_template (id),
    code        VARCHAR(10)  NOT NULL,
    label       VARCHAR(250) NOT NULL,
    position    INTEGER      NOT NULL,
    level       INTEGER      NOT NULL DEFAULT 1,
    line_type   VARCHAR(10)  NOT NULL DEFAULT 'DETAIL',
    section     VARCHAR(12)  NOT NULL,
    formula     VARCHAR(500),
    note_ref    VARCHAR(20),
    CONSTRAINT uk_acc_statement_line UNIQUE (template_id, code),
    CONSTRAINT ck_acc_statement_line_type CHECK (line_type IN ('DETAIL', 'TOTAL')),
    CONSTRAINT ck_acc_statement_line_section CHECK (section IN ('ASSET', 'LIABILITY', 'CHARGE', 'PRODUCT', 'RESULT', 'FLOW'))
);

CREATE TABLE IF NOT EXISTS acc_statement_mapping
(
    id             BIGINT PRIMARY KEY,
    template_id    BIGINT      NOT NULL REFERENCES acc_statement_template (id),
    line_code      VARCHAR(10) NOT NULL,
    account_prefix VARCHAR(20) NOT NULL,
    balance_side   VARCHAR(10) NOT NULL DEFAULT 'ANY',
    value_column   VARCHAR(15) NOT NULL DEFAULT 'GROSS',
    CONSTRAINT uk_acc_statement_mapping UNIQUE (template_id, account_prefix, balance_side, value_column),
    CONSTRAINT ck_acc_statement_mapping_side CHECK (balance_side IN ('ANY', 'DEBIT', 'CREDIT')),
    CONSTRAINT ck_acc_statement_mapping_column CHECK (value_column IN ('GROSS', 'DEPRECIATION'))
);

-- ------------------------------------------------------------------ cloture, editions, notes

CREATE TABLE IF NOT EXISTS acc_closing_step
(
    id               BIGINT PRIMARY KEY,
    fiscal_year_code VARCHAR(10)  NOT NULL,
    step_code        VARCHAR(40)  NOT NULL,
    status           VARCHAR(10)  NOT NULL DEFAULT 'TODO',
    detail           TEXT,
    checked_by       VARCHAR(180),
    checked_at       TIMESTAMPTZ,
    comment          VARCHAR(1000),
    CONSTRAINT uk_acc_closing_step UNIQUE (fiscal_year_code, step_code),
    CONSTRAINT ck_acc_closing_step_status CHECK (status IN ('TODO', 'DONE', 'FAILED', 'WAIVED'))
);

CREATE TABLE IF NOT EXISTS acc_generated_report
(
    id               BIGINT PRIMARY KEY,
    number           VARCHAR(60)  NOT NULL UNIQUE,
    report_code      VARCHAR(40)  NOT NULL,
    fiscal_year_code VARCHAR(10)  NOT NULL,
    period_from      DATE         NOT NULL,
    period_to        DATE         NOT NULL,
    template_version INTEGER,
    data_json        TEXT         NOT NULL,
    content_hash     VARCHAR(64)  NOT NULL,
    pdf              BYTEA,
    derogation       TEXT,
    generated_by     VARCHAR(180),
    generated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_acc_generated_report_year ON acc_generated_report (fiscal_year_code, report_code);

CREATE TABLE IF NOT EXISTS acc_note_text
(
    id               BIGINT PRIMARY KEY,
    fiscal_year_code VARCHAR(10)  NOT NULL,
    note_code        VARCHAR(10)  NOT NULL,
    body             TEXT,
    not_applicable   BOOLEAN      NOT NULL DEFAULT FALSE,
    status           VARCHAR(12)  NOT NULL DEFAULT 'DRAFT',
    author           VARCHAR(180),
    validated_by     VARCHAR(180),
    validated_at     TIMESTAMPTZ,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_acc_note_text UNIQUE (fiscal_year_code, note_code),
    CONSTRAINT ck_acc_note_text_status CHECK (status IN ('DRAFT', 'VALIDATED'))
);

-- ------------------------------------------------------------------ justification des soldes, confirmations

CREATE TABLE IF NOT EXISTS balance_justification
(
    id                BIGINT PRIMARY KEY,
    account_number    VARCHAR(20)    NOT NULL,
    third_party_code  VARCHAR(120),
    justified_at      DATE           NOT NULL,
    open_items        TEXT           NOT NULL,
    total_open        NUMERIC(19, 4) NOT NULL,
    book_balance      NUMERIC(19, 4) NOT NULL,
    difference        NUMERIC(19, 4) NOT NULL,
    comment           VARCHAR(1000),
    prepared_by       VARCHAR(180)   NOT NULL,
    prepared_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    reviewed_by       VARCHAR(180),
    reviewed_at       TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_balance_justification_account ON balance_justification (account_number, third_party_code, justified_at);

CREATE TABLE IF NOT EXISTS acc_confirmation_request
(
    id               BIGINT PRIMARY KEY,
    number           VARCHAR(40)    NOT NULL UNIQUE,
    account_number   VARCHAR(20)    NOT NULL,
    third_party_code VARCHAR(120)   NOT NULL,
    balance_date     DATE           NOT NULL,
    balance          NUMERIC(19, 4) NOT NULL,
    open_items       TEXT,
    status           VARCHAR(12)    NOT NULL DEFAULT 'PREPARED',
    sent_at          TIMESTAMPTZ,
    reminded_at      TIMESTAMPTZ,
    reply_channel    VARCHAR(20),
    replied_balance  NUMERIC(19, 4),
    reply_detail     VARCHAR(2000),
    replied_at       TIMESTAMPTZ,
    difference       NUMERIC(19, 4),
    reconciled_by    VARCHAR(180),
    reconciled_at    TIMESTAMPTZ,
    created_by       VARCHAR(180)   NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_acc_confirmation_status CHECK (status IN ('PREPARED', 'SENT', 'REMINDED', 'AGREED', 'DISPUTED', 'RECONCILED'))
);
CREATE INDEX IF NOT EXISTS ix_acc_confirmation_party ON acc_confirmation_request (account_number, third_party_code, balance_date);

-- ------------------------------------------------------------------ modeles SYSCOHADA revise

INSERT INTO acc_statement_template (id, code, kind, version, label, effective_from, created_by)
SELECT id, code, kind, 1, label, DATE '2026-01-01', 'SYSTEM'
FROM (VALUES (3180001, 'BILAN', 'BALANCE_SHEET', 'Bilan (SYSCOHADA revise, systeme normal)'),
             (3180002, 'COMPTE_RESULTAT', 'INCOME_STATEMENT', 'Compte de resultat (SYSCOHADA revise, systeme normal)'),
             (3180003, 'TFT', 'CASH_FLOW', 'Tableau des flux de tresorerie (SYSCOHADA revise)')) AS t(id, code, kind, label)
WHERE NOT EXISTS (SELECT 1 FROM acc_statement_template s WHERE s.code = t.code AND s.version = 1);

WITH seed (template_code, code, label, position, level, line_type, section, formula, note_ref) AS (
    VALUES
    -- Bilan, actif
    ('BILAN', 'AD', 'Immobilisations incorporelles', 10, 1, 'TOTAL', 'ASSET', 'AE+AF+AG+AH', '3'),
    ('BILAN', 'AE', 'Frais de developpement et de prospection', 11, 2, 'DETAIL', 'ASSET', NULL, '3'),
    ('BILAN', 'AF', 'Brevets, licences, logiciels et droits similaires', 12, 2, 'DETAIL', 'ASSET', NULL, '3'),
    ('BILAN', 'AG', 'Fonds commercial et droit au bail', 13, 2, 'DETAIL', 'ASSET', NULL, '3'),
    ('BILAN', 'AH', 'Autres immobilisations incorporelles', 14, 2, 'DETAIL', 'ASSET', NULL, '3'),
    ('BILAN', 'AI', 'Immobilisations corporelles', 20, 1, 'TOTAL', 'ASSET', 'AJ+AK+AL+AM+AN', '3'),
    ('BILAN', 'AJ', 'Terrains', 21, 2, 'DETAIL', 'ASSET', NULL, '3'),
    ('BILAN', 'AK', 'Batiments', 22, 2, 'DETAIL', 'ASSET', NULL, '3'),
    ('BILAN', 'AL', 'Amenagements, agencements et installations', 23, 2, 'DETAIL', 'ASSET', NULL, '3'),
    ('BILAN', 'AM', 'Materiel, mobilier et actifs biologiques', 24, 2, 'DETAIL', 'ASSET', NULL, '3'),
    ('BILAN', 'AN', 'Materiel de transport', 25, 2, 'DETAIL', 'ASSET', NULL, '3'),
    ('BILAN', 'AP', 'Avances et acomptes verses sur immobilisations', 30, 1, 'DETAIL', 'ASSET', NULL, '3'),
    ('BILAN', 'AQ', 'Immobilisations financieres', 40, 1, 'TOTAL', 'ASSET', 'AR+AS', '4'),
    ('BILAN', 'AR', 'Titres de participation', 41, 2, 'DETAIL', 'ASSET', NULL, '4'),
    ('BILAN', 'AS', 'Autres immobilisations financieres', 42, 2, 'DETAIL', 'ASSET', NULL, '4'),
    ('BILAN', 'AZ', 'TOTAL ACTIF IMMOBILISE', 50, 0, 'TOTAL', 'ASSET', 'AD+AI+AP+AQ', NULL),
    ('BILAN', 'BA', 'Actif circulant HAO', 60, 1, 'DETAIL', 'ASSET', NULL, '5'),
    ('BILAN', 'BB', 'Stocks et encours', 61, 1, 'DETAIL', 'ASSET', NULL, '6'),
    ('BILAN', 'BG', 'Creances et emplois assimiles', 62, 1, 'TOTAL', 'ASSET', 'BH+BI+BJ', NULL),
    ('BILAN', 'BH', 'Fournisseurs, avances versees', 63, 2, 'DETAIL', 'ASSET', NULL, '17'),
    ('BILAN', 'BI', 'Clients', 64, 2, 'DETAIL', 'ASSET', NULL, '7'),
    ('BILAN', 'BJ', 'Autres creances', 65, 2, 'DETAIL', 'ASSET', NULL, '8'),
    ('BILAN', 'BK', 'TOTAL ACTIF CIRCULANT', 70, 0, 'TOTAL', 'ASSET', 'BA+BB+BG', NULL),
    ('BILAN', 'BQ', 'Titres de placement', 80, 1, 'DETAIL', 'ASSET', NULL, '9'),
    ('BILAN', 'BR', 'Valeurs a encaisser', 81, 1, 'DETAIL', 'ASSET', NULL, '10'),
    ('BILAN', 'BS', 'Banques, cheques postaux, caisse et assimiles', 82, 1, 'DETAIL', 'ASSET', NULL, '11'),
    ('BILAN', 'BT', 'TOTAL TRESORERIE-ACTIF', 90, 0, 'TOTAL', 'ASSET', 'BQ+BR+BS', NULL),
    ('BILAN', 'BU', 'Ecart de conversion-Actif', 95, 1, 'DETAIL', 'ASSET', NULL, '12'),
    ('BILAN', 'BZ', 'TOTAL GENERAL ACTIF', 99, 0, 'TOTAL', 'ASSET', 'AZ+BK+BT+BU', NULL),
    -- Bilan, passif
    ('BILAN', 'CA', 'Capital', 110, 1, 'DETAIL', 'LIABILITY', NULL, '13'),
    ('BILAN', 'CB', 'Apporteurs capital non appele (-)', 111, 1, 'DETAIL', 'LIABILITY', NULL, '13'),
    ('BILAN', 'CD', 'Primes liees au capital social', 112, 1, 'DETAIL', 'LIABILITY', NULL, '14'),
    ('BILAN', 'CE', 'Ecarts de reevaluation', 113, 1, 'DETAIL', 'LIABILITY', NULL, '3E'),
    ('BILAN', 'CF', 'Reserves indisponibles', 114, 1, 'DETAIL', 'LIABILITY', NULL, '14'),
    ('BILAN', 'CG', 'Reserves libres', 115, 1, 'DETAIL', 'LIABILITY', NULL, '14'),
    ('BILAN', 'CH', 'Report a nouveau (+ ou -)', 116, 1, 'DETAIL', 'LIABILITY', NULL, '14'),
    ('BILAN', 'CJ', 'Resultat net de l''exercice (benefice + ou perte -)', 117, 1, 'DETAIL', 'LIABILITY', '@RESULT', NULL),
    ('BILAN', 'CL', 'Subventions d''investissement', 118, 1, 'DETAIL', 'LIABILITY', NULL, '15A'),
    ('BILAN', 'CM', 'Provisions reglementees', 119, 1, 'DETAIL', 'LIABILITY', NULL, '15A'),
    ('BILAN', 'CP', 'TOTAL CAPITAUX PROPRES ET RESSOURCES ASSIMILEES', 120, 0, 'TOTAL', 'LIABILITY', 'CA+CB+CD+CE+CF+CG+CH+CJ+CL+CM', NULL),
    ('BILAN', 'DA', 'Emprunts et dettes financieres diverses', 130, 1, 'DETAIL', 'LIABILITY', NULL, '16A'),
    ('BILAN', 'DB', 'Dettes de location-acquisition', 131, 1, 'DETAIL', 'LIABILITY', NULL, '16A'),
    ('BILAN', 'DC', 'Provisions pour risques et charges', 132, 1, 'DETAIL', 'LIABILITY', NULL, '16A'),
    ('BILAN', 'DD', 'TOTAL DETTES FINANCIERES ET RESSOURCES ASSIMILEES', 140, 0, 'TOTAL', 'LIABILITY', 'DA+DB+DC', NULL),
    ('BILAN', 'DF', 'TOTAL RESSOURCES STABLES', 145, 0, 'TOTAL', 'LIABILITY', 'CP+DD', NULL),
    ('BILAN', 'DH', 'Dettes circulantes HAO', 150, 1, 'DETAIL', 'LIABILITY', NULL, '5'),
    ('BILAN', 'DI', 'Clients, avances recues', 151, 1, 'DETAIL', 'LIABILITY', NULL, '7'),
    ('BILAN', 'DJ', 'Fournisseurs d''exploitation', 152, 1, 'DETAIL', 'LIABILITY', NULL, '17'),
    ('BILAN', 'DK', 'Dettes fiscales et sociales', 153, 1, 'DETAIL', 'LIABILITY', NULL, '18'),
    ('BILAN', 'DM', 'Autres dettes', 154, 1, 'DETAIL', 'LIABILITY', NULL, '19'),
    ('BILAN', 'DN', 'Provisions pour risques a court terme', 155, 1, 'DETAIL', 'LIABILITY', NULL, '19'),
    ('BILAN', 'DP', 'TOTAL PASSIF CIRCULANT', 160, 0, 'TOTAL', 'LIABILITY', 'DH+DI+DJ+DK+DM+DN', NULL),
    ('BILAN', 'DQ', 'Banques, credits d''escompte', 170, 1, 'DETAIL', 'LIABILITY', NULL, '20'),
    ('BILAN', 'DR', 'Banques, etablissements financiers et credits de tresorerie', 171, 1, 'DETAIL', 'LIABILITY', NULL, '20'),
    ('BILAN', 'DT', 'TOTAL TRESORERIE-PASSIF', 180, 0, 'TOTAL', 'LIABILITY', 'DQ+DR', NULL),
    ('BILAN', 'DV', 'Ecart de conversion-Passif', 185, 1, 'DETAIL', 'LIABILITY', NULL, '12'),
    ('BILAN', 'DZ', 'TOTAL GENERAL PASSIF', 199, 0, 'TOTAL', 'LIABILITY', 'DF+DP+DT+DV', NULL),
    -- Compte de resultat
    ('COMPTE_RESULTAT', 'TA', 'Ventes de marchandises', 10, 1, 'DETAIL', 'PRODUCT', NULL, '21'),
    ('COMPTE_RESULTAT', 'RA', 'Achats de marchandises', 11, 1, 'DETAIL', 'CHARGE', NULL, '22'),
    ('COMPTE_RESULTAT', 'RB', 'Variation de stocks de marchandises', 12, 1, 'DETAIL', 'CHARGE', NULL, '6'),
    ('COMPTE_RESULTAT', 'XA', 'MARGE COMMERCIALE', 13, 0, 'TOTAL', 'RESULT', 'TA-RA-RB', NULL),
    ('COMPTE_RESULTAT', 'TB', 'Ventes de produits fabriques', 20, 1, 'DETAIL', 'PRODUCT', NULL, '21'),
    ('COMPTE_RESULTAT', 'TC', 'Travaux, services vendus', 21, 1, 'DETAIL', 'PRODUCT', NULL, '21'),
    ('COMPTE_RESULTAT', 'TD', 'Produits accessoires', 22, 1, 'DETAIL', 'PRODUCT', NULL, '21'),
    ('COMPTE_RESULTAT', 'XB', 'CHIFFRE D''AFFAIRES', 23, 0, 'TOTAL', 'RESULT', 'TA+TB+TC+TD', NULL),
    ('COMPTE_RESULTAT', 'TE', 'Production stockee (ou destockage)', 30, 1, 'DETAIL', 'PRODUCT', NULL, '6'),
    ('COMPTE_RESULTAT', 'TF', 'Production immobilisee', 31, 1, 'DETAIL', 'PRODUCT', NULL, '21'),
    ('COMPTE_RESULTAT', 'TG', 'Subventions d''exploitation', 32, 1, 'DETAIL', 'PRODUCT', NULL, '21'),
    ('COMPTE_RESULTAT', 'TH', 'Autres produits', 33, 1, 'DETAIL', 'PRODUCT', NULL, '21'),
    ('COMPTE_RESULTAT', 'TI', 'Transferts de charges d''exploitation', 34, 1, 'DETAIL', 'PRODUCT', NULL, '12'),
    ('COMPTE_RESULTAT', 'RC', 'Achats de matieres premieres et fournitures liees', 40, 1, 'DETAIL', 'CHARGE', NULL, '22'),
    ('COMPTE_RESULTAT', 'RD', 'Variation de stocks de matieres premieres et fournitures liees', 41, 1, 'DETAIL', 'CHARGE', NULL, '6'),
    ('COMPTE_RESULTAT', 'RE', 'Autres achats', 42, 1, 'DETAIL', 'CHARGE', NULL, '22'),
    ('COMPTE_RESULTAT', 'RF', 'Variation de stocks d''autres approvisionnements', 43, 1, 'DETAIL', 'CHARGE', NULL, '6'),
    ('COMPTE_RESULTAT', 'RG', 'Transports', 44, 1, 'DETAIL', 'CHARGE', NULL, '23'),
    ('COMPTE_RESULTAT', 'RH', 'Services exterieurs', 45, 1, 'DETAIL', 'CHARGE', NULL, '24'),
    ('COMPTE_RESULTAT', 'RI', 'Impots et taxes', 46, 1, 'DETAIL', 'CHARGE', NULL, '25'),
    ('COMPTE_RESULTAT', 'RJ', 'Autres charges', 47, 1, 'DETAIL', 'CHARGE', NULL, '26'),
    ('COMPTE_RESULTAT', 'XC', 'VALEUR AJOUTEE', 48, 0, 'TOTAL', 'RESULT', 'XA+TB+TC+TD+TE+TF+TG+TH+TI-RC-RD-RE-RF-RG-RH-RI-RJ', NULL),
    ('COMPTE_RESULTAT', 'RK', 'Charges de personnel', 50, 1, 'DETAIL', 'CHARGE', NULL, '27'),
    ('COMPTE_RESULTAT', 'XD', 'EXCEDENT BRUT D''EXPLOITATION', 51, 0, 'TOTAL', 'RESULT', 'XC-RK', '28'),
    ('COMPTE_RESULTAT', 'TJ', 'Reprises d''amortissements, de provisions et de depreciations', 60, 1, 'DETAIL', 'PRODUCT', NULL, '28'),
    ('COMPTE_RESULTAT', 'RL', 'Dotations aux amortissements, aux provisions et aux depreciations', 61, 1, 'DETAIL', 'CHARGE', NULL, '3C'),
    ('COMPTE_RESULTAT', 'XE', 'RESULTAT D''EXPLOITATION', 62, 0, 'TOTAL', 'RESULT', 'XD+TJ-RL', NULL),
    ('COMPTE_RESULTAT', 'TK', 'Revenus financiers et assimiles', 70, 1, 'DETAIL', 'PRODUCT', NULL, '29'),
    ('COMPTE_RESULTAT', 'TL', 'Reprises de provisions et depreciations financieres', 71, 1, 'DETAIL', 'PRODUCT', NULL, '28'),
    ('COMPTE_RESULTAT', 'TM', 'Transferts de charges financieres', 72, 1, 'DETAIL', 'PRODUCT', NULL, '12'),
    ('COMPTE_RESULTAT', 'RM', 'Frais financiers et charges assimilees', 73, 1, 'DETAIL', 'CHARGE', NULL, '29'),
    ('COMPTE_RESULTAT', 'RN', 'Dotations aux provisions et aux depreciations financieres', 74, 1, 'DETAIL', 'CHARGE', NULL, '3C'),
    ('COMPTE_RESULTAT', 'XF', 'RESULTAT FINANCIER', 75, 0, 'TOTAL', 'RESULT', 'TK+TL+TM-RM-RN', NULL),
    ('COMPTE_RESULTAT', 'XG', 'RESULTAT DES ACTIVITES ORDINAIRES', 76, 0, 'TOTAL', 'RESULT', 'XE+XF', NULL),
    ('COMPTE_RESULTAT', 'TN', 'Produits des cessions d''immobilisations', 80, 1, 'DETAIL', 'PRODUCT', NULL, '3D'),
    ('COMPTE_RESULTAT', 'TO', 'Autres produits HAO', 81, 1, 'DETAIL', 'PRODUCT', NULL, '30'),
    ('COMPTE_RESULTAT', 'RO', 'Valeurs comptables des cessions d''immobilisations', 82, 1, 'DETAIL', 'CHARGE', NULL, '3D'),
    ('COMPTE_RESULTAT', 'RP', 'Autres charges HAO', 83, 1, 'DETAIL', 'CHARGE', NULL, '30'),
    ('COMPTE_RESULTAT', 'XH', 'RESULTAT HORS ACTIVITES ORDINAIRES', 84, 0, 'TOTAL', 'RESULT', 'TN+TO-RO-RP', NULL),
    ('COMPTE_RESULTAT', 'RQ', 'Participation des travailleurs', 90, 1, 'DETAIL', 'CHARGE', NULL, '30'),
    ('COMPTE_RESULTAT', 'RS', 'Impots sur le resultat', 91, 1, 'DETAIL', 'CHARGE', NULL, NULL),
    ('COMPTE_RESULTAT', 'XI', 'RESULTAT NET', 99, 0, 'TOTAL', 'RESULT', 'XG+XH-RQ-RS', NULL),
    -- Tableau des flux de tresorerie (calcule par le logiciel, poste par poste)
    ('TFT', 'ZA', 'Tresorerie nette au 1er janvier (tresorerie actif N-1 - tresorerie passif N-1)', 10, 0, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FA', 'Capacite d''autofinancement globale (CAFG)', 20, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FB', '- Actif circulant HAO', 21, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FC', '- Variation des stocks', 22, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FD', '- Variation des creances', 23, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FE', '+ Variation du passif circulant', 24, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'ZB', 'Flux de tresorerie provenant des activites operationnelles', 29, 0, 'TOTAL', 'FLOW', 'FA-FB-FC-FD+FE', NULL),
    ('TFT', 'FF', '- Decaissements lies aux acquisitions d''immobilisations incorporelles', 30, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FG', '- Decaissements lies aux acquisitions d''immobilisations corporelles', 31, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FH', '- Decaissements lies aux acquisitions d''immobilisations financieres', 32, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FI', '+ Encaissements lies aux cessions d''immobilisations incorporelles et corporelles', 33, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FJ', '+ Encaissements lies aux cessions d''immobilisations financieres', 34, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'ZC', 'Flux de tresorerie provenant des activites d''investissement', 39, 0, 'TOTAL', 'FLOW', '-FF-FG-FH+FI+FJ', NULL),
    ('TFT', 'FK', '+ Augmentations de capital par apports nouveaux', 40, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FL', '+ Subventions d''investissement recues', 41, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FM', '- Prelevements sur le capital', 42, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FN', '- Dividendes verses', 43, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'ZD', 'Flux de tresorerie provenant des capitaux propres', 49, 0, 'TOTAL', 'FLOW', 'FK+FL-FM-FN', NULL),
    ('TFT', 'FO', '+ Emprunts', 50, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FP', '+ Autres dettes financieres', 51, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'FQ', '- Remboursements des emprunts et autres dettes financieres', 52, 1, 'DETAIL', 'FLOW', NULL, NULL),
    ('TFT', 'ZE', 'Flux de tresorerie provenant des capitaux etrangers', 59, 0, 'TOTAL', 'FLOW', 'FO+FP-FQ', NULL),
    ('TFT', 'ZF', 'Flux de tresorerie provenant des activites de financement', 60, 0, 'TOTAL', 'FLOW', 'ZD+ZE', NULL),
    ('TFT', 'ZG', 'VARIATION DE LA TRESORERIE NETTE DE LA PERIODE', 70, 0, 'TOTAL', 'FLOW', 'ZB+ZC+ZF', NULL),
    ('TFT', 'ZH', 'Tresorerie nette au 31 decembre', 80, 0, 'TOTAL', 'FLOW', 'ZG+ZA', NULL)
), numbered AS (
    SELECT s.*, t.id AS template_id, ROW_NUMBER() OVER (ORDER BY s.template_code, s.position) AS rank
    FROM seed s JOIN acc_statement_template t ON t.code = s.template_code AND t.version = 1
    WHERE NOT EXISTS (SELECT 1 FROM acc_statement_line l WHERE l.template_id = t.id AND l.code = s.code)
)
INSERT INTO acc_statement_line (id, template_id, code, label, position, level, line_type, section, formula, note_ref)
SELECT 3181000 + rank, template_id, code, label, position, level, line_type, section, formula, note_ref FROM numbered;

-- Correspondance comptes vers postes : le prefixe le plus long l'emporte ; un sens (DEBIT, CREDIT)
-- ne s'applique qu'au solde de ce sens, ce qui classe un compte de tiers a solde inverse de l'autre cote.
WITH seed (template_code, line_code, prefix, side, value_column) AS (
    VALUES
    ('BILAN', 'AE', '211', 'ANY', 'GROSS'), ('BILAN', 'AF', '212', 'ANY', 'GROSS'), ('BILAN', 'AF', '213', 'ANY', 'GROSS'),
    ('BILAN', 'AF', '214', 'ANY', 'GROSS'), ('BILAN', 'AG', '215', 'ANY', 'GROSS'), ('BILAN', 'AG', '216', 'ANY', 'GROSS'),
    ('BILAN', 'AH', '217', 'ANY', 'GROSS'), ('BILAN', 'AH', '218', 'ANY', 'GROSS'), ('BILAN', 'AH', '219', 'ANY', 'GROSS'),
    ('BILAN', 'AH', '20', 'ANY', 'GROSS'), ('BILAN', 'AH', '21', 'ANY', 'GROSS'),
    ('BILAN', 'AJ', '22', 'ANY', 'GROSS'), ('BILAN', 'AK', '23', 'ANY', 'GROSS'), ('BILAN', 'AL', '234', 'ANY', 'GROSS'),
    ('BILAN', 'AL', '235', 'ANY', 'GROSS'), ('BILAN', 'AL', '238', 'ANY', 'GROSS'), ('BILAN', 'AL', '239', 'ANY', 'GROSS'),
    ('BILAN', 'AM', '24', 'ANY', 'GROSS'), ('BILAN', 'AN', '245', 'ANY', 'GROSS'), ('BILAN', 'AP', '25', 'ANY', 'GROSS'),
    ('BILAN', 'AR', '26', 'ANY', 'GROSS'), ('BILAN', 'AS', '27', 'ANY', 'GROSS'),
    ('BILAN', 'AE', '2811', 'ANY', 'DEPRECIATION'), ('BILAN', 'AF', '2812', 'ANY', 'DEPRECIATION'), ('BILAN', 'AF', '2813', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'AF', '2814', 'ANY', 'DEPRECIATION'), ('BILAN', 'AG', '2815', 'ANY', 'DEPRECIATION'), ('BILAN', 'AG', '2816', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'AH', '281', 'ANY', 'DEPRECIATION'), ('BILAN', 'AE', '2911', 'ANY', 'DEPRECIATION'), ('BILAN', 'AF', '2912', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'AF', '2913', 'ANY', 'DEPRECIATION'), ('BILAN', 'AF', '2914', 'ANY', 'DEPRECIATION'), ('BILAN', 'AG', '2915', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'AG', '2916', 'ANY', 'DEPRECIATION'), ('BILAN', 'AH', '291', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'AJ', '282', 'ANY', 'DEPRECIATION'), ('BILAN', 'AJ', '292', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'AK', '283', 'ANY', 'DEPRECIATION'), ('BILAN', 'AL', '2834', 'ANY', 'DEPRECIATION'), ('BILAN', 'AL', '2835', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'AL', '2838', 'ANY', 'DEPRECIATION'), ('BILAN', 'AK', '293', 'ANY', 'DEPRECIATION'), ('BILAN', 'AL', '2934', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'AL', '2935', 'ANY', 'DEPRECIATION'), ('BILAN', 'AL', '2938', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'AM', '284', 'ANY', 'DEPRECIATION'), ('BILAN', 'AN', '2845', 'ANY', 'DEPRECIATION'), ('BILAN', 'AM', '294', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'AN', '2945', 'ANY', 'DEPRECIATION'), ('BILAN', 'AP', '295', 'ANY', 'DEPRECIATION'), ('BILAN', 'AR', '296', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'AS', '297', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'BA', '485', 'ANY', 'GROSS'), ('BILAN', 'BA', '488', 'ANY', 'GROSS'), ('BILAN', 'BA', '498', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'BB', '3', 'ANY', 'GROSS'), ('BILAN', 'BB', '39', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'BH', '409', 'ANY', 'GROSS'), ('BILAN', 'BH', '40', 'DEBIT', 'GROSS'), ('BILAN', 'BH', '490', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'BI', '41', 'DEBIT', 'GROSS'), ('BILAN', 'BI', '491', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'BJ', '42', 'DEBIT', 'GROSS'), ('BILAN', 'BJ', '43', 'DEBIT', 'GROSS'), ('BILAN', 'BJ', '44', 'DEBIT', 'GROSS'),
    ('BILAN', 'BJ', '45', 'DEBIT', 'GROSS'), ('BILAN', 'BJ', '46', 'DEBIT', 'GROSS'), ('BILAN', 'BJ', '47', 'DEBIT', 'GROSS'),
    ('BILAN', 'BJ', '185', 'DEBIT', 'GROSS'), ('BILAN', 'BJ', '49', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'BQ', '50', 'ANY', 'GROSS'), ('BILAN', 'BQ', '590', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'BR', '51', 'ANY', 'GROSS'), ('BILAN', 'BR', '591', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'BS', '52', 'DEBIT', 'GROSS'), ('BILAN', 'BS', '53', 'DEBIT', 'GROSS'), ('BILAN', 'BS', '54', 'DEBIT', 'GROSS'),
    ('BILAN', 'BS', '55', 'DEBIT', 'GROSS'), ('BILAN', 'BS', '56', 'DEBIT', 'GROSS'), ('BILAN', 'BS', '57', 'DEBIT', 'GROSS'),
    ('BILAN', 'BS', '58', 'DEBIT', 'GROSS'), ('BILAN', 'BS', '59', 'ANY', 'DEPRECIATION'),
    ('BILAN', 'BU', '478', 'ANY', 'GROSS'),
    ('BILAN', 'CA', '10', 'ANY', 'GROSS'), ('BILAN', 'CB', '109', 'ANY', 'GROSS'), ('BILAN', 'CD', '105', 'ANY', 'GROSS'),
    ('BILAN', 'CE', '106', 'ANY', 'GROSS'), ('BILAN', 'CF', '11', 'ANY', 'GROSS'), ('BILAN', 'CG', '118', 'ANY', 'GROSS'),
    ('BILAN', 'CH', '12', 'ANY', 'GROSS'), ('BILAN', 'CJ', '13', 'ANY', 'GROSS'), ('BILAN', 'CL', '14', 'ANY', 'GROSS'),
    ('BILAN', 'CM', '15', 'ANY', 'GROSS'), ('BILAN', 'DA', '16', 'ANY', 'GROSS'), ('BILAN', 'DA', '18', 'ANY', 'GROSS'),
    ('BILAN', 'DB', '17', 'ANY', 'GROSS'), ('BILAN', 'DC', '19', 'ANY', 'GROSS'),
    ('BILAN', 'DH', '481', 'CREDIT', 'GROSS'), ('BILAN', 'DH', '482', 'CREDIT', 'GROSS'), ('BILAN', 'DH', '484', 'CREDIT', 'GROSS'),
    ('BILAN', 'DH', '4998', 'ANY', 'GROSS'), ('BILAN', 'DI', '41', 'CREDIT', 'GROSS'), ('BILAN', 'DI', '419', 'CREDIT', 'GROSS'),
    ('BILAN', 'DJ', '40', 'CREDIT', 'GROSS'), ('BILAN', 'DK', '42', 'CREDIT', 'GROSS'), ('BILAN', 'DK', '43', 'CREDIT', 'GROSS'),
    ('BILAN', 'DK', '44', 'CREDIT', 'GROSS'), ('BILAN', 'DM', '45', 'CREDIT', 'GROSS'), ('BILAN', 'DM', '46', 'CREDIT', 'GROSS'),
    ('BILAN', 'DM', '47', 'CREDIT', 'GROSS'), ('BILAN', 'DM', '185', 'CREDIT', 'GROSS'), ('BILAN', 'DN', '499', 'ANY', 'GROSS'),
    ('BILAN', 'DN', '599', 'ANY', 'GROSS'),
    ('BILAN', 'DQ', '564', 'CREDIT', 'GROSS'), ('BILAN', 'DQ', '565', 'CREDIT', 'GROSS'), ('BILAN', 'DR', '52', 'CREDIT', 'GROSS'),
    ('BILAN', 'DR', '53', 'CREDIT', 'GROSS'), ('BILAN', 'DR', '54', 'CREDIT', 'GROSS'), ('BILAN', 'DR', '55', 'CREDIT', 'GROSS'),
    ('BILAN', 'DR', '56', 'CREDIT', 'GROSS'), ('BILAN', 'DR', '57', 'CREDIT', 'GROSS'), ('BILAN', 'DR', '58', 'CREDIT', 'GROSS'),
    ('BILAN', 'DV', '479', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'TA', '701', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TB', '702', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TB', '703', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'TB', '704', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TC', '705', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TC', '706', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'TD', '707', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TD', '708', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TC', '709', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'TC', '70', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'TE', '73', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TF', '72', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TG', '71', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'TH', '75', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TI', '78', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TM', '787', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'RA', '601', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RB', '6031', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RB', '603', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'RC', '602', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RD', '6032', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RF', '6033', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'RE', '60', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RG', '61', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RH', '62', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'RH', '63', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RI', '64', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RJ', '65', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'RK', '66', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TJ', '79', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TL', '797', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'RL', '68', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RL', '69', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RN', '697', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'RN', '687', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'TK', '77', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RM', '67', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'TN', '82', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TO', '84', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'TO', '86', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'TO', '88', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RO', '81', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RP', '83', 'ANY', 'GROSS'),
    ('COMPTE_RESULTAT', 'RP', '85', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RQ', '87', 'ANY', 'GROSS'), ('COMPTE_RESULTAT', 'RS', '89', 'ANY', 'GROSS')
), numbered AS (
    SELECT s.*, t.id AS template_id, ROW_NUMBER() OVER (ORDER BY s.template_code, s.prefix, s.side, s.value_column) AS rank
    FROM seed s JOIN acc_statement_template t ON t.code = s.template_code AND t.version = 1
    WHERE NOT EXISTS (SELECT 1 FROM acc_statement_mapping m WHERE m.template_id = t.id AND m.account_prefix = s.prefix
                                                              AND m.balance_side = s.side AND m.value_column = s.value_column)
)
INSERT INTO acc_statement_mapping (id, template_id, line_code, account_prefix, balance_side, value_column)
SELECT 3182000 + rank, template_id, line_code, prefix, side, value_column FROM numbered;

-- ------------------------------------------------------------------ droits

WITH new_permission (name, display_name, module, action) AS (
    VALUES ('ACCOUNTING_CLOSING_READ', 'Consulter la liste de cloture, les etats financiers et les editions definitives', 'ACCOUNTING_CLOSING', 'READ'),
           ('ACCOUNTING_CLOSING_MANAGE', 'Cocher les etapes de cloture et rediger les notes annexes', 'ACCOUNTING_CLOSING', 'MANAGE'),
           ('ACCOUNTING_CLOSING_FINALIZE', 'Clore l''exercice, deroger a une etape et generer les etats definitifs', 'ACCOUNTING_CLOSING', 'FINALIZE'),
           ('ACCOUNTING_STATEMENT_EDIT', 'Modifier la correspondance des comptes vers les postes des etats', 'ACCOUNTING_STATEMENT', 'EDIT'),
           ('BALANCE_JUSTIFICATION_PREPARE', 'Preparer les justificatifs de solde et les confirmations de solde', 'BALANCE_JUSTIFICATION', 'PREPARE'),
           ('BALANCE_JUSTIFICATION_REVIEW', 'Revoir les justificatifs de solde', 'BALANCE_JUSTIFICATION', 'REVIEW')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE FROM numbered;

WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN')) AS roles(r)
    CROSS JOIN (VALUES ('ACCOUNTING_CLOSING_READ'), ('ACCOUNTING_CLOSING_MANAGE'), ('ACCOUNTING_CLOSING_FINALIZE'),
                       ('ACCOUNTING_STATEMENT_EDIT'), ('BALANCE_JUSTIFICATION_PREPARE'), ('BALANCE_JUSTIFICATION_REVIEW')) AS perms(p)
    UNION ALL
    SELECT 'FINANCE', p FROM (VALUES ('ACCOUNTING_CLOSING_READ'), ('ACCOUNTING_CLOSING_MANAGE'), ('BALANCE_JUSTIFICATION_PREPARE'),
                                     ('BALANCE_JUSTIFICATION_REVIEW')) AS perms(p)
    UNION ALL
    SELECT r, p FROM (VALUES ('MANAGER'), ('AUDITOR')) AS roles(r)
    CROSS JOIN (VALUES ('ACCOUNTING_CLOSING_READ'), ('ACCOUNTING_REPORT_READ')) AS perms(p)
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE FROM grants g JOIN role r ON r.name = g.role_name JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

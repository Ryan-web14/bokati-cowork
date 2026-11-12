-- Plan finance, lot F1 : socle comptable SYSCOHADA.
--
-- Plan de comptes (nomenclature de reference et plan de l'entreprise), comptes auxiliaires de
-- tiers, exercices et periodes, journaux, pieces et lignes en partie double, numerotation continue
-- et protegee, empreinte chainee, controle de continuite, pieces en attente d'equilibrage,
-- lettrage.
--
-- Garanties en base (plan, sections 4.6 et 4.8) :
--   * une ligne porte soit un debit soit un credit, jamais negatif ;
--   * une piece ne passe en VALIDATED qu'equilibree (sauf marqueur accepted_unbalanced) ;
--   * une piece validee ne se modifie pas (numero, journal, date, lignes) et ne se supprime pas ;
--   * le compteur de numerotation n'avance que d'un pas a la fois.

CREATE TABLE IF NOT EXISTS acc_reference_chart
(
    number     VARCHAR(20)  PRIMARY KEY,
    label      VARCHAR(255) NOT NULL,
    class_code VARCHAR(1)   NOT NULL
);

CREATE TABLE IF NOT EXISTS acc_account
(
    id                  BIGINT PRIMARY KEY,
    company_code        VARCHAR(40)  NOT NULL DEFAULT 'MAIN',
    number              VARCHAR(20)  NOT NULL UNIQUE,
    label               VARCHAR(255) NOT NULL,
    parent_number       VARCHAR(20),
    class_code          VARCHAR(1)   NOT NULL,
    nature              VARCHAR(20)  NOT NULL,
    normal_side         VARCHAR(6)   NOT NULL,
    reconcilable        BOOLEAN      NOT NULL DEFAULT FALSE,
    third_party_type    VARCHAR(20),
    treasury_type       VARCHAR(20),
    analytic_required   BOOLEAN      NOT NULL DEFAULT FALSE,
    tax_code            VARCHAR(30),
    statement_line_code VARCHAR(40),
    status              VARCHAR(20)  NOT NULL,
    merged_into         VARCHAR(20),
    created_by          VARCHAR(180),
    approved_by         VARCHAR(180),
    approved_at         TIMESTAMPTZ,
    updated_by          VARCHAR(180),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_account_status CHECK (status IN ('PENDING_APPROVAL', 'ACTIVE', 'INACTIVE', 'MERGED')),
    CONSTRAINT ck_acc_account_nature CHECK (nature IN ('BALANCE', 'MANAGEMENT', 'HAO', 'ANALYTIC', 'OFF_BALANCE')),
    CONSTRAINT ck_acc_account_side CHECK (normal_side IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_acc_account_class CHECK (substr(number, 1, 1) = class_code)
);
CREATE INDEX IF NOT EXISTS idx_acc_account_parent ON acc_account (parent_number);

CREATE TABLE IF NOT EXISTS acc_account_history
(
    id             BIGINT PRIMARY KEY,
    account_number VARCHAR(20)  NOT NULL,
    field          VARCHAR(60)  NOT NULL,
    old_value      TEXT,
    new_value      TEXT,
    reason         VARCHAR(500),
    changed_by     VARCHAR(180),
    changed_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_acc_account_history ON acc_account_history (account_number, changed_at DESC);

CREATE TABLE IF NOT EXISTS acc_third_party_account
(
    id             BIGINT PRIMARY KEY,
    account_number VARCHAR(20)  NOT NULL,
    party_type     VARCHAR(20)  NOT NULL,
    party_code     VARCHAR(120) NOT NULL,
    label          VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_acc_third_party UNIQUE (account_number, party_code)
);

CREATE TABLE IF NOT EXISTS acc_fiscal_year
(
    id           BIGINT PRIMARY KEY,
    company_code VARCHAR(40) NOT NULL DEFAULT 'MAIN',
    code         VARCHAR(10) NOT NULL UNIQUE,
    start_date   DATE        NOT NULL,
    end_date     DATE        NOT NULL,
    status       VARCHAR(20) NOT NULL,
    closed_at    TIMESTAMPTZ,
    closed_by    VARCHAR(180),
    CONSTRAINT ck_acc_fiscal_year_status CHECK (status IN ('OPEN', 'CLOSING', 'CLOSED')),
    CONSTRAINT ck_acc_fiscal_year_dates CHECK (end_date > start_date)
);

CREATE TABLE IF NOT EXISTS acc_period
(
    id             BIGINT PRIMARY KEY,
    fiscal_year_id BIGINT      NOT NULL REFERENCES acc_fiscal_year (id),
    code           VARCHAR(10) NOT NULL UNIQUE,
    start_date     DATE        NOT NULL,
    end_date       DATE        NOT NULL,
    status         VARCHAR(20) NOT NULL,
    locked_at      TIMESTAMPTZ,
    locked_by      VARCHAR(180),
    closed_at      TIMESTAMPTZ,
    closed_by      VARCHAR(180),
    CONSTRAINT ck_acc_period_status CHECK (status IN ('OPEN', 'LOCKED', 'CLOSED'))
);

CREATE TABLE IF NOT EXISTS acc_journal
(
    id                      BIGINT PRIMARY KEY,
    company_code            VARCHAR(40)  NOT NULL DEFAULT 'MAIN',
    code                    VARCHAR(30)  NOT NULL UNIQUE,
    label                   VARCHAR(255) NOT NULL,
    type                    VARCHAR(20)  NOT NULL,
    treasury_account_number VARCHAR(20),
    numbering_reset         VARCHAR(10)  NOT NULL DEFAULT 'YEAR',
    auto_validate           BOOLEAN      NOT NULL DEFAULT TRUE,
    allowed_accounts        VARCHAR(2000),
    cash_register_code      VARCHAR(100),
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by              VARCHAR(180),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_journal_type CHECK (type IN ('SALES', 'PURCHASES', 'CASH', 'BANK', 'MOBILE_MONEY', 'WALLET', 'MISC', 'STOCK',
        'OPENING', 'REVERSAL', 'OFF_BALANCE', 'ANALYTIC', 'PAYROLL')),
    CONSTRAINT ck_acc_journal_reset CHECK (numbering_reset IN ('YEAR', 'PERIOD'))
);

CREATE TABLE IF NOT EXISTS acc_entry
(
    id                  BIGINT PRIMARY KEY,
    company_code        VARCHAR(40)    NOT NULL DEFAULT 'MAIN',
    entry_number        VARCHAR(60)    UNIQUE,
    provisional_number  VARCHAR(60)    NOT NULL UNIQUE,
    journal_code        VARCHAR(30)    NOT NULL,
    fiscal_year_code    VARCHAR(10)    NOT NULL,
    period_code         VARCHAR(10)    NOT NULL,
    sequence_number     INTEGER,
    entry_date          DATE           NOT NULL,
    original_date       DATE,
    document_date       DATE,
    label               VARCHAR(500)   NOT NULL,
    origin              VARCHAR(20)    NOT NULL,
    source_type         VARCHAR(60),
    source_code         VARCHAR(120),
    status              VARCHAR(20)    NOT NULL,
    accepted_unbalanced BOOLEAN        NOT NULL DEFAULT FALSE,
    unbalanced_amount   NUMERIC(19, 4),
    pending_reason      VARCHAR(60),
    pending_detail      VARCHAR(1000),
    total_debit         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_credit        NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_by          VARCHAR(180)   NOT NULL,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    validated_by        VARCHAR(180),
    validated_at        TIMESTAMPTZ,
    reversal_of         VARCHAR(60),
    reversed_by_entry   VARCHAR(60),
    reversal_reason     VARCHAR(500),
    regularization_of   VARCHAR(60),
    regularized_by      VARCHAR(60),
    rejected_by         VARCHAR(180),
    rejected_at         TIMESTAMPTZ,
    rejection_reason    VARCHAR(500),
    content_hash        VARCHAR(64),
    previous_hash       VARCHAR(64),
    attachments_json    JSONB,
    CONSTRAINT ck_acc_entry_status CHECK (status IN ('DRAFT', 'PENDING_BALANCE', 'VALIDATED', 'REVERSED', 'REJECTED')),
    CONSTRAINT ck_acc_entry_origin CHECK (origin IN ('MANUAL', 'CASH', 'PLATFORM', 'IMPORT', 'CLOSING', 'OPENING', 'REVERSAL', 'REGULARIZATION')),
    CONSTRAINT ck_acc_entry_number CHECK (status NOT IN ('VALIDATED', 'REVERSED') OR (entry_number IS NOT NULL AND sequence_number IS NOT NULL)),
    CONSTRAINT uq_acc_entry_sequence UNIQUE (journal_code, fiscal_year_code, period_code, sequence_number)
);
CREATE INDEX IF NOT EXISTS idx_acc_entry_journal_date ON acc_entry (journal_code, entry_date);
CREATE INDEX IF NOT EXISTS idx_acc_entry_status ON acc_entry (status, period_code);
CREATE INDEX IF NOT EXISTS idx_acc_entry_source ON acc_entry (source_type, source_code);

CREATE TABLE IF NOT EXISTS acc_entry_line
(
    id               BIGINT PRIMARY KEY,
    entry_id         BIGINT         NOT NULL REFERENCES acc_entry (id),
    line_no          INTEGER        NOT NULL,
    account_number   VARCHAR(20)    NOT NULL,
    third_party_type VARCHAR(20),
    third_party_code VARCHAR(120),
    label            VARCHAR(500),
    debit            NUMERIC(19, 4) NOT NULL DEFAULT 0,
    credit           NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency         VARCHAR(3),
    foreign_amount   NUMERIC(19, 4),
    rate             NUMERIC(19, 8),
    tax_code         VARCHAR(30),
    tax_base         NUMERIC(19, 4),
    due_date         DATE,
    reference        VARCHAR(120),
    CONSTRAINT ck_acc_line_amounts CHECK (debit >= 0 AND credit >= 0 AND NOT (debit > 0 AND credit > 0)),
    CONSTRAINT uq_acc_line_no UNIQUE (entry_id, line_no)
);
CREATE INDEX IF NOT EXISTS idx_acc_line_account ON acc_entry_line (account_number, third_party_code);

CREATE TABLE IF NOT EXISTS acc_numbering_counter
(
    id               BIGINT PRIMARY KEY,
    journal_code     VARCHAR(30) NOT NULL,
    fiscal_year_code VARCHAR(10) NOT NULL,
    period_code      VARCHAR(10) NOT NULL DEFAULT '',
    last_number      INTEGER     NOT NULL DEFAULT 0,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_acc_numbering_counter UNIQUE (journal_code, fiscal_year_code, period_code)
);

CREATE TABLE IF NOT EXISTS acc_numbering_check
(
    id               BIGINT PRIMARY KEY,
    journal_code     VARCHAR(30)  NOT NULL,
    fiscal_year_code VARCHAR(10)  NOT NULL,
    checked_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    checked_by       VARCHAR(180),
    first_number     INTEGER,
    last_number      INTEGER,
    entry_count      INTEGER      NOT NULL,
    counter_value    INTEGER,
    gaps             TEXT,
    duplicates       TEXT,
    chain_ok         BOOLEAN      NOT NULL,
    ok               BOOLEAN      NOT NULL
);

CREATE TABLE IF NOT EXISTS acc_pending_entry_log
(
    id               BIGINT PRIMARY KEY,
    entry_id         BIGINT        NOT NULL REFERENCES acc_entry (id),
    reason           VARCHAR(40)   NOT NULL,
    difference       NUMERIC(19, 4),
    detail           VARCHAR(1000),
    notified_at      TIMESTAMPTZ,
    reminders        INTEGER       NOT NULL DEFAULT 0,
    last_reminder_at TIMESTAMPTZ,
    resolved_by      VARCHAR(180),
    resolved_at      TIMESTAMPTZ,
    resolution       VARCHAR(20),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_acc_pending_reason CHECK (reason IN ('UNBALANCED', 'COUNTERPART_NOT_ALLOWED', 'MISSING_ACCOUNT', 'ANALYTIC_INCOMPLETE', 'INACTIVE_ACCOUNT')),
    CONSTRAINT ck_acc_pending_resolution CHECK (resolution IS NULL OR resolution IN ('REGENERATED', 'COMPLETED', 'REJECTED', 'SUSPENSE', 'ACCEPTED'))
);

CREATE TABLE IF NOT EXISTS acc_lettering
(
    id                       BIGINT PRIMARY KEY,
    code                     VARCHAR(20)    NOT NULL,
    account_number           VARCHAR(20)    NOT NULL,
    third_party_code         VARCHAR(120),
    kind                     VARCHAR(20)    NOT NULL,
    status                   VARCHAR(20)    NOT NULL,
    total_debit              NUMERIC(19, 4) NOT NULL,
    total_credit             NUMERIC(19, 4) NOT NULL,
    difference               NUMERIC(19, 4) NOT NULL DEFAULT 0,
    difference_entry_number  VARCHAR(60),
    lettered_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
    lettered_by              VARCHAR(180)   NOT NULL,
    unlettered_at            TIMESTAMPTZ,
    unlettered_by            VARCHAR(180),
    unlettering_reason       VARCHAR(500),
    CONSTRAINT ck_acc_lettering_kind CHECK (kind IN ('AUTO_REFERENCE', 'AUTO_MATCH', 'MANUAL')),
    CONSTRAINT ck_acc_lettering_status CHECK (status IN ('PARTIAL', 'COMPLETE', 'UNLETTERED'))
);
CREATE INDEX IF NOT EXISTS idx_acc_lettering_account ON acc_lettering (account_number, third_party_code, status);

CREATE TABLE IF NOT EXISTS acc_lettering_line
(
    id             BIGINT PRIMARY KEY,
    lettering_id   BIGINT         NOT NULL REFERENCES acc_lettering (id),
    entry_line_id  BIGINT         NOT NULL REFERENCES acc_entry_line (id),
    matched_amount NUMERIC(19, 4) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_acc_lettering_line ON acc_lettering_line (entry_line_id);

-- ---------------------------------------------------------------- declencheurs

CREATE OR REPLACE FUNCTION acc_entry_check_balance() RETURNS TRIGGER AS $$
DECLARE
    sum_debit  NUMERIC;
    sum_credit NUMERIC;
    line_count INTEGER;
BEGIN
    IF NEW.status = 'VALIDATED' AND (OLD.status IS DISTINCT FROM 'VALIDATED') AND OLD.status <> 'REVERSED' THEN
        SELECT COALESCE(SUM(debit), 0), COALESCE(SUM(credit), 0), COUNT(*)
        INTO sum_debit, sum_credit, line_count
        FROM acc_entry_line WHERE entry_id = NEW.id;
        IF line_count < 2 THEN
            RAISE EXCEPTION 'Piece % : une piece comptable a au moins deux lignes', NEW.provisional_number;
        END IF;
        IF sum_debit <> sum_credit AND NOT NEW.accepted_unbalanced THEN
            RAISE EXCEPTION 'Piece % desequilibree : debit % credit %', NEW.provisional_number, sum_debit, sum_credit;
        END IF;
        IF NEW.entry_number IS NULL THEN
            RAISE EXCEPTION 'Piece % : une piece validee porte son numero definitif', NEW.provisional_number;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_acc_entry_check_balance ON acc_entry;
CREATE TRIGGER trg_acc_entry_check_balance BEFORE UPDATE ON acc_entry
    FOR EACH ROW EXECUTE FUNCTION acc_entry_check_balance();

CREATE OR REPLACE FUNCTION acc_entry_protect() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.status IN ('VALIDATED', 'REVERSED') THEN
            RAISE EXCEPTION 'Piece % validee : elle ne se supprime pas, elle se contre-passe', OLD.entry_number;
        END IF;
        RETURN OLD;
    END IF;
    IF OLD.status IN ('VALIDATED', 'REVERSED') THEN
        IF NEW.entry_number IS DISTINCT FROM OLD.entry_number
            OR NEW.journal_code IS DISTINCT FROM OLD.journal_code
            OR NEW.entry_date IS DISTINCT FROM OLD.entry_date
            OR NEW.fiscal_year_code IS DISTINCT FROM OLD.fiscal_year_code
            OR NEW.period_code IS DISTINCT FROM OLD.period_code
            OR NEW.sequence_number IS DISTINCT FROM OLD.sequence_number
            OR NEW.content_hash IS DISTINCT FROM OLD.content_hash
            OR NEW.previous_hash IS DISTINCT FROM OLD.previous_hash
            OR NEW.total_debit IS DISTINCT FROM OLD.total_debit
            OR NEW.total_credit IS DISTINCT FROM OLD.total_credit
            OR NEW.label IS DISTINCT FROM OLD.label THEN
            RAISE EXCEPTION 'Piece % validee : son numero, son journal, sa date, son libelle et ses montants ne changent plus', OLD.entry_number;
        END IF;
        IF NEW.status NOT IN ('VALIDATED', 'REVERSED') THEN
            RAISE EXCEPTION 'Piece % validee : elle ne revient pas en brouillard', OLD.entry_number;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_acc_entry_protect ON acc_entry;
CREATE TRIGGER trg_acc_entry_protect BEFORE UPDATE OR DELETE ON acc_entry
    FOR EACH ROW EXECUTE FUNCTION acc_entry_protect();

CREATE OR REPLACE FUNCTION acc_entry_line_protect() RETURNS TRIGGER AS $$
DECLARE
    entry_status VARCHAR;
    entry_ref    BIGINT;
BEGIN
    entry_ref := CASE WHEN TG_OP = 'DELETE' THEN OLD.entry_id ELSE NEW.entry_id END;
    SELECT status INTO entry_status FROM acc_entry WHERE id = entry_ref;
    IF entry_status IN ('VALIDATED', 'REVERSED') THEN
        RAISE EXCEPTION 'Les lignes d''une piece validee ne se modifient pas';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_acc_entry_line_protect ON acc_entry_line;
CREATE TRIGGER trg_acc_entry_line_protect BEFORE INSERT OR UPDATE OR DELETE ON acc_entry_line
    FOR EACH ROW EXECUTE FUNCTION acc_entry_line_protect();

CREATE OR REPLACE FUNCTION acc_numbering_counter_protect() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Un compteur de numerotation ne se supprime pas';
    END IF;
    IF NEW.last_number <> OLD.last_number + 1 THEN
        RAISE EXCEPTION 'Le compteur % % n''avance que d''un numero a la fois', OLD.journal_code, OLD.fiscal_year_code;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_acc_numbering_counter_protect ON acc_numbering_counter;
CREATE TRIGGER trg_acc_numbering_counter_protect BEFORE UPDATE OR DELETE ON acc_numbering_counter
    FOR EACH ROW EXECUTE FUNCTION acc_numbering_counter_protect();

-- -------------------------------------------------------------- plan de comptes

WITH seed (number, label, nature, normal_side, reconcilable, third_party_type, treasury_type, analytic_required) AS (
    VALUES
    ('1', 'Comptes de ressources durables', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('10', 'Capital', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('101', 'Capital social', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('104', 'Compte de l''exploitant', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('109', 'Apporteurs, capital souscrit non appelé', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('11', 'Réserves', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('111', 'Réserve légale', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('118', 'Autres réserves', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('12', 'Report à nouveau', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('121', 'Report à nouveau créditeur', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('129', 'Report à nouveau débiteur', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('13', 'Résultat net de l''exercice', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('131', 'Résultat net : bénéfice', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('139', 'Résultat net : perte', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('14', 'Subventions d''investissement', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('141', 'Subventions d''équipement', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('15', 'Provisions réglementées et fonds assimilés', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('151', 'Amortissements dérogatoires', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('16', 'Emprunts et dettes assimilées', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('162', 'Emprunts et dettes auprès des établissements de crédit', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('165', 'Dépôts et cautionnements reçus', 'BALANCE', 'CREDIT', TRUE, 'CUSTOMER', NULL, FALSE),
    ('166', 'Intérêts courus', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('17', 'Dettes de location-acquisition', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('19', 'Provisions pour risques et charges', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('191', 'Provisions pour litiges', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('192', 'Provisions pour garanties données aux clients', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('195', 'Provisions pour impôts', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('196', 'Provisions pour pensions et obligations similaires', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('198', 'Autres provisions pour risques et charges', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('2', 'Comptes d''actif immobilisé', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('21', 'Immobilisations incorporelles', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('211', 'Frais de développement', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('213', 'Logiciels et sites internet', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('22', 'Terrains', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('23', 'Bâtiments, installations techniques et agencements', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('231', 'Bâtiments sur sol propre', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('232', 'Bâtiments sur sol d''autrui', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('234', 'Aménagements, agencements et installations techniques', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('235', 'Aménagements de bureaux', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('239', 'Bâtiments et installations en cours', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('24', 'Matériel, mobilier et actifs biologiques', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('241', 'Matériel et outillage industriel et commercial', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('244', 'Matériel et mobilier', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('2441', 'Matériel de bureau', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('2442', 'Matériel informatique', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('2444', 'Mobilier de bureau', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('245', 'Matériel de transport', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('25', 'Avances et acomptes versés sur immobilisations', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('251', 'Avances et acomptes sur immobilisations incorporelles', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('252', 'Avances et acomptes sur immobilisations corporelles', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('27', 'Autres immobilisations financières', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('275', 'Dépôts et cautionnements versés', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('28', 'Amortissements', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('281', 'Amortissements des immobilisations incorporelles', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('283', 'Amortissements des bâtiments et aménagements', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('284', 'Amortissements du matériel', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('2844', 'Amortissements du matériel et mobilier', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('2845', 'Amortissements du matériel de transport', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('29', 'Dépréciations des immobilisations', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('3', 'Comptes de stocks', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('31', 'Marchandises', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('32', 'Matières premières et fournitures liées', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('33', 'Autres approvisionnements', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('39', 'Dépréciations des stocks', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4', 'Comptes de tiers', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('40', 'Fournisseurs et comptes rattachés', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('401', 'Fournisseurs, dettes en compte', 'BALANCE', 'CREDIT', TRUE, 'SUPPLIER', NULL, FALSE),
    ('408', 'Fournisseurs, factures non parvenues', 'BALANCE', 'CREDIT', TRUE, 'SUPPLIER', NULL, FALSE),
    ('409', 'Fournisseurs débiteurs', 'BALANCE', 'DEBIT', TRUE, 'SUPPLIER', NULL, FALSE),
    ('41', 'Clients et comptes rattachés', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('411', 'Clients', 'BALANCE', 'DEBIT', TRUE, 'CUSTOMER', NULL, FALSE),
    ('416', 'Créances clients litigieuses ou douteuses', 'BALANCE', 'DEBIT', FALSE, 'CUSTOMER', NULL, FALSE),
    ('4161', 'Créances litigieuses', 'BALANCE', 'DEBIT', TRUE, 'CUSTOMER', NULL, FALSE),
    ('4162', 'Créances douteuses', 'BALANCE', 'DEBIT', TRUE, 'CUSTOMER', NULL, FALSE),
    ('418', 'Clients, produits à recevoir', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('4181', 'Clients, factures à établir', 'BALANCE', 'DEBIT', TRUE, 'CUSTOMER', NULL, FALSE),
    ('419', 'Clients créditeurs', 'BALANCE', 'CREDIT', FALSE, 'CUSTOMER', NULL, FALSE),
    ('4191', 'Clients, avances et acomptes reçus', 'BALANCE', 'CREDIT', TRUE, 'CUSTOMER', NULL, FALSE),
    ('42', 'Personnel', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('421', 'Personnel, avances et acomptes', 'BALANCE', 'DEBIT', TRUE, 'EMPLOYEE', NULL, FALSE),
    ('422', 'Personnel, rémunérations dues', 'BALANCE', 'CREDIT', TRUE, 'EMPLOYEE', NULL, FALSE),
    ('428', 'Personnel, charges à payer et produits à recevoir', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('43', 'Organismes sociaux', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('431', 'Sécurité sociale', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('438', 'Organismes sociaux, charges à payer et produits à recevoir', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('44', 'État et collectivités publiques', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('441', 'État, impôt sur les bénéfices', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('443', 'État, TVA facturée', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4431', 'TVA facturée sur ventes', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4432', 'TVA facturée sur prestations de services', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('444', 'État, TVA due ou crédit de TVA', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4441', 'État, TVA due', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4449', 'État, crédit de TVA à reporter', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('445', 'État, TVA récupérable', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('4451', 'TVA récupérable sur immobilisations', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('4452', 'TVA récupérable sur achats', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('4454', 'TVA récupérable sur services extérieurs', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('447', 'État, impôts retenus à la source', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('448', 'État, charges à payer et produits à recevoir', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('46', 'Associés et groupe', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('462', 'Associés, comptes courants', 'BALANCE', 'CREDIT', TRUE, 'PARTNER', NULL, FALSE),
    ('47', 'Débiteurs et créditeurs divers', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('471', 'Débiteurs et créditeurs divers, comptes d''attente', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('4718', 'Écarts d''équilibrage à régulariser', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('4719', 'Encaissements à identifier', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('476', 'Charges constatées d''avance', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('477', 'Produits constatés d''avance', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('48', 'Créances et dettes hors activités ordinaires', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('481', 'Fournisseurs d''investissements', 'BALANCE', 'CREDIT', TRUE, 'SUPPLIER', NULL, FALSE),
    ('485', 'Créances sur cessions d''immobilisations', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('49', 'Dépréciations et provisions pour risques à court terme', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('491', 'Dépréciations des comptes clients', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4911', 'Dépréciations des créances litigieuses', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4912', 'Dépréciations des créances douteuses', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('499', 'Provisions pour risques à court terme', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('5', 'Comptes de trésorerie', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('51', 'Valeurs à encaisser', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('513', 'Chèques à encaisser', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('52', 'Banques', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('521', 'Banques locales', 'BALANCE', 'DEBIT', FALSE, NULL, 'BANK', FALSE),
    ('57', 'Caisse', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('571', 'Caisse siège social', 'BALANCE', 'DEBIT', FALSE, NULL, 'CASH', FALSE),
    ('58', 'Régies d''avances, accréditifs et virements internes', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('585', 'Virements de fonds', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('59', 'Dépréciations des titres de placement et de la trésorerie', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('6', 'Comptes de charges des activités ordinaires', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('60', 'Achats et variations de stocks', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('601', 'Achats de marchandises', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('603', 'Variations des stocks de biens achetés', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('604', 'Achats stockés de matières et fournitures consommables', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('605', 'Autres achats', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6051', 'Fournitures non stockables, eau', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6052', 'Fournitures non stockables, électricité', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6053', 'Fournitures non stockables, autres énergies', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6054', 'Fournitures d''entretien non stockables', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6055', 'Fournitures de bureau non stockables', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6056', 'Achats de petit matériel et outillage', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('61', 'Transports', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('614', 'Transports du personnel', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('618', 'Autres frais de transport', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6181', 'Voyages et déplacements', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('62', 'Services extérieurs', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('622', 'Locations et charges locatives', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('624', 'Entretien, réparations et maintenance', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('625', 'Primes d''assurance', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('626', 'Études, recherches et documentation', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6261', 'Frais postaux', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('627', 'Publicité, publications, relations publiques', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('628', 'Frais de télécommunications', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('63', 'Autres services extérieurs', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('631', 'Frais bancaires', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('632', 'Rémunérations d''intermédiaires et de conseils', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('638', 'Autres charges externes', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('64', 'Impôts et taxes', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('65', 'Autres charges', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('651', 'Pertes sur créances clients et autres débiteurs', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6511', 'Pertes sur créances clients', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('658', 'Charges diverses', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('659', 'Charges pour dépréciations et provisions à court terme d''exploitation', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6594', 'Charges pour dépréciations des créances', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('66', 'Charges de personnel', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('661', 'Rémunérations directes versées au personnel', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('664', 'Charges sociales', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('67', 'Frais financiers et charges assimilées', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('671', 'Intérêts des emprunts', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('673', 'Escomptes accordés', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('676', 'Pertes de change', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('68', 'Dotations aux amortissements', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('681', 'Dotations aux amortissements d''exploitation', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6812', 'Dotations aux amortissements des immobilisations incorporelles', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6813', 'Dotations aux amortissements des immobilisations corporelles', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('69', 'Dotations aux provisions et aux dépréciations', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('691', 'Dotations aux provisions et dépréciations d''exploitation', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('7', 'Comptes de produits des activités ordinaires', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('70', 'Ventes', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('701', 'Ventes de marchandises', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('706', 'Services vendus', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('7061', 'Locations d''espaces de travail', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('7062', 'Abonnements et pass', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('7063', 'Réservations de salles', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('7064', 'Domiciliation', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('707', 'Produits accessoires', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('709', 'Rabais, remises et ristournes accordés', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('71', 'Subventions d''exploitation', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('72', 'Production immobilisée', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('75', 'Autres produits', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('758', 'Produits divers', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('759', 'Reprises de charges pour dépréciations et provisions à court terme', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('7594', 'Reprises de dépréciations des créances', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('77', 'Revenus financiers et produits assimilés', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('773', 'Escomptes obtenus', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('776', 'Gains de change', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('78', 'Transferts de charges', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('79', 'Reprises de provisions, de dépréciations et autres', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('791', 'Reprises de provisions et dépréciations d''exploitation', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('798', 'Reprises d''amortissements', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('799', 'Reprises de subventions d''investissement', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('8', 'Comptes des autres charges et des autres produits', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('81', 'Valeurs comptables des cessions d''immobilisations', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('811', 'Immobilisations incorporelles cédées', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('812', 'Immobilisations corporelles cédées', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('82', 'Produits des cessions d''immobilisations', 'HAO', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('821', 'Produits de cession d''immobilisations incorporelles', 'HAO', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('822', 'Produits de cession d''immobilisations corporelles', 'HAO', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('83', 'Charges hors activités ordinaires', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('84', 'Produits hors activités ordinaires', 'HAO', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('85', 'Dotations hors activités ordinaires', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('851', 'Dotations aux provisions réglementées', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('86', 'Reprises hors activités ordinaires', 'HAO', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('861', 'Reprises des provisions réglementées', 'HAO', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('87', 'Participation des travailleurs', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('89', 'Impôts sur le résultat', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('891', 'Impôts sur les bénéfices de l''exercice', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('9', 'Comptes des engagements hors bilan et de comptabilité analytique', 'OFF_BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('90', 'Engagements hors bilan', 'OFF_BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('901', 'Engagements donnés', 'OFF_BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('902', 'Contrepartie des engagements donnés', 'OFF_BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('905', 'Engagements reçus', 'OFF_BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('906', 'Contrepartie des engagements reçus', 'OFF_BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('91', 'Comptes réfléchis', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('92', 'Comptes de reclassements', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('93', 'Centres d''analyse', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('94', 'Stocks en comptabilité analytique', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('95', 'Coûts', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('96', 'Écarts sur coûts préétablis', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('97', 'Différences de traitement comptable', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('98', 'Résultats analytiques', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('99', 'Liaisons internes', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE)
)
INSERT INTO acc_reference_chart (number, label, class_code)
SELECT s.number, s.label, substr(s.number, 1, 1)
FROM seed s
WHERE NOT EXISTS (SELECT 1 FROM acc_reference_chart r WHERE r.number = s.number);

WITH seed (number, label, nature, normal_side, reconcilable, third_party_type, treasury_type, analytic_required) AS (
    VALUES
    ('1', 'Comptes de ressources durables', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('10', 'Capital', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('101', 'Capital social', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('104', 'Compte de l''exploitant', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('109', 'Apporteurs, capital souscrit non appelé', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('11', 'Réserves', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('111', 'Réserve légale', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('118', 'Autres réserves', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('12', 'Report à nouveau', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('121', 'Report à nouveau créditeur', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('129', 'Report à nouveau débiteur', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('13', 'Résultat net de l''exercice', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('131', 'Résultat net : bénéfice', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('139', 'Résultat net : perte', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('14', 'Subventions d''investissement', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('141', 'Subventions d''équipement', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('15', 'Provisions réglementées et fonds assimilés', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('151', 'Amortissements dérogatoires', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('16', 'Emprunts et dettes assimilées', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('162', 'Emprunts et dettes auprès des établissements de crédit', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('165', 'Dépôts et cautionnements reçus', 'BALANCE', 'CREDIT', TRUE, 'CUSTOMER', NULL, FALSE),
    ('166', 'Intérêts courus', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('17', 'Dettes de location-acquisition', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('19', 'Provisions pour risques et charges', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('191', 'Provisions pour litiges', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('192', 'Provisions pour garanties données aux clients', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('195', 'Provisions pour impôts', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('196', 'Provisions pour pensions et obligations similaires', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('198', 'Autres provisions pour risques et charges', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('2', 'Comptes d''actif immobilisé', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('21', 'Immobilisations incorporelles', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('211', 'Frais de développement', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('213', 'Logiciels et sites internet', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('22', 'Terrains', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('23', 'Bâtiments, installations techniques et agencements', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('231', 'Bâtiments sur sol propre', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('232', 'Bâtiments sur sol d''autrui', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('234', 'Aménagements, agencements et installations techniques', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('235', 'Aménagements de bureaux', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('239', 'Bâtiments et installations en cours', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('24', 'Matériel, mobilier et actifs biologiques', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('241', 'Matériel et outillage industriel et commercial', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('244', 'Matériel et mobilier', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('2441', 'Matériel de bureau', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('2442', 'Matériel informatique', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('2444', 'Mobilier de bureau', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('245', 'Matériel de transport', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('25', 'Avances et acomptes versés sur immobilisations', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('251', 'Avances et acomptes sur immobilisations incorporelles', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('252', 'Avances et acomptes sur immobilisations corporelles', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('27', 'Autres immobilisations financières', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('275', 'Dépôts et cautionnements versés', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('28', 'Amortissements', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('281', 'Amortissements des immobilisations incorporelles', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('283', 'Amortissements des bâtiments et aménagements', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('284', 'Amortissements du matériel', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('2844', 'Amortissements du matériel et mobilier', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('2845', 'Amortissements du matériel de transport', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('29', 'Dépréciations des immobilisations', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('3', 'Comptes de stocks', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('31', 'Marchandises', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('32', 'Matières premières et fournitures liées', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('33', 'Autres approvisionnements', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('39', 'Dépréciations des stocks', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4', 'Comptes de tiers', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('40', 'Fournisseurs et comptes rattachés', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('401', 'Fournisseurs, dettes en compte', 'BALANCE', 'CREDIT', TRUE, 'SUPPLIER', NULL, FALSE),
    ('408', 'Fournisseurs, factures non parvenues', 'BALANCE', 'CREDIT', TRUE, 'SUPPLIER', NULL, FALSE),
    ('409', 'Fournisseurs débiteurs', 'BALANCE', 'DEBIT', TRUE, 'SUPPLIER', NULL, FALSE),
    ('41', 'Clients et comptes rattachés', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('411', 'Clients', 'BALANCE', 'DEBIT', TRUE, 'CUSTOMER', NULL, FALSE),
    ('416', 'Créances clients litigieuses ou douteuses', 'BALANCE', 'DEBIT', FALSE, 'CUSTOMER', NULL, FALSE),
    ('4161', 'Créances litigieuses', 'BALANCE', 'DEBIT', TRUE, 'CUSTOMER', NULL, FALSE),
    ('4162', 'Créances douteuses', 'BALANCE', 'DEBIT', TRUE, 'CUSTOMER', NULL, FALSE),
    ('418', 'Clients, produits à recevoir', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('4181', 'Clients, factures à établir', 'BALANCE', 'DEBIT', TRUE, 'CUSTOMER', NULL, FALSE),
    ('419', 'Clients créditeurs', 'BALANCE', 'CREDIT', FALSE, 'CUSTOMER', NULL, FALSE),
    ('4191', 'Clients, avances et acomptes reçus', 'BALANCE', 'CREDIT', TRUE, 'CUSTOMER', NULL, FALSE),
    ('42', 'Personnel', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('421', 'Personnel, avances et acomptes', 'BALANCE', 'DEBIT', TRUE, 'EMPLOYEE', NULL, FALSE),
    ('422', 'Personnel, rémunérations dues', 'BALANCE', 'CREDIT', TRUE, 'EMPLOYEE', NULL, FALSE),
    ('428', 'Personnel, charges à payer et produits à recevoir', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('43', 'Organismes sociaux', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('431', 'Sécurité sociale', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('438', 'Organismes sociaux, charges à payer et produits à recevoir', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('44', 'État et collectivités publiques', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('441', 'État, impôt sur les bénéfices', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('443', 'État, TVA facturée', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4431', 'TVA facturée sur ventes', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4432', 'TVA facturée sur prestations de services', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('444', 'État, TVA due ou crédit de TVA', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4441', 'État, TVA due', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4449', 'État, crédit de TVA à reporter', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('445', 'État, TVA récupérable', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('4451', 'TVA récupérable sur immobilisations', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('4452', 'TVA récupérable sur achats', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('4454', 'TVA récupérable sur services extérieurs', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('447', 'État, impôts retenus à la source', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('448', 'État, charges à payer et produits à recevoir', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('46', 'Associés et groupe', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('462', 'Associés, comptes courants', 'BALANCE', 'CREDIT', TRUE, 'PARTNER', NULL, FALSE),
    ('47', 'Débiteurs et créditeurs divers', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('471', 'Débiteurs et créditeurs divers, comptes d''attente', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('4718', 'Écarts d''équilibrage à régulariser', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('4719', 'Encaissements à identifier', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('476', 'Charges constatées d''avance', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('477', 'Produits constatés d''avance', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('48', 'Créances et dettes hors activités ordinaires', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('481', 'Fournisseurs d''investissements', 'BALANCE', 'CREDIT', TRUE, 'SUPPLIER', NULL, FALSE),
    ('485', 'Créances sur cessions d''immobilisations', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('49', 'Dépréciations et provisions pour risques à court terme', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('491', 'Dépréciations des comptes clients', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4911', 'Dépréciations des créances litigieuses', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('4912', 'Dépréciations des créances douteuses', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('499', 'Provisions pour risques à court terme', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('5', 'Comptes de trésorerie', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('51', 'Valeurs à encaisser', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('513', 'Chèques à encaisser', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('52', 'Banques', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('521', 'Banques locales', 'BALANCE', 'DEBIT', FALSE, NULL, 'BANK', FALSE),
    ('57', 'Caisse', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('571', 'Caisse siège social', 'BALANCE', 'DEBIT', FALSE, NULL, 'CASH', FALSE),
    ('58', 'Régies d''avances, accréditifs et virements internes', 'BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('585', 'Virements de fonds', 'BALANCE', 'DEBIT', TRUE, NULL, NULL, FALSE),
    ('59', 'Dépréciations des titres de placement et de la trésorerie', 'BALANCE', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('6', 'Comptes de charges des activités ordinaires', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('60', 'Achats et variations de stocks', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('601', 'Achats de marchandises', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('603', 'Variations des stocks de biens achetés', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('604', 'Achats stockés de matières et fournitures consommables', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('605', 'Autres achats', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6051', 'Fournitures non stockables, eau', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6052', 'Fournitures non stockables, électricité', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6053', 'Fournitures non stockables, autres énergies', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6054', 'Fournitures d''entretien non stockables', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6055', 'Fournitures de bureau non stockables', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6056', 'Achats de petit matériel et outillage', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('61', 'Transports', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('614', 'Transports du personnel', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('618', 'Autres frais de transport', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6181', 'Voyages et déplacements', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('62', 'Services extérieurs', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('622', 'Locations et charges locatives', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('624', 'Entretien, réparations et maintenance', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('625', 'Primes d''assurance', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('626', 'Études, recherches et documentation', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6261', 'Frais postaux', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('627', 'Publicité, publications, relations publiques', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('628', 'Frais de télécommunications', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('63', 'Autres services extérieurs', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('631', 'Frais bancaires', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('632', 'Rémunérations d''intermédiaires et de conseils', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('638', 'Autres charges externes', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('64', 'Impôts et taxes', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('65', 'Autres charges', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('651', 'Pertes sur créances clients et autres débiteurs', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6511', 'Pertes sur créances clients', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('658', 'Charges diverses', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('659', 'Charges pour dépréciations et provisions à court terme d''exploitation', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6594', 'Charges pour dépréciations des créances', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('66', 'Charges de personnel', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('661', 'Rémunérations directes versées au personnel', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('664', 'Charges sociales', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('67', 'Frais financiers et charges assimilées', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('671', 'Intérêts des emprunts', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('673', 'Escomptes accordés', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('676', 'Pertes de change', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('68', 'Dotations aux amortissements', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('681', 'Dotations aux amortissements d''exploitation', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6812', 'Dotations aux amortissements des immobilisations incorporelles', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('6813', 'Dotations aux amortissements des immobilisations corporelles', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('69', 'Dotations aux provisions et aux dépréciations', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('691', 'Dotations aux provisions et dépréciations d''exploitation', 'MANAGEMENT', 'DEBIT', FALSE, NULL, NULL, TRUE),
    ('7', 'Comptes de produits des activités ordinaires', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('70', 'Ventes', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('701', 'Ventes de marchandises', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('706', 'Services vendus', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('7061', 'Locations d''espaces de travail', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('7062', 'Abonnements et pass', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('7063', 'Réservations de salles', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('7064', 'Domiciliation', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('707', 'Produits accessoires', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('709', 'Rabais, remises et ristournes accordés', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('71', 'Subventions d''exploitation', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('72', 'Production immobilisée', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('75', 'Autres produits', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('758', 'Produits divers', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('759', 'Reprises de charges pour dépréciations et provisions à court terme', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('7594', 'Reprises de dépréciations des créances', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('77', 'Revenus financiers et produits assimilés', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('773', 'Escomptes obtenus', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('776', 'Gains de change', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('78', 'Transferts de charges', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('79', 'Reprises de provisions, de dépréciations et autres', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('791', 'Reprises de provisions et dépréciations d''exploitation', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('798', 'Reprises d''amortissements', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('799', 'Reprises de subventions d''investissement', 'MANAGEMENT', 'CREDIT', FALSE, NULL, NULL, TRUE),
    ('8', 'Comptes des autres charges et des autres produits', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('81', 'Valeurs comptables des cessions d''immobilisations', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('811', 'Immobilisations incorporelles cédées', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('812', 'Immobilisations corporelles cédées', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('82', 'Produits des cessions d''immobilisations', 'HAO', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('821', 'Produits de cession d''immobilisations incorporelles', 'HAO', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('822', 'Produits de cession d''immobilisations corporelles', 'HAO', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('83', 'Charges hors activités ordinaires', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('84', 'Produits hors activités ordinaires', 'HAO', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('85', 'Dotations hors activités ordinaires', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('851', 'Dotations aux provisions réglementées', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('86', 'Reprises hors activités ordinaires', 'HAO', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('861', 'Reprises des provisions réglementées', 'HAO', 'CREDIT', FALSE, NULL, NULL, FALSE),
    ('87', 'Participation des travailleurs', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('89', 'Impôts sur le résultat', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('891', 'Impôts sur les bénéfices de l''exercice', 'HAO', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('9', 'Comptes des engagements hors bilan et de comptabilité analytique', 'OFF_BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('90', 'Engagements hors bilan', 'OFF_BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('901', 'Engagements donnés', 'OFF_BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('902', 'Contrepartie des engagements donnés', 'OFF_BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('905', 'Engagements reçus', 'OFF_BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('906', 'Contrepartie des engagements reçus', 'OFF_BALANCE', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('91', 'Comptes réfléchis', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('92', 'Comptes de reclassements', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('93', 'Centres d''analyse', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('94', 'Stocks en comptabilité analytique', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('95', 'Coûts', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('96', 'Écarts sur coûts préétablis', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('97', 'Différences de traitement comptable', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('98', 'Résultats analytiques', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE),
    ('99', 'Liaisons internes', 'ANALYTIC', 'DEBIT', FALSE, NULL, NULL, FALSE)
), numbered AS (
    SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.number) AS rank,
           (SELECT p.number FROM seed p
            WHERE s.number LIKE p.number || '%' AND p.number <> s.number
            ORDER BY length(p.number) DESC LIMIT 1) AS parent_number
    FROM seed s
    WHERE NOT EXISTS (SELECT 1 FROM acc_account a WHERE a.number = s.number)
)
INSERT INTO acc_account (id, number, label, parent_number, class_code, nature, normal_side, reconcilable, third_party_type,
                         treasury_type, analytic_required, statement_line_code, status, created_by)
SELECT 2960000 + rank, number, label, parent_number, substr(number, 1, 1), nature, normal_side, reconcilable, third_party_type,
       treasury_type, analytic_required, 'CLASSE_' || substr(number, 1, 1), 'ACTIVE', 'SYSTEM'
FROM numbered;

-- ------------------------------------------------------- exercice 2026, journaux

INSERT INTO acc_fiscal_year (id, code, start_date, end_date, status)
SELECT 2960001, '2026', DATE '2026-01-01', DATE '2026-12-31', 'OPEN'
WHERE NOT EXISTS (SELECT 1 FROM acc_fiscal_year WHERE code = '2026');

INSERT INTO acc_period (id, fiscal_year_id, code, start_date, end_date, status)
SELECT 2961000 + m, (SELECT id FROM acc_fiscal_year WHERE code = '2026'),
       '2026-' || lpad(m::text, 2, '0'),
       make_date(2026, m, 1),
       (make_date(2026, m, 1) + INTERVAL '1 month - 1 day')::date,
       'OPEN'
FROM generate_series(1, 12) AS m
WHERE NOT EXISTS (SELECT 1 FROM acc_period p WHERE p.code = '2026-' || lpad(m::text, 2, '0'));

WITH seed (code, label, type, auto_validate) AS (
    VALUES ('VT', 'Ventes', 'SALES', TRUE),
           ('AC', 'Achats', 'PURCHASES', TRUE),
           ('OD', 'Operations diverses', 'MISC', FALSE),
           ('PF', 'Portefeuille electronique', 'WALLET', TRUE),
           ('ST', 'Stock', 'STOCK', TRUE),
           ('AN', 'A-nouveaux', 'OPENING', FALSE),
           ('EXT', 'Extournes', 'REVERSAL', TRUE),
           ('HB', 'Engagements hors bilan', 'OFF_BALANCE', TRUE),
           ('CAG', 'Comptabilite analytique', 'ANALYTIC', TRUE),
           ('PA', 'Paie', 'PAYROLL', FALSE)
), numbered AS (
    SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.code) AS rank
    FROM seed s WHERE NOT EXISTS (SELECT 1 FROM acc_journal j WHERE j.code = s.code)
)
INSERT INTO acc_journal (id, code, label, type, auto_validate, created_by)
SELECT 2962000 + rank, code, label, type, auto_validate, 'SYSTEM' FROM numbered;

INSERT INTO sequence_definition (id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2960001, 'acc_provisional_entry', 'Piece comptable provisoire', 'Numero provisoire du brouillard et des pieces en attente (jamais definitif)', 'BR', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 7, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE lower(code) = 'acc_provisional_entry');

-- ------------------------------------------------------------------ permissions

WITH new_permission (name, display_name, module, action) AS (
    VALUES ('ACCOUNTING_REPORT_READ', 'Consulter la comptabilite (journaux, grand livre, balance)', 'ACCOUNTING_REPORT', 'READ'),
           ('ACCOUNTING_ENTRY_CREATE', 'Saisir des ecritures', 'ACCOUNTING_ENTRY', 'CREATE'),
           ('ACCOUNTING_ENTRY_VALIDATE', 'Valider des ecritures', 'ACCOUNTING_ENTRY', 'VALIDATE'),
           ('ACCOUNTING_ENTRY_REVERSE', 'Contre-passer des ecritures', 'ACCOUNTING_ENTRY', 'REVERSE'),
           ('ACCOUNTING_ENTRY_BALANCE', 'Equilibrer ou rejeter une piece en attente', 'ACCOUNTING_ENTRY', 'BALANCE'),
           ('ACCOUNTING_ACCOUNT_CREATE', 'Creer un compte divisionnaire', 'ACCOUNTING_ACCOUNT', 'CREATE'),
           ('ACCOUNTING_ACCOUNT_MANAGE', 'Gerer le plan de comptes', 'ACCOUNTING_ACCOUNT', 'MANAGE'),
           ('ACCOUNTING_JOURNAL_CREATE', 'Creer un journal', 'ACCOUNTING_JOURNAL', 'CREATE'),
           ('ACCOUNTING_JOURNAL_MANAGE', 'Gerer les journaux', 'ACCOUNTING_JOURNAL', 'MANAGE'),
           ('ACCOUNTING_PERIOD_LOCK', 'Verrouiller une periode comptable', 'ACCOUNTING_PERIOD', 'LOCK'),
           ('ACCOUNTING_PERIOD_CLOSE', 'Clore une periode ou un exercice', 'ACCOUNTING_PERIOD', 'CLOSE'),
           ('ACCOUNTING_LETTERING_MANUAL', 'Lettrer a la main', 'ACCOUNTING_LETTERING', 'MANUAL'),
           ('ACCOUNTING_LETTERING_UNLETTER', 'Delettrer', 'ACCOUNTING_LETTERING', 'UNLETTER'),
           ('ACCOUNTING_SETTINGS_MANAGE', 'Parametrer la comptabilite', 'ACCOUNTING_SETTINGS', 'MANAGE')
), numbered AS (
    SELECT n.*, ROW_NUMBER() OVER (ORDER BY n.name) AS rank
    FROM new_permission n
    WHERE NOT EXISTS (SELECT 1 FROM permission p WHERE p.name = n.name)
)
INSERT INTO permission (id, name, display_name, module, action, is_system_permission, is_active)
SELECT COALESCE((SELECT MAX(id) FROM permission), 0) + rank, name, display_name, module, action, TRUE, TRUE
FROM numbered;

-- Administrateurs : tout. Finance : comptable (tout sauf le parametrage et la gestion du plan).
-- Direction (MANAGER) : lecture.
WITH grants (role_name, permission_name) AS (
    SELECT r, p FROM (VALUES ('SUPER_ADMIN'), ('ADMIN')) AS roles(r)
    CROSS JOIN (VALUES ('ACCOUNTING_REPORT_READ'), ('ACCOUNTING_ENTRY_CREATE'), ('ACCOUNTING_ENTRY_VALIDATE'),
                       ('ACCOUNTING_ENTRY_REVERSE'), ('ACCOUNTING_ENTRY_BALANCE'), ('ACCOUNTING_ACCOUNT_CREATE'),
                       ('ACCOUNTING_ACCOUNT_MANAGE'), ('ACCOUNTING_JOURNAL_CREATE'), ('ACCOUNTING_JOURNAL_MANAGE'),
                       ('ACCOUNTING_PERIOD_LOCK'), ('ACCOUNTING_PERIOD_CLOSE'), ('ACCOUNTING_LETTERING_MANUAL'),
                       ('ACCOUNTING_LETTERING_UNLETTER'), ('ACCOUNTING_SETTINGS_MANAGE')) AS perms(p)
    UNION ALL
    SELECT 'FINANCE', p FROM (VALUES ('ACCOUNTING_REPORT_READ'), ('ACCOUNTING_ENTRY_CREATE'), ('ACCOUNTING_ENTRY_VALIDATE'),
                                     ('ACCOUNTING_ENTRY_REVERSE'), ('ACCOUNTING_ENTRY_BALANCE'), ('ACCOUNTING_ACCOUNT_CREATE'),
                                     ('ACCOUNTING_JOURNAL_CREATE'), ('ACCOUNTING_PERIOD_LOCK'), ('ACCOUNTING_LETTERING_MANUAL'),
                                     ('ACCOUNTING_LETTERING_UNLETTER')) AS perms(p)
    UNION ALL
    SELECT 'MANAGER', 'ACCOUNTING_REPORT_READ'
)
INSERT INTO role_permission (role_id, permission_id, created_by, is_active)
SELECT r.id, p.id, 'SYSTEM', TRUE
FROM grants g
JOIN role r ON r.name = g.role_name
JOIN permission p ON p.name = g.permission_name
WHERE NOT EXISTS (SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

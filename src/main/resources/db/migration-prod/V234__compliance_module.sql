-- Conformite et surveillance.
--
-- Detecter n'est pas surveiller. Un signalement qui n'ouvre pas un dossier, qu'aucune personne
-- nommee ne doit traiter, et dont la cloture n'est pas datee, ne protege de rien : il documente
-- seulement, apres coup, que le systeme savait. Ce module fait le reste · des regles qui sont des
-- donnees, des dossiers qui ont un responsable et une echeance, une piste d'audit qu'on ne peut
-- pas reecrire, et la preuve des controles qu'on a faits.

-- ---------------------------------------------------------------------------------------------
-- 1. Regles de detection · des donnees, pas du code
-- ---------------------------------------------------------------------------------------------
--
-- Un seuil qui se change par deploiement ne se change jamais. Chaque regle porte ses parametres ;
-- le code ne porte que la facon de les lire. Les compteurs de succes et de bruit sont tenus ici
-- parce que c'est la mesure qui garde le module vivant : une regle qui ne produit que du bruit
-- finit par etre ignoree, et son bruit couvre les signaux qui comptaient.

CREATE TABLE IF NOT EXISTS compliance_rule (
    id BIGINT PRIMARY KEY,
    rule_code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(40) NOT NULL,
    detector VARCHAR(60) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    action VARCHAR(40) NOT NULL DEFAULT 'FLAG',
    amount_threshold NUMERIC(19,4),
    count_threshold INTEGER,
    ratio_threshold NUMERIC(9,4),
    window_minutes INTEGER,
    parameters_json TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMPTZ,
    effective_to TIMESTAMPTZ,
    hit_count BIGINT NOT NULL DEFAULT 0,
    confirmed_count BIGINT NOT NULL DEFAULT 0,
    dismissed_count BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(120),
    approved_by VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_compliance_rule_counts CHECK (hit_count >= 0 AND confirmed_count >= 0 AND dismissed_count >= 0)
);

COMMENT ON COLUMN compliance_rule.detector IS
    'Nom du detecteur Java qui lit les parametres · VELOCITY_COUNT, VELOCITY_AMOUNT, STRUCTURING, RAPID_IN_OUT, FAN_OUT, FAN_IN, CIRCULARITY, DORMANT_AMOUNT, IDENTITY_LIMIT, SHARED_DEVICE';
COMMENT ON COLUMN compliance_rule.action IS
    'FLAG signale · REQUIRE_REVIEW ouvre un dossier · BLOCK_OPERATION refuse avant execution · FREEZE_WALLET gele le portefeuille';

INSERT INTO compliance_rule (id, rule_code, name, description, category, detector, severity, action, amount_threshold, count_threshold, ratio_threshold, window_minutes, created_by, approved_by)
SELECT 2380001, 'CR-VELOCITY-COUNT', 'Trop d operations en peu de temps', 'Plus de N operations sortantes sur une fenetre courte', 'VELOCITY', 'VELOCITY_COUNT', 'MEDIUM', 'FLAG', NULL, 8, NULL, 60, 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM compliance_rule WHERE rule_code = 'CR-VELOCITY-COUNT');

INSERT INTO compliance_rule (id, rule_code, name, description, category, detector, severity, action, amount_threshold, count_threshold, ratio_threshold, window_minutes, created_by, approved_by)
SELECT 2380002, 'CR-VELOCITY-AMOUNT', 'Montant cumule eleve en peu de temps', 'Cumul des sorties au-dela du seuil sur la fenetre', 'VELOCITY', 'VELOCITY_AMOUNT', 'HIGH', 'REQUIRE_REVIEW', 1500000, NULL, NULL, 1440, 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM compliance_rule WHERE rule_code = 'CR-VELOCITY-AMOUNT');

INSERT INTO compliance_rule (id, rule_code, name, description, category, detector, severity, action, amount_threshold, count_threshold, ratio_threshold, window_minutes, created_by, approved_by)
SELECT 2380003, 'CR-STRUCTURING', 'Fractionnement sous un seuil', 'Plusieurs transferts juste sous le plafond unitaire, ce que tout seuil finit par produire', 'PATTERN', 'STRUCTURING', 'HIGH', 'REQUIRE_REVIEW', 300000, 3, 0.8, 1440, 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM compliance_rule WHERE rule_code = 'CR-STRUCTURING');

INSERT INTO compliance_rule (id, rule_code, name, description, category, detector, severity, action, amount_threshold, count_threshold, ratio_threshold, window_minutes, created_by, approved_by)
SELECT 2380004, 'CR-RAPID-IN-OUT', 'Aller-retour · le portefeuille sert de tuyau', 'Rechargement ou reception puis transfert sortant de la majeure partie dans la foulee', 'PATTERN', 'RAPID_IN_OUT', 'HIGH', 'REQUIRE_REVIEW', 100000, NULL, 0.8, 120, 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM compliance_rule WHERE rule_code = 'CR-RAPID-IN-OUT');

INSERT INTO compliance_rule (id, rule_code, name, description, category, detector, severity, action, amount_threshold, count_threshold, ratio_threshold, window_minutes, created_by, approved_by)
SELECT 2380005, 'CR-FAN-OUT', 'Un emetteur qui arrose', 'Transferts vers trop de destinataires distincts sur la fenetre', 'COUNTERPARTY', 'FAN_OUT', 'MEDIUM', 'FLAG', NULL, 6, NULL, 1440, 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM compliance_rule WHERE rule_code = 'CR-FAN-OUT');

INSERT INTO compliance_rule (id, rule_code, name, description, category, detector, severity, action, amount_threshold, count_threshold, ratio_threshold, window_minutes, created_by, approved_by)
SELECT 2380006, 'CR-FAN-IN', 'Un destinataire qui collecte', 'Receptions depuis trop d emetteurs distincts sur la fenetre', 'COUNTERPARTY', 'FAN_IN', 'HIGH', 'REQUIRE_REVIEW', NULL, 6, NULL, 1440, 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM compliance_rule WHERE rule_code = 'CR-FAN-IN');

INSERT INTO compliance_rule (id, rule_code, name, description, category, detector, severity, action, amount_threshold, count_threshold, ratio_threshold, window_minutes, created_by, approved_by)
SELECT 2380007, 'CR-CIRCULARITY', 'Circularite', 'A vers B vers C vers A sur la fenetre, qui ne sert qu a brouiller l origine', 'PATTERN', 'CIRCULARITY', 'HIGH', 'REQUIRE_REVIEW', NULL, NULL, NULL, 4320, 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM compliance_rule WHERE rule_code = 'CR-CIRCULARITY');

INSERT INTO compliance_rule (id, rule_code, name, description, category, detector, severity, action, amount_threshold, count_threshold, ratio_threshold, window_minutes, created_by, approved_by)
SELECT 2380008, 'CR-DORMANT-AMOUNT', 'Reveil avec un montant sans rapport', 'Compte dormant redevenu actif avec une sortie tres au-dessus de son historique', 'PATTERN', 'DORMANT_AMOUNT', 'HIGH', 'REQUIRE_REVIEW', 50000, NULL, 3.0, 1440, 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM compliance_rule WHERE rule_code = 'CR-DORMANT-AMOUNT');

INSERT INTO compliance_rule (id, rule_code, name, description, category, detector, severity, action, amount_threshold, count_threshold, ratio_threshold, window_minutes, created_by, approved_by)
SELECT 2380009, 'CR-IDENTITY-LIMIT', 'Au-dela de ce que le niveau de verification autorise', 'Tentatives repetees au-dela du plafond du niveau · une demande de pieces vaut mieux qu un refus sec', 'IDENTITY', 'IDENTITY_LIMIT', 'MEDIUM', 'FLAG', NULL, 3, NULL, 1440, 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM compliance_rule WHERE rule_code = 'CR-IDENTITY-LIMIT');

INSERT INTO compliance_rule (id, rule_code, name, description, category, detector, severity, action, amount_threshold, count_threshold, ratio_threshold, window_minutes, created_by, approved_by)
SELECT 2380010, 'CR-SHARED-DEVICE', 'Un appareil pour plusieurs portefeuilles', 'Le meme appareil pilote des portefeuilles sans lien entre eux', 'IDENTITY', 'SHARED_DEVICE', 'HIGH', 'REQUIRE_REVIEW', NULL, 3, NULL, 43200, 'SYSTEM', 'SYSTEM'
WHERE NOT EXISTS (SELECT 1 FROM compliance_rule WHERE rule_code = 'CR-SHARED-DEVICE');

-- Le signalement sait desormais quelle regle l'a leve.
ALTER TABLE wallet_risk_flag
    ADD COLUMN IF NOT EXISTS rule_code VARCHAR(60),
    ADD COLUMN IF NOT EXISTS case_number VARCHAR(100);

-- ---------------------------------------------------------------------------------------------
-- 2. Le dossier, pas seulement l'alerte
-- ---------------------------------------------------------------------------------------------
--
-- Un dossier a un responsable, une echeance et une decision motivee. Une decision sans motif ecrit
-- n'est pas une decision, c'est un classement · la base refuse la cloture sans motif.

CREATE TABLE IF NOT EXISTS compliance_case (
    id BIGINT PRIMARY KEY,
    case_number VARCHAR(100) NOT NULL UNIQUE,
    subject_type VARCHAR(40) NOT NULL,
    subject_code VARCHAR(120) NOT NULL,
    wallet_id BIGINT,
    title VARCHAR(255) NOT NULL,
    triggered_by_flags TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    assigned_to VARCHAR(120),
    assigned_at TIMESTAMPTZ,
    due_at TIMESTAMPTZ NOT NULL,
    findings TEXT,
    decision VARCHAR(40),
    decision_rationale TEXT,
    decided_by VARCHAR(120),
    decided_at TIMESTAMPTZ,
    escalated_to VARCHAR(120),
    escalated_at TIMESTAMPTZ,
    opened_by VARCHAR(120) NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_compliance_case_wallet FOREIGN KEY (wallet_id) REFERENCES wallet_account(id),
    CONSTRAINT ck_compliance_case_decision CHECK (
        status NOT LIKE 'CLOSED_%'
        OR (decision IS NOT NULL AND decision_rationale IS NOT NULL AND length(trim(decision_rationale)) >= 10
            AND decided_by IS NOT NULL AND decided_at IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_compliance_case_open ON compliance_case(status, priority, due_at);
CREATE INDEX IF NOT EXISTS idx_compliance_case_subject ON compliance_case(subject_type, subject_code);
CREATE INDEX IF NOT EXISTS idx_compliance_case_assignee ON compliance_case(assigned_to, status);

CREATE TABLE IF NOT EXISTS compliance_case_note (
    id BIGINT PRIMARY KEY,
    case_id BIGINT NOT NULL,
    author VARCHAR(120) NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_compliance_note_case FOREIGN KEY (case_id) REFERENCES compliance_case(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_compliance_note_case ON compliance_case_note(case_id, created_at);

-- ---------------------------------------------------------------------------------------------
-- 3. Listes · le controle et la preuve qu'il a eu lieu
-- ---------------------------------------------------------------------------------------------
--
-- Le point technique qui compte n'est pas le controle mais la conservation de sa preuve : quelle
-- liste, quelle version, quelle date, quel resultat. Un controle dont on ne peut pas montrer qu'il
-- a eu lieu n'a, en pratique, pas eu lieu.

CREATE TABLE IF NOT EXISTS screening_list_entry (
    id BIGINT PRIMARY KEY,
    list_code VARCHAR(60) NOT NULL,
    list_version VARCHAR(60) NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    aliases TEXT,
    birth_year INTEGER,
    nationality VARCHAR(3),
    reference VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    loaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    loaded_by VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_screening_entry_name ON screening_list_entry(normalized_name);
CREATE INDEX IF NOT EXISTS idx_screening_entry_list ON screening_list_entry(list_code, list_version, active);

COMMENT ON COLUMN screening_list_entry.entry_type IS 'SANCTION ou PEP';

CREATE TABLE IF NOT EXISTS screening_check (
    id BIGINT PRIMARY KEY,
    check_number VARCHAR(100) NOT NULL UNIQUE,
    subject_type VARCHAR(40) NOT NULL,
    subject_code VARCHAR(120) NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    trigger_reason VARCHAR(60) NOT NULL,
    lists_checked TEXT NOT NULL,
    result VARCHAR(20) NOT NULL,
    matched_entry_ids TEXT,
    match_details TEXT,
    checked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    checked_by VARCHAR(120) NOT NULL DEFAULT 'SYSTEM',
    case_number VARCHAR(100),
    reviewed_by VARCHAR(120),
    reviewed_at TIMESTAMPTZ,
    review_outcome VARCHAR(40)
);

CREATE INDEX IF NOT EXISTS idx_screening_check_subject ON screening_check(subject_type, subject_code, checked_at DESC);
CREATE INDEX IF NOT EXISTS idx_screening_check_result ON screening_check(result, checked_at DESC);

COMMENT ON COLUMN screening_check.lists_checked IS 'Codes et versions des listes consultees, tels qu au moment du controle · la preuve';
COMMENT ON COLUMN screening_check.result IS 'CLEAR, POSSIBLE_MATCH ou MATCH';

-- Un controle est une preuve · il ne se modifie pas, sauf pour porter sa revue.
CREATE OR REPLACE FUNCTION screening_check_guard()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Un controle de liste est une preuve · il ne se supprime pas' USING ERRCODE = '23514';
    END IF;
    IF NEW.subject_code <> OLD.subject_code OR NEW.lists_checked <> OLD.lists_checked
       OR NEW.result <> OLD.result OR NEW.checked_at <> OLD.checked_at
       OR NEW.matched_entry_ids IS DISTINCT FROM OLD.matched_entry_ids THEN
        RAISE EXCEPTION 'Le resultat d un controle de liste ne se reecrit pas · seule sa revue s ajoute' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_screening_check_guard ON screening_check;
CREATE TRIGGER trg_screening_check_guard
    BEFORE UPDATE OR DELETE ON screening_check
    FOR EACH ROW EXECUTE FUNCTION screening_check_guard();

-- ---------------------------------------------------------------------------------------------
-- 4. Piste d'audit chainee, conservee dix ans
-- ---------------------------------------------------------------------------------------------
--
-- Chainee comme le grand livre des portefeuilles. La duree de conservation n'est pas negociable et
-- se pose des la conception : une purge ecrite trop tot est irreversible. Il n'y a donc pas de
-- purge · seulement une date en dessous de laquelle aucune ne serait admissible.

CREATE TABLE IF NOT EXISTS compliance_audit_entry (
    id BIGINT PRIMARY KEY,
    entry_uuid UUID NOT NULL UNIQUE,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor VARCHAR(120) NOT NULL,
    actor_role VARCHAR(60),
    action VARCHAR(60) NOT NULL,
    subject_type VARCHAR(40) NOT NULL,
    subject_code VARCHAR(120) NOT NULL,
    before_state TEXT,
    after_state TEXT,
    rationale TEXT,
    previous_hash VARCHAR(128) NOT NULL,
    current_hash VARCHAR(128) NOT NULL,
    retain_until DATE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_compliance_audit_subject ON compliance_audit_entry(subject_type, subject_code, occurred_at);
CREATE INDEX IF NOT EXISTS idx_compliance_audit_time ON compliance_audit_entry(occurred_at);

COMMENT ON COLUMN compliance_audit_entry.retain_until IS
    'Dix ans apres l evenement · aucune purge admissible avant cette date, et aucune n est ecrite';

CREATE OR REPLACE FUNCTION compliance_audit_is_append_only()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'La piste d audit de conformite ne se modifie pas et ne se supprime pas' USING ERRCODE = '23514';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_compliance_audit_append_only ON compliance_audit_entry;
CREATE TRIGGER trg_compliance_audit_append_only
    BEFORE UPDATE OR DELETE ON compliance_audit_entry
    FOR EACH ROW EXECUTE FUNCTION compliance_audit_is_append_only();

-- ---------------------------------------------------------------------------------------------
-- 5. Gel sur instruction · distinct de la suspension commerciale
-- ---------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS compliance_freeze (
    id BIGINT PRIMARY KEY,
    freeze_number VARCHAR(100) NOT NULL UNIQUE,
    wallet_id BIGINT NOT NULL,
    authority VARCHAR(255) NOT NULL,
    instruction_reference VARCHAR(255) NOT NULL,
    instruction_date DATE,
    rationale TEXT NOT NULL,
    frozen_by VARCHAR(120) NOT NULL,
    frozen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    lifted_by VARCHAR(120),
    lifted_at TIMESTAMPTZ,
    lift_reference VARCHAR(255),
    lift_rationale TEXT,
    case_number VARCHAR(100),
    CONSTRAINT fk_compliance_freeze_wallet FOREIGN KEY (wallet_id) REFERENCES wallet_account(id),
    CONSTRAINT ck_compliance_freeze_lift CHECK (lifted_at IS NULL OR (lifted_by IS NOT NULL AND lift_rationale IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_compliance_freeze_wallet ON compliance_freeze(wallet_id, lifted_at);

-- Un seul gel actif par portefeuille · deux instructions se cumulent dans le dossier, pas ici.
CREATE UNIQUE INDEX IF NOT EXISTS uk_compliance_freeze_active ON compliance_freeze(wallet_id) WHERE lifted_at IS NULL;

-- ---------------------------------------------------------------------------------------------
-- 6. Reverification periodique
-- ---------------------------------------------------------------------------------------------

ALTER TABLE wallet_account
    ADD COLUMN IF NOT EXISTS identity_review_due_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS identity_reviewed_at TIMESTAMPTZ;

-- ---------------------------------------------------------------------------------------------
-- 7. Sequences
-- ---------------------------------------------------------------------------------------------

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2380101, 'compliance_case', 'Dossier de conformite', 'Sequence des dossiers de conformite', 'CCS', null, '{PREFIX}-{YYYY}-{SEQ}', 6, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'compliance_case');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2380102, 'screening_check', 'Controle de liste', 'Sequence des controles de listes', 'SCR', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 6, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'screening_check');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2380103, 'compliance_freeze', 'Gel sur instruction', 'Sequence des gels sur instruction', 'CFZ', null, '{PREFIX}-{YYYY}-{SEQ}', 5, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'compliance_freeze');

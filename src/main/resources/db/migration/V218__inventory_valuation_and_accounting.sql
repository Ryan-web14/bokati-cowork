-- ============================================================
-- Lot 2 · Valorisation et comptabilite matiere
-- ============================================================
-- Methode de valorisation parametrable (cout moyen pondere ou FIFO), couches de
-- cout, periodes comptables, photos de valorisation, ecritures de stock et motifs
-- d'ajustement.
--
-- Regle de non-regression : valuation_method reste NULL partout apres cette
-- migration. Un article qui ne declare rien retombe sur le cout moyen pondere et
-- se comporte exactement comme avant. Aucune couche de cout n'est creee ici.

-- 1. Methode de valorisation

ALTER TABLE inventory_category
    ADD COLUMN IF NOT EXISTS valuation_method VARCHAR(30);

ALTER TABLE inventory_item
    ADD COLUMN IF NOT EXISTS valuation_method VARCHAR(30),
    ADD COLUMN IF NOT EXISTS valuation_method_since TIMESTAMP(6) WITH TIME ZONE;

-- 2. Couches de cout, alimentees uniquement pour les articles en FIFO

CREATE TABLE IF NOT EXISTS stock_cost_layer (
    id BIGINT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    initial_quantity NUMERIC(19, 4) NOT NULL,
    remaining_quantity NUMERIC(19, 4) NOT NULL,
    unit_cost BIGINT NOT NULL,
    received_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    source_movement_code VARCHAR(90),
    lot_number VARCHAR(120),
    seeded BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_stock_cost_layer_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    CONSTRAINT fk_stock_cost_layer_location FOREIGN KEY (location_id) REFERENCES inventory_location (id),
    CONSTRAINT chk_stock_cost_layer_quantities
        CHECK (initial_quantity > 0 AND remaining_quantity >= 0 AND remaining_quantity <= initial_quantity),
    CONSTRAINT chk_stock_cost_layer_cost CHECK (unit_cost >= 0)
);

-- Ordre de consommation FIFO : le plus ancien d'abord, l'identifiant departageant les ex aequo.
CREATE INDEX IF NOT EXISTS idx_stock_cost_layer_consumption
    ON stock_cost_layer (item_id, location_id, received_at, id)
    WHERE remaining_quantity > 0;

CREATE INDEX IF NOT EXISTS idx_stock_cost_layer_movement
    ON stock_cost_layer (source_movement_code);

-- 3. Periodes comptables

CREATE TABLE IF NOT EXISTS inventory_period (
    id BIGINT PRIMARY KEY,
    period_code VARCHAR(40) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    closed_by VARCHAR(120),
    closed_at TIMESTAMP(6) WITH TIME ZONE,
    reopened_by VARCHAR(120),
    reopened_at TIMESTAMP(6) WITH TIME ZONE,
    reopen_reason TEXT,
    closing_stock_value BIGINT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_period_code UNIQUE (period_code),
    CONSTRAINT chk_inventory_period_range CHECK (end_date >= start_date)
);

-- Deux periodes ne peuvent pas se chevaucher.
CREATE INDEX IF NOT EXISTS idx_inventory_period_range ON inventory_period (start_date, end_date);

-- 4. Photos de valorisation

CREATE TABLE IF NOT EXISTS stock_valuation_snapshot (
    id BIGINT PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    period_code VARCHAR(40),
    item_code VARCHAR(80) NOT NULL,
    location_code VARCHAR(80) NOT NULL,
    quantity_on_hand NUMERIC(19, 4) NOT NULL,
    unit_cost BIGINT,
    total_value BIGINT NOT NULL,
    valuation_method VARCHAR(30) NOT NULL,
    oldest_layer_age_days INTEGER,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_stock_snapshot_unique UNIQUE (snapshot_date, item_code, location_code)
);

CREATE INDEX IF NOT EXISTS idx_stock_snapshot_period ON stock_valuation_snapshot (period_code);

-- 5. Ecritures comptables de stock

CREATE TABLE IF NOT EXISTS stock_journal_entry (
    id BIGINT PRIMARY KEY,
    movement_code VARCHAR(90) NOT NULL,
    item_code VARCHAR(80) NOT NULL,
    location_code VARCHAR(80),
    stock_account VARCHAR(40) NOT NULL,
    counterpart_account VARCHAR(40) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    accounting_date DATE NOT NULL,
    period_code VARCHAR(40),
    label VARCHAR(255),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_stock_journal_movement UNIQUE (movement_code),
    CONSTRAINT chk_stock_journal_amount CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_stock_journal_date ON stock_journal_entry (accounting_date);
CREATE INDEX IF NOT EXISTS idx_stock_journal_period ON stock_journal_entry (period_code);

-- 6. Motifs d'ajustement et seuils d'approbation

CREATE TABLE IF NOT EXISTS inventory_adjustment_reason (
    id BIGINT PRIMARY KEY,
    reason_code VARCHAR(40) NOT NULL,
    label VARCHAR(160) NOT NULL,
    counterpart_account VARCHAR(40) NOT NULL,
    negative_only BOOLEAN NOT NULL DEFAULT FALSE,
    positive_only BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_adjustment_reason_code UNIQUE (reason_code),
    CONSTRAINT chk_adjustment_reason_sense CHECK (NOT (negative_only AND positive_only))
);

CREATE TABLE IF NOT EXISTS inventory_adjustment_approval_rule (
    id BIGINT PRIMARY KEY,
    approval_level VARCHAR(40) NOT NULL,
    min_amount BIGINT NOT NULL,
    max_amount BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_adjustment_rule_range CHECK (max_amount IS NULL OR max_amount >= min_amount)
);

-- 7. Rattachement du motif codifie au mouvement

ALTER TABLE stock_movement
    ADD COLUMN IF NOT EXISTS adjustment_reason_code VARCHAR(40);

-- 8. Mise a jour du declencheur d'immuabilite
--    La colonne ajoutee ci-dessus doit etre figee au meme titre que les autres, sans quoi
--    le motif comptable d'un ajustement resterait reecrivable apres coup.

CREATE OR REPLACE FUNCTION fn_stock_movement_check_immutable()
RETURNS TRIGGER AS $$
BEGIN
    IF (
           NEW.id                      IS DISTINCT FROM OLD.id
        OR NEW.movement_code           IS DISTINCT FROM OLD.movement_code
        OR NEW.item_id                 IS DISTINCT FROM OLD.item_id
        OR NEW.location_from_id        IS DISTINCT FROM OLD.location_from_id
        OR NEW.location_to_id          IS DISTINCT FROM OLD.location_to_id
        OR NEW.movement_type           IS DISTINCT FROM OLD.movement_type
        OR NEW.quantity                IS DISTINCT FROM OLD.quantity
        OR NEW.unit_cost               IS DISTINCT FROM OLD.unit_cost
        OR NEW.total_cost              IS DISTINCT FROM OLD.total_cost
        OR NEW.reference_type          IS DISTINCT FROM OLD.reference_type
        OR NEW.reference_code          IS DISTINCT FROM OLD.reference_code
        OR NEW.reason                  IS DISTINCT FROM OLD.reason
        OR NEW.reason_code             IS DISTINCT FROM OLD.reason_code
        OR NEW.adjustment_reason_code  IS DISTINCT FROM OLD.adjustment_reason_code
        OR NEW.allow_negative_override IS DISTINCT FROM OLD.allow_negative_override
        OR NEW.performed_by            IS DISTINCT FROM OLD.performed_by
        OR NEW.performed_at            IS DISTINCT FROM OLD.performed_at
    ) THEN
        RAISE EXCEPTION
            'Le mouvement de stock % est immuable. Seule la contre-passation est autorisee : '
            'creez un mouvement inverse via reverseMovement() au lieu de modifier celui-ci.',
            OLD.movement_code;
    END IF;

    IF OLD.reversed = TRUE AND NEW.reversed = FALSE THEN
        RAISE EXCEPTION
            'Le mouvement de stock % est deja contre-passe. Le retour en arriere est interdit.',
            OLD.movement_code;
    END IF;

    IF OLD.reversal_of_movement_id IS NOT NULL
       AND NEW.reversal_of_movement_id IS DISTINCT FROM OLD.reversal_of_movement_id THEN
        RAISE EXCEPTION
            'Le rattachement de contre-passation du mouvement % est fige.',
            OLD.movement_code;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 9. Motifs d'ajustement de base
--    Comptes SYSCOHADA usuels. A ajuster selon le plan comptable reel de l'entreprise.

INSERT INTO inventory_adjustment_reason
    (id, reason_code, label, counterpart_account, negative_only, positive_only, active, created_at, updated_at)
SELECT 2180001, 'COUNT_VARIANCE', 'Ecart d''inventaire physique', '6031', FALSE, FALSE, TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventory_adjustment_reason WHERE reason_code = 'COUNT_VARIANCE');

INSERT INTO inventory_adjustment_reason
    (id, reason_code, label, counterpart_account, negative_only, positive_only, active, created_at, updated_at)
SELECT 2180002, 'BREAKAGE', 'Casse', '6581', TRUE, FALSE, TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventory_adjustment_reason WHERE reason_code = 'BREAKAGE');

INSERT INTO inventory_adjustment_reason
    (id, reason_code, label, counterpart_account, negative_only, positive_only, active, created_at, updated_at)
SELECT 2180003, 'LOSS', 'Perte', '6581', TRUE, FALSE, TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventory_adjustment_reason WHERE reason_code = 'LOSS');

INSERT INTO inventory_adjustment_reason
    (id, reason_code, label, counterpart_account, negative_only, positive_only, active, created_at, updated_at)
SELECT 2180004, 'THEFT', 'Vol', '6581', TRUE, FALSE, TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventory_adjustment_reason WHERE reason_code = 'THEFT');

INSERT INTO inventory_adjustment_reason
    (id, reason_code, label, counterpart_account, negative_only, positive_only, active, created_at, updated_at)
SELECT 2180005, 'FOUND', 'Excedent constate', '7581', FALSE, TRUE, TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventory_adjustment_reason WHERE reason_code = 'FOUND');

INSERT INTO inventory_adjustment_reason
    (id, reason_code, label, counterpart_account, negative_only, positive_only, active, created_at, updated_at)
SELECT 2180006, 'DATA_CORRECTION', 'Correction de saisie', '6031', FALSE, FALSE, TRUE, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM inventory_adjustment_reason WHERE reason_code = 'DATA_CORRECTION');

-- 10. Sequences metier du lot

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2180101, 'inventory_period', 'Periode inventaire', 'Sequence des periodes comptables de stock', 'PER', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'inventory_period');

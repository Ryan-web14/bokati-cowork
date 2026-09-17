-- ============================================================
-- Lot 3 · Controle qualite, non-conformites et rappels
-- ============================================================
-- Plans de controle, inspections, non-conformites et rappels produit.
--
-- Regle de non-regression : aucun plan de controle n'est cree par cette migration.
-- Sans plan declare, aucune mise en quarantaine automatique n'a lieu, et les
-- receptions se comportent exactement comme avant.

-- 1. Plans de controle et criteres

CREATE TABLE IF NOT EXISTS quality_control_plan (
    id BIGINT PRIMARY KEY,
    plan_code VARCHAR(80) NOT NULL,
    name VARCHAR(220) NOT NULL,
    item_id BIGINT,
    category_id BIGINT,
    control_stage VARCHAR(40) NOT NULL,
    sampling_mode VARCHAR(40) NOT NULL,
    sampling_parameter NUMERIC(19, 4),
    decision_on_fail VARCHAR(40) NOT NULL DEFAULT 'QUARANTINED',
    quarantine_on_receipt BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_quality_plan_code UNIQUE (plan_code),
    CONSTRAINT fk_quality_plan_item FOREIGN KEY (item_id) REFERENCES inventory_item (id) ON DELETE CASCADE,
    CONSTRAINT fk_quality_plan_category FOREIGN KEY (category_id) REFERENCES inventory_category (id) ON DELETE CASCADE,
    -- Un plan vise soit un article, soit une categorie, jamais les deux ni aucun des deux.
    CONSTRAINT chk_quality_plan_target
        CHECK ((item_id IS NOT NULL AND category_id IS NULL) OR (item_id IS NULL AND category_id IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_quality_plan_item ON quality_control_plan (item_id, control_stage) WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_quality_plan_category ON quality_control_plan (category_id, control_stage) WHERE active = TRUE;

CREATE TABLE IF NOT EXISTS quality_criterion (
    id BIGINT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    criterion_code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    unit VARCHAR(40),
    min_value NUMERIC(19, 4),
    max_value NUMERIC(19, 4),
    boolean_expected BOOLEAN NOT NULL DEFAULT FALSE,
    blocking BOOLEAN NOT NULL DEFAULT TRUE,
    position INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT uk_quality_criterion_plan_code UNIQUE (plan_id, criterion_code),
    CONSTRAINT fk_quality_criterion_plan FOREIGN KEY (plan_id) REFERENCES quality_control_plan (id) ON DELETE CASCADE,
    CONSTRAINT chk_quality_criterion_range CHECK (min_value IS NULL OR max_value IS NULL OR max_value >= min_value)
);

-- 2. Inspections

CREATE TABLE IF NOT EXISTS quality_inspection (
    id BIGINT PRIMARY KEY,
    inspection_code VARCHAR(80) NOT NULL,
    plan_id BIGINT,
    item_id BIGINT NOT NULL,
    lot_id BIGINT,
    location_id BIGINT,
    source_type VARCHAR(60),
    source_code VARCHAR(120),
    sampled_quantity NUMERIC(19, 4),
    conform_quantity NUMERIC(19, 4),
    non_conform_quantity NUMERIC(19, 4),
    decision VARCHAR(40) NOT NULL,
    decision_by VARCHAR(120),
    decision_reason TEXT,
    inspected_by VARCHAR(120),
    inspected_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    non_conformance_code VARCHAR(80),
    CONSTRAINT uk_quality_inspection_code UNIQUE (inspection_code),
    CONSTRAINT fk_quality_inspection_plan FOREIGN KEY (plan_id) REFERENCES quality_control_plan (id),
    CONSTRAINT fk_quality_inspection_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    CONSTRAINT fk_quality_inspection_lot FOREIGN KEY (lot_id) REFERENCES stock_lot (id),
    CONSTRAINT fk_quality_inspection_location FOREIGN KEY (location_id) REFERENCES inventory_location (id)
);

CREATE INDEX IF NOT EXISTS idx_quality_inspection_lot ON quality_inspection (lot_id);
CREATE INDEX IF NOT EXISTS idx_quality_inspection_item_date ON quality_inspection (item_id, inspected_at DESC);

CREATE TABLE IF NOT EXISTS quality_inspection_result (
    id BIGINT PRIMARY KEY,
    inspection_id BIGINT NOT NULL,
    criterion_code VARCHAR(40) NOT NULL,
    criterion_name VARCHAR(160),
    measured_value NUMERIC(19, 4),
    measured_flag BOOLEAN,
    conform BOOLEAN NOT NULL DEFAULT FALSE,
    blocking BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    CONSTRAINT fk_quality_result_inspection FOREIGN KEY (inspection_id) REFERENCES quality_inspection (id) ON DELETE CASCADE
);

-- 3. Non-conformites

CREATE TABLE IF NOT EXISTS inventory_non_conformance (
    id BIGINT PRIMARY KEY,
    non_conformance_code VARCHAR(80) NOT NULL,
    item_id BIGINT NOT NULL,
    lot_id BIGINT,
    source_type VARCHAR(60),
    source_code VARCHAR(120),
    quantity NUMERIC(19, 4),
    severity VARCHAR(30) NOT NULL,
    description TEXT,
    disposition VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    corrective_action TEXT,
    responsible_code VARCHAR(120),
    due_date DATE,
    supplier_claim_code VARCHAR(120),
    cost_impact BIGINT,
    detected_by VARCHAR(120),
    detected_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    closed_by VARCHAR(120),
    closed_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT uk_non_conformance_code UNIQUE (non_conformance_code),
    CONSTRAINT fk_non_conformance_item FOREIGN KEY (item_id) REFERENCES inventory_item (id),
    CONSTRAINT fk_non_conformance_lot FOREIGN KEY (lot_id) REFERENCES stock_lot (id)
);

CREATE INDEX IF NOT EXISTS idx_non_conformance_open
    ON inventory_non_conformance (item_id, detected_at DESC)
    WHERE closed_at IS NULL;

-- 4. Rappels produit

CREATE TABLE IF NOT EXISTS inventory_product_recall (
    id BIGINT PRIMARY KEY,
    recall_code VARCHAR(80) NOT NULL,
    item_id BIGINT NOT NULL,
    lot_number_from VARCHAR(120),
    lot_number_to VARCHAR(120),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    reason TEXT,
    frozen_lot_count INTEGER,
    quantity_in_stock NUMERIC(19, 4),
    quantity_issued NUMERIC(19, 4),
    quantity_recovered NUMERIC(19, 4) NOT NULL DEFAULT 0,
    launched_by VARCHAR(120),
    launched_at TIMESTAMP(6) WITH TIME ZONE,
    closed_by VARCHAR(120),
    closed_at TIMESTAMP(6) WITH TIME ZONE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_product_recall_code UNIQUE (recall_code),
    CONSTRAINT fk_product_recall_item FOREIGN KEY (item_id) REFERENCES inventory_item (id)
);

CREATE INDEX IF NOT EXISTS idx_product_recall_active
    ON inventory_product_recall (item_id)
    WHERE status = 'ACTIVE';

-- 5. Sequences metier du lot

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2200001, 'quality_inspection', 'Inspection qualite', 'Sequence des inspections qualite', 'CQ', null, '{PREFIX}-{YYYY}{MM}-{SEQ}', 5, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'quality_inspection');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2200002, 'non_conformance', 'Non-conformite', 'Sequence des non-conformites', 'NC', null, '{PREFIX}-{YYYY}-{SEQ}', 5, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'non_conformance');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2200003, 'product_recall', 'Rappel produit', 'Sequence des rappels produit', 'RAP', null, '{PREFIX}-{YYYY}-{SEQ}', 4, 1, 1, 'YEARLY', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'product_recall');

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2200004, 'quality_control_plan', 'Plan de controle', 'Sequence des plans de controle qualite', 'PCQ', null, '{PREFIX}-{SEQ}', 4, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'quality_control_plan');

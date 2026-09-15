-- ============================================================
-- Lot 1 · Referentiel article et identification
-- ============================================================
-- Cycle de vie, caracteristiques physiques, codes-barres multiples,
-- conditionnements, substituts, traductions, historiques et variantes.

-- 1. Extension de inventory_item

ALTER TABLE inventory_item
    ADD COLUMN IF NOT EXISTS lifecycle_status  VARCHAR(30),
    ADD COLUMN IF NOT EXISTS revision          VARCHAR(40),
    ADD COLUMN IF NOT EXISTS weight_kg         NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS volume_m3         NUMERIC(19, 6),
    ADD COLUMN IF NOT EXISTS length_mm         INTEGER,
    ADD COLUMN IF NOT EXISTS width_mm          INTEGER,
    ADD COLUMN IF NOT EXISTS height_mm         INTEGER,
    ADD COLUMN IF NOT EXISTS stackable         BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS template_id       BIGINT,
    ADD COLUMN IF NOT EXISTS variant_signature VARCHAR(255);

-- Reprise : le booleen active devient derive du statut, il faut donc partir de lui.
UPDATE inventory_item
SET lifecycle_status = CASE WHEN active THEN 'ACTIVE' ELSE 'OBSOLETE' END
WHERE lifecycle_status IS NULL;

ALTER TABLE inventory_item
    ALTER COLUMN lifecycle_status SET NOT NULL,
    ALTER COLUMN lifecycle_status SET DEFAULT 'ACTIVE';

-- 2. Modeles d'articles a variantes

CREATE TABLE IF NOT EXISTS inventory_item_template (
    id BIGINT PRIMARY KEY,
    template_code VARCHAR(80) NOT NULL,
    name VARCHAR(220) NOT NULL,
    description TEXT,
    category_id BIGINT,
    unit_id BIGINT,
    item_type VARCHAR(40) NOT NULL,
    tracking_type VARCHAR(40) NOT NULL,
    default_cost BIGINT,
    sale_price BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_template_code UNIQUE (template_code),
    CONSTRAINT fk_inventory_template_category FOREIGN KEY (category_id) REFERENCES inventory_category (id),
    CONSTRAINT fk_inventory_template_unit FOREIGN KEY (unit_id) REFERENCES inventory_unit (id)
);

CREATE TABLE IF NOT EXISTS inventory_variant_axis (
    id BIGINT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    axis_code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    position INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT uk_inventory_axis_template_code UNIQUE (template_id, axis_code),
    CONSTRAINT fk_inventory_axis_template FOREIGN KEY (template_id) REFERENCES inventory_item_template (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS inventory_variant_value (
    id BIGINT PRIMARY KEY,
    axis_id BIGINT NOT NULL,
    value_code VARCHAR(40) NOT NULL,
    label VARCHAR(120) NOT NULL,
    position INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_inventory_variant_value_code UNIQUE (axis_id, value_code),
    CONSTRAINT fk_inventory_variant_value_axis FOREIGN KEY (axis_id) REFERENCES inventory_variant_axis (id) ON DELETE CASCADE
);

ALTER TABLE inventory_item
    DROP CONSTRAINT IF EXISTS fk_inventory_item_template;

ALTER TABLE inventory_item
    ADD CONSTRAINT fk_inventory_item_template
        FOREIGN KEY (template_id) REFERENCES inventory_item_template (id);

-- 3. Codes-barres

CREATE TABLE IF NOT EXISTS inventory_barcode (
    id BIGINT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    barcode_type VARCHAR(30) NOT NULL,
    barcode_value VARCHAR(120) NOT NULL,
    unit_code VARCHAR(80),
    quantity NUMERIC(19, 4) NOT NULL DEFAULT 1,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_barcode_value UNIQUE (barcode_value),
    CONSTRAINT fk_inventory_barcode_item FOREIGN KEY (item_id) REFERENCES inventory_item (id) ON DELETE CASCADE,
    CONSTRAINT chk_inventory_barcode_quantity CHECK (quantity > 0)
);

-- Un seul code principal par article.
CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_barcode_primary
    ON inventory_barcode (item_id)
    WHERE is_primary = TRUE;

CREATE INDEX IF NOT EXISTS idx_inventory_barcode_item ON inventory_barcode (item_id);

-- 4. Conditionnements

CREATE TABLE IF NOT EXISTS inventory_packaging (
    id BIGINT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    packaging_level VARCHAR(30) NOT NULL,
    name VARCHAR(120),
    quantity NUMERIC(19, 4) NOT NULL,
    barcode_value VARCHAR(120),
    weight_kg NUMERIC(19, 4),
    length_mm INTEGER,
    width_mm INTEGER,
    height_mm INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_packaging_item_level UNIQUE (item_id, packaging_level),
    CONSTRAINT fk_inventory_packaging_item FOREIGN KEY (item_id) REFERENCES inventory_item (id) ON DELETE CASCADE,
    CONSTRAINT chk_inventory_packaging_quantity CHECK (quantity > 0)
);

-- 5. Substituts

CREATE TABLE IF NOT EXISTS inventory_item_substitute (
    id BIGINT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    substitute_item_id BIGINT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 1,
    conversion_factor NUMERIC(19, 6) NOT NULL DEFAULT 1,
    bidirectional BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_substitute_pair UNIQUE (item_id, substitute_item_id),
    CONSTRAINT fk_inventory_substitute_item FOREIGN KEY (item_id) REFERENCES inventory_item (id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_substitute_target FOREIGN KEY (substitute_item_id) REFERENCES inventory_item (id) ON DELETE CASCADE,
    CONSTRAINT chk_inventory_substitute_not_self CHECK (item_id <> substitute_item_id),
    CONSTRAINT chk_inventory_substitute_factor CHECK (conversion_factor > 0)
);

CREATE INDEX IF NOT EXISTS idx_inventory_substitute_item ON inventory_item_substitute (item_id, priority);

-- 6. Traductions

CREATE TABLE IF NOT EXISTS inventory_item_translation (
    id BIGINT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    language_code VARCHAR(8) NOT NULL,
    name VARCHAR(220) NOT NULL,
    description TEXT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_inventory_translation_item_lang UNIQUE (item_id, language_code),
    CONSTRAINT fk_inventory_translation_item FOREIGN KEY (item_id) REFERENCES inventory_item (id) ON DELETE CASCADE
);

-- 7. Historiques de prix et de revision

CREATE TABLE IF NOT EXISTS inventory_item_price_history (
    id BIGINT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    price_type VARCHAR(30) NOT NULL,
    previous_value BIGINT,
    new_value BIGINT,
    changed_by VARCHAR(120),
    reason TEXT,
    changed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_inventory_price_history_item FOREIGN KEY (item_id) REFERENCES inventory_item (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_inventory_price_history_item
    ON inventory_item_price_history (item_id, changed_at DESC);

CREATE TABLE IF NOT EXISTS inventory_item_revision_history (
    id BIGINT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    previous_revision VARCHAR(40),
    new_revision VARCHAR(40) NOT NULL,
    changed_by VARCHAR(120),
    reason TEXT,
    document_code VARCHAR(120),
    changed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_inventory_revision_history_item FOREIGN KEY (item_id) REFERENCES inventory_item (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_inventory_revision_history_item
    ON inventory_item_revision_history (item_id, changed_at DESC);

-- 8. Index de support

CREATE INDEX IF NOT EXISTS idx_inventory_item_lifecycle ON inventory_item (lifecycle_status);
CREATE INDEX IF NOT EXISTS idx_inventory_item_template ON inventory_item (template_id);

-- 9. Sequences metier du lot

INSERT INTO sequence_definition
(id, code, name, description, prefix, suffix, pattern, padding, initial_value, increment_step, reset_policy, enabled, system_managed, created_at, updated_at)
SELECT 2170001, 'inventory_item_template', 'Modele article', 'Sequence des modeles d''articles a variantes', 'TPL', null, '{PREFIX}-{SEQ}', 5, 1, 1, 'NEVER', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM sequence_definition WHERE code = 'inventory_item_template');

-- ============================================================
-- Lot 3 · Tracabilite et quarantaine des lots
-- ============================================================
-- Genealogie des lots, blocage administratif distinct de la quarantaine, et
-- prise en compte du stock immobilise dans le calcul du disponible.
--
-- Regle de non-regression : aucun lot n'est en quarantaine ni bloque apres cette
-- migration, et quantity_quarantined vaut zero partout. Le disponible reste donc
-- exactement celui d'avant.

-- 1. Suivi de la quarantaine et blocage administratif

ALTER TABLE stock_lot
    ADD COLUMN IF NOT EXISTS quarantined_at       TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS quarantined_by       VARCHAR(120),
    ADD COLUMN IF NOT EXISTS blocked              BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS block_reason_type    VARCHAR(40),
    ADD COLUMN IF NOT EXISTS block_reason         TEXT,
    ADD COLUMN IF NOT EXISTS blocked_at           TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS blocked_by           VARCHAR(120),
    ADD COLUMN IF NOT EXISTS release_approved_by  VARCHAR(120),
    ADD COLUMN IF NOT EXISTS released_at          TIMESTAMP(6) WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_stock_lot_immobilised
    ON stock_lot (item_id, location_id)
    WHERE quarantined = TRUE OR blocked = TRUE;

-- 2. Quantite immobilisee portee par le niveau de stock

ALTER TABLE stock_level
    ADD COLUMN IF NOT EXISTS quantity_quarantined NUMERIC(19, 4) NOT NULL DEFAULT 0;

-- Le disponible existant reste juste : aucune quantite n'est immobilisee a ce stade.

-- 3. Genealogie des lots

CREATE TABLE IF NOT EXISTS stock_lot_genealogy (
    id BIGINT PRIMARY KEY,
    parent_lot_id BIGINT NOT NULL,
    child_lot_id BIGINT NOT NULL,
    relation VARCHAR(40) NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    source_movement_code VARCHAR(90),
    notes TEXT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by VARCHAR(120),
    CONSTRAINT fk_lot_genealogy_parent FOREIGN KEY (parent_lot_id) REFERENCES stock_lot (id) ON DELETE CASCADE,
    CONSTRAINT fk_lot_genealogy_child FOREIGN KEY (child_lot_id) REFERENCES stock_lot (id) ON DELETE CASCADE,
    CONSTRAINT chk_lot_genealogy_not_self CHECK (parent_lot_id <> child_lot_id),
    CONSTRAINT chk_lot_genealogy_quantity CHECK (quantity > 0),
    CONSTRAINT uk_lot_genealogy_pair UNIQUE (parent_lot_id, child_lot_id, relation)
);

CREATE INDEX IF NOT EXISTS idx_lot_genealogy_parent ON stock_lot_genealogy (parent_lot_id);
CREATE INDEX IF NOT EXISTS idx_lot_genealogy_child ON stock_lot_genealogy (child_lot_id);

-- 4. Index de support pour la remontee de tracabilite par numero de lot

CREATE INDEX IF NOT EXISTS idx_stock_lot_number ON stock_lot (lot_number);
CREATE INDEX IF NOT EXISTS idx_stock_movement_lot_number ON stock_movement_lot (lot_number);

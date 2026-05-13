CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE OR REPLACE FUNCTION inventory_normalize_text(input TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
AS $$
SELECT public.unaccent(lower(COALESCE(input, '')));
$$;

CREATE OR REPLACE FUNCTION inventory_search_match(source TEXT, query TEXT)
RETURNS BOOLEAN
LANGUAGE sql
IMMUTABLE
AS $$
SELECT
    query IS NULL
    OR inventory_normalize_text(source) LIKE '%' || inventory_normalize_text(query) || '%'
    OR inventory_normalize_text(source) % inventory_normalize_text(query);
$$;

CREATE OR REPLACE FUNCTION inventory_search_score(source TEXT, query TEXT)
RETURNS REAL
LANGUAGE sql
IMMUTABLE
AS $$
SELECT CASE
    WHEN query IS NULL THEN 0
    ELSE GREATEST(
        similarity(inventory_normalize_text(source), inventory_normalize_text(query)),
        CASE WHEN inventory_normalize_text(source) LIKE '%' || inventory_normalize_text(query) || '%' THEN 0.75 ELSE 0 END
    )
END;
$$;

ALTER TABLE inventory_item
    ALTER COLUMN default_cost TYPE BIGINT USING ROUND(default_cost::numeric)::BIGINT,
    ALTER COLUMN sale_price TYPE BIGINT USING ROUND(sale_price::numeric)::BIGINT;

ALTER TABLE stock_level
    ALTER COLUMN average_cost TYPE BIGINT USING ROUND(average_cost::numeric)::BIGINT;

ALTER TABLE stock_movement
    ALTER COLUMN unit_cost TYPE BIGINT USING ROUND(unit_cost::numeric)::BIGINT,
    ALTER COLUMN total_cost TYPE BIGINT USING ROUND(total_cost::numeric)::BIGINT;

ALTER TABLE asset
    ALTER COLUMN purchase_cost TYPE BIGINT USING ROUND(purchase_cost::numeric)::BIGINT;

ALTER TABLE asset_maintenance
    ALTER COLUMN cost TYPE BIGINT USING ROUND(cost::numeric)::BIGINT;

CREATE INDEX IF NOT EXISTS idx_inventory_item_search_text_trgm
ON inventory_item
USING gin (inventory_normalize_text(search_text) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_inventory_item_name_trgm
ON inventory_item
USING gin (inventory_normalize_text(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_inventory_item_code_trgm
ON inventory_item
USING gin (inventory_normalize_text(item_code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_inventory_item_psku_trgm
ON inventory_item
USING gin (inventory_normalize_text(psku) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_inventory_item_short_code_trgm
ON inventory_item
USING gin (inventory_normalize_text(short_code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_inventory_item_display_code_trgm
ON inventory_item
USING gin (inventory_normalize_text(display_code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_inventory_item_identification_code_trgm
ON inventory_item
USING gin (inventory_normalize_text(identification_code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_asset_code_trgm
ON asset
USING gin (inventory_normalize_text(asset_code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_asset_serial_number_trgm
ON asset
USING gin (inventory_normalize_text(serial_number) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_asset_tag_trgm
ON asset
USING gin (inventory_normalize_text(asset_tag) gin_trgm_ops);

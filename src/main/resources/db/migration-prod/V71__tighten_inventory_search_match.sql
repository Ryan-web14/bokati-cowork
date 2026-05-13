CREATE OR REPLACE FUNCTION inventory_search_match(source TEXT, query TEXT)
RETURNS BOOLEAN
LANGUAGE sql
IMMUTABLE
AS $$
SELECT
    inventory_normalize_text(query) <> ''
    AND (
        inventory_normalize_text(source) LIKE '%' || inventory_normalize_text(query) || '%'
        OR (
            length(inventory_normalize_text(query)) >= 3
            AND similarity(inventory_normalize_text(source), inventory_normalize_text(query)) >= 0.35
        )
    );
$$;

CREATE OR REPLACE FUNCTION inventory_search_score(source TEXT, query TEXT)
RETURNS REAL
LANGUAGE sql
IMMUTABLE
AS $$
SELECT CASE
    WHEN inventory_normalize_text(query) = '' THEN 0
    ELSE GREATEST(
        CASE
            WHEN length(inventory_normalize_text(query)) >= 3
            THEN similarity(inventory_normalize_text(source), inventory_normalize_text(query))
            ELSE 0
        END,
        CASE WHEN inventory_normalize_text(source) LIKE '%' || inventory_normalize_text(query) || '%' THEN 0.90 ELSE 0 END
    )
END;
$$;

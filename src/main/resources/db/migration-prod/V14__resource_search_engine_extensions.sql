CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE INDEX IF NOT EXISTS idx_resource_type_code_trgm
ON resource_type
USING gin (normalize_text(code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_resource_type_name_trgm
ON resource_type
USING gin (normalize_text(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_resource_type_description_trgm
ON resource_type
USING gin (normalize_text(description) gin_trgm_ops);

CREATE OR REPLACE FUNCTION search_resource_type(p_query TEXT)
RETURNS TABLE (
    id BIGINT,
    code TEXT,
    name TEXT,
    description TEXT,
    score REAL
)
LANGUAGE sql
AS $$
    SELECT
        rt.id,
        rt.code,
        rt.name,
        rt.description,
        GREATEST(
            similarity(normalize_text(rt.code), normalize_text(p_query)),
            similarity(normalize_text(rt.name), normalize_text(p_query)),
            similarity(normalize_text(rt.description), normalize_text(p_query))
        ) AS score
    FROM resource_type rt
    WHERE
        normalize_text(rt.code) % normalize_text(p_query)
        OR normalize_text(rt.name) % normalize_text(p_query)
        OR normalize_text(rt.description) % normalize_text(p_query)
    ORDER BY score DESC
    LIMIT 20;
$$;

CREATE INDEX IF NOT EXISTS idx_resource_group_code_trgm
ON resource_group
USING gin (normalize_text(code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_resource_group_name_trgm
ON resource_group
USING gin (normalize_text(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_resource_group_description_trgm
ON resource_group
USING gin (normalize_text(description) gin_trgm_ops);

CREATE OR REPLACE FUNCTION search_resource_group(p_query TEXT)
RETURNS TABLE (
    id BIGINT,
    code TEXT,
    name TEXT,
    description TEXT,
    score REAL
)
LANGUAGE sql
AS $$
    SELECT
        rg.id,
        rg.code,
        rg.name,
        rg.description,
        GREATEST(
            similarity(normalize_text(rg.code), normalize_text(p_query)),
            similarity(normalize_text(rg.name), normalize_text(p_query)),
            similarity(normalize_text(rg.description), normalize_text(p_query))
        ) AS score
    FROM resource_group rg
    WHERE
        normalize_text(rg.code) % normalize_text(p_query)
        OR normalize_text(rg.name) % normalize_text(p_query)
        OR normalize_text(rg.description) % normalize_text(p_query)
    ORDER BY score DESC
    LIMIT 20;
$$;

CREATE INDEX IF NOT EXISTS idx_resource_policy_code_trgm
ON resource_policy
USING gin (normalize_text(code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_resource_policy_name_trgm
ON resource_policy
USING gin (normalize_text(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_resource_policy_description_trgm
ON resource_policy
USING gin (normalize_text(description) gin_trgm_ops);

CREATE OR REPLACE FUNCTION search_resource_policy(p_query TEXT)
RETURNS TABLE (
    id BIGINT,
    code TEXT,
    name TEXT,
    description TEXT,
    score REAL
)
LANGUAGE sql
AS $$
    SELECT
        rp.id,
        rp.code,
        rp.name,
        rp.description,
        GREATEST(
            similarity(normalize_text(rp.code), normalize_text(p_query)),
            similarity(normalize_text(rp.name), normalize_text(p_query)),
            similarity(normalize_text(rp.description), normalize_text(p_query))
        ) AS score
    FROM resource_policy rp
    WHERE
        normalize_text(rp.code) % normalize_text(p_query)
        OR normalize_text(rp.name) % normalize_text(p_query)
        OR normalize_text(rp.description) % normalize_text(p_query)
    ORDER BY score DESC
    LIMIT 20;
$$;

ALTER TABLE customer ADD COLUMN IF NOT EXISTS email VARCHAR(255);

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE OR REPLACE FUNCTION normalize_text(input TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
AS $$
SELECT public.unaccent(lower(COALESCE(input, '')));
$$;

CREATE INDEX IF NOT EXISTS idx_customer_firstname_trgm
ON customer
USING gin (normalize_text(firstname) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_customer_lastname_trgm
ON customer
USING gin (normalize_text(lastname) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_customer_email_trgm
ON customer
USING gin (normalize_text(email) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_customer_company_trgm
ON customer
USING gin (normalize_text(company_name) gin_trgm_ops);

CREATE OR REPLACE FUNCTION search_customer(p_query TEXT)
RETURNS TABLE (
    id BIGINT,
    customer_id TEXT,
    fullname TEXT,
    email TEXT,
    company_name TEXT,
    score REAL
)
LANGUAGE sql
AS $$
    SELECT
        c.id,
        c.customer_id,
        concat_ws(' ', c.firstname, c.lastname) AS fullname,
        c.email,
        c.company_name,
        GREATEST(
            similarity(normalize_text(concat_ws(' ', c.firstname, c.lastname)), normalize_text(p_query)),
            similarity(normalize_text(c.email), normalize_text(p_query)),
            similarity(normalize_text(c.company_name), normalize_text(p_query))
        ) AS score
    FROM customer c
    WHERE
        normalize_text(concat_ws(' ', c.firstname, c.lastname)) % normalize_text(p_query)
        OR normalize_text(c.email) % normalize_text(p_query)
        OR normalize_text(c.company_name) % normalize_text(p_query)
    ORDER BY score DESC
    LIMIT 20;
$$;

CREATE INDEX IF NOT EXISTS idx_member_firstname_trgm
ON member
USING gin (normalize_text(firstname) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_member_lastname_trgm
ON member
USING gin (normalize_text(lastname) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_member_email_trgm
ON member
USING gin (normalize_text(email) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_member_phone_trgm
ON member
USING gin (normalize_text(phone) gin_trgm_ops);

CREATE OR REPLACE FUNCTION search_member(p_query TEXT)
RETURNS TABLE (
    id BIGINT,
    member_id TEXT,
    firstname TEXT,
    lastname TEXT,
    email TEXT,
    phone TEXT,
    score REAL
)
LANGUAGE sql
AS $$
    SELECT
        m.id,
        m.member_id,
        m.firstname,
        m.lastname,
        m.email,
        m.phone,
        GREATEST(
            similarity(normalize_text(concat_ws(' ', m.firstname, m.lastname)), normalize_text(p_query)),
            similarity(normalize_text(m.email), normalize_text(p_query)),
            similarity(normalize_text(m.phone), normalize_text(p_query))
        ) AS score
    FROM member m
    WHERE
        normalize_text(concat_ws(' ', m.firstname, m.lastname)) % normalize_text(p_query)
        OR normalize_text(m.email) % normalize_text(p_query)
        OR normalize_text(m.phone) % normalize_text(p_query)
    ORDER BY score DESC
    LIMIT 20;
$$;

CREATE INDEX IF NOT EXISTS idx_resource_code_trgm
ON resource
USING gin (normalize_text(code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_resource_name_trgm
ON resource
USING gin (normalize_text(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_resource_description_trgm
ON resource
USING gin (normalize_text(description) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_resource_zone_trgm
ON resource
USING gin (normalize_text(zone) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_resource_location_label_trgm
ON resource
USING gin (normalize_text(location_label) gin_trgm_ops);

CREATE OR REPLACE FUNCTION search_resource(p_query TEXT)
RETURNS TABLE (
    id BIGINT,
    code TEXT,
    name TEXT,
    type TEXT,
    description TEXT,
    score REAL
)
LANGUAGE sql
AS $$
    SELECT
        r.id,
        r.code,
        r.name,
        rt.code AS type,
        r.description,
        GREATEST(
            similarity(normalize_text(r.code), normalize_text(p_query)),
            similarity(normalize_text(r.name), normalize_text(p_query)),
            similarity(normalize_text(rt.code), normalize_text(p_query)),
            similarity(normalize_text(r.description), normalize_text(p_query)),
            similarity(normalize_text(r.zone), normalize_text(p_query)),
            similarity(normalize_text(r.location_label), normalize_text(p_query))
        ) AS score
    FROM resource r
    JOIN resource_type rt ON rt.id = r.type_id
    WHERE
        normalize_text(r.code) % normalize_text(p_query)
        OR normalize_text(r.name) % normalize_text(p_query)
        OR normalize_text(rt.code) % normalize_text(p_query)
        OR normalize_text(r.description) % normalize_text(p_query)
        OR normalize_text(r.zone) % normalize_text(p_query)
        OR normalize_text(r.location_label) % normalize_text(p_query)
    ORDER BY score DESC
    LIMIT 20;
$$;

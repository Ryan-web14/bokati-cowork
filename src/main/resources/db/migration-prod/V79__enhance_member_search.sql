CREATE INDEX IF NOT EXISTS idx_member_member_id_trgm
ON member
USING gin (normalize_text(member_id) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_customer_customer_id_trgm
ON customer
USING gin (normalize_text(customer_id) gin_trgm_ops);

CREATE OR REPLACE FUNCTION search_member_advanced(p_query TEXT)
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
            similarity(normalize_text(m.member_id), normalize_text(p_query)),
            similarity(normalize_text(COALESCE(c.customer_id, '')), normalize_text(p_query)),
            similarity(normalize_text(concat_ws(' ', m.firstname, m.lastname)), normalize_text(p_query)),
            similarity(normalize_text(m.email), normalize_text(p_query)),
            similarity(normalize_text(m.phone), normalize_text(p_query))
        ) AS score
    FROM member m
    LEFT JOIN customer c ON c.id = m.customer_id
    WHERE m.deleted = false
      AND (
        normalize_text(m.member_id) LIKE '%' || normalize_text(p_query) || '%'
        OR normalize_text(COALESCE(c.customer_id, '')) LIKE '%' || normalize_text(p_query) || '%'
        OR normalize_text(concat_ws(' ', m.firstname, m.lastname)) LIKE '%' || normalize_text(p_query) || '%'
        OR normalize_text(m.email) LIKE '%' || normalize_text(p_query) || '%'
        OR normalize_text(m.phone) LIKE '%' || normalize_text(p_query) || '%'
        OR normalize_text(m.member_id) % normalize_text(p_query)
        OR normalize_text(COALESCE(c.customer_id, '')) % normalize_text(p_query)
        OR normalize_text(concat_ws(' ', m.firstname, m.lastname)) % normalize_text(p_query)
        OR normalize_text(m.email) % normalize_text(p_query)
        OR normalize_text(m.phone) % normalize_text(p_query)
      )
    ORDER BY score DESC;
$$;

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
    SELECT *
    FROM search_member_advanced(p_query)
    LIMIT 20;
$$;

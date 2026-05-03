CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE INDEX IF NOT EXISTS idx_security_users_email_trgm
    ON users USING gin (normalize_text(email) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_security_users_user_id_trgm
    ON users USING gin (normalize_text(user_id) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_security_role_name_trgm
    ON role USING gin (normalize_text(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_security_role_display_name_trgm
    ON role USING gin (normalize_text(display_name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_security_permission_name_trgm
    ON permission USING gin (normalize_text(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_security_permission_display_name_trgm
    ON permission USING gin (normalize_text(display_name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_security_permission_module_action_trgm
    ON permission USING gin (normalize_text(module || ':' || action) gin_trgm_ops);

CREATE OR REPLACE FUNCTION search_admin_user(p_query TEXT)
RETURNS TABLE (
    id BIGINT,
    score REAL
)
LANGUAGE sql
AS $$
    SELECT
        u.id,
        GREATEST(
            similarity(normalize_text(u.email), normalize_text(p_query)),
            similarity(normalize_text(u.user_id), normalize_text(p_query))
        ) AS score
    FROM users u
    WHERE u.deleted = false
      AND (
          normalize_text(u.email) % normalize_text(p_query)
          OR normalize_text(u.user_id) % normalize_text(p_query)
          OR normalize_text(u.email) LIKE '%' || normalize_text(p_query) || '%'
          OR normalize_text(u.user_id) LIKE '%' || normalize_text(p_query) || '%'
      )
    ORDER BY score DESC, u.email ASC
    LIMIT 20;
$$;

CREATE OR REPLACE FUNCTION search_admin_role(p_query TEXT)
RETURNS TABLE (
    id BIGINT,
    score REAL
)
LANGUAGE sql
AS $$
    SELECT
        r.id,
        GREATEST(
            similarity(normalize_text(r.name), normalize_text(p_query)),
            similarity(normalize_text(r.display_name), normalize_text(p_query)),
            similarity(normalize_text(r.description), normalize_text(p_query))
        ) AS score
    FROM role r
    WHERE normalize_text(r.name) % normalize_text(p_query)
       OR normalize_text(r.display_name) % normalize_text(p_query)
       OR normalize_text(r.description) % normalize_text(p_query)
       OR normalize_text(r.name) LIKE '%' || normalize_text(p_query) || '%'
       OR normalize_text(r.display_name) LIKE '%' || normalize_text(p_query) || '%'
    ORDER BY score DESC, r.name ASC
    LIMIT 20;
$$;

CREATE OR REPLACE FUNCTION search_admin_permission(p_query TEXT)
RETURNS TABLE (
    id BIGINT,
    score REAL
)
LANGUAGE sql
AS $$
    SELECT
        p.id,
        GREATEST(
            similarity(normalize_text(p.name), normalize_text(p_query)),
            similarity(normalize_text(p.display_name), normalize_text(p_query)),
            similarity(normalize_text(p.module || ':' || p.action), normalize_text(p_query))
        ) AS score
    FROM permission p
    WHERE normalize_text(p.name) % normalize_text(p_query)
       OR normalize_text(p.display_name) % normalize_text(p_query)
       OR normalize_text(p.module || ':' || p.action) % normalize_text(p_query)
       OR normalize_text(p.name) LIKE '%' || normalize_text(p_query) || '%'
       OR normalize_text(p.display_name) LIKE '%' || normalize_text(p_query) || '%'
       OR normalize_text(p.module || ':' || p.action) LIKE '%' || normalize_text(p_query) || '%'
    ORDER BY score DESC, p.module ASC, p.action ASC
    LIMIT 20;
$$;

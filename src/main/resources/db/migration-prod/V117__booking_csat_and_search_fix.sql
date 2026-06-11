-- Satisfaction score (1–5) enregistré après complétion de la réservation
ALTER TABLE booking ADD COLUMN IF NOT EXISTS csat_score SMALLINT CHECK (csat_score BETWEEN 1 AND 5);

-- Mise à jour de search_booking : ILIKE d'abord (prefix/substring), trigram en complément (fuzzy)
CREATE OR REPLACE FUNCTION search_booking(p_query TEXT)
RETURNS TABLE (
    id            BIGINT,
    booking_number TEXT,
    resource_code  TEXT,
    owner_code     TEXT,
    status         TEXT,
    score          REAL
)
LANGUAGE sql
AS $$
    SELECT
        b.id,
        b.booking_number,
        r.code AS resource_code,
        b.owner_code,
        b.status,
        GREATEST(
            similarity(normalize_text(b.booking_number),              normalize_text(p_query)),
            similarity(normalize_text(b.owner_code),                  normalize_text(p_query)),
            similarity(normalize_text(COALESCE(b.contact_name,  '')), normalize_text(p_query)),
            similarity(normalize_text(COALESCE(b.contact_email, '')), normalize_text(p_query)),
            similarity(normalize_text(r.code),                        normalize_text(p_query)),
            similarity(normalize_text(r.name),                        normalize_text(p_query))
        ) AS score
    FROM booking b
    JOIN resource r ON r.id = b.resource_id
    WHERE b.deleted = false
      AND (
            -- substring / prefix — works for any query length, case-insensitive
            b.booking_number                   ILIKE '%' || p_query || '%'
         OR b.owner_code                       ILIKE '%' || p_query || '%'
         OR COALESCE(b.contact_name,  '')      ILIKE '%' || p_query || '%'
         OR COALESCE(b.contact_email, '')      ILIKE '%' || p_query || '%'
         OR r.code                             ILIKE '%' || p_query || '%'
         OR r.name                             ILIKE '%' || p_query || '%'
         -- trigram fuzzy — catches typos and partial normalized matches
         OR normalize_text(b.booking_number)                   % normalize_text(p_query)
         OR normalize_text(b.owner_code)                       % normalize_text(p_query)
         OR normalize_text(COALESCE(b.contact_name,  ''))      % normalize_text(p_query)
         OR normalize_text(COALESCE(b.contact_email, ''))      % normalize_text(p_query)
         OR normalize_text(r.code)                             % normalize_text(p_query)
         OR normalize_text(r.name)                             % normalize_text(p_query)
      )
    ORDER BY score DESC
    LIMIT 20;
$$;

CREATE INDEX IF NOT EXISTS idx_entitlement_definition_code_trgm
ON entitlement_definition
USING gin (normalize_text(code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_entitlement_definition_name_trgm
ON entitlement_definition
USING gin (normalize_text(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_entitlement_definition_description_trgm
ON entitlement_definition
USING gin (normalize_text(description) gin_trgm_ops);

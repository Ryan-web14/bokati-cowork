-- ============================================================
-- V185: Fine-grained amendment content — sections & variables
-- ============================================================

-- Section changes within an amendment (ADD / MODIFY / REMOVE) — BIGINT PK
CREATE TABLE contract_amendment_section (
    id                 BIGINT       PRIMARY KEY,
    amendment_id       BIGINT       REFERENCES contract_amendment(id) ON DELETE CASCADE,
    amendment_code     VARCHAR(120) NOT NULL,
    action             VARCHAR(20)  NOT NULL CHECK (action IN ('ADD','MODIFY','REMOVE')),
    target_section_ref VARCHAR(500),
    section_type       VARCHAR(40)  NOT NULL DEFAULT 'ARTICLE',
    title              VARCHAR(500),
    content            TEXT,
    section_order      INTEGER      NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_amsec_amendment_code ON contract_amendment_section(amendment_code);

-- Variable-level changes within an amendment — BIGINT PK
CREATE TABLE contract_amendment_variable (
    id             BIGINT       PRIMARY KEY,
    amendment_id   BIGINT       REFERENCES contract_amendment(id) ON DELETE CASCADE,
    amendment_code VARCHAR(120) NOT NULL,
    variable_key   VARCHAR(120) NOT NULL,
    previous_value TEXT,
    new_value      TEXT         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_amvar_amendment_key UNIQUE (amendment_code, variable_key)
);

CREATE INDEX idx_amvar_amendment_code ON contract_amendment_variable(amendment_code);

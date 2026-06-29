-- ============================================================
-- V183: Enrich contract_template + create contract draft tables
-- ============================================================

-- 1. Add content/layout columns to existing contract_template
ALTER TABLE contract_template ADD COLUMN IF NOT EXISTS html_content         TEXT;
ALTER TABLE contract_template ADD COLUMN IF NOT EXISTS css_content          TEXT;
ALTER TABLE contract_template ADD COLUMN IF NOT EXISTS header_html          TEXT;
ALTER TABLE contract_template ADD COLUMN IF NOT EXISTS footer_html          TEXT;
ALTER TABLE contract_template ADD COLUMN IF NOT EXISTS category             VARCHAR(100);
ALTER TABLE contract_template ADD COLUMN IF NOT EXISTS variable_definitions JSONB DEFAULT '[]'::jsonb;

CREATE INDEX IF NOT EXISTS idx_contract_template_category ON contract_template(category);

-- 2. Contract draft (free-form contract authoring)
CREATE TABLE contract_draft (
    id            BIGINT       PRIMARY KEY,
    code          VARCHAR(120) NOT NULL UNIQUE,
    title         VARCHAR(500) NOT NULL,
    description   TEXT,
    template_code VARCHAR(120),
    owner_type    VARCHAR(50),
    owner_code    VARCHAR(120),
    business_code VARCHAR(120),
    css_content   TEXT,
    header_html   TEXT,
    footer_html   TEXT,
    status        VARCHAR(30)  NOT NULL DEFAULT 'DRAFTING',
    created_by    BIGINT       NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_contract_draft_owner ON contract_draft(owner_type, owner_code);

-- 3. Contract draft sections (articles, clauses, blocks)
CREATE TABLE contract_draft_section (
    id            BIGINT       PRIMARY KEY,
    draft_id      BIGINT       NOT NULL REFERENCES contract_draft(id) ON DELETE CASCADE,
    title         VARCHAR(500),
    content       TEXT         NOT NULL,
    section_type  VARCHAR(50)  NOT NULL DEFAULT 'ARTICLE',
    section_order INTEGER      NOT NULL DEFAULT 0,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_contract_draft_section_draft ON contract_draft_section(draft_id);

-- ============================================================
-- V173: Contract template management & contract drafting
-- ============================================================

-- 1. Enrich contract_template with full content storage
ALTER TABLE contract_template ADD COLUMN html_content   TEXT;
ALTER TABLE contract_template ADD COLUMN css_content    TEXT;
ALTER TABLE contract_template ADD COLUMN header_html    TEXT;
ALTER TABLE contract_template ADD COLUMN footer_html    TEXT;
ALTER TABLE contract_template ADD COLUMN category       VARCHAR(100);
ALTER TABLE contract_template ADD COLUMN variable_definitions JSONB DEFAULT '[]'::jsonb;

-- 2. Contract draft (rédaction libre de contrats)
CREATE TABLE contract_draft (
    id              BIGINT       PRIMARY KEY,
    code            VARCHAR(120) NOT NULL UNIQUE,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    template_code   VARCHAR(120),
    owner_type      VARCHAR(50),
    owner_code      VARCHAR(120),
    business_code   VARCHAR(120),
    css_content     TEXT,
    header_html     TEXT,
    footer_html     TEXT,
    status          VARCHAR(30)  NOT NULL DEFAULT 'DRAFTING',
    created_by      BIGINT       NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 3. Contract draft sections (articles, clauses, blocs)
CREATE TABLE contract_draft_section (
    id              BIGINT       PRIMARY KEY,
    draft_id        BIGINT       NOT NULL REFERENCES contract_draft(id) ON DELETE CASCADE,
    title           VARCHAR(500),
    content         TEXT         NOT NULL,
    section_type    VARCHAR(50)  NOT NULL DEFAULT 'ARTICLE',
    section_order   INTEGER      NOT NULL DEFAULT 0,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_contract_draft_section_draft ON contract_draft_section(draft_id);
CREATE INDEX idx_contract_draft_owner ON contract_draft(owner_type, owner_code);
CREATE INDEX idx_contract_template_category ON contract_template(category);

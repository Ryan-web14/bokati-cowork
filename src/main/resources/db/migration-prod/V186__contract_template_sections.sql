-- ============================================================
-- V186: Fine-grained section management for contract templates
-- ============================================================

CREATE TABLE contract_template_section (
    id             BIGINT       PRIMARY KEY,
    template_id    BIGINT       NOT NULL REFERENCES contract_template(id) ON DELETE CASCADE,
    template_code  VARCHAR(120) NOT NULL,
    title          VARCHAR(500),
    content        TEXT         NOT NULL,
    section_type   VARCHAR(50)  NOT NULL DEFAULT 'ARTICLE',
    section_order  INTEGER      NOT NULL DEFAULT 0,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cts_template_code ON contract_template_section(template_code);
CREATE INDEX idx_cts_template_id   ON contract_template_section(template_id);

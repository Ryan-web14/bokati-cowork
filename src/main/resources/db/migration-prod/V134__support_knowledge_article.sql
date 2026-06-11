CREATE TABLE IF NOT EXISTS support_knowledge_article (
    id BIGSERIAL PRIMARY KEY,
    article_code VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(30),
    tags VARCHAR(500),
    public_visible BOOLEAN NOT NULL DEFAULT FALSE,
    internal_only BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    view_count BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_support_knowledge_article_code UNIQUE (article_code),
    CONSTRAINT uq_support_knowledge_article_slug UNIQUE (slug)
);

CREATE INDEX IF NOT EXISTS idx_support_knowledge_article_active ON support_knowledge_article (active, public_visible);
CREATE INDEX IF NOT EXISTS idx_support_knowledge_article_category ON support_knowledge_article (category);

-- Tracks every preview/download access per document for audit purposes
CREATE TABLE document_access_log (
    id          BIGSERIAL PRIMARY KEY,
    document_id BIGINT       NOT NULL REFERENCES document (id),
    user_id     BIGINT       NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    accessed_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    ip_address  VARCHAR(45),
    user_agent  TEXT
);

CREATE INDEX idx_doc_access_doc  ON document_access_log (document_id, accessed_at DESC);
CREATE INDEX idx_doc_access_user ON document_access_log (user_id, accessed_at DESC);

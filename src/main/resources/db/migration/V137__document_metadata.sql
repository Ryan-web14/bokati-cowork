-- Free-form key-value metadata per document
CREATE TABLE document_metadata (
    id          BIGSERIAL PRIMARY KEY,
    document_id BIGINT       NOT NULL REFERENCES document (id),
    meta_key    VARCHAR(100) NOT NULL,
    meta_value  TEXT,
    CONSTRAINT uq_document_meta_key UNIQUE (document_id, meta_key)
);

CREATE INDEX idx_document_metadata_doc ON document_metadata (document_id);
CREATE INDEX idx_document_metadata_key ON document_metadata (meta_key);

-- Document tag catalog
CREATE TABLE document_tag (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(80)  NOT NULL UNIQUE,
    label      VARCHAR(150) NOT NULL,
    color      VARCHAR(7),
    space      VARCHAR(50),
    created_by BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_document_tag_space ON document_tag (space);

-- Tag assignments to documents
CREATE TABLE document_tag_assignment (
    id          BIGSERIAL PRIMARY KEY,
    document_id BIGINT    NOT NULL REFERENCES document (id),
    tag_id      BIGINT    NOT NULL REFERENCES document_tag (id),
    tagged_by   BIGINT,
    tagged_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_document_tag UNIQUE (document_id, tag_id)
);

CREATE INDEX idx_tag_assignment_doc ON document_tag_assignment (document_id);
CREATE INDEX idx_tag_assignment_tag ON document_tag_assignment (tag_id);

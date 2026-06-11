CREATE TABLE document_retention_policy (
    id                  BIGINT                      NOT NULL,
    code                VARCHAR(100)                NOT NULL,
    name                VARCHAR(200)                NOT NULL,
    space               VARCHAR(50),
    document_type_code  VARCHAR(100),
    retention_days      INTEGER                     NOT NULL,
    retention_reference VARCHAR(30)                 NOT NULL DEFAULT 'UPLOAD_DATE',
    action              VARCHAR(20)                 NOT NULL DEFAULT 'ARCHIVE',
    active              BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE    DEFAULT NOW(),
    created_by          BIGINT,

    CONSTRAINT pk_document_retention_policy PRIMARY KEY (id),
    CONSTRAINT uq_document_retention_policy_code UNIQUE (code)
);

CREATE INDEX idx_doc_retention_space  ON document_retention_policy(space)  WHERE space IS NOT NULL;
CREATE INDEX idx_doc_retention_active ON document_retention_policy(active);

-- V171: Document folder hierarchy for GED

CREATE TABLE IF NOT EXISTS document_folder
(
    id          BIGINT       NOT NULL,
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(300) NOT NULL,
    description TEXT,
    parent_id   BIGINT,
    space       VARCHAR(50)  NOT NULL DEFAULT 'GENERIC',
    owner_type  VARCHAR(20),
    owner_id    BIGINT,
    path        VARCHAR(2000) NOT NULL DEFAULT '/',
    depth       INTEGER      NOT NULL DEFAULT 0,
    sort_order  INTEGER               DEFAULT 0,
    color       VARCHAR(7),
    icon        VARCHAR(50),
    created_by  BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_document_folder PRIMARY KEY (id),
    CONSTRAINT uk_document_folder_code UNIQUE (code),
    CONSTRAINT fk_document_folder_parent FOREIGN KEY (parent_id) REFERENCES document_folder (id)
);

CREATE INDEX IF NOT EXISTS idx_document_folder_parent ON document_folder (parent_id);
CREATE INDEX IF NOT EXISTS idx_document_folder_space ON document_folder (space);
CREATE INDEX IF NOT EXISTS idx_document_folder_path ON document_folder (path varchar_pattern_ops);
CREATE INDEX IF NOT EXISTS idx_document_folder_owner ON document_folder (owner_type, owner_id);

-- Add folder_id to document
ALTER TABLE document
    ADD COLUMN IF NOT EXISTS folder_id BIGINT;

ALTER TABLE document
    ADD CONSTRAINT fk_document_folder FOREIGN KEY (folder_id) REFERENCES document_folder (id);

CREATE INDEX IF NOT EXISTS idx_document_folder_id ON document (folder_id);

-- Sequence for folder codes
INSERT INTO sequence_definition (
    id, code, name, description, prefix, suffix, pattern,
    padding, initial_value, increment_step, reset_policy, enabled, system_managed,
    created_at, updated_at
)
SELECT 171001, 'FOLDER', 'Dossier GED', 'Séquence des dossiers documentaires', 'FLD', null, '{PREFIX}-{YYYY}{MM}-{SEQ}',
       6, 1, 1, 'MONTHLY', true, false, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM sequence_definition WHERE code = 'FOLDER'
);
